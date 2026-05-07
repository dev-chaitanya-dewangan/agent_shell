package dev.agentshell.app.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agentshell.app.agent.AgentLoopManager
import dev.agentshell.app.agent.AgentResult
import dev.agentshell.app.agent.AgentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agentLoopManager: AgentLoopManager
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        // Observe agent state transitions and map them to chat messages
        viewModelScope.launch {
            agentLoopManager.agentState.collect { agentState ->
                when (agentState) {
                    is AgentState.Streaming -> {
                        _state.update { it.copy(streamingToken = agentState.partial) }
                    }
                    is AgentState.Acting -> {
                        val msg = ChatMessage(
                            role = MessageRole.TOOL_CALL,
                            content = "[TOOL] Calling: ${agentState.tool} (step ${agentState.step})",
                            stepIndex = agentState.step
                        )
                        _state.update { s ->
                            s.copy(messages = s.messages + msg, streamingToken = "")
                        }
                    }
                    is AgentState.Reflecting -> {
                        _state.update { it.copy(streamingToken = "") }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun onIntent(intent: ChatIntent) {
        when (intent) {
            is ChatIntent.InputChanged -> _state.update { it.copy(currentInput = intent.text) }
            is ChatIntent.SubmitTask   -> submitTask()
            is ChatIntent.ClearChat   -> _state.update { ChatState() }
        }
    }

    private fun submitTask() {
        val task = _state.value.currentInput.trim()
        if (task.isEmpty() || _state.value.isAgentRunning) return

        // Add user message immediately
        val userMsg = ChatMessage(role = MessageRole.USER, content = task)
        _state.update {
            it.copy(
                messages = it.messages + userMsg,
                currentInput = "",
                isAgentRunning = true,
                streamingToken = ""
            )
        }

        viewModelScope.launch {
            val result = agentLoopManager.run(task)

            val agentMsg = when (result) {
                is AgentResult.Success -> ChatMessage(
                    role = MessageRole.AGENT,
                    content = result.message.ifBlank { "[Agent completed in ${result.stepsExecuted} step(s)]" }
                )
                is AgentResult.MaxDepthReached -> ChatMessage(
                    role = MessageRole.SYSTEM,
                    content = "[Agent reached max depth (${result.stepsExecuted} steps). Task may be incomplete.]"
                )
                is AgentResult.Failure -> ChatMessage(
                    role = MessageRole.ERROR,
                    content = "[Error] ${result.reason}"
                )
            }

            _state.update { s ->
                s.copy(
                    messages = s.messages + agentMsg,
                    isAgentRunning = false,
                    streamingToken = ""
                )
            }
        }
    }
}
