package tech.mmarca.openvitals.features.activity

import java.time.Instant
import tech.mmarca.openvitals.domain.insights.ActivitySplits
import tech.mmarca.openvitals.domain.insights.SplitSource
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ExerciseRouteData
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.domain.model.SpeedSample

/** One point of the session's elevation profile. */
internal data class ActivityElevationSample(
    val time: Instant,
    val meters: Double,
)

/** One point of a speed trace derived from the splits. */
internal data class ActivitySpeedTraceSample(
    val time: Instant,
    val metersPerSecond: Double,
)

/**
 * Speed reconstructed from the splits. A split's speed holds across its
 * window, so the trace is a step, not a smooth curve.
 */
internal data class ActivitySplitSpeedTrace(
    /** Two points per split: start and end, same speed. */
    val samples: List<ActivitySpeedTraceSample>,

    /** How many splits are behind the trace. */
    val splitCount: Int,

    /**
     * Total distance over total elapsed. Stated, because the mean of the
     * plotted points would be the arithmetic mean, not the harmonic one.
     */
    val averageMetersPerSecond: Double,
)

/** The cadence kinds that recorded something, in enum order. An empty chart is worse than none. */
internal fun activityCadenceKinds(
    samples: List<ActivityCadenceSample>,
): List<ActivityCadenceKind> =
    ActivityCadenceKind.entries.filter { kind -> samples.any { it.kind == kind } }

/** Pace bars are scaled per kilometre, whatever the units: the scale is a ratio. */
private const val PaceScaleUnitMeters = 1000.0

/** The slowest split in s/km, the bar scale's slow end. Null when no split has a pace. */
internal fun slowestSplitPaceSeconds(splits: ActivitySplits): Double? =
    splits.splits.mapNotNull { it.paceSecondsPerUnit(PaceScaleUnitMeters) }.maxOrNull()

/** The fastest split, in seconds per kilometre — the bar scale's fast end. */
internal fun fastestSplitPaceSeconds(splits: ActivitySplits): Double? =
    splits.splits.mapNotNull { it.paceSecondsPerUnit(PaceScaleUnitMeters) }.minOrNull()

/**
 * Speed rebuilt from the splits. Refused when a recorded trace exists, when
 * the splits are [SplitSource.ESTIMATED] (a flat line nobody measured), or
 * with a single split (the header already states it).
 */
internal fun splitSpeedTrace(
    recordedSpeed: List<SpeedSample>,
    splits: ActivitySplits,
): ActivitySplitSpeedTrace? {
    if (recordedSpeed.isNotEmpty()) return null
    if (splits.source == SplitSource.ESTIMATED) return null

    val samples = mutableListOf<ActivitySpeedTraceSample>()
    var meters = 0.0
    var seconds = 0.0
    for (split in splits.splits) {
        // No distance or no time: no speed, not drawn.
        val pace = split.paceSecondsPerMeter ?: continue
        if (!pace.isFinite() || pace <= 0) continue
        val metersPerSecond = 1.0 / pace
        if (!metersPerSecond.isFinite()) continue

        // The step: this speed held from here to there.
        samples.add(ActivitySpeedTraceSample(split.startTime, metersPerSecond))
        samples.add(ActivitySpeedTraceSample(split.endTime, metersPerSecond))
        meters += split.distanceMeters
        seconds += split.elapsedMs / 1000.0
    }

    val splitCount = samples.size / 2
    if (splitCount < 2 || meters <= 0 || seconds <= 0) return null
    return ActivitySplitSpeedTrace(
        samples = samples,
        splitCount = splitCount,
        averageMetersPerSecond = meters / seconds,
    )
}

/**
 * The session's height over time. Points without altitude are skipped,
 * not read as sea level.
 */
internal fun elevationProfile(route: ExerciseRouteData): List<ActivityElevationSample> {
    if (route.status != ExerciseRouteStatus.DATA) return emptyList()
    val samples = route.points
        .mapNotNull { point ->
            point.altitudeMeters?.let { ActivityElevationSample(point.time, it) }
        }
        .sortedBy { it.time }
    // One height is a fact, not a profile.
    return if (samples.size > 1) samples else emptyList()
}
