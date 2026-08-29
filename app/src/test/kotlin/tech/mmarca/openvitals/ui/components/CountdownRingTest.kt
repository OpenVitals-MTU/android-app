package tech.mmarca.openvitals.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class CountdownRingTest {

    @Test
    fun `holds full while not running`() {
        assertEquals(1f, countdownRemainingFraction(endMillis = null, totalMillis = 30_000L, clockMillis = 5_000L))
    }

    @Test
    fun `tracks the remaining share of the window`() {
        assertEquals(0.5f, countdownRemainingFraction(endMillis = 30_000L, totalMillis = 30_000L, clockMillis = 15_000L))
        assertEquals(0.1f, countdownRemainingFraction(endMillis = 30_000L, totalMillis = 30_000L, clockMillis = 27_000L), 1e-6f)
    }

    @Test
    fun `clamps past the end and before the start`() {
        assertEquals(0f, countdownRemainingFraction(endMillis = 30_000L, totalMillis = 30_000L, clockMillis = 31_000L))
        assertEquals(1f, countdownRemainingFraction(endMillis = 30_000L, totalMillis = 30_000L, clockMillis = -5_000L))
    }

    @Test
    fun `an empty window is already over`() {
        assertEquals(0f, countdownRemainingFraction(endMillis = 30_000L, totalMillis = 0L, clockMillis = 0L))
    }
}
