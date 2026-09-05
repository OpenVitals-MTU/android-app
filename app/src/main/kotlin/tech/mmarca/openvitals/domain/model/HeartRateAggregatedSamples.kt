package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant

/** Buckets used when Health Connect aggregates heart rate for day/long-range charts. */
val HeartRateChartBucketDuration: Duration = Duration.ofMinutes(15)

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

internal fun shouldUseAggregatedHeartRateSamples(range: Duration): Boolean =
    range > HeartRateRawSampleMaxRange

internal fun heartRateSampleFromAggregateBucket(
    startTime: Instant,
    avgBpm: Long,
): HeartRateSample = HeartRateSample(
    time = startTime,
    beatsPerMinute = avgBpm,
    source = "",
)
