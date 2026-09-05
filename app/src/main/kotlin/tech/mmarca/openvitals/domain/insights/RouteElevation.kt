package tech.mmarca.openvitals.domain.insights

import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/**
 * Cumulative ascent from altitudes, with GPS noise filtered out. Summing
 * every positive step banks the ±3-5 m vertical error thousands of times.
 * Two filters: an exponential moving average, then hysteresis, so gain is
 * banked only once the smoothed altitude moves [MIN_STEP_METERS] from the
 * last accepted reference. Simulated flat routes report about 15 m.
 */
object RouteElevation {

    /** EMA weight. Heavier smoothing lags and under-reports sparse routes. */
    private const val SMOOTHING_ALPHA = 0.3

    /** How far the smoothed altitude must move before it counts. Above smoothed GPS noise. */
    private const val MIN_STEP_METERS = 5.0

    data class Change(val gain: Double, val loss: Double)

    /** Cumulative ascent in meters, ignoring nulls and non-finite values. */
    fun elevationGainFromAltitudes(altitudes: Iterable<Double?>): Double =
        accumulate(altitudes).gain

    /** Cumulative descent in meters, as a positive number. */
    fun elevationLossFromAltitudes(altitudes: Iterable<Double?>): Double =
        accumulate(altitudes).loss

    /** Ascent and descent in one pass, for callers that need both. */
    fun elevationChangeFromAltitudes(altitudes: Iterable<Double?>): Change =
        accumulate(altitudes)

    /** Cumulative ascent over a recorded or imported route. */
    fun routeElevationGain(points: List<ExerciseRoutePoint>): Double =
        elevationGainFromAltitudes(points.map { it.altitudeMeters })

    /** Cumulative descent over a recorded or imported route, as a positive number. */
    fun routeElevationLoss(points: List<ExerciseRoutePoint>): Double =
        elevationLossFromAltitudes(points.map { it.altitudeMeters })

    private fun accumulate(altitudes: Iterable<Double?>): Change {
        var smoothed: Double? = null
        var reference: Double? = null
        var lastAltitude: Double? = null
        var gain = 0.0
        var loss = 0.0

        for (altitude in altitudes) {
            if (altitude == null || !altitude.isFinite()) continue
            lastAltitude = altitude
            val nextSmoothed = smoothed?.let { it + (altitude - it) * SMOOTHING_ALPHA } ?: altitude
            smoothed = nextSmoothed
            val currentReference = reference
            if (currentReference == null) {
                reference = nextSmoothed
                continue
            }
            val delta = nextSmoothed - currentReference
            if (delta >= MIN_STEP_METERS) {
                gain += delta
                reference = nextSmoothed
            } else if (delta <= -MIN_STEP_METERS) {
                loss += -delta
                reference = nextSmoothed
            }
            // Anything smaller is noise: the reference does not move.
        }

        // Settle the smoothing lag against the final raw altitude: on a short
        // route the trailing tail is a large share of the whole.
        val finalAltitude = lastAltitude
        val finalReference = reference
        if (finalAltitude != null && finalReference != null) {
            val residual = finalAltitude - finalReference
            if (residual >= MIN_STEP_METERS) {
                gain += residual
            } else if (residual <= -MIN_STEP_METERS) {
                loss += -residual
            }
        }

        return Change(gain = gain, loss = loss)
    }
}
