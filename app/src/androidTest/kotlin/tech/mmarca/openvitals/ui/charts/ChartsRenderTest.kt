// The chart composables live under ui/charts/ but declare ui.components; this file mirrors that.
package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Smoke tests: each chart composes with sample data, in both themes, without throwing.
 * "It rendered" is asserted through a tagged wrapper. Pixels are the goldens' job.
 */
class ChartsRenderTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sparkline_renders() {
        pump {
            MetricSparklineChart(
                values = listOf(3.0, 7.0, 2.0, 9.0, 4.0, 8.0),
                accentColor = ACCENT,
                modifier = Modifier.size(200.dp, 60.dp),
            )
        }
        composeRule.onNodeWithTag(CHART).assertIsDisplayed()
    }

    @Test
    fun sparkline_withASingleValue_renders() {
        // One point has no line, and the x-step divides by `size - 1`.
        pump {
            MetricSparklineChart(
                values = listOf(5.0),
                accentColor = ACCENT,
                singlePointLine = true,
                modifier = Modifier.size(200.dp, 60.dp),
            )
        }
        composeRule.onNodeWithTag(CHART).assertIsDisplayed()
    }

    @Test
    fun periodBarChart_week_rendersBarsAndAxis() {
        pump {
            PeriodBarChart(
                title = "Steps",
                values = weekValues(),
                selectedRange = TimeRange.WEEK,
                period = WEEK,
                accentColor = ACCENT,
                summaryText = "This week · 15,500",
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
            )
        }

        composeRule.onNodeWithText("Steps").assertIsDisplayed()
        composeRule.onNodeWithText("This week · 15,500").assertIsDisplayed()
    }

    @Test
    fun periodBarChart_tapSelectsADay() {
        var selected: LocalDate? = null

        pump {
            PeriodBarChart(
                title = "Steps",
                values = weekValues(),
                selectedRange = TimeRange.WEEK,
                period = WEEK,
                accentColor = ACCENT,
                summaryText = "week",
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                onDateSelected = { selected = it },
            )
        }

        composeRule.onNodeWithTag(CHART).performClick()
        composeRule.waitForIdle()

        // Which day, not merely some day: a tap in the middle of a fixed week lands on its middle.
        assertEquals(LocalDate.of(2024, 6, 13), selected)
    }

    @Test
    fun periodHistoryChart_month_rendersACalendarHeatmap() {
        pump {
            PeriodHistoryChart(
                title = "Steps",
                values = weekValues(),
                selectedRange = TimeRange.MONTH,
                period = MONTH,
                accentColor = ACCENT,
                summaryText = "This month",
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
            )
        }

        composeRule.onNodeWithText("Steps").assertIsDisplayed()
        composeRule.onNodeWithText("This month").assertIsDisplayed()
    }

    @Test
    fun periodHistoryChart_year_rendersAYearHeatmap() {
        pump {
            PeriodHistoryChart(
                title = "Steps",
                values = weekValues(),
                selectedRange = TimeRange.YEAR,
                period = YEAR,
                accentColor = ACCENT,
                summaryText = "This year",
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
            )
        }

        composeRule.onNodeWithText("Steps").assertIsDisplayed()
        composeRule.onNodeWithText("This year").assertIsDisplayed()
    }

    @Test
    fun metricBarChart_buildsItsSummaryFromThePeriodTitle() {
        pump {
            MetricBarChart(
                title = "Steps",
                values = weekValues(),
                selectedRange = TimeRange.WEEK,
                period = WEEK,
                accentColor = ACCENT,
                summaryValue = "15,500",
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
            )
        }

        composeRule.onNodeWithText("Steps").assertIsDisplayed()
    }

    @Test
    fun metricLineChart_week_rendersLineAndAxis() {
        pump {
            MetricLineChart(
                title = "Resting HR",
                points = weekValues().map { MetricLinePoint(date = it.date, value = it.value) },
                selectedRange = TimeRange.WEEK,
                period = WEEK,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                accentColor = ACCENT,
                summaryText = "avg 58",
            )
        }

        composeRule.onNodeWithText("Resting HR").assertIsDisplayed()
        composeRule.onNodeWithText("avg 58").assertIsDisplayed()
    }

    @Test
    fun metricLineChart_day_rendersATimeAxisWithDistinctTimes() {
        val day = LocalDate.of(2024, 6, 10)
        val points = listOf(6L, 12L, 18L).map { hour ->
            MetricLinePoint(
                date = day,
                value = 60.0 + hour,
                time = day.atStartOfDay(java.time.ZoneId.systemDefault())
                    .plusHours(hour)
                    .toInstant(),
            )
        }

        pump {
            MetricLineChart(
                title = "Heart rate",
                points = points,
                selectedRange = TimeRange.DAY,
                period = DatePeriod(day, day),
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                accentColor = ACCENT,
                summaryText = "avg 72",
            )
        }

        composeRule.onNodeWithText("Heart rate").assertIsDisplayed()
    }

    @Test
    fun metricLineChart_rendersNothingWhenThereAreNoPoints() {
        // An empty series draws nothing, not a titled empty frame. "No data" is the screen's message.
        pump {
            MetricLineChart(
                title = "Empty",
                points = emptyList(),
                selectedRange = TimeRange.WEEK,
                period = WEEK,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                accentColor = ACCENT,
                summaryText = "n/a",
            )
        }

        composeRule.onNodeWithText("Empty").assertDoesNotExist()
        composeRule.onNodeWithText("n/a").assertDoesNotExist()
    }

    @Test
    fun periodChartXAxis_rendersItsLabels() {
        val dates = (0L..6L).map { WEEK.start.plusDays(it) }

        pump {
            PeriodChartXAxis(
                dates = dates,
                selectedRange = TimeRange.WEEK,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
            )
        }

        composeRule.onNodeWithTag(CHART).assertIsDisplayed()
    }

    @Test
    fun charts_renderInDarkTheme() {
        // A dark-only division by a zero alpha would never show up in the light cases.
        composeRule.setContent {
            OpenVitalsTheme(themeMode = AppThemeMode.DARK) {
                Box(Modifier.testTag(CHART).verticalScroll(rememberScrollState())) {
                    PeriodBarChart(
                        title = "Steps",
                        values = weekValues(),
                        selectedRange = TimeRange.WEEK,
                        period = WEEK,
                        accentColor = ACCENT,
                        summaryText = "dark",
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    )
                }
            }
        }

        composeRule.onNodeWithText("Steps").assertIsDisplayed()
        composeRule.onNodeWithText("dark").assertIsDisplayed()
    }

    private fun pump(content: @Composable () -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Box(Modifier.testTag(CHART).verticalScroll(rememberScrollState())) { content() }
            }
        }
    }

    private fun weekValues() = listOf(
        PeriodChartValue(LocalDate.of(2024, 6, 10), 1200.0),
        PeriodChartValue(LocalDate.of(2024, 6, 11), 8400.0),
        PeriodChartValue(LocalDate.of(2024, 6, 13), 300.0),
        PeriodChartValue(LocalDate.of(2024, 6, 16), 5600.0),
    )

    private companion object {
        const val CHART = "chart-under-test"
        val ACCENT = Color(0xFF4CAF50)

        /** A fixed week, month and year, so nothing here depends on today. */
        val WEEK = DatePeriod(LocalDate.of(2024, 6, 10), LocalDate.of(2024, 6, 16))
        val MONTH = DatePeriod(LocalDate.of(2024, 6, 1), LocalDate.of(2024, 6, 30))
        val YEAR = DatePeriod(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31))
    }
}
