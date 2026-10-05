package com.munawar.ai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.munawar.ai.core.data.AssistantLanguage
import com.munawar.ai.core.voice.SpeechInputManager
import com.munawar.ai.core.voice.VoiceEngine
import com.munawar.ai.service.AssistantForegroundService
import com.munawar.ai.ui.main.MainScreen
import com.munawar.ai.ui.onboarding.OnboardingScreen
import com.munawar.ai.ui.settings.SettingsScreen
import com.munawar.ai.ui.theme.MunawarAITheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var voiceEngine: VoiceEngine
    private lateinit var speechInput: SpeechInputManager

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as MunawarApplication

        voiceEngine = VoiceEngine(this) { }
        speechInput = SpeechInputManager(
            context = this,
            onPartial = { },
            onFinal = { text -> viewModel.onUserUtterance(text) },
            onError = { },
            onListeningChanged = { }
        )
        viewModel.attachVoice(voiceEngine, speechInput)
        requestCorePermissions()

        setContent {
            var onboardingDone by remember { mutableStateOf<Boolean?>(null) }
            var showSettings by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                onboardingDone = app.preferences.onboardingDone.first()
            }
            LaunchedEffect(Unit) {
                val lang = app.preferences.language.first()
                voiceEngine.setLanguage(lang.ttsLocaleTag)
                speechInput.setLocale(lang.sttLocaleTag)
                val pitch = app.preferences.voicePitch.first()
                val rate = app.preferences.voiceRate.first()
                voiceEngine.configure(pitch, rate)
            }

            MunawarAITheme {
                when (onboardingDone) {
                    null -> { /* loading */ }
                    false -> OnboardingScreen(app = app, onFinished = { onboardingDone = true })
                    true -> if (showSettings) {
                        SettingsScreen(app = app, onBack = {
                            showSettings = false
                            scope.launch {
                                val lang = app.preferences.language.first()
                                voiceEngine.setLanguage(lang.ttsLocaleTag)
                                speechInput.setLocale(lang.sttLocaleTag)
                                voiceEngine.configure(app.preferences.voicePitch.first(), app.preferences.voiceRate.first())
                                val wakeWord = app.preferences.wakeWordEnabled.first()
                                if (wakeWord) startAssistantService() else stopAssistantService()
                            }
                        })
                    } else {
                        MainScreen(
                            viewModel = viewModel,
                            onOpenSettings = { showSettings = true },
                            onMicClick = {
                                val currentlyListening = viewModel.ui.value.state == AssistantState.LISTENING
                                if (currentlyListening) viewModel.stopListening() else viewModel.startListening()
                            },
                            onQuickAction = { prefill -> viewModel.onUserUtterance(prefill) }
                        )
                    }
                }
            }
        }
    }

    private fun requestCorePermissions() {
        val needed = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            needed.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val toRequest = needed.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (toRequest.isNotEmpty()) permissionLauncher.launch(toRequest.toTypedArray())
    }

    private fun startAssistantService() {
        val intent = Intent(this, AssistantForegroundService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopAssistantService() {
        stopService(Intent(this, AssistantForegroundService::class.java))
    }

    override fun onDestroy() {
        voiceEngine.shutdown()
        speechInput.cancel()
        super.onDestroy()
    }
}
