package tech.mmarca.openvitals.data.sync

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.WeightRecord
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.domain.insights.basalMetabolicRateKcal
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.historyReadStart
import tech.mmarca.openvitals.healthconnect.withStrictHealthConnectReads

/**
 * The opt-in basal metabolic rate estimate: one BasalMetabolicRateRecord per
 * day, from the body profile by Mifflin-St Jeor, for days no other source
 * covers. Kept reconciled as the weight history changes. Mirrors
 * [StepDistanceBackfillService]: the last [HistorySyncScheduler] drain, and
 * run from settings on enable or on a profile change.
 */
@Singleton
class BmrEstimateService @Inject constructor(
    private val hc: HealthConnectManager,
    private val preferences: PreferencesRepository,
) {
    private val running = AtomicBoolean(false)

    @Volatile
    private var lastPass: Instant? = null

    suspend fun syncIncremental() = sync(force = false)

    suspend fun syncNow() = sync(force = true)

    /** True when the estimate could be written now: Health Connect is up and the grants are in. */
    suspend fun canWrite(): Boolean =
        hc.availability() == HealthConnectAvailability.AVAILABLE &&
            hc.grantedPermissions().containsAll(RequiredPermissions)

    /**
     * Removes every estimated record. It must finish: the feature is off, and what is left
     * would stay in Health Connect. So it is noted as pending first, it does not stop when
     * the caller's screen goes away, and a purge that did not finish is tried again by the
     * next pass ([sync]).
     */
    suspend fun purgeDerivedRecords() {
        preferences.bmrEstimatePurgePending = true
        if (!running.compareAndSet(false, true)) return
        try {
            if (hc.availability() != HealthConnectAvailability.AVAILABLE) return
            withContext(NonCancellable) {
                val today = LocalDate.now()
                hc.purgeEstimatedBmr(today.minusDays(HistoryLookbackDays)..today)
                lastPass = null
                preferences.bmrEstimatePurgePending = false
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Log.w(TAG, "BMR estimate purge failed", t)
        } finally {
            running.set(false)
        }
    }

    private suspend fun sync(force: Boolean) {
        if (!preferences.bmrEstimateEnabled) {
            // Off, but the records of a purge that never finished are still there.
            if (preferences.bmrEstimatePurgePending) purgeDerivedRecords()
            return
        }
        if (!running.compareAndSet(false, true)) return
        try {
            val now = Instant.now()
            val last = lastPass
            if (!force && last != null && Duration.between(last, now) < Throttle) return
            if (hc.availability() != HealthConnectAvailability.AVAILABLE) return
            // Own records only would hide another app's rate, and this pass would write over it.
            if (!hc.readsOtherAppsDataNow()) return

            val granted = hc.grantedPermissions()
            if (!granted.containsAll(RequiredPermissions)) return

            val today = LocalDate.now()
            val start = hc.historyReadStart(today.minusDays(EstimateWindowDays - 1), today, granted)
            // Strict: a failed weight read would look like "no weight", and the reconcile
            // would then delete every estimate. A failed read must abort the pass.
            withStrictHealthConnectReads {
                val profile = preferences.bodyProfile()
                val estimateByDay = estimatesByDay(profile, start, today, granted)
                hc.reconcileEstimatedBmr(window = start..today, estimateByDay = estimateByDay)
            }
            lastPass = now
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Log.w(TAG, "BMR estimate failed", t)
        } finally {
            running.set(false)
        }
    }

    /**
     * Each day uses the weight last measured on or before it, so an old day is not
     * rewritten with today's weight. A day before the first measurement falls back to
     * the declared profile weight. Height is the latest measurement, or the declared one.
     */
    private suspend fun estimatesByDay(
        profile: BodyProfile,
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): Map<LocalDate, Double> {
        val weights = if (readPermission(WeightRecord::class) in granted) {
            hc.readWeightEntries(start.minusDays(WeightLookbackDays).dayStart(), end.dayEndExclusive())
                .sortedBy(WeightEntry::time)
        } else {
            emptyList()
        }
        val heightCm = if (readPermission(HeightRecord::class) in granted) {
            hc.readLatestHeight() ?: profile.heightCm
        } else {
            profile.heightCm
        }
        val estimates = mutableMapOf<LocalDate, Double>()
        var day = start
        var weightIndex = 0
        var weightKg = profile.weightKg
        while (!day.isAfter(end)) {
            val dayEnd = day.dayEndExclusive()
            while (weightIndex < weights.size && weights[weightIndex].time < dayEnd) {
                weightKg = weights[weightIndex].weightKg
                weightIndex++
            }
            profile.copy(weightKg = weightKg, heightCm = heightCm).normalized(day)
                .basalMetabolicRateKcal(day)
                ?.let { estimates[day] = it }
            day = day.plusDays(1)
        }
        return estimates
    }

    companion object {
        private const val TAG = "BmrEstimate"
        private const val EstimateWindowDays = 90L
        private const val WeightLookbackDays = 365L
        private val Throttle: Duration = Duration.ofMinutes(30)

        /** Read to see other sources' rates, write to save the estimate. */
        val RequiredPermissions: Set<String> = setOf(
            HealthPermission.getReadPermission(BasalMetabolicRateRecord::class),
            HealthPermission.getWritePermission(BasalMetabolicRateRecord::class),
        )
    }
}
