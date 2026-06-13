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
 * "Codex" provider implemented against the OpenAI Chat Completions API
 * (OpenAI is the successor to the original Codex models).
 * Endpoint: https://api.openai.com/v1/chat/completions
 * Auth: Bearer token. Forces JSON object output via response_format.
 */
class CodexClient(private val apiKey: String) : AIProvider {

    override val type = AiProviderType.CODEX
    private val json = "application/json".toMediaType()

    override suspend fun analyze(systemPrompt: String, userJson: String): AiSignal =
        withContext(Dispatchers.IO) {
            val messages = JSONArray()
                .put(JSONObject().put("role", "system").put("content", systemPrompt))
                .put(JSONObject().put("role", "user").put("content", userJson))
            val body = JSONObject()
                .put("model", MODEL)
                .put("temperature", 0.2)
                .put("response_format", JSONObject().put("type", "json_object"))
                .put("messages", messages)
                .toString()

            val req = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody(json))
                .build()

            NetworkModule.okHttp.newCall(req).execute().use { resp ->
                val raw = resp.body?.string()
                if (!resp.isSuccessful) return@withContext AiSignal.noTrade("Codex HTTP ${resp.code}")
                val text = runCatching {
                    JSONObject(raw!!).getJSONArray("choices").getJSONObject(0)
                        .getJSONObject("message").getString("content")
                }.getOrNull()
                AiJson.extractSignal(text)
            }
        }

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("model", MODEL)
                .put("max_tokens", 5)
                .put("messages", JSONArray().put(
                    JSONObject().put("role", "user").put("content", "ping")))
                .toString()
            val req = Request.Builder()
                .url(ENDPOINT)
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody(json))
                .build()
            NetworkModule.okHttp.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) "Codex (OpenAI) connection OK"
                else throw IllegalStateException("HTTP ${resp.code}: ${resp.body?.string()?.take(160)}")
            }
        }
    }

    companion object {
        private const val ENDPOINT = "https://api.openai.com/v1/chat/completions"
        private const val MODEL = "gpt-4o-mini"
    }
}
