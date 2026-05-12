package dev.agentshell.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.agentshell.app.BuildConfig
import dev.agentshell.app.agent.AgentLoopManager
import dev.agentshell.app.agent.ToolDispatcher
import dev.agentshell.app.agent.TermuxBridgeRepository
import dev.agentshell.app.brain.BrainLogger
import dev.agentshell.app.brain.HermesContextBuilder
import dev.agentshell.app.data.db.AppDatabase
import androidx.datastore.core.DataStore
import dev.agentshell.app.data.db.ChatMessageDao
import dev.agentshell.app.data.db.ChatSessionDao
import dev.agentshell.app.brain.BrainLogDao
import dev.agentshell.app.miniapp.MiniAppDao
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dev.agentshell.app.data.settings.SettingsRepository
import dev.agentshell.app.llm.DynamicLLMEngine
import dev.agentshell.app.llm.LLMEngine
import dev.agentshell.app.voice.TextToSpeechManager
import dev.agentshell.app.voice.VoiceInputManager
import java.io.File
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ─── DataStore & Settings ────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.dataStore

    @Provides
    @Singleton
    fun provideSettingsRepository(dataStore: DataStore<Preferences>): SettingsRepository =
        SettingsRepository(dataStore)

    // ─── LLM Layer ───────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideLLMEngine(settingsRepository: SettingsRepository): LLMEngine =
        DynamicLLMEngine(settingsRepository)

    // ─── Agent Layer ──────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideToolDispatcher(
        termuxBridge: TermuxBridgeRepository,
        miniAppDao: MiniAppDao,
        @ApplicationContext context: Context
    ): ToolDispatcher = ToolDispatcher(termuxBridge, miniAppDao, context)

    @Provides
    @Singleton
    fun provideAgentLoopManager(
        llmEngine: LLMEngine,
        toolDispatcher: ToolDispatcher,
        hermesContextBuilder: HermesContextBuilder,
        brainLogger: BrainLogger
    ): AgentLoopManager = AgentLoopManager(llmEngine, toolDispatcher, hermesContextBuilder, brainLogger)

    // ─── Database Layer ───────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        AppDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideChatSessionDao(db: AppDatabase): ChatSessionDao = db.chatSessionDao()

    @Provides
    @Singleton
    fun provideChatMessageDao(db: AppDatabase): ChatMessageDao = db.chatMessageDao()

    @Provides
    @Singleton
    fun provideBrainLogDao(db: AppDatabase): BrainLogDao = db.brainLogDao()

    @Provides
    @Singleton
    fun provideMiniAppDao(db: AppDatabase): MiniAppDao = db.miniAppDao()

    // ─── Voice Layer ──────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideVoiceInputManager(@ApplicationContext context: Context): VoiceInputManager =
        VoiceInputManager(context)

    @Provides
    @Singleton
    fun provideTextToSpeechManager(@ApplicationContext context: Context): TextToSpeechManager =
        TextToSpeechManager(context).also { it.initialize() }
}
