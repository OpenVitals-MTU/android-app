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

    @Test
    fun rollingPeriodTitlesUseFixedDayWindowLabels() {
        assertEquals(
            "Last 7 days",
            periodTitle(
                TimeRange.WEEK,
                DatePeriod(today.minusDays(6), today),
                today,
                weekPeriodMode = WeekPeriodMode.LAST_7_DAYS,
            ),
        )
        assertEquals(
            "Last 30 days",
            periodTitle(
                TimeRange.MONTH,
                DatePeriod(today.minusDays(29), today),
                today,
                weekPeriodMode = WeekPeriodMode.LAST_7_DAYS,
            ),
        )
        assertEquals(
            "Last 365 days",
            periodTitle(
                TimeRange.YEAR,
                DatePeriod(today.minusDays(364), today),
                today,
                weekPeriodMode = WeekPeriodMode.LAST_7_DAYS,
            ),
        )
    }
}
