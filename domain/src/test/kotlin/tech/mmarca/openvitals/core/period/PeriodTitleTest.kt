package tech.mmarca.openvitals.core.period

import java.time.LocalDate
import java.util.Locale
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class PeriodTitleTest {
    private val today = LocalDate.of(2026, 6, 10)

    // The dated labels are locale-sensitive; the titles themselves are hard-coded English.
    private var previousLocale: Locale = Locale.getDefault()

    @Before
    fun pinLocale() {
        previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.ENGLISH)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(previousLocale)
    }

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
    fun pastPeriodTitlesUseDatedLabels() {
        assertEquals(
            "Week of Mon 1 Jun",
            periodTitle(
                TimeRange.WEEK,
                DatePeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 7)),
                today,
            ),
        )
        assertEquals(
            "May 2026",
            periodTitle(
                TimeRange.MONTH,
                DatePeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                today,
            ),
        )
        assertEquals(
            "2025",
            periodTitle(
                TimeRange.YEAR,
                DatePeriod(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31)),
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

    @Test
    fun pastRollingPeriodsReadAsTheDatedSpanTheyCover() {
        // A rolling month that no longer ends today is a 30-day span, not the calendar month its start falls in.
        assertEquals(
            "12 Apr – 11 May 2026",
            periodTitle(
                TimeRange.MONTH,
                DatePeriod(today.minusDays(59), today.minusDays(30)),
                today,
                weekPeriodMode = WeekPeriodMode.LAST_7_DAYS,
            ),
        )
        assertEquals(
            "28 May – 3 Jun 2026",
            periodTitle(
                TimeRange.WEEK,
                DatePeriod(today.minusDays(13), today.minusDays(7)),
                today,
                weekPeriodMode = WeekPeriodMode.LAST_7_DAYS,
            ),
        )
    }

    @Test
    fun aPastRollingSpanThatStraddlesAYearShowsBothYears() {
        assertEquals(
            "20 Dec 2024 – 19 Dec 2025",
            periodTitle(
                TimeRange.YEAR,
                DatePeriod(LocalDate.of(2024, 12, 20), LocalDate.of(2025, 12, 19)),
                today,
                weekPeriodMode = WeekPeriodMode.LAST_7_DAYS,
            ),
        )
    }
}
