package com.munawar.ai.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.munawar.ai.MainActivity
import com.munawar.ai.R

/**
 * Keeps a persistent notification while "Always Active" is enabled in Settings.
 * This does NOT silently record audio in the background — it exists purely to
 * host the continuous-listening loop the user explicitly opted into, and Android
 * requires a visible notification for any foreground microphone service so the
 * user always knows listening is active.
 */
class AssistantForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "munawar_assistant_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createChannelIfNeeded()
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MunawarAI is listening")
            .setContentText("Tap to open. Turn off Always Active in Settings to stop.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(CHANNEL_ID, "MunawarAI Assistant", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null
}
