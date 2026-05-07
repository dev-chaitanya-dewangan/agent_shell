package dev.agentshell.app.llm

import dev.agentshell.app.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class DynamicLLMEngine(
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) : LLMEngine {

    private var activeEngine: LLMEngine = GeminiEngine("", "")
    override val statusFlow = MutableStateFlow<EngineStatus>(EngineStatus.Loading)

    init {
        combine(
            settingsRepository.providerTypeFlow,
            settingsRepository.openRouterApiKeyFlow,
            settingsRepository.geminiApiKeyFlow,
            settingsRepository.openRouterModelFlow,
            settingsRepository.geminiModelFlow
        ) { provider, orKey, geminiKey, orModel, geminiModel ->
            activeEngine = when (provider) {
                ProviderType.OPENROUTER -> OpenRouterEngine(orKey, orModel)
                ProviderType.GOOGLE_GEMINI -> GeminiEngine(geminiKey, geminiModel)
                else -> GeminiEngine(geminiKey, geminiModel)
            }
            statusFlow.value = activeEngine.statusFlow.value
        }.launchIn(scope)
    }

    override val providerType: ProviderType
        get() = activeEngine.providerType

    override val isReady: Boolean
        get() = activeEngine.isReady

    override fun generate(
        prompt: String,
        systemPrompt: String,
        maxTokens: Int,
        temperature: Float
    ): Flow<String> {
        if (!isReady) return flowOf("[Error: API Key missing or Engine not ready. Check Settings.]")
        return activeEngine.generate(prompt, systemPrompt, maxTokens, temperature)
    }

    override suspend fun complete(prompt: String, maxTokens: Int): String {
        return activeEngine.complete(prompt, maxTokens)
    }

    override fun countTokens(text: String): Int {
        return activeEngine.countTokens(text)
    }

    override suspend fun ping(): PingResult {
        return activeEngine.ping()
    }
}
