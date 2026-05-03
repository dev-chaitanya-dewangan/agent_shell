package dev.agentshell.app.agent

import dev.agentshell.app.terminal.TerminalSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class ToolResult {
    data class Success(val message: String) : ToolResult()
    data class Failure(val reason: String) : ToolResult()
}

class ToolDispatcher(private val terminalSession: TerminalSession) {
    
    fun dispatch(toolName: String, params: Map<String, String>): Flow<String> {
        return when (toolName) {
            "run_shell" -> {
                val cmd = params["command"] ?: "echo 'No command provided'"
                terminalSession.executeCommand(cmd)
            }
            "write_file" -> {
                val path = params["path"] ?: "error.txt"
                val content = params["content"] ?: ""
                // Simple escaping for echo, better approach is a file write API directly
                terminalSession.executeCommand("cat << 'EOF' > $path\n$content\nEOF")
            }
            else -> {
                flow { emit("[Error: Unknown tool $toolName]") }
            }
        }
    }
}
