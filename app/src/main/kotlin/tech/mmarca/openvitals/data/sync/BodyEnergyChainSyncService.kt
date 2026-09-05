package tech.mmarca.openvitals.data.sync

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.data.repository.BodyEnergyBaselineCacheStore
import tech.mmarca.openvitals.data.repository.BodyEnergyChainSettlingDays
import tech.mmarca.openvitals.data.repository.BodyEnergyTimelineStore
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyRepository
import tech.mmarca.openvitals.data.repository.contract.BodyEnergyTimelineQuery
import tech.mmarca.openvitals.data.repository.contract.HealthRepository
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimelineAlgorithmVersion
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.RefreshMode

/** How many days back the warm window reaches. Matches the repository's chain lookback. */
const val BodyEnergyChainWarmDays = 14L

/**
 * Keeps a rolling window of Body Energy days computed and stored, so the
 * chain is almost never cold. The foreground fill is bounded to two days;
 * closing a week belongs here. Best-effort: failures are retried next pass.
 */
@Singleton
class BodyEnergyChainSyncService(
    private val repository: BodyEnergyRepository,
    private val store: BodyEnergyTimelineStore,
    private val baselineStore: BodyEnergyBaselineCacheStore,
    private val healthRepository: HealthRepository,
    private val preferencesRepository: PreferencesRepository,
    private val clock: () -> Instant = Instant::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
    private val windowDays: Long = BodyEnergyChainWarmDays,
    private val elapsedMillis: () -> Long = { System.nanoTime() / 1_000_000L },
) {
    @Inject
    constructor(
        repository: BodyEnergyRepository,
        store: BodyEnergyTimelineStore,
        baselineStore: BodyEnergyBaselineCacheStore,
        healthRepository: HealthRepository,
        preferencesRepository: PreferencesRepository,
    ) : this(
        repository = repository,
        store = store,
        baselineStore = baselineStore,
        healthRepository = healthRepository,
        preferencesRepository = preferencesRepository,
        clock = Instant::now,
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var inFlight: Deferred<Unit>? = null

    /**
     * Warms the chain. Concurrent calls share one run. [force] bypasses the
     * throttle for a caller that just made the stored chain wrong.
     */
    suspend fun syncAll(force: Boolean = false) {
        val deferred = lock.withLock {
            inFlight?.takeIf { it.isActive } ?: newRun(force).also { inFlight = it }
        }
        deferred.await()
    }

    private fun newRun(force: Boolean): Deferred<Unit> {
        lateinit var created: Deferred<Unit>
        created = scope.async(start = CoroutineStart.LAZY) {
            try {
                sync(force)
            } finally {
                lock.withLock { if (inFlight === created) inFlight = null }
            }
        }
        return created
    }

    private suspend fun sync(force: Boolean) {
        try {
            // One-shot cleanup of the retired SharedPreferences timelines.
            baselineStore.purgeLegacyTimelineEntries()

            if (healthRepository.availability() != HealthConnectAvailability.AVAILABLE) return
            val granted = healthRepository.grantedPermissions()
            if (ReadHeartRatePermission !in granted) return

            // Rows under a retired calibration are wrong, not stale: purge them.
            val signature = globalSignature(granted)
            if (store.storedGlobalSignature() != signature) {
                store.purgeAll()
                store.writeGlobalSignature(signature)
            }

            val now = clock()
            val lastPass = store.lastPassAt()
            if (!force && lastPass != null && Duration.between(lastPass, now) < Throttle) return

            val today = now.atZone(zone).toLocalDate()
            store.applyRetention(today)

            // Skip days the repository would serve from storage. The rule must
            // match the repository's: a day is worth revisiting only while it can
            // still gain late data, or once today's copy has aged past a day.
            val window = store.storedDaysBetween(
                today.minusDays(windowDays - 1),
                today.minusDays(1),
            )
            val freshEpochDays = window.filter { day ->
                today.toEpochDay() - day.date.toEpochDay() > BodyEnergyChainSettlingDays ||
                    Duration.between(day.generatedAt, now) < DayFreshness
            }.mapTo(mutableSetOf()) { it.date.toEpochDay() }

            // Oldest first: each day's seed must be stored before its successor.
            val startedAt = elapsedMillis()
            var completed = true
            for (back in (windowDays - 1) downTo 1L) {
                if (elapsedMillis() - startedAt >= PassBudgetMillis) {
                    completed = false
                    break
                }
                // Today is skipped: the foreground load owns it.
                val date = today.minusDays(back)
                if (date.toEpochDay() in freshEpochDays) continue
                repository.loadTimeline(
                    BodyEnergyTimelineQuery(
                        period = DatePeriod(date, date),
                        range = TimeRange.DAY,
                        refreshMode = RefreshMode.NORMAL,
                    )
                )
            }

            // Only a completed pass resets the throttle.
            if (completed) store.writeLastPassAt(now)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Log.w(TAG, "Body Energy warm pass failed, will retry", t)
        }
    }

    /**
     * The chain-wide validity stamp: algorithm version plus the shared
     * inputs. The learned gains are left out: a mismatch purges every stored
     * day, and the watch fit nudges gains on every sync.
     */
    private fun globalSignature(granted: Set<String>): String {
        val permissions = granted.sorted().joinToString(",")
        val configured = preferencesRepository.bodyEnergyCalibration().zoneSignature()
        return "v$BodyEnergyTimelineAlgorithmVersion|${configured.hashCode()}|${permissions.hashCode()}"
    }

    private companion object {
        private const val TAG = "BodyEnergyChainSync"

        /** A cold install walks the whole window at ~8 reads a day. A pass stops at the budget and resumes. */
        const val PassBudgetMillis = 90_000L

        /** Every screen open calls `syncAll`; this keeps five opens from five walks. */
        val Throttle: Duration = Duration.ofMinutes(30)

        /** How long a stored past day counts as fresh, matching the repository. */
        val DayFreshness: Duration = Duration.ofHours(24)

        val ReadHeartRatePermission: String =
            HealthPermission.getReadPermission(HeartRateRecord::class)
    }
}
