package com.cryptosignalai.data.remote.ai

import com.cryptosignalai.data.model.AiProviderType
import com.cryptosignalai.data.model.AiSignal
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Common contract for all AI back-ends. The app sends ONLY processed JSON
 * (computed indicators + rule context) — never raw candles — and requires a
 * strict JSON response matching [AiSignal].
 */
interface AIProvider {
    val type: AiProviderType

    /** Validate a setup. [systemPrompt] sets rules; [userJson] is processed data. */
    suspend fun analyze(systemPrompt: String, userJson: String): AiSignal

    /** Lightweight connectivity + auth check. Returns human-readable result. */
    suspend fun testConnection(): Result<String>

    companion object {
        /** Build the configured provider with its key. Returns null if no key. */
        fun create(type: AiProviderType, apiKey: String?): AIProvider? {
            if (apiKey.isNullOrBlank()) return null
            return when (type) {
                AiProviderType.CLAUDE -> ClaudeClient(apiKey)
                AiProviderType.GEMINI -> GeminiClient(apiKey)
                AiProviderType.CODEX -> CodexClient(apiKey)
            }
        }
    }
}

/** The strict system prompt shared by every provider. */
object AiPrompt {
    val SYSTEM = """
        You are a disciplined crypto trade-signal validator. You receive ONLY
        pre-computed technical indicators and rule-engine context as JSON. You
        never see raw candles and you must NEVER guess or invent numbers.

        Decide whether the setup justifies a trade. Strongly prefer NO_TRADE
        over weak or ambiguous setups. If signals conflict, the answer is
        NO_TRADE. Only return BUY or SELL when the evidence is clear and the
        risk/reward is sound.

        Respond with EXACTLY ONE JSON object and nothing else. No markdown, no
        prose, no code fences. The schema is:
        {
          "signal": "BUY|SELL|HOLD|NO_TRADE",
          "confidence": 0-100,
          "entry": number,
          "stop_loss": number,
          "take_profit": number,
          "risk": "LOW|MEDIUM|HIGH",
          "reason": "short reason"
        }
        Use the provided entry/stop/target as anchors unless the data clearly
        contradicts them. confidence must reflect agreement of the indicators.
    """.trimIndent()
}

/** Shared JSON helpers for parsing model output. */
object AiJson {
    val gson: Gson = GsonBuilder().setLenient().create()

    /** Extract the first {...} JSON object from arbitrary model text. */
    fun extractSignal(text: String?): AiSignal {
        if (text.isNullOrBlank()) return AiSignal.noTrade("Empty AI response")
        val start = text.indexOf('{')
        val end = text.lastIndexOf('}')
        if (start < 0 || end <= start) return AiSignal.noTrade("Malformed AI response")
        val json = text.substring(start, end + 1)
        return runCatching { gson.fromJson(json, AiSignal::class.java) }
            .getOrElse { AiSignal.noTrade("Unparseable AI JSON") }
            .let { normalize(it) }
    }

    private fun normalize(s: AiSignal): AiSignal {
        val sig = s.signal.uppercase().trim()
        val valid = sig in listOf("BUY", "SELL", "HOLD", "NO_TRADE")
        val risk = s.risk.uppercase().trim().ifBlank { "MEDIUM" }
        return s.copy(
            signal = if (valid) sig else "NO_TRADE",
            confidence = s.confidence.coerceIn(0, 100),
            risk = if (risk in listOf("LOW", "MEDIUM", "HIGH")) risk else "MEDIUM"
        )
    }
}
