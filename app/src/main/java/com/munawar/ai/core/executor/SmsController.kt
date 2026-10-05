package com.munawar.ai.core.executor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.munawar.ai.core.ai.ActionResult

class SmsController(private val context: Context, private val resolver: ContactResolver) {

    /** autoSendMode: ALWAYS_CONFIRM | FIRST_ONLY | AUTOMATIC — the confirmation gate lives in the ViewModel. */
    fun send(contactName: String, message: String): ActionResult {
        val matches = resolver.findContacts(contactName)
        if (matches.isEmpty()) return ActionResult(false, "I couldn't find a contact named $contactName.")
        if (matches.size > 1) {
            val names = matches.joinToString(", ") { it.name }
            return ActionResult(false, "Multiple contacts match $contactName: $names. Which one?")
        }
        val number = matches.first().number
        val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

        return try {
            if (hasPermission) {
                val smsManager = context.getSystemService(SmsManager::class.java)
                smsManager.sendTextMessage(number, null, message, null, null)
                ActionResult(true, "Sent to ${matches.first().name}: \"$message\"")
            } else {
                // No SMS permission — fall back to opening the messaging app pre-filled, never fake success.
                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$number")).apply {
                    putExtra("sms_body", message)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                ActionResult(true, "I don't have SMS permission yet, so I've opened the message app for ${matches.first().name} with your text ready.")
            }
        } catch (e: Exception) {
            ActionResult(false, "I couldn't send that message: ${e.message}")
        }
    }
}
