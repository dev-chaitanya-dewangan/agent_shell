package dev.agentshell.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import java.io.File

@HiltAndroidApp
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        deployBridgeScript()
    }

    /** Copies the bundled bridge_watcher.sh asset to /sdcard so Termux can run it. */
    private fun deployBridgeScript() {
        try {
            val bridgeDir = File("/sdcard/agentshell/bridge").apply { mkdirs() }
            val dest = File(bridgeDir, "bridge_watcher.sh")
            assets.open("termux/bridge_watcher.sh").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.setExecutable(true)
        } catch (_: Exception) {
            // Silently fail if storage isn't available yet
        }
    }
}
