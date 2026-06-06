package tech.mmarca.openvitals.features.mindfulness.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MindfulnessReminderBootReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: MindfulnessReminderController

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
)
