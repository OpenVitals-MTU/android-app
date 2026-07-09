package tech.mmarca.openvitals.ui.components

import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.core.period.periodFor
import java.time.DayOfWeek
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodNavigatorTest {

    // All anchor dates are well in the past so coerceAtMost(today) never clips them.
    private val monday = LocalDate.of(2023, 1, 9)   // a known Monday
    private val wednesday = LocalDate.of(2023, 3, 15)
    private val firstOfMonth = LocalDate.of(2023, 1, 1)
    private val midYear = LocalDate.of(2023, 6, 15)

    // ─── DAY ──────────────────────────────────────────────────────────────────

    @Test fun `periodFor DAY returns single-day period`() {
        val period = periodFor(TimeRange.DAY, wednesday)
        assertEquals(wednesday, period.start)
        assertEquals(wednesday, period.end)
    }

    // ─── WEEK ─────────────────────────────────────────────────────────────────

    @Test fun `periodFor WEEK anchored on Monday spans Mon to Sun`() {
        val period = periodFor(TimeRange.WEEK, monday)
        assertEquals(monday, period.start)
        assertEquals(monday.plusDays(6), period.end)
    }

    @Test fun `periodFor WEEK anchored mid-week snaps start back to Monday`() {
        val period = periodFor(TimeRange.WEEK, wednesday)
        assertEquals(DayOfWeek.MONDAY, period.start.dayOfWeek)
        assertEquals(period.start.plusDays(6), period.end)
    }

    @Test fun `periodFor WEEK start is always Monday`() {
        val period = periodFor(TimeRange.WEEK, wednesday)
        assertEquals(DayOfWeek.MONDAY, period.start.dayOfWeek)
    }

    @Test fun `periodFor WEEK end is always Sunday for past dates`() {
        val period = periodFor(TimeRange.WEEK, wednesday)
        assertEquals(DayOfWeek.SUNDAY, period.end.dayOfWeek)
    }

    // ─── MONTH ────────────────────────────────────────────────────────────────

    @Test fun `periodFor MONTH start is first day of month`() {
        val period = periodFor(TimeRange.MONTH, midYear)
        assertEquals(LocalDate.of(2023, 6, 1), period.start)
    }

    @Test fun `periodFor MONTH end is last day of month`() {
        val period = periodFor(TimeRange.MONTH, midYear)
        assertEquals(LocalDate.of(2023, 6, 30), period.end)
    }

    @Test fun `periodFor MONTH spans full month for January`() {
        val period = periodFor(TimeRange.MONTH, firstOfMonth)
        assertEquals(LocalDate.of(2023, 1, 1), period.start)
        assertEquals(LocalDate.of(2023, 1, 31), period.end)
    }

    @Test fun `periodFor MONTH respects February length in leap year`() {
        val feb2024 = LocalDate.of(2024, 2, 14)
        val period = periodFor(TimeRange.MONTH, feb2024)
        assertEquals(LocalDate.of(2024, 2, 1), period.start)
        assertEquals(LocalDate.of(2024, 2, 29), period.end)
    }

    @Test fun `periodFor MONTH supports rolling last thirty days`() {
        val period = periodFor(
            range = TimeRange.MONTH,
            anchorDate = wednesday,
            weekPeriodMode = WeekPeriodMode.LAST_7_DAYS,
        )

        assertEquals(wednesday.minusDays(29), period.start)
        assertEquals(wednesday, period.end)
    }

    // ─── YEAR ─────────────────────────────────────────────────────────────────

    @Test fun `periodFor YEAR start is January 1`() {
        val period = periodFor(TimeRange.YEAR, midYear)
        assertEquals(LocalDate.of(2023, 1, 1), period.start)
    }

    @Test fun `periodFor YEAR end is December 31`() {
        val period = periodFor(TimeRange.YEAR, midYear)
        assertEquals(LocalDate.of(2023, 12, 31), period.end)
    }

    @Test fun `periodFor YEAR supports rolling last three hundred sixty five days`() {
        val period = periodFor(
            range = TimeRange.YEAR,
            anchorDate = midYear,
            weekPeriodMode = WeekPeriodMode.LAST_7_DAYS,
        )

        assertEquals(midYear.minusDays(364), period.start)
        assertEquals(midYear, period.end)
    }

    // ─── coerceAtMost(today) guard ────────────────────────────────────────────

    @Test fun `periodFor end is never after today`() {
        val today = LocalDate.now()
        for (range in TimeRange.entries) {
            val period = periodFor(range, today)
            assertFalse(
                "Period end for $range should not be after today",
                period.end.isAfter(today),
            )
        }
    }

    @Test fun `periodFor start is never after end for current periods`() {
        val today = LocalDate.now()
        for (range in TimeRange.entries) {
            val period = periodFor(range, today)
            assertFalse(
                "Period start for $range should not be after end",
                period.start.isAfter(period.end),
            )
        }
    }

    // ─── Ordering invariant ───────────────────────────────────────────────────

    @Test fun `periodFor start is never after end for past dates`() {
        val pastDates = listOf(wednesday, monday, firstOfMonth, midYear)
        for (date in pastDates) {
            for (range in TimeRange.entries) {
                val period = periodFor(range, date)
                assertFalse(
                    "start should be <= end for $range at $date",
                    period.start.isAfter(period.end),
                )
            }
        }
    }

    @Test fun `consecutively earlier anchors produce consecutively earlier periods`() {
        val week1 = periodFor(TimeRange.WEEK, monday)
        val week2 = periodFor(TimeRange.WEEK, monday.minusWeeks(1))
        assertTrue(week2.start.isBefore(week1.start))
        assertTrue(week2.end.isBefore(week1.end))
    }
}
