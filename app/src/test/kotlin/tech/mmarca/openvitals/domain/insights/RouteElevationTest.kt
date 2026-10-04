package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/** Summing raw per-point rises turned a 750 m climb into ~15 km: GPS vertical noise was banked thousands of times. */
class RouteElevationTest {

    /** A climb of [trueGain] metres over [samples], with Gaussian vertical error of [sigma]. Sigma 3 m is realistic. */
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
        // The old accumulator's error grew with sample count. Same climb, 4x samples.
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
        // 30 m up, down, up; each level held so the smoothing can follow.
        val altitudes = listOf(0.0, 30.0, 0.0, 30.0).flatMap { level ->
            List<Double?>(40) { level }
        }
        assertEquals(60.0, RouteElevation.elevationGainFromAltitudes(altitudes), 6.0)
        assertEquals(30.0, RouteElevation.elevationLossFromAltitudes(altitudes), 6.0)
    }

    @Test
    fun `a sparse imported route is not under-reported`() {
        // One point every ~100 m: a heavily lagging filter would swallow much of the climb.
        val sparse = List<Double?>(50) { i -> 750.0 * (i / 49.0) }
        assertTrue(RouteElevation.elevationGainFromAltitudes(sparse) > 680.0)
    }

    @Test
    fun `a two point climb is not swallowed by the smoothing lag`() {
        // With two points the lag is the whole route; the residual settle recovers it.
        assertEquals(80.0, RouteElevation.elevationGainFromAltitudes(listOf(10.0, 90.0)), 2.0)
        assertEquals(80.0, RouteElevation.elevationLossFromAltitudes(listOf(90.0, 10.0)), 2.0)
    }

    @Test
    fun `movement below the step threshold never accumulates`() {
        // Jitter of plus-minus 2 m, under the 5 m step, so nothing is banked.
        val jitter = List<Double?>(500) { i -> if (i % 2 == 0) 2.0 else -2.0 }
        assertEquals(0.0, RouteElevation.elevationGainFromAltitudes(jitter), 0.0)
    }

    @Test
    fun `null and non-finite altitudes are skipped not treated as zero`() {
        // A null as 0 m would invent a fall to sea level.
        val withHoles = listOf(100.0, null, 103.0, Double.NaN, 106.0, null, 109.0)
        assertTrue(RouteElevation.elevationGainFromAltitudes(withHoles) < 14.0)
        assertEquals(0.0, RouteElevation.elevationGainFromAltitudes(emptyList()), 0.0)
        assertEquals(0.0, RouteElevation.elevationGainFromAltitudes(listOf(null, null)), 0.0)
        assertEquals(0.0, RouteElevation.elevationGainFromAltitudes(listOf<Double?>(42.0)), 0.0)
    }

    /** One fix a second, carrying the accuracies the phone reported for it. */
    private fun fixes(
        altitudes: List<Double?>,
        verticalAccuracy: Double?,
        horizontalAccuracy: Double?,
        startSecond: Long = 0L,
    ): List<ExerciseRoutePoint> = altitudes.mapIndexed { i, altitude ->
        ExerciseRoutePoint(
            time = Instant.parse("2026-10-04T10:00:00Z").plusSeconds(startSecond + i),
            latitude = 48.0,
            longitude = 11.0,
            altitudeMeters = altitude,
            horizontalAccuracyMeters = horizontalAccuracy,
            verticalAccuracyMeters = verticalAccuracy,
        )
    }

    @Test
    fun `a flat walk through a building does not climb Everest`() {
        // 45 min under open sky, then 15 min indoors where the altitude swings by tens of meters.
        // The fixed 5 m step banked several km here; the fixes said how little to trust them.
        val outside = fixes(noisyClimb(0.0, 2700, sigma = 3.0, seed = 3), verticalAccuracy = 4.0, horizontalAccuracy = 4.0)
        val indoors = fixes(
            noisyClimb(0.0, 900, sigma = 40.0, seed = 4),
            verticalAccuracy = 40.0,
            horizontalAccuracy = 25.0,
            startSecond = 2700,
        )
        val walk = outside + indoors

        assertTrue(RouteElevation.elevationGainFromAltitudes(walk.map { it.altitudeMeters }) > 1_000.0)
        // The building adds nothing to what the open-sky part reports on its own.
        assertEquals(RouteElevation.routeElevationGain(outside), RouteElevation.routeElevationGain(walk), 1.0)
    }

    @Test
    fun `an under-reported vertical accuracy is floored at the horizontal one`() {
        // Phones claim 15 m vertically indoors while the horizontal fix is already 28 m off.
        val indoors = fixes(noisyClimb(0.0, 900, sigma = 40.0, seed = 5), verticalAccuracy = 15.0, horizontalAccuracy = 28.0)
        assertTrue(RouteElevation.routeElevationGain(indoors) < 30.0)
    }

    @Test
    fun `a step grows with the fix's own uncertainty`() {
        // Under trees: 10 m of vertical noise, honestly reported, would bank km at a 5 m step.
        val underTrees = fixes(noisyClimb(0.0, 3600, sigma = 10.0, seed = 6), verticalAccuracy = 6.0, horizontalAccuracy = 15.0)
        assertTrue(RouteElevation.elevationGainFromAltitudes(underTrees.map { it.altitudeMeters }) > 1_000.0)
        assertTrue(RouteElevation.routeElevationGain(underTrees) < 300.0)
    }

    @Test
    fun `a real climb with reported accuracy is still measured`() {
        val climb = fixes(noisyClimb(300.0, 3600, sigma = 5.0), verticalAccuracy = 10.0, horizontalAccuracy = 8.0)
        assertEquals(300.0, RouteElevation.routeElevationGain(climb), 40.0)
        val sparse = fixes(List(50) { i -> 750.0 * (i / 49.0) }, verticalAccuracy = 8.0, horizontalAccuracy = 5.0)
        assertTrue(RouteElevation.routeElevationGain(sparse) > 680.0)
    }

    @Test
    fun `a route without vertical accuracy is filtered as before`() {
        // GPX imports and DEM-corrected altitudes carry none; their altitudes are taken as they are.
        val altitudes = noisyClimb(300.0, 3600)
        val route = fixes(altitudes, verticalAccuracy = null, horizontalAccuracy = 25.0)
        assertEquals(
            RouteElevation.elevationGainFromAltitudes(altitudes),
            RouteElevation.routeElevationGain(route),
            0.0,
        )
    }

    @Test
    fun `settled altitudes rise and fall by exactly the route's gain and loss`() {
        // Splits are cut from this profile, so they must add up to the saved figure.
        val route = fixes(
            listOf(null, null) + noisyClimb(300.0, 1800, sigma = 3.0) + noisyClimb(-120.0, 1800, seed = 12).map { it!! + 300.0 },
            verticalAccuracy = 4.0,
            horizontalAccuracy = 4.0,
        )

        val settled = RouteElevation.withSettledAltitudes(route)
        val steps = settled.mapNotNull { it.altitudeMeters }.zipWithNext { a, b -> b - a }

        assertEquals(route.size, settled.size)
        assertEquals(null, settled[0].altitudeMeters)
        assertEquals(RouteElevation.routeElevationGain(route), steps.filter { it > 0 }.sum(), 1e-6)
        assertEquals(RouteElevation.routeElevationLoss(route), -steps.filter { it < 0 }.sum(), 1e-6)
    }
}
