package tech.mmarca.openvitals.core.geo

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Great-circle (haversine) distance in meters between two WGS84 coordinates. */
internal fun haversineMeters(
    startLatitude: Double,
    startLongitude: Double,
    endLatitude: Double,
    endLongitude: Double,
): Double {
    val lat1 = Math.toRadians(startLatitude)
    val lat2 = Math.toRadians(endLatitude)
    val deltaLat = Math.toRadians(endLatitude - startLatitude)
    val deltaLon = Math.toRadians(endLongitude - startLongitude)
    val a = sin(deltaLat / 2.0) * sin(deltaLat / 2.0) +
        cos(lat1) * cos(lat2) * sin(deltaLon / 2.0) * sin(deltaLon / 2.0)
    val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))
    return EarthRadiusMeters * c
}

private const val EarthRadiusMeters = 6_371_000.0
