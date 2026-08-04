package tech.mmarca.openvitals.data.sync

import android.util.Log
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyAggregateEntity
import tech.mmarca.openvitals.data.local.vitalscache.VitalsDailyCacheDao
import tech.mmarca.openvitals.data.local.vitalscache.VitalsSyncCursorEntity
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager

/**
 * Keeps the daily calories-burned cache current, same shape as
 * [VitalsHistorySyncService] but for one metric with two quirks:
 *
 *  - The full-history read is CHUNKED (365 days at a time, newest first): a
 *    single multi-year TotalCaloriesBurned aggregate can throw or take minutes.
 *    The newest chunk atomically replaces the metric; older chunks upsert onto it.
 *  - Only recorded totals are cached (`includeEstimatedCalories = false`), and
 *    only positive-burn days get rows — consumers zero-fill missing days, and
 *    the synthesized-basal filter in the reader keeps never-tracked days out.
 *
 * The app never writes TotalCaloriesBurned itself, but a workout changes what
 * Health Connect derives for the day — the activity write-through patches those
 * days, and this service reconciles everything else.
 */
@Singleton
class CaloriesHistorySyncService @Inject constructor(
    private val hc: HealthConnectManager,
    private val dao: VitalsDailyCacheDao,
) {
    private val running = AtomicBoolean(false)

    suspend fun syncAll() = sync(incrementalOnly = false)

    suspend fun syncIncremental() = sync(incrementalOnly = true)

    private suspend fun sync(incrementalOnly: Boolean) {
        if (!running.compareAndSet(false, true)) return
        try {
            if (hc.availability() != HealthConnectAvailability.AVAILABLE) return
            if (readPermission(TotalCaloriesBurnedRecord::class) !in hc.grantedPermissions()) return
            val token = dao.cursor(VitalsCacheKeys.CALORIES_BURNED)?.changesToken
            if (token.isNullOrEmpty()) {
                if (!incrementalOnly) fullSync()
                return
            }
            incrementalSync(token)
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Log.w(TAG, "Calories cache sync failed", t)
        } finally {
            running.set(false)
        }
    }

    private suspend fun fullSync() {
        for (legacy in VitalsCacheKeys.LEGACY_CALORIES_BURNED) {
            dao.purgeMetric(legacy)
        }
        val today = LocalDate.now()
        val earliest = today.minusDays(HistoryLookbackDays)
        val token = hc.getChangesToken(TotalCaloriesBurnedRecord::class)

        var chunkEnd = today
        var isNewestChunk = true
        while (!chunkEnd.isBefore(earliest)) {
            val chunkStart = maxOf(chunkEnd.minusDays(ChunkDays - 1), earliest)
            val rows = readDays(chunkStart, chunkEnd)
            if (isNewestChunk) {
                dao.replaceMetric(VitalsCacheKeys.CALORIES_BURNED, rows)
            } else {
                rows.forEach { dao.upsertDay(it) }
            }
            isNewestChunk = false
            chunkEnd = chunkStart.minusDays(1)
        }
        dao.writeFullSync(
            VitalsSyncCursorEntity(
                metric = VitalsCacheKeys.CALORIES_BURNED,
                changesToken = token.ifEmpty { null },
                lastFullSyncMillis = System.currentTimeMillis(),
            ),
        )
    }

    private suspend fun incrementalSync(startToken: String) {
        var token = startToken
        while (true) {
            val batch = hc.getChanges(token)
            if (batch.tokenExpired || batch.hasDeletions) {
                fullSync()
                return
            }
            for (day in batch.upsertedDays) {
                recomputeDay(day)
            }
            token = batch.nextToken
            dao.writeToken(VitalsCacheKeys.CALORIES_BURNED, token)
            if (!batch.hasMore) break
        }
    }

    /**
     * Write-through hook: a workout write changes what Health Connect derives
     * for its day. No-op without a cursor; failures swallowed — the write must
     * not fail, and the next drain reconciles. Token untouched.
     */
    suspend fun patchDays(days: Set<LocalDate>) {
        try {
            dao.cursor(VitalsCacheKeys.CALORIES_BURNED) ?: return
            days.forEach { recomputeDay(it) }
        } catch (t: Throwable) {
            if (t is kotlinx.coroutines.CancellationException) throw t
            Log.w(TAG, "Calories cache patch failed", t)
        }
    }

    private suspend fun recomputeDay(day: LocalDate) {
        val row = readDays(day, day).firstOrNull { it.epochDay == day.toEpochDay() }
        if (row == null) {
            dao.deleteDay(VitalsCacheKeys.CALORIES_BURNED, day.toEpochDay())
        } else {
            dao.upsertDay(row)
        }
    }

    /**
     * Only positive-burn days become rows, summed by epoch day so a DST-clipped
     * tail bucket sharing a date cannot violate the primary key.
     */
    private suspend fun readDays(start: LocalDate, end: LocalDate): List<VitalsDailyAggregateEntity> =
        hc.readDailyNutrition(
            startDate = start,
            endDate = end,
            includeHydration = false,
            includeEstimatedCalories = false,
        )
            .filter { it.caloriesBurnedKcal > 0.0 }
            .groupBy { it.date.toEpochDay() }
            .map { (epochDay, days) ->
                VitalsDailyAggregateEntity(
                    metric = VitalsCacheKeys.CALORIES_BURNED,
                    epochDay = epochDay,
                    valueSum = days.sumOf { it.caloriesBurnedKcal },
                    secondarySum = null,
                    sampleCount = 1,
                )
            }
            .sortedBy { it.epochDay }

    private companion object {
        private const val TAG = "CaloriesHistorySync"
        private const val ChunkDays = 365L
    }
}
