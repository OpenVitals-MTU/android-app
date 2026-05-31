package tech.mmarca.openvitals.features.hydration.reminders

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import tech.mmarca.openvitals.MainActivity
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter

@Singleton
class HydrationReminderNotificationService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val unitFormatter: UnitFormatter,
) {
    init {
        createNotificationChannel()
    }

    @SuppressLint("MissingPermission")
    fun showHydrationReminder(currentLiters: Double, dailyGoalLiters: Double) {
        if (!HydrationReminderController.hasNotificationPermission(context)) return
        NotificationManagerCompat.from(context).notify(
            NotificationId,
            buildNotification(currentLiters, dailyGoalLiters),
        )
    }

    fun cancelReminderNotification() {
        NotificationManagerCompat.from(context).cancel(NotificationId)
    }

    private fun buildNotification(currentLiters: Double, dailyGoalLiters: Double): Notification {
        val current = unitFormatter.hydration(currentLiters)
        val goal = unitFormatter.hydration(dailyGoalLiters)
        val progressPercent = if (dailyGoalLiters > 0.0) {
            ((currentLiters / dailyGoalLiters) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            0
        }
        val progressText = context.getString(
            R.string.hydration_reminder_notification_progress,
            current.text,
            goal.text,
        )
        val contentText = context.getString(
            R.string.hydration_reminder_notification_body,
            current.text,
            goal.text,
        )

        return NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_stat_hydration_reminder)
            .setContentTitle(context.getString(R.string.hydration_reminder_notification_title))
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(openAppPendingIntent())
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setProgress(100, progressPercent, false)
            .setSubText(progressText)
            .setColor(HydrationNotificationColor)
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            ChannelId,
            context.getString(R.string.hydration_reminder_notification_channel),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.hydration_reminder_notification_channel_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}

private const val ChannelId = "hydration_reminders"
private const val NotificationId = 4091
private const val RequestOpenApp = 20
private val HydrationNotificationColor = 0xFF03A9F4.toInt()
