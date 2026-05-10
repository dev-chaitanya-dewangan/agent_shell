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

    /** Copies the bundled bridge_watcher.sh asset to external files dir — no special permissions needed. */
    private fun deployBridgeScript() {
        try {
            // getExternalFilesDir is always writable — no MANAGE_EXTERNAL_STORAGE needed
            val bridgeDir = File(getExternalFilesDir(null), "bridge").apply { mkdirs() }
            val dest = File(bridgeDir, "bridge_watcher.sh")
            assets.open("termux/bridge_watcher.sh").use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            dest.setExecutable(true)
        } catch (_: Exception) {
            // Silently fail if external storage isn't available yet
        }
    }
}
