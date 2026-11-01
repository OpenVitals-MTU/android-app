package tech.mmarca.openvitals.healthconnect

import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.aggregate.AggregationResultGroupedByDuration
import java.time.Instant
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.math.roundToLong
import kotlinx.coroutines.withContext
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import tech.mmarca.openvitals.domain.model.MaxInsightAggregateBuckets

/**
 * Marks a block whose Health Connect reads must fail loudly.
 *
 * A screen wants an empty state when a read fails, so the readers return a fallback. A
 * writer must not take "the read failed" for "there is no data" and save it: a rate limit
 * once emptied 730 days of cached history and stamped the sync complete. Inside
 * [withStrictHealthConnectReads] every guarded read throws instead.
 */
internal class StrictHealthConnectReads private constructor() :
    AbstractCoroutineContextElement(StrictHealthConnectReads) {
    companion object Key : CoroutineContext.Key<StrictHealthConnectReads> {
        val Element = StrictHealthConnectReads()
    }
}

internal suspend fun <T> withStrictHealthConnectReads(block: suspend CoroutineScope.() -> T): T =
    withContext(StrictHealthConnectReads.Element, block)

/**
 * Collects whether any guarded read under it came back degraded: skipped for
 * the backoff, or refused and answered with its fallback. A screen read never
 * throws for a refusal, so without this a tile cannot tell "no data" from
 * "Health Connect would not say"; with it, the dashboard knows which tiles
 * to ask for again once the backoff ends.
 */
internal class HealthConnectReadOutcome : AbstractCoroutineContextElement(HealthConnectReadOutcome) {
    companion object Key : CoroutineContext.Key<HealthConnectReadOutcome>

    private val degradedFlag = AtomicBoolean(false)

    val degraded: Boolean
        get() = degradedFlag.get()

    fun markDegraded() {
        degradedFlag.set(true)
    }
}

/** [block]'s value and whether a read under it was degraded. Child coroutines inherit the outcome. */
internal class HealthConnectReadResult<T>(val value: T, val degraded: Boolean)

internal suspend fun <T> withHealthConnectReadOutcome(
    block: suspend CoroutineScope.() -> T,
): HealthConnectReadResult<T> {
    val outcome = HealthConnectReadOutcome()
    val value = withContext(outcome, block)
    return HealthConnectReadResult(value, outcome.degraded)
}

private suspend fun markReadDegraded() {
    currentCoroutineContext()[HealthConnectReadOutcome]?.markDegraded()
}

internal class HealthConnectReaderSupport(
    private val clientProvider: () -> HealthConnectClient,
    private val diagnostics: HealthConnectDiagnostics,
    private val rateLimitMessage: (Long) -> String,
    private val syncEnabled: () -> Boolean = { true },
    /** Every read hops here. A test passes its own, so the whole read runs on test time. */
    private val dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    /** The clock the backoff is read with. A test binds it to the scheduler so a wait ends. */
    private val nowMillis: () -> Long = System::currentTimeMillis,
    /** Gap between call starts, in ms. Zero on the platform provider; the standalone app gets a breath. */
    pacingGapMillis: Long = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        HealthConnectCallPacer.StandaloneProviderGapMillis
    } else {
        0L
    },
) {
    private val readSemaphore = Semaphore(MaxConcurrentReads)
    private val pacer = HealthConnectCallPacer(nowMillis, pacingGapMillis)

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
            // The deadline moves while this sleeps: another reader's hit escalates it.
            var backoffMillis = HealthConnectRateLimitBackoff.remainingMillis(nowMillis())
            while (backoffMillis > 0L) {
                Log.w(TAG, "Waiting to retry $safeOperation after Health Connect rate limit")
                delay(backoffMillis)
                backoffMillis = HealthConnectRateLimitBackoff.remainingMillis(nowMillis())
            }
            var startedAt = 0L
            try {
                return readSemaphore.withPermit {
                    delay(pacer.reserve(HealthConnectRateLimitBackoff.hasBeenRateLimited))
                    Log.d(TAG, "Starting $safeOperation ${diagnosticsSummary()}")
                    HealthConnectCallCounters.started(safeOperation)
                    startedAt = nowMillis()
                    withContext(dispatchers.io) {
                        block().also {
                            Log.d(TAG, "Finished $safeOperation successfully")
                            HealthConnectCallCounters.succeeded(safeOperation)
                        }
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                if (HealthConnectRateLimitBackoff.isRateLimitFailure(t)) {
                    HealthConnectCallCounters.rateLimited(safeOperation)
                    val rateLimit = HealthConnectRateLimitBackoff.markRateLimited(
                        cause = t,
                        messageForRetryAfter = rateLimitMessage,
                        nowMillis = nowMillis(),
                        readStartedAtMillis = startedAt,
                    )
                    Log.w(TAG, "Rate limited $safeOperation ${diagnosticsSummary()}", t)
                    if (!hasRetriedRateLimit) {
                        hasRetriedRateLimit = true
                        continue
                    }
                    throw rateLimit
                }
                HealthConnectCallCounters.failed(safeOperation)
                Log.e(TAG, "Failed $safeOperation ${diagnosticsSummary()}", t)
                throw t
            }
        }
    }

    /**
     * One attempt, bounded in how long it may wait. The backoff is process
     * global, so a read that would wait longer than [MaxRateLimitWaitMillis]
     * returns the fallback instead of holding the screen for minutes.
     *
     * The backoff is read under the permit. A dashboard queues dozens of reads
     * behind two permits; when the first one is refused, the rest must see the
     * refusal when their turn comes, not the clear state they were queued in.
     * Checked outside the permit, each of them went on to call Health Connect,
     * was refused in turn, and re-armed the minute: the toast said "1 min" forever.
     */
    private suspend fun <T> withRateLimitGuard(
        operation: String,
        fallback: T,
        block: suspend () -> T,
    ): T {
        val safeOperation = operation.privacySafeOperationName()
        // A writer's read must not pass a failure off as "no data".
        val strict = currentCoroutineContext()[StrictHealthConnectReads] != null
        if (!syncEnabled()) {
            Log.d(TAG, "Skipping $safeOperation - Health Connect sync paused")
            check(!strict) { "Health Connect access is paused" }
            return fallback
        }
        return readSemaphore.withPermit {
            val backoffMillis = HealthConnectRateLimitBackoff.remainingMillis(nowMillis())
            if (backoffMillis > MaxRateLimitWaitMillis) {
                Log.w(TAG, "Skipping $safeOperation - Health Connect rate limited for ${backoffMillis}ms more")
                HealthConnectCallCounters.skippedForBackoff(safeOperation)
                if (strict) HealthConnectRateLimitBackoff.throwIfActive(rateLimitMessage, nowMillis())
                markReadDegraded()
                return@withPermit fallback
            }
            if (backoffMillis > 0L) {
                Log.w(TAG, "Waiting to retry $safeOperation after Health Connect rate limit")
                delay(backoffMillis)
            }
            delay(pacer.reserve(HealthConnectRateLimitBackoff.hasBeenRateLimited))
            Log.d(TAG, "Starting $safeOperation ${diagnosticsSummary()}")
            HealthConnectCallCounters.started(safeOperation)
            val startedAt = nowMillis()
            try {
                withContext(dispatchers.io) {
                    block().also {
                        Log.d(TAG, "Finished $safeOperation successfully")
                        HealthConnectCallCounters.succeeded(safeOperation)
                    }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                if (HealthConnectRateLimitBackoff.isRateLimitFailure(t)) {
                    HealthConnectCallCounters.rateLimited(safeOperation)
                    val rateLimit = HealthConnectRateLimitBackoff.markRateLimited(
                        cause = t,
                        messageForRetryAfter = rateLimitMessage,
                        nowMillis = nowMillis(),
                        readStartedAtMillis = startedAt,
                    )
                    Log.w(TAG, "Rate limited $safeOperation ${diagnosticsSummary()}", t)
                    if (strict) throw rateLimit
                    markReadDegraded()
                } else {
                    HealthConnectCallCounters.failed(safeOperation)
                    Log.e(TAG, "Failed $safeOperation ${diagnosticsSummary()}", t)
                    if (strict) throw t
                }
                fallback
            }
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

/**
 * Chunk size for hour-bucketed reads: 360 buckets of four metrics per request.
 * That is fewer values than the 21 days of three metrics it replaced, which
 * stayed inside the Binder budget.
 */
internal const val HourlyAggregateMaxQueryDays = 15L

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

/**
 * Splits `[start, end)` into windows of at most [maxBuckets] buckets of
 * [bucket], on bucket boundaries. A rate-limit retry then replays one
 * window, not the whole range. Empty for inverted ranges.
 */
internal fun aggregateWindows(
    start: Instant,
    end: Instant,
    bucket: Duration,
    maxBuckets: Long = MaxInsightAggregateBuckets,
): List<Pair<Instant, Instant>> {
    val span = bucket.multipliedBy(maxBuckets)
    val windows = mutableListOf<Pair<Instant, Instant>>()
    var windowStart = start
    while (windowStart.isBefore(end)) {
        val windowEnd = minOf(end, windowStart.plus(span))
        windows += windowStart to windowEnd
        windowStart = windowEnd
    }
    return windows
}
