package tech.mmarca.openvitals.features.hydration.reminders

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
import tech.mmarca.openvitals.MainActivity
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.manualentry.hydration.isValidHydrationContainerMilliliters

@Singleton
class HydrationReminderNotificationService @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val unitFormatter: UnitFormatter,
    private val preferencesRepository: PreferencesRepository,
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
            .apply {
                quickAddAmountsMilliliters().forEachIndexed { index, milliliters ->
                    addAction(
                        0,
                        context.getString(
                            R.string.hydration_reminder_quick_add_action,
                            quickAddLabel(milliliters),
                        ),
                        quickAddPendingIntent(index, milliliters),
                    )
                }
            }
            .build()
    }

    /** The one-tap volumes: the last two cup sizes, padded to [QuickAddActionCount]. */
    private fun quickAddAmountsMilliliters(): List<Double> =
        hydrationQuickAddAmountsMilliliters(
            recentAmountsMilliliters = preferencesRepository.recentHydrationAmountsMilliliters(),
            lastCustomAmountMilliliters = preferencesRepository.lastCustomHydrationAmountMilliliters(),
        )

    private fun quickAddLabel(milliliters: Double): String =
        hydrationQuickAddLabel(
            milliliters = milliliters,
            unitSystem = unitFormatter.unitSystem(UnitQuantity.HYDRATION),
            unitFormatter = unitFormatter,
        )

    private fun quickAddPendingIntent(index: Int, milliliters: Double): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            RequestQuickAddBase + index,
            Intent(context, HydrationQuickAddReceiver::class.java).apply {
                action = HydrationQuickAddReceiver.ACTION_QUICK_ADD
                putExtra(HydrationQuickAddReceiver.EXTRA_AMOUNT_MILLILITERS, milliliters)
            },
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

/** The one-tap volumes: the last two cup sizes, padded to [QuickAddActionCount] distinct sizes. */
internal fun hydrationQuickAddAmountsMilliliters(
    recentAmountsMilliliters: List<Double>,
    lastCustomAmountMilliliters: Double?,
): List<Double> {
    val amounts = mutableListOf<Double>()
    fun add(value: Double?) {
        if (amounts.size >= QuickAddActionCount) return
        if (value == null || !isValidHydrationContainerMilliliters(value)) return
        if (value in amounts) return
        amounts += value
    }
    recentAmountsMilliliters.forEach { add(it) }
    add(lastCustomAmountMilliliters)
    FallbackQuickAddAmountsMilliliters.forEach { add(it) }
    return amounts
}

/** Metric formats as whole ml, which reads better than "0.35 L" on a button. */
internal fun hydrationQuickAddLabel(
    milliliters: Double,
    unitSystem: UnitSystem,
    unitFormatter: UnitFormatter,
): String = when (unitSystem) {
    UnitSystem.METRIC -> "${milliliters.roundToInt()} ml"
    UnitSystem.IMPERIAL -> unitFormatter.hydration(milliliters / 1000.0).text
}

internal const val ChannelId = "hydration_reminders"
internal const val NotificationId = 4091
private const val RequestOpenApp = 20
private const val RequestQuickAddBase = 21
private const val QuickAddActionCount = 2
private val HydrationNotificationColor = 0xFF03A9F4.toInt()

/** When the user has never logged anything: a glass and a bottle. */
private val FallbackQuickAddAmountsMilliliters = listOf(250.0, 500.0)
