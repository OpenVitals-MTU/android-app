package tech.mmarca.openvitals.ui.components

import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.periodFor
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PeriodNavigatorTest {

    // All anchor dates are well in the past so coerceAtMost(today) never clips them.
    private val monday = LocalDate.of(2023, 1, 9)   // a known Monday
    private val wednesday = LocalDate.of(2023, 3, 15)
    private val firstOfMonth = LocalDate.of(2023, 1, 1)
    private val midYear = LocalDate.of(2023, 6, 15)

    // DAY.

    @Test fun `periodFor DAY returns single-day period`() {
        val period = periodFor(TimeRange.DAY, wednesday)
        assertEquals(wednesday, period.start)
        assertEquals(wednesday, period.end)
    }

    // WEEK.

    @Test fun `periodFor WEEK anchored on Monday spans Mon to Sun`() {
        val period = periodFor(TimeRange.WEEK, monday)
        assertEquals(monday, period.start)
        assertEquals(monday.plusDays(6), period.end)
    }

    @Test fun `periodFor WEEK anchored mid-week snaps start back to Monday`() {
        assertEquals(
            DatePeriod(LocalDate.of(2023, 3, 13), LocalDate.of(2023, 3, 19)),
            periodFor(TimeRange.WEEK, wednesday),
        )
    }

    // MONTH.

    @Test fun `periodFor MONTH spans the first to the last day of the month`() {
        assertEquals(
            mapOf(
                midYear to DatePeriod(LocalDate.of(2023, 6, 1), LocalDate.of(2023, 6, 30)),
                firstOfMonth to DatePeriod(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 1, 31)),
            ),
            listOf(midYear, firstOfMonth).associateWith { periodFor(TimeRange.MONTH, it) },
        )
    }

    @Test fun `periodFor MONTH respects February length in leap year`() {
        val feb2024 = LocalDate.of(2024, 2, 14)
        val period = periodFor(TimeRange.MONTH, feb2024)
        assertEquals(LocalDate.of(2024, 2, 1), period.start)
        assertEquals(LocalDate.of(2024, 2, 29), period.end)
    }

    // YEAR.

    @Test fun `periodFor YEAR spans January 1 to December 31`() {
        assertEquals(
            DatePeriod(LocalDate.of(2023, 1, 1), LocalDate.of(2023, 12, 31)),
            periodFor(TimeRange.YEAR, midYear),
        )
    }

    // coerceAtMost(today) guard.

    @Test fun `periodFor end is never after today`() {
        val today = wednesday

        assertEquals(
            mapOf(
                TimeRange.DAY to DatePeriod(today, today),
                TimeRange.WEEK to DatePeriod(LocalDate.of(2023, 3, 13), today),
                TimeRange.MONTH to DatePeriod(LocalDate.of(2023, 3, 1), today),
                TimeRange.YEAR to DatePeriod(LocalDate.of(2023, 1, 1), today),
            ),
            TimeRange.entries.associateWith { periodFor(it, anchorDate = today, today = today) },
        )
    }

    // Ordering invariant.

    @Test fun `consecutively earlier anchors produce consecutively earlier periods`() {
        val week1 = periodFor(TimeRange.WEEK, monday)
        val week2 = periodFor(TimeRange.WEEK, monday.minusWeeks(1))
        assertTrue(week2.start.isBefore(week1.start))
        assertTrue(week2.end.isBefore(week1.end))
    }
}
