package dev.agentshell.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * VoiceInputManager — wraps Android SpeechRecognizer into a clean Flow<VoiceResult>.
 *
 * Usage:
 *   voiceInputManager.listen().collect { result ->
 *       when (result) {
 *           is VoiceResult.Partial -> showLiveText(result.text)
 *           is VoiceResult.Final   -> submitToAgent(result.text)
 *           is VoiceResult.Error   -> showError(result.message)
 *       }
 *   }
 *
 * Requires: RECORD_AUDIO permission granted before calling listen().
 */
@Singleton
class VoiceInputManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    sealed class VoiceResult {
        data class Partial(val text: String)                          : VoiceResult()
        data class Final(val text: String)                           : VoiceResult()
        /** @param retryable true = transient error, user can tap mic again safely */
        data class Error(val message: String, val retryable: Boolean = true) : VoiceResult()
    }

    fun listen(): Flow<VoiceResult> = callbackFlow {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            trySend(VoiceResult.Error("Speech recognition not available on this device"))
            close()
            return@callbackFlow
        }

        // Use offline on-device model (built into Pixel 6a and most modern Android phones).
        // Falls back to network automatically if on-device model is unavailable.
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            // ← KEY: Forces Android to use the on-device neural speech model.
            // No audio data leaves the phone. Works without internet.
            // On Pixel 6a this uses Google's Tensor G2-optimised on-device ASR.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            // Cut off silence after 1.5s so the agent responds faster
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
        }

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: return
                trySend(VoiceResult.Partial(partial))
            }

            override fun onResults(results: Bundle?) {
                val text = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                trySend(VoiceResult.Final(text))
                close()
            }

            override fun onError(error: Int) {
                val (msg, retryable) = when (error) {
                    SpeechRecognizer.ERROR_AUDIO              -> Pair("Audio recording error — check mic", false)
                    SpeechRecognizer.ERROR_CLIENT             -> Pair("Client error — tap mic to retry", true)
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Pair("RECORD_AUDIO permission missing — enable in Settings → Apps → agentShell", false)
                    SpeechRecognizer.ERROR_NETWORK            -> Pair("No internet — offline mode active, tap mic to retry", true)
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT    -> Pair("Network timeout — tap mic to retry", true)
                    SpeechRecognizer.ERROR_NO_MATCH           -> Pair("Didn't catch that — tap mic and speak clearly", true)
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY    -> Pair("Mic is busy — wait a moment and retry", true)
                    SpeechRecognizer.ERROR_SERVER             -> Pair("Server error — tap mic to retry", true)
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT     -> Pair("No speech detected — tap mic and speak", true)
                    else                                      -> Pair("Voice error ($error) — tap mic to retry", true)
                }
                trySend(VoiceResult.Error(msg, retryable))
                close()
            }

            // Required overrides — not needed for our use case
            override fun onReadyForSpeech(params: Bundle?)   {}
            override fun onBeginningOfSpeech()               {}
            override fun onRmsChanged(rmsdB: Float)          {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech()                     {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        recognizer.startListening(intent)

        awaitClose {
            recognizer.stopListening()
            recognizer.destroy()
        }
    }
}
