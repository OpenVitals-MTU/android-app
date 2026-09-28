package tech.mmarca.openvitals.features.cycle.reminders

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.MainActivity
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.CycleReminderConfig
import tech.mmarca.openvitals.domain.model.CycleReminderVisibility
import tech.mmarca.openvitals.domain.model.PillDay

/**
 * Posts a cycle reminder. The lock-screen version is always neutral; what an
 * unlocked screen shows follows the visibility setting.
 */
@Singleton
class CycleReminderNotificationService @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    init {
        createNotificationChannel()
    }

    /** [pillDay] is today's place in the scheme, for the pill reminder's descriptive text. */
    @SuppressLint("MissingPermission")
    fun show(type: CycleReminderType, config: CycleReminderConfig, pillDay: PillDay? = null) {
        if (!CycleReminderController.hasNotificationPermission(context)) return
        NotificationManagerCompat.from(context).notify(notificationId(type), build(type, config, pillDay))
    }

    fun cancel(type: CycleReminderType) {
        NotificationManagerCompat.from(context).cancel(notificationId(type))
    }

    /** The three behind the master switch. The pill reminder is cancelled on its own. */
    fun cancelCycleReminders() {
        CycleReminderType.entries.forEach { if (it != CycleReminderType.PILL) cancel(it) }
    }

    private fun build(type: CycleReminderType, config: CycleReminderConfig, pillDay: PillDay?): Notification {
        val (title, body) = content(type, config, pillDay)
        val public = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_cycle_reminder)
            .setContentTitle(context.getString(R.string.cycle_reminder_public_title))
            .setContentText(context.getString(R.string.cycle_reminder_public_body))
            .build()
        return NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_cycle_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(openAppPendingIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public)
            .setColor(CycleNotificationColor)
            .apply {
                if (type == CycleReminderType.PILL) {
                    addAction(0, context.getString(R.string.cycle_reminder_pill_taken_action), pillTakenPendingIntent())
                }
            }
            .build()
    }

    private fun content(type: CycleReminderType, config: CycleReminderConfig, pillDay: PillDay?): Pair<String, String> =
        when (config.visibility) {
            CycleReminderVisibility.CONCEALED -> concealed(type)
            CycleReminderVisibility.CUSTOM -> Pair(
                config.customTitle.ifBlank { concealed(type).first },
                config.customBody.ifBlank { concealed(type).second },
            )
            CycleReminderVisibility.DESCRIPTIVE -> when (type) {
                CycleReminderType.DAILY_CHECK_IN ->
                    context.getString(R.string.cycle_reminder_daily_title) to context.getString(R.string.cycle_reminder_daily_body)
                CycleReminderType.PERIOD_WINDOW ->
                    context.getString(R.string.cycle_reminder_window_title) to context.getString(R.string.cycle_reminder_window_body)
                CycleReminderType.LATE_CYCLE ->
                    context.getString(R.string.cycle_reminder_late_title) to context.getString(R.string.cycle_reminder_late_body)
                CycleReminderType.PILL -> context.getString(R.string.cycle_reminder_pill_title) to pillBody(pillDay)
            }
        }

    private fun pillBody(pillDay: PillDay?): String =
        if (pillDay == null) {
            context.getString(R.string.cycle_reminder_pill_body_plain)
        } else {
            context.getString(R.string.cycle_reminder_pill_body, pillDay.dayOfPhase, pillDay.phaseLength)
        }

    /** Neutral copy. The pill's version says nothing about a daily log either. */
    private fun concealed(type: CycleReminderType): Pair<String, String> {
        val body = if (type == CycleReminderType.PILL) R.string.cycle_reminder_pill_concealed_body else R.string.cycle_reminder_concealed_body
        return context.getString(R.string.cycle_reminder_concealed_title) to context.getString(body)
    }

    private fun pillTakenPendingIntent(): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            RequestPillTaken,
            Intent(context, CycleReminderReceiver::class.java).setAction(CycleReminderReceiver.ActionPillTaken),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun openAppPendingIntent(): PendingIntent =
        PendingIntent.getActivity(
            context,
            RequestOpenApp,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            ChannelId,
            context.getString(R.string.cycle_reminders_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.cycle_reminders_notification_channel_desc)
            enableVibration(true)
            setShowBadge(true)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun notificationId(type: CycleReminderType): Int = NotificationIdBase + type.ordinal

    private companion object {
        const val ChannelId = "cycle_reminders"
        const val NotificationIdBase = 4120
        const val RequestOpenApp = 23
        const val RequestPillTaken = 24
        val CycleNotificationColor = 0xFFBE5C85.toInt()
    }
}
