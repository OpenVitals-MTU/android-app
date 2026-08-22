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
 * This is that budget exactly: the calculators drop a pair whose spacing is
 * `> 5 min`, so consecutive five-minute buckets are the coarsest slicing that
 * still counts as continuous coverage.
 *
 * It was one minute, which bought resolution nothing downstream reads and cost
 * five times the buckets. A grouped-duration response is one Binder parcel, and
 * a local day at one-minute slicing is 1440 buckets in it — an order of
 * magnitude past the ~122 that [tech.mmarca.openvitals.healthconnect.DailyAggregateMaxQueryDays]
 * was measured safe at. On a watch that writes heart rate continuously the read
 * did not return a coarse trace, it threw TransactionTooLargeException, and the
 * caller degraded it to an empty list: weekly cardio load silently fell back to
 * step-based movement estimates on exactly the devices with the best data.
 */
val HeartRateInsightBucketDuration: Duration = Duration.ofMinutes(5)

/**
 * The most buckets one insight aggregate request may ask for.
 *
 * Same reasoning as [tech.mmarca.openvitals.healthconnect.DailyAggregateMaxQueryDays],
 * applied to the other axis: that constant bounds a response by DAYS at one
 * bucket each, this one bounds it by buckets directly. At
 * [HeartRateInsightBucketDuration] a local day is 288 buckets, so a day costs
 * two requests instead of one oversized one.
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
