package com.munawar.ai.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.munawar.ai.AssistantState
import com.munawar.ai.ChatMessage
import com.munawar.ai.MainViewModel
import com.munawar.ai.ui.components.MunawarOrb
import com.munawar.ai.ui.theme.MunawarPink
import com.munawar.ai.ui.theme.MunawarPurple
import com.munawar.ai.ui.theme.MunawarTeal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenSettings: () -> Unit,
    onMicClick: () -> Unit,
    onQuickAction: (String) -> Unit
) {
    val ui by viewModel.ui.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MunawarAI", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    MunawarOrb(
                        stateLabel = ui.state.name,
                        colorA = MunawarPurple,
                        colorB = if (ui.state == AssistantState.SPEAKING) MunawarTeal else MunawarPink
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(ui.statusText, style = MaterialTheme.typography.titleMedium)
                }
            }

            LazyColumn(modifier = Modifier.weight(1f)) {
                items(ui.messages) { msg -> MessageBubble(msg) }
            }

            if (ui.state == AssistantState.AWAITING_CONFIRMATION) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(onClick = { viewModel.confirmPendingAction(true) }) { Text("Yes, do it") }
                    OutlinedButton(onClick = { viewModel.confirmPendingAction(false) }) { Text("Cancel") }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickChip("Call") { onQuickAction("Call ") }
                QuickChip("Message") { onQuickAction("Send a message to ") }
                QuickChip("YouTube") { onQuickAction("Play on YouTube: ") }
                QuickChip("Torch") { onQuickAction("Turn on the flashlight") }
            }

            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                FloatingActionButton(onClick = onMicClick, containerColor = MunawarPurple) {
                    Icon(
                        if (ui.state == AssistantState.LISTENING) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = "Microphone",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    AssistChip(onClick = onClick, label = { Text(label) })
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.speaker == "user"
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MunawarPurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
