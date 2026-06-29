package tech.mmarca.openvitals.ui.components

import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodChartTest {

    @Test fun `month buckets keep one slot per day and fill missing dates with zero`() {
        val period = DatePeriod(
            start = LocalDate.of(2026, 4, 1),
            end = LocalDate.of(2026, 4, 5),
        )

        val buckets = periodBarBuckets(
            values = listOf(
                PeriodChartValue(LocalDate.of(2026, 4, 1), 100.0),
                PeriodChartValue(LocalDate.of(2026, 4, 3), 50.0),
            ),
            selectedRange = TimeRange.MONTH,
            period = period,
        )

        assertEquals(5, buckets.size)
        assertEquals(LocalDate.of(2026, 4, 2), buckets[1].date)
        assertEquals(0.0, buckets[1].value, 0.0)
        assertEquals(50.0, buckets[2].value, 0.0)
    }

    @Test fun `year buckets aggregate daily values by month`() {
        val period = DatePeriod(
            start = LocalDate.of(2026, 1, 1),
            end = LocalDate.of(2026, 3, 31),
        )

        val buckets = periodBarBuckets(
            values = listOf(
                PeriodChartValue(LocalDate.of(2026, 1, 1), 100.0),
                PeriodChartValue(LocalDate.of(2026, 1, 2), 50.0),
                PeriodChartValue(LocalDate.of(2026, 3, 10), 25.0),
            ),
            selectedRange = TimeRange.YEAR,
            period = period,
        )

        assertEquals(3, buckets.size)
        assertEquals(LocalDate.of(2026, 1, 1), buckets[0].date)
        assertEquals(150.0, buckets[0].value, 0.0)
        assertEquals(0.0, buckets[1].value, 0.0)
        assertEquals(25.0, buckets[2].value, 0.0)
    }

    @Test fun `year buckets can average non-zero daily values`() {
        val period = DatePeriod(
            start = LocalDate.of(2026, 1, 1),
            end = LocalDate.of(2026, 1, 31),
        )

        val buckets = periodBarBuckets(
            values = listOf(
                PeriodChartValue(LocalDate.of(2026, 1, 1), 8.0),
                PeriodChartValue(LocalDate.of(2026, 1, 2), 6.0),
            ),
            selectedRange = TimeRange.YEAR,
            period = period,
            yearAggregation = PeriodBarAggregation.AVERAGE_NON_ZERO,
        )

        assertEquals(1, buckets.size)
        assertEquals(7.0, buckets.single().value, 0.0)
    }

    @Test fun `month labels only show stable tick positions`() {
        assertTrue(isPeriodChartLabelVisible(index = 0, lastIndex = 30, selectedRange = TimeRange.MONTH))
        assertFalse(isPeriodChartLabelVisible(index = 1, lastIndex = 30, selectedRange = TimeRange.MONTH))
        assertTrue(isPeriodChartLabelVisible(index = 5, lastIndex = 30, selectedRange = TimeRange.MONTH))
        assertTrue(isPeriodChartLabelVisible(index = 30, lastIndex = 30, selectedRange = TimeRange.MONTH))
    }

    @Test fun `year labels show every monthly bucket`() {
        for (index in 0..11) {
            assertTrue(isPeriodChartLabelVisible(index = index, lastIndex = 11, selectedRange = TimeRange.YEAR))
        }
    }

    @Test fun `year labels thin dense daily series`() {
        assertTrue(isPeriodChartLabelVisible(index = 0, lastIndex = 364, selectedRange = TimeRange.YEAR))
        assertFalse(isPeriodChartLabelVisible(index = 1, lastIndex = 364, selectedRange = TimeRange.YEAR))
        assertTrue(isPeriodChartLabelVisible(index = 30, lastIndex = 364, selectedRange = TimeRange.YEAR))
        assertTrue(isPeriodChartLabelVisible(index = 364, lastIndex = 364, selectedRange = TimeRange.YEAR))
    }

    @Test fun `y axis labels are ordered from high to low`() {
        assertEquals(listOf("10", "5", "0"), chartYAxisLabels(minValue = 0.0, maxValue = 10.0))
    }

    @Test fun `compact y axis values abbreviate large numbers`() {
        assertEquals("1.5k", formatCompactAxisValue(1_500.0))
        assertEquals("2M", formatCompactAxisValue(2_000_000.0))
    }

    @Test fun `y axis labels keep fallback midpoint visible when formatter rounds it`() {
        val labels = chartYAxisLabels(
            minValue = 0.0,
            maxValue = 1.0,
            valueFormatter = { it.toLong().toString() },
        )

        assertEquals(listOf("1", "0.5", "0"), labels)
    }
}
