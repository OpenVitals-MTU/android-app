package tech.mmarca.openvitals.domain.report

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ReportDailyValue
import tech.mmarca.openvitals.domain.model.ReportGranularity
import tech.mmarca.openvitals.domain.model.ReportValueKind
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode

class ReportRollupTest {

    // A Monday, so the Monday-aligned weekly cases read plainly.
    private val monday = LocalDate.of(2026, 6, 1)

    private fun day(offset: Long, value: Double, min: Double? = null, max: Double? = null, secondary: Double? = null) =
        ReportDailyValue(
            date = monday.plusDays(offset),
            value = value,
            min = min,
            max = max,
            secondaryValue = secondary,
        )

    @Test fun `daily granularity keeps one point per day with data and omits gaps`() {
        val points = ReportRollup.rollup(
            daily = listOf(day(0, 100.0), day(2, 300.0)),
            valueKind = ReportValueKind.SUM,
            granularity = ReportGranularity.DAILY,
            weekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
            rangeStart = monday,
            rangeEnd = monday.plusDays(6),
        )

        assertEquals(2, points.size)
        assertEquals(monday, points[0].bucketStart)
        assertEquals(monday, points[0].bucketEnd)
        assertEquals(100.0, points[0].value, 1e-9)
        assertEquals(monday.plusDays(2), points[1].bucketStart)
        assertEquals(300.0, points[1].value, 1e-9)
    }

    @Test fun `SUM buckets add up and AVERAGE buckets mean out`() {
        val daily = listOf(day(0, 10.0), day(1, 20.0), day(2, 30.0))

        val sum = ReportRollup.rollup(
            daily, ReportValueKind.SUM, ReportGranularity.WEEKLY,
            ActivityWeekMode.MONDAY_TO_SUNDAY, monday, monday.plusDays(6),
        ).single()
        val avg = ReportRollup.rollup(
            daily, ReportValueKind.AVERAGE, ReportGranularity.WEEKLY,
            ActivityWeekMode.MONDAY_TO_SUNDAY, monday, monday.plusDays(6),
        ).single()

        assertEquals(60.0, sum.value, 1e-9)
        assertEquals(20.0, avg.value, 1e-9)
        assertEquals(3, sum.daysWithData)
    }

    @Test fun `monday-aligned weeks split on the calendar boundary`() {
        val points = ReportRollup.rollup(
            daily = listOf(day(5, 1.0), day(6, 2.0), day(7, 4.0)),
            valueKind = ReportValueKind.SUM,
            granularity = ReportGranularity.WEEKLY,
            weekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
            rangeStart = monday,
            rangeEnd = monday.plusDays(13),
        )

        assertEquals(2, points.size)
        assertEquals(3.0, points[0].value, 1e-9)
        assertEquals(monday.plusDays(7), points[1].bucketStart)
        assertEquals(4.0, points[1].value, 1e-9)
    }

    @Test fun `LAST_7_DAYS weeks are rolling blocks anchored at the range start`() {
        val start = monday.plusDays(3)
        val points = ReportRollup.rollup(
            daily = listOf(
                ReportDailyValue(start, 1.0),
                ReportDailyValue(start.plusDays(6), 2.0),
                ReportDailyValue(start.plusDays(7), 4.0),
            ),
            valueKind = ReportValueKind.SUM,
            granularity = ReportGranularity.WEEKLY,
            weekMode = ActivityWeekMode.LAST_7_DAYS,
            rangeStart = start,
            rangeEnd = start.plusDays(13),
        )

        assertEquals(2, points.size)
        assertEquals(start, points[0].bucketStart)
        assertEquals(start.plusDays(6), points[0].bucketEnd)
        assertEquals(3.0, points[0].value, 1e-9)
        assertEquals(start.plusDays(7), points[1].bucketStart)
    }

    @Test fun `monthly buckets clamp their edges to the report range`() {
        val rangeStart = LocalDate.of(2026, 5, 20)
        val rangeEnd = LocalDate.of(2026, 6, 10)
        val points = ReportRollup.rollup(
            daily = listOf(
                ReportDailyValue(LocalDate.of(2026, 5, 25), 1.0),
                ReportDailyValue(LocalDate.of(2026, 6, 5), 2.0),
            ),
            valueKind = ReportValueKind.SUM,
            granularity = ReportGranularity.MONTHLY,
            weekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd,
        )

        assertEquals(2, points.size)
        assertEquals(rangeStart, points[0].bucketStart)
        assertEquals(LocalDate.of(2026, 5, 31), points[0].bucketEnd)
        assertEquals(LocalDate.of(2026, 6, 1), points[1].bucketStart)
        assertEquals(rangeEnd, points[1].bucketEnd)
    }

    @Test fun `bucket min and max fold the daily extremes where the source has them`() {
        val point = ReportRollup.rollup(
            daily = listOf(
                day(0, 70.0, min = 48.0, max = 155.0),
                day(1, 68.0, min = 51.0, max = 130.0),
                day(2, 90.0),
            ),
            valueKind = ReportValueKind.AVERAGE,
            granularity = ReportGranularity.WEEKLY,
            weekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
            rangeStart = monday,
            rangeEnd = monday.plusDays(6),
        ).single()

        assertEquals(48.0, point.min, 1e-9)
        assertEquals(155.0, point.max, 1e-9)
        assertEquals(76.0, point.value, 1e-9)
    }

    @Test fun `values outside the range are dropped before bucketing`() {
        val points = ReportRollup.rollup(
            daily = listOf(day(-1, 999.0), day(0, 1.0), day(7, 999.0)),
            valueKind = ReportValueKind.SUM,
            granularity = ReportGranularity.WEEKLY,
            weekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
            rangeStart = monday,
            rangeEnd = monday.plusDays(6),
        )

        assertEquals(1.0, points.single().value, 1e-9)
    }

    @Test fun `blood pressure's diastolic rides along as the secondary series`() {
        val point = ReportRollup.rollup(
            daily = listOf(
                day(0, 120.0, secondary = 80.0),
                day(1, 130.0, secondary = 86.0),
            ),
            valueKind = ReportValueKind.AVERAGE,
            granularity = ReportGranularity.WEEKLY,
            weekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
            rangeStart = monday,
            rangeEnd = monday.plusDays(6),
        ).single()

        assertEquals(125.0, point.value, 1e-9)
        assertEquals(83.0, point.secondaryValue!!, 1e-9)
        assertEquals(80.0, point.secondaryMin!!, 1e-9)
        assertEquals(86.0, point.secondaryMax!!, 1e-9)
    }

    @Test fun `summary comes from daily values and only SUM metrics get a total`() {
        val daily = listOf(day(0, 10.0), day(1, 20.0), day(3, 60.0))

        val sum = ReportRollup.summarize(daily, ReportValueKind.SUM)!!
        val avg = ReportRollup.summarize(daily, ReportValueKind.AVERAGE)!!

        assertEquals(30.0, sum.average, 1e-9)
        assertEquals(10.0, sum.min, 1e-9)
        assertEquals(60.0, sum.max, 1e-9)
        assertEquals(90.0, sum.total!!, 1e-9)
        assertEquals(3, sum.daysWithData)
        assertNull(avg.total)
    }

    @Test fun `summary min and max use the daily extremes, not the daily means`() {
        val summary = ReportRollup.summarize(
            listOf(day(0, 70.0, min = 44.0, max = 160.0), day(1, 72.0)),
            ReportValueKind.AVERAGE,
        )!!

        assertEquals(44.0, summary.min, 1e-9)
        assertEquals(160.0, summary.max, 1e-9)
    }

    @Test fun `change over range is last minus first daily value, null for a single day`() {
        val changed = ReportRollup.summarize(
            listOf(day(3, 82.0), day(0, 84.5)), // arrival order must not matter
            ReportValueKind.AVERAGE,
        )!!
        val single = ReportRollup.summarize(listOf(day(0, 84.5)), ReportValueKind.AVERAGE)!!

        assertEquals(-2.5, changed.changeOverRange!!, 1e-9)
        assertNull(single.changeOverRange)
    }

    @Test fun `summary of nothing is null and rollup of nothing is empty`() {
        assertNull(ReportRollup.summarize(emptyList(), ReportValueKind.SUM))
        assertTrue(
            ReportRollup.rollup(
                emptyList(), ReportValueKind.SUM, ReportGranularity.DAILY,
                ActivityWeekMode.MONDAY_TO_SUNDAY, monday, monday,
            ).isEmpty(),
        )
    }
}
