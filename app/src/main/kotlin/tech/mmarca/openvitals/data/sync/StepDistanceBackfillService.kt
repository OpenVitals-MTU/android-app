package tech.mmarca.openvitals.data.sync

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.StepsRecord
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.preferences.StrideLength
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/**
 * The opt-in "distance from steps" backfill: one daily DistanceRecord for
 * days with steps and no other distance, kept reconciled. The last
 * [HistorySyncScheduler] drain, and run from settings on enable.
 */
@Singleton
class StepDistanceBackfillService @Inject constructor(
    private val hc: HealthConnectManager,
    private val preferences: PreferencesRepository,
) {
    private val running = AtomicBoolean(false)

    @Volatile
    private var lastPass: Instant? = null

    suspend fun syncIncremental() = sync(force = false)

    suspend fun syncNow() = sync(force = true)

    suspend fun purgeDerivedRecords() {
        if (!running.compareAndSet(false, true)) return
        try {
            if (hc.availability() != HealthConnectAvailability.AVAILABLE) return
            val today = LocalDate.now()
            hc.purgeStepDerivedDistance(today.minusDays(HistoryLookbackDays)..today)
            lastPass = null
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Log.w(TAG, "Step distance purge failed", t)
        } finally {
            running.set(false)
        }
    }

    private suspend fun sync(force: Boolean) {
        if (!preferences.stepDistanceBackfillEnabled) return
        if (!running.compareAndSet(false, true)) return
        try {
            val now = Instant.now()
            val last = lastPass
            if (!force && last != null && Duration.between(last, now) < Throttle) return
            if (hc.availability() != HealthConnectAvailability.AVAILABLE) return

            val granted = hc.grantedPermissions()
            val required = setOf(
                readPermission(StepsRecord::class),
                readPermission(DistanceRecord::class),
                HealthPermission.getWritePermission(DistanceRecord::class),
            )
            if (!granted.containsAll(required)) return

            val today = LocalDate.now()
            val start = backfillStart(today, granted)
            val stepsByDay = hc.readDailySteps(
                startDate = start,
                endDate = today,
                includeSteps = true,
                includeDistance = false,
            ).associate { it.date to it.steps }

            hc.reconcileStepDerivedDistance(
                window = start..today,
                stepsByDay = stepsByDay,
                strideMeters = StrideLength.normalize(preferences.strideLengthMeters),
            )
            lastPass = now
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Log.w(TAG, "Step distance backfill failed", t)
        } finally {
            running.set(false)
        }
    }

    private fun backfillStart(today: LocalDate, granted: Set<String>): LocalDate {
        val start = today.minusDays(BackfillWindowDays - 1)
        val historyPermissionRequired =
            HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY in hc.additionalDataAccessPermissions
        return if (
            historyPermissionRequired &&
            HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY !in granted
        ) {
            maxOf(start, today.minusDays(29))
        } else {
            start
        }
    }

    private companion object {
        const val TAG = "StepDistanceBackfill"
        const val BackfillWindowDays = 90L
        val Throttle: Duration = Duration.ofMinutes(30)
    }
}
