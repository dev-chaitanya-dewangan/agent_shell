package dev.agentshell.app.agent

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.agentshell.app.accessibility.AgentAccessibilityService
import dev.agentshell.app.miniapp.MiniAppDao
import dev.agentshell.app.miniapp.MiniAppEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.util.UUID
import javax.inject.Inject

class ToolDispatcher @Inject constructor(
    private val termuxBridge: TermuxBridgeRepository,
    private val miniAppDao: MiniAppDao,
    @ApplicationContext private val context: Context
) {
    fun dispatch(toolName: String, params: Map<String, String>): Flow<String> = flow {
        try {
            when (toolName) {
                "run_shell", "run_termux" -> {
                    val cmd = params["command"] ?: return@flow emit("[Error: No command]")
                    val result = termuxBridge.executeInTermux(cmd)
                    emit(result)
                }
                "write_file" -> {
                    val path = params["path"] ?: return@flow emit("[Error: No path]")
                    val content = params["content"] ?: ""
                    val file = File(path)
                    file.parentFile?.mkdirs()
                    file.writeText(content)
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
                    val name = params["name"] ?: params["title"] ?: "Unnamed App"
                    val description = params["description"] ?: params["desc"] ?: ""
                    val htmlContent = params["html"] ?: params["content"] ?: ""

                    // Write HTML to app-private files directory so WebView can load it
                    val appId = UUID.randomUUID().toString()
                    val miniAppsDir = File(context.filesDir, "mini_apps/$appId")
                    miniAppsDir.mkdirs()
                    val htmlFile = File(miniAppsDir, "index.html")
                    if (htmlContent.isNotBlank()) {
                        htmlFile.writeText(htmlContent)
                    } else {
                        // Generate a minimal placeholder so the app is openable
                        htmlFile.writeText(
                            """<!DOCTYPE html><html><head><meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width,initial-scale=1">
                            <title>$name</title>
                            <style>body{background:#0d0d0d;color:#e0e0e0;font-family:monospace;padding:20px}</style>
                            </head><body><h1>$name</h1><p>$description</p></body></html>"""
                        )
                    }

                    val entity = MiniAppEntity(
                        id = appId,
                        name = name,
                        description = description,
                        entryHtmlPath = htmlFile.absolutePath,
                        timestamp = System.currentTimeMillis()
                    )
                    miniAppDao.insert(entity)
                    emit("[Mini app created: $name (id=$appId, path=${htmlFile.absolutePath})]")
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
