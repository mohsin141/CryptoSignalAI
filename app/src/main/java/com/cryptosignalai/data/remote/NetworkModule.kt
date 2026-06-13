package com.cryptosignalai.data.remote

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Centralised Retrofit/OkHttp setup. No backend of our own — these are direct
 * calls to public third-party services.
 */
object NetworkModule {

    const val BINANCE_BASE = "https://api.binance.com/"
    const val COINGECKO_BASE = "https://api.coingecko.com/"

    val gson: Gson = GsonBuilder().setLenient().create()

    val okHttp: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    private fun retrofit(baseUrl: String): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttp)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val binanceApi: BinanceApi by lazy { retrofit(BINANCE_BASE).create(BinanceApi::class.java) }
    val coinGeckoApi: CoinGeckoApi by lazy { retrofit(COINGECKO_BASE).create(CoinGeckoApi::class.java) }
}
