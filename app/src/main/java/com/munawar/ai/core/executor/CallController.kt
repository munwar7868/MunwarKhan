package com.munawar.ai.core.executor

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import com.munawar.ai.core.ai.ActionResult

class CallController(private val context: Context, private val resolver: ContactResolver) {

    fun call(contactName: String): ActionResult {
        val hasPermission = ContextCompat.checkSelfPermission(context, android.Manifest.permission.CALL_PHONE) ==
            PackageManager.PERMISSION_GRANTED

        val matches = resolver.findContacts(contactName)
        if (matches.isEmpty()) {
            return ActionResult(false, "I couldn't find a contact named $contactName.")
        }
        if (matches.size > 1) {
            val names = matches.joinToString(", ") { it.name }
            return ActionResult(false, "I found more than one match for $contactName: $names. Which one do you mean?")
        }

        val number = matches.first().number
        return try {
            val action = if (hasPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL
            val intent = Intent(action, Uri.parse("tel:$number")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            if (hasPermission) ActionResult(true, "Calling ${matches.first().name} now.")
            else ActionResult(true, "I don't have call permission yet, so I've opened the dialer for ${matches.first().name}.")
        } catch (e: Exception) {
            ActionResult(false, "I couldn't start the call: ${e.message}")
        }
    }
}
