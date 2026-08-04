package tech.mmarca.openvitals.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChartViewportTest {

    @Test fun `starts as the whole chart`() {
        assertEquals(1f, ChartViewport.Full.span, 0f)
        assertFalse(ChartViewport.Full.isZoomed)
        assertEquals(0.25f, ChartViewport.Full.visibleFraction(0.25f), 0f)
    }

    @Test fun `zooming keeps the point between the fingers under the fingers`() {
        // Pinch out by 2x around a point a quarter of the way across the plot. Whatever
        // datum was under that point must still be under it — that is what makes the
        // chart feel like it is being stretched rather than replaced.
        val before = ChartViewport.Full
        val anchorData = before.dataFraction(0.25f)

        val after = before.zoomed(2f, 0.25f)

        assertEquals(0.5f, after.span, 1e-6f)
        assertEquals(0.25f, after.visibleFraction(anchorData), 1e-6f)
    }

    @Test fun `a point outside the window is NOT clamped into it`() {
        val view = ChartViewport.Full.zoomed(2f, 0.5f)

        // A line leaving the left edge has to carry on to where it really is. Clamping
        // it would bend it up into the corner and draw a value nobody recorded.
        assertTrue(view.visibleFraction(0f) < 0f)
        assertTrue(view.visibleFraction(1f) > 1f)
    }

    @Test fun `panning moves the data under the finger by the distance dragged`() {
        val view = ChartViewport.Full.zoomed(4f, 0.5f) // span 0.25
        val datum = view.dataFraction(0.5f)

        // Drag a tenth of the plot to the left and the datum comes WITH the finger: it
        // was halfway across the plot, and it is now a tenth further left.
        val panned = view.panned(-0.1f)

        assertEquals(0.4f, panned.visibleFraction(datum), 1e-6f)
    }

    @Test fun `the window stops at the ends rather than sliding off`() {
        val view = ChartViewport.Full.zoomed(4f, 0f)
        assertEquals(0f, view.start, 0f)

        // Dragging further left has nothing to show, so it does nothing.
        assertEquals(0f, view.panned(1f).start, 0f)
        assertEquals(view.span, view.panned(1f).end, 1e-6f)

        val right = ChartViewport.Full.zoomed(4f, 1f)
        assertEquals(1f, right.end, 1e-6f)
        assertEquals(1f, right.panned(-1f).end, 1e-6f)
    }

    @Test fun `there is a floor on how far you can zoom in`() {
        val view = ChartViewport.Full.zoomed(1000f, 0.5f)
        assertEquals(ChartViewport.MinimumSpan, view.span, 1e-6f)
    }

    @Test fun `zooming back out never overshoots the whole chart`() {
        val view = ChartViewport.Full.zoomed(4f, 0.5f).zoomed(0.01f, 0.5f)
        assertEquals(1f, view.span, 0f)
        assertEquals(0f, view.start, 0f)
        assertFalse(view.isZoomed)
    }
}
