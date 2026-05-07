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

            reader.use { buf ->
                var line: String?
                // The Gemini streamGenerateContent API returns a JSON array of response chunks.
                // We parse it as a sequence of objects since the format is typically:
                // [
                //   {...},
                //   {...}
                // ]
                // It sends chunks progressively. We can use a simple parser or just accumulate and parse.
                // Since HttpURLConnection might buffer, let's just parse JSON manually if we can,
                // or read lines. Gemini's stream often puts "text": "..." inside parts.
                
                // For simplicity in a mock-like direct HTTP implementation, we can just look for "text": "..."
                while (buf.readLine().also { line = it } != null) {
                    val l = line!!.trim()
                    if (l.startsWith("\"text\": \"")) {
                        // Extract text using simple substring
                        val textPart = l.substringAfter("\"text\": \"").substringBeforeLast("\"")
                        // Unescape newlines and quotes
                        val unescaped = textPart.replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
                        emit(unescaped)
                    }
                }
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
