package dev.agentshell.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * TextToSpeechManager — singleton TTS engine.
 *
 * Used by the `speak` tool so the agent can read results aloud.
 * The static `instance` allows ToolDispatcher to call it without
 * Hilt injection (since ToolDispatcher is already fully constructed).
 *
 * Call initialize() once from App.kt or AppModule.
 */
@Singleton
class TextToSpeechManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        // Set by initialize() — accessible from ToolDispatcher without injection
        var instance: TextToSpeechManager? = null
            private set
    }

    private var tts: TextToSpeech? = null
    private var isReady = false

    /**
     * Must be called once before speak() is usable.
     * Called from AppModule after Hilt constructs this singleton.
     */
    fun initialize() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.getDefault()
                tts?.setSpeechRate(0.95f)  // Slightly slower than default — clearer
                isReady = true
                instance = this
            }
        }
    }

    /**
     * Speaks the given text and suspends until the utterance is complete.
     * Safe to call from a coroutine in ToolDispatcher.
     */
    suspend fun speak(text: String) {
        val engine = tts ?: return
        if (!isReady) return

        suspendCancellableCoroutine { cont ->
            val utteranceId = UUID.randomUUID().toString()
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?)  {}
                override fun onDone(utteranceId: String?)   { if (cont.isActive) cont.resume(Unit) }
                override fun onError(utteranceId: String?)  { if (cont.isActive) cont.resume(Unit) }
            })
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            cont.invokeOnCancellation { engine.stop() }
        }
    }

    fun stop() = tts?.stop()

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isReady = false
        instance = null
    }
}
