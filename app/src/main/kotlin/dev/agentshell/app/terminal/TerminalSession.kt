package dev.agentshell.app.terminal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

class TerminalSession(private val workingDir: File) {

    fun executeCommand(command: String): Flow<String> = flow {
        try {
            // Echo the command back like a real terminal
            emit("> $command")
            
            val process = ProcessBuilder()
                .command("sh", "-c", command)
                .directory(workingDir)
                .redirectErrorStream(true)
                .start()

            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                emit(line ?: "")
            }
            
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                 emit("[Process completed with exit code $exitCode]")
            }
        } catch (e: Exception) {
            emit("[Error executing command: ${e.message}]")
        }
    }.flowOn(Dispatchers.IO)
}
