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
 * Anthropic Claude Messages API.
 * Endpoint: https://api.anthropic.com/v1/messages
 * Auth: header x-api-key + anthropic-version.
 * Change MODEL below to any model your key has access to.
 */
class ClaudeClient(private val apiKey: String) : AIProvider {

    override val type = AiProviderType.CLAUDE
    private val json = "application/json".toMediaType()

    override suspend fun analyze(systemPrompt: String, userJson: String): AiSignal =
        withContext(Dispatchers.IO) {
            val body = JSONObject()
                .put("model", MODEL)
                .put("max_tokens", 512)
                .put("system", systemPrompt)
                .put("messages", JSONArray().put(
                    JSONObject().put("role", "user").put("content", userJson)
                ))
                .toString()

            val req = Request.Builder()
                .url(ENDPOINT)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody(json))
                .build()

            NetworkModule.okHttp.newCall(req).execute().use { resp ->
                val raw = resp.body?.string()
                if (!resp.isSuccessful) return@withContext AiSignal.noTrade("Claude HTTP ${resp.code}")
                val text = runCatching {
                    JSONObject(raw!!).getJSONArray("content").getJSONObject(0).getString("text")
                }.getOrNull()
                AiJson.extractSignal(text)
            }
        }

    override suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val body = JSONObject()
                .put("model", MODEL)
                .put("max_tokens", 16)
                .put("messages", JSONArray().put(
                    JSONObject().put("role", "user").put("content", "ping")
                ))
                .toString()
            val req = Request.Builder()
                .url(ENDPOINT)
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody(json))
                .build()
            NetworkModule.okHttp.newCall(req).execute().use { resp ->
                if (resp.isSuccessful) "Claude connection OK"
                else throw IllegalStateException("HTTP ${resp.code}: ${resp.body?.string()?.take(160)}")
            }
        }
    }

    companion object {
        private const val ENDPOINT = "https://api.anthropic.com/v1/messages"
        private const val MODEL = "claude-3-5-sonnet-latest"
    }
}
