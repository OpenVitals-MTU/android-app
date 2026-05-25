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

    @Test fun `selection driver persists range and clamps next period`() {
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.MONTH,
            initialDate = LocalDate.of(2026, 4, 15),
        )

        assertEquals(TimeRange.WEEK, driver.selectRange(TimeRange.WEEK).selectedRange)
        assertEquals(LocalDate.of(2026, 4, 8), driver.previousPeriod().selectedDate)

        val currentDriver = PeriodSelectionDriver(
            initialRange = TimeRange.WEEK,
            initialDate = today,
        )

        assertEquals(null, currentDriver.nextPeriod())
    }
}
