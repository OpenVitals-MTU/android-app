package tech.mmarca.openvitals.features.manualentry.activity.recording

import java.time.Duration
import java.time.Instant
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import tech.mmarca.openvitals.domain.model.ActivityRecordingLap
import tech.mmarca.openvitals.domain.model.ExerciseLapData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

internal enum class ActivityRecordingTab {
    MAP,
    STATS,
    INTERVALS,
    BY_TIME,
    BY_DISTANCE,
}

internal data class ActivityRecordingSplit(
    val index: Int,
    val startTime: Instant?,
    val endTime: Instant?,
    val startDistanceMeters: Double,
    val endDistanceMeters: Double?,
    val distanceMeters: Double,
    val elapsedMillis: Long,
    val averageSpeedMetersPerSecond: Double,
    val maxSpeedMetersPerSecond: Double,
    val climbMeters: Double,
)

internal fun activityRecordingIntervalSplits(
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
): List<ActivityRecordingSplit> =
    points.toContinuousRouteSegments(routeBreakIndexes)
        .mapIndexedNotNull { index, segment ->
            segment.toRouteSegmentSplit(index = index + 1)
        }

internal fun activityRecordingLapSplits(
    laps: List<ActivityRecordingLap>,
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
    recordingStartTime: Instant?,
    activeEndTime: Instant? = null,
): List<ActivityRecordingSplit> {
    if (recordingStartTime == null || laps.isEmpty()) return emptyList()
    val closedSplits = laps
        .sortedBy { it.startTime }
        .mapIndexedNotNull { index, lap ->
            lap.toSplit(
                index = index + 1,
                points = points,
                routeBreakIndexes = routeBreakIndexes,
            )
        }
    val openStart = laps.maxByOrNull { it.endTime }?.endTime ?: recordingStartTime
    val openEnd = activeEndTime
    val openSplit = if (openEnd != null && openStart.isBefore(openEnd)) {
        ActivityRecordingLap(
            startTime = openStart,
            endTime = openEnd,
            distanceMeters = null,
        ).toSplit(
            index = closedSplits.size + 1,
            points = points,
            routeBreakIndexes = routeBreakIndexes,
        )
    } else {
        null
    }
    return closedSplits + listOfNotNull(openSplit)
}

internal fun exerciseLapSplits(
    laps: List<ExerciseLapData>,
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int> = emptyList(),
): List<ActivityRecordingSplit> =
    laps
        .sortedBy { it.startTime }
        .map { lap ->
            ActivityRecordingLap(
                startTime = lap.startTime,
                endTime = lap.endTime,
                distanceMeters = lap.lengthMeters,
            )
        }
        .mapIndexedNotNull { index, lap ->
            lap.toSplit(
                index = index + 1,
                points = points,
                routeBreakIndexes = routeBreakIndexes,
            )
        }

internal fun activityRecordingRouteDistanceMeters(
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
    startTime: Instant,
    endTime: Instant,
): Double =
    points.routeStatsBetween(routeBreakIndexes, startTime, endTime).distanceMeters

internal fun activityRecordingTimeSplits(
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
    splitMillis: Long,
): List<ActivityRecordingSplit> {
    if (splitMillis <= 0L) return emptyList()
    val firstTime = points.firstOrNull()?.time ?: return emptyList()
    val buckets = linkedMapOf<Int, MutableSplitStats>()

    points.toContinuousRouteSegments(routeBreakIndexes).forEach { segment ->
        segment.zipWithNext().forEach { (start, end) ->
            val elapsedMillis = Duration.between(start.time, end.time).toMillis()
            if (elapsedMillis <= 0L) return@forEach

            val pairStartOffset = Duration.between(firstTime, start.time).toMillis().coerceAtLeast(0L)
            val pairEndOffset = Duration.between(firstTime, end.time).toMillis().coerceAtLeast(pairStartOffset)
            val pairStart = pairStartOffset.toDouble()
            var cursor = pairStart
            val pairEnd = pairEndOffset.toDouble()
            val distanceMeters = start.distanceMetersTo(end)
            val climbMeters = start.climbMetersTo(end)
            val speedMetersPerSecond = distanceMeters.speedFor(elapsedMillis.toDouble())

            while (cursor < pairEnd - SplitEpsilon) {
                val bucketIndex = floor(cursor / splitMillis).toInt()
                val bucketEnd = minOf((bucketIndex + 1) * splitMillis.toDouble(), pairEnd)
                val fraction = (bucketEnd - cursor) / (pairEnd - pairStart)
                buckets.getOrPut(bucketIndex) { MutableSplitStats() }
                    .add(
                        distanceMeters = distanceMeters * fraction,
                        elapsedMillis = elapsedMillis * fraction,
                        climbMeters = climbMeters * fraction,
                        speedMetersPerSecond = speedMetersPerSecond,
                    )
                cursor = bucketEnd
            }
        }
    }

    return buckets.entries.map { (bucketIndex, stats) ->
        val startOffset = bucketIndex * splitMillis
        ActivityRecordingSplit(
            index = bucketIndex + 1,
            startTime = firstTime.plusMillis(startOffset),
            endTime = firstTime.plusMillis(startOffset + splitMillis),
            startDistanceMeters = 0.0,
            endDistanceMeters = null,
            distanceMeters = stats.distanceMeters,
            elapsedMillis = stats.elapsedMillisRounded(),
            averageSpeedMetersPerSecond = stats.averageSpeedMetersPerSecond(),
            maxSpeedMetersPerSecond = stats.maxSpeedMetersPerSecond,
            climbMeters = stats.climbMeters,
        )
    }
}

internal fun activityRecordingDistanceSplits(
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
    splitMeters: Double,
): List<ActivityRecordingSplit> {
    if (splitMeters <= 0.0 || !splitMeters.isFinite()) return emptyList()
    val buckets = linkedMapOf<Int, MutableSplitStats>()
    var routeDistanceMeters = 0.0

    points.toContinuousRouteSegments(routeBreakIndexes).forEach { segment ->
        segment.zipWithNext().forEach { (start, end) ->
            val elapsedMillis = Duration.between(start.time, end.time).toMillis()
            if (elapsedMillis <= 0L) return@forEach

            val distanceMeters = start.distanceMetersTo(end)
            if (distanceMeters <= SplitEpsilon) return@forEach

            val climbMeters = start.climbMetersTo(end)
            val speedMetersPerSecond = distanceMeters.speedFor(elapsedMillis.toDouble())
            var consumedMeters = 0.0
            while (consumedMeters < distanceMeters - SplitEpsilon) {
                val absoluteDistance = routeDistanceMeters + consumedMeters
                val bucketIndex = floor(absoluteDistance / splitMeters).toInt()
                val bucketEndDistance = minOf((bucketIndex + 1) * splitMeters, routeDistanceMeters + distanceMeters)
                val distancePortion = bucketEndDistance - absoluteDistance
                if (distancePortion <= SplitEpsilon) break

                val fraction = distancePortion / distanceMeters
                buckets.getOrPut(bucketIndex) { MutableSplitStats() }
                    .add(
                        distanceMeters = distancePortion,
                        elapsedMillis = elapsedMillis * fraction,
                        climbMeters = climbMeters * fraction,
                        speedMetersPerSecond = speedMetersPerSecond,
                    )
                consumedMeters += distancePortion
            }
            routeDistanceMeters += distanceMeters
        }
    }

    return buckets.entries.map { (bucketIndex, stats) ->
        ActivityRecordingSplit(
            index = bucketIndex + 1,
            startTime = null,
            endTime = null,
            startDistanceMeters = bucketIndex * splitMeters,
            endDistanceMeters = (bucketIndex + 1) * splitMeters,
            distanceMeters = stats.distanceMeters,
            elapsedMillis = stats.elapsedMillisRounded(),
            averageSpeedMetersPerSecond = stats.averageSpeedMetersPerSecond(),
            maxSpeedMetersPerSecond = stats.maxSpeedMetersPerSecond,
            climbMeters = stats.climbMeters,
        )
    }
}

private fun ActivityRecordingLap.toSplit(
    index: Int,
    points: List<ExerciseRoutePoint>,
    routeBreakIndexes: List<Int>,
): ActivityRecordingSplit? {
    if (!startTime.isBefore(endTime)) return null
    val elapsedMillis = Duration.between(startTime, endTime).toMillis().coerceAtLeast(0L)
    if (elapsedMillis <= 0L) return null
    val stats = points.routeStatsBetween(routeBreakIndexes, startTime, endTime)
    val splitDistanceMeters = when {
        stats.distanceMeters > 0.0 -> stats.distanceMeters
        distanceMeters != null -> distanceMeters
        else -> 0.0
    }
    return ActivityRecordingSplit(
        index = index,
        startTime = startTime,
        endTime = endTime,
        startDistanceMeters = 0.0,
        endDistanceMeters = null,
        distanceMeters = splitDistanceMeters,
        elapsedMillis = elapsedMillis,
        averageSpeedMetersPerSecond = splitDistanceMeters.speedFor(elapsedMillis.toDouble()),
        maxSpeedMetersPerSecond = stats.maxSpeedMetersPerSecond,
        climbMeters = stats.climbMeters,
    )
}

private fun List<ExerciseRoutePoint>.toRouteSegmentSplit(index: Int): ActivityRecordingSplit? {
    if (size < 2) return null
    val stats = MutableSplitStats()
    zipWithNext().forEach { (start, end) ->
        val elapsedMillis = Duration.between(start.time, end.time).toMillis()
        if (elapsedMillis <= 0L) return@forEach

        val distanceMeters = start.distanceMetersTo(end)
        stats.add(
            distanceMeters = distanceMeters,
            elapsedMillis = elapsedMillis.toDouble(),
            climbMeters = start.climbMetersTo(end),
            speedMetersPerSecond = distanceMeters.speedFor(elapsedMillis.toDouble()),
        )
    }
    if (stats.elapsedMillis <= 0.0) return null

    return ActivityRecordingSplit(
        index = index,
        startTime = first().time,
        endTime = last().time,
        startDistanceMeters = 0.0,
        endDistanceMeters = null,
        distanceMeters = stats.distanceMeters,
        elapsedMillis = stats.elapsedMillisRounded(),
        averageSpeedMetersPerSecond = stats.averageSpeedMetersPerSecond(),
        maxSpeedMetersPerSecond = stats.maxSpeedMetersPerSecond,
        climbMeters = stats.climbMeters,
    )
}

internal fun List<ExerciseRoutePoint>.toContinuousRouteSegments(routeBreakIndexes: List<Int>): List<List<ExerciseRoutePoint>> {
    if (isEmpty()) return emptyList()
    val breakIndexes = routeBreakIndexes
        .filter { it in 1 until size }
        .toSet()
    val segments = mutableListOf<MutableList<ExerciseRoutePoint>>()
    forEachIndexed { index, point ->
        if (index == 0 || index in breakIndexes) {
            segments += mutableListOf(point)
        } else {
            segments.lastOrNull()?.add(point)
        }
    }
    return segments
}

private fun List<ExerciseRoutePoint>.routeStatsBetween(
    routeBreakIndexes: List<Int>,
    startTime: Instant,
    endTime: Instant,
): MutableSplitStats {
    val stats = MutableSplitStats()
    if (!startTime.isBefore(endTime)) return stats
    toContinuousRouteSegments(routeBreakIndexes).forEach { segment ->
        segment.zipWithNext().forEach { (start, end) ->
            val elapsedMillis = Duration.between(start.time, end.time).toMillis()
            if (elapsedMillis <= 0L) return@forEach
            val overlapStart = maxOf(start.time, startTime)
            val overlapEnd = minOf(end.time, endTime)
            if (!overlapStart.isBefore(overlapEnd)) return@forEach

            val overlapMillis = Duration.between(overlapStart, overlapEnd)
                .toMillis()
                .coerceAtLeast(0L)
            if (overlapMillis <= 0L) return@forEach
            val fraction = overlapMillis.toDouble() / elapsedMillis.toDouble()
            val distanceMeters = start.distanceMetersTo(end)
            stats.add(
                distanceMeters = distanceMeters * fraction,
                elapsedMillis = overlapMillis.toDouble(),
                climbMeters = start.climbMetersTo(end) * fraction,
                speedMetersPerSecond = distanceMeters.speedFor(elapsedMillis.toDouble()),
            )
        }
    }
    return stats
}

private class MutableSplitStats {
    var distanceMeters: Double = 0.0
        private set
    var elapsedMillis: Double = 0.0
        private set
    var climbMeters: Double = 0.0
        private set
    var maxSpeedMetersPerSecond: Double = 0.0
        private set

    fun add(
        distanceMeters: Double,
        elapsedMillis: Double,
        climbMeters: Double,
        speedMetersPerSecond: Double,
    ) {
        this.distanceMeters += distanceMeters.coerceAtLeast(0.0)
        this.elapsedMillis += elapsedMillis.coerceAtLeast(0.0)
        this.climbMeters += climbMeters.coerceAtLeast(0.0)
        maxSpeedMetersPerSecond = maxOf(maxSpeedMetersPerSecond, speedMetersPerSecond)
    }

    fun elapsedMillisRounded(): Long =
        elapsedMillis.toLong().coerceAtLeast(0L)

    fun averageSpeedMetersPerSecond(): Double =
        distanceMeters.speedFor(elapsedMillis)
}

private fun Double.speedFor(elapsedMillis: Double): Double {
    val elapsedSeconds = elapsedMillis / 1_000.0
    return if (this > 0.0 && elapsedSeconds > 0.0) this / elapsedSeconds else 0.0
}

private fun ExerciseRoutePoint.climbMetersTo(other: ExerciseRoutePoint): Double {
    val startAltitude = altitudeMeters ?: return 0.0
    val endAltitude = other.altitudeMeters ?: return 0.0
    val delta = endAltitude - startAltitude
    return if (delta >= MinSplitClimbMeters) delta else 0.0
}

private fun ExerciseRoutePoint.distanceMetersTo(other: ExerciseRoutePoint): Double {
    val lat1 = Math.toRadians(latitude)
    val lat2 = Math.toRadians(other.latitude)
    val deltaLat = Math.toRadians(other.latitude - latitude)
    val deltaLon = Math.toRadians(other.longitude - longitude)
    val a = sin(deltaLat / 2.0).pow(2.0) +
        cos(lat1) * cos(lat2) * sin(deltaLon / 2.0).pow(2.0)
    val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    return EarthRadiusMeters * c
}

private const val EarthRadiusMeters = 6_371_000.0
private const val MinSplitClimbMeters = 1.0
private const val SplitEpsilon = 0.000001
