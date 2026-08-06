package tech.mmarca.openvitals.domain.model

import kotlin.math.abs
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CoMapsRouteSimplifyTest {

    // Roughly one metre of latitude, and of longitude at this latitude.
    private val latMeter = 1.0 / 111_320.0
    private val lonMeterAt59 = latMeter / 0.5150380749100542

    @Test fun `two points or fewer pass through untouched`() {
        val two = doubleArrayOf(59.0, 24.0, 59.1, 24.1)
        assertSame(two, simplifyCoMapsRoutePoints(two))
        val empty = DoubleArray(0)
        assertSame(empty, simplifyCoMapsRoutePoints(empty))
    }

    @Test fun `a straight stretch collapses to its endpoints`() {
        // 101 points down one line of longitude — a motorway due south.
        val points = DoubleArray(101 * 2) { index ->
            if (index % 2 == 0) 59.0 + (index / 2) * latMeter * 100 else 24.0
        }

        val simplified = simplifyCoMapsRoutePoints(points)

        assertArrayEquals(
            doubleArrayOf(points[0], points[1], points[200], points[201]),
            simplified,
            1e-12,
        )
    }

    @Test fun `a corner the eye can see survives`() {
        // Out 100 m east, then 100 m north: the corner is ~70 m off the chord.
        val points = doubleArrayOf(
            59.0, 24.0,
            59.0, 24.0 + lonMeterAt59 * 100,
            59.0 + latMeter * 100, 24.0 + lonMeterAt59 * 100,
        )

        assertArrayEquals(points, simplifyCoMapsRoutePoints(points), 1e-12)
    }

    @Test fun `a sub-tolerance wiggle is dropped, a larger one is kept`() {
        fun wiggle(meters: Double): DoubleArray = doubleArrayOf(
            59.0, 24.0,
            59.0 + latMeter * meters, 24.0 + lonMeterAt59 * 100,
            59.0, 24.0 + lonMeterAt59 * 200,
        )

        assertEquals(2, simplifyCoMapsRoutePoints(wiggle(1.0)).size / 2)
        assertEquals(3, simplifyCoMapsRoutePoints(wiggle(10.0)).size / 2)
    }

    @Test fun `the result is a subsequence and every dropped point stays near the line`() {
        // A jagged 2 km ride: alternating small offsets, some above tolerance.
        val points = DoubleArray(400) { index ->
            val point = index / 2
            if (index % 2 == 0) {
                59.0 + latMeter * ((point % 5) * 2.0)
            } else {
                24.0 + lonMeterAt59 * (point * 10.0)
            }
        }

        val simplified = simplifyCoMapsRoutePoints(points)

        // Endpoints survive.
        assertEquals(points[0], simplified[0], 0.0)
        assertEquals(points[1], simplified[1], 0.0)
        assertEquals(points[points.size - 2], simplified[simplified.size - 2], 0.0)
        assertEquals(points[points.size - 1], simplified[simplified.size - 1], 0.0)
        // Subsequence: every kept pair exists in the original, in order.
        var cursor = 0
        for (kept in 0 until simplified.size / 2) {
            var found = false
            while (cursor < points.size / 2) {
                if (points[cursor * 2] == simplified[kept * 2] &&
                    points[cursor * 2 + 1] == simplified[kept * 2 + 1]
                ) {
                    found = true
                    break
                }
                cursor++
            }
            assertTrue("kept point $kept not found in order", found)
        }
        // Every original point sits within tolerance of the simplified line
        // (checked against the nearest kept segment, generously in degrees).
        val toleranceDegrees = CoMapsRouteToleranceMeters * latMeter * 1.5
        for (point in 0 until points.size / 2) {
            val lat = points[point * 2]
            val lon = points[point * 2 + 1]
            var nearest = Double.MAX_VALUE
            for (kept in 0 until simplified.size / 2 - 1) {
                nearest = minOf(
                    nearest,
                    pointToSegmentDegrees(
                        lat, lon,
                        simplified[kept * 2], simplified[kept * 2 + 1],
                        simplified[kept * 2 + 2], simplified[kept * 2 + 3],
                    ),
                )
            }
            assertTrue(
                "point $point strays ${nearest / latMeter} m from the line",
                nearest <= toleranceDegrees,
            )
        }
    }

    @Test fun `antimeridian deltas measure short, not around the globe`() {
        // Three collinear points crossing 180°: still collapses to two.
        val points = doubleArrayOf(0.0, 179.9999, 0.0, 180.0, 0.0, -179.9999)
        assertEquals(2, simplifyCoMapsRoutePoints(points).size / 2)
    }

    private fun pointToSegmentDegrees(
        lat: Double, lon: Double,
        latA: Double, lonA: Double,
        latB: Double, lonB: Double,
    ): Double {
        val bx = lonB - lonA
        val by = latB - latA
        val px = lon - lonA
        val py = lat - latA
        val lengthSquared = bx * bx + by * by
        if (lengthSquared == 0.0) return maxOf(abs(px), abs(py))
        val t = ((px * bx + py * by) / lengthSquared).coerceIn(0.0, 1.0)
        val dx = px - t * bx
        val dy = py - t * by
        return kotlin.math.hypot(dx, dy)
    }
}
