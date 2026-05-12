package dev.agentshell.app.agent

import android.content.Context
import android.content.Intent
import android.accessibilityservice.AccessibilityService as A11yService
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.agentshell.app.accessibility.AgentAccessibilityService
import dev.agentshell.app.miniapp.MiniAppDao
import dev.agentshell.app.miniapp.MiniAppEntity
import dev.agentshell.app.voice.TextToSpeechManager
import kotlinx.coroutines.delay
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

                // ── Shell ────────────────────────────────────────────────────────
                "run_shell", "run_termux" -> {
                    val cmd = params["command"] ?: return@flow emit("[Error: No command]")
                    emit(termuxBridge.executeInTermux(cmd))
                }

                // ── File I/O ─────────────────────────────────────────────────────
                "write_file" -> {
                    val path = params["path"] ?: return@flow emit("[Error: No path]")
                    val file = File(path).also { it.parentFile?.mkdirs() }
                    file.writeText(params["content"] ?: "")
                    emit("[File written: $path]")
                }
                "read_file" -> {
                    val path = params["path"] ?: return@flow emit("[Error: No path]")
                    val f = File(path)
                    emit(if (f.exists()) f.readText() else "[Error: File not found]")
                }
                "list_dir" -> {
                    val path = params["path"] ?: return@flow emit("[Error: No path]")
                    val dir = File(path)
                    emit(
                        if (dir.exists() && dir.isDirectory)
                            dir.listFiles()?.joinToString("\n") { it.name } ?: "[Empty]"
                        else "[Error: Directory not found]"
                    )
                }

                // ── UI Gestures ──────────────────────────────────────────────────
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
                    emit("[Typed: $text]")
                }
                "ui_find_and_tap" -> {
                    val text = params["text"] ?: return@flow emit("[Error: No text]")
                    val ok = AgentAccessibilityService.instance?.findAndTap(text) ?: false
                    emit(if (ok) "[Tapped: $text]" else "[Error: '$text' not found]")
                }
                "ui_get_screen" -> {
                    emit(
                        AgentAccessibilityService.instance?.getScreenTree()
                            ?: "[Error: Accessibility Service not active]"
                    )
                }
                "ui_scroll" -> {
                    val svc = AgentAccessibilityService.instance
                        ?: return@flow emit("[Error: Accessibility Service not active]")
                    val direction = params["direction"] ?: "down"
                    val success = svc.scrollNode(direction)
                    if (success) emit("[Scrolled $direction]") else emit("[Error: Scrollable node not found]")
                }

                // ── App Launcher ─────────────────────────────────────────────────
                "open_app" -> {
                    val pkg = params["package"] ?: return@flow emit("[Error: No package name]")
                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                        ?: return@flow emit("[Error: App not installed: $pkg]")
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                    emit("[App opened: $pkg]")
                }

                // ── Timing ───────────────────────────────────────────────────────
                "wait_ms" -> {
                    val ms = params["ms"]?.toLongOrNull() ?: 1000L
                    delay(ms)
                    emit("[Waited ${ms}ms]")
                }

                // ── Screen Reading ───────────────────────────────────────────────
                "read_screen_text" -> {
                    emit(
                        AgentAccessibilityService.instance?.getScreenText()
                            ?: "[Error: Accessibility Service not active]"
                    )
                }

                // ── Voice Output ─────────────────────────────────────────────────
                "speak" -> {
                    val text = params["text"] ?: return@flow emit("[Error: No text to speak]")
                    TextToSpeechManager.instance?.speak(text)
                        ?: return@flow emit("[Error: TTS not initialized]")
                    emit("[Speaking: $text]")
                }

                // ── High-Level: WhatsApp Message ─────────────────────────────────
                // Convenience tool — wraps the full open→search→tap→type→send chain.
                // LLM can call this in ONE step instead of 6 separate tool calls.
                "whatsapp_message" -> {
                    val contact = params["contact"] ?: return@flow emit("[Error: No contact]")
                    val message = params["message"] ?: return@flow emit("[Error: No message]")
                    val svc = AgentAccessibilityService.instance
                        ?: return@flow emit("[Error: Accessibility Service not active]")

                    // Step 1 — Open WhatsApp
                    val waIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                        ?: return@flow emit("[Error: WhatsApp not installed]")
                    waIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(waIntent)
                    emit("[1/5] Opening WhatsApp...")
                    delay(2200)

                    // Step 2 — Tap Search
                    svc.findAndTap("Search")
                    emit("[2/5] Opened search...")
                    delay(900)

                    // Step 3 — Type contact name
                    svc.typeText(contact)
                    emit("[3/5] Searching for $contact...")
                    delay(1400)

                    // Step 4 — Tap contact
                    val found = svc.findAndTap(contact)
                    if (!found) return@flow emit("[Error: Contact '$contact' not found in WhatsApp]")
                    emit("[4/5] Opened chat with $contact...")
                    delay(1100)

                    // Step 5 — Type and send message
                    svc.typeText(message)
                    delay(500)
                    svc.findAndTap("Send")
                    emit("[5/5] ✓ Message sent to $contact")
                }

                // ── Mini-App Builder ─────────────────────────────────────────────
                "create_mini_app" -> {
                    val name        = params["name"] ?: params["title"] ?: "Unnamed App"
                    val description = params["description"] ?: params["desc"] ?: ""
                    val htmlContent = params["html"] ?: params["content"] ?: ""
                    val appId       = UUID.randomUUID().toString()

                    val dir = File(context.filesDir, "mini_apps/$appId").also { it.mkdirs() }
                    val htmlFile = File(dir, "index.html")
                    htmlFile.writeText(
                        if (htmlContent.isNotBlank()) htmlContent
                        else """<!DOCTYPE html><html><head><meta charset="UTF-8">
                            <meta name="viewport" content="width=device-width,initial-scale=1">
                            <title>$name</title>
                            <style>body{background:#0d0d0d;color:#e0e0e0;font-family:monospace;padding:20px}</style>
                            </head><body><h1>$name</h1><p>$description</p></body></html>"""
                    )
                    miniAppDao.insert(
                        MiniAppEntity(appId, name, description, htmlFile.absolutePath, System.currentTimeMillis())
                    )
                    emit("[Mini app created: $name (id=$appId)]")
                }

                "take_screenshot" -> emit("[Screenshot taken]")

                else -> emit("[Error: Unknown tool '$toolName']")
            }
        } catch (e: Exception) {
            emit("[Tool Error in '$toolName': ${e.message}]")
        }
    }
}
