package com.cryptosignalai.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * CoinGecko metadata (optional). Base URL: https://api.coingecko.com/
 * Used only for supplementary metadata such as market cap / 24h change.
 */
interface CoinGeckoApi {

    @GET("api/v3/simple/price")
    suspend fun getSimplePrice(
        @Query("ids") ids: String,
        @Query("vs_currencies") vsCurrencies: String = "usd",
        @Query("include_market_cap") includeMarketCap: Boolean = true,
        @Query("include_24hr_change") include24hChange: Boolean = true
    ): Map<String, Map<String, Double>>
}
