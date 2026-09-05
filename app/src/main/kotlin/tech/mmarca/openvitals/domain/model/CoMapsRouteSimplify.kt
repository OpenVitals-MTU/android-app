package tech.mmarca.openvitals.domain.model

import kotlin.math.cos
import kotlin.math.hypot

/**
 * Douglas-Peucker over an interleaved `lat, lon` buffer, tolerance in
 * metres. Both endpoints survive, the result is an ordered subsequence,
 * and every dropped point lies within [toleranceMeters] of the line.
 */
fun simplifyCoMapsRoutePoints(
    points: DoubleArray,
    toleranceMeters: Double = CoMapsRouteToleranceMeters,
): DoubleArray {
    val count = points.size / 2
    if (count <= 2 || toleranceMeters <= 0.0) return points

    val keep = BooleanArray(count)
    keep[0] = true
    keep[count - 1] = true

    // Iterative, so a pathological route is not a pathological call depth.
    val ranges = ArrayDeque<Int>()
    ranges.addLast(0)
    ranges.addLast(count - 1)
    while (ranges.isNotEmpty()) {
        val end = ranges.removeLast()
        val start = ranges.removeLast()
        if (end - start < 2) continue

        var farthest = -1
        var farthestDistance = 0.0
        for (index in start + 1 until end) {
            val distance = perpendicularDistanceMeters(points, start, end, index)
            if (distance > farthestDistance) {
                farthestDistance = distance
                farthest = index
            }
        }
        if (farthest >= 0 && farthestDistance > toleranceMeters) {
            keep[farthest] = true
            ranges.addLast(start)
            ranges.addLast(farthest)
            ranges.addLast(farthest)
            ranges.addLast(end)
        }
    }

    val keptCount = keep.count { it }
    if (keptCount == count) return points
    val simplified = DoubleArray(keptCount * 2)
    var write = 0
    for (index in 0 until count) {
        if (!keep[index]) continue
        simplified[write++] = points[index * 2]
        simplified[write++] = points[index * 2 + 1]
    }
    return simplified
}

/** Distance in metres from [p] to segment [a]..[b]. Equirectangular plane around [a]. */
private fun perpendicularDistanceMeters(points: DoubleArray, a: Int, b: Int, p: Int): Double {
    val latA = points[a * 2]
    val lonA = points[a * 2 + 1]
    val cosLat = cos(Math.toRadians(latA))

    fun x(lon: Double): Double {
        var delta = lon - lonA
        // A route through the antimeridian must not measure around the globe.
        if (delta > 180.0) delta -= 360.0
        if (delta < -180.0) delta += 360.0
        return delta * cosLat * MetersPerDegree
    }

    fun y(lat: Double): Double = (lat - latA) * MetersPerDegree

    val bx = x(points[b * 2 + 1])
    val by = y(points[b * 2])
    val px = x(points[p * 2 + 1])
    val py = y(points[p * 2])

    val lengthSquared = bx * bx + by * by
    if (lengthSquared == 0.0) return hypot(px, py)
    val t = ((px * bx + py * by) / lengthSquared).coerceIn(0.0, 1.0)
    return hypot(px - t * bx, py - t * by)
}

/** Under the drawn line width at any realistic zoom, so the line still bends at every bend. */
const val CoMapsRouteToleranceMeters = 3.0

private const val MetersPerDegree = 111_320.0
