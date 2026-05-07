package dev.agentshell.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.agentshell.app.llm.ProviderType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        val KEY_PROVIDER = stringPreferencesKey("provider_type")
        val KEY_OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_OPENROUTER_MODEL = stringPreferencesKey("openrouter_model")
        val KEY_GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val KEY_OPENROUTER_CUSTOM_MODELS = stringPreferencesKey("openrouter_custom_models")
        val KEY_GEMINI_CUSTOM_MODELS = stringPreferencesKey("gemini_custom_models")
        
        const val DEFAULT_GEMINI_MODEL = "gemini-1.5-flash-preview-0514"
        const val DEFAULT_OPENROUTER_MODEL = "google/gemini-flash-1.5-exp"
    }

    val providerTypeFlow: Flow<ProviderType> = dataStore.data.map { prefs ->
        val name = prefs[KEY_PROVIDER] ?: ProviderType.GOOGLE_GEMINI.name
        try {
            ProviderType.valueOf(name)
        } catch (e: Exception) {
            ProviderType.GOOGLE_GEMINI
        }
    }

    val openRouterApiKeyFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_OPENROUTER_API_KEY] ?: ""
    }

    val geminiApiKeyFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_GEMINI_API_KEY] ?: ""
    }

    val openRouterModelFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_OPENROUTER_MODEL] ?: DEFAULT_OPENROUTER_MODEL
    }

    val geminiModelFlow: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_GEMINI_MODEL] ?: DEFAULT_GEMINI_MODEL
    }

    val openRouterCustomModelsFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        val csv = prefs[KEY_OPENROUTER_CUSTOM_MODELS] ?: ""
        if (csv.isBlank()) emptyList() else csv.split(",").map { it.trim() }
    }

    val geminiCustomModelsFlow: Flow<List<String>> = dataStore.data.map { prefs ->
        val csv = prefs[KEY_GEMINI_CUSTOM_MODELS] ?: ""
        if (csv.isBlank()) emptyList() else csv.split(",").map { it.trim() }
    }

    suspend fun setProviderType(provider: ProviderType) {
        dataStore.edit { prefs ->
            prefs[KEY_PROVIDER] = provider.name
        }
    }

    suspend fun setOpenRouterApiKey(key: String) {
        dataStore.edit { prefs ->
            prefs[KEY_OPENROUTER_API_KEY] = key
        }
    }

    suspend fun setGeminiApiKey(key: String) {
        dataStore.edit { prefs ->
            prefs[KEY_GEMINI_API_KEY] = key
        }
    }

    suspend fun setOpenRouterModel(model: String) {
        dataStore.edit { prefs ->
            prefs[KEY_OPENROUTER_MODEL] = model
        }
    }

    suspend fun setGeminiModel(model: String) {
        dataStore.edit { prefs ->
            prefs[KEY_GEMINI_MODEL] = model
        }
    }

    suspend fun addOpenRouterCustomModels(modelsCsv: String) {
        val newModels = modelsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        dataStore.edit { prefs ->
            val existing = prefs[KEY_OPENROUTER_CUSTOM_MODELS] ?: ""
            val currentList = if (existing.isBlank()) emptyList() else existing.split(",").map { it.trim() }
            val combined = (currentList + newModels).distinct()
            prefs[KEY_OPENROUTER_CUSTOM_MODELS] = combined.joinToString(",")
        }
    }

    suspend fun addGeminiCustomModels(modelsCsv: String) {
        val newModels = modelsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        dataStore.edit { prefs ->
            val existing = prefs[KEY_GEMINI_CUSTOM_MODELS] ?: ""
            val currentList = if (existing.isBlank()) emptyList() else existing.split(",").map { it.trim() }
            val combined = (currentList + newModels).distinct()
            prefs[KEY_GEMINI_CUSTOM_MODELS] = combined.joinToString(",")
        }
    }
}
