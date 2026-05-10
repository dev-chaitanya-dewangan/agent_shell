package dev.agentshell.app.agent

import dev.agentshell.app.accessibility.AgentAccessibilityService
import dev.agentshell.app.terminal.TerminalSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject

class ToolDispatcher @Inject constructor(
    private val terminalSession: TerminalSession,
    private val termuxBridge: TermuxBridgeRepository
) {
    fun dispatch(toolName: String, params: Map<String, String>): Flow<String> = flow {
        try {
            when (toolName) {
                "run_shell" -> {
                    val cmd = params["command"] ?: "echo 'No command provided'"
                    terminalSession.executeCommand(cmd).collect { emit(it) }
                }
                "run_termux" -> {
                    val cmd = params["command"] ?: return@flow emit("[Error: No command]")
                    val result = termuxBridge.executeInTermux(cmd)
                    emit(result)
                }
                "write_file" -> {
                    val path = params["path"] ?: return@flow emit("[Error: No path]")
                    val content = params["content"] ?: ""
                    File(path).writeText(content)
                    emit("[File written: $path]")
                }
                "read_file" -> {
                    val path = params["path"] ?: return@flow emit("[Error: No path]")
                    val file = File(path)
                    if (file.exists()) emit(file.readText()) else emit("[Error: File not found]")
                }
                "list_dir" -> {
                    val path = params["path"] ?: return@flow emit("[Error: No path]")
                    val dir = File(path)
                    if (dir.exists() && dir.isDirectory) {
                        emit(dir.listFiles()?.joinToString("\n") { it.name } ?: "[Empty directory]")
                    } else {
                        emit("[Error: Directory not found]")
                    }
                }
                "ui_tap" -> {
                    val x = params["x"]?.toFloatOrNull() ?: return@flow emit("[Error: Invalid x]")
                    val y = params["y"]?.toFloatOrNull() ?: return@flow emit("[Error: Invalid y]")
                    AgentAccessibilityService.instance?.tapAt(x, y)
                        ?: return@flow emit("[Error: Accessibility Service not active]")
                    emit("[Tapped at $x, $y]")
                }
                "ui_type" -> {
                    val text = params["text"] ?: return@flow emit("[Error: No text]")
                    AgentAccessibilityService.instance?.typeText(text)
                        ?: return@flow emit("[Error: Accessibility Service not active]")
                    emit("[Typed text]")
                }
                "ui_find_and_tap" -> {
                    val text = params["text"] ?: return@flow emit("[Error: No text]")
                    val success = AgentAccessibilityService.instance?.findAndTap(text) ?: false
                    if (success) emit("[Tapped element with text: $text]")
                    else emit("[Error: Element not found or Service not active]")
                }
                "ui_get_screen" -> {
                    val tree = AgentAccessibilityService.instance?.getScreenTree()
                        ?: return@flow emit("[Error: Accessibility Service not active]")
                    emit(tree)
                }
                "open_app" -> {
                    // TODO: Intent to launch app by package
                    emit("[App opened]")
                }
                "create_mini_app" -> {
                    // TODO: Database insertion
                    emit("[Mini app created]")
                }
                "take_screenshot" -> {
                    emit("[Screenshot taken]")
                }
                else -> {
                    emit("[Error: Unknown tool $toolName]")
                }
            }
        } catch (e: Exception) {
            emit("[Tool Execution Error: ${e.message}]")
        }
    }
}
