// The chart composables live under ui/charts/ but declare ui.components; this file mirrors that.
package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.testing.goldenDates
import tech.mmarca.openvitals.ui.theme.SleepColor

/**
 * [MetricBarChart] / [PeriodHistoryChart]: WEEK is bars, MONTH a calendar heatmap, YEAR a dot
 * heatmap, from one call. The range dispatch is what these shoot.
 */
class PeriodChartGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun week_bars() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 300.dp) {
                SleepBarChart(range = TimeRange.WEEK, period = WEEK, summary = "7h 32m avg")
            }
        }

        composeRule.assertVisualRootMatchesGolden("period_chart_week")
    }

    @Test
    fun week_barsWithTheSelectedDayHighlighted() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 300.dp) {
                SleepBarChart(
                    range = TimeRange.WEEK,
                    period = WEEK,
                    summary = "7h 32m avg",
                    selectedDate = LocalDate.of(2026, 6, 19),
                    onDateSelected = {},
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("period_chart_week_selected")
    }

    @Test
    fun month_theCalendarHeatmap() {
        // The rolling 30-day window the navigator hands over, read off [LocalPeriodWeekMode].
        val month = DatePeriod(LocalDate.of(2026, 5, 24), LocalDate.of(2026, 6, 22))

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 460.dp) {
                CompositionLocalProvider(LocalPeriodWeekMode provides WeekPeriodMode.LAST_7_DAYS) {
                    SleepBarChart(range = TimeRange.MONTH, period = month, summary = "7h 28m avg")
                }
            }
        }

        composeRule.assertVisualRootMatchesGolden("period_chart_month")
    }

    @Test
    fun year_theDotHeatmap() {
        // The year heatmap always draws the calendar year of `period.start`; that divergence is pinned.
        val year = DatePeriod(LocalDate.of(2025, 7, 1), LocalDate.of(2026, 6, 22))

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 440.dp) {
                SleepBarChart(
                    range = TimeRange.YEAR,
                    period = year,
                    summary = "7h 30m avg",
                    yearAggregation = PeriodBarAggregation.AVERAGE_NON_ZERO,
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("period_chart_year")
    }

    @Composable
    private fun SleepBarChart(
        range: TimeRange,
        period: DatePeriod,
        summary: String,
        selectedDate: LocalDate? = null,
        onDateSelected: ((LocalDate) -> Unit)? = null,
        yearAggregation: PeriodBarAggregation = PeriodBarAggregation.SUM,
    ) {
        MetricBarChart(
            title = "Sleep",
            values = nights(period.start, period.end),
            selectedRange = range,
            period = period,
            accentColor = SleepColor,
            // Sleep's own alpha, not the 0.85 default.
            accentAlpha = 0.75f,
            summaryValue = summary,
            dateTimeFormatterProvider = DateTimeFormatterProvider(),
            yearAggregation = yearAggregation,
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            valueFormatter = { "${FORMATTER.decimal(it, 1)}h" },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }

    private companion object {
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
        val WEEK = DatePeriod(LocalDate.of(2026, 6, 16), LocalDate.of(2026, 6, 22))

        /** Hours slept per night: a weekend lie-in, a short Wednesday, and nights with no record. */
        fun sleepHours(date: LocalDate): Double {
            if (date.dayOfMonth % 11 == 0) return 0.0
            val base = if (date.dayOfWeek.value >= 6) 8.4 else 7.0
            return base + ((date.dayOfMonth * 3) % 7) * 0.12
        }

        fun nights(start: LocalDate, end: LocalDate): List<PeriodChartValue> =
            goldenDates(start, end)
                .map { it to sleepHours(it) }
                .filter { (_, hours) -> hours > 0.0 }
                .map { (date, hours) -> PeriodChartValue(date, hours) }
    }
}
