package tech.mmarca.openvitals.domain.insights

import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/**
 * Cumulative ascent from altitudes, with GPS noise filtered out. Summing
 * every positive step banks the ±3-5 m vertical error thousands of times.
 * Two filters: an exponential moving average, then hysteresis, so gain is
 * banked only once the smoothed altitude moves [MIN_STEP_METERS] from the
 * last accepted reference. Simulated flat routes report about 15 m.
 *
 * Route points also carry their fix's accuracy. A fixed 5 m step is tuned
 * for open sky; indoors or under structures the vertical error reaches tens
 * of meters and a flat walk banked 14 km. So a point's step grows to its own
 * uncertainty, and a point too uncertain to say anything is skipped.
 */
object RouteElevation {

    /** EMA weight. Heavier smoothing lags and under-reports sparse routes. */
    private const val SMOOTHING_ALPHA = 0.3

    /** How far the smoothed altitude must move before it counts. Above smoothed GPS noise. */
    private const val MIN_STEP_METERS = 5.0

    /** A fix whose altitude is less certain than this says nothing about a climb. */
    private const val MAX_ALTITUDE_UNCERTAINTY_METERS = 20.0

    data class Change(val gain: Double, val loss: Double)

    private class Sample(val index: Int, val altitude: Double, val stepMeters: Double)

    /** Cumulative ascent in meters, ignoring nulls and non-finite values. */
    fun elevationGainFromAltitudes(altitudes: Iterable<Double?>): Double =
        accumulate(altitudes.toSamples()).gain

    /** Cumulative descent in meters, as a positive number. */
    fun elevationLossFromAltitudes(altitudes: Iterable<Double?>): Double =
        accumulate(altitudes.toSamples()).loss

    /** Ascent and descent in one pass, for callers that need both. */
    fun elevationChangeFromAltitudes(altitudes: Iterable<Double?>): Change =
        accumulate(altitudes.toSamples())

    /** Cumulative ascent over a recorded or imported route. */
    fun routeElevationGain(points: List<ExerciseRoutePoint>): Double =
        accumulate(points.toSamples()).gain

    /** Cumulative descent over a recorded or imported route, as a positive number. */
    fun routeElevationLoss(points: List<ExerciseRoutePoint>): Double =
        accumulate(points.toSamples()).loss

    /**
     * The route with each altitude replaced by where the filter had settled
     * at that point: a staircase that moves only when a change is banked. Its
     * rises sum to [routeElevationGain] and its falls to [routeElevationLoss],
     * so splits cut from it add up to the whole. Run it over the whole route,
     * not per split, or each split starts the filter afresh. Points before the
     * first usable altitude keep none; later ones carry the settled value.
     */
    fun withSettledAltitudes(points: List<ExerciseRoutePoint>): List<ExerciseRoutePoint> {
        val settled = arrayOfNulls<Double>(points.size)
        accumulate(points.toSamples(), settled)
        var carried: Double? = null
        return points.mapIndexed { i, point ->
            carried = settled[i] ?: carried
            point.copy(altitudeMeters = carried)
        }
    }

    private fun Iterable<Double?>.toSamples(): Sequence<Sample> =
        asSequence()
            .withIndex()
            .mapNotNull { (i, altitude) ->
                altitude?.takeIf { it.isFinite() }?.let { Sample(i, it, MIN_STEP_METERS) }
            }

    private fun List<ExerciseRoutePoint>.toSamples(): Sequence<Sample> =
        asSequence().withIndex().mapNotNull { (i, point) ->
            val altitude = point.altitudeMeters?.takeIf { it.isFinite() } ?: return@mapNotNull null
            val uncertainty = point.altitudeUncertaintyMeters()
            when {
                uncertainty == null -> Sample(i, altitude, MIN_STEP_METERS)
                uncertainty > MAX_ALTITUDE_UNCERTAINTY_METERS -> null
                else -> Sample(i, altitude, maxOf(MIN_STEP_METERS, uncertainty))
            }
        }

    /**
     * The fix's vertical accuracy, floored at its horizontal accuracy: GPS
     * vertical error is rarely the smaller of the two, and phones under-report
     * it indoors. Null without a vertical accuracy, as on GPX imports and on
     * DEM-corrected altitudes, which the fix's accuracy no longer describes.
     */
    private fun ExerciseRoutePoint.altitudeUncertaintyMeters(): Double? {
        val vertical = verticalAccuracyMeters?.takeIf { it.isFinite() && it > 0.0 } ?: return null
        val horizontal = horizontalAccuracyMeters?.takeIf { it.isFinite() && it > 0.0 } ?: 0.0
        return maxOf(vertical, horizontal)
    }

    /** [settled], when given, receives the reference after each sample, by the sample's index. */
    private fun accumulate(samples: Sequence<Sample>, settled: Array<Double?>? = null): Change {
        var smoothed: Double? = null
        var reference: Double? = null
        var last: Sample? = null
        var gain = 0.0
        var loss = 0.0

        for (sample in samples) {
            last = sample
            val nextSmoothed = smoothed?.let { it + (sample.altitude - it) * SMOOTHING_ALPHA } ?: sample.altitude
            smoothed = nextSmoothed
            val currentReference = reference
            if (currentReference == null) {
                reference = nextSmoothed
                settled?.set(sample.index, nextSmoothed)
                continue
            }
            val delta = nextSmoothed - currentReference
            if (delta >= sample.stepMeters) {
                gain += delta
                reference = nextSmoothed
            } else if (delta <= -sample.stepMeters) {
                loss += -delta
                reference = nextSmoothed
            }
            // Anything smaller is noise: the reference does not move.
            settled?.set(sample.index, reference)
        }

        // Settle the smoothing lag against the final raw altitude: on a short
        // route the trailing tail is a large share of the whole.
        val finalSample = last
        val finalReference = reference
        if (finalSample != null && finalReference != null) {
            val residual = finalSample.altitude - finalReference
            if (residual >= finalSample.stepMeters) {
                gain += residual
                settled?.set(finalSample.index, finalSample.altitude)
            } else if (residual <= -finalSample.stepMeters) {
                loss += -residual
                settled?.set(finalSample.index, finalSample.altitude)
            }
        }

        return Change(gain = gain, loss = loss)
    }
}
