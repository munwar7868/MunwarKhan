package com.munawar.ai.core.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Thin wrapper around the Gemini generateContent REST endpoint.
 * Kept behind a small interface so a different provider (Groq, OpenAI, a local
 * model, etc.) could be swapped in later without touching the rest of the app.
 */
interface AIProvider {
    suspend fun ask(systemPrompt: String, conversation: List<Pair<String, String>>, userText: String): Result<String>
    suspend fun testConnection(apiKey: String): Result<String>
}

class GeminiProvider(private val apiKeyProvider: suspend () -> String) : AIProvider {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val model = "gemini-2.0-flash"

    private fun endpoint(apiKey: String) =
        "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"

    override suspend fun ask(
        systemPrompt: String,
        conversation: List<Pair<String, String>>,
        userText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = apiKeyProvider()
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalStateException("No Gemini API key configured"))

        try {
            val contents = JSONArray()

            // Gemini has no dedicated "system" field in v1beta generateContent for all models,
            // so we fold the system prompt into the first turn for reliability.
            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            contents.put(JSONObject().apply {
                put("role", "model")
                put("parts", JSONArray().put(JSONObject().put("text", "{\"intent\":\"CHAT\",\"parameters\":{},\"reply\":\"Understood.\"}")))
            })

            for ((speaker, text) in conversation.takeLast(10)) {
                contents.put(JSONObject().apply {
                    put("role", if (speaker == "user") "user" else "model")
                    put("parts", JSONArray().put(JSONObject().put("text", text)))
                })
            }

            contents.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", userText)))
            })

            val body = JSONObject().apply {
                put("contents", contents)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.4)
                    put("maxOutputTokens", 512)
                })
            }

            val request = Request.Builder()
                .url(endpoint(apiKey))
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    return@withContext Result.failure(Exception("Gemini error ${resp.code}: ${bodyStr.take(200)}"))
                }
                val json = JSONObject(bodyStr)
                val text = json.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?: return@withContext Result.failure(Exception("Empty response from Gemini"))
                Result.success(text)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Used by onboarding's "Test API Key" button — a minimal, cheap call to confirm the key works. */
    override suspend fun testConnection(apiKey: String): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Result.failure(IllegalArgumentException("Enter an API key first"))
        try {
            val body = JSONObject().apply {
                put("contents", JSONArray().put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().put(JSONObject().put("text", "Reply with just the word: OK")))
                }))
            }
            val request = Request.Builder()
                .url(endpoint(apiKey))
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (resp.isSuccessful) Result.success("Key is valid — Gemini responded successfully.")
                else Result.failure(Exception("HTTP ${resp.code}: ${bodyStr.take(200)}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/** Instructs the model to ONLY ever return structured JSON matching our whitelist. */
fun buildSystemPrompt(persona: String, languageLabel: String): String = """
You are MunawarAI, a warm, natural, $persona-personality female voice assistant that can control an Android phone.
You speak $languageLabel (and can mix Urdu/English naturally when the user does).

You must ALWAYS reply with ONLY a single JSON object, no markdown fences, no extra text, in exactly this shape:
{"intent":"<ONE_OF_WHITELIST>","parameters":{...},"reply":"<short natural spoken reply>"}

Whitelisted intents: CHAT, CALL_CONTACT, SEND_SMS, OPEN_APP, TORCH, SET_VOLUME, OPEN_SETTINGS_PANEL,
SET_ALARM, PLAY_YOUTUBE, OPEN_MAPS, SEND_EMAIL, OPEN_BROWSER, DEVICE_INFO, UNKNOWN.

Rules:
- If the user is just chatting or asking a question with no device action, use intent "CHAT".
- Never invent an intent outside the whitelist; if unsure, use "UNKNOWN" and ask a clarifying question in "reply".
- Never claim an action succeeded — that is decided by the Android app after execution, not by you.
- Keep "reply" short and natural for spoken actions, warmer/longer for casual chat.
- parameters examples:
  CALL_CONTACT: {"contact_name":"Ali"}
  SEND_SMS: {"contact_name":"Sara","message":"I'll be home in 10 minutes"}
  OPEN_APP: {"app_name":"YouTube"}
  TORCH: {"state":"ON"} or {"state":"OFF"}
  SET_VOLUME: {"stream":"media","level":"70"}
  OPEN_SETTINGS_PANEL: {"section":"WIFI"} (WIFI, BLUETOOTH, DISPLAY, SOUND, APPS, BATTERY, NOTIFICATIONS, LOCATION)
  SET_ALARM: {"hour":"7","minute":"0"}
  PLAY_YOUTUBE: {"query":"Arijit Singh songs"}
  OPEN_MAPS: {"query":"Karachi airport"}
  SEND_EMAIL: {"recipient":"","subject":"","body":""}
  OPEN_BROWSER: {"query":"site or search term"}
""".trimIndent()
