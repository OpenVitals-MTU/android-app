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
     * Like [withLogging] but a failure that survives the rate-limit retry is
     * RETHROWN instead of degraded to a fallback, and a backoff is WAITED OUT
     * rather than declined. For callers whose silence would lie — a sync stream
     * that quietly truncated on rate limiting let the session report
     * "completed" for a transfer that wasn't.
     *
     * The waiting is the difference from [withLogging] and it is deliberate:
     * this path is a long background transfer with no screen behind it, so a
     * minute spent riding out a throttle costs nothing anyone is watching, and
     * resuming beats restarting the transfer.
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
     * One attempt, with a hard bound on how long it may spend not making it.
     *
     * Everything on a screen comes through here, and the rate-limit backoff is
     * process-global and armed a minute at a time — so one throttled call is
     * seen by every read the screen fans out. Sitting that minute out per read
     * is what turned a throttle into a dashboard that showed a spinner for
     * several minutes and then came up blank anyway: the wait happened before
     * [readSemaphore] so it did not even serialise, the retry waited a second
     * minute, and a re-arm from any concurrent read pushed the deadline out
     * again underneath all of them.
     *
     * So a read that would have to wait longer than [MaxRateLimitWaitMillis]
     * returns its caller's fallback instead. The data is no worse than it would
     * have been after the wait — the quota is spent either way — and the screen
     * gets to say so while it is still true, rather than holding the UI hostage
     * to a number the user is never shown.
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

        /**
         * The longest a single read may sit on the rate-limit backoff.
         *
         * Short enough that a whole screen's fan-out still settles inside one
         * frame budget's worth of patience rather than the minute the backoff
         * is armed for.
         */
        private const val MaxRateLimitWaitMillis = 2_000L
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

/**
 * The widest date range a single day-bucketed aggregate request may cover.
 *
 * A grouped-by-day aggregate response is one Binder parcel: every bucket
 * carries its requested metrics plus their data origins, so the parcel grows
 * with days × metrics. A year-long six-metric request measured ~800KB on a
 * dense dataset — `TransactionTooLargeException: data parcel size 811824
 * bytes` — and the 1MB Binder buffer is shared across a process's in-flight
 * transactions, so two concurrent long reads can fail even when each alone
 * would squeak through. A quarter-year slice keeps the worst response around
 * a quarter of the budget.
 */
internal const val DailyAggregateMaxQueryDays = 122L

/**
 * Chunk size for hour-bucketed aggregate reads (daily heart-rate summaries).
 * 21 days is ~504 buckets per request — roughly four times the bucket count of
 * a [DailyAggregateMaxQueryDays] daily read, still comfortably inside the
 * shared Binder budget, while a year stays under twenty sequential requests.
 */
internal const val HourlyAggregateMaxQueryDays = 21L

/**
 * Splits `[startDate, endDate]` (inclusive) into consecutive inclusive chunks
 * of at most [maxDays] days, for day-bucketed aggregate reads that must not
 * exceed [DailyAggregateMaxQueryDays] per request. Empty for inverted ranges.
 */
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
