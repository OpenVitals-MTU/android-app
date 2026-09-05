package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record

/**
 * Health Connect caps a record at roughly 1 MB, and a long GPS route
 * crosses it. The error names the limit and the actual size, which is
 * enough to decimate the route by that ratio and retry.
 */
internal object OversizedRouteShrinker {

    private val recordSizeRegex = Regex("single record size limit:\\s*(\\d+),\\s*was:\\s*(\\d+)")

    /** How much of the route to keep below the limit, for per-point overhead the ratio misses. */
    private const val Margin = 0.9

    /** The smallest route worth keeping; Health Connect wants at least two points. */
    private const val MinPoints = 2

    /** The limit and actual size out of a record-size failure, or null. */
    fun recordSizeOverrun(error: Throwable): Pair<Long, Long>? {
        val match = recordSizeRegex.find(error.message.orEmpty()) ?: return null
        val limit = match.groupValues[1].toLongOrNull() ?: return null
        val was = match.groupValues[2].toLongOrNull() ?: return null
        if (limit <= 0L || was <= limit) return null
        return limit to was
    }

    /** [records] with every route decimated by `limit / was`, or null when nothing can shrink. */
    fun shrink(records: List<Record>, limit: Long, was: Long): List<Record>? {
        val keepRatio = (limit.toDouble() / was.toDouble()) * Margin
        var shrankAny = false
        val result = records.map { record ->
            val session = record as? ExerciseSessionRecord ?: return@map record
            val route = (session.exerciseRouteResult as? ExerciseRouteResult.Data)?.exerciseRoute
                ?: return@map record
            val points = route.route
            val target = (points.size * keepRatio).toInt().coerceAtLeast(MinPoints)
            if (points.size <= MinPoints || target >= points.size) return@map record
            shrankAny = true
            session.withRoute(ExerciseRoute(decimate(points, target)))
        }
        return if (shrankAny) result else null
    }

    /** Keeps [target] points spread evenly, first and last included, timestamps strictly increasing. */
    fun decimate(points: List<ExerciseRoute.Location>, target: Int): List<ExerciseRoute.Location> {
        if (target >= points.size) return points
        val lastIndex = points.lastIndex
        val kept = ArrayList<ExerciseRoute.Location>(target)
        var previous = -1
        for (slot in 0 until target) {
            // The last slot lands exactly on lastIndex.
            var index = (slot.toLong() * lastIndex / (target - 1)).toInt()
            if (index <= previous) index = previous + 1
            kept.add(points[index])
            previous = index
        }
        return kept
    }

    private fun ExerciseSessionRecord.withRoute(route: ExerciseRoute): ExerciseSessionRecord =
        ExerciseSessionRecord(
            startTime = startTime,
            startZoneOffset = startZoneOffset,
            endTime = endTime,
            endZoneOffset = endZoneOffset,
            metadata = metadata,
            exerciseType = exerciseType,
            title = title,
            notes = notes,
            segments = segments,
            laps = laps,
            exerciseRoute = route,
            plannedExerciseSessionId = plannedExerciseSessionId,
        )
}
