package tech.mmarca.openvitals.domain.insights

import androidx.health.connect.client.records.ExerciseSegment
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToLong
import tech.mmarca.openvitals.core.geo.haversineMeters
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseLapData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.domain.model.isDistanceBasedExercise
import tech.mmarca.openvitals.domain.model.movingDurationMs

/**
 * Per-segment splits ("laps") for a distance-based activity. Pure arithmetic.
 * The UI must show [ActivitySplits.source]: a split cut from GPS and one
 * guessed from the average are different claims.
 */

/** Where the splits came from, most trustworthy first. One computation, one provenance. */
internal enum class SplitSource {
    /** The device wrote lap records. Shown as recorded, never re-cut. */
    DEVICE_LAPS,

    /** Cut from the GPS route by accumulating haversine distance between fixes. */
    ROUTE,

    /** Cut by integrating SpeedRecord samples: the treadmill case. */
    SPEED_SAMPLES,

    /** Total distance spread evenly over the duration. The UI must label it. */
    ESTIMATED,
}

/** One split row. */
internal data class ActivitySplit(
    /** 1-based, as displayed. */
    val index: Int,

    /** The split's own distance. The final split is usually shorter; see [isPartial]. */
    val distanceMeters: Double,

    /**
     * Moving time: the window less any pause inside it. Pace and the speed
     * trace divide by this. [startTime] and [endTime] stay wall-clock.
     */
    val elapsedMs: Long,
    val startTime: Instant,
    val endTime: Instant,

    /** Shorter than the requested distance (the trailing remainder). Never true for device laps. */
    val isPartial: Boolean,

    /** Mean heart rate inside `[startTime, endTime)`, or null when no sample falls there. */
    val averageHeartRateBpm: Double? = null,

    /** Ascent/descent from the route's altitudes. Null, not 0, without altitude data. */
    val elevationGainMeters: Double? = null,
    val elevationLossMeters: Double? = null,

    /**
     * This split's pace minus the activity's average pace, in s/km. Negative
     * is faster. Null when either pace is undefined.
     */
    val paceDeltaSecondsPerKilometer: Double? = null,
) {
    /** Seconds per meter, or null when the split covered no distance or no time. */
    val paceSecondsPerMeter: Double?
        get() {
            val seconds = elapsedMs / 1000.0
            if (distanceMeters <= 0 || seconds <= 0) return null
            return seconds / distanceMeters
        }

    /** The split's pace expressed per display unit (1000 m, or 1609.344 m). */
    fun paceSecondsPerUnit(unitMeters: Double): Double? {
        val perMeter = paceSecondsPerMeter
        if (perMeter == null || unitMeters <= 0) return null
        return perMeter * unitMeters
    }

    /** [paceDeltaSecondsPerKilometer] re-expressed per display unit. */
    fun paceDeltaSecondsPerUnit(unitMeters: Double): Double? {
        val deltaPerKm = paceDeltaSecondsPerKilometer
        if (deltaPerKm == null || unitMeters <= 0) return null
        return deltaPerKm * unitMeters / 1000.0
    }

    /** Convenience for the common metric case, and the name the spec uses. */
    val paceDeltaSeconds: Double? get() = paceDeltaSecondsPerKilometer
}

/** The splits plus the one thing the UI must not lose: where they came from. */
internal data class ActivitySplits(
    val source: SplitSource,
    val splits: List<ActivitySplit>,
) {
    val isEmpty: Boolean get() = splits.isEmpty()
    val isNotEmpty: Boolean get() = splits.isNotEmpty()

    companion object {
        private val None = ActivitySplits(SplitSource.ESTIMATED, emptyList())

        fun none(): ActivitySplits = None
    }
}

/** The default split distance: one kilometer. Storage is always metric. */
internal const val DefaultSplitDistanceMeters = 1000.0

/** A trailing remainder shorter than this is dropped rather than shown as "0 m". */
private const val MinPartialMeters = 1.0

/** Guards a pathological split distance: 1 cm over a marathon is 4.2 million rows. */
private const val MaxSplits = 500

/**
 * Splits for [workout], cut every [splitDistanceMeters]. Device laps win
 * outright, then route, speed samples, estimate. Empty for an activity that
 * does not travel.
 */
internal fun buildActivitySplits(
    workout: ExerciseData,
    routePoints: List<ExerciseRoutePoint>,
    speedSamples: List<SpeedSample>,
    heartRateSamples: List<HeartRateSample>,
    splitDistanceMeters: Double,
): ActivitySplits {
    // Whether an activity has splits depends on its kind, not its data: GPS drift
    // on a bench gave a strength session "1.0 km" splits at 30 min/km.
    if (!isDistanceBasedExercise(workout.exerciseType)) {
        return ActivitySplits.none()
    }

    val unit = if (splitDistanceMeters.isFinite() && splitDistanceMeters > 0) {
        splitDistanceMeters
    } else {
        DefaultSplitDistanceMeters
    }

    // A Health Connect read merges sources, so samples may not be time-ordered.
    val heartRates = heartRateSamples.sortedBy { it.time }
    val speeds = speedSamples.sortedBy { it.time }
    val sortedRoutePoints = routePoints.sortedBy { it.time }

    val laps = usableLaps(workout)
    val routeNodes = routeNodes(sortedRoutePoints)
    val speedNodes = speedNodes(speeds)
    val totalDistance = workout.totalDistanceMeters ?: 0.0
    val lapDistance = laps.fold(0.0) { sum, lap -> sum + max(0.0, lap.lengthMeters ?: 0.0) }

    val hasAnyDistance = isPositive(totalDistance) ||
        isPositive(lapDistance) ||
        isPositive(routeNodes.lastOrNull()?.cumulativeMeters ?: 0.0) ||
        isPositive(speedNodes.lastOrNull()?.cumulativeMeters ?: 0.0)
    if (!hasAnyDistance) return ActivitySplits.none()

    val raw: List<RawSplit>
    val source: SplitSource
    if (laps.isNotEmpty()) {
        source = SplitSource.DEVICE_LAPS
        raw = lapSplits(laps, routeNodes)
    } else if (routeNodes.size >= 2 && isPositive(routeNodes.last().cumulativeMeters)) {
        source = SplitSource.ROUTE
        raw = cutNodes(routeNodes, unit)
    } else if (speedNodes.size >= 2 && isPositive(speedNodes.last().cumulativeMeters)) {
        source = SplitSource.SPEED_SAMPLES
        raw = cutNodes(speedNodes, unit)
    } else {
        source = SplitSource.ESTIMATED
        raw = estimatedSplits(workout, unit)
    }
    if (raw.isEmpty()) return ActivitySplits.none()

    // The yardstick is the whole activity's average pace, not the mean of split paces.
    val activityPaceSecondsPerMeter = activityPaceSecondsPerMeter(workout, raw)

    val splits = raw.mapIndexed { i, entry ->
        // Estimated splits are already laid out over moving time; taking a pause
        // out again would double-count it.
        val windowMs = Duration.between(entry.startTime, entry.endTime).toMillis()
        val pausedMs = if (source == SplitSource.ESTIMATED) {
            0L
        } else {
            pausedMillisBetween(workout, entry.startTime, entry.endTime)
        }
        val elapsedMs = (windowMs - pausedMs).coerceAtLeast(0L)
        val elapsedSeconds = elapsedMs / 1000.0
        val splitPaceSecondsPerMeter = if (entry.distanceMeters > 0 && elapsedSeconds > 0) {
            elapsedSeconds / entry.distanceMeters
        } else {
            null
        }
        val paceDeltaPerKm = if (splitPaceSecondsPerMeter != null && activityPaceSecondsPerMeter != null) {
            (splitPaceSecondsPerMeter - activityPaceSecondsPerMeter) * 1000.0
        } else {
            null
        }

        ActivitySplit(
            index = i + 1,
            distanceMeters = entry.distanceMeters,
            elapsedMs = elapsedMs,
            startTime = entry.startTime,
            endTime = entry.endTime,
            isPartial = entry.isPartial,
            averageHeartRateBpm = averageHeartRate(heartRates, entry.startTime, entry.endTime),
            elevationGainMeters = entry.elevationGainMeters,
            elevationLossMeters = entry.elevationLossMeters,
            paceDeltaSecondsPerKilometer = paceDeltaPerKm,
        )
    }
    return ActivitySplits(source = source, splits = splits)
}

private fun isPositive(value: Double): Boolean = value.isFinite() && value > 0

/** How much of `[start, end)` was paused, from the PAUSE segments. */
private fun pausedMillisBetween(workout: ExerciseData, start: Instant, end: Instant): Long {
    val from = start.toEpochMilli()
    val to = end.toEpochMilli()
    if (to <= from) return 0L
    var paused = 0L
    for (segment in workout.segments) {
        if (segment.segmentType != ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE) continue
        val overlapStart = max(from, segment.startTime.toEpochMilli())
        val overlapEnd = min(to, segment.endTime.toEpochMilli())
        if (overlapEnd > overlapStart) paused += overlapEnd - overlapStart
    }
    // A split cannot be more than entirely paused.
    return min(paused, to - from)
}

/** Laps with a sane time window, oldest first. */
private fun usableLaps(workout: ExerciseData): List<ExerciseLapData> =
    workout.laps
        .filter { !it.endTime.isBefore(it.startTime) }
        .sortedBy { it.startTime }

/**
 * The activity's average pace in s/m, the baseline for pace deltas. Recorded
 * totals first, the sum of the splits for laps-only imports.
 */
private fun activityPaceSecondsPerMeter(workout: ExerciseData, raw: List<RawSplit>): Double? {
    val recordedDistance = workout.totalDistanceMeters ?: 0.0
    // Moving time, measured the same way the splits are.
    val recordedSeconds = workout.movingDurationMs() / 1000.0
    if (isPositive(recordedDistance) && recordedSeconds > 0) {
        return recordedSeconds / recordedDistance
    }
    val splitDistance = raw.sumOf { it.distanceMeters }
    val splitSeconds = raw.sumOf {
        Duration.between(it.startTime, it.endTime).toNanos() / 1_000_000_000.0
    }
    if (!isPositive(splitDistance) || splitSeconds <= 0) return null
    return splitSeconds / splitDistance
}

private fun averageHeartRate(
    samples: List<HeartRateSample>,
    start: Instant,
    end: Instant,
): Double? {
    var sum = 0L
    var count = 0
    for (sample in samples) {
        // Half-open [start, end): a boundary sample belongs to one split only.
        if (sample.time.isBefore(start)) continue
        if (!sample.time.isBefore(end)) break
        sum += sample.beatsPerMinute
        count++
    }
    if (count == 0) return null
    return (sum.toDouble() / count).roundToLong().toDouble()
}

/** A split before heart rate / pace delta are attached. */
private data class RawSplit(
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Double,
    val isPartial: Boolean,
    val elevationGainMeters: Double? = null,
    val elevationLossMeters: Double? = null,
)

/** A point on a distance-over-time curve: a GPS fix, a speed sample or an interpolated boundary. */
private data class Node(
    val time: Instant,
    val cumulativeMeters: Double,
    val altitudeMeters: Double? = null,
)

/** GPS fixes to cumulative haversine distance. Bad segments contribute 0. */
private fun routeNodes(points: List<ExerciseRoutePoint>): List<Node> {
    if (points.isEmpty()) return emptyList()
    val nodes = mutableListOf(
        Node(
            time = points.first().time,
            cumulativeMeters = 0.0,
            altitudeMeters = points.first().altitudeMeters,
        ),
    )
    var cumulative = 0.0
    for (i in 1 until points.size) {
        val previous = points[i - 1]
        val current = points[i]
        val segment = haversineMeters(
            previous.latitude,
            previous.longitude,
            current.latitude,
            current.longitude,
        )
        if (segment.isFinite() && segment > 0) cumulative += segment
        nodes.add(
            Node(
                time = current.time,
                cumulativeMeters = cumulative,
                altitudeMeters = current.altitudeMeters,
            ),
        )
    }
    return nodes
}

/**
 * Speed samples to cumulative distance by trapezoidal integration. No
 * altitude. Same integration as `distanceFromSpeedSamples`, kept per node.
 */
private fun speedNodes(samples: List<SpeedSample>): List<Node> {
    if (samples.isEmpty()) return emptyList()
    val nodes = mutableListOf(
        Node(time = samples.first().time, cumulativeMeters = 0.0),
    )
    var cumulative = 0.0
    for (i in 1 until samples.size) {
        val previous = samples[i - 1]
        val current = samples[i]
        val seconds = Duration.between(previous.time, current.time).toNanos() / 1_000_000_000.0
        val v0 = max(0.0, previous.metersPerSecond)
        val v1 = max(0.0, current.metersPerSecond)
        if (seconds > 0 && v0.isFinite() && v1.isFinite()) {
            cumulative += (v0 + v1) / 2.0 * seconds
        }
        nodes.add(Node(time = current.time, cumulativeMeters = cumulative))
    }
    return nodes
}

/**
 * Cut a distance-over-time curve every [unit] meters. The crossing is
 * interpolated: snapping to a 5 s GPS node gives ~8 s/km pace error.
 */
private fun cutNodes(nodes: List<Node>, unit: Double): List<RawSplit> {
    val total = nodes.last().cumulativeMeters
    if (!isPositive(total)) return emptyList()

    val splits = mutableListOf<RawSplit>()
    var splitStart = nodes.first()
    var splitStartIndex = 0
    var boundary = unit

    for (i in 1 until nodes.size) {
        if (splits.size >= MaxSplits) break
        val previous = nodes[i - 1]
        val current = nodes[i]
        val segment = current.cumulativeMeters - previous.cumulativeMeters
        if (segment <= 0) continue

        while (boundary <= current.cumulativeMeters && splits.size < MaxSplits) {
            val fraction = (boundary - previous.cumulativeMeters) / segment
            val crossing = interpolate(previous, current, fraction, boundary)
            val elevation = elevationBetween(nodes, splitStartIndex, i, splitStart, crossing)
            splits.add(
                RawSplit(
                    startTime = splitStart.time,
                    endTime = crossing.time,
                    distanceMeters = crossing.cumulativeMeters - splitStart.cumulativeMeters,
                    isPartial = false,
                    elevationGainMeters = elevation?.gain,
                    elevationLossMeters = elevation?.loss,
                ),
            )
            splitStart = crossing
            // `splitStartIndex` is the node at or before the split start, so the
            // next split's interior nodes begin at i.
            splitStartIndex = i - 1
            boundary += unit
        }
    }

    // The trailing remainder: a real, shorter split, flagged.
    val last = nodes.last()
    val remainder = last.cumulativeMeters - splitStart.cumulativeMeters
    if (remainder >= MinPartialMeters && splits.size < MaxSplits) {
        val elevation = elevationBetween(nodes, splitStartIndex, nodes.size - 1, splitStart, last)
        splits.add(
            RawSplit(
                startTime = splitStart.time,
                endTime = last.time,
                distanceMeters = remainder,
                isPartial = true,
                elevationGainMeters = elevation?.gain,
                elevationLossMeters = elevation?.loss,
            ),
        )
    }
    return splits
}

/** The node at [targetDistance], [fraction] of the way from [from] to [to]. */
private fun interpolate(from: Node, to: Node, fraction: Double, targetDistance: Double): Node {
    val clamped = if (fraction.isFinite()) fraction.coerceIn(0.0, 1.0) else 0.0
    val spanNanos = Duration.between(from.time, to.time).toNanos().toDouble()
    val time = from.time.plusNanos((spanNanos * clamped).roundToLong())
    val fromAltitude = from.altitudeMeters
    val toAltitude = to.altitudeMeters
    val altitude = if (fromAltitude != null && toAltitude != null) {
        fromAltitude + (toAltitude - fromAltitude) * clamped
    } else {
        fromAltitude ?: toAltitude
    }
    return Node(
        time = time,
        cumulativeMeters = targetDistance,
        altitudeMeters = altitude,
    )
}

private data class Elevation(val gain: Double, val loss: Double)

/**
 * Ascent/descent from [start] through the nodes in `(startIndex, endIndex]`
 * to [end]. Null when no two consecutive points carry an altitude.
 */
private fun elevationBetween(
    nodes: List<Node>,
    startIndex: Int,
    endIndex: Int,
    start: Node,
    end: Node,
): Elevation? {
    val walk = buildList {
        add(start)
        for (i in startIndex + 1 until endIndex) add(nodes[i])
        add(end)
    }
    var gain = 0.0
    var loss = 0.0
    var sawPair = false
    for (i in 1 until walk.size) {
        val from = walk[i - 1].altitudeMeters
        val to = walk[i].altitudeMeters
        if (from == null || to == null || !from.isFinite() || !to.isFinite()) continue
        sawPair = true
        val delta = to - from
        if (delta > 0) {
            gain += delta
        } else {
            loss += -delta
        }
    }
    if (!sawPair) return null
    return Elevation(gain, loss)
}

/** Device laps as recorded. A lap with no distance takes it from the route, if any. */
private fun lapSplits(laps: List<ExerciseLapData>, routeNodes: List<Node>): List<RawSplit> =
    laps.map { lap ->
        val recorded = lap.lengthMeters
        val fromRoute = routeSpan(routeNodes, lap.startTime, lap.endTime)
        val distance = if (recorded != null && recorded.isFinite() && recorded >= 0) {
            recorded
        } else {
            fromRoute?.distanceMeters ?: 0.0
        }
        RawSplit(
            startTime = lap.startTime,
            endTime = lap.endTime,
            distanceMeters = distance,
            // An uneven device lap is not a truncated one.
            isPartial = false,
            elevationGainMeters = fromRoute?.elevation?.gain,
            elevationLossMeters = fromRoute?.elevation?.loss,
        )
    }

private data class RouteSpan(val distanceMeters: Double, val elevation: Elevation?)

/** Route distance and elevation between two times, for a lap spanning part of a route. */
private fun routeSpan(nodes: List<Node>, start: Instant, end: Instant): RouteSpan? {
    if (nodes.size < 2 || !end.isAfter(start)) return null
    val inside = nodes.filter { !it.time.isBefore(start) && !it.time.isAfter(end) }
    if (inside.size < 2) return null
    val distance = inside.last().cumulativeMeters - inside.first().cumulativeMeters
    var gain = 0.0
    var loss = 0.0
    var sawPair = false
    for (i in 1 until inside.size) {
        val from = inside[i - 1].altitudeMeters
        val to = inside[i].altitudeMeters
        if (from == null || to == null || !from.isFinite() || !to.isFinite()) continue
        sawPair = true
        val delta = to - from
        if (delta > 0) {
            gain += delta
        } else {
            loss += -delta
        }
    }
    return RouteSpan(
        max(0.0, distance),
        if (sawPair) Elevation(gain, loss) else null,
    )
}

/**
 * The last resort: total distance spread evenly over moving time. Every
 * split has the same pace, so the UI labels the source as estimated.
 */
private fun estimatedSplits(workout: ExerciseData, unit: Double): List<RawSplit> {
    val total = workout.totalDistanceMeters ?: 0.0
    if (!isPositive(total)) return emptyList()
    val moving = workout.movingDurationMs()
    val durationNanos = if (moving <= 0) 0.0 else moving * 1_000_000.0

    val splits = mutableListOf<RawSplit>()
    var covered = 0.0
    var index = 0
    while (covered < total && index < MaxSplits) {
        val distance = min(unit, total - covered)
        // Below the partial floor the remainder is noise.
        if (distance < MinPartialMeters && index > 0) break
        val startFraction = covered / total
        val endFraction = (covered + distance) / total
        splits.add(
            RawSplit(
                startTime = workout.startTime.plusNanos((durationNanos * startFraction).roundToLong()),
                endTime = workout.startTime.plusNanos((durationNanos * endFraction).roundToLong()),
                distanceMeters = distance,
                isPartial = distance < unit,
                // No route: elevation is unknown, not zero.
            ),
        )
        covered += distance
        index++
    }
    return splits
}
