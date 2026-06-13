package com.cryptosignalai.domain.rules

import com.cryptosignalai.data.model.IndicatorSnapshot
import com.cryptosignalai.data.model.RuleResult
import kotlin.math.abs

/**
 * Deterministic gate that runs BEFORE any AI call.
 *
 * Rejects setups that are statistically weak so we never waste an AI request
 * (and never act) on noise. A setup must collect >= MIN_CONFIRMATIONS aligned
 * signals, have real volume, trend strength (ADX), no trend conflict, and an
 * acceptable risk/reward before it is allowed to continue.
 */
object RuleEngine {

    private const val MIN_CONFIRMATIONS = 4
    private const val MIN_ADX = 20.0
    private const val MIN_RISK_REWARD = 1.5
    private const val ATR_STOP_MULT = 1.5
    private const val ATR_TARGET_MULT = 3.0   // 2:1 reward:risk baseline

    fun evaluate(ind: IndicatorSnapshot): RuleResult {
        val reasons = mutableListOf<String>()
        val rejects = mutableListOf<String>()

        var bullish = 0
        var bearish = 0

        // 1. EMA stack / trend
        when (ind.emaTrend) {
            "BULLISH" -> { bullish++; reasons.add("EMA stack bullish (20>50>200)") }
            "BEARISH" -> { bearish++; reasons.add("EMA stack bearish (20<50<200)") }
        }

        // 2. Price vs VWAP
        if (ind.lastClose > ind.vwap) { bullish++; reasons.add("Price above VWAP") }
        else if (ind.lastClose < ind.vwap) { bearish++; reasons.add("Price below VWAP") }

        // 3. MACD
        if (ind.macdLine > ind.macdSignal && ind.macdHist > 0) { bullish++; reasons.add("MACD bullish crossover") }
        else if (ind.macdLine < ind.macdSignal && ind.macdHist < 0) { bearish++; reasons.add("MACD bearish crossover") }

        // 4. RSI momentum (avoid overbought/oversold extremes as entries)
        when {
            ind.rsi in 50.0..68.0 -> { bullish++; reasons.add("RSI momentum up (${ind.rsi})") }
            ind.rsi in 32.0..50.0 -> { bearish++; reasons.add("RSI momentum down (${ind.rsi})") }
        }

        // 5. Bollinger position
        if (ind.lastClose > ind.bbMiddle) { bullish++; reasons.add("Above Bollinger mid-band") }
        else if (ind.lastClose < ind.bbMiddle) { bearish++; reasons.add("Below Bollinger mid-band") }

        // 6. Volume spike confirming the move
        if (ind.volumeSpike) {
            reasons.add("Volume spike confirms move")
            if (ind.lastClose >= ind.bbMiddle) bullish++ else bearish++
        }

        val bias = when {
            bullish > bearish -> "BUY"
            bearish > bullish -> "SELL"
            else -> "NEUTRAL"
        }
        val confirmations = maxOf(bullish, bearish)

        // ---- Risk / reward (ATR based, clamped to S/R) ----
        val entry = ind.lastClose
        val riskDist = ind.atr * ATR_STOP_MULT
        val rewardDist = ind.atr * ATR_TARGET_MULT
        val (stop, target) = when (bias) {
            "BUY" -> (entry - riskDist) to (entry + rewardDist)
            "SELL" -> (entry + riskDist) to (entry - rewardDist)
            else -> entry to entry
        }
        val rr = if (riskDist > 0) rewardDist / riskDist else 0.0

        // ---- Rejection checks ----
        if (bias == "NEUTRAL") rejects.add("No directional bias")
        if (confirmations < MIN_CONFIRMATIONS) rejects.add("Only $confirmations/$MIN_CONFIRMATIONS confirmations")
        if (!ind.volumeSpike && ind.lastVolume < ind.avgVolume * 0.8) rejects.add("Weak volume")
        if (ind.adx < MIN_ADX) rejects.add("ADX ${ind.adx} < $MIN_ADX (no trend strength)")
        if (isTrendConflict(bias, ind)) rejects.add("Trend conflict with EMA200")
        if (rr < MIN_RISK_REWARD) rejects.add("Risk/reward $rr < $MIN_RISK_REWARD")

        val passed = rejects.isEmpty()
        return RuleResult(
            passed = passed,
            confirmations = confirmations,
            bias = bias,
            reasons = reasons,
            rejectReasons = rejects,
            suggestedEntry = entry,
            suggestedStop = round2(stop),
            suggestedTarget = round2(target),
            riskReward = round2(rr)
        )
    }

    /** A BUY against a clearly bearish long-term trend (or vice-versa) is a conflict. */
    private fun isTrendConflict(bias: String, ind: IndicatorSnapshot): Boolean {
        val longTermBull = ind.lastClose > ind.ema200
        return when (bias) {
            "BUY" -> !longTermBull && abs(ind.lastClose - ind.ema200) / ind.ema200 > 0.02
            "SELL" -> longTermBull && abs(ind.lastClose - ind.ema200) / ind.ema200 > 0.02
            else -> false
        }
    }

    private fun round2(v: Double) = Math.round(v * 100.0) / 100.0
}
