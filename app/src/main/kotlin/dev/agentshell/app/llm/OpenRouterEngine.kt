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

class OpenRouterEngine(private val apiKey: String) : LLMEngine {

    override val providerType = ProviderType.OPENROUTER
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
        // Fallback simple HTTP implementation since OkHttp isn't fully configured yet
        try {
            val url = URL("https://openrouter.ai/api/v1/chat/completions")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $apiKey")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonBody = JSONObject().apply {
                put("model", "minimax/minimax-m1:free")
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
                put("max_tokens", maxTokens)
                put("temperature", temperature)
                put("stream", true) // Ensure streaming
            }

            conn.outputStream.write(jsonBody.toString().toByteArray())

            BufferedReader(InputStreamReader(conn.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("data: ") && line != "data: [DONE]") {
                        val data = line!!.substring(6)
                        try {
                            val chunk = JSONObject(data)
                            val content = chunk.getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("delta")
                                .optString("content", "")
                            if (content.isNotEmpty()) {
                                emit(content)
                            }
                        } catch (e: Exception) {
                            // ignore malformed chunks
                        }
                    }
                }
            }
        } catch (e: Exception) {
            emit("[LLM Error: ${e.message}]")
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun complete(prompt: String, maxTokens: Int): String {
        return "Complete not fully implemented in mock"
    }

    override fun countTokens(text: String): Int {
        // Fast approximation
        return text.length / 4
    }

    override suspend fun ping(): PingResult {
        return if (isReady) PingResult.Success(100L) else PingResult.Failure("No API Key")
    }
}
