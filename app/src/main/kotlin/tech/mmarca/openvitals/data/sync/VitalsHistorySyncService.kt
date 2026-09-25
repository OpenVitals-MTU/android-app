package tech.mmarca.openvitals.data.sync

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlinx.coroutines.async
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyAggregateEntity
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.local.vitalscache.VitalsSyncCursorEntity
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.withStrictHealthConnectReads
import tech.mmarca.openvitals.data.local.vitalscache.VitalsCacheFingerprintKey
import tech.mmarca.openvitals.data.local.vitalscache.vitalsCacheFingerprint

/**
 * Keeps the vitals daily-aggregate cache current via the Changes API, one
 * token per type. The full history read is paid once; after that only
 * changed days are re-read. Deletions and expired tokens force a rebuild.
 * Freshness is cursor presence, not age.
 */
@Singleton
class VitalsHistorySyncService @Inject constructor(
    private val hc: HealthConnectManager,
    private val dao: VitalsDailyCacheDao,
) {
    // The run belongs to the process, not to the screen that asked for it. The first full
    // sync reads years of records. In a screen's scope, leaving the screen cancelled it, and
    // since it writes at the end, the next visit started again from nothing.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()
    private var inFlight: Deferred<Unit>? = null
    private var inFlightIsFull = false

    internal data class MetricSpec(
        val key: String,
        val recordType: KClass<out Record>,
        val readPermission: String,
        val read: suspend (LocalDate, LocalDate) -> List<VitalsDailyAggregateEntity>,
    )

    private fun specs(): List<MetricSpec> = listOf(
        MetricSpec(VitalsCacheKeys.BLOOD_PRESSURE, BloodPressureRecord::class, readPermission(BloodPressureRecord::class)) { start, end ->
            hc.readDailyBloodPressure(start.dayStart(), end.dayEndExclusive()).map { point ->
                VitalsDailyAggregateEntity(
                    metric = VitalsCacheKeys.BLOOD_PRESSURE,
                    epochDay = point.date.toEpochDay(),
                    valueSum = point.systolic * point.count,
                    secondarySum = point.diastolic * point.count,
                    sampleCount = point.count.toLong(),
                )
            }
        },
        singleValueSpec(VitalsCacheKeys.SPO2, OxygenSaturationRecord::class) { start, end -> hc.readDailySpO2(start, end) },
        singleValueSpec(VitalsCacheKeys.RESPIRATORY_RATE, RespiratoryRateRecord::class) { start, end -> hc.readDailyRespiratoryRate(start, end) },
        singleValueSpec(VitalsCacheKeys.BODY_TEMPERATURE, BodyTemperatureRecord::class) { start, end -> hc.readDailyBodyTemperature(start, end) },
        singleValueSpec(VitalsCacheKeys.VO2_MAX, Vo2MaxRecord::class) { start, end -> hc.readDailyVo2Max(start, end) },
        singleValueSpec(VitalsCacheKeys.BLOOD_GLUCOSE, BloodGlucoseRecord::class) { start, end -> hc.readDailyBloodGlucose(start, end) },
        singleValueSpec(VitalsCacheKeys.SKIN_TEMPERATURE, SkinTemperatureRecord::class) { start, end -> hc.readDailySkinTemperature(start, end) },
    )

    private fun singleValueSpec(
        key: String,
        recordType: KClass<out Record>,
        read: suspend (java.time.Instant, java.time.Instant) -> List<tech.mmarca.openvitals.domain.model.DailyVitalPoint>,
    ): MetricSpec = MetricSpec(key, recordType, readPermission(recordType)) { start, end ->
        read(start.dayStart(), end.dayEndExclusive()).map { point ->
            VitalsDailyAggregateEntity(
                metric = key,
                epochDay = point.date.toEpochDay(),
                valueSum = point.value * point.count,
                secondarySum = null,
                sampleCount = point.count.toLong(),
            )
        }
    }

    /** Full sync allowed: pays for a metric's first history rebuild when needed. */
    suspend fun syncAll() = join(incrementalOnly = false)

    /** Cheap drain only: returns immediately for metrics that never full-synced. */
    suspend fun syncIncremental() = join(incrementalOnly = true)

    /**
     * Waits for a run that covers what the caller asked for. Callers share one run. A caller
     * that is cancelled stops waiting; the run goes on.
     */
    private suspend fun join(incrementalOnly: Boolean) {
        while (true) {
            var covers = true
            val run = lock.withLock {
                val current = inFlight?.takeIf { it.isActive }
                if (current != null) {
                    // A drain does not do a first full sync. Wait for it, then run one.
                    covers = inFlightIsFull || incrementalOnly
                    current
                } else {
                    inFlightIsFull = !incrementalOnly
                    scope.async { sync(incrementalOnly) }.also { inFlight = it }
                }
            }
            run.await()
            if (covers) return
        }
    }

    private suspend fun sync(incrementalOnly: Boolean) {
        try {
            if (hc.availability() != HealthConnectAvailability.AVAILABLE) return
            // In the background without the grant, a read returns this app's own records only.
            if (!hc.readsOtherAppsDataNow()) return
            val granted = hc.grantedPermissions()
            val skinTemperatureAvailable = hc.isSkinTemperatureAvailable()
            dropCacheBuiltUnderOtherConditions(granted)
            // Strict: a failed read must abort the metric, not be cached as "no data".
            withStrictHealthConnectReads {
                specs().map { spec ->
                    async {
                        try {
                            if (spec.readPermission !in granted) return@async
                            if (spec.key == VitalsCacheKeys.SKIN_TEMPERATURE && !skinTemperatureAvailable) return@async
                            syncMetric(spec, incrementalOnly)
                        } catch (t: Throwable) {
                            if (t is kotlinx.coroutines.CancellationException) throw t
                            Log.w(TAG, "Vitals cache sync failed metric=${spec.key}", t)
                        }
                    }
                }.forEach { it.await() }
            }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            // The run is shared. One caller's wait must not rethrow what another's run hit.
            Log.w(TAG, "Vitals cache sync failed", t)
        }
    }

    /**
     * A change token only tells what records changed. It cannot tell that the user granted
     * the history permission, or moved to another time zone. The fingerprint does. On a
     * mismatch every metric loses its rows and its cursor, so reads go live until the next
     * full sync has rebuilt them.
     */
    private suspend fun dropCacheBuiltUnderOtherConditions(granted: Set<String>) {
        val current = vitalsCacheFingerprint(granted, ZoneId.systemDefault())
        val stored = dao.cursor(VitalsCacheFingerprintKey)?.changesToken
        if (stored == current) return
        // No fingerprint yet: a cache from before this rule. Adopt it as it is, or every
        // install would pay for a full rebuild on update.
        if (stored != null) specs().forEach { dao.purgeMetric(it.key) }
        dao.writeFullSync(VitalsSyncCursorEntity(VitalsCacheFingerprintKey, current, null))
    }

    private suspend fun syncMetric(spec: MetricSpec, incrementalOnly: Boolean) {
        val token = dao.cursor(spec.key)?.changesToken
        if (token.isNullOrEmpty()) {
            if (!incrementalOnly) fullSync(spec)
            return
        }
        incrementalSync(spec, token)
    }

    private suspend fun fullSync(spec: MetricSpec) {
        val today = LocalDate.now()
        val earliest = today.minusDays(HistoryLookbackDays)
        // Register the token before the slow read, so records written meanwhile are caught.
        val token = hc.getChangesToken(spec.recordType)
        // These reads load raw records. A per-minute metric is a million of them over the
        // full history, so read a chunk at a time and keep only its daily rows.
        val rows = mutableListOf<VitalsDailyAggregateEntity>()
        var chunkEnd = today
        while (!chunkEnd.isBefore(earliest)) {
            val chunkStart = maxOf(chunkEnd.minusDays(FullSyncChunkDays - 1), earliest)
            rows += spec.read(chunkStart, chunkEnd)
            chunkEnd = chunkStart.minusDays(1)
        }
        // One swap at the end, so a reader never sees half a history.
        dao.replaceMetric(spec.key, rows)
        dao.writeFullSync(
            VitalsSyncCursorEntity(
                metric = spec.key,
                changesToken = token.ifEmpty { null },
                lastFullSyncMillis = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun incrementalSync(spec: MetricSpec, startToken: String) {
        var token = startToken
        while (true) {
            val batch = hc.getChanges(token)
            if (batch.tokenExpired || batch.hasDeletions) {
                // Deletions carry no date, so the whole metric rebuilds. Same for an expired token.
                fullSync(spec)
                return
            }
            for (day in batch.upsertedDays) {
                recomputeDay(spec, day)
            }
            token = batch.nextToken
            // Persisted after the page is applied: a replay is harmless.
            dao.writeToken(spec.key, token)
            if (!batch.hasMore) break
        }
    }

    /**
     * Write-through hook: recomputes just [days] after the app's own write.
     * No-op without a cursor. Failures are swallowed; the next drain reconciles.
     */
    suspend fun patchDays(key: String, days: Set<LocalDate>) {
        try {
            val spec = specs().firstOrNull { it.key == key } ?: return
            dao.cursor(key) ?: return
            withStrictHealthConnectReads { days.forEach { recomputeDay(spec, it) } }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Log.w(TAG, "Vitals cache patch failed metric=$key", t)
        }
    }

    /** An empty read deletes the day, so the read must be a strict one: see [sync] and [patchDays]. */
    private suspend fun recomputeDay(spec: MetricSpec, day: LocalDate) {
        val row = spec.read(day, day).firstOrNull { it.epochDay == day.toEpochDay() }
        if (row == null) {
            dao.deleteDay(spec.key, day.toEpochDay())
        } else {
            dao.upsertDay(row)
        }
    }

    internal companion object {
        private const val TAG = "VitalsHistorySync"

        /** Days of raw records held at once per metric. Chunks end on local days, so no day is split. */
        const val FullSyncChunkDays = 30L
    }
}

/** Stable cache keys — deliberately independent of enum names so renames are safe. */
object VitalsCacheKeys {
    const val BLOOD_PRESSURE = "bloodPressure"
    const val SPO2 = "spo2"
    const val VO2_MAX = "vo2Max"
    const val RESPIRATORY_RATE = "respiratoryRate"
    const val BODY_TEMPERATURE = "bodyTemperature"
    const val BLOOD_GLUCOSE = "bloodGlucose"
    const val SKIN_TEMPERATURE = "skinTemperature"

    /** No cache spec behind it — HRV reads live; the key only names the metric. */
    const val HRV = "hrv"
}

/** How far back the daily-aggregate caches cover. */
internal const val HistoryLookbackDays = 730L

internal fun LocalDate.dayStart(): java.time.Instant =
    atStartOfDay(ZoneId.systemDefault()).toInstant()

internal fun LocalDate.dayEndExclusive(): java.time.Instant =
    plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

internal fun readPermission(recordType: KClass<out Record>): String =
    HealthPermission.getReadPermission(recordType)
