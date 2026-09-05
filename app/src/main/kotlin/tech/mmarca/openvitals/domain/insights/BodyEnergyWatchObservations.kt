package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import kotlin.math.abs

/** A raw watch body-energy sample, before it is paired with the model. */
data class WatchBodyEnergySample(
    val time: Instant,
    /** The watch's own 0-100 score. */
    val score: Int,
)

/** How much time one observation stands for. Shared so the downsampler and the bookkeeping agree. */
val WatchObservationBucket: Duration = Duration.ofHours(1)

/** The bucket [time] falls in. Callers persist the last fitted index. */
fun watchObservationBucketIndex(time: Instant): Long =
    Math.floorDiv(time.toEpochMilli(), WatchObservationBucket.toMillis())

/**
 * Turns raw watch samples into observations against a timeline: at most
 * one per [bucket], each paired to the nearest point, which carries the
 * prediction and the driving influence. Samples with no point within
 * [maxPairingGap], and unmeasured points, are dropped.
 */
fun buildWatchObservations(
    samples: List<WatchBodyEnergySample>,
    timeline: BodyEnergyTimeline,
    bucket: Duration = WatchObservationBucket,
    maxPairingGap: Duration = Duration.ofMinutes(30),
): List<BodyEnergyWatchReading> {
    if (samples.isEmpty() || timeline.points.isEmpty()) return emptyList()

    val sorted = samples.sortedBy { it.time }

    // The first sample per bucket, so the choice does not drift with sync frequency.
    val kept = mutableListOf<WatchBodyEnergySample>()
    var currentBucket: Long? = null
    for (sample in sorted) {
        val index = Math.floorDiv(sample.time.toEpochMilli(), bucket.toMillis())
        if (index == currentBucket) continue
        currentBucket = index
        kept += sample
    }

    return kept.mapNotNull { sample ->
        val point = timeline.points.nearestPoint(sample.time, maxPairingGap) ?: return@mapNotNull null
        if (point.state == BodyEnergyBucketState.UNMEASURABLE) return@mapNotNull null
        BodyEnergyWatchReading(
            time = sample.time,
            observedScore = sample.score.coerceIn(0, 100),
            predictedScore = point.score,
            dominantInfluence = point.primaryInfluence,
        )
    }
}

private fun List<BodyEnergyTimelinePoint>.nearestPoint(
    time: Instant,
    maxGap: Duration,
): BodyEnergyTimelinePoint? {
    var best: BodyEnergyTimelinePoint? = null
    var bestGap = maxGap.toMillis() + 1
    for (point in this) {
        val gap = abs(point.time.toEpochMilli() - time.toEpochMilli())
        if (gap < bestGap) {
            bestGap = gap
            best = point
        }
    }
    return best
}
