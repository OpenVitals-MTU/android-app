package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.roundToLong
import kotlinx.coroutines.withContext

internal class HealthConnectReaderSupport(
    private val clientProvider: () -> HealthConnectClient,
    private val diagnostics: HealthConnectDiagnostics,
    private val rateLimitMessage: (Long) -> String,
    private val syncEnabled: () -> Boolean = { true },
) {
    private val readSemaphore = Semaphore(MaxConcurrentReads)

    fun client(): HealthConnectClient = clientProvider()

    fun diagnosticsSummary(): String = diagnostics.summary()

    suspend fun <T> withLogging(
        operation: String,
        fallback: T,
        block: suspend () -> T,
    ): T = withRateLimitGuard(operation, fallback, block)

    suspend fun <T> withNullableLogging(
        operation: String,
        block: suspend () -> T?,
    ): T? = withRateLimitGuard(operation, null, block)

    /**
     * Like [withLogging], but a failure that survives the retry is rethrown
     * and a backoff is waited out. For a background transfer whose silence
     * would lie: a truncated stream once reported "completed".
     */
    suspend fun <T> withLoggingOrThrow(
        operation: String,
        block: suspend () -> T,
    ): T {
        val safeOperation = operation.privacySafeOperationName()
        var hasRetriedRateLimit = false
        while (true) {
            check(syncEnabled()) { "Health Connect access is paused" }
            val backoffMillis = HealthConnectRateLimitBackoff.remainingMillis()
            if (backoffMillis > 0L) {
                Log.w(TAG, "Waiting to retry $safeOperation after Health Connect rate limit")
                delay(backoffMillis)
            }
            Log.d(TAG, "Starting $safeOperation ${diagnosticsSummary()}")
            try {
                return readSemaphore.withPermit {
                    withContext(Dispatchers.IO) {
                        block().also { Log.d(TAG, "Finished $safeOperation successfully") }
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                if (HealthConnectRateLimitBackoff.isRateLimitFailure(t)) {
                    val rateLimit = HealthConnectRateLimitBackoff.markRateLimited(t, rateLimitMessage)
                    Log.w(TAG, "Rate limited $safeOperation ${diagnosticsSummary()}", t)
                    if (!hasRetriedRateLimit) {
                        hasRetriedRateLimit = true
                        delay(rateLimit.retryAfterMillis)
                        continue
                    }
                    throw rateLimit
                }
                Log.e(TAG, "Failed $safeOperation ${diagnosticsSummary()}", t)
                throw t
            }
        }
    }

    /**
     * One attempt, bounded in how long it may wait. The backoff is process
     * global, so a read that would wait longer than [MaxRateLimitWaitMillis]
     * returns the fallback instead of holding the screen for minutes.
     */
    private suspend fun <T> withRateLimitGuard(
        operation: String,
        fallback: T,
        block: suspend () -> T,
    ): T {
        val safeOperation = operation.privacySafeOperationName()
        if (!syncEnabled()) {
            Log.d(TAG, "Skipping $safeOperation - Health Connect sync paused")
            return fallback
        }
        val backoffMillis = HealthConnectRateLimitBackoff.remainingMillis()
        if (backoffMillis > MaxRateLimitWaitMillis) {
            Log.w(TAG, "Skipping $safeOperation - Health Connect rate limited for ${backoffMillis}ms more")
            return fallback
        }
        if (backoffMillis > 0L) {
            Log.w(TAG, "Waiting to retry $safeOperation after Health Connect rate limit")
            delay(backoffMillis)
        }
        Log.d(TAG, "Starting $safeOperation ${diagnosticsSummary()}")

        return try {
            readSemaphore.withPermit {
                withContext(Dispatchers.IO) {
                    block().also { Log.d(TAG, "Finished $safeOperation successfully") }
                }
            }
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            if (HealthConnectRateLimitBackoff.isRateLimitFailure(t)) {
                HealthConnectRateLimitBackoff.markRateLimited(t, rateLimitMessage)
                Log.w(TAG, "Rate limited $safeOperation ${diagnosticsSummary()}", t)
            } else {
                Log.e(TAG, "Failed $safeOperation ${diagnosticsSummary()}", t)
            }
            fallback
        }
    }

    fun dayRange(date: LocalDate): Pair<Instant, Instant> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant()
        val end = if (date == LocalDate.now(zone)) {
            Instant.now()
        } else {
            date.plusDays(1).atStartOfDay(zone).toInstant()
        }
        return start to end
    }

    private companion object {
        private const val TAG = "HealthConnectManager"
        private const val MaxConcurrentReads = 2

        /** The longest a single read may sit on the rate-limit backoff. */
        private const val MaxRateLimitWaitMillis = 2_000L
    }
}

private fun String.privacySafeOperationName(): String =
    substringBefore('[')

/**
 * The local date a fixed-24h bucket belongs to. Buckets drift up to an hour
 * off local midnight across DST, so the midpoint dates them: it always falls
 * inside the one local day the bucket covers. Full buckets only; a clipped
 * tail bucket can share a date, so read through [byLocalDate].
 */
internal fun dayBucketDate(start: Instant, end: Instant, zone: ZoneId): LocalDate =
    Instant.ofEpochMilli((start.toEpochMilli() + end.toEpochMilli()) / 2)
        .atZone(zone)
        .toLocalDate()

/**
 * One local date and every bucket that landed on it. A DST fall-back leaves
 * a one-hour tail bucket on a date the previous full bucket owns; folding
 * the day's buckets together reconstructs the day.
 */
internal class DayBuckets(
    val date: LocalDate,
    val buckets: List<AggregationResultGroupedByDuration>,
) {
    /** Total of an additive metric (steps, volume, energy) over the day. */
    fun total(selector: (AggregationResult) -> Double?): Double =
        buckets.sumOf { selector(it.result) ?: 0.0 }

    fun totalLong(selector: (AggregationResult) -> Long?): Long =
        buckets.sumOf { selector(it.result) ?: 0L }

    /** Total of an additive metric, or null when no bucket carried one. */
    fun totalOrNull(selector: (AggregationResult) -> Double?): Double? {
        val values = buckets.mapNotNull { selector(it.result) }
        return if (values.isEmpty()) null else values.sum()
    }

    /** True when any bucket of the day satisfies [predicate]. */
    fun any(predicate: (AggregationResult) -> Boolean): Boolean =
        buckets.any { predicate(it.result) }

    /** Lowest reading of a per-sample metric, ignoring buckets without one. */
    fun lowest(selector: (AggregationResult) -> Long?): Long? =
        buckets.mapNotNull { selector(it.result) }.minOrNull()

    fun highest(selector: (AggregationResult) -> Long?): Long? =
        buckets.mapNotNull { selector(it.result) }.maxOrNull()

    /**
     * Duration-weighted mean of an averaged metric. Health Connect gives no
     * sample count, so the bucket span is the only weight available.
     */
    fun weightedAverage(selector: (AggregationResult) -> Long?): Long? {
        var weighted = 0.0
        var totalMillis = 0L
        buckets.forEach { bucket ->
            val value = selector(bucket.result) ?: return@forEach
            val millis = bucket.endTime.toEpochMilli() - bucket.startTime.toEpochMilli()
            if (millis <= 0L) return@forEach
            weighted += value.toDouble() * millis
            totalMillis += millis
        }
        return if (totalMillis == 0L) null else (weighted / totalMillis).roundToLong()
    }
}

/** Groups 24h aggregate buckets onto the local dates they cover, in date order. */
internal fun List<AggregationResultGroupedByDuration>.byLocalDate(
    zone: ZoneId,
): List<DayBuckets> =
    groupBy { dayBucketDate(it.startTime, it.endTime, zone) }
        .toSortedMap()
        .map { (date, buckets) -> DayBuckets(date, buckets) }

/**
 * The widest range one day-bucketed aggregate request may cover. The
 * response is one Binder parcel that grows with days times metrics; a
 * year-long request hit TransactionTooLargeException. A quarter year stays
 * around a quarter of the shared 1MB budget.
 */
internal const val DailyAggregateMaxQueryDays = 122L

/** Chunk size for hour-bucketed reads: ~504 buckets per request, inside the Binder budget. */
internal const val HourlyAggregateMaxQueryDays = 21L

/** Splits an inclusive date range into chunks of at most [maxDays]. Empty for inverted ranges. */
internal fun dailyAggregateDateChunks(
    startDate: LocalDate,
    endDate: LocalDate,
    maxDays: Long = DailyAggregateMaxQueryDays,
): List<Pair<LocalDate, LocalDate>> {
    if (endDate.isBefore(startDate) || maxDays <= 0L) return emptyList()

    val chunks = mutableListOf<Pair<LocalDate, LocalDate>>()
    var chunkStart = startDate
    while (!chunkStart.isAfter(endDate)) {
        val chunkEnd = minOf(chunkStart.plusDays(maxDays - 1), endDate)
        chunks += chunkStart to chunkEnd
        chunkStart = chunkEnd.plusDays(1)
    }
    return chunks
}
