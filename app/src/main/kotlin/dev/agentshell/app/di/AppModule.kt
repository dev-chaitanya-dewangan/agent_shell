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
import dev.agentshell.app.data.db.ChatMessageDao
import dev.agentshell.app.data.db.ChatSessionDao
import dev.agentshell.app.llm.LLMEngine
import dev.agentshell.app.llm.OpenRouterEngine
import dev.agentshell.app.terminal.TerminalSession
import java.io.File
import javax.inject.Singleton

/**
 * Root Hilt module.
 *
 * Provides all singletons across the app lifetime.
 * New dependencies should be added here with @Provides @Singleton.
 *
 * HOW TO ADD API KEY (for developers):
 *   1. Create `local.properties` in project root (already in .gitignore)
 *   2. Add: OPENROUTER_API_KEY=sk-or-v1-your-key-here
 *   3. Rebuild to regenerate BuildConfig
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // ─── LLM Layer ───────────────────────────────────────────────────────────

    @Provides
    @Singleton
    fun provideLLMEngine(): LLMEngine = OpenRouterEngine(BuildConfig.OPENROUTER_API_KEY)

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
