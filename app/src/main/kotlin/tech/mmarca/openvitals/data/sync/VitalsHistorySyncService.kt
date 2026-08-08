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
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyAggregateEntity
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.local.vitalscache.VitalsSyncCursorEntity
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/**
 * Keeps the local vitals daily-aggregate cache current via the Health Connect
 * Changes API, one token per record type.
 *
 * The expensive part — reading a metric's whole history raw — is paid ONCE per
 * metric, in [syncAll]'s full rebuild. After that, each poll of the changes
 * token names the local days that gained or changed records, and only those
 * days are re-read. Deletions carry no date, so any deletion (and an expired
 * token) falls back to the full rebuild.
 *
 * Freshness is cursor presence, not age: readers trust the cached rows exactly
 * while a cursor row with a token exists, and this service rebuilds whenever it
 * does not.
 */
@Singleton
class VitalsHistorySyncService @Inject constructor(
    private val hc: HealthConnectManager,
    private val dao: VitalsDailyCacheDao,
) {
    private val running = AtomicBoolean(false)

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
    suspend fun syncAll() = sync(incrementalOnly = false)

    /** Cheap drain only: returns immediately for metrics that never full-synced. */
    suspend fun syncIncremental() = sync(incrementalOnly = true)

    private suspend fun sync(incrementalOnly: Boolean) {
        if (!running.compareAndSet(false, true)) return
        try {
            if (hc.availability() != HealthConnectAvailability.AVAILABLE) return
            val granted = hc.grantedPermissions()
            val skinTemperatureAvailable = hc.isSkinTemperatureAvailable()
            coroutineScope {
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
        } finally {
            running.set(false)
        }
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
        // Register the token BEFORE the slow history read: records written while
        // the read runs are then caught by the next incremental drain instead of
        // falling into neither snapshot.
        val token = hc.getChangesToken(spec.recordType)
        val rows = spec.read(earliest, today)
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
                // Deletions carry only an id — the affected day is unknowable,
                // so the whole metric rebuilds. Same for an expired token.
                fullSync(spec)
                return
            }
            for (day in batch.upsertedDays) {
                recomputeDay(spec, day)
            }
            token = batch.nextToken
            // Persisted after the page's days are applied: a crash mid-page
            // replays the page, and the absolute recompute makes that harmless.
            dao.writeToken(spec.key, token)
            if (!batch.hasMore) break
        }
    }

    /**
     * Write-through hook: recomputes just [days] after the app's own write,
     * update, or delete. No-op without a cursor — a partial cache the readers
     * would trust must never be seeded outside a full sync. Failures are
     * swallowed: a patch must never fail the write, and the next drain
     * reconciles anyway. The changes token is deliberately untouched.
     */
    suspend fun patchDays(key: String, days: Set<LocalDate>) {
        try {
            val spec = specs().firstOrNull { it.key == key } ?: return
            dao.cursor(key) ?: return
            days.forEach { recomputeDay(spec, it) }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Log.w(TAG, "Vitals cache patch failed metric=$key", t)
        }
    }

    private suspend fun recomputeDay(spec: MetricSpec, day: LocalDate) {
        val row = spec.read(day, day).firstOrNull { it.epochDay == day.toEpochDay() }
        if (row == null) {
            dao.deleteDay(spec.key, day.toEpochDay())
        } else {
            dao.upsertDay(row)
        }
    }

    private companion object {
        private const val TAG = "VitalsHistorySync"
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

    /**
     * The `.v2` suffix is the cache-format version: rows written before the
     * synthesized-basal fix carry Health Connect's BMR baseline as burned days,
     * and bumping the key makes the cursor lookup miss, forcing the rebuild
     * that rewrites them.
     */
    const val CALORIES_BURNED = "totalCaloriesBurned.v2"
    val LEGACY_CALORIES_BURNED = listOf("totalCaloriesBurned")
}

/** How far back the daily-aggregate caches cover. */
internal const val HistoryLookbackDays = 730L

internal fun LocalDate.dayStart(): java.time.Instant =
    atStartOfDay(ZoneId.systemDefault()).toInstant()

internal fun LocalDate.dayEndExclusive(): java.time.Instant =
    plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

internal fun readPermission(recordType: KClass<out Record>): String =
    HealthPermission.getReadPermission(recordType)
