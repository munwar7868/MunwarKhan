package com.munawar.ai.core.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID

/**
 * Abstraction over text-to-speech so a higher-quality cloud voice provider
 * can be plugged in later without touching call sites. Ships with Android's
 * built-in TTS configured for a natural female voice where the device offers one.
 */
class VoiceEngine(context: Context, private val onSpeakingStateChanged: (Boolean) -> Unit) {

    private var tts: TextToSpeech? = null
    private var ready = false

    init {
        tts = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                selectFemaleVoiceIfAvailable()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { onSpeakingStateChanged(true) }
                    override fun onDone(utteranceId: String?) { onSpeakingStateChanged(false) }
                    override fun onError(utteranceId: String?) { onSpeakingStateChanged(false) }
                })
            }
        }
    }

    private fun selectFemaleVoiceIfAvailable() {
        val engine = tts ?: return
        val femaleVoice = engine.voices?.firstOrNull {
            it.name.contains("female", ignoreCase = true) && !it.isNetworkConnectionRequired
        }
        if (femaleVoice != null) engine.voice = femaleVoice
    }

    fun setLanguage(localeTag: String) {
        tts?.language = Locale.forLanguageTag(localeTag)
    }

    fun configure(pitch: Float, rate: Float) {
        tts?.setPitch(pitch)
        tts?.setSpeechRate(rate)
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
    }
}
