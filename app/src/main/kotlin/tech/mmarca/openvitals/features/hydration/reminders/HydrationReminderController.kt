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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.domain.model.HydrationReminderConfig
import tech.mmarca.openvitals.data.repository.contract.HydrationRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.features.manualentry.hydration.HydrationDrinkLogOutcome
import tech.mmarca.openvitals.features.manualentry.hydration.isValidHydrationContainerMilliliters
import tech.mmarca.openvitals.features.manualentry.hydration.writeHydrationAndNutritionEntry

@Singleton
class HydrationReminderController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferencesRepository: PreferencesRepository,
    private val hydrationRepository: HydrationRepository,
    private val nutritionRepository: NutritionRepository,
    private val notificationService: HydrationReminderNotificationService,
    private val alarmManager: HydrationReminderAlarmManager,
    dispatcherProvider: DispatcherProvider,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)

    /** Serialises scheduling, so the last caller in is the one that arms the alarm. */
    private val scheduling = Mutex()

    fun config(): HydrationReminderConfig =
        preferencesRepository.hydrationReminderConfig()

    fun updateConfig(config: HydrationReminderConfig) {
        val normalized = config.normalized()
        preferencesRepository.setHydrationReminderConfig(normalized)
        applyConfig(normalized)
    }

    fun applyConfig(config: HydrationReminderConfig = preferencesRepository.hydrationReminderConfig()) {
        scope.launch {
            scheduling.withLock { applyConfigNow(config) }
        }
    }

    fun handleReminderAlarm(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                scheduling.withLock { handleReminderAlarmNow() }
            } finally {
                onComplete()
            }
        }
    }

    fun restoreSchedule(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                scheduling.withLock {
                    val config = preferencesRepository.hydrationReminderConfig()
                    if (config.enabled) {
                        applyConfigNow(config)
                    } else {
                        clearReminder()
                    }
                }
            } finally {
                onComplete()
            }
        }
    }

    fun hideReminderNotification() {
        notificationService.cancelReminderNotification()
    }

    /**
     * Logs plain water from a notification action. Remembers the size before
     * the write; re-anchors the schedule only after a real write. Never throws.
     */
    fun handleQuickAdd(milliliters: Double, onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                if (!isValidHydrationContainerMilliliters(milliliters)) return@launch
                hydrationRepository.setLastCustomHydrationAmountMilliliters(milliliters)
                hydrationRepository.recordRecentHydrationAmountMilliliters(milliliters)
                val outcome = runCatching {
                    writeHydrationAndNutritionEntry(
                        repository = hydrationRepository,
                        nutritionRepository = nutritionRepository,
                        rawLiters = milliliters / 1000.0,
                        hydrationMultiplier = 1.0,
                        nutritionName = null,
                        nutrientValues = emptyMap(),
                        // Plain water writes no nutrition record.
                        canWriteNutrition = false,
                    )
                }.getOrElse { error ->
                    Log.w(TAG, "Hydration quick-add failed", error)
                    null
                }
                notificationService.cancelReminderNotification()
                val wroteHydration =
                    (outcome as? HydrationDrinkLogOutcome.Success)?.value?.wroteHydration == true
                if (wroteHydration) {
                    runCatching {
                        scheduling.withLock {
                            applyConfigNow(preferencesRepository.hydrationReminderConfig())
                        }
                    }.onFailure { error ->
                        // Re-anchoring is a nicety; never surface an error over a landed drink.
                        Log.w(TAG, "Hydration quick-add re-anchor failed", error)
                    }
                }
            } finally {
                onComplete()
            }
        }
    }

    /** Posts the reminder immediately, as a scheduled fire would. Diagnostics only. */
    fun showTestReminder(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                if (!hasNotificationPermission(context)) return@launch
                notificationService.showHydrationReminder(
                    todayHydrationLiters(),
                    preferencesRepository.hydrationDailyGoalLiters,
                )
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
        scheduleNextReminder(
            normalized,
            dailyGoalMet = isDailyGoalMet(),
            lastIntake = lastIntakeTime(),
        )
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
        scheduleNextReminder(config, dailyGoalMet = goalMet, lastIntake = lastIntakeTime())
    }

    /** When the last drink was logged, so the countdown is measured from it. Null on failure. */
    private suspend fun lastIntakeTime(): ZonedDateTime? = runCatching {
        val today = LocalDate.now()
        hydrationRepository.loadHydrationEntries(today.minusDays(1), today)
            .maxOfOrNull { it.startTime }
            ?.atZone(java.time.ZoneId.systemDefault())
    }.getOrNull()

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

    private fun scheduleNextReminder(
        config: HydrationReminderConfig,
        dailyGoalMet: Boolean,
        lastIntake: ZonedDateTime? = null,
    ) {
        val triggerAt = calculateNextHydrationReminderTime(
            now = ZonedDateTime.now(),
            config = config,
            dailyGoalMet = dailyGoalMet,
            lastIntake = lastIntake,
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
