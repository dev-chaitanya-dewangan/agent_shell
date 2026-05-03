package dev.agentshell.app.terminal

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class TerminalState(
    val outputLog: List<String> = emptyList(),
    val currentInput: String = "",
    val isExecuting: Boolean = false
)

class TerminalViewModel(application: Application) : AndroidViewModel(application) {
    
    // We use the app's filesDir as the default isolated home directory for safety
    private val workingDir = File(application.filesDir, "home").apply {
        if (!exists()) mkdirs()
    }
    
    private val session = TerminalSession(workingDir)
    
    private val _state = MutableStateFlow(TerminalState(
        outputLog = listOf("Welcome to agentShell Terminal (ProcessBuilder Backend)")
    ))
    val state: StateFlow<TerminalState> = _state.asStateFlow()

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
            session.executeCommand(command).collect { outputLine ->
                _state.update { currentState ->
                    val newLog = currentState.outputLog.toMutableList()
                    newLog.add(outputLine)
                    // Keep buffer at 1000 lines for memory safety in 80/20 implementation
                    if (newLog.size > 1000) {
                        newLog.removeAt(0)
                    }
                    currentState.copy(outputLog = newLog)
                }
            }
            
            _state.update { it.copy(isExecuting = false) }
        }
    }
    
    fun clearTerminal() {
        _state.update { it.copy(outputLog = emptyList()) }
    }
}
