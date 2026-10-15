package tech.mmarca.openvitals.core.stats

import java.time.Duration
import java.time.Instant

/**
 * Mean of per-minute bucket means: the time-weighted average. A per-sample
 * mean weights by how often the device wrote, so a 1 Hz workout printed a
 * 79 bpm day as 115. Empty buckets are skipped. Null on no samples.
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
