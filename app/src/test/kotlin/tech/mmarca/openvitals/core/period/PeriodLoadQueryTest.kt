package tech.mmarca.openvitals.core.period

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PeriodLoadQueryTest {

    private val today = LocalDate.of(2026, 5, 25)

    @Test fun `query clamps future anchor date before creating windows`() {
        val query = PeriodLoadQuery(
            range = TimeRange.WEEK,
            anchorDate = today.plusDays(4),
            today = today,
        )

        assertEquals(today, query.selectedDate)
        assertEquals(LocalDate.of(2026, 5, 25), query.windows.current.start)
        assertEquals(today, query.windows.current.end)
    }

    @Test fun `query creates current previous and baseline windows`() {
        val query = PeriodLoadQuery(
            range = TimeRange.MONTH,
            anchorDate = LocalDate.of(2026, 4, 14),
            today = today,
            baselineDays = 30,
        )

        assertEquals(DatePeriod(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)), query.windows.current)
        assertEquals(DatePeriod(LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)), query.windows.previous)
        assertEquals(DatePeriod(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 31)), query.windows.baseline)
    }

    @Test fun `query uses rolling last seven days when week period mode requests it`() {
        val query = PeriodLoadQuery(
            range = TimeRange.WEEK,
            anchorDate = today,
            today = today,
            baselineDays = 30,
            weekPeriodMode = WeekPeriodMode.LAST_7_DAYS,
        )

        assertEquals(DatePeriod(today.minusDays(6), today), query.windows.current)
        assertEquals(DatePeriod(today.minusDays(13), today.minusDays(7)), query.windows.previous)
        assertEquals(DatePeriod(today.minusDays(36), today.minusDays(7)), query.windows.baseline)
    }

    @Test fun `query clips current Monday to Sunday load window to today`() {
        val wednesday = LocalDate.of(2026, 5, 27)
        val query = PeriodLoadQuery(
            range = TimeRange.WEEK,
            anchorDate = wednesday,
            today = wednesday,
            weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
        )

        assertEquals(DatePeriod(LocalDate.of(2026, 5, 25), wednesday), query.windows.current)
    }

    @Test fun `selection driver persists range and clamps next period`() {
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.MONTH,
            initialDate = LocalDate.of(2026, 4, 15),
        )

        assertEquals(TimeRange.WEEK, driver.selectRange(TimeRange.WEEK).selectedRange)
        assertEquals(LocalDate.of(2026, 4, 8), driver.previousPeriod().selectedDate)

        val currentDriver = PeriodSelectionDriver(
            initialRange = TimeRange.WEEK,
            initialDate = LocalDate.now(),
        )

        assertEquals(null, currentDriver.nextPeriod())
    }

    @Test fun `selection driver advances unpinned stale day to today on resume`() {
        val startDate = LocalDate.now()
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.DAY,
            initialDate = startDate,
        )
        val tomorrow = startDate.plusDays(1)

        val updated = driver.resumeCurrentPeriod(tomorrow)

        assertEquals(tomorrow, updated?.selectedDate)
        assertEquals(tomorrow, driver.selection.selectedDate)
    }

    @Test fun `selection driver keeps user pinned past day on resume`() {
        val startDate = LocalDate.now()
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.DAY,
            initialDate = startDate,
        )
        val yesterday = startDate.minusDays(1)

        driver.previousPeriod()
        val updated = driver.resumeCurrentPeriod(startDate.plusDays(1))

        assertEquals(null, updated)
        assertEquals(yesterday, driver.selection.selectedDate)
    }
}
