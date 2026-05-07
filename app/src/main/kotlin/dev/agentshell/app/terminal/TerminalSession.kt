package dev.agentshell.app.terminal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.UUID

class TerminalSession(
    private val workingDir: File,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    private val _outputFlow = MutableSharedFlow<String>(replay = 100, extraBufferCapacity = 1000)
    val outputFlow: SharedFlow<String> = _outputFlow.asSharedFlow()

    private var process: Process? = null
    private var writer: BufferedWriter? = null

    init {
        startShell()
    }

    private fun startShell() {
        try {
            process = ProcessBuilder()
                .command("sh")
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()

            writer = BufferedWriter(OutputStreamWriter(process!!.outputStream))
            val reader = BufferedReader(InputStreamReader(process!!.inputStream))

            scope.launch {
                try {
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        _outputFlow.emit(line ?: "")
                    }
                } catch (e: Exception) {
                    _outputFlow.emit("[Shell died: ${e.message}]")
                }
                _outputFlow.emit("[Session ended]")
            }

            // Setup basic environment
            scope.launch {
                writer?.write("export PATH=\$PATH:/system/bin:/system/xbin\n")
                writer?.write("alias pkg='echo \"pkg is mocked. Please use real Termux for packages.\"'\n")
                writer?.write("alias clear='echo \"\\033[2J\\033[H\"'\n")
                writer?.flush()
            }
        } catch (e: Exception) {
            scope.launch { _outputFlow.emit("[Error starting shell: ${e.message}]") }
        }
    }

    fun executeCommand(command: String): Flow<String> = flow {
        val cmdId = UUID.randomUUID().toString().take(8)
        val endMarker = "__END_CMD_${cmdId}__"
        
        if (!command.startsWith("export ") && !command.startsWith("alias ")) {
            emit("> $command")
            _outputFlow.emit("> $command")
        }
        
        try {
            writer?.apply {
                write("$command\n")
                write("echo $endMarker\n")
                flush()
            }
            
            // Collect from the shared flow until we see the marker
            _outputFlow.collect { line ->
                if (line.trim() == endMarker) {
                    throw kotlinx.coroutines.CancellationException("Command Finished")
                } else if (line.isNotEmpty() && !line.startsWith("> ") && !line.startsWith("echo __END_CMD_")) {
                    emit(line)
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            // Normal termination of command
        } catch (e: Exception) {
            emit("[Error executing command: ${e.message}]")
            _outputFlow.emit("[Error: ${e.message}]")
            if (process?.isAlive == false) startShell()
        }
    }
}
