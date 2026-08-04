package tech.mmarca.openvitals.features.hydration.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HydrationReminderBootReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: HydrationReminderController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RestorableScheduleActions) return
        val pendingResult = goAsync()
        controller.restoreSchedule {
            pendingResult.finish()
        }
    }
}

private val RestorableScheduleActions = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
    // The alarms are wall-clock-anchored, so a zone or clock shift needs a
    // re-arm or the reminder fires at the old absolute instant.
    Intent.ACTION_TIMEZONE_CHANGED,
    Intent.ACTION_TIME_CHANGED,
)
