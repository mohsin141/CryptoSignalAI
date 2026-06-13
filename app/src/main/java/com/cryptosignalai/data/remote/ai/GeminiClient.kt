package com.cryptosignalai.data.remote.ai

import com.cryptosignalai.data.model.AiProviderType
import com.cryptosignalai.data.model.AiSignal
import com.cryptosignalai.data.remote.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Google Gemini API (Generative Language).
 * Endpoint: .../models/{model}:generateContent?key=API_KEY
 * Requests strict JSON via responseMimeType.
 */
class GeminiClient(private val apiKey: String) : AIProvider {

    override val type = AiProviderType.GEMINI
    private val json = "application/json".toMediaType()

    override suspend fun analyze(systemPrompt: String, userJson: String): AiSignal =
        withContext(Dispatchers.IO) {
            val parts = JSONArray().put(JSONObject().put("text", "$systemPrompt\n\nDATA:\n$userJson"))
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
                .put("generationConfig", JSONObject()
                    .put("temperature", 0.2)
                    .put("responseMimeType", "application/json"))
                .toString()

            val req = Request.Builder()
                .url("$BASE/$MODEL:generateContent?key=$apiKey")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody(json))
                .build()

            NetworkModule.okHttp.newCall(req).execute().use { resp ->
                val raw = resp.body?.string()
                if (!resp.isSuccessful) return@withContext AiSignal.noTrade("Gemini HTTP ${resp.code}")
                val text = runCatching {
                    JSONObject(raw!!)
                        .getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts")
                        .getJSONObject(0).getString("text")
                }.getOrNull()
                AiJson.extractSignal(text)
            }
        }

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts",
                    JSONArray().put(JSONObject().put("text", "ping")))))
                .toString()
            val req = Request.Builder()
                .url("$BASE/$MODEL:generateContent?key=$apiKey")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody(json))
                .build()
            NetworkModule.okHttp.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) "Gemini connection OK"
                else throw IllegalStateException("HTTP ${resp.code}: ${resp.body?.string()?.take(160)}")
            }
        }
    }

    companion object {
        private const val BASE = "https://generativelanguage.googleapis.com/v1beta/models"
        private const val MODEL = "gemini-2.5-flash"
    }
}
