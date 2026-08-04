package tech.mmarca.openvitals.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ChartRangeTest {

    @Test fun `an empty series falls back to a unit range`() {
        val range = ChartRange.padded(emptyList())
        assertEquals(0.0, range.min, 0.0)
        assertEquals(1.0, range.max, 0.0)
    }

    @Test fun `pads by the given fraction of the span`() {
        val range = ChartRange.padded(listOf(10.0, 20.0)) // span 10, fraction 0.08
        assertEquals(9.2, range.min, 1e-9)
        assertEquals(20.8, range.max, 1e-9)
    }

    @Test fun `a flat series pads against the value's own magnitude`() {
        // A steady 70 kg must not get a hairline axis around it.
        val range = ChartRange.padded(listOf(70.0, 70.0))
        assertEquals(70.0 - 5.6, range.min, 1e-9)
        assertEquals(70.0 + 5.6, range.max, 1e-9)
    }

    @Test fun `a flat series below one pads against a unit basis`() {
        val range = ChartRange.padded(listOf(0.5, 0.5))
        assertEquals(0.42, range.min, 1e-9)
        assertEquals(0.58, range.max, 1e-9)
    }

    @Test fun `the floor stops the padding from dipping below it`() {
        // A series that never goes negative keeps its floor at zero.
        val floored = ChartRange.padded(listOf(0.1, 5.0), floor = 0.0)
        assertEquals(0.0, floored.min, 0.0)

        // But a low that clears the floor on its own is left alone.
        val clear = ChartRange.padded(listOf(1.0, 10.0), floor = 0.0)
        assertEquals(1.0 - 0.72, clear.min, 1e-9)
    }
}
