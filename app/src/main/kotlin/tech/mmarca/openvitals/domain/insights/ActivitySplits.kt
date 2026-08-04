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
 * Per-segment splits ("laps") for a distance-based activity.
 *
 * Pure arithmetic: no I/O, no Android framework beyond the Health Connect
 * constants. The activity detail screen feeds this whatever it managed to load
 * (route points, speed samples, heart rate samples) and renders
 * [ActivitySplits.splits] with the provenance in [ActivitySplits.source]
 * spelled out in the header — a split derived from a GPS route and a split
 * guessed from the session average are NOT the same claim, and the UI must not
 * present them as if they were.
 */

/**
 * Where the splits came from, in descending order of trustworthiness. Lives on
 * the result rather than on each row: one computation yields one provenance.
 */
internal enum class SplitSource {
    /**
     * The recording device/app wrote lap records. Shown as recorded — never
     * re-cut to the split distance, because a lap is whatever the device called
     * a lap (a track session's 400 m, a button press, an uneven interval).
     */
    DEVICE_LAPS,

    /** Cut from the GPS route by accumulating haversine distance between fixes. */
    ROUTE,

    /**
     * Cut by integrating SpeedRecord samples over time — the treadmill case,
     * where there is no route but the belt reports speed.
     */
    SPEED_SAMPLES,

    /**
     * Nothing per-time exists: total distance divided evenly over the duration.
     * Every split necessarily shows the activity's average pace. Honest, but the
     * UI MUST label it, or a flat line reads as a real (and eerily even) run.
     */
    ESTIMATED,
}

/** One split row. */
internal data class ActivitySplit(
    /** 1-based, as displayed. */
    val index: Int,

    /**
     * The split's own distance. The final split is usually a partial and keeps
     * its real (short) distance — see [isPartial].
     */
    val distanceMeters: Double,

    /**
     * MOVING time: the split's window less any of it the recording was paused
     * for. Everything derived from a split — its pace, the speed trace rebuilt
     * from it, the pace delta — divides by this.
     *
     * Wall-clock would count a pause as riding. A real 21-minute bike ride with a
     * 10½-minute pause in it reported 4.9 km/h average and a 4.1 km/h first
     * kilometre, because the pause sat inside that kilometre's window and nothing
     * took it out again. It was 12 km/h. [startTime] and [endTime] stay
     * wall-clock — they say WHEN, and the heart-rate mean and the chart's x axis
     * need that.
     */
    val elapsedMs: Long,
    val startTime: Instant,
    val endTime: Instant,

    /**
     * True when this split is shorter than the requested split distance (the
     * trailing remainder). Device laps are never marked partial: an uneven lap
     * is not an incomplete one.
     */
    val isPartial: Boolean,

    /**
     * Mean of the heart-rate samples inside `[startTime, endTime)`, rounded to
     * whole beats. Null when no sample falls in the window — never 0.
     */
    val averageHeartRateBpm: Double? = null,

    /**
     * Cumulative ascent/descent across the split, from the route's altitudes.
     * Null (NOT 0) when the split has no altitude data — a treadmill run did not
     * climb zero meters, it climbed an unknown number of them.
     */
    val elevationGainMeters: Double? = null,
    val elevationLossMeters: Double? = null,

    /**
     * This split's pace minus the whole activity's average pace, in seconds per
     * kilometer (storage is metric; [paceDeltaSecondsPerUnit] converts at the
     * display boundary). Negative = faster than the activity average.
     *
     * Null when either pace is undefined (zero distance or zero elapsed).
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

/**
 * A trailing remainder shorter than this is dropped rather than shown as a
 * "0 m" split — a GPS route that overshoots 5 km by 40 cm did not run a sixth
 * split.
 */
private const val MinPartialMeters = 1.0

/**
 * Guards a pathological (or hostile) preference: a 1 cm split distance over a
 * marathon would otherwise try to build 4.2 million rows.
 */
private const val MaxSplits = 500

/**
 * Splits for [workout], cut every [splitDistanceMeters] — unless the workout
 * already carries device laps, which win outright.
 *
 * Source priority: device laps > GPS route > speed samples > estimated. See
 * [SplitSource]. Returns an empty result for an activity that does not travel,
 * and for one that travelled no measurable distance.
 */
internal fun buildActivitySplits(
    workout: ExerciseData,
    routePoints: List<ExerciseRoutePoint>,
    speedSamples: List<SpeedSample>,
    heartRateSamples: List<HeartRateSample>,
    splitDistanceMeters: Double,
): ActivitySplits {
    // Whether an activity HAS splits is a question about its kind, not its data.
    // The old gate only asked "is there any distance?", and a strength session
    // answers yes: a phone left on the bench picks up a couple of hundred metres of
    // GPS drift, Health Connect records it faithfully, and a lifting session was
    // duly cut into "1.0 km" and "181 m" splits at a 30:29 min/km pace. The distance
    // was real; the splits were nonsense.
    if (!isDistanceBasedExercise(workout.exerciseType)) {
        return ActivitySplits.none()
    }

    val unit = if (splitDistanceMeters.isFinite() && splitDistanceMeters > 0) {
        splitDistanceMeters
    } else {
        DefaultSplitDistanceMeters
    }

    // Defensive: nothing guarantees the caller's samples are time-ordered (a
    // Health Connect read merges several source apps), and every walk below
    // assumes they are.
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

    // The yardstick for paceDelta is the WHOLE activity's average pace, not the
    // mean of the split paces: a 200 m partial should not drag the baseline.
    val activityPaceSecondsPerMeter = activityPaceSecondsPerMeter(workout, raw)

    val splits = raw.mapIndexed { i, entry ->
        // Estimated splits are exempt: their windows are a fiction (total distance
        // spread evenly over the session), already laid out over moving time, so
        // taking a pause out of one of them again would both double-count it and
        // hand a single split a pace the others do not share — the one property
        // that source has.
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

/**
 * How much of `[start, end)` the recording was paused for, in milliseconds.
 *
 * Pauses reach Health Connect as `EXERCISE_SEGMENT_TYPE_PAUSE` segments (see
 * the recording writer), so a session read back knows where they were — this is
 * the same fact `ActivityMetrics.pausedDurationMs` totals for the whole
 * workout, measured against one split's window.
 */
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
    // A split cannot be more than entirely paused, whatever overlapping segments
    // a source app wrote.
    return min(paused, to - from)
}

/**
 * Laps with a sane time window, oldest first. A lap that ends before it starts
 * is a source-app bug, not a lap.
 */
private fun usableLaps(workout: ExerciseData): List<ExerciseLapData> =
    workout.laps
        .filter { !it.endTime.isBefore(it.startTime) }
        .sortedBy { it.startTime }

/**
 * The whole activity's average pace in seconds per meter — the baseline every
 * split's [ActivitySplit.paceDeltaSecondsPerKilometer] is measured against.
 *
 * Prefers the recorded session totals; falls back to the sum of the splits
 * when the session has no distance/duration of its own (laps-only imports).
 */
private fun activityPaceSecondsPerMeter(workout: ExerciseData, raw: List<RawSplit>): Double? {
    val recordedDistance = workout.totalDistanceMeters ?: 0.0
    // Moving, not wall-clock, so the baseline every split is compared against is
    // measured the same way the splits are.
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
        // Half-open [start, end): a sample on a boundary belongs to exactly one
        // split, never to both.
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

/**
 * A point on a monotone distance-over-time curve, whatever produced it: a GPS
 * fix, a speed sample, or an interpolated split boundary. Sharing one node
 * type is what lets the route and speed-sample cutters be the same code.
 */
private data class Node(
    val time: Instant,
    val cumulativeMeters: Double,
    val altitudeMeters: Double? = null,
)

/**
 * GPS fixes → cumulative haversine distance. Non-finite or backwards segments
 * contribute 0 rather than corrupting the curve.
 */
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
 * Speed samples → cumulative distance by trapezoidal integration of v·dt.
 * No altitude: a SpeedRecord says nothing about the ground going up.
 *
 * The same integration `distanceFromSpeedSamples` (ActivityBackfill.kt) totals
 * for the whole session, kept per-node here so the curve can be cut.
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
 * Cut a distance-over-time curve every [unit] meters.
 *
 * The crossing is INTERPOLATED between the two bracketing nodes, not snapped
 * to the next one: at a 5 s GPS cadence, snapping quantises every split's
 * elapsed time to ±5 s, which on a 1 km split is a visible ~8 s/km pace error.
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
            // The crossing sits between nodes[i-1] and nodes[i]. `splitStartIndex` is
            // by convention the index of the node at-or-before the split start, so
            // the next split's interior nodes begin at i (and nodes[i] is NOT lost).
            splitStartIndex = i - 1
            boundary += unit
        }
    }

    // The trailing remainder: a real, shorter split, kept and flagged.
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
 * Cumulative ascent/descent from [start] through the nodes strictly inside
 * `(startIndex, endIndex]`… and on to [end]. Null when no two consecutive
 * points in the window both carry an altitude — a split with no altitude data
 * gained an UNKNOWN amount of elevation, not zero.
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

/**
 * Device laps, shown as recorded. A lap's length is what the device wrote; if
 * it wrote none, the route (if any) supplies it, and failing that the lap has
 * no distance to show.
 */
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
            // A device lap is never "partial": an uneven lap is not a truncated
            // one, it is simply the lap the device recorded.
            isPartial = false,
            elevationGainMeters = fromRoute?.elevation?.gain,
            elevationLossMeters = fromRoute?.elevation?.loss,
        )
    }

private data class RouteSpan(val distanceMeters: Double, val elevation: Elevation?)

/**
 * Route distance + elevation between two times, for a lap that spans part of
 * a recorded route.
 */
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
 * The last resort: total distance spread evenly over the duration. Every split
 * gets the same pace by construction — which is exactly why the UI labels this
 * source as estimated instead of drawing a suspiciously flat bar chart and
 * letting the user believe it.
 *
 * Spread over MOVING duration: the one number these splits do claim is a pace,
 * and a pace measured against a clock that ran through a pause is not one.
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
        // Below the partial floor the remainder is noise, not a split.
        if (distance < MinPartialMeters && index > 0) break
        val startFraction = covered / total
        val endFraction = (covered + distance) / total
        splits.add(
            RawSplit(
                startTime = workout.startTime.plusNanos((durationNanos * startFraction).roundToLong()),
                endTime = workout.startTime.plusNanos((durationNanos * endFraction).roundToLong()),
                distanceMeters = distance,
                isPartial = distance < unit,
                // No route: elevation is unknown, and unknown is not zero.
            ),
        )
        covered += distance
        index++
    }
    return splits
}
