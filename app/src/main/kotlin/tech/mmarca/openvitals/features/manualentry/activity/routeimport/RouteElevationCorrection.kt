package tech.mmarca.openvitals.features.manualentry.activity.routeimport

import android.util.Log
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.insights.RouteElevation
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.activity.elevation.ElevationTileRepository

/**
 * Swaps every point's altitude for the DEM value from [lookup]. Null as soon
 * as one point has none: a route half on the DEM and half on a drifting
 * barometer would be worse than either.
 */
internal fun correctRouteAltitudes(
    points: List<ExerciseRoutePoint>,
    lookup: (latitude: Double, longitude: Double) -> Double?,
): List<ExerciseRoutePoint>? {
    if (points.isEmpty()) return null
    val corrected = ArrayList<ExerciseRoutePoint>(points.size)
    for (point in points) {
        val altitude = lookup(point.latitude, point.longitude) ?: return null
        corrected += point.copy(altitudeMeters = altitude)
    }
    return corrected
}

/** The import with DEM altitudes and the gain recomputed from them, through the same filter a recording uses. */
internal fun RouteFileImport.withDemElevation(correctedPoints: List<ExerciseRoutePoint>): RouteFileImport =
    copy(
        points = correctedPoints,
        elevationGainedMeters = RouteElevation.routeElevationGain(correctedPoints),
    )

/**
 * Replaces an imported route's altitudes with offline DEM values. Off, no
 * tiles, or incomplete coverage all return the input unchanged.
 */
@Singleton
class RouteElevationCorrector @Inject constructor(
    private val tiles: ElevationTileRepository,
    private val preferences: PreferencesRepository,
) {

    fun correct(routeImport: RouteFileImport): RouteFileImport {
        if (routeImport.points.isEmpty()) return routeImport
        if (!preferences.elevationCorrectionEnabled || !tiles.hasTiles) return routeImport

        val corrected = try {
            correctRouteAltitudes(routeImport.points, tiles::elevationAt)
        } catch (error: IOException) {
            // A tile that vanished under a mapping: keep what the file said.
            log("${routeImport.fileName}: tile read failed, kept file altitudes: $error")
            return routeImport
        }
        if (corrected == null) {
            log("${routeImport.fileName}: no DEM coverage for every point, kept file altitudes")
            return routeImport
        }
        return routeImport.withDemElevation(corrected)
    }

    // Guarded: the JVM Log stub throws in tests that do not mock it.
    private fun log(message: String) {
        runCatching { Log.i(TAG, "[ELEVATION] $message") }
    }

    private companion object {
        const val TAG = "RouteElevation"
    }
}
