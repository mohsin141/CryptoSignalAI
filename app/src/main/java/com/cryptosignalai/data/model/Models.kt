package com.cryptosignalai.data.model

import com.google.gson.annotations.SerializedName

/** The 10 supported tokens. Binance symbol + CoinGecko id for metadata. */
enum class Token(
    val displayName: String,
    val binanceSymbol: String,
    val coinGeckoId: String
) {
    BTC("Bitcoin", "BTCUSDT", "bitcoin"),
    ETH("Ethereum", "ETHUSDT", "ethereum"),
    BNB("BNB", "BNBUSDT", "binancecoin"),
    SOL("Solana", "SOLUSDT", "solana"),
    XRP("XRP", "XRPUSDT", "ripple"),
    ADA("Cardano", "ADAUSDT", "cardano"),
    DOGE("Dogecoin", "DOGEUSDT", "dogecoin"),
    AVAX("Avalanche", "AVAXUSDT", "avalanche-2"),
    LINK("Chainlink", "LINKUSDT", "chainlink"),
    TON("Toncoin", "TONUSDT", "the-open-network");

    companion object {
        fun fromName(name: String): Token = entries.firstOrNull { it.name == name } ?: BTC
    }
}

/** Supported chart timeframes mapped to Binance interval strings. */
enum class Timeframe(val label: String, val binanceInterval: String) {
    M5("5m", "5m"),
    M15("15m", "15m"),
    H1("1h", "1h"),
    H4("4h", "4h"),
    D1("1d", "1d");

    companion object {
        fun fromLabel(label: String): Timeframe = entries.firstOrNull { it.label == label } ?: H1
    }
}

/** A single OHLCV candle. */
data class Candle(
    val openTime: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double,
    val closeTime: Long
)

/** Locally computed technical indicators for the latest candle. */
data class IndicatorSnapshot(
    val token: Token,
    val timeframe: Timeframe,
    val lastClose: Double,
    val rsi: Double,
    val macdLine: Double,
    val macdSignal: Double,
    val macdHist: Double,
    val ema20: Double,
    val ema50: Double,
    val ema200: Double,
    val bbUpper: Double,
    val bbMiddle: Double,
    val bbLower: Double,
    val atr: Double,
    val adx: Double,
    val vwap: Double,
    val support: Double,
    val resistance: Double,
    val volumeSpike: Boolean,
    val avgVolume: Double,
    val lastVolume: Double
) {
    /** EMA stacking trend label used by UI + rule engine. */
    val emaTrend: String
        get() = when {
            ema20 > ema50 && ema50 > ema200 -> "BULLISH"
            ema20 < ema50 && ema50 < ema200 -> "BEARISH"
            else -> "MIXED"
        }
}

/** Output of the deterministic rule engine that runs BEFORE any AI call. */
data class RuleResult(
    val passed: Boolean,
    val confirmations: Int,
    val bias: String,            // BUY / SELL / NEUTRAL
    val reasons: List<String>,
    val rejectReasons: List<String>,
    val suggestedEntry: Double,
    val suggestedStop: Double,
    val suggestedTarget: Double,
    val riskReward: Double
)

/** Strict-schema response we require from the AI. */
data class AiSignal(
    @SerializedName("signal") val signal: String,        // BUY|SELL|HOLD|NO_TRADE
    @SerializedName("confidence") val confidence: Int,   // 0-100
    @SerializedName("entry") val entry: Double,
    @SerializedName("stop_loss") val stopLoss: Double,
    @SerializedName("take_profit") val takeProfit: Double,
    @SerializedName("risk") val risk: String,            // LOW|MEDIUM|HIGH
    @SerializedName("reason") val reason: String
) {
    companion object {
        fun noTrade(reason: String) = AiSignal("NO_TRADE", 0, 0.0, 0.0, 0.0, "LOW", reason)
    }
}

/** Result of the local backtest over similar historical conditions. */
data class BacktestResult(
    val approved: Boolean,
    val winRate: Double,     // 0..1
    val samples: Int,
    val avgReturn: Double
)

/** Final, fully validated signal shown in the UI and stored to the DB. */
data class FinalSignal(
    val token: Token,
    val timeframe: Timeframe,
    val decision: String,        // BUY|SELL|HOLD|NO_TRADE
    val confidence: Int,
    val entry: Double,
    val stopLoss: Double,
    val takeProfit: Double,
    val risk: String,
    val reason: String,
    val indicators: IndicatorSnapshot?,
    val backtestWinRate: Double,
    val backtestSamples: Int,
    val createdAt: Long = System.currentTimeMillis()
)

/** Wrapper used by the pipeline so the UI can show progress / failure stage. */
data class PipelineOutcome(
    val final: FinalSignal,
    val stageLog: List<String>
)

enum class AiProviderType(val displayName: String) {
    CLAUDE("Claude API"),
    GEMINI("Gemini API"),
    CODEX("Codex API")
}
