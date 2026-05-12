package dev.agentshell.app.terminal

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.agentshell.app.agent.TermuxBridgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class TerminalState(
    val outputLog: List<String> = emptyList(),
    val currentInput: String = "",
    val isExecuting: Boolean = false,
    /** true when MANAGE_EXTERNAL_STORAGE is not granted — UI shows a banner */
    val needsStoragePermission: Boolean = false
)

@HiltViewModel
class TerminalViewModel @Inject constructor(
    private val termuxBridge: TermuxBridgeRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(TerminalState())
    val state: StateFlow<TerminalState> = _state.asStateFlow()

    init {
        runBridgeSelfTest()
    }

    /** Check storage permission and verify the bridge dir is writable. */
    private fun runBridgeSelfTest() {
        viewModelScope.launch {
            val log = mutableListOf<String>()
            log += "Welcome to agentShell Terminal // Termux Background Bridge"
            log += "─────────────────────────────────────────────────"

            // 1. Check MANAGE_EXTERNAL_STORAGE (Android 11+ requires runtime grant)
            val hasStoragePerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                true // Below Android 11 the manifest declaration is enough
            }

            if (!hasStoragePerm) {
                log += "[PERM] ⚠ MANAGE_EXTERNAL_STORAGE not granted"
                log += "[PERM] Tap 'GRANT STORAGE' button above to fix this"
                _state.update { it.copy(outputLog = log, needsStoragePermission = true) }
                return@launch
            }

            // 2. Try to create and write to the bridge dir
            val bridgeDir = File("/sdcard/Download/agentshell/bridge")
            try {
                bridgeDir.mkdirs()
                val testFile = File(bridgeDir, ".selftest")
                testFile.writeText("ok")
                testFile.delete()
                log += "[OK] Bridge dir writable: ${bridgeDir.absolutePath}"
            } catch (e: Exception) {
                log += "[ERROR] Bridge dir not writable: ${e.message}"
                log += "[FIX] In Termux, run once: termux-setup-storage"
                log += "[FIX] Then reopen agentShell"
                _state.update { it.copy(outputLog = log, needsStoragePermission = false) }
                return@launch
            }

            log += "[OK] Storage permission: granted"
            log += "[INFO] Start watcher in Termux:"
            log += "  bash /sdcard/Download/agentshell/bridge/bridge_watcher.sh"
            log += "─────────────────────────────────────────────────"

            _state.update { it.copy(outputLog = log, needsStoragePermission = false) }
        }
    }

    fun onInputChanged(input: String) {
        _state.update { it.copy(currentInput = input) }
    }

    fun submitCommand() {
        val command = _state.value.currentInput.trim()
        if (command.isEmpty()) return

        _state.update {
            val newLog = it.outputLog.toMutableList().apply { add("> $command") }
            it.copy(currentInput = "", isExecuting = true, outputLog = newLog)
        }

        viewModelScope.launch {
            val result = termuxBridge.executeInTermux(command)

            _state.update { currentState ->
                val newLog = currentState.outputLog.toMutableList()
                newLog.addAll(result.split("\n"))
                if (newLog.size > 1000) newLog.subList(0, newLog.size - 1000).clear()
                currentState.copy(isExecuting = false, outputLog = newLog)
            }
        }
    }

    fun clearTerminal() {
        _state.update { it.copy(outputLog = emptyList()) }
    }
}
