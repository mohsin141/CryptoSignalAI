package com.cryptosignalai.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Binance public market-data endpoints (no API key required for these reads).
 * Base URL: https://api.binance.com/
 *
 * Klines are returned as an array of arrays:
 * [ openTime, open, high, low, close, volume, closeTime, ... ]
 *
 * We deserialize every element as String for maximum robustness (Gson's
 * JsonReader.nextString() reads JSON numbers as strings too), then convert in
 * the repository. This avoids generic Object/typing pitfalls.
 */
interface BinanceApi {

    @GET("api/v3/klines")
    suspend fun getKlines(
        @Query("symbol") symbol: String,
        @Query("interval") interval: String,
        @Query("limit") limit: Int = 500
    ): List<List<String>>

    @GET("api/v3/ticker/24hr")
    suspend fun get24hTicker(
        @Query("symbol") symbol: String
    ): Ticker24h
}

data class Ticker24h(
    val symbol: String,
    val lastPrice: String,
    val priceChangePercent: String,
    val highPrice: String,
    val lowPrice: String,
    val volume: String,
    val quoteVolume: String
)
