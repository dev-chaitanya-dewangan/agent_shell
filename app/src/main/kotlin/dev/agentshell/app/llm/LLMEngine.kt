package dev.agentshell.app.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

enum class ProviderType {
    LOCAL_GEMMA,
    OPENROUTER,
    GOOGLE_GEMINI,
    SELF_HOSTED_OLLAMA,
    SELF_HOSTED_LMSTUDIO
}

sealed class EngineStatus {
    object Idle : EngineStatus()
    object Loading : EngineStatus()
    object Ready : EngineStatus()
    data class Error(val message: String) : EngineStatus()
}

sealed class PingResult {
    data class Success(val latencyMs: Long) : PingResult()
    data class Failure(val reason: String) : PingResult()
}

interface LLMEngine {
    val providerType: ProviderType
    val isReady: Boolean
    val statusFlow: StateFlow<EngineStatus>

    fun generate(
        prompt: String,
        systemPrompt: String,
        maxTokens: Int = 1024,
        temperature: Float = 0.8f
    ): Flow<String>

    suspend fun complete(prompt: String, maxTokens: Int = 256): String

    fun countTokens(text: String): Int

    suspend fun ping(): PingResult
}
