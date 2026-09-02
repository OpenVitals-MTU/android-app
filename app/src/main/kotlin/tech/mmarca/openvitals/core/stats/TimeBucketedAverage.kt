package tech.mmarca.openvitals.core.stats

import java.time.Duration
import java.time.Instant

/**
 * Mean of per-minute bucket means: the time-weighted average for sampled
 * series, stated once instead of re-derived per metric.
 *
 * A plain per-sample mean weights a reading by how often the device wrote,
 * not by how long the value held. Gadgetbridge-style sources record background
 * heart rate about once a minute but workouts at ~1 Hz, so a 50-minute run
 * (~3000 samples) outweighs the other 23 hours (~1440 samples) and a day that
 * averaged 79 bpm prints as 115. Averaging each occupied bucket first and then
 * averaging the buckets gives every recorded minute the same weight, which is
 * also what Gadgetbridge and Google Fit report.
 *
 * Empty buckets are skipped rather than interpolated: minutes with no reading
 * say nothing, and a sparse metric (a lone SpO2 spot check) still counts as
 * one bucket instead of being swallowed by a denser stretch elsewhere.
 *
 * Uniformly sampled series (at most one sample per bucket) reduce to the plain
 * mean, so callers with well-behaved sources see no change.
 *
 * Returns null on no samples, for the same reason [averageOrNull] does.
 */
fun <T> Iterable<T>.timeBucketedAverageOrNull(
    time: (T) -> Instant,
    value: (T) -> Double,
    bucket: Duration = Duration.ofMinutes(1),
): Double? {
    val bucketMillis = bucket.toMillis()
    require(bucketMillis > 0L) { "bucket must be positive, was $bucket" }
    // bucket index -> [sum, count]
    val buckets = HashMap<Long, DoubleArray>()
    for (item in this) {
        val key = Math.floorDiv(time(item).toEpochMilli(), bucketMillis)
        val acc = buckets.getOrPut(key) { DoubleArray(2) }
        acc[0] += value(item)
        acc[1] += 1.0
    }
    if (buckets.isEmpty()) return null
    var sumOfBucketMeans = 0.0
    for (acc in buckets.values) {
        sumOfBucketMeans += acc[0] / acc[1]
    }
    return sumOfBucketMeans / buckets.size
}
