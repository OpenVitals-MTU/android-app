package tech.mmarca.openvitals.domain.model

import java.time.Duration
import java.time.Instant

/** Buckets used when Health Connect aggregates heart rate for day/long-range charts. */
val HeartRateChartBucketDuration: Duration = Duration.ofMinutes(15)

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
