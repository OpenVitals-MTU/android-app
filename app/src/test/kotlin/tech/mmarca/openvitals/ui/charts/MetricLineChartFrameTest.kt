package tech.mmarca.openvitals.ui.charts

import androidx.compose.ui.graphics.Color
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.ui.components.LineAxisRange
import tech.mmarca.openvitals.ui.components.MetricLinePoint
import tech.mmarca.openvitals.ui.components.MetricLineSeries
import tech.mmarca.openvitals.ui.components.metricLineChartFrame
import tech.mmarca.openvitals.ui.components.metricLinePointFraction

/** What the line chart works out once per data change, instead of on every recomposition and draw. */
class MetricLineChartFrameTest {

    private val monday = LocalDate.of(2026, 9, 14)
    private val week = DatePeriod(monday, monday.plusDays(6))

    private fun series(vararg points: MetricLinePoint) = listOf(MetricLineSeries(points.toList(), Color.Red, "hr"))

    @Test
    fun `points outside the period and values that are not numbers are left out of the range`() {
        val frame = metricLineChartFrame(
            series(
                MetricLinePoint(monday.minusDays(1), value = 500.0),
                MetricLinePoint(monday, value = 60.0),
                MetricLinePoint(monday.plusDays(1), value = Double.NaN),
                MetricLinePoint(monday.plusDays(2), value = 72.0),
            ),
            week,
            TimeRange.WEEK,
        )

        assertNotNull(frame)
        assertEquals(60.0, frame!!.minValue, 0.0)
        assertEquals(72.0, frame.maxValue, 0.0)
        assertEquals(2, frame.series.single().points.size)
    }

    @Test
    fun `nothing in the period, or a day with one instant, draws no line`() {
        assertNull(metricLineChartFrame(series(MetricLinePoint(monday.minusDays(9), value = 1.0)), week, TimeRange.WEEK))

        val noon = monday.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant()
        val day = DatePeriod(monday, monday)
        assertNull(metricLineChartFrame(series(MetricLinePoint(date = monday, value = 60.0, time = noon)), day, TimeRange.DAY))
        assertNotNull(
            metricLineChartFrame(
                series(MetricLinePoint(date = monday, value = 60.0, time = noon), MetricLinePoint(date = monday, value = 61.0, time = noon.plusSeconds(60))),
                day,
                TimeRange.DAY,
            ),
        )
    }

    @Test
    fun `a point sits in the middle of its day's slot, and the highest value at the top`() {
        val fraction = metricLinePointFraction(
            point = MetricLinePoint(monday.plusDays(3), value = 100.0),
            selectedRange = TimeRange.WEEK,
            period = week,
            dayStart = Instant.EPOCH,
            dayDurationMillis = 1L,
            periodDayCount = 7,
            minValue = 0.0,
            maxValue = 100.0,
        )

        assertEquals(3.5f / 7f, fraction.x, 1e-6f)
        assertEquals(0f, fraction.y, 1e-6f)
    }

    @Test
    fun `a series narrower than one unit still fills its axis`() {
        // A tenth of a degree, a mmol/L: the top of the data sat a third of the way up, under its own label.
        val fraction = metricLinePointFraction(
            point = MetricLinePoint(monday, value = 0.4),
            selectedRange = TimeRange.WEEK,
            period = week,
            dayStart = Instant.EPOCH,
            dayDurationMillis = 1L,
            periodDayCount = 7,
            minValue = 0.0,
            maxValue = 0.4,
        )

        assertEquals(0f, fraction.y, 1e-6f)
    }

    @Test
    fun `the padded axis keeps the line charts' own rule`() {
        val (min, max) = LineAxisRange.Padded.resolve(60.0, 72.0)
        assertEquals(60.0 - 12.0 * 0.08, min, 1e-9)
        assertEquals(72.0 + 12.0 * 0.08, max, 1e-9)

        val (flatMin, flatMax) = LineAxisRange.Padded.resolve(5.0, 5.0)
        assertEquals(4.0, flatMin, 1e-9)
        assertEquals(6.0, flatMax, 1e-9)
    }

    @Test
    fun `a zero-centred axis is symmetric, so the middle label is zero`() {
        val (min, max) = LineAxisRange.ZeroCentred.resolve(-0.3, 0.7)
        assertEquals(-0.756, min, 1e-9)
        assertEquals(0.756, max, 1e-9)
        assertEquals(0.0, (min + max) / 2, 1e-9)
    }

    @Test
    fun `a zero-centred axis gives a flat zero series room`() {
        val (min, max) = LineAxisRange.ZeroCentred.resolve(0.0, 0.0)
        assertEquals(-0.108, min, 1e-9)
        assertEquals(0.108, max, 1e-9)
    }
}
