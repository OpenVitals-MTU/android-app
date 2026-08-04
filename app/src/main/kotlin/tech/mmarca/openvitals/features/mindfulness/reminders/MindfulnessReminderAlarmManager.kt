package tech.mmarca.openvitals.features.mindfulness.reminders

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
class MindfulnessReminderAlarmManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(triggerAt: ZonedDateTime): Boolean {
        val triggerAtMillis = triggerAt.toInstant().toEpochMilli()
        val nowMillis = System.currentTimeMillis()
        if (triggerAtMillis <= nowMillis) {
            Log.w(TAG, "Ignoring mindfulness reminder alarm in the past")
            return false
        }

        val pendingIntent = reminderPendingIntent(PendingIntent.FLAG_UPDATE_CURRENT) ?: return false
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent,
        )
        Log.d(TAG, "Scheduled mindfulness reminder alarm")
        return true
    }

    fun cancel() {
        val pendingIntent = reminderPendingIntent(PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Cancelled mindfulness reminder alarm")
    }

    private fun reminderPendingIntent(extraFlags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            ReminderRequestCode,
            Intent(context, MindfulnessReminderReceiver::class.java).setAction(ActionMindfulnessReminder),
            extraFlags or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val TAG = "MindfulnessReminderAlarmManager"
        private const val ActionMindfulnessReminder = "tech.mmarca.openvitals.action.MINDFULNESS_REMINDER"
        private const val ReminderRequestCode = 4092
    }
}
