package tech.mmarca.openvitals.domain.insights

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CrossMetricInsightsTest {

    private val start = LocalDate.of(2026, 5, 1)

    @Test
    fun calculatesPositiveCorrelationForPairedDays() {
        val primary = valuesOf(1.0, 2.0, 3.0, 4.0)
        val secondary = valuesOf(2.0, 4.0, 6.0, 8.0)

        val insight = crossMetricInsight(primary, secondary)

        assertNotNull(insight)
        assertEquals(1.0, checkNotNull(insight).correlation, 0.0001)
        assertEquals(CrossMetricDirection.POSITIVE, insight.direction)
        assertEquals(CrossMetricStrength.STRONG, insight.strength)
        assertEquals(4, insight.pairedDays)
    }

    @Test
    fun calculatesNegativeCorrelationForPairedDays() {
        val primary = valuesOf(1.0, 2.0, 3.0, 4.0)
        val secondary = valuesOf(8.0, 6.0, 4.0, 2.0)

        val insight = crossMetricInsight(primary, secondary)

        assertNotNull(insight)
        assertEquals(-1.0, checkNotNull(insight).correlation, 0.0001)
        assertEquals(CrossMetricDirection.NEGATIVE, insight.direction)
        assertEquals(CrossMetricStrength.STRONG, insight.strength)
    }

    @Test
    fun ignoresUnpairedAndEmptyValues() {
        val primary = valuesOf(0.0, 2.0, 3.0, 4.0)
        val secondary = listOf(
            CrossMetricValue(start.plusDays(1), 4.0),
            CrossMetricValue(start.plusDays(2), 6.0),
            CrossMetricValue(start.plusDays(3), 8.0),
            CrossMetricValue(start.plusDays(10), 100.0),
        )

        val insight = crossMetricInsight(primary, secondary)

        assertNotNull(insight)
        assertEquals(3, checkNotNull(insight).pairedDays)
        assertEquals(1.0, insight.correlation, 0.0001)
    }

    @Test
    fun returnsNullWhenThereAreNotEnoughPairs() {
        val primary = valuesOf(1.0, 2.0)
        val secondary = valuesOf(2.0, 4.0)

        val insight = crossMetricInsight(primary, secondary)

        assertNull(insight)
    }

    private fun valuesOf(vararg values: Double): List<CrossMetricValue> =
        values.mapIndexed { index, value ->
            CrossMetricValue(start.plusDays(index.toLong()), value)
        }
}
