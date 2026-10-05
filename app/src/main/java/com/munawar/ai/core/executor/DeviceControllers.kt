package com.munawar.ai.core.executor

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.munawar.ai.core.ai.ActionResult

class TorchController(private val context: Context) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var currentlyOn = false

    fun setTorch(on: Boolean): ActionResult {
        return try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return ActionResult(false, "This device doesn't have a controllable flashlight.")
            cameraManager.setTorchMode(cameraId, on)
            currentlyOn = on
            ActionResult(true, if (on) "Flashlight is on." else "Flashlight is off.")
        } catch (e: Exception) {
            ActionResult(false, "I couldn't control the flashlight on this device.")
        }
    }
}

class VolumeController(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun setVolume(stream: String, levelPercent: Int): ActionResult {
        val streamType = when (stream.lowercase()) {
            "ring", "ringtone" -> AudioManager.STREAM_RING
            "alarm" -> AudioManager.STREAM_ALARM
            "call", "voice" -> AudioManager.STREAM_VOICE_CALL
            else -> AudioManager.STREAM_MUSIC
        }
        return try {
            val max = audioManager.getStreamMaxVolume(streamType)
            val target = (max * levelPercent.coerceIn(0, 100) / 100.0).toInt()
            audioManager.setStreamVolume(streamType, target, 0)
            ActionResult(true, "Volume set to $levelPercent percent.")
        } catch (e: Exception) {
            ActionResult(false, "I couldn't change the volume: ${e.message}")
        }
    }
}

class AppController(private val context: Context) {
    fun open(appNameQuery: String): ActionResult {
        val pm = context.packageManager
        val launchables = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
            .map { it to pm.getApplicationLabel(it).toString() }
            .filter { it.second.contains(appNameQuery, ignoreCase = true) }

        if (launchables.isEmpty()) return ActionResult(false, "I couldn't find an app called $appNameQuery.")
        if (launchables.size > 1) {
            val names = launchables.joinToString(", ") { it.second }
            return ActionResult(false, "I found more than one app matching $appNameQuery: $names. Which one?")
        }
        return try {
            val intent = pm.getLaunchIntentForPackage(launchables.first().first.packageName)
            intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ActionResult(true, "Opening ${launchables.first().second}.")
        } catch (e: Exception) {
            ActionResult(false, "I couldn't open that app: ${e.message}")
        }
    }
}

class SettingsController(private val context: Context) {
    fun openPanel(section: String): ActionResult {
        val action = when (section.uppercase()) {
            "WIFI" -> Settings.ACTION_WIFI_SETTINGS
            "BLUETOOTH" -> Settings.ACTION_BLUETOOTH_SETTINGS
            "DISPLAY" -> Settings.ACTION_DISPLAY_SETTINGS
            "SOUND" -> Settings.ACTION_SOUND_SETTINGS
            "APPS" -> Settings.ACTION_APPLICATION_SETTINGS
            "BATTERY" -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            "NOTIFICATIONS" -> Settings.ACTION_APP_NOTIFICATION_SETTINGS
            "LOCATION" -> Settings.ACTION_LOCATION_SOURCE_SETTINGS
            "ACCESSIBILITY" -> Settings.ACTION_ACCESSIBILITY_SETTINGS
            "AIRPLANE" -> Settings.ACTION_AIRPLANE_MODE_SETTINGS
            "DATA" -> Settings.ACTION_DATA_ROAMING_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }
        return try {
            context.startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            ActionResult(true, "Android won't let me change that directly, so I've opened the $section settings panel for you.")
        } catch (e: Exception) {
            ActionResult(false, "I couldn't open that settings panel: ${e.message}")
        }
    }
}

class AlarmController(private val context: Context) {
    fun setAlarm(hour: Int, minute: Int, label: String = "MunawarAI alarm"): ActionResult {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, label)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val h12 = if (hour % 12 == 0) 12 else hour % 12
            val ampm = if (hour < 12) "AM" else "PM"
            ActionResult(true, "Alarm set for $h12:${minute.toString().padStart(2,'0')} $ampm.")
        } catch (e: Exception) {
            ActionResult(false, "I couldn't set that alarm: ${e.message}")
        }
    }
}

class MediaController(private val context: Context) {

    fun playYoutube(query: String): ActionResult = try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        ActionResult(true, "Searching YouTube for $query.")
    } catch (e: Exception) {
        ActionResult(false, "I couldn't open YouTube: ${e.message}")
    }

    fun openMaps(query: String): ActionResult = try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(query)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        ActionResult(true, "Opening maps for $query.")
    } catch (e: Exception) {
        ActionResult(false, "I couldn't open Maps: ${e.message}")
    }

    fun openBrowser(query: String): ActionResult = try {
        val looksLikeUrl = query.contains(".") && !query.contains(" ")
        val url = if (looksLikeUrl) {
            if (query.startsWith("http")) query else "https://$query"
        } else {
            "https://www.google.com/search?q=${Uri.encode(query)}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        ActionResult(true, "Opening that in your browser.")
    } catch (e: Exception) {
        ActionResult(false, "I couldn't open the browser: ${e.message}")
    }

    fun sendEmail(recipient: String, subject: String, body: String): ActionResult = try {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$recipient")).apply {
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        ActionResult(true, "I've opened your email app with that message ready — I can't confirm it's sent until you tap send.")
    } catch (e: Exception) {
        ActionResult(false, "I couldn't open an email composer: ${e.message}")
    }
}

class DeviceInfoController(private val context: Context) {
    fun batteryPercent(): ActionResult {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            val pct = bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ActionResult(true, "Your battery is at $pct percent.")
        } catch (e: Exception) {
            ActionResult(false, "I couldn't read the battery level.")
        }
    }
}
