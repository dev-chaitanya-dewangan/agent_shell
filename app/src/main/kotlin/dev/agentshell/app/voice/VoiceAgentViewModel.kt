package dev.agentshell.app.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agentshell.app.agent.AgentLoopManager
import dev.agentshell.app.agent.AgentState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VoiceAgentViewModel @Inject constructor(
    private val voiceInputManager: VoiceInputManager,
    private val agentLoopManager: AgentLoopManager,
    private val ttsManager: TextToSpeechManager
) : ViewModel() {

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _spokenText = MutableStateFlow("")
    val spokenText: StateFlow<String> = _spokenText.asStateFlow()

    val agentState: StateFlow<AgentState> = agentLoopManager.agentState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AgentState.Idle)

    fun startListening() {
        if (_isListening.value) return
        _isListening.value = true
        _spokenText.value = ""

        viewModelScope.launch {
            voiceInputManager.listen().collect { result ->
                when (result) {
                    is VoiceInputManager.VoiceResult.Partial -> {
                        _spokenText.value = result.text
                    }
                    is VoiceInputManager.VoiceResult.Final -> {
                        _spokenText.value = result.text
                        _isListening.value = false
                        runAgentTask(result.text)
                    }
                    is VoiceInputManager.VoiceResult.Error -> {
                        _spokenText.value = result.message
                        _isListening.value = false
                    }
                }
            }
        }
    }

    private fun runAgentTask(task: String) {
        viewModelScope.launch {
            val result = agentLoopManager.run(task)
            when (result) {
                is dev.agentshell.app.agent.AgentResult.Success -> {
                    ttsManager.speak(result.message)
                }
                is dev.agentshell.app.agent.AgentResult.Failure -> {
                    ttsManager.speak("Error: ${result.reason}")
                }
                is dev.agentshell.app.agent.AgentResult.MaxDepthReached -> {
                    ttsManager.speak("I reached the maximum number of steps without completing the task.")
                }
            }
        }
    }
}
