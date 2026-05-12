package dev.agentshell.app.chat

import java.util.UUID

/**
 * Role of a chat message — drives color and styling in the UI.
 */
enum class MessageRole {
    USER,        // Amber text — user typed this
    AGENT,       // Cream text — LLM final response
    STREAMING,   // Amber dimmed — tokens streaming in live
    TOOL_CALL,   // Muted green — agent is invoking a tool
    TOOL_OUTPUT, // Sand text — output from tool execution
    SYSTEM,      // Muted blue — system/status messages
    ERROR        // Muted red — errors from agent or tools
}

/**
 * Immutable chat message model. Each message has a stable ID so the
 * LazyColumn can efficiently diff the list without re-rendering.
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    /** Which agent step produced this message (for tool_call / tool_output). */
    val stepIndex: Int? = null
)

/** MVI State for the Chat screen. */
data class ChatState(
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            role = MessageRole.SYSTEM,
            content = "[agentShell] Ready. Type a task or tap the mic."
        )
    ),
    val currentInput: String = "",
    val isAgentRunning: Boolean = false,
    val streamingToken: String = "",
    /** True while SpeechRecognizer is actively listening. */
    val isListening: Boolean = false,
    /** Partial speech recognition text shown live while user speaks. */
    val liveVoiceText: String = ""
)

/** MVI Intents — all user actions on the Chat screen. */
sealed class ChatIntent {
    data class InputChanged(val text: String) : ChatIntent()
    object SubmitTask       : ChatIntent()
    object ClearChat        : ChatIntent()
    /** Start a brand-new chat session, clearing all messages. */
    object NewSession       : ChatIntent()
    /** User pressed and held the mic button — start listening. */
    object StartVoiceInput  : ChatIntent()
    /** User released the mic button — stop listening and submit. */
    object StopVoiceInput   : ChatIntent()
}
