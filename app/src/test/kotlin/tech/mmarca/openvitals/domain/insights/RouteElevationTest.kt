package tech.mmarca.openvitals.domain.insights

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The bug this exists to prevent: summing raw per-point rises turned a real
 * 750 m climb into ~15 km on the review screen. GPS vertical noise is larger
 * than any sane per-step threshold, so it was banked thousands of times over
 * a long ride.
 */
class RouteElevationTest {

    /**
     * A climb of [trueGain] metres spread evenly over [samples], with Gaussian
     * vertical error of [sigma] applied to every sample. Sigma 3 m is
     * representative of real GPS vertical accuracy.
     */
    private fun noisyClimb(
        trueGain: Double,
        samples: Int,
        sigma: Double = 3.0,
        seed: Int = 11,
    ): List<Double?> {
        val random = Random(seed)
        return List(samples) { i ->
            trueGain * (i.toDouble() / max(samples - 1, 1)) + gaussian(random) * sigma
        }
    }

    private fun gaussian(random: Random): Double {
        // Box-Muller: kotlin.random has no normal distribution.
        val u1 = 1.0 - random.nextDouble()
        val u2 = 1.0 - random.nextDouble()
        return sqrt(-2.0 * ln(u1)) * cos(2 * Math.PI * u2)
    }

    @Test
    fun `a flat route reports essentially no climb`() {
        val flat = noisyClimb(trueGain = 0.0, samples = 3600)
        // A naive sum over this same data yields thousands of meters.
        assertTrue(RouteElevation.elevationGainFromAltitudes(flat) < 60.0)
    }

    @Test
    fun `a real climb is reported accurately not inflated`() {
        val oneHour = noisyClimb(trueGain = 300.0, samples = 3600)
        val twoHours = noisyClimb(trueGain = 750.0, samples = 7200)

        assertEquals(300.0, RouteElevation.elevationGainFromAltitudes(oneHour), 60.0)
        assertEquals(750.0, RouteElevation.elevationGainFromAltitudes(twoHours), 100.0)
    }

    @Test
    fun `accuracy does not decay with route length`() {
        // The old accumulator's error grew with sample count, which is why a
        // longer ride was proportionally more wrong. Same climb, 4x samples.
        val short = RouteElevation.elevationGainFromAltitudes(
            noisyClimb(trueGain = 200.0, samples = 900),
        )
        val long = RouteElevation.elevationGainFromAltitudes(
            noisyClimb(trueGain = 200.0, samples = 3600),
        )
        assertTrue(abs(short - long) < 80.0)
    }

    @Test
    fun `a clean staircase is measured and descent is not counted as gain`() {
        // 30 m up, 30 m down, 30 m up; each level held so the smoothing can
        // follow the step.
        val altitudes = listOf(0.0, 30.0, 0.0, 30.0).flatMap { level ->
            List<Double?>(40) { level }
        }
        assertEquals(60.0, RouteElevation.elevationGainFromAltitudes(altitudes), 6.0)
        assertEquals(30.0, RouteElevation.elevationLossFromAltitudes(altitudes), 6.0)
    }

    @Test
    fun `a sparse imported route is not under-reported`() {
        // A GPX with one point every ~100 m: few samples, so a heavily lagging
        // filter would swallow much of the climb. Regression guard for the
        // smoothing constant.
        val sparse = List<Double?>(50) { i -> 750.0 * (i / 49.0) }
        assertTrue(RouteElevation.elevationGainFromAltitudes(sparse) > 680.0)
    }

    @Test
    fun `a two point climb is not swallowed by the smoothing lag`() {
        // With only two points the smoothing lag IS the whole route; the
        // residual settle against the last raw altitude recovers it.
        assertEquals(80.0, RouteElevation.elevationGainFromAltitudes(listOf(10.0, 90.0)), 2.0)
        assertEquals(80.0, RouteElevation.elevationLossFromAltitudes(listOf(90.0, 10.0)), 2.0)
    }

    @Test
    fun `movement below the step threshold never accumulates`() {
        // Jitter of plus-minus 2 m, five hundred times. Every sample is under
        // the 5 m step, so the reference must not move and nothing is banked.
        val jitter = List<Double?>(500) { i -> if (i % 2 == 0) 2.0 else -2.0 }
        assertEquals(0.0, RouteElevation.elevationGainFromAltitudes(jitter), 0.0)
    }

    @Test
    fun `null and non-finite altitudes are skipped not treated as zero`() {
        // Treating a null as 0 m would invent a fall to sea level and a climb
        // back out.
        val withHoles = listOf(100.0, null, 103.0, Double.NaN, 106.0, null, 109.0)
        assertTrue(RouteElevation.elevationGainFromAltitudes(withHoles) < 14.0)
        assertEquals(0.0, RouteElevation.elevationGainFromAltitudes(emptyList()), 0.0)
        assertEquals(0.0, RouteElevation.elevationGainFromAltitudes(listOf(null, null)), 0.0)
        assertEquals(0.0, RouteElevation.elevationGainFromAltitudes(listOf<Double?>(42.0)), 0.0)
    }
}
