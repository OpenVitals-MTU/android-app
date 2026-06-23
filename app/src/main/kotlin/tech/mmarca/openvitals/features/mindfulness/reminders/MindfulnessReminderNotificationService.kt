package tech.mmarca.openvitals.features.mindfulness.reminders

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
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import tech.mmarca.openvitals.MainActivity
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter

@Singleton
class MindfulnessReminderNotificationService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val unitFormatter: UnitFormatter,
) {
    init {
        createNotificationChannel()
    }

    @SuppressLint("MissingPermission")
    fun showMindfulnessReminder(currentMinutes: Double, dailyGoalMinutes: Double) {
        if (!MindfulnessReminderController.hasNotificationPermission(context)) return
        NotificationManagerCompat.from(context).notify(
            NotificationId,
            buildNotification(currentMinutes, dailyGoalMinutes),
        )
    }

    fun cancelReminderNotification() {
        NotificationManagerCompat.from(context).cancel(NotificationId)
    }

    private fun buildNotification(currentMinutes: Double, dailyGoalMinutes: Double): Notification {
        val current = unitFormatter.minutes(currentMinutes.roundToLong().coerceAtLeast(0L))
        val goal = unitFormatter.minutes(dailyGoalMinutes.roundToLong().coerceAtLeast(0L))
        val progressPercent = if (dailyGoalMinutes > 0.0) {
            ((currentMinutes / dailyGoalMinutes) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        val progressText = context.getString(
            R.string.mindfulness_reminder_notification_progress,
            current.text,
            goal.text,
        )
        val contentText = context.getString(
            R.string.mindfulness_reminder_notification_body,
            goal.text,
        )

        return NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_mindfulness_reminder)
            .setContentTitle(context.getString(R.string.mindfulness_reminder_notification_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(openAppPendingIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(100, progressPercent, false)
            .setSubText(progressText)
            .setColor(MindfulnessNotificationColor)
            .build()
    }

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
            context.getString(R.string.mindfulness_reminder_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.mindfulness_reminder_notification_channel_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

private const val ChannelId = "mindfulness_reminders"
private const val NotificationId = 4092
private const val RequestOpenApp = 21
private val MindfulnessNotificationColor = 0xFF8E6C8A.toInt()
