package tech.mmarca.openvitals.core.period

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PeriodSelectionDriverSelectDayTest {

    @Test
    fun `selectDay switches to the day range anchored on the tapped date`() {
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.MONTH,
            initialDate = LocalDate.now(),
        )

        val selection = driver.selectDay(LocalDate.now().minusDays(3))

        assertEquals(TimeRange.DAY, selection.selectedRange)
        assertEquals(LocalDate.now().minusDays(3), selection.selectedDate)
        assertEquals(selection, driver.selection)
    }

    @Test
    fun `selectDay persists the day range like selectRange does`() {
        var persisted: TimeRange? = null
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.MONTH,
            initialDate = LocalDate.now(),
            onRangeSelected = { persisted = it },
        )

        driver.selectDay(LocalDate.now().minusDays(2))

        assertEquals(TimeRange.DAY, persisted)
    }

    @Test
    fun `selectDay pins a past day so resuming does not bounce back to today`() {
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.MONTH,
            initialDate = LocalDate.now(),
        )

        driver.selectDay(LocalDate.now().minusDays(5))

        assertNull(driver.resumeCurrentPeriod())
        assertEquals(LocalDate.now().minusDays(5), driver.selection.selectedDate)
    }

    @Test
    fun `selecting today still resumes, because today is not a pinned past period`() {
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.MONTH,
            initialDate = LocalDate.now(),
        )

        driver.selectDay(LocalDate.now())

        assertNull(driver.resumeCurrentPeriod())
        assertEquals(TimeRange.DAY, driver.selection.selectedRange)
    }
}
