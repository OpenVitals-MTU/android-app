package tech.mmarca.openvitals.ui.components

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ChartDecimationTest {

    @Test fun `returns the same list unchanged when already at or below target`() {
        val points = List(50) { Offset(it.toFloat(), it.toFloat()) }
        assertSame(points, decimateOffsets(points, 50))
        assertSame(points, decimateOffsets(points, 100))
    }

    @Test fun `does not downsample when target is degenerate`() {
        val points = List(50) { Offset(it.toFloat(), it.toFloat()) }
        assertSame(points, decimateOffsets(points, 2))
    }

    @Test fun `reduces to exactly the target count`() {
        val points = List(5000) { Offset(it.toFloat(), (it % 7).toFloat()) }
        assertEquals(500, decimateOffsets(points, 500).size)
    }

    @Test fun `keeps the first and last point`() {
        val points = List(1000) { Offset(it.toFloat(), (it % 13).toFloat()) }
        val result = decimateOffsets(points, 100)
        assertEquals(points.first(), result.first())
        assertEquals(points.last(), result.last())
    }

    @Test fun `preserves an isolated peak because LTTB keeps extremes`() {
        // A flat line with one tall spike in the middle.
        val spikeIndex = 500
        val points = List(1000) { index ->
            Offset(index.toFloat(), if (index == spikeIndex) 1000f else 0f)
        }

        val result = decimateOffsets(points, 50)
        val maxY = result.maxOf { it.y }
        // The spike must survive downsampling.
        assertEquals(1000f, maxY, 0f)
    }
}
