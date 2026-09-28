package tech.mmarca.openvitals.features.cycle.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class CycleReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: CycleReminderController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ActionPillTaken) {
            val pendingResult = goAsync()
            controller.markPillTaken(LocalDate.now()) { pendingResult.finish() }
            return
        }
        val type = intent.getStringExtra(ExtraType)?.let { name -> CycleReminderType.entries.firstOrNull { it.name == name } }
            ?: return
        val pendingResult = goAsync()
        controller.handleReminderAlarm(type) { pendingResult.finish() }
    }

    companion object {
        const val ExtraType = "tech.mmarca.openvitals.extra.CYCLE_REMINDER_TYPE"

        /** The pill notification's Taken action. */
        const val ActionPillTaken = "tech.mmarca.openvitals.action.CYCLE_PILL_TAKEN"
    }
}

@AndroidEntryPoint
class CycleReminderBootReceiver : BroadcastReceiver() {
    @Inject lateinit var controller: CycleReminderController

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in RestorableScheduleActions) return
        val pendingResult = goAsync()
        controller.restoreSchedule { pendingResult.finish() }
    }
}

private val RestorableScheduleActions = setOf(
    Intent.ACTION_BOOT_COMPLETED,
    Intent.ACTION_MY_PACKAGE_REPLACED,
    // The alarms are wall-clock-anchored, so a zone or clock shift needs a re-arm.
    Intent.ACTION_TIMEZONE_CHANGED,
    Intent.ACTION_TIME_CHANGED,
)
