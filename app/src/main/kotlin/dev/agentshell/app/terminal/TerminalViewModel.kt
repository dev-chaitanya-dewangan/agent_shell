package dev.agentshell.app.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.agentshell.app.agent.TermuxBridgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalState(
    val outputLog: List<String> = listOf("Welcome to agentShell Terminal // Termux Background Bridge"),
    val currentInput: String = "",
    val isExecuting: Boolean = false
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val termuxBridge: TermuxBridgeRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    fun onInputChanged(input: String) {
        _state.update { it.copy(currentInput = input) }
    }

    fun submitCommand() {
        val command = _state.value.currentInput.trim()
        if (command.isEmpty()) return

        _state.update { 
            val newLog = it.outputLog.toMutableList().apply { 
                add("> $command") 
            }
            it.copy(
                currentInput = "", 
                isExecuting = true,
                outputLog = newLog
            ) 
        }

        viewModelScope.launch {
            val result = termuxBridge.executeInTermux(command)
            
            _state.update { currentState ->
                val newLog = currentState.outputLog.toMutableList()
                newLog.addAll(result.split("\n"))
                
                // Keep the log length manageable
                if (newLog.size > 1000) {
                    newLog.subList(0, newLog.size - 1000).clear()
                }
                
                currentState.copy(
                    isExecuting = false,
                    outputLog = newLog
                )
            }
        }
    }
    
    fun clearTerminal() {
        _state.update { it.copy(outputLog = emptyList()) }
    }
}
