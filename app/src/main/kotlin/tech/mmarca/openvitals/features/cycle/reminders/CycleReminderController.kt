package tech.mmarca.openvitals.features.cycle.reminders

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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.data.repository.contract.CycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.CyclePreferences
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.domain.cycle.CycleEstimateResult
import tech.mmarca.openvitals.domain.model.CycleReminderConfig
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.withStrictHealthConnectReads

/**
 * Plans the three cycle alarms from the stored config and the recorded
 * history. Every path serialises on one mutex, so the last caller in is the
 * one that arms the alarms.
 */
@Singleton
class CycleReminderController @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: CyclePreferences,
    private val cycleRepository: CycleRepository,
    private val journal: CycleJournalRepository,
    private val hc: HealthConnectManager,
    private val notificationService: CycleReminderNotificationService,
    private val alarmManager: CycleReminderAlarmManager,
    dispatcherProvider: DispatcherProvider,
) : CycleReminderSettings {
    private val scope = CoroutineScope(SupervisorJob() + dispatcherProvider.io)
    private val scheduling = Mutex()

    override fun config(): CycleReminderConfig = preferences.cycleReminderConfig()

    override fun updateConfig(config: CycleReminderConfig) {
        val normalized = config.normalized()
        preferences.setCycleReminderConfig(normalized)
        applyConfig(normalized)
    }

    override fun applyStoredConfig() = applyConfig(preferences.cycleReminderConfig())

    fun applyConfig(config: CycleReminderConfig) {
        scope.launch { scheduling.withLock { reconcile(config) } }
    }

    fun handleReminderAlarm(type: CycleReminderType, onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                scheduling.withLock { handleAlarmNow(type) }
            } finally {
                onComplete()
            }
        }
    }

    fun restoreSchedule(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                scheduling.withLock { reconcile(preferences.cycleReminderConfig()) }
            } finally {
                onComplete()
            }
        }
    }

    private suspend fun handleAlarmNow(type: CycleReminderType) {
        val config = preferences.cycleReminderConfig()
        if (config.enabled && hasNotificationPermission(context) && shouldFire(type, config)) {
            notificationService.show(type, config)
        }
        reconcile(config)
    }

    /** The daily check-in stays quiet once today is logged; the others fire when armed. */
    private suspend fun shouldFire(type: CycleReminderType, config: CycleReminderConfig): Boolean = when (type) {
        CycleReminderType.DAILY_CHECK_IN -> config.dailyCheckInEnabled && !loggedToday()
        CycleReminderType.PERIOD_WINDOW -> config.periodWindowEnabled
        CycleReminderType.LATE_CYCLE -> config.lateCycleEnabled
    }

    private suspend fun reconcile(config: CycleReminderConfig) {
        if (!config.enabled || !hasNotificationPermission(context)) {
            CycleReminderType.entries.forEach(alarmManager::cancel)
            notificationService.cancelAll()
            return
        }
        val now = ZonedDateTime.now()
        if (config.dailyCheckInEnabled) {
            alarmManager.schedule(CycleReminderType.DAILY_CHECK_IN, CycleReminderSchedule.nextDailyCheckIn(now, config, loggedToday()))
        } else {
            alarmManager.cancel(CycleReminderType.DAILY_CHECK_IN)
        }
        if (!config.periodWindowEnabled) alarmManager.cancel(CycleReminderType.PERIOD_WINDOW)
        if (!config.lateCycleEnabled) alarmManager.cancel(CycleReminderType.LATE_CYCLE)
        if (!config.periodWindowEnabled && !config.lateCycleEnabled) return
        when (val read = currentEstimate()) {
            // The alarms already armed stand until the estimate can be read again.
            EstimateRead.Unavailable -> Log.i(TAG, "Estimate not readable now; the window and late alarms stay as they are")
            is EstimateRead.Read -> {
                val estimate = read.estimate
                scheduleOrCancel(
                    CycleReminderType.PERIOD_WINDOW,
                    if (config.periodWindowEnabled && estimate != null) CycleReminderSchedule.periodWindow(now, config, estimate) else null,
                )
                scheduleOrCancel(
                    CycleReminderType.LATE_CYCLE,
                    if (config.lateCycleEnabled && estimate != null) CycleReminderSchedule.lateCycle(now, config, estimate) else null,
                )
            }
        }
    }

    /** Posts the daily reminder now, as a scheduled fire would. Diagnostics only. */
    fun showTestReminder(onComplete: () -> Unit = {}) {
        scope.launch {
            try {
                notificationService.show(CycleReminderType.DAILY_CHECK_IN, preferences.cycleReminderConfig())
            } finally {
                onComplete()
            }
        }
    }

    private fun scheduleOrCancel(type: CycleReminderType, at: ZonedDateTime?) {
        if (at == null) alarmManager.cancel(type) else alarmManager.schedule(type, at)
    }

    /**
     * The journal row or the app's own bleeding record for today. Own records
     * need no background grant. Unreadable counts as not logged: reminding is
     * the safe default.
     */
    private suspend fun loggedToday(): Boolean {
        val today = LocalDate.now()
        return runCatching {
            withTimeoutOrNull(ReadTimeoutMillis) {
                withStrictHealthConnectReads {
                    journal.entry(today)?.hasObservations == true || cycleRepository.loadDayLog(today).bleeding != null
                }
            } ?: false
        }.onFailure { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Could not read today's log before the reminder", error)
        }.getOrDefault(false)
    }

    private sealed interface EstimateRead {
        /** Null: the read permission is missing, so there is nothing to plan on. */
        data class Read(val estimate: CycleEstimateResult?) : EstimateRead

        /** No grant, a timeout, or a failure. Not the same as "no estimate". */
        data object Unavailable : EstimateRead
    }

    /**
     * Strict and bounded. Without the background grant Health Connect answers
     * with this app's records only, and a period logged elsewhere would read
     * as late; a paused or rate-limited read answers empty. Neither may cancel
     * alarms that were right.
     */
    private suspend fun currentEstimate(): EstimateRead {
        if (!hc.readsOtherAppsDataNow()) return EstimateRead.Unavailable
        return runCatching {
            withTimeoutOrNull(ReadTimeoutMillis) {
                withStrictHealthConnectReads {
                    EstimateRead.Read(cycleRepository.loadCycleStatistics(LocalDate.now())?.estimate)
                }
            } ?: EstimateRead.Unavailable
        }.onFailure { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Could not read the estimate for the reminders", error)
        }.getOrDefault(EstimateRead.Unavailable)
    }

    companion object {
        private const val TAG = "CycleReminderController"

        /** An alarm holds its broadcast; a read may not hold it for long. */
        private const val ReadTimeoutMillis = 8_000L

        fun hasNotificationPermission(context: Context): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
    }
}
