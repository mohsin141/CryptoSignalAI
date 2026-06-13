package com.cryptosignalai.data.repository

import com.cryptosignalai.data.model.Candle
import com.cryptosignalai.data.model.Timeframe
import com.cryptosignalai.data.model.Token
import com.cryptosignalai.data.remote.BinanceApi
import com.cryptosignalai.data.remote.CoinGeckoApi
import com.cryptosignalai.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches OHLCV candles (Binance) and optional metadata (CoinGecko).
 * Pure data access — no business logic.
 */
class MarketRepository(
    private val binance: BinanceApi = NetworkModule.binanceApi,
    private val coinGecko: CoinGeckoApi = NetworkModule.coinGeckoApi
) {

    /** Fetch and parse candles for a token/timeframe. Most recent candle is last. */
    suspend fun getCandles(
        token: Token,
        timeframe: Timeframe,
        limit: Int = 500
    ): List<Candle> = withContext(Dispatchers.IO) {
        val raw = binance.getKlines(token.binanceSymbol, timeframe.binanceInterval, limit)
        raw.mapNotNull { row ->
            if (row.size < 7) return@mapNotNull null
            runCatching {
                Candle(
                    openTime = row[0].toLong(),
                    open = row[1].toDouble(),
                    high = row[2].toDouble(),
                    low = row[3].toDouble(),
                    close = row[4].toDouble(),
                    volume = row[5].toDouble(),
                    closeTime = row[6].toLong()
                )
            }.getOrNull()
        }
    }

    /** Latest traded price (also derivable from the last candle close). */
    suspend fun getLastPrice(token: Token): Double = withContext(Dispatchers.IO) {
        binance.get24hTicker(token.binanceSymbol).lastPrice.toDouble()
    }

    /** Optional metadata enrichment. Returns null if the request fails. */
    suspend fun getMetadata(token: Token): Map<String, Double>? = withContext(Dispatchers.IO) {
        runCatching { coinGecko.getSimplePrice(token.coinGeckoId)[token.coinGeckoId] }.getOrNull()
    }
}
