package dev.agentshell.app.brain

import android.content.Context
import androidx.core.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrainLogger @Inject constructor(
    private val dao: BrainLogDao,
    @ApplicationContext private val context: Context
) {
    suspend fun log(type: LogType, tag: String, content: String) {
        val entry = BrainLogEntity(
            timestamp = System.currentTimeMillis(),
            type = type.name, 
            tag = tag, 
            content = content
        )
        dao.insert(entry)
        appendToMarkdown(entry)
    }

    private suspend fun appendToMarkdown(entry: BrainLogEntity) = withContext(Dispatchers.IO) {
        val brainDir = File(context.filesDir, "brain").apply { mkdirs() }
        val memFile = File(brainDir, "MEMORY.md")
        val atomicFile = AtomicFile(memFile)
        
        val existing = if (memFile.exists()) memFile.readLines() else emptyList()
        val newLine = "- [${entry.type}] ${entry.tag}: ${entry.content.take(200)}"
        val kept = (existing + newLine).takeLast(50)  // keep last 50 lines
        
        val stream = atomicFile.startWrite()
        try {
            stream.bufferedWriter().use { it.write(kept.joinToString("\n")) }
            atomicFile.finishWrite(stream)
        } catch (e: Exception) {
            atomicFile.failWrite(stream)
        }
    }
}
