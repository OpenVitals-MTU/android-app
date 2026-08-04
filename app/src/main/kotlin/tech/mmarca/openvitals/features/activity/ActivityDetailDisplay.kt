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
 * Speed over the session for a device that recorded no speed — reconstructed
 * from the splits, which know how far each segment went and how long it took.
 *
 * A split's speed holds ACROSS the split (it is an average over a window, not
 * a reading at an instant), so the trace is a STEP: flat for the split's
 * window, and jumping at its boundary. It is meant to look blocky. A smooth
 * curve here would claim a resolution these numbers do not have.
 */
internal data class ActivitySplitSpeedTrace(
    /** Two points per split — its start and its end, at the same speed. */
    val samples: List<ActivitySpeedTraceSample>,

    /**
     * How many splits are behind the trace. The card counts splits, not
     * samples: there is no such thing as a sample here.
     */
    val splitCount: Int,

    /**
     * Total distance over total elapsed, across the splits that are drawn.
     *
     * Stated rather than left to the chart, which would take the mean of the
     * plotted points: with equal-distance splits that is their arithmetic
     * mean, and average speed over equal distances is the HARMONIC mean — so
     * the chart would quietly report a slightly faster session than happened,
     * and disagree with the average speed elsewhere on the same screen.
     */
    val averageMetersPerSecond: Double,
)

/**
 * The cadence kinds that actually recorded something, in enum order.
 *
 * A card per kind, and only for the kinds with samples: a bike ride with no
 * footpod has no step cadence to draw, and an empty chart is worse than no chart.
 */
internal fun activityCadenceKinds(
    samples: List<ActivityCadenceSample>,
): List<ActivityCadenceKind> =
    ActivityCadenceKind.entries.filter { kind -> samples.any { it.kind == kind } }

/**
 * The metric pace bars are scaled per KILOMETRE, whatever the user's units.
 * The scale is a ratio between the activity's own splits, and a ratio does not
 * care which unit its terms are in — but mixing the two would.
 */
private const val PaceScaleUnitMeters = 1000.0

/**
 * The slowest split, in seconds per kilometre — the bar scale's slow end.
 * Null when no split has a pace (which is what leaves the bars unpainted).
 */
internal fun slowestSplitPaceSeconds(splits: ActivitySplits): Double? =
    splits.splits.mapNotNull { it.paceSecondsPerUnit(PaceScaleUnitMeters) }.maxOrNull()

/** The fastest split, in seconds per kilometre — the bar scale's fast end. */
internal fun fastestSplitPaceSeconds(splits: ActivitySplits): Double? =
    splits.splits.mapNotNull { it.paceSecondsPerUnit(PaceScaleUnitMeters) }.minOrNull()

/**
 * Speed rebuilt from the splits — the trace for a session whose device wrote
 * no `SpeedRecord` but whose route (or laps) says how far each segment went
 * and how long it took.
 *
 * It refuses in two cases, and the refusals are the point:
 *
 *  * A RECORDED trace exists. A measurement beats a reconstruction, and two
 *    speed cards on one screen disagreeing by a hair would be worse than
 *    either alone.
 *  * The splits are [SplitSource.ESTIMATED] — total distance spread evenly
 *    over the duration. Every split then carries the identical average pace by
 *    construction, so the "trace" is a horizontal line asserting a metronomic
 *    pace nobody measured. That is the same reason the splits card paints no
 *    bars for this source, and a line is a stronger claim than a bar.
 *
 * One split is also refused: a single average is the number the header already
 * states, and drawing it as a chart adds nothing but the suggestion that it
 * was measured over time.
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
        // Null for a split that covered no distance or took no time — a paused
        // lap, a lap the device wrote no length for. It has no speed, so it is
        // not drawn.
        val pace = split.paceSecondsPerMeter ?: continue
        if (!pace.isFinite() || pace <= 0) continue
        val metersPerSecond = 1.0 / pace
        if (!metersPerSecond.isFinite()) continue

        // The step: this speed held from here to there, and we do not know
        // when within the split it was faster or slower.
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
 * The session's height over time, from whichever route points carry an
 * altitude.
 *
 * A route may carry altitude on some points and not others — a fix taken
 * indoors, a device that drops it under a poor sky — so the ones without are
 * skipped rather than being read as sea level, which would draw a cliff.
 */
internal fun elevationProfile(route: ExerciseRouteData): List<ActivityElevationSample> {
    if (route.status != ExerciseRouteStatus.DATA) return emptyList()
    val samples = route.points
        .mapNotNull { point ->
            point.altitudeMeters?.let { ActivityElevationSample(point.time, it) }
        }
        .sortedBy { it.time }
    // One height is a fact, not a profile. Two is a line.
    return if (samples.size > 1) samples else emptyList()
}
