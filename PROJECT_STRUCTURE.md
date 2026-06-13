# Project structure

```
CryptoSignalAI/
├── settings.gradle.kts          # modules + repositories
├── build.gradle.kts             # root plugins
├── gradle.properties            # Gradle/AndroidX flags
├── gradle/
│   ├── libs.versions.toml       # version catalog (all dependency versions)
│   └── wrapper/gradle-wrapper.properties
├── gradlew / gradlew.bat        # wrapper scripts
├── README.md
├── PROJECT_STRUCTURE.md
└── app/
    ├── build.gradle.kts         # app module: SDKs, deps, build types
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml  # permissions, app + activity
        ├── res/                 # theme, colors, strings, launcher icon, backup rules
        └── java/com/cryptosignalai/
            ├── CryptoSignalApp.kt        # Application; schedules WorkManager
            ├── MainActivity.kt           # Compose host; asks notif permission
            ├── data/
            │   ├── model/Models.kt       # Token, Timeframe, Candle, IndicatorSnapshot,
            │   │                         #   RuleResult, AiSignal, BacktestResult, FinalSignal…
            │   ├── remote/
            │   │   ├── BinanceApi.kt      # OHLCV klines + 24h ticker
            │   │   ├── CoinGeckoApi.kt    # optional metadata
            │   │   ├── NetworkModule.kt   # Retrofit/OkHttp/Gson setup
            │   │   └── ai/
            │   │       ├── AIProvider.kt  # interface + factory + strict prompt + JSON parser
            │   │       ├── ClaudeClient.kt
            │   │       ├── GeminiClient.kt
            │   │       └── CodexClient.kt # OpenAI Chat Completions
            │   ├── local/                 # Room: Entities, SignalDao, AppDatabase
            │   └── repository/            # MarketRepository, SignalRepository
            ├── domain/
            │   ├── indicators/IndicatorEngine.kt   # RSI/MACD/EMA/BB/ATR/ADX/VWAP/S-R/volume
            │   ├── rules/RuleEngine.kt             # pre-AI gate
            │   ├── backtest/BacktestEngine.kt      # local pattern win-rate
            │   └── pipeline/SignalPipeline.kt      # orchestrates the full flow
            ├── security/SecureStorage.kt           # EncryptedSharedPreferences
            ├── notification/NotificationHelper.kt  # local notifications
            ├── work/
            │   ├── SignalWorker.kt                 # periodic CoroutineWorker
            │   └── SignalScheduler.kt              # enqueue/cancel (every 15 min)
            └── ui/
                ├── theme/                          # Color, Type, Theme (dark)
                ├── components/SignalCard.kt, IndicatorSummary.kt
                ├── home/HomeScreen.kt, HomeViewModel.kt
                ├── settings/SettingsScreen.kt, SettingsViewModel.kt
                ├── history/HistoryScreen.kt, HistoryViewModel.kt
                └── navigation/AppNav.kt
```
