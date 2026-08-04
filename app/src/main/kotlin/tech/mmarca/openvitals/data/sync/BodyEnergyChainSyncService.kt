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

/**
 * How many days back the warm window reaches. Matches the repository's chain
 * lookback, so any day inside the window finds a stored anchor.
 */
const val BodyEnergyChainWarmDays = 14L

/**
 * Keeps a rolling window of recent Body Energy days computed and stored, so the
 * chain the detail screen walks is almost never cold.
 *
 * Body Energy carries across midnight: each day opens where the previous one
 * closed. Without a warm window, a user who last opened the app a week ago would
 * find no stored predecessor and the day would have to restart at the neutral
 * score — the foreground gap fill is deliberately bounded to two days because
 * each day costs ~8 Health Connect reads, and closing a week of them while
 * someone waits on a screen is not acceptable. That work belongs here.
 *
 * Best-effort throughout, like [CaloriesHistorySyncService]: every failure is
 * swallowed and retried on the next pass, never surfaced.
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
     * Warm the chain. Concurrent calls share one run.
     *
     * Pass [force] to bypass the throttle — for a caller that has just made the
     * stored chain wrong (an import back-filling days) rather than one merely
     * opening a screen. A forced call still joins an in-flight run: whatever is
     * already walking will pick up the holes.
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
            // One-shot cleanup of the retired SharedPreferences timelines. This
            // is the natural home for it: already best-effort, and it runs
            // before any chain work needs the prefs.
            baselineStore.purgeLegacyTimelineEntries()

            if (healthRepository.availability() != HealthConnectAvailability.AVAILABLE) return
            val granted = healthRepository.grantedPermissions()
            if (ReadHeartRatePermission !in granted) return

            // Rows computed under a retired calibration are wrong, not merely
            // stale, so a signature change purges rather than letting them age
            // out.
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

            // Days the repository would serve from storage cost nothing to keep.
            // Skipping them here rather than letting its cache check do it saves
            // a permission round-trip and a store read per day — on the common
            // warm pass that is the difference between a dozen calls and none.
            //
            // The rule must match the repository's, or this would keep
            // recomputing settled days it would happily have served: a day is
            // worth revisiting only while it can still gain late-arriving data,
            // or while today's copy of it has aged past a day.
            val window = store.storedDaysBetween(
                today.minusDays(windowDays - 1),
                today.minusDays(1),
            )
            val freshEpochDays = window.filter { day ->
                today.toEpochDay() - day.date.toEpochDay() > BodyEnergyChainSettlingDays ||
                    Duration.between(day.generatedAt, now) < DayFreshness
            }.mapTo(mutableSetOf()) { it.date.toEpochDay() }

            // Oldest first, and that order is load-bearing: each day's seed must
            // already be stored by the time its successor is computed.
            val startedAt = elapsedMillis()
            var completed = true
            for (back in (windowDays - 1) downTo 1L) {
                if (elapsedMillis() - startedAt >= PassBudgetMillis) {
                    completed = false
                    break
                }
                // Today is skipped: the foreground load owns it, and recomputing
                // it here would fight its 15-minute freshness window.
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

            // Only a completed pass resets the throttle; a budget-truncated one
            // lets the next open pick up the remaining days immediately.
            if (completed) store.writeLastPassAt(now)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (t: Throwable) {
            Log.w(TAG, "Body Energy warm pass failed, will retry", t)
        }
    }

    /**
     * The chain-wide validity stamp: algorithm version plus the configured and
     * permission inputs every day shares. The per-day signature additionally
     * folds in the body profile, whose value varies by date — that belongs on
     * the row, not here.
     *
     * The personal gains are deliberately NOT in it, because a mismatch here
     * purges every stored day and every stored bucket. Gains can move by a
     * fraction of a percent (historically the watch fit nudged them per
     * observation), so including them would delete up to
     * `BodyEnergyBucketRetentionDays` of history on a sub-percent change,
     * which is no way to build the weekly view those buckets exist for. A gain change does not make a stored
     * row wrong enough to destroy it: the per-day signature still refuses to
     * SERVE one, so it is recomputed on demand, and the seed lookup can still
     * anchor on it in the meantime.
     */
    private fun globalSignature(granted: Set<String>): String {
        val permissions = granted.sorted().joinToString(",")
        val configured = preferencesRepository.bodyEnergyCalibration().zoneSignature()
        return "v$BodyEnergyTimelineAlgorithmVersion|${configured.hashCode()}|${permissions.hashCode()}"
    }

    private companion object {
        private const val TAG = "BodyEnergyChainSync"

        /**
         * A cold install has to walk the whole window at ~8 Health Connect reads
         * a day. That is fine in the background but must not run away, so a pass
         * stops when the budget is spent and resumes where it left off — the
         * days it already wrote are skipped as fresh next time.
         */
        const val PassBudgetMillis = 90_000L

        /**
         * Every screen open calls `syncAll`; without this, opening Body Energy
         * five times in a minute would re-walk the window five times.
         */
        val Throttle: Duration = Duration.ofMinutes(30)

        /**
         * How long a stored past day counts as fresh, matching the repository's
         * own past-day staleness rule. Only used to skip work the repository
         * would otherwise skip anyway; the repository stays the authority.
         */
        val DayFreshness: Duration = Duration.ofHours(24)

        val ReadHeartRatePermission: String =
            HealthPermission.getReadPermission(HeartRateRecord::class)
    }
}
