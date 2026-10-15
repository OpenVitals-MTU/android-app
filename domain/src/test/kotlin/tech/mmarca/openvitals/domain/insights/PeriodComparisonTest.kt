package tech.mmarca.openvitals.domain.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeriodComparisonTest {

    @Test fun `comparison reports upward percent change`() {
        val comparison = periodComparison(currentValue = 120.0, previousValue = 100.0)

        assertEquals(20.0, comparison.change, 0.01)
        assertEquals(20.0, comparison.percentChange ?: 0.0, 0.01)
        assertEquals(PeriodComparisonDirection.UP, comparison.direction)
    }

    @Test fun `comparison reports downward percent change`() {
        val comparison = periodComparison(currentValue = 75.0, previousValue = 100.0)

        assertEquals(-25.0, comparison.change, 0.01)
        assertEquals(-25.0, comparison.percentChange ?: 0.0, 0.01)
        assertEquals(PeriodComparisonDirection.DOWN, comparison.direction)
    }

    @Test fun `comparison omits percent when previous value is zero`() {
        val comparison = periodComparison(currentValue = 10.0, previousValue = 0.0)

        assertEquals(10.0, comparison.absoluteChange, 0.01)
        assertNull(comparison.percentChange)
        assertEquals(PeriodComparisonDirection.UP, comparison.direction)
    }
}
