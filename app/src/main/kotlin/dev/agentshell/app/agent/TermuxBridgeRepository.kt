package dev.agentshell.app.agent

import android.content.Context
import android.content.Intent
import android.os.FileObserver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.File
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume

class TermuxBridgeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Method A: Termux:API RUN_COMMAND intent (requires com.termux.api)
    fun runViaIntent(command: String) {
        val intent = Intent().apply {
            setClassName("com.termux", "com.termux.app.RunCommandService")
            action = "com.termux.RUN_COMMAND"
            putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/bash")
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    // Method B: Shared storage file bridge (reliable fallback, no Termux:API needed)
    private val bridgeDir = File("/sdcard/agentshell/bridge").apply { mkdirs() }

    suspend fun executeInTermux(command: String, timeoutMs: Long = 30_000): String {
        if (!bridgeDir.exists()) bridgeDir.mkdirs()
        
        val cmdId = UUID.randomUUID().toString().take(8)
        val cmdFile = File(bridgeDir, "cmd_$cmdId.json")
        val resultFile = File(bridgeDir, "result_$cmdId.json")
        
        // Use JSONObject for encoding to avoid adding extra serialization dependencies if not present
        val json = JSONObject().apply {
            put("id", cmdId)
            put("command", command)
        }
        cmdFile.writeText(json.toString())

        return withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                val observer = object : FileObserver(bridgeDir.path, CLOSE_WRITE) {
                    override fun onEvent(event: Int, path: String?) {
                        if (path == "result_$cmdId.json") {
                            stopWatching()
                            try {
                                val resultJson = JSONObject(resultFile.readText())
                                cont.resume(resultJson.optString("output", "[No output]"))
                            } catch (e: Exception) {
                                cont.resume("[Error parsing Termux output: ${e.message}]")
                            }
                        }
                    }
                }
                observer.startWatching()
                cont.invokeOnCancellation { observer.stopWatching() }
                
                // Double check in case it finished before observer started
                if (resultFile.exists()) {
                    observer.stopWatching()
                    try {
                        val resultJson = JSONObject(resultFile.readText())
                        if (cont.isActive) cont.resume(resultJson.optString("output", "[No output]"))
                    } catch (e: Exception) {
                        if (cont.isActive) cont.resume("[Error parsing Termux output: ${e.message}]")
                    }
                }
            }
        } ?: "[Termux bridge timeout ${timeoutMs}ms — is the watcher running?]"
    }
}
