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
    ): T = withRateLimitRetry(operation, fallback, block)

    suspend fun <T> withNullableLogging(
        operation: String,
        block: suspend () -> T?,
    ): T? = withRateLimitRetry(operation, null, block)

    private suspend fun <T> withRateLimitRetry(
        operation: String,
        fallback: T,
        block: suspend () -> T,
    ): T {
        val safeOperation = operation.privacySafeOperationName()
        var hasRetriedRateLimit = false
        var result: Result<T>? = null

        while (result == null) {
            if (!syncEnabled()) {
                Log.d(TAG, "Skipping $safeOperation - Health Connect sync paused")
                return fallback
            }
            waitForActiveRateLimit(safeOperation)
            Log.d(TAG, "Starting $safeOperation ${diagnosticsSummary()}")

            try {
                result = readSemaphore.withPermit {
                    withContext(Dispatchers.IO) {
                        Result.success(block().also {
                            Log.d(TAG, "Finished $safeOperation successfully")
                        })
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
                } else {
                    Log.e(TAG, "Failed $safeOperation ${diagnosticsSummary()}", t)
                }
                result = Result.success(fallback)
            }
        }

        return result.getOrThrow()
    }

    private suspend fun waitForActiveRateLimit(operation: String) {
        try {
            HealthConnectRateLimitBackoff.throwIfActive(rateLimitMessage)
        } catch (rateLimit: HealthConnectRateLimitException) {
            Log.w(TAG, "Waiting to retry $operation after Health Connect rate limit")
            delay(rateLimit.retryAfterMillis)
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
    }
}

private fun String.privacySafeOperationName(): String =
    substringBefore('[')

/**
 * The local date a fixed-24h aggregate bucket belongs to.
 *
 * `Duration.ofDays(1)` slicing stays instant-aligned across DST transitions, so
 * bucket boundaries drift up to an hour off local midnight. Dating a bucket by
 * its *start* then puts two buckets on the fall-back date and none on the
 * spring-forward date — a doubled and a missing day on every year heatmap. The
 * bucket's midpoint always falls inside the one local day it covers, so every
 * date gets exactly one full bucket. (Period slicing would cut true local days,
 * but it resolves records against their stored zone offset and undercounts.)
 *
 * That one-bucket-per-date guarantee covers *full* buckets only — the range's
 * clipped tail bucket can share a date with the full bucket before it, so read
 * buckets through [byLocalDate] rather than dating them one by one.
 */
internal fun dayBucketDate(start: Instant, end: Instant, zone: ZoneId): LocalDate =
    Instant.ofEpochMilli((start.toEpochMilli() + end.toEpochMilli()) / 2)
        .atZone(zone)
        .toLocalDate()

/**
 * One local date and every aggregate bucket that landed on it.
 *
 * [dayBucketDate] gives each *full* bucket its own date, but Health Connect
 * clips the last bucket of a range to the requested end. A range spanning a
 * DST fall-back is an hour longer than a whole number of 24h slices, so that
 * leftover hour becomes a second, one-hour bucket covering the final day's
 * 23:00–24:00 — a date the preceding full bucket already owns. Keying those
 * into a map lets the sliver win and blanks the day; emitting one row each
 * leaves a duplicate date for the caller to trip over. Folding the day's
 * buckets together is what actually reconstructs the day.
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

    /**
     * Total of an additive metric, or null when no bucket of the day carried
     * one — for metrics where absent and zero mean different things.
     */
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
     * Duration-weighted mean of an averaged metric.
     *
     * Health Connect reports a mean per bucket and no sample count, so the
     * bucket's own span is the only weight available. It assumes samples fall
     * evenly through the bucket, which is wrong in detail but keeps a clipped
     * one-hour sliver from counting as much as the day it trails.
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
