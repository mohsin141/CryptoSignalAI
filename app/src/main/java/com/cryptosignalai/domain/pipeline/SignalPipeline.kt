package com.cryptosignalai.domain.pipeline

import com.cryptosignalai.data.model.AiProviderType
import com.cryptosignalai.data.model.FinalSignal
import com.cryptosignalai.data.model.PipelineOutcome
import com.cryptosignalai.data.model.Timeframe
import com.cryptosignalai.data.model.Token
import com.cryptosignalai.data.remote.ai.AIProvider
import com.cryptosignalai.data.remote.ai.AiPrompt
import com.cryptosignalai.data.repository.MarketRepository
import com.cryptosignalai.domain.backtest.BacktestEngine
import com.cryptosignalai.domain.indicators.IndicatorEngine
import com.cryptosignalai.domain.rules.RuleEngine
import org.json.JSONObject

/**
 * Orchestrates the full validated-signal flow:
 *
 * 1. Fetch market data (candles)
 * 2. Calculate indicators locally
 * 3. Run deterministic rule validation
 * 4. If valid -> send PROCESSED JSON (never raw candles) to the selected AI
 * 5. AI validates and returns strict JSON
 * 6. Run local backtest check (win-rate >= 70%)
 * 7. Produce the final signal
 *
 * Any failed gate short-circuits to a NO_TRADE result with an explanatory log.
 */
class SignalPipeline(
    private val marketRepo: MarketRepository = MarketRepository()
) {

    suspend fun run(
        token: Token,
        timeframe: Timeframe,
        providerType: AiProviderType,
        apiKey: String?
    ): PipelineOutcome {
        val log = mutableListOf<String>()

        // 1. Market data
        val candles = runCatching { marketRepo.getCandles(token, timeframe, 500) }
            .getOrElse {
                return noTrade(token, timeframe, "Failed to fetch market data: ${it.message}", log,
                    "Market data fetch failed")
            }
        log.add("Fetched ${candles.size} candles")

        // 2. Indicators
        val ind = IndicatorEngine.analyze(token, timeframe, candles)
            ?: return noTrade(token, timeframe, "Insufficient data for indicators", log,
                "Indicators: not enough candles")
        log.add("Indicators computed (RSI ${ind.rsi}, ADX ${ind.adx}, trend ${ind.emaTrend})")

        // 3. Rule validation
        val rule = RuleEngine.evaluate(ind)
        if (!rule.passed) {
            log.add("Rule engine REJECT: ${rule.rejectReasons.joinToString("; ")}")
            return PipelineOutcome(
                FinalSignal(
                    token, timeframe, "NO_TRADE", 0,
                    rule.suggestedEntry, rule.suggestedStop, rule.suggestedTarget,
                    "LOW", "Rule gate failed: ${rule.rejectReasons.joinToString(", ")}",
                    ind, 0.0, 0
                ), log
            )
        }
        log.add("Rule engine PASS: ${rule.confirmations} confirmations, bias ${rule.bias}, RR ${rule.riskReward}")

        // 4. + 5. AI validation
        val provider = AIProvider.create(providerType, apiKey)
            ?: return noTrade(token, timeframe,
                "No API key set for ${providerType.displayName}", log,
                "AI skipped: missing API key")
        val processed = buildProcessedJson(token, timeframe, ind, rule)
        val ai = runCatching { provider.analyze(AiPrompt.SYSTEM, processed) }
            .getOrElse {
                return noTrade(token, timeframe, "AI call failed: ${it.message}", log,
                    "AI call error")
            }
        log.add("AI(${providerType.displayName}) -> ${ai.signal} @ ${ai.confidence}%")

        if (ai.signal == "NO_TRADE" || ai.signal == "HOLD") {
            return PipelineOutcome(
                FinalSignal(
                    token, timeframe, ai.signal, ai.confidence,
                    ai.entry, ai.stopLoss, ai.takeProfit, ai.risk,
                    ai.reason.ifBlank { "AI declined the setup" }, ind, 0.0, 0
                ), log
            )
        }

        // 6. Backtest
        val backtest = BacktestEngine.run(candles, ind, rule.bias)
        log.add("Backtest: winRate ${(backtest.winRate * 100).toInt()}% over ${backtest.samples} samples")
        if (!backtest.approved) {
            return PipelineOutcome(
                FinalSignal(
                    token, timeframe, "NO_TRADE", 0,
                    ai.entry, ai.stopLoss, ai.takeProfit, "LOW",
                    "Backtest below 70% win-rate (${(backtest.winRate * 100).toInt()}% / ${backtest.samples})",
                    ind, backtest.winRate, backtest.samples
                ), log
            )
        }

        // 7. Final signal — anchor to AI numbers, fall back to rule suggestions.
        val entry = ai.entry.takeIf { it > 0 } ?: rule.suggestedEntry
        val stop = ai.stopLoss.takeIf { it > 0 } ?: rule.suggestedStop
        val tp = ai.takeProfit.takeIf { it > 0 } ?: rule.suggestedTarget
        val finalConfidence = minOf(ai.confidence, (backtest.winRate * 100).toInt())
        log.add("FINAL ${ai.signal} confidence $finalConfidence%")

        return PipelineOutcome(
            FinalSignal(
                token, timeframe, ai.signal, finalConfidence,
                entry, stop, tp, ai.risk,
                ai.reason.ifBlank { rule.reasons.joinToString(", ") },
                ind, backtest.winRate, backtest.samples
            ), log
        )
    }

    /** Only processed indicators + rule context are ever sent to the AI. */
    private fun buildProcessedJson(
        token: Token, timeframe: Timeframe,
        ind: com.cryptosignalai.data.model.IndicatorSnapshot,
        rule: com.cryptosignalai.data.model.RuleResult
    ): String {
        val o = JSONObject()
        o.put("token", token.name)
        o.put("timeframe", timeframe.label)
        o.put("last_close", ind.lastClose)
        val indicators = JSONObject()
            .put("rsi", ind.rsi)
            .put("macd_line", ind.macdLine)
            .put("macd_signal", ind.macdSignal)
            .put("macd_hist", ind.macdHist)
            .put("ema20", ind.ema20)
            .put("ema50", ind.ema50)
            .put("ema200", ind.ema200)
            .put("ema_trend", ind.emaTrend)
            .put("bb_upper", ind.bbUpper)
            .put("bb_middle", ind.bbMiddle)
            .put("bb_lower", ind.bbLower)
            .put("atr", ind.atr)
            .put("adx", ind.adx)
            .put("vwap", ind.vwap)
            .put("support", ind.support)
            .put("resistance", ind.resistance)
            .put("volume_spike", ind.volumeSpike)
            .put("avg_volume", ind.avgVolume)
            .put("last_volume", ind.lastVolume)
        o.put("indicators", indicators)
        val ruleCtx = JSONObject()
            .put("bias", rule.bias)
            .put("confirmations", rule.confirmations)
            .put("reasons", rule.reasons.joinToString("; "))
            .put("suggested_entry", rule.suggestedEntry)
            .put("suggested_stop", rule.suggestedStop)
            .put("suggested_target", rule.suggestedTarget)
            .put("risk_reward", rule.riskReward)
        o.put("rule_context", ruleCtx)
        return o.toString()
    }

    private fun noTrade(
        token: Token, timeframe: Timeframe, reason: String,
        log: MutableList<String>, logLine: String
    ): PipelineOutcome {
        log.add(logLine)
        return PipelineOutcome(
            FinalSignal(token, timeframe, "NO_TRADE", 0, 0.0, 0.0, 0.0, "LOW", reason, null, 0.0, 0),
            log
        )
    }
}
