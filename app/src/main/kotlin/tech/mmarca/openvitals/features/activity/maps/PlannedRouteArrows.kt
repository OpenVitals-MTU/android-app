package tech.mmarca.openvitals.features.activity.maps

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline

/**
 * A direction arrow on the planned route, shaped the way CoMaps shapes its
 * own: a short white shaft that FOLLOWS the line into the bend and out of it,
 * ending in a head that points the way on.
 *
 * A straight marker sitting on the corner reads as a label; an arrow bent
 * through the corner reads as the manoeuvre itself.
 */
internal class PlannedRouteArrow(
    /** Interleaved `lat, lon`: entry arm, the corner, exit arm. */
    val shaft: DoubleArray,
    /** Where the head sits — the tip of the exit arm. */
    val headLatitude: Double,
    val headLongitude: Double,
    /** Which way the head points, degrees clockwise from north. */
    val bearingDegrees: Float,
)

/**
 * The bends of the route: one bent arrow wherever the polyline turns by
 * [minTurnDegrees] or more. Derived from geometry alone — the provider shares
 * geometry, not manoeuvres — which is exactly what a turn is geometrically.
 */
internal fun plannedRouteTurnArrows(
    polyline: CoMapsRoutePolyline?,
    minTurnDegrees: Double = 30.0,
    armMeters: Double = 16.0,
): List<PlannedRouteArrow> {
    if (polyline == null || polyline.pointCount < 3) return emptyList()
    val arrows = mutableListOf<PlannedRouteArrow>()
    for (index in 1 until polyline.pointCount - 1) {
        val cornerLat = polyline.latitudeAt(index)
        val cornerLon = polyline.longitudeAt(index)
        val bearingIn = bearingDegrees(
            polyline.latitudeAt(index - 1), polyline.longitudeAt(index - 1),
            cornerLat, cornerLon,
        ) ?: continue
        val bearingOut = bearingDegrees(
            cornerLat, cornerLon,
            polyline.latitudeAt(index + 1), polyline.longitudeAt(index + 1),
        ) ?: continue
        val turn = ((bearingOut - bearingIn + 540.0) % 360.0) - 180.0
        if (abs(turn) < minTurnDegrees) continue

        // The arms reach back along the incoming segment and on along the
        // outgoing one — at most [armMeters], never past the next corner.
        val entry = interpolateTowards(
            fromLat = cornerLat, fromLon = cornerLon,
            toLat = polyline.latitudeAt(index - 1), toLon = polyline.longitudeAt(index - 1),
            meters = armMeters,
        )
        val exit = interpolateTowards(
            fromLat = cornerLat, fromLon = cornerLon,
            toLat = polyline.latitudeAt(index + 1), toLon = polyline.longitudeAt(index + 1),
            meters = armMeters,
        )
        arrows += PlannedRouteArrow(
            shaft = doubleArrayOf(entry[0], entry[1], cornerLat, cornerLon, exit[0], exit[1]),
            headLatitude = exit[0],
            headLongitude = exit[1],
            bearingDegrees = bearingOut.toFloat(),
        )
    }
    return arrows
}

/**
 * The point [meters] from `from` towards `to`, capped at 80% of the way so
 * an arm never swallows the next corner.
 */
private fun interpolateTowards(
    fromLat: Double,
    fromLon: Double,
    toLat: Double,
    toLon: Double,
    meters: Double,
): DoubleArray {
    val distance = distanceMeters(fromLat, fromLon, toLat, toLon)
    if (distance <= 0.0) return doubleArrayOf(fromLat, fromLon)
    val t = (meters / distance).coerceAtMost(0.8)
    return doubleArrayOf(
        fromLat + (toLat - fromLat) * t,
        fromLon + (toLon - fromLon) * t,
    )
}

private fun distanceMeters(
    latitudeA: Double,
    longitudeA: Double,
    latitudeB: Double,
    longitudeB: Double,
): Double {
    var deltaLon = longitudeB - longitudeA
    if (deltaLon > 180.0) deltaLon -= 360.0
    if (deltaLon < -180.0) deltaLon += 360.0
    val east = deltaLon * cos(Math.toRadians((latitudeA + latitudeB) / 2.0))
    val north = latitudeB - latitudeA
    return hypot(east, north) * MetersPerDegree
}

/**
 * Initial bearing from one point to the next, degrees clockwise from north,
 * or null for a degenerate pair. Equirectangular: exact enough for the
 * road-length segments this is fed.
 */
private fun bearingDegrees(
    latitudeA: Double,
    longitudeA: Double,
    latitudeB: Double,
    longitudeB: Double,
): Double? {
    var deltaLon = longitudeB - longitudeA
    if (deltaLon > 180.0) deltaLon -= 360.0
    if (deltaLon < -180.0) deltaLon += 360.0
    val east = deltaLon * cos(Math.toRadians((latitudeA + latitudeB) / 2.0))
    val north = latitudeB - latitudeA
    if (east == 0.0 && north == 0.0) return null
    return (Math.toDegrees(atan2(east, north)) + 360.0) % 360.0
}

private const val MetersPerDegree = 111_320.0
