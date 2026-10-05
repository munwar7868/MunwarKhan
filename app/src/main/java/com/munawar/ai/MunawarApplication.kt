package com.munawar.ai

import android.app.Application
import com.munawar.ai.core.ai.GeminiProvider
import com.munawar.ai.core.data.PreferencesManager
import kotlinx.coroutines.flow.first

class MunawarApplication : Application() {
    lateinit var preferences: PreferencesManager
    lateinit var geminiProvider: GeminiProvider

    override fun onCreate() {
        super.onCreate()
        preferences = PreferencesManager(this)
        geminiProvider = GeminiProvider(apiKeyProvider = { preferences.apiKey.first() })
    }
}
