package com.munawar.ai.core.ai

import org.json.JSONObject

/**
 * Whitelisted action types the AI is allowed to request.
 * The AI NEVER executes code directly — it only returns one of these,
 * which the Android layer validates before doing anything.
 */
enum class ActionType {
    CHAT,               // plain conversational reply, no device action
    CALL_CONTACT,
    SEND_SMS,
    OPEN_APP,
    TORCH,
    SET_VOLUME,
    OPEN_SETTINGS_PANEL,
    SET_ALARM,
    PLAY_YOUTUBE,
    OPEN_MAPS,
    SEND_EMAIL,
    OPEN_BROWSER,
    DEVICE_INFO,        // e.g. battery percentage
    UNKNOWN
}

enum class RiskLevel { LOW, MEDIUM, HIGH }

fun ActionType.riskLevel(): RiskLevel = when (this) {
    ActionType.CHAT, ActionType.OPEN_APP, ActionType.TORCH, ActionType.SET_VOLUME,
    ActionType.PLAY_YOUTUBE, ActionType.OPEN_MAPS, ActionType.OPEN_BROWSER,
    ActionType.DEVICE_INFO, ActionType.OPEN_SETTINGS_PANEL -> RiskLevel.LOW
    ActionType.SEND_SMS, ActionType.SEND_EMAIL, ActionType.SET_ALARM -> RiskLevel.MEDIUM
    ActionType.CALL_CONTACT -> RiskLevel.MEDIUM
    ActionType.UNKNOWN -> RiskLevel.LOW
}

data class ParsedAction(
    val type: ActionType,
    val params: Map<String, String>,
    val spokenReply: String
) {
    companion object {
        /**
         * Parses the strict JSON the model is instructed to return, e.g.
         * {"intent":"TORCH","parameters":{"state":"ON"},"reply":"Sure, turning it on."}
         * Falls back to a CHAT action with the raw text if parsing fails —
         * this is the only fallback path, never arbitrary code execution.
         */
        fun fromModelText(raw: String): ParsedAction {
            return try {
                val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
                val json = JSONObject(cleaned)
                val intentName = json.optString("intent", "CHAT").uppercase()
                val type = try { ActionType.valueOf(intentName) } catch (e: Exception) { ActionType.UNKNOWN }
                val paramsJson = json.optJSONObject("parameters")
                val params = mutableMapOf<String, String>()
                if (paramsJson != null) {
                    val keys = paramsJson.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        params[k] = paramsJson.optString(k, "")
                    }
                }
                val reply = json.optString("reply", "")
                ParsedAction(type, params, reply.ifBlank { raw })
            } catch (e: Exception) {
                ParsedAction(ActionType.CHAT, emptyMap(), raw)
            }
        }
    }
}

data class ActionResult(val success: Boolean, val message: String)
