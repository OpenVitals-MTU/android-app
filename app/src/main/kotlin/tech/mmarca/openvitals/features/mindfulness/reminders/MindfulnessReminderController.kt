package tech.mmarca.openvitals.features.mindfulness.reminders

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
import tech.mmarca.openvitals.domain.insights.MetricDailyGoalKey
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.domain.model.MindfulnessReminderConfig
import tech.mmarca.openvitals.data.repository.contract.MindfulnessRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository

@Singleton
class MindfulnessReminderController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val mindfulnessRepository: MindfulnessRepository,
    private val notificationService: MindfulnessReminderNotificationService,
    private val alarmManager: MindfulnessReminderAlarmManager,
    dispatcherProvider: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    fun config(): MindfulnessReminderConfig =
        preferencesRepository.mindfulnessReminderConfig()

    fun updateConfig(config: MindfulnessReminderConfig) {
        val normalized = config.normalized()
        preferencesRepository.setMindfulnessReminderConfig(normalized)
        applyConfig(normalized)
    }

    fun applyConfig(config: MindfulnessReminderConfig = preferencesRepository.mindfulnessReminderConfig()) {
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
                val config = preferencesRepository.mindfulnessReminderConfig()
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

    private suspend fun applyConfigNow(config: MindfulnessReminderConfig) {
        val normalized = config.normalized()
        if (!normalized.enabled || !hasNotificationPermission(context)) {
            clearReminder()
            return
        }
        scheduleNextReminder(normalized, dailyGoalMet = isDailyGoalMet())
    }

    private suspend fun handleReminderAlarmNow() {
        val config = preferencesRepository.mindfulnessReminderConfig().normalized()
        if (!config.enabled || !hasNotificationPermission(context)) {
            clearReminder()
            return
        }

        val currentMinutes = todayMindfulnessMinutes()
        val dailyGoalMinutes = preferencesRepository.dailyGoalFor(MetricDailyGoalKey.MINDFULNESS_MINUTES)
        val goalMet = dailyGoalMinutes > 0.0 && currentMinutes >= dailyGoalMinutes
        if (!goalMet) {
            notificationService.showMindfulnessReminder(currentMinutes, dailyGoalMinutes)
        }
        scheduleNextReminder(config, dailyGoalMet = goalMet)
    }

    private suspend fun isDailyGoalMet(): Boolean {
        val dailyGoalMinutes = preferencesRepository.dailyGoalFor(MetricDailyGoalKey.MINDFULNESS_MINUTES)
        return dailyGoalMinutes > 0.0 && todayMindfulnessMinutes() >= dailyGoalMinutes
    }

    private suspend fun todayMindfulnessMinutes(): Double {
        val today = LocalDate.now()
        return runCatching {
            mindfulnessRepository.loadMindfulnessSessions(today, today)
                .sumOf { session -> session.durationMs.coerceAtLeast(0L) }
                .toDouble() / MillisPerMinute
        }.onFailure { error ->
            Log.w(TAG, "Could not read today's mindfulness before reminder", error)
        }.getOrDefault(0.0)
    }

    private fun scheduleNextReminder(config: MindfulnessReminderConfig, dailyGoalMet: Boolean) {
        val triggerAt = calculateNextMindfulnessReminderTime(
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
        private const val TAG = "MindfulnessReminderController"
        private const val MillisPerMinute = 60_000.0

        fun hasNotificationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PackageManager.PERMISSION_GRANTED
    }
}
