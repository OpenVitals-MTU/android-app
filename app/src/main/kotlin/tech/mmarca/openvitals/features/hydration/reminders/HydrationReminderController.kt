package tech.mmarca.openvitals.features.hydration.reminders

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.data.repository.HydrationRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository

@Singleton
class HydrationReminderController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val hydrationRepository: HydrationRepository,
    private val notificationService: HydrationReminderNotificationService,
    private val alarmManager: HydrationReminderAlarmManager,
    dispatcherProvider: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    fun config(): HydrationReminderConfig =
        preferencesRepository.hydrationReminderConfig()

    fun updateConfig(config: HydrationReminderConfig) {
        val normalized = config.normalized()
        preferencesRepository.setHydrationReminderConfig(normalized)
        applyConfig(normalized)
    }

    fun applyConfig(config: HydrationReminderConfig = preferencesRepository.hydrationReminderConfig()) {
        scope.launch {
            applyConfigNow(config)
        }
    }

    fun handleReminderAlarm(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                handleReminderAlarmNow()
            } finally {
                onComplete()
            }
        }
    }

    fun restoreSchedule(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                val config = preferencesRepository.hydrationReminderConfig()
                if (config.enabled) {
                    applyConfigNow(config)
                } else {
                    clearReminder()
                }
            } finally {
                onComplete()
            }
        }
    }

    private suspend fun applyConfigNow(config: HydrationReminderConfig) {
        val normalized = config.normalized()
        if (!normalized.enabled || !hasNotificationPermission(context)) {
            clearReminder()
            return
        }
        scheduleNextReminder(normalized, dailyGoalMet = isDailyGoalMet())
    }

    private suspend fun handleReminderAlarmNow() {
        val config = preferencesRepository.hydrationReminderConfig().normalized()
        if (!config.enabled || !hasNotificationPermission(context)) {
            clearReminder()
            return
        }

        val now = ZonedDateTime.now()
        val currentLiters = todayHydrationLiters()
        val dailyGoalLiters = preferencesRepository.hydrationDailyGoalLiters
        val goalMet = dailyGoalLiters > 0.0 && currentLiters >= dailyGoalLiters
        if (!goalMet && isWithinHydrationReminderActiveHours(now.toLocalTime(), config)) {
            notificationService.showHydrationReminder(currentLiters, dailyGoalLiters)
        }
        scheduleNextReminder(config, dailyGoalMet = goalMet)
    }

    private suspend fun isDailyGoalMet(): Boolean {
        val dailyGoalLiters = preferencesRepository.hydrationDailyGoalLiters
        return dailyGoalLiters > 0.0 && todayHydrationLiters() >= dailyGoalLiters
    }

    private suspend fun todayHydrationLiters(): Double {
        val today = LocalDate.now()
        return runCatching {
            hydrationRepository.loadDailyHydration(today, today).sumOf { it.liters }
        }.onFailure { error ->
            Log.w(TAG, "Could not read today's hydration before reminder", error)
        }.getOrDefault(0.0)
    }

    private fun scheduleNextReminder(config: HydrationReminderConfig, dailyGoalMet: Boolean) {
        val triggerAt = calculateNextHydrationReminderTime(
            now = ZonedDateTime.now(),
            config = config,
            dailyGoalMet = dailyGoalMet,
        )
        alarmManager.schedule(triggerAt)
    }

    private fun clearReminder() {
        alarmManager.cancel()
        notificationService.cancelReminderNotification()
    }

    companion object {
        private const val TAG = "HydrationReminderController"

        fun hasNotificationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
    }
}
