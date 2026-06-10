package tech.mmarca.openvitals.core.period

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PeriodSelectionTest {

    private val today = LocalDate.of(2026, 4, 26)

    @Test fun `selectRange keeps future selected date capped at today`() {
        val selection = PeriodSelection(
            selectedRange = TimeRange.WEEK,
            selectedDate = today.plusDays(3),
        )

        val updated = selection.selectRange(TimeRange.MONTH, today)

        assertEquals(TimeRange.MONTH, updated.selectedRange)
        assertEquals(today, updated.selectedDate)
    }

    @Test fun `previousPeriod moves by selected range`() {
        val selection = PeriodSelection(
            selectedRange = TimeRange.MONTH,
            selectedDate = LocalDate.of(2026, 4, 15),
        )

        assertEquals(LocalDate.of(2026, 3, 15), selection.previousPeriod().selectedDate)
    }

    @Test fun `nextPeriod does not move beyond current period`() {
        val selection = PeriodSelection(
            selectedRange = TimeRange.WEEK,
            selectedDate = today,
        )

        assertEquals(selection, selection.nextPeriod(today))
    }

    @Test fun `nextPeriod moves when the next period is not in the future`() {
        val selection = PeriodSelection(
            selectedRange = TimeRange.WEEK,
            selectedDate = today.minusWeeks(2),
        )

        val updated = selection.nextPeriod(today)

        assertEquals(today.minusWeeks(1), updated.selectedDate)
        assertFalse(updated.period(today).end.isAfter(today))
    }

    @Test fun `previousPeriodFor returns previous calendar period`() {
        val period = previousPeriodFor(
            range = TimeRange.MONTH,
            anchorDate = LocalDate.of(2026, 4, 15),
            today = today,
        )

        assertEquals(LocalDate.of(2026, 3, 1), period.start)
        assertEquals(LocalDate.of(2026, 3, 31), period.end)
    }

    @Test fun `displayPeriodFor keeps full Monday to Sunday week even when today is mid week`() {
        val wednesday = LocalDate.of(2026, 5, 27)
        val period = displayPeriodFor(
            range = TimeRange.WEEK,
            anchorDate = wednesday,
            today = wednesday,
            weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
        )

        assertEquals(LocalDate.of(2026, 5, 25), period.start)
        assertEquals(LocalDate.of(2026, 5, 31), period.end)
    }

    @Test fun `displayPeriodFor supports rolling last seven days`() {
        val period = displayPeriodFor(
            range = TimeRange.WEEK,
            anchorDate = today,
            today = today,
            weekPeriodMode = WeekPeriodMode.LAST_7_DAYS,
        )

        assertEquals(today.minusDays(6), period.start)
        assertEquals(today, period.end)
    }
}
