package tech.mmarca.openvitals.core.period

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PeriodTitleTest {
    private val today = LocalDate.of(2026, 6, 10)

    @Test
    fun dayTitlesUseRelativeLabelsForTodayAndYesterday() {
        assertEquals(
            "Today",
            periodTitle(TimeRange.DAY, DatePeriod(today, today), today),
        )
        assertEquals(
            "Yesterday",
            periodTitle(TimeRange.DAY, DatePeriod(today.minusDays(1), today.minusDays(1)), today),
        )
    }

    @Test
    fun periodTitlesUseCurrentLabelsWhenPeriodContainsToday() {
        assertEquals(
            "This week",
            periodTitle(
                TimeRange.WEEK,
                DatePeriod(LocalDate.of(2026, 6, 8), today),
                today,
            ),
        )
        assertEquals(
            "This month",
            periodTitle(
                TimeRange.MONTH,
                DatePeriod(LocalDate.of(2026, 6, 1), today),
                today,
            ),
        )
        assertEquals(
            "This year",
            periodTitle(
                TimeRange.YEAR,
                DatePeriod(LocalDate.of(2026, 1, 1), today),
                today,
            ),
        )
    }
}
