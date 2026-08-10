package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant

/** Buckets used when Health Connect aggregates heart rate for day/long-range charts. */
val HeartRateChartBucketDuration: Duration = Duration.ofMinutes(15)

/**
 * Buckets for strain math (cardio load / intensity minutes).
 *
 * Chart aggregation uses [HeartRateChartBucketDuration] (15 minutes). TRIMP and
 * intensity coverage only keep gaps of five minutes or less, so feeding those
 * calculators the chart series zeroes coverage and forces movement fallback.
 * One-minute buckets stay inside that gap budget without loading every raw beat
 * across multi-day windows.
 */
val HeartRateInsightBucketDuration: Duration = Duration.ofMinutes(1)

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
