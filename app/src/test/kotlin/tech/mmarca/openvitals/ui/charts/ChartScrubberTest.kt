package tech.mmarca.openvitals.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The scrub's landing rule, tested at the pure layer: nearest SAMPLE by x, never an
 * interpolation. The curve between two samples is a shape the app invented, and a
 * tooltip must only ever report a number that was actually measured.
 */
class ChartScrubberTest {

    private fun target(x: Float) =
        ScrubTarget(xFraction = x, yFraction = 0.5f, primary = "$x")

    @Test fun `no targets means nothing to land on`() {
        assertNull(nearestScrubTargetIndex(emptyList(), 0.5f))
    }

    @Test fun `snaps to the nearest target by x`() {
        val targets = listOf(target(0f), target(0.5f), target(1f))
        assertEquals(0, nearestScrubTargetIndex(targets, 0.1f))
        assertEquals(1, nearestScrubTargetIndex(targets, 0.4f))
        assertEquals(1, nearestScrubTargetIndex(targets, 0.6f))
        assertEquals(2, nearestScrubTargetIndex(targets, 0.9f))
    }

    @Test fun `a tie keeps the first target`() {
        // Strict less-than: exactly between two samples, the earlier one wins, so the
        // selection is stable rather than flickering with float noise.
        val targets = listOf(target(0.4f), target(0.6f))
        assertEquals(0, nearestScrubTargetIndex(targets, 0.5f))
    }

    @Test fun `a fraction outside the targets lands on the nearest end`() {
        val targets = listOf(target(0.3f), target(0.7f))
        assertEquals(0, nearestScrubTargetIndex(targets, 0f))
        assertEquals(1, nearestScrubTargetIndex(targets, 1f))
    }
}
