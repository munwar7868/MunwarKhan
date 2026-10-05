package com.munawar.ai.core.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "munawar_settings")

enum class Persona(val label: String, val basePitch: Float, val baseRate: Float) {
    FRIENDLY("Friendly", 1.05f, 1.0f),
    PROFESSIONAL("Professional", 0.95f, 0.95f),
    CUTE("Cute", 1.2f, 1.05f),
    COMPANION("Companion", 1.05f, 0.95f),
    MINIMAL("Minimal", 1.0f, 1.1f),
    ENERGETIC("Energetic", 1.1f, 1.15f)
}

enum class AssistantLanguage(val ttsLocaleTag: String, val sttLocaleTag: String, val label: String) {
    ENGLISH("en-US", "en-US", "English"),
    URDU("ur-PK", "ur-PK", "Urdu"),
    ROMAN_URDU("en-US", "en-US", "Roman Urdu (typed as English)")
}

/**
 * All persisted runtime configuration. The Gemini API key lives here (DataStore),
 * never hard-coded in source or committed to the APK.
 */
class PreferencesManager(private val context: Context) {

    private object Keys {
        val API_KEY = stringPreferencesKey("gemini_api_key")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val PERSONA = stringPreferencesKey("persona")
        val LANGUAGE = stringPreferencesKey("language")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val VOICE_RATE = floatPreferencesKey("voice_rate")
        val WAKE_WORD_ENABLED = booleanPreferencesKey("wake_word_enabled")
        val AUTO_SEND_SMS = stringPreferencesKey("auto_send_sms")
        val MEMORY_ENABLED = booleanPreferencesKey("memory_enabled")
    }

    val apiKey: Flow<String> = context.dataStore.data.map { it[Keys.API_KEY] ?: "" }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val persona: Flow<Persona> = context.dataStore.data.map {
        Persona.valueOf(it[Keys.PERSONA] ?: Persona.FRIENDLY.name)
    }
    val language: Flow<AssistantLanguage> = context.dataStore.data.map {
        AssistantLanguage.valueOf(it[Keys.LANGUAGE] ?: AssistantLanguage.ENGLISH.name)
    }
    val voicePitch: Flow<Float> = context.dataStore.data.map { it[Keys.VOICE_PITCH] ?: 1.0f }
    val voiceRate: Flow<Float> = context.dataStore.data.map { it[Keys.VOICE_RATE] ?: 1.0f }
    val wakeWordEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.WAKE_WORD_ENABLED] ?: false }
    val autoSendSms: Flow<String> = context.dataStore.data.map { it[Keys.AUTO_SEND_SMS] ?: "ALWAYS_CONFIRM" }
    val memoryEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.MEMORY_ENABLED] ?: true }

    suspend fun setApiKey(key: String) = context.dataStore.edit { it[Keys.API_KEY] = key.trim() }
    suspend fun setOnboardingDone(done: Boolean) = context.dataStore.edit { it[Keys.ONBOARDING_DONE] = done }
    suspend fun setPersona(p: Persona) = context.dataStore.edit { it[Keys.PERSONA] = p.name }
    suspend fun setLanguage(l: AssistantLanguage) = context.dataStore.edit { it[Keys.LANGUAGE] = l.name }
    suspend fun setVoicePitch(v: Float) = context.dataStore.edit { it[Keys.VOICE_PITCH] = v }
    suspend fun setVoiceRate(v: Float) = context.dataStore.edit { it[Keys.VOICE_RATE] = v }
    suspend fun setWakeWordEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.WAKE_WORD_ENABLED] = enabled }
    suspend fun setAutoSendSms(mode: String) = context.dataStore.edit { it[Keys.AUTO_SEND_SMS] = mode }
    suspend fun setMemoryEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.MEMORY_ENABLED] = enabled }

    suspend fun clearAllMemory() = context.dataStore.edit { it.remove(Keys.MEMORY_ENABLED) }
}
