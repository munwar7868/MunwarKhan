package com.munawar.ai.core.executor

import android.content.Context
import com.munawar.ai.core.ai.ActionResult
import com.munawar.ai.core.ai.ActionType
import com.munawar.ai.core.ai.ParsedAction

/**
 * The ONLY place that turns a validated ParsedAction into a real device action.
 * Every branch either performs a real, verifiable action or explains why it can't —
 * it never reports success it hasn't checked.
 */
class ActionExecutor(context: Context) {

    private val contactResolver = ContactResolver(context)
    private val callController = CallController(context, contactResolver)
    private val smsController = SmsController(context, contactResolver)
    private val torchController = TorchController(context)
    private val volumeController = VolumeController(context)
    private val appController = AppController(context)
    private val settingsController = SettingsController(context)
    private val alarmController = AlarmController(context)
    private val mediaController = MediaController(context)
    private val deviceInfoController = DeviceInfoController(context)

    fun execute(action: ParsedAction): ActionResult {
        return when (action.type) {
            ActionType.CHAT, ActionType.UNKNOWN -> ActionResult(true, action.spokenReply)

            ActionType.CALL_CONTACT -> callController.call(action.params["contact_name"].orEmpty())

            ActionType.SEND_SMS -> smsController.send(
                action.params["contact_name"].orEmpty(),
                action.params["message"].orEmpty()
            )

            ActionType.OPEN_APP -> appController.open(action.params["app_name"].orEmpty())

            ActionType.TORCH -> torchController.setTorch(
                action.params["state"]?.equals("ON", ignoreCase = true) == true
            )

            ActionType.SET_VOLUME -> volumeController.setVolume(
                action.params["stream"] ?: "media",
                action.params["level"]?.toIntOrNull() ?: 50
            )

            ActionType.OPEN_SETTINGS_PANEL -> settingsController.openPanel(action.params["section"] ?: "GENERAL")

            ActionType.SET_ALARM -> alarmController.setAlarm(
                action.params["hour"]?.toIntOrNull() ?: 7,
                action.params["minute"]?.toIntOrNull() ?: 0
            )

            ActionType.PLAY_YOUTUBE -> mediaController.playYoutube(action.params["query"].orEmpty())

            ActionType.OPEN_MAPS -> mediaController.openMaps(action.params["query"].orEmpty())

            ActionType.OPEN_BROWSER -> mediaController.openBrowser(action.params["query"].orEmpty())

            ActionType.SEND_EMAIL -> mediaController.sendEmail(
                action.params["recipient"].orEmpty(),
                action.params["subject"].orEmpty(),
                action.params["body"].orEmpty()
            )

            ActionType.DEVICE_INFO -> deviceInfoController.batteryPercent()
        }
    }
}
