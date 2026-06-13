package com.cryptosignalai.domain.backtest

import com.cryptosignalai.data.model.BacktestResult
import com.cryptosignalai.data.model.Candle
import com.cryptosignalai.data.model.IndicatorSnapshot
import com.cryptosignalai.domain.indicators.IndicatorEngine
import kotlin.math.abs

/**
 * Simple, local, pattern-based backtest.
 *
 * It scans history for candles whose conditions resemble the CURRENT setup
 * (same EMA-trend direction, similar RSI, matching MACD sign) and measures how
 * often that pattern resolved profitably over a fixed forward horizon, using
 * the same ATR-derived stop/target ratio the live signal would use.
 *
 * A setup is only approved when the historical win-rate is >= 70% over a
 * meaningful sample size. Otherwise it is rejected -> NO_TRADE.
 */
object BacktestEngine {

    private const val MIN_WIN_RATE = 0.70
    private const val MIN_SAMPLES = 10
    private const val FORWARD_HORIZON = 12
    private const val RSI_TOLERANCE = 8.0
    private const val ATR_STOP_MULT = 1.5
    private const val ATR_TARGET_MULT = 3.0

    fun run(candles: List<Candle>, current: IndicatorSnapshot, bias: String): BacktestResult {
        if (bias != "BUY" && bias != "SELL") return BacktestResult(false, 0.0, 0, 0.0)
        if (candles.size < 260) return BacktestResult(false, 0.0, 0, 0.0)

        val closes = candles.map { it.close }
        val highs = candles.map { it.high }
        val lows = candles.map { it.low }

        // Full-length aligned series (NaN during warmup).
        val rsiSeries = alignedRsi(closes, 14)
        val ema20 = IndicatorEngine.ema(closes, 20)
        val ema50 = IndicatorEngine.ema(closes, 50)
        val ema200 = IndicatorEngine.ema(closes, 200)
        val macdHist = alignedMacdHist(closes)

        // Stop/target as a fraction of price, derived from the current ATR.
        val stopPct = (current.atr * ATR_STOP_MULT) / current.lastClose
        val targetPct = (current.atr * ATR_TARGET_MULT) / current.lastClose
        if (stopPct <= 0.0) return BacktestResult(false, 0.0, 0, 0.0)

        var wins = 0
        var samples = 0
        var returnSum = 0.0

        val start = 200
        val end = candles.size - FORWARD_HORIZON - 1
        for (i in start until end) {
            if (rsiSeries[i].isNaN() || macdHist[i].isNaN()) continue

            val trend = when {
                ema20[i] > ema50[i] && ema50[i] > ema200[i] -> "BUY"
                ema20[i] < ema50[i] && ema50[i] < ema200[i] -> "SELL"
                else -> "NEUTRAL"
            }
            val macdSignOk = if (bias == "BUY") macdHist[i] > 0 else macdHist[i] < 0
            val rsiClose = abs(rsiSeries[i] - current.rsi) <= RSI_TOLERANCE

            if (trend == bias && macdSignOk && rsiClose) {
                samples++
                val entry = closes[i]
                val stop = if (bias == "BUY") entry * (1 - stopPct) else entry * (1 + stopPct)
                val target = if (bias == "BUY") entry * (1 + targetPct) else entry * (1 - targetPct)

                var resolved = false
                for (j in i + 1..i + FORWARD_HORIZON) {
                    if (bias == "BUY") {
                        if (highs[j] >= target) { wins++; returnSum += targetPct; resolved = true; break }
                        if (lows[j] <= stop) { returnSum -= stopPct; resolved = true; break }
                    } else {
                        if (lows[j] <= target) { wins++; returnSum += targetPct; resolved = true; break }
                        if (highs[j] >= stop) { returnSum -= stopPct; resolved = true; break }
                    }
                }
                if (!resolved) {
                    val finalClose = closes[i + FORWARD_HORIZON]
                    val ret = if (bias == "BUY") (finalClose - entry) / entry else (entry - finalClose) / entry
                    returnSum += ret
                    if (ret > 0) wins++
                }
            }
        }

        if (samples < MIN_SAMPLES) return BacktestResult(false, 0.0, samples, 0.0)
        val winRate = wins.toDouble() / samples
        val avgReturn = returnSum / samples
        return BacktestResult(
            approved = winRate >= MIN_WIN_RATE,
            winRate = round4(winRate),
            samples = samples,
            avgReturn = round4(avgReturn)
        )
    }

    private fun alignedRsi(closes: List<Double>, period: Int): DoubleArray {
        val out = DoubleArray(closes.size) { Double.NaN }
        val rsi = IndicatorEngine.rsi(closes, period)
        // rsi[0] corresponds to index = period
        for (k in rsi.indices) {
            val idx = period + k
            if (idx < out.size) out[idx] = rsi[k]
        }
        return out
    }

    private fun alignedMacdHist(closes: List<Double>): DoubleArray {
        val emaFast = IndicatorEngine.ema(closes, 12)
        val emaSlow = IndicatorEngine.ema(closes, 26)
        val macdSeries = closes.indices.map { emaFast[it] - emaSlow[it] }
        val signal = IndicatorEngine.ema(macdSeries, 9)
        return DoubleArray(closes.size) { macdSeries[it] - signal[it] }
    }

    private fun round4(v: Double) = Math.round(v * 10000.0) / 10000.0
}
