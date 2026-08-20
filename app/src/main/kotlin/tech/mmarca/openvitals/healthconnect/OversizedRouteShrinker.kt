package tech.mmarca.openvitals.healthconnect

import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.Record

/**
 * Health Connect caps a single record at roughly 1 MB, a limit its platform
 * `RateLimiter` enforces and no API exposes. The only record this app builds
 * whose size follows the data is an exercise session with a GPS route, and a
 * multi-hour ride recorded at one point per second crosses the cap. The insert
 * then fails the same way on every retry, which in a batched import also
 * takes the whole batch down with it.
 *
 * The platform does say how far over the record was
 * ("single record size limit: 1000000, was: 1700644"), which is enough to
 * decimate the route by that ratio and try again. Seen first in Gadgetbridge's
 * Health Connect exporter, which hit the same wall with the same watches.
 */
internal object OversizedRouteShrinker {

    private val recordSizeRegex = Regex("single record size limit:\\s*(\\d+),\\s*was:\\s*(\\d+)")

    /** How much of the route to keep below the limit, for per-point overhead the ratio misses. */
    private const val Margin = 0.9

    /** The smallest route worth keeping; Health Connect wants at least two points. */
    private const val MinPoints = 2

    /**
     * Parses the limit and actual size out of a record-size failure, or null
     * when [error] is about something else.
     */
    fun recordSizeOverrun(error: Throwable): Pair<Long, Long>? {
        val match = recordSizeRegex.find(error.message.orEmpty()) ?: return null
        val limit = match.groupValues[1].toLongOrNull() ?: return null
        val was = match.groupValues[2].toLongOrNull() ?: return null
        if (limit <= 0L || was <= limit) return null
        return limit to was
    }

    /**
     * Returns [records] with every routed exercise session's route decimated
     * by `limit / was`, or null when nothing in the list can be made smaller,
     * so the caller lets the original failure stand.
     */
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

    /**
     * Keeps [target] points spread evenly over the route, always including
     * the first and the last. Each index is taken once, so timestamps stay
     * strictly increasing, which Health Connect requires of a route.
     */
    fun decimate(points: List<ExerciseRoute.Location>, target: Int): List<ExerciseRoute.Location> {
        if (target >= points.size) return points
        val lastIndex = points.lastIndex
        val kept = ArrayList<ExerciseRoute.Location>(target)
        var previous = -1
        for (slot in 0 until target) {
            // Rounded position of this slot along the route; the last slot
            // lands exactly on lastIndex.
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
