package tech.mmarca.openvitals.domain.model

import kotlin.math.roundToLong

/** Upper bound for day-view heart rate charts and in-memory use. */
const val MaxHeartRateChartSamples = 2_500

/**
 * Reduces high-frequency heart rate samples to a chart-friendly count while preserving
 * the overall day shape via bucket averaging.
 */
fun List<HeartRateSample>.reducedForChart(
    maxSamples: Int = MaxHeartRateChartSamples,
): List<HeartRateSample> {
    if (size <= maxSamples) return this
    val sorted = sortedBy { it.time }
    val bucketSize = (sorted.size + maxSamples - 1) / maxSamples
    return sorted.chunked(bucketSize).mapNotNull { bucket ->
        if (bucket.isEmpty()) return@mapNotNull null
        val midIndex = bucket.size / 2
        val representative = bucket[midIndex]
        HeartRateSample(
            time = representative.time,
            beatsPerMinute = bucket.map { it.beatsPerMinute }.average().roundToLong(),
            source = representative.source,
        )
    }
}
