package tech.mmarca.openvitals.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The curve is decoration. The data still has to be true.
 *
 * A plain spline (Catmull-Rom, natural cubic) overshoots: to stay smooth through a
 * corner it swings past the points either side. On a cumulative chart that reads as the
 * running total DIPPING between two drinks — a line saying you un-drank water. These
 * tests pin that it cannot happen.
 *
 * The Android-backed [smoothPath] cannot be walked in a JVM test, so these sample the
 * pure [monotoneCubicSegments] layer with the cubic Bézier formula — the same geometry
 * the path builder emits.
 */
class ChartCurveTest {

    /** The point of a cubic Bézier segment at parameter [t]. */
    private fun cubicAt(p0: Offset, segment: CurveSegment.Cubic, t: Float): Offset {
        val u = 1f - t
        val x = u * u * u * p0.x +
            3f * u * u * t * segment.control1.x +
            3f * u * t * t * segment.control2.x +
            t * t * t * segment.end.x
        val y = u * u * u * p0.y +
            3f * u * u * t * segment.control1.y +
            3f * u * t * t * segment.control2.y +
            t * t * t * segment.end.y
        return Offset(x, y)
    }

    /** Samples the whole curve densely, segment by segment. */
    private fun sampled(points: List<Offset>, stepsPerSegment: Int = 100): List<Offset> {
        if (points.isEmpty()) return emptyList()
        var start = points.first()
        val out = mutableListOf(start)
        for (segment in monotoneCubicSegments(points)) {
            when (segment) {
                is CurveSegment.Line -> out.add(segment.end)
                is CurveSegment.Cubic -> for (i in 1..stepsPerSegment) {
                    out.add(cubicAt(start, segment, i / stepsPerSegment.toFloat()))
                }
            }
            start = when (segment) {
                is CurveSegment.Line -> segment.end
                is CurveSegment.Cubic -> segment.end
            }
        }
        return out
    }

    @Test fun `a rising series never dips on the way up`() {
        // Screen coords: y grows DOWNWARD, so a rising total is a falling y. The three
        // clustered points then the long flat run are exactly the shape that makes a
        // naive spline overshoot.
        val samples = sampled(
            listOf(
                Offset(0f, 200f),
                Offset(20f, 190f),
                Offset(40f, 100f),
                Offset(60f, 40f),
                Offset(300f, 40f),
            ),
        )

        for (i in 1 until samples.size) {
            // The curve must never go back DOWN — that would be the running total dipping.
            assertTrue(
                "y rose from ${samples[i - 1].y} to ${samples[i].y} at sample $i",
                samples[i].y <= samples[i - 1].y + 0.01f,
            )
        }
    }

    @Test fun `a flat run stays flat`() {
        // "You drank nothing between nine and six" must read as a flat line, not as a
        // gentle bulge that implies you were sipping the whole time.
        val samples = sampled(
            listOf(
                Offset(0f, 200f),
                Offset(50f, 100f),
                Offset(150f, 100f),
                Offset(200f, 20f),
            ),
        )

        samples
            .filter { it.x > 60f && it.x < 140f }
            .forEach { assertEquals("the flat stretch bulged at x=${it.x}", 100f, it.y, 0.5f) }
    }

    @Test fun `never overshoots beyond the extreme samples`() {
        // A heart rate curve must not swing under the slowest beat actually recorded.
        val samples = sampled(
            listOf(
                Offset(0f, 100f),
                Offset(50f, 100f),
                Offset(100f, 20f),
                Offset(150f, 100f),
            ),
        )

        assertTrue(samples.minOf { it.y } >= 20f - 0.01f)
        assertTrue(samples.maxOf { it.y } <= 100f + 0.01f)
    }

    @Test fun `draws a vertical riser straight rather than looping through it`() {
        // Two readings at the same instant. No function curves through that, and a
        // spline that tries will loop the path back on itself.
        val points = listOf(
            Offset(0f, 100f),
            Offset(50f, 100f),
            Offset(50f, 40f),
            Offset(100f, 40f),
        )

        val segments = monotoneCubicSegments(points)
        assertTrue(segments[1] is CurveSegment.Line)

        val samples = sampled(points)
        for (i in 1 until samples.size) {
            assertTrue(
                "the path doubled back in x at sample $i",
                samples[i].x >= samples[i - 1].x - 0.01f,
            )
        }
    }

    @Test fun `degenerate inputs yield no segments and do not throw`() {
        assertTrue(monotoneCubicSegments(emptyList()).isEmpty())
        assertTrue(monotoneCubicSegments(listOf(Offset(5f, 5f))).isEmpty())
        assertEquals(1, monotoneCubicSegments(listOf(Offset(0f, 0f), Offset(10f, 10f))).size)
    }

    // ── movingAverageY ──────────────────────────────────────────────────────

    @Test fun `fewer than three points pass through untouched`() {
        val points = listOf(Offset(0f, 10f), Offset(1f, 20f))
        assertSame(points, movingAverageY(points))
    }

    @Test fun `averages a centred window and clamps it at the edges`() {
        // 5 points -> radius (5/16=0) coerced up to 1.
        val points = listOf(
            Offset(0f, 0f),
            Offset(1f, 10f),
            Offset(2f, 20f),
            Offset(3f, 30f),
            Offset(4f, 40f),
        )

        val result = movingAverageY(points)

        assertEquals(5f, result[0].y, 1e-4f) // (0+10)/2 — window clamped at the start
        assertEquals(10f, result[1].y, 1e-4f) // (0+10+20)/3
        assertEquals(20f, result[2].y, 1e-4f)
        assertEquals(35f, result[4].y, 1e-4f) // (30+40)/2 — clamped at the end
        // x is never touched: this smooths the SAMPLES, not the axis.
        result.forEachIndexed { index, offset -> assertEquals(points[index].x, offset.x, 0f) }
    }

    @Test fun `the window widens with the point count up to radius four`() {
        // 80 points -> radius (80/16=5) coerced down to 4: a nine-sample window.
        val points = List(80) { Offset(it.toFloat(), if (it == 40) 90f else 0f) }

        val result = movingAverageY(points)

        assertEquals(10f, result[40].y, 1e-4f) // 90 spread over 9 samples
        assertEquals(10f, result[36].y, 1e-4f) // spike still inside the window at 40-4
        assertEquals(0f, result[35].y, 1e-4f) // and outside it one step further
    }
}
