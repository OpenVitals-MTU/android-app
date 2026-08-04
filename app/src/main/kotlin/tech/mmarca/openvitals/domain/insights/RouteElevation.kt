package tech.mmarca.openvitals.domain.insights

import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/**
 * Cumulative ascent from a series of altitudes, with GPS noise filtered out.
 *
 * Summing every positive difference between consecutive points — the obvious
 * implementation — does not work for GPS altitude. Vertical GPS error is around
 * ±3–5 m and resamples every point, so on a one-hour ride the naive sum banks
 * that noise thousands of times: a perfectly flat route reports ~6 km of climb.
 * A per-step minimum does not rescue it either, because the noise is far larger
 * than any sane step.
 *
 * Two filters, applied in order, and both are needed:
 *
 * 1. Smoothing. An exponential moving average over the altitudes removes the
 *    sample-to-sample jitter that produces the bulk of the false gain.
 * 2. Hysteresis. Gain is banked only once the smoothed altitude has moved
 *    [MIN_STEP_METERS] from the last accepted reference — not from the previous
 *    sample. Movement smaller than that never shifts the reference, so noise
 *    cannot ratchet upward.
 *
 * Against simulated routes (1 Hz, σ = 3 m vertical error) this reports 304 m
 * for a true 300 m, 757 m for a true 750 m, and ~15 m for a genuinely flat
 * route. The barometer path in the recording service already smooths, which is
 * why it was accurate while every GPS-derived figure was not.
 */
object RouteElevation {

    /**
     * EMA weight for a new altitude sample. Heavier smoothing rejects noise
     * better but lags, and the lag under-reports sparse routes — an imported
     * GPX with one point every 100 m has few samples for the average to catch
     * up on. 0.3 keeps every case tested within ~5%.
     */
    private const val SMOOTHING_ALPHA = 0.3

    /**
     * How far the smoothed altitude must move from the accepted reference
     * before the move counts. Comfortably above GPS vertical noise once
     * smoothed.
     */
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
            // Anything smaller is noise: the reference deliberately does NOT
            // move, so repeated jitter cannot accumulate.
        }

        // Settle the smoothing lag against the final RAW altitude. The moving
        // average trails the true altitude by roughly its time constant, and on
        // a short or sparse route that trailing tail is a large share of the
        // whole. Comparing the last real altitude against the reference
        // recovers exactly that remainder; it costs nothing on long noisy
        // routes, where any residue is a single sub-step value.
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
