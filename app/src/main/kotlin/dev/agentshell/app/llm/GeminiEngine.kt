package dev.agentshell.app.llm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Gemini REST engine.
 *
 * Free-tier stable models (as of 2025):
 *   gemini-2.0-flash          — fast, free quota, recommended default
 *   gemini-2.0-flash-lite     — lightest / fastest
 *   gemini-1.5-flash          — stable, generous free quota
 *   gemini-1.5-flash-8b       — small 8B variant
 *   gemini-1.5-pro            — highest quality, limited free quota
 *
 * Preview / experimental (may be removed without notice — avoid for prod):
 *   gemini-2.5-flash-preview-04-17
 *   gemini-2.5-pro-preview-05-06
 */

class GeminiEngine(
    private val apiKey: String,
    private val model: String
) : LLMEngine {

    override val providerType = ProviderType.GOOGLE_GEMINI
    override val isReady: Boolean = apiKey.isNotEmpty()
    override val statusFlow = MutableStateFlow<EngineStatus>(
        if (isReady) EngineStatus.Ready else EngineStatus.Error("Missing API Key")
    )

    override fun generate(
        prompt: String,
        systemPrompt: String,
        maxTokens: Int,
        temperature: Float
    ): Flow<String> = flow {
        try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:streamGenerateContent?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                if (systemPrompt.isNotBlank()) {
                    put("systemInstruction", JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", systemPrompt)
                            })
                        })
                    })
                }
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                put("generationConfig", JSONObject().apply {
                    put("maxOutputTokens", maxTokens)
                    put("temperature", temperature)
                })
            }

            conn.outputStream.write(jsonBody.toString().toByteArray())

            val reader = if (conn.responseCode in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream))
            } else {
                BufferedReader(InputStreamReader(conn.errorStream))
            }

            // Accumulate full body then parse — HttpURLConnection buffers anyway
            val body = reader.use { it.readText() }

            if (conn.responseCode !in 200..299) {
                emit("[LLM Error ${conn.responseCode}]: $body")
                return@flow
            }

            // Gemini streamGenerateContent returns a JSON array:
            // [{"candidates":[{"content":{"parts":[{"text":"..."}]}}]}, ...]
            // We accumulate and emit each text chunk in order.
            try {
                val chunks = JSONArray(body)
                for (i in 0 until chunks.length()) {
                    val chunk = chunks.optJSONObject(i) ?: continue
                    val candidates = chunk.optJSONArray("candidates") ?: continue
                    for (ci in 0 until candidates.length()) {
                        val content = candidates.optJSONObject(ci)
                            ?.optJSONObject("content") ?: continue
                        val parts = content.optJSONArray("parts") ?: continue
                        for (pi in 0 until parts.length()) {
                            val text = parts.optJSONObject(pi)?.optString("text") ?: continue
                            if (text.isNotEmpty()) emit(text)
                        }
                    }
                }
            } catch (jsonEx: Exception) {
                // Fallback: raw body so user sees something useful
                emit("[Parse Error: ${jsonEx.message}] Raw: ${body.take(300)}")
            }
        } catch (e: Exception) {
            emit("[LLM Error: ${e.message}]")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun complete(prompt: String, maxTokens: Int): String {
        return "Complete not fully implemented"
    }

    override fun countTokens(text: String): Int {
        return text.length / 4 // Fast approximation
    }

    override suspend fun ping(): PingResult {
        return if (isReady) PingResult.Success(100L) else PingResult.Failure("No API Key")
    }
}
