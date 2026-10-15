package tech.mmarca.openvitals.core.period

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PeriodSelectionDriverTest {

    @Test fun `an initial date anchors the date but keeps the given range`() {
        val yesterday = LocalDate.now().minusDays(1)
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.WEEK,
            initialDate = yesterday,
        )

        assertEquals(TimeRange.WEEK, driver.selection.selectedRange)
        assertEquals(yesterday, driver.selection.selectedDate)
    }

    @Test fun `a future initial date is clamped to today`() {
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.DAY,
            initialDate = LocalDate.now().plusDays(3),
        )

        assertEquals(LocalDate.now(), driver.selection.selectedDate)
    }

    @Test fun `arriving pinned to a past period suppresses auto-resume`() {
        val lastMonth = LocalDate.now().minusDays(40)
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.DAY,
            initialDate = lastMonth,
        )

        assertNull(driver.resumeCurrentPeriod())
        assertEquals(lastMonth, driver.selection.selectedDate)
    }

    @Test fun `arriving on today still auto-resumes after the day rolls over`() {
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.DAY,
            initialDate = LocalDate.now(),
        )

        val tomorrow = LocalDate.now().plusDays(1)
        val resumed = driver.resumeCurrentPeriod(today = tomorrow)

        assertEquals(tomorrow, resumed?.selectedDate)
    }
}
