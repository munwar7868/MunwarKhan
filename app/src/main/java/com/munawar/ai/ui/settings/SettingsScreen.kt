package com.munawar.ai.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munawar.ai.MunawarApplication
import com.munawar.ai.core.data.AssistantLanguage
import com.munawar.ai.core.data.Persona
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: MunawarApplication, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var apiKey by remember { mutableStateOf("") }
    var persona by remember { mutableStateOf(Persona.FRIENDLY) }
    var language by remember { mutableStateOf(AssistantLanguage.ENGLISH) }
    var pitch by remember { mutableStateOf(1.0f) }
    var rate by remember { mutableStateOf(1.0f) }
    var wakeWordEnabled by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Result<String>?>(null) }
    var testing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apiKey = app.preferences.apiKey.first()
        persona = app.preferences.persona.first()
        language = app.preferences.language.first()
        pitch = app.preferences.voicePitch.first()
        rate = app.preferences.voiceRate.first()
        wakeWordEnabled = app.preferences.wakeWordEnabled.first()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("Gemini API key", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it; testResult = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("API key") }
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    enabled = !testing && apiKey.isNotBlank(),
                    onClick = {
                        testing = true
                        scope.launch {
                            testResult = app.geminiProvider.testConnection(apiKey)
                            testing = false
                        }
                    }
                ) { Text(if (testing) "Testing…" else "Test key") }
                Spacer(Modifier.width(12.dp))
                Button(onClick = { scope.launch { app.preferences.setApiKey(apiKey) } }) { Text("Save") }
                Spacer(Modifier.width(12.dp))
                testResult?.let {
                    Icon(
                        if (it.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                        contentDescription = null,
                        tint = if (it.isSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            }

            Divider(modifier = Modifier.padding(vertical = 20.dp))

            Text("Personality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Persona.values().forEach { p ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = persona == p, onClick = {
                        persona = p
                        scope.launch { app.preferences.setPersona(p) }
                    })
                    Text(p.label)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 20.dp))

            Text("Language", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            AssistantLanguage.values().forEach { l ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = language == l, onClick = {
                        language = l
                        scope.launch { app.preferences.setLanguage(l) }
                    })
                    Text(l.label)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 20.dp))

            Text("Voice tuning", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Pitch: ${"%.2f".format(pitch)}")
            Slider(value = pitch, onValueChange = { pitch = it }, valueRange = 0.5f..1.8f,
                onValueChangeFinished = { scope.launch { app.preferences.setVoicePitch(pitch) } })
            Text("Speed: ${"%.2f".format(rate)}")
            Slider(value = rate, onValueChange = { rate = it }, valueRange = 0.5f..1.8f,
                onValueChangeFinished = { scope.launch { app.preferences.setVoiceRate(rate) } })

            Divider(modifier = Modifier.padding(vertical = 20.dp))

            Text("Always Active (experimental)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Keeps the microphone listening continuously in the background so you don't have to tap the mic " +
                "each time. This is not a true low-power wake word — it uses more battery and keeps a " +
                "persistent notification while on. Turn it off any time.",
                style = MaterialTheme.typography.bodySmall
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = wakeWordEnabled, onCheckedChange = {
                    wakeWordEnabled = it
                    scope.launch { app.preferences.setWakeWordEnabled(it) }
                })
                Spacer(Modifier.width(8.dp))
                Text(if (wakeWordEnabled) "On" else "Off")
            }

            Divider(modifier = Modifier.padding(vertical = 20.dp))

            Text("Privacy", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "MunawarAI only accesses contacts, calls, SMS, camera (torch) or the microphone when you use a " +
                "feature that needs it, and only after you grant that Android permission.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
