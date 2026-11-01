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
 * The activity progress read carries several metrics a bucket and keeps this.
 */
const val MaxInsightAggregateBuckets = 144L

/**
 * The most buckets one heart-rate insight request may ask for. The response
 * is one Binder parcel; 360 buckets of four metrics were measured safe for
 * the hourly reads, and a heart-rate insight bucket carries one. A day is
 * 288 and a fortnight 4,032, so the dashboard's two-week heart-rate walk is
 * a dozen requests, not thirty.
 */
const val MaxHeartRateInsightAggregateBuckets = 360L

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
