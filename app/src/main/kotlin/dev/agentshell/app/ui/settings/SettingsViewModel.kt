package dev.agentshell.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agentshell.app.data.settings.SettingsRepository
import dev.agentshell.app.llm.ProviderType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsState(
    val provider: ProviderType = ProviderType.GOOGLE_GEMINI,
    val openRouterKey: String = "",
    val geminiKey: String = "",
    val openRouterModel: String = SettingsRepository.DEFAULT_OPENROUTER_MODEL,
    val geminiModel: String = SettingsRepository.DEFAULT_GEMINI_MODEL,
    val openRouterCustomModels: List<String> = emptyList(),
    val geminiCustomModels: List<String> = emptyList(),
    val isSaving: Boolean = false
) {
    val activeKey: String
        get() = if (provider == ProviderType.OPENROUTER) openRouterKey else geminiKey

    val availableModels: List<String>
        get() = if (provider == ProviderType.OPENROUTER) {
            listOf(SettingsRepository.DEFAULT_OPENROUTER_MODEL) + openRouterCustomModels
        } else {
            listOf(SettingsRepository.DEFAULT_GEMINI_MODEL) + geminiCustomModels
        }
    
    val selectedModel: String
        get() = if (provider == ProviderType.OPENROUTER) openRouterModel else geminiModel
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        val flow1 = combine(
            repository.providerTypeFlow,
            repository.openRouterApiKeyFlow,
            repository.geminiApiKeyFlow,
            repository.openRouterModelFlow
        ) { p1, p2, p3, p4 ->
            listOf(p1, p2, p3, p4)
        }
        
        val flow2 = combine(
            repository.geminiModelFlow,
            repository.openRouterCustomModelsFlow,
            repository.geminiCustomModelsFlow
        ) { p5, p6, p7 ->
            listOf(p5, p6, p7)
        }

        combine(flow1, flow2) { f1, f2 ->
            val provider = f1[0] as ProviderType
            val orKey = f1[1] as String
            val geminiKey = f1[2] as String
            val orModel = f1[3] as String
            val geminiModel = f2[0] as String
            @Suppress("UNCHECKED_CAST")
            val orCustom = f2[1] as List<String>
            @Suppress("UNCHECKED_CAST")
            val geminiCustom = f2[2] as List<String>

            _state.value = _state.value.copy(
                provider = provider,
                openRouterKey = orKey,
                geminiKey = geminiKey,
                openRouterModel = orModel,
                geminiModel = geminiModel,
                openRouterCustomModels = orCustom,
                geminiCustomModels = geminiCustom
            )
        }.launchIn(viewModelScope)
    }

    fun setProvider(provider: ProviderType) {
        viewModelScope.launch {
            repository.setProviderType(provider)
        }
    }

    fun updateApiKey(key: String) {
        viewModelScope.launch {
            if (_state.value.provider == ProviderType.OPENROUTER) {
                repository.setOpenRouterApiKey(key)
            } else {
                repository.setGeminiApiKey(key)
            }
        }
    }

    fun setModel(model: String) {
        viewModelScope.launch {
            if (_state.value.provider == ProviderType.OPENROUTER) {
                repository.setOpenRouterModel(model)
            } else {
                repository.setGeminiModel(model)
            }
        }
    }

    fun addCustomModels(csv: String) {
        viewModelScope.launch {
            if (_state.value.provider == ProviderType.OPENROUTER) {
                repository.addOpenRouterCustomModels(csv)
            } else {
                repository.addGeminiCustomModels(csv)
            }
        }
    }
}
