package tech.mmarca.openvitals.features.cycle.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** One wall-clock alarm per reminder type. */
@Singleton
class CycleReminderAlarmManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    private val alarmManager: AlarmManager
        get() = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(type: CycleReminderType, triggerAt: ZonedDateTime): Boolean {
        val triggerAtMillis = triggerAt.toInstant().toEpochMilli()
        if (triggerAtMillis <= System.currentTimeMillis()) {
            Log.w(TAG, "Ignoring cycle reminder alarm in the past type=$type")
            return false
        }
        val pendingIntent = pendingIntent(type, PendingIntent.FLAG_UPDATE_CURRENT) ?: return false
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        Log.d(TAG, "Scheduled cycle reminder alarm type=$type")
        return true
    }

    fun cancel(type: CycleReminderType) {
        val pendingIntent = pendingIntent(type, PendingIntent.FLAG_NO_CREATE) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
        Log.d(TAG, "Cancelled cycle reminder alarm type=$type")
    }

    private fun pendingIntent(type: CycleReminderType, extraFlags: Int): PendingIntent? =
        PendingIntent.getBroadcast(
            context,
            RequestCodeBase + type.ordinal,
            Intent(context, CycleReminderReceiver::class.java)
                .setAction(ActionCycleReminder)
                .putExtra(CycleReminderReceiver.ExtraType, type.name),
            extraFlags or PendingIntent.FLAG_IMMUTABLE,
        )

    companion object {
        private const val TAG = "CycleReminderAlarmManager"
        private const val ActionCycleReminder = "tech.mmarca.openvitals.action.CYCLE_REMINDER"
        private const val RequestCodeBase = 4120
    }
}
