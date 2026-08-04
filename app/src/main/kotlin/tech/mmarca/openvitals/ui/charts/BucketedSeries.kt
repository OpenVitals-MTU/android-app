package tech.mmarca.openvitals.ui.components

import java.time.Duration
import java.time.Instant

/**
 * One bucket of a time series aggregated into fixed-width windows: its representative
 * instant (the bucket centre) and the average, minimum and maximum of the samples that
 * fell in it — everything the aggregated ("Google-Health-style") chart view needs to
 * draw an average line with a min/max band.
 */
data class BucketPoint(
    val time: Instant,
    val average: Double,
    val min: Double,
    val max: Double,
    val count: Int,
)

/**
 * Buckets [samples] into [bucketMinutes]-wide windows measured from [dayStart],
 * returning one [BucketPoint] per non-empty bucket in ascending time order.
 *
 * Samples before [dayStart] and non-finite values are skipped. Buckets are aligned to
 * [dayStart] so their boundaries are stable across refreshes. Returns an empty list when
 * [bucketMinutes] <= 0 or there is nothing to aggregate.
 */
fun <T> bucketedSeries(
    samples: Iterable<T>,
    bucketMinutes: Int,
    dayStart: Instant,
    time: (T) -> Instant,
    value: (T) -> Double,
): List<BucketPoint> {
    if (bucketMinutes <= 0) return emptyList()

    val buckets = HashMap<Int, BucketAccumulator>()
    for (sample in samples) {
        val minutesFromStart = Duration.between(dayStart, time(sample)).toMinutes()
        if (minutesFromStart < 0) continue
        val v = value(sample)
        if (!v.isFinite()) continue
        val index = (minutesFromStart / bucketMinutes).toInt()
        buckets.getOrPut(index) { BucketAccumulator() }.add(v)
    }
    if (buckets.isEmpty()) return emptyList()

    return buckets.keys.sorted().map { index ->
        val acc = buckets.getValue(index)
        // Centre of the bucket, so the point sits in the middle of the window it
        // summarises rather than at its leading edge.
        val centre = dayStart
            .plus(Duration.ofMinutes(index.toLong() * bucketMinutes))
            .plusSeconds(bucketMinutes * 30L)
        BucketPoint(
            time = centre,
            average = acc.sum / acc.count,
            min = acc.min,
            max = acc.max,
            count = acc.count,
        )
    }
}

private class BucketAccumulator {
    var sum = 0.0
    var count = 0
    var min = Double.POSITIVE_INFINITY
    var max = Double.NEGATIVE_INFINITY

    fun add(value: Double) {
        sum += value
        count += 1
        if (value < min) min = value
        if (value > max) max = value
    }
}
