package tech.mmarca.openvitals.core.period

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import java.time.LocalDate

/**
 * Flipping [WeekPeriodMode] must reload the unchanged anchor: the same anchor derives a
 * differently shaped period, so the old data cannot stay on screen, and the title follows.
 */
class PeriodSelectionDriverWeekModeTest {

    // A Wednesday, so the calendar week and the rolling week genuinely differ.
    private val today: LocalDate = LocalDate.of(2025, 6, 25)

    @Test
    fun `changing the week mode reloads the selection and retitles`() {
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.WEEK,
            initialDate = today,
            initialWeekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
        )
        val anchorBefore = driver.selection
        val queryBefore = loadQuery(driver)
        assertEquals(
            "This week",
            periodTitle(TimeRange.WEEK, queryBefore.windows.current, today, WeekPeriodMode.MONDAY_TO_SUNDAY),
        )

        driver.weekPeriodMode = WeekPeriodMode.LAST_7_DAYS

        // The anchor selection itself is untouched by the mode flip...
        assertEquals(anchorBefore, driver.selection)

        // The unchanged anchor now derives a differently shaped window, so the host must reload.
        val queryAfter = loadQuery(driver)
        assertNotEquals(queryBefore.windows.current, queryAfter.windows.current)
        assertEquals(
            DatePeriod(start = today.minusDays(6), end = today),
            queryAfter.windows.current,
        )

        // ...and the title follows the new mode.
        assertEquals(
            "Last 7 days",
            periodTitle(TimeRange.WEEK, queryAfter.windows.current, today, WeekPeriodMode.LAST_7_DAYS),
        )
    }

    @Test
    fun `flipping the week mode back re-derives the calendar week for the same anchor`() {
        val driver = PeriodSelectionDriver(
            initialRange = TimeRange.WEEK,
            initialDate = today,
            initialWeekPeriodMode = WeekPeriodMode.LAST_7_DAYS,
        )
        assertEquals(
            DatePeriod(start = today.minusDays(6), end = today),
            loadQuery(driver).windows.current,
        )

        driver.weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY

        // Monday of the anchor's week, clipped to today.
        assertEquals(
            DatePeriod(start = LocalDate.of(2025, 6, 23), end = today),
            loadQuery(driver).windows.current,
        )
        assertEquals(
            "This week",
            periodTitle(TimeRange.WEEK, loadQuery(driver).windows.current, today, WeekPeriodMode.MONDAY_TO_SUNDAY),
        )
    }

    /** The exact query the view models rebuild on every load(). */
    private fun loadQuery(driver: PeriodSelectionDriver): PeriodLoadQuery =
        PeriodLoadQuery(
            range = driver.selection.selectedRange,
            anchorDate = driver.selection.selectedDate,
            today = today,
            weekPeriodMode = driver.weekPeriodMode,
        )
}
