package tech.mmarca.openvitals.features.hydration.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HydrationReminderAlarmManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(triggerAt: ZonedDateTime): Boolean {
        val triggerAtMillis = triggerAt.toInstant().toEpochMilli()
        val nowMillis = System.currentTimeMillis()
        if (triggerAtMillis <= nowMillis) {
            Log.w(TAG, "Ignoring hydration reminder alarm in the past: $triggerAt")
            return false
        }

        val pendingIntent = reminderPendingIntent(PendingIntent.FLAG_UPDATE_CURRENT) ?: return false
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
        Log.d(TAG, "Scheduled hydration reminder alarm for $triggerAt")
        return true
    }

    fun cancel() {
        val pendingIntent = reminderPendingIntent(PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Cancelled hydration reminder alarm")
    }

    private fun reminderPendingIntent(extraFlags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            ReminderRequestCode,
            Intent(context, HydrationReminderReceiver::class.java).setAction(ActionHydrationReminder),
            extraFlags or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val TAG = "HydrationReminderAlarmManager"
        private const val ActionHydrationReminder = "tech.mmarca.openvitals.action.HYDRATION_REMINDER"
        private const val ReminderRequestCode = 4091
    }
}
