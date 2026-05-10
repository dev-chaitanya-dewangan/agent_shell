package dev.agentshell.app.brain

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HermesContextBuilder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun buildSystemPrompt(): String = withContext(Dispatchers.IO) {
        val soul = readBrain("SOUL.md")
        val user = readBrain("USER.md")
        val memory = readLastNLines("MEMORY.md", 30)
        """
        $soul
        --- USER CONTEXT ---
        $user
        --- RECENT MEMORY (last 30 events) ---
        $memory
        """.trimIndent()
    }

    fun ensureDefaultFiles() {
        val brainDir = File(context.filesDir, "brain").apply { mkdirs() }
        listOf("SOUL.md", "USER.md").forEach { name ->
            val target = File(brainDir, name)
            if (!target.exists()) {
                try {
                    context.assets.open("brain/$name").use { input ->
                        target.outputStream().use { input.copyTo(it) }
                    }
                } catch (e: Exception) {
                    // Ignore if missing
                }
            }
        }
    }

    private fun readBrain(name: String): String {
        val file = File(context.filesDir, "brain/$name")
        return if (file.exists()) file.readText() else ""
    }

    private fun readLastNLines(name: String, n: Int): String {
        val file = File(context.filesDir, "brain/$name")
        return if (file.exists()) file.readLines().takeLast(n).joinToString("\n") else ""
    }
}
