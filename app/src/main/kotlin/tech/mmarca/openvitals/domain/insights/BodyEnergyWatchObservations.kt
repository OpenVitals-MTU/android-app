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

/**
 * How much time one calibration observation stands for.
 *
 * Shared so the downsampler here and the caller's "which buckets have already
 * been fitted" bookkeeping cannot drift apart — if they disagree, a bucket is
 * either fitted twice or never.
 */
val WatchObservationBucket: Duration = Duration.ofHours(1)

/**
 * The bucket [time] falls in, as an index. Callers persist the last fitted index
 * so a bucket contributes exactly one observation no matter how many syncs
 * happen to touch it.
 */
fun watchObservationBucketIndex(time: Instant): Long =
    Math.floorDiv(time.toEpochMilli(), WatchObservationBucket.toMillis())

/**
 * Turns raw watch samples into calibration observations against a computed
 * timeline.
 *
 * Two jobs, both pure:
 *
 *  * **Downsample.** The watch emits a sample a minute. Feeding every one in
 *    would let a single day outvote months of evidence, so at most one per
 *    [bucket] is kept. Combined with the small watch learning rate, a day
 *    contributes a nudge rather than a shove.
 *  * **Pair.** Each kept sample is matched to the nearest timeline point, which
 *    already carries both what this app predicted at that moment and the
 *    influence that was driving it — the gain a mismatch is attributed to.
 *    Reusing the timeline's own `primaryInfluence` rather than re-deriving one
 *    keeps a watch correction pointed at exactly the gain the moment would have
 *    moved, with the zone and workout context a reconstruction from the point's
 *    components alone would lose.
 *
 * Samples with no point within [maxPairingGap] are dropped: attributing an error
 * to a gain the model was not exercising at that time would teach it the wrong
 * lesson. Points the model itself could not measure are skipped for the same
 * reason.
 */
fun buildWatchObservations(
    samples: List<WatchBodyEnergySample>,
    timeline: BodyEnergyTimeline,
    bucket: Duration = WatchObservationBucket,
    maxPairingGap: Duration = Duration.ofMinutes(30),
): List<BodyEnergyWatchReading> {
    if (samples.isEmpty() || timeline.points.isEmpty()) return emptyList()

    val sorted = samples.sortedBy { it.time }

    // One sample per bucket — the first in each, so the choice is deterministic
    // and does not drift with how often the user happens to sync.
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
