package com.munawar.ai.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.munawar.ai.MunawarApplication
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as MunawarApplication
        val enabled = runBlocking { app.preferences.wakeWordEnabled.first() }
        if (enabled) {
            ContextCompat.startForegroundService(context, Intent(context, AssistantForegroundService::class.java))
        }
    }
}
