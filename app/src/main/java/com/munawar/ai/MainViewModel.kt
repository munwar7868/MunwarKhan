package com.munawar.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.munawar.ai.core.ai.*
import com.munawar.ai.core.data.AssistantLanguage
import com.munawar.ai.core.data.Persona
import com.munawar.ai.core.executor.ActionExecutor
import com.munawar.ai.core.voice.SpeechInputManager
import com.munawar.ai.core.voice.VoiceEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class AssistantState { IDLE, LISTENING, THINKING, SPEAKING, AWAITING_CONFIRMATION, ERROR }

data class ChatMessage(val speaker: String, val text: String) // speaker: "user" | "assistant"

data class UiState(
    val state: AssistantState = AssistantState.IDLE,
    val statusText: String = "How can I help?",
    val messages: List<ChatMessage> = emptyList(),
    val pendingAction: ParsedAction? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<MunawarApplication>()
    private val executor = ActionExecutor(application)

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private var voiceEngine: VoiceEngine? = null
    private var speechInput: SpeechInputManager? = null
    private val conversationHistory = mutableListOf<Pair<String, String>>()

    fun attachVoice(voiceEngine: VoiceEngine, speechInput: SpeechInputManager) {
        this.voiceEngine = voiceEngine
        this.speechInput = speechInput
    }

    fun onUserUtterance(text: String) {
        if (text.isBlank()) return
        _ui.value = _ui.value.copy(
            messages = _ui.value.messages + ChatMessage("user", text),
            state = AssistantState.THINKING,
            statusText = "Thinking…"
        )
        conversationHistory.add("user" to text)

        viewModelScope.launch {
            val persona = app.preferences.persona.first()
            val language = app.preferences.language.first()
            val systemPrompt = buildSystemPrompt(persona.label, language.label)

            val result = app.geminiProvider.ask(systemPrompt, conversationHistory, text)
            result.onSuccess { raw ->
                val parsed = ParsedAction.fromModelText(raw)
                conversationHistory.add("assistant" to raw)
                handleParsedAction(parsed)
            }.onFailure { err ->
                val msg = "I couldn't reach Gemini: ${err.message}. Check your API key in Settings."
                speak(msg)
                _ui.value = _ui.value.copy(
                    state = AssistantState.ERROR,
                    statusText = "Connection error",
                    messages = _ui.value.messages + ChatMessage("assistant", msg)
                )
            }
        }
    }

    private fun handleParsedAction(action: ParsedAction) {
        val risk = action.type.riskLevel()
        if (risk == RiskLevel.MEDIUM || risk == RiskLevel.HIGH) {
            // Ask for confirmation before doing anything real.
            val confirmText = "${action.spokenReply} Should I go ahead?"
            speak(confirmText)
            _ui.value = _ui.value.copy(
                state = AssistantState.AWAITING_CONFIRMATION,
                statusText = "Waiting for confirmation",
                messages = _ui.value.messages + ChatMessage("assistant", confirmText),
                pendingAction = action
            )
        } else {
            runAction(action)
        }
    }

    fun confirmPendingAction(confirmed: Boolean) {
        val action = _ui.value.pendingAction ?: return
        _ui.value = _ui.value.copy(pendingAction = null)
        if (confirmed) {
            runAction(action)
        } else {
            val msg = "Okay, I won't do that."
            speak(msg)
            _ui.value = _ui.value.copy(
                state = AssistantState.IDLE,
                statusText = "How can I help?",
                messages = _ui.value.messages + ChatMessage("assistant", msg)
            )
        }
    }

    private fun runAction(action: ParsedAction) {
        val result = executor.execute(action)
        // Verification: only ever speak/display what actually happened.
        speak(result.message)
        _ui.value = _ui.value.copy(
            state = if (result.success) AssistantState.IDLE else AssistantState.ERROR,
            statusText = "How can I help?",
            messages = _ui.value.messages + ChatMessage("assistant", result.message)
        )
    }

    fun startListening() {
        _ui.value = _ui.value.copy(state = AssistantState.LISTENING, statusText = "Listening…")
        speechInput?.startListening()
    }

    fun stopListening() {
        speechInput?.stopListening()
        _ui.value = _ui.value.copy(state = AssistantState.IDLE, statusText = "How can I help?")
    }

    private fun speak(text: String) {
        voiceEngine?.speak(text)
    }

    fun clearConversation() {
        conversationHistory.clear()
        _ui.value = _ui.value.copy(messages = emptyList())
    }
}
