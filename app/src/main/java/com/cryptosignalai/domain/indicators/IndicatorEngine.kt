package com.cryptosignalai.domain.indicators

import com.cryptosignalai.data.model.Candle
import com.cryptosignalai.data.model.IndicatorSnapshot
import com.cryptosignalai.data.model.Timeframe
import com.cryptosignalai.data.model.Token
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Pure, local technical-indicator calculations. No network, no AI.
 * All series functions return a list aligned to the input (older -> newer).
 * The candle list MUST be ordered oldest-first.
 */
object IndicatorEngine {

    fun analyze(token: Token, timeframe: Timeframe, candles: List<Candle>): IndicatorSnapshot? {
        if (candles.size < 210) return null   // need >= 200 for EMA200 + buffer

        val closes = candles.map { it.close }
        val highs = candles.map { it.high }
        val lows = candles.map { it.low }
        val volumes = candles.map { it.volume }

        val rsi = rsi(closes, 14).lastOrNull() ?: return null
        val (macdLine, signalLine, hist) = macd(closes)
        val ema20 = ema(closes, 20).last()
        val ema50 = ema(closes, 50).last()
        val ema200 = ema(closes, 200).last()
        val (bbU, bbM, bbL) = bollinger(closes, 20, 2.0)
        val atr = atr(highs, lows, closes, 14).lastOrNull() ?: 0.0
        val adx = adx(highs, lows, closes, 14)
        val vwap = vwap(candles)
        val lookback = 50
        val support = lows.takeLast(lookback).min()
        val resistance = highs.takeLast(lookback).max()
        val avgVol = volumes.takeLast(20).average()
        val lastVol = volumes.last()
        val volumeSpike = lastVol > avgVol * 1.5

        return IndicatorSnapshot(
            token = token,
            timeframe = timeframe,
            lastClose = closes.last(),
            rsi = rsi.round2(),
            macdLine = macdLine.round4(),
            macdSignal = signalLine.round4(),
            macdHist = hist.round4(),
            ema20 = ema20.round2(),
            ema50 = ema50.round2(),
            ema200 = ema200.round2(),
            bbUpper = bbU.round2(),
            bbMiddle = bbM.round2(),
            bbLower = bbL.round2(),
            atr = atr.round4(),
            adx = adx.round2(),
            vwap = vwap.round2(),
            support = support.round2(),
            resistance = resistance.round2(),
            volumeSpike = volumeSpike,
            avgVolume = avgVol.round2(),
            lastVolume = lastVol.round2()
        )
    }

    // ---- EMA ---------------------------------------------------------------
    fun ema(values: List<Double>, period: Int): List<Double> {
        if (values.isEmpty()) return emptyList()
        val k = 2.0 / (period + 1)
        val out = ArrayList<Double>(values.size)
        var prev = values.first()
        out.add(prev)
        for (i in 1 until values.size) {
            prev = values[i] * k + prev * (1 - k)
            out.add(prev)
        }
        return out
    }

    fun sma(values: List<Double>, period: Int): Double =
        values.takeLast(period).average()

    // ---- RSI (Wilder) ------------------------------------------------------
    fun rsi(closes: List<Double>, period: Int = 14): List<Double> {
        if (closes.size <= period) return emptyList()
        val rsiList = ArrayList<Double>()
        var gainSum = 0.0
        var lossSum = 0.0
        for (i in 1..period) {
            val ch = closes[i] - closes[i - 1]
            if (ch >= 0) gainSum += ch else lossSum -= ch
        }
        var avgGain = gainSum / period
        var avgLoss = lossSum / period
        rsiList.add(rsiFrom(avgGain, avgLoss))
        for (i in period + 1 until closes.size) {
            val ch = closes[i] - closes[i - 1]
            val gain = if (ch > 0) ch else 0.0
            val loss = if (ch < 0) -ch else 0.0
            avgGain = (avgGain * (period - 1) + gain) / period
            avgLoss = (avgLoss * (period - 1) + loss) / period
            rsiList.add(rsiFrom(avgGain, avgLoss))
        }
        return rsiList
    }

    private fun rsiFrom(avgGain: Double, avgLoss: Double): Double {
        if (avgLoss == 0.0) return 100.0
        val rs = avgGain / avgLoss
        return 100.0 - (100.0 / (1.0 + rs))
    }

    // ---- MACD --------------------------------------------------------------
    /** Returns Triple(macdLine, signalLine, histogram) for the latest candle. */
    fun macd(closes: List<Double>, fast: Int = 12, slow: Int = 26, signal: Int = 9): Triple<Double, Double, Double> {
        val emaFast = ema(closes, fast)
        val emaSlow = ema(closes, slow)
        val macdSeries = closes.indices.map { emaFast[it] - emaSlow[it] }
        val signalSeries = ema(macdSeries, signal)
        val macdLine = macdSeries.last()
        val signalLine = signalSeries.last()
        return Triple(macdLine, signalLine, macdLine - signalLine)
    }

    // ---- Bollinger Bands ---------------------------------------------------
    /** Returns Triple(upper, middle, lower). */
    fun bollinger(closes: List<Double>, period: Int = 20, mult: Double = 2.0): Triple<Double, Double, Double> {
        val window = closes.takeLast(period)
        val mean = window.average()
        val variance = window.sumOf { (it - mean) * (it - mean) } / period
        val sd = sqrt(variance)
        return Triple(mean + mult * sd, mean, mean - mult * sd)
    }

    // ---- ATR (Wilder) ------------------------------------------------------
    fun atr(highs: List<Double>, lows: List<Double>, closes: List<Double>, period: Int = 14): List<Double> {
        if (highs.size <= period) return emptyList()
        val tr = ArrayList<Double>()
        for (i in 1 until highs.size) {
            val h = highs[i]; val l = lows[i]; val pc = closes[i - 1]
            tr.add(max(h - l, max(abs(h - pc), abs(l - pc))))
        }
        val out = ArrayList<Double>()
        var atr = tr.take(period).average()
        out.add(atr)
        for (i in period until tr.size) {
            atr = (atr * (period - 1) + tr[i]) / period
            out.add(atr)
        }
        return out
    }

    // ---- ADX (Wilder) ------------------------------------------------------
    fun adx(highs: List<Double>, lows: List<Double>, closes: List<Double>, period: Int = 14): Double {
        val n = highs.size
        if (n <= period * 2) return 0.0
        val plusDM = DoubleArray(n)
        val minusDM = DoubleArray(n)
        val tr = DoubleArray(n)
        for (i in 1 until n) {
            val upMove = highs[i] - highs[i - 1]
            val downMove = lows[i - 1] - lows[i]
            plusDM[i] = if (upMove > downMove && upMove > 0) upMove else 0.0
            minusDM[i] = if (downMove > upMove && downMove > 0) downMove else 0.0
            val h = highs[i]; val l = lows[i]; val pc = closes[i - 1]
            tr[i] = max(h - l, max(abs(h - pc), abs(l - pc)))
        }
        // Wilder smoothing
        var trS = 0.0; var pS = 0.0; var mS = 0.0
        for (i in 1..period) { trS += tr[i]; pS += plusDM[i]; mS += minusDM[i] }
        val dxList = ArrayList<Double>()
        for (i in period + 1 until n) {
            trS = trS - trS / period + tr[i]
            pS = pS - pS / period + plusDM[i]
            mS = mS - mS / period + minusDM[i]
            val plusDI = if (trS == 0.0) 0.0 else 100.0 * pS / trS
            val minusDI = if (trS == 0.0) 0.0 else 100.0 * mS / trS
            val diSum = plusDI + minusDI
            val dx = if (diSum == 0.0) 0.0 else 100.0 * abs(plusDI - minusDI) / diSum
            dxList.add(dx)
        }
        if (dxList.size < period) return dxList.average().takeIf { !it.isNaN() } ?: 0.0
        var adx = dxList.take(period).average()
        for (i in period until dxList.size) {
            adx = (adx * (period - 1) + dxList[i]) / period
        }
        return adx
    }

    // ---- VWAP (over loaded window) ----------------------------------------
    fun vwap(candles: List<Candle>): Double {
        val window = candles.takeLast(50)
        var pv = 0.0; var vol = 0.0
        for (c in window) {
            val typical = (c.high + c.low + c.close) / 3.0
            pv += typical * c.volume
            vol += c.volume
        }
        return if (vol == 0.0) candles.last().close else pv / vol
    }

    private fun Double.round2(): Double = Math.round(this * 100.0) / 100.0
    private fun Double.round4(): Double = Math.round(this * 10000.0) / 10000.0
}
