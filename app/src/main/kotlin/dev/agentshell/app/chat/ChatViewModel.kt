package dev.agentshell.app.chat

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.agentshell.app.agent.AgentLoopManager
import dev.agentshell.app.agent.AgentResult
import dev.agentshell.app.agent.AgentState
import dev.agentshell.app.voice.VoiceInputManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agentLoopManager: AgentLoopManager,
    private val voiceInputManager: VoiceInputManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    private var voiceJob: Job? = null

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
                            content = "▶ ${agentState.tool} (step ${agentState.step})",
                            stepIndex = agentState.step
                        )
                        _state.update { s -> s.copy(messages = s.messages + msg, streamingToken = "") }
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
            is ChatIntent.InputChanged   -> _state.update { it.copy(currentInput = intent.text) }
            is ChatIntent.SubmitTask     -> submitTask()
            is ChatIntent.ClearChat      -> _state.update { ChatState() }
            is ChatIntent.NewSession     -> _state.update { ChatState() }
            is ChatIntent.StartVoiceInput -> startVoiceInput()
            is ChatIntent.StopVoiceInput  -> stopVoiceInput()
        }
    }

    private fun startVoiceInput() {
        // Guard: check RECORD_AUDIO permission
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            addSystemMessage("[Voice] RECORD_AUDIO permission not granted. Enable in Settings.")
            return
        }
        if (_state.value.isListening) return  // Already listening

        _state.update { it.copy(isListening = true, liveVoiceText = "") }

        voiceJob = viewModelScope.launch {
            voiceInputManager.listen().collect { result ->
                when (result) {
                    is VoiceInputManager.VoiceResult.Partial -> {
                        _state.update { it.copy(liveVoiceText = result.text, currentInput = result.text) }
                    }
                    is VoiceInputManager.VoiceResult.Final -> {
                        _state.update {
                            it.copy(
                                isListening = false,
                                liveVoiceText = "",
                                currentInput = result.text
                            )
                        }
                        // Auto-submit when voice recognition finishes
                        if (result.text.isNotBlank()) submitTask()
                    }
                    is VoiceInputManager.VoiceResult.Error -> {
                        _state.update { it.copy(isListening = false, liveVoiceText = "") }
                        addSystemMessage("[Voice Error] ${result.message}")
                    }
                }
            }
        }
    }

    private fun stopVoiceInput() {
        voiceJob?.cancel()
        voiceJob = null
        _state.update { it.copy(isListening = false, liveVoiceText = "") }
    }

    private fun submitTask() {
        val task = _state.value.currentInput.trim()
        if (task.isEmpty() || _state.value.isAgentRunning) return

        val userMsg = ChatMessage(role = MessageRole.USER, content = task)
        _state.update {
            it.copy(
                messages = it.messages + userMsg,
                currentInput = "",
                isAgentRunning = true,
                streamingToken = "",
                isListening = false,
                liveVoiceText = ""
            )
        }

        viewModelScope.launch {
            val result = agentLoopManager.run(task)

            val agentMsg = when (result) {
                is AgentResult.Success ->
                    ChatMessage(
                        role = MessageRole.AGENT,
                        content = result.message.ifBlank { "✓ Done in ${result.stepsExecuted} step(s)" }
                    )
                is AgentResult.MaxDepthReached ->
                    ChatMessage(
                        role = MessageRole.SYSTEM,
                        content = "[Max steps reached (${result.stepsExecuted}). Task may be incomplete.]"
                    )
                is AgentResult.Failure ->
                    ChatMessage(role = MessageRole.ERROR, content = "[Error] ${result.reason}")
            }

            _state.update { s ->
                s.copy(messages = s.messages + agentMsg, isAgentRunning = false, streamingToken = "")
            }
        }
    }

    private fun addSystemMessage(text: String) {
        _state.update { s ->
            s.copy(messages = s.messages + ChatMessage(role = MessageRole.SYSTEM, content = text))
        }
    }
}
