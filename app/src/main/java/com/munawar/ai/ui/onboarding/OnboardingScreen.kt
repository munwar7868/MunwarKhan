package com.munawar.ai.ui.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munawar.ai.MunawarApplication
import com.munawar.ai.core.data.Persona
import kotlinx.coroutines.launch

/**
 * First-run flow: welcome -> Gemini API key entry with a live "Test Key" check
 * -> persona/voice pick -> done. Nothing is hard-coded; the key is written to
 * DataStore only after the user enters it here (or later in Settings).
 */
@Composable
fun OnboardingScreen(app: MunawarApplication, onFinished: () -> Unit) {
    var step by remember { mutableStateOf(0) }
    var apiKey by remember { mutableStateOf("") }
    var testing by remember { mutableStateOf(false) }
    var testResult by remember { mutableStateOf<Result<String>?>(null) }
    var persona by remember { mutableStateOf(Persona.FRIENDLY) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(24.dp),
            horizontalAlignment = Alignment.Start
        ) {
            when (step) {
                0 -> {
                    Text("Welcome to MunawarAI", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "MunawarAI is a voice-first assistant. It can chat with you and, where Android allows it, " +
                        "control things on your phone — calls, messages, apps, torch, alarms and more. Every " +
                        "action is confirmed and verified; nothing happens silently.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = { step = 1 }) { Text("Continue") }
                }
                1 -> {
                    Text("Connect Gemini", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "MunawarAI uses Google's Gemini API to understand what you say. Get a free key from " +
                        "Google AI Studio, then paste it below. It's stored only on this device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it; testResult = null },
                        label = { Text("Gemini API key") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            enabled = apiKey.isNotBlank() && !testing,
                            onClick = {
                                testing = true
                                scope.launch {
                                    val result = app.geminiProvider.testConnection(apiKey)
                                    testResult = result
                                    testing = false
                                }
                            }
                        ) { Text(if (testing) "Testing…" else "Test API Key") }
                        Spacer(Modifier.width(12.dp))
                        testResult?.let { r ->
                            if (r.isSuccess) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            } else {
                                Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                    testResult?.let { r ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            r.getOrNull() ?: "Couldn't verify key: ${r.exceptionOrNull()?.message}",
                            color = if (r.isSuccess) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Row {
                        TextButton(onClick = { step = 0 }) { Text("Back") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            enabled = apiKey.isNotBlank(),
                            onClick = {
                                scope.launch {
                                    app.preferences.setApiKey(apiKey)
                                    step = 2
                                }
                            }
                        ) { Text("Continue") }
                    }
                }
                2 -> {
                    Text("Pick a personality", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("You can change this anytime in Settings.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Persona.values().forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = persona == p, onClick = { persona = p })
                            Spacer(Modifier.width(8.dp))
                            Text(p.label)
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                    Row {
                        TextButton(onClick = { step = 1 }) { Text("Back") }
                        Spacer(Modifier.width(8.dp))
                        Button(onClick = {
                            scope.launch {
                                app.preferences.setPersona(persona)
                                app.preferences.setOnboardingDone(true)
                                onFinished()
                            }
                        }) { Text("Finish") }
                    }
                }
            }
        }
    }
}
