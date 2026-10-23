package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant

/**
 * Buckets for strain math. TRIMP and intensity coverage drop pairs more
 * than five minutes apart, so five-minute buckets are the coarsest slicing
 * that still counts as continuous. One-minute buckets overflowed the
 * Binder parcel on watches that write heart rate continuously.
 */
val HeartRateInsightBucketDuration: Duration = Duration.ofMinutes(5)

/**
 * The most buckets one insight request may ask for. At
 * [HeartRateInsightBucketDuration] a day is 288, so a day costs two requests.
 */
const val MaxInsightAggregateBuckets = 144L

/** Workout-length ranges keep raw samples for finer chart resolution. */
val HeartRateRawSampleMaxRange: Duration = Duration.ofHours(4)

/**
 * The longest window read sample by sample: a day plus its lookback, or a
 * very long workout. A 1 Hz source is 86,400 samples a day, and 28 days of
 * them ran the app out of memory. Longer windows read as buckets.
 */
val HeartRateRawReadMaxRange: Duration = Duration.ofHours(36)

fun exceedsRawHeartRateReadRange(range: Duration): Boolean =
    range > HeartRateRawReadMaxRange

fun shouldUseAggregatedHeartRateSamples(range: Duration): Boolean =
    range > HeartRateRawSampleMaxRange

fun heartRateSampleFromAggregateBucket(
    startTime: Instant,
    avgBpm: Long,
): HeartRateSample = HeartRateSample(
    time = startTime,
    beatsPerMinute = avgBpm,
    source = "",
)

/**
 * Raw samples folded into the series a grouped-duration aggregate returns:
 * one sample per [bucket], on a grid that restarts at each window's start,
 * holding the bucket's mean truncated to a whole bpm as `BPM_AVG` is. A
 * window of [HeartRateRawSampleMaxRange] or less keeps its samples raw.
 *
 * Holds sums, not means, so the folds of several reads can be merged: a
 * record that starts in one read can carry samples into the next window.
 */
class HeartRateInsightBuckets(
    windows: List<Pair<Instant, Instant>>,
    bucket: Duration = HeartRateInsightBucketDuration,
) {
    private val windowStarts = LongArray(windows.size) { windows[it].first.toEpochMilli() }
    private val windowEnds = LongArray(windows.size) { windows[it].second.toEpochMilli() }
    private val keepsRaw = BooleanArray(windows.size) {
        !shouldUseAggregatedHeartRateSamples(Duration.between(windows[it].first, windows[it].second))
    }
    private val bucketMillis = bucket.toMillis()

    /** Bucket start in epoch millis to `[bpm sum, sample count]`. */
    private val sums = HashMap<Long, LongArray>()
    private val raw = mutableListOf<HeartRateSample>()

    /** Folds [sample] in; a sample outside every window is dropped. */
    fun add(sample: HeartRateSample) {
        val time = sample.time.toEpochMilli()
        val window = windowStarts.binarySearch(time).let { if (it >= 0) it else -it - 2 }
        if (window < 0 || time >= windowEnds[window]) return
        if (keepsRaw[window]) {
            raw += sample
            return
        }
        val windowStart = windowStarts[window]
        val bucketStart = windowStart + (time - windowStart) / bucketMillis * bucketMillis
        val sum = sums.getOrPut(bucketStart) { LongArray(2) }
        sum[0] += sample.beatsPerMinute
        sum[1]++
    }

    /** Adds [other]'s samples; both must be built over the same windows. */
    fun addAll(other: HeartRateInsightBuckets) {
        other.sums.forEach { (bucketStart, otherSum) ->
            val sum = sums.getOrPut(bucketStart) { LongArray(2) }
            sum[0] += otherSum[0]
            sum[1] += otherSum[1]
        }
        raw += other.raw
    }

    fun samples(): List<HeartRateSample> =
        (
            sums.map { (bucketStart, sum) ->
                heartRateSampleFromAggregateBucket(Instant.ofEpochMilli(bucketStart), sum[0] / sum[1])
            } + raw
        ).sortedBy { it.time }
}
