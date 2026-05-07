package dev.agentshell.app.terminal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TerminalState(
    val outputLog: List<String> = emptyList(),
    val currentInput: String = "",
    val isExecuting: Boolean = false
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val session: TerminalSession
) : ViewModel() {
    
    private val _state = MutableStateFlow(TerminalState(
        outputLog = listOf("Welcome to agentShell Terminal (Stateful Backend)")
    ))
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    init {
        // Collect continuous shell output
        viewModelScope.launch {
            session.outputFlow.collect { outputLine ->
                _state.update { currentState ->
                    val newLog = currentState.outputLog.toMutableList()
                    newLog.add(outputLine)
                    if (newLog.size > 1000) {
                        newLog.removeAt(0)
                    }
                    currentState.copy(outputLog = newLog)
                }
            }
        }
    }

    fun onInputChanged(input: String) {
        _state.update { it.copy(currentInput = input) }
    }

    fun submitCommand() {
        val command = _state.value.currentInput.trim()
        if (command.isEmpty()) return

        _state.update { 
            it.copy(
                currentInput = "", 
                isExecuting = true
            ) 
        }

        viewModelScope.launch {
            // executeCommand handles writing to process and emitting to outputFlow internally
            session.executeCommand(command).collect {
                // Ignore collection here, because we already collect continuous output in init
                // We just collect it to suspend until command finishes
            }
            _state.update { it.copy(isExecuting = false) }
        }
    }
    
    fun clearTerminal() {
        _state.update { it.copy(outputLog = emptyList()) }
    }
}
