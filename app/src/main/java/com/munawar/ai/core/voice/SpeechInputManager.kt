package com.munawar.ai.core.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Wraps Android's on-device SpeechRecognizer.
 *
 * Note on "wake word": Android has no public always-on, low-power wake-word API.
 * A real "Hey Munawar" wake word needs a dedicated engine (e.g. Picovoice Porcupine)
 * that listens to raw audio continuously with minimal battery impact. What this class
 * offers instead is an explicit, user-controlled continuous-listening loop (restarting
 * the recognizer after each utterance) that is only active when the user has turned on
 * "Always Active" in Settings — never silently.
 */
class SpeechInputManager(
    private val context: Context,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit,
    private val onError: (String) -> Unit,
    private val onListeningChanged: (Boolean) -> Unit
) {
    private var recognizer: SpeechRecognizer? = null
    private var localeTag: String = "en-US"
    private var continuousMode = false

    fun setLocale(tag: String) {
        localeTag = tag
    }

    fun setContinuousMode(enabled: Boolean) {
        continuousMode = enabled
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("Speech recognition isn't available on this device.")
            return
        }
        stopListening()
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { onListeningChanged(true) }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { onListeningChanged(false) }

                override fun onError(error: Int) {
                    onListeningChanged(false)
                    onError("Speech recognition error ($error).")
                    if (continuousMode) startListening()
                }

                override fun onResults(results: Bundle?) {
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!text.isNullOrBlank()) onFinal(text)
                    if (continuousMode) startListening()
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                    if (!text.isNullOrBlank()) onPartial(text)
                }

                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
        recognizer?.startListening(intent)
    }

    fun stopListening() {
        recognizer?.stopListening()
        recognizer?.destroy()
        recognizer = null
    }

    fun cancel() {
        continuousMode = false
        stopListening()
        onListeningChanged(false)
    }
}
