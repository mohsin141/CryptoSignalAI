# CryptoSignalAI

A **personal-use, fully local** Android app (Kotlin + Jetpack Compose) that analyzes the
top 10 crypto tokens and produces **AI-validated trade signals**. There is **no backend**:
the app talks only to public market-data APIs and to the AI provider you configure.

> ⚠️ For personal/educational use only. This is **not** financial advice. Crypto trading is
> high-risk. Signals can be wrong. Never trade money you cannot afford to lose.

---

## What it does

```
Fetch market data (Binance)
      → Calculate indicators locally (RSI, MACD, EMA20/50/200, Bollinger, ATR, ADX, VWAP, S/R, vol spike)
      → Deterministic Rule Engine (≥4 confirmations, ADX≥20, volume, trend, risk/reward)
      → If it passes, send PROCESSED JSON (never raw candles) to the AI you chose
      → AI returns strict JSON (BUY/SELL/HOLD/NO_TRADE + confidence + entry/SL/TP + risk)
      → Local Backtest (pattern win-rate must be ≥ 70%)
      → Final signal shown + stored + notification (only if strong & confidence ≥ 80%)
```
Any failed gate ⇒ **NO_TRADE**.

### Supported tokens
BTC, ETH, BNB, SOL, XRP, ADA, DOGE, AVAX, LINK, TON (switchable in the UI).

### Timeframes
5m, 15m, 1h, 4h, 1d.

### AI providers (pick one in Settings, add your own API key)
- **Claude API** — Anthropic Messages API
- **Gemini API** — Google Generative Language API
- **Codex API** — implemented against the OpenAI Chat Completions API

API keys are stored **encrypted on-device** via Jetpack Security (`EncryptedSharedPreferences`,
AES-256, Android Keystore). Keys are excluded from cloud backup.

---

## Requirements

- **Android Studio** Koala (2024.1.1) or newer
- **JDK 17** (bundled with recent Android Studio)
- **Android SDK 34**, min device **Android 8.0 (API 26)**
- Internet connection on the device/emulator

---

## How to build (Android Studio — recommended)

1. **Unzip** `CryptoSignalAI.zip`.
2. In Android Studio: **File → Open** → select the `CryptoSignalAI` folder.
3. Let Gradle **sync** (Android Studio downloads dependencies and, if missing, regenerates
   the Gradle wrapper jar automatically).
4. Plug in a device (USB debugging on) **or** start an emulator.
5. Press **Run ▶** (or `Shift+F10`).

### Generate an installable APK
- **Debug APK:** menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
  Output: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK (signed):** **Build → Generate Signed Bundle / APK → APK**, create or choose
  a keystore, finish. Output: `app/build/outputs/apk/release/app-release.apk`

---

## How to build (command line)

> The repo ships the `gradlew` / `gradlew.bat` scripts. The binary
> `gradle/wrapper/gradle-wrapper.jar` is generated on first Android Studio sync. If you build
> purely from the CLI without ever opening Android Studio, generate it once with a locally
> installed Gradle 8.9:  `gradle wrapper --gradle-version 8.9`

```bash
# from the project root
# Debug APK
./gradlew assembleDebug          # Windows: gradlew.bat assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# Release APK (configure signing first, see below)
./gradlew assembleRelease

# Install onto a connected device
./gradlew installDebug
# or
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### (Optional) Release signing
Create `~/.gradle/gradle.properties` or pass `-P` flags, then add a `signingConfigs` block to
`app/build.gradle.kts`. For personal use the **debug APK is enough** and installs directly.

---

## First run

1. Open the app → tap **Settings**.
2. Choose your AI model (Claude / Gemini / Codex), paste its **API key**, tap **Save**,
   then **Test connection**.
3. Back on Home: pick a **token** and **timeframe**, tap **Refresh**.
4. Toggle **background checks** (every 15 min) and **notifications** in Settings.

> Without an API key the pipeline stops at the AI step and returns NO_TRADE.

### Where to get API keys
- Claude: https://console.anthropic.com/
- Gemini: https://aistudio.google.com/app/apikey
- OpenAI (Codex): https://platform.openai.com/api-keys

You can change the exact model strings in
`app/src/main/java/com/cryptosignalai/data/remote/ai/*Client.kt` (`MODEL` constants).

---

## Permissions
- `INTERNET`, `ACCESS_NETWORK_STATE` — fetch market data + call AI
- `POST_NOTIFICATIONS` (Android 13+) — local alerts
- `RECEIVE_BOOT_COMPLETED` — WorkManager reschedules periodic checks after reboot

## Tech / dependencies
Jetpack Compose (Material 3), Navigation Compose, Retrofit + OkHttp + Gson, Room (KSP),
AndroidX Security Crypto, WorkManager, Kotlin Coroutines, Lifecycle.

See `PROJECT_STRUCTURE.md` for the full file tree and what each file does.
