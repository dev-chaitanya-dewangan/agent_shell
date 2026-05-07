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
import dev.agentshell.app.data.db.AppDatabase
import androidx.datastore.core.DataStore
import dev.agentshell.app.data.db.ChatMessageDao
import dev.agentshell.app.data.db.ChatSessionDao
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import dev.agentshell.app.data.settings.SettingsRepository
import dev.agentshell.app.llm.DynamicLLMEngine
import dev.agentshell.app.llm.LLMEngine
import dev.agentshell.app.terminal.TerminalSession
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

    // ─── Terminal Layer ───────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideTerminalSession(@ApplicationContext context: Context): TerminalSession {
        val workingDir = File(context.filesDir, "home").apply { mkdirs() }
        return TerminalSession(workingDir)
    }

    // ─── Agent Layer ──────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideToolDispatcher(terminalSession: TerminalSession): ToolDispatcher =
        ToolDispatcher(terminalSession)

    @Provides
    @Singleton
    fun provideAgentLoopManager(
        llmEngine: LLMEngine,
        toolDispatcher: ToolDispatcher
    ): AgentLoopManager = AgentLoopManager(llmEngine, toolDispatcher)

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
}
