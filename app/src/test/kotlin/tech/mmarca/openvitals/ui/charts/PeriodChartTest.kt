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
        val labelled = periodChartLabelIndices(daysFrom(LocalDate.of(2026, 5, 1), 31), TimeRange.MONTH)

        assertTrue(0 in labelled)
        assertFalse(1 in labelled)
        assertTrue(5 in labelled)
        assertTrue(30 in labelled)
    }

    @Test fun `a short month drops the last label rather than colliding it`() {
        // February's 28th sits two slots after its 26th. With three slots per label, the last one goes.
        val february = daysFrom(LocalDate.of(2026, 2, 1), 28)

        val roomy = periodChartLabelIndices(february, TimeRange.MONTH, minimumGap = 1)
        val cramped = periodChartLabelIndices(february, TimeRange.MONTH, minimumGap = 3)

        assertTrue(27 in roomy)
        assertTrue(25 in cramped)
        assertFalse(27 in cramped)
    }

    @Test fun `a label wider than the gap thins the whole row`() {
        // Twelve month names will not fit: every other one.
        val months = (0..11).map { LocalDate.of(2026, 1, 1).plusMonths(it.toLong()) }

        val thinned = periodChartLabelIndices(months, TimeRange.YEAR, minimumGap = 2)

        assertEquals(setOf(0, 2, 4, 6, 8, 10), thinned)
    }

    @Test fun `year labels show every monthly bucket`() {
        val months = (0..11).map { LocalDate.of(2026, 1, 1).plusMonths(it.toLong()) }

        assertEquals((0..11).toSet(), periodChartLabelIndices(months, TimeRange.YEAR))
    }

    @Test fun `a year of days is labelled by month, not every thirtieth day`() {
        // Every thirtieth day drifts off the calendar: it printed "May" twice.
        val days = daysFrom(LocalDate.of(2026, 1, 1), 365)
        val labelled = periodChartLabelIndices(days, TimeRange.YEAR)

        assertEquals(12, labelled.size)
        assertTrue(labelled.all { days[it].dayOfMonth == 1 })
        assertFalse(364 in labelled)
    }

    private fun daysFrom(start: LocalDate, count: Int): List<LocalDate> =
        (0 until count).map { start.plusDays(it.toLong()) }

    @Test fun `y axis labels are ordered from high to low`() {
        assertEquals(listOf("10", "5", "0"), chartYAxisLabels(minValue = 0.0, maxValue = 10.0))
    }

    @Test fun `compact y axis values abbreviate large numbers`() {
        // Small numbers pass straight through; "0.0k" abbreviates nothing.
        assertEquals("0", formatCompactAxisValue(0.0))
        assertEquals("12", formatCompactAxisValue(12.0))
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

    // Zoomed bar geometry.

    @Test fun `unzoomed bar geometry splits the plot evenly between buckets`() {
        val geometry = periodBarGeometry(
            plotWidth = 700f,
            bucketCount = 7,
            viewportSpan = 1f,
            pxPerDp = 1f,
        )

        assertEquals(7f, geometry.visibleSlots, 1e-4f)
        assertEquals(100f, geometry.slotWidth, 1e-4f)
        assertEquals(8f, geometry.gap, 1e-4f)
        assertEquals(92f, geometry.barWidth, 1e-4f)
    }

    @Test fun `zooming widens the slots by the inverse of the span`() {
        val geometry = periodBarGeometry(
            plotWidth = 700f,
            bucketCount = 28,
            viewportSpan = 0.25f,
            pxPerDp = 1f,
        )

        // 28 buckets at quarter zoom is 7 slots on screen, so the gap follows how crowded the plot looks.
        assertEquals(7f, geometry.visibleSlots, 1e-4f)
        assertEquals(100f, geometry.slotWidth, 1e-4f)
        assertEquals(8f, geometry.gap, 1e-4f)
    }

    @Test fun `the gap never eats more than three fifths of a slot`() {
        val geometry = periodBarGeometry(
            plotWidth = 70f,
            bucketCount = 7,
            viewportSpan = 1f,
            pxPerDp = 1f,
        )

        assertEquals(10f, geometry.slotWidth, 1e-4f)
        assertEquals(6f, geometry.gap, 1e-4f)
        assertEquals(4f, geometry.barWidth, 1e-4f)
    }

    @Test fun `a bar is never thinner than a dp however dense the period`() {
        val geometry = periodBarGeometry(
            plotWidth = 10f,
            bucketCount = 365,
            viewportSpan = 1f,
            pxPerDp = 3f,
        )

        assertEquals(3f, geometry.barWidth, 1e-4f)
    }

    @Test fun `bucketless charts do not divide by zero`() {
        val geometry = periodBarGeometry(
            plotWidth = 700f,
            bucketCount = 0,
            viewportSpan = 1f,
            pxPerDp = 1f,
        )

        assertEquals(1f, geometry.visibleSlots, 1e-4f)
        assertEquals(700f, geometry.slotWidth, 1e-4f)
    }

    @Test fun `slot positions follow the viewport`() {
        val viewport = ChartViewport(start = 0.5f, end = 1f)

        // The fourth of seven days is 3/7 of the data; with the back half on show, a seventh of the plot.
        assertEquals(0f, periodSlotLeftFraction(0, 7, ChartViewport.Full), 1e-4f)
        assertEquals(3f / 7f, periodSlotLeftFraction(3, 7, ChartViewport.Full), 1e-4f)
        assertEquals(1f / 7f, periodSlotLeftFraction(4, 7, viewport), 1e-4f)
        // Scrolled off the left edge: negative, deliberately not clamped.
        assertTrue(periodSlotLeftFraction(0, 7, viewport) < 0f)
    }

    @Test fun `slots wholly off either edge are skipped`() {
        assertTrue(isPeriodSlotVisible(slotLeft = 0f, slotWidth = 100f, plotWidth = 700f))
        assertTrue(isPeriodSlotVisible(slotLeft = -50f, slotWidth = 100f, plotWidth = 700f))
        assertTrue(isPeriodSlotVisible(slotLeft = 650f, slotWidth = 100f, plotWidth = 700f))
        assertFalse(isPeriodSlotVisible(slotLeft = -100.5f, slotWidth = 100f, plotWidth = 700f))
        assertFalse(isPeriodSlotVisible(slotLeft = 700.5f, slotWidth = 100f, plotWidth = 700f))
    }

    @Test fun `a tap selects the bucket it lands on, zoomed or not`() {
        assertEquals(0, periodBarIndexAt(xFraction = 0f, bucketCount = 7, viewport = ChartViewport.Full))
        assertEquals(3, periodBarIndexAt(xFraction = 0.5f, bucketCount = 7, viewport = ChartViewport.Full))
        assertEquals(6, periodBarIndexAt(xFraction = 1f, bucketCount = 7, viewport = ChartViewport.Full))

        // With the back half on show, the plot's left edge is the fourth day; tapping the first visible bar selects it.
        val zoomed = ChartViewport(start = 0.5f, end = 1f)
        assertEquals(3, periodBarIndexAt(xFraction = 0f, bucketCount = 7, viewport = zoomed))
        assertEquals(6, periodBarIndexAt(xFraction = 1f, bucketCount = 7, viewport = zoomed))
    }

    @Test fun `tapping an empty chart cannot index off the end`() {
        assertEquals(0, periodBarIndexAt(xFraction = 0.5f, bucketCount = 0, viewport = ChartViewport.Full))
    }

    // Zoomed axis label positioning.

    @Test fun `zoomed axis labels sit over their own bars`() {
        val viewport = ChartViewport(start = 0.5f, end = 1f)
        val rowWidth = 700f
        val slotWidth = rowWidth / (7 * viewport.span)

        assertEquals(200f, slotWidth, 1e-4f)
        // Day 4 straddles the left edge, so it starts just outside the plot and is still drawn.
        assertEquals(-100f, periodSlotLeftFraction(3, 7, viewport) * rowWidth, 1e-3f)
        assertTrue(isPeriodSlotVisible(-100f, slotWidth, rowWidth))
        // Day 5 is exactly one slot along: the label row's slots step with the bars'.
        assertEquals(100f, periodSlotLeftFraction(4, 7, viewport) * rowWidth, 1e-3f)
        // Days 1..3 are wholly off the left edge and are culled rather than drawn.
        for (index in 0..2) {
            val left = periodSlotLeftFraction(index, 7, viewport) * rowWidth
            assertFalse(isPeriodSlotVisible(left, slotWidth, rowWidth))
        }
    }

    @Test fun `an unzoomed axis keeps every slot`() {
        val rowWidth = 700f
        val slotWidth = rowWidth / 7f
        for (index in 0..6) {
            val left = periodSlotLeftFraction(index, 7, ChartViewport.Full) * rowWidth
            assertTrue(isPeriodSlotVisible(left, slotWidth, rowWidth))
        }
    }

    @Test fun `the gap thresholds step down as the plot gets crowded`() {
        assertEquals(8f, periodBarGapDp(7), 0f)
        assertEquals(6f, periodBarGapDp(8), 0f)
        assertEquals(6f, periodBarGapDp(12), 0f)
        assertEquals(3f, periodBarGapDp(13), 0f)
        assertEquals(3f, periodBarGapDp(31), 0f)
        assertEquals(1f, periodBarGapDp(32), 0f)
    }
}
