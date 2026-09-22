package tech.mmarca.openvitals.ui.charts

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.ui.components.MetricLinePoint
import tech.mmarca.openvitals.ui.components.PlotSummary
import tech.mmarca.openvitals.ui.components.chartBucketDescription
import tech.mmarca.openvitals.ui.components.lineChartBucketValues
import tech.mmarca.openvitals.ui.components.plotSummary

/** What a plot is reduced to when spoken: its bounds, its last value, and one value per bucket. */
class ChartSpokenSummaryTest {

    @Test
    fun `a series is summarized by its count, bounds and latest value`() {
        assertEquals(
            PlotSummary(count = 4, lowest = 2.0, highest = 9.0, latest = 5.0),
            plotSummary(listOf(3.0, 9.0, 2.0, 5.0)),
        )
    }

    @Test
    fun `an empty series has nothing to say`() {
        assertNull(plotSummary(emptyList()))
    }

    @Test
    fun `a bucket is spoken as its date and the value, or that there is none`() {
        assertEquals("14 Sept 2026, 12", chartBucketDescription("14 Sept 2026", "12", "No data"))
        assertEquals("14 Sept 2026, No data", chartBucketDescription("14 Sept 2026", null, "No data"))
    }

    @Test
    fun `a day bucket is the mean of its points and an empty day is null`() {
        val day = LocalDate.of(2026, 9, 14)
        val points = listOf(
            MetricLinePoint(day, 60.0),
            MetricLinePoint(day, 80.0),
            MetricLinePoint(day.plusDays(2), 90.0),
        )

        val values = lineChartBucketValues(points, listOf(day, day.plusDays(1), day.plusDays(2)), TimeRange.WEEK)

        assertEquals(listOf(70.0, null, 90.0), values)
    }

    @Test
    fun `a year chart buckets by month`() {
        val points = listOf(
            MetricLinePoint(LocalDate.of(2026, 3, 2), 10.0),
            MetricLinePoint(LocalDate.of(2026, 3, 30), 30.0),
            MetricLinePoint(LocalDate.of(2026, 5, 1), 50.0),
        )
        val months = listOf(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 1))

        assertEquals(listOf(20.0, null, 50.0), lineChartBucketValues(points, months, TimeRange.YEAR))
    }
}
