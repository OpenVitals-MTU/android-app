// The charts live under ui/charts/ but declare ui.components.
package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of the chart-level pinch cases of Flutter's `day_chart_zoom_test.dart`,
 * `metric_line_chart_zoom_test.dart`, `session_chart_zoom_test.dart` and
 * `bar_chart_zoom_test.dart`.
 *
 * `ChartZoomGestureTest` proves the gesture reaches a viewport. These prove the
 * chart *wires that viewport into its axis*, which is a separate mistake and a
 * worse one: a plot that zooms while its hour row stays at `00:00 … 24:00`
 * still names every reading, just with the wrong times. It looks right, and it
 * is the elapsed-scaling bug all over again.
 */
class ChartPinchAxisTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun pinchingADayChart_zoomsThePlotAndItsHours() {
        val day = LocalDate.of(2026, 6, 23)
        val points = (0..23).map { hour ->
            MetricLinePoint(
                date = day,
                value = 60.0 + hour,
                time = day.atStartOfDay(ZoneId.systemDefault()).plusHours(hour.toLong()).toInstant(),
            )
        }

        composeRule.setContent {
            OpenVitalsTheme {
                Box(Modifier.testTag(CHART).fillMaxWidth()) {
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
            }
        }

        // At full zoom the row spans the whole day.
        composeRule.onNodeWithText(MIDNIGHT).assertExists()

        composeRule.onNodeWithTag(CHART).performTouchInput { pinchApart() }
        composeRule.waitForIdle()

        // Zoomed about the middle, the visible slice no longer starts at
        // midnight — so neither may the row.
        composeRule.onNodeWithText(MIDNIGHT).assertDoesNotExist()
    }

    @Test
    fun pinchingASessionChart_zoomsTheTraceAndItsElapsedRow() {
        val start = java.time.Instant.parse("2026-06-23T08:00:00Z")
        val axis = SessionAxis(start = start, end = start.plusSeconds(45 * 60))
        val fullLabels = axis.elapsedLabelsFor()

        composeRule.setContent {
            OpenVitalsTheme {
                Box(Modifier.testTag(CHART).fillMaxWidth()) {
                    ChartZoom(axis) { zoom ->
                        ChartXAxisWithYAxis {
                            SessionAxisLabels(axis = axis, viewport = zoom.viewport)
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText(fullLabels.first()).assertExists()

        composeRule.onNodeWithTag(CHART).performTouchInput { pinchApart() }
        composeRule.waitForIdle()

        // The elapsed row is derived from the viewport, so zooming past the
        // start of the session has to take "00:00" off the row with it.
        composeRule.onNodeWithText(fullLabels.first()).assertDoesNotExist()
    }

    @Test
    fun aWeekBarChart_drawsEveryDayUntilItIsPinched() {
        // Unzoomed a week is seven slots and seven dates. Zoomed, the days that
        // have scrolled off must not be drawn at all: a date with no bar under
        // it names nothing, and a row that kept all seven would put every label
        // over the wrong bar.
        val formatter = DateTimeFormatterProvider()
        val days = (0L..6L).map { MONDAY.plusDays(it) }
        val labels = days.map { formatter.chartDayOfMonth().format(it) }

        composeRule.setContent {
            OpenVitalsTheme {
                Box(Modifier.testTag(CHART).fillMaxWidth()) {
                    PeriodBarChart(
                        title = "Steps",
                        values = days.mapIndexed { index, date ->
                            PeriodChartValue(date, (index + 1) * 1_000.0)
                        },
                        selectedRange = TimeRange.WEEK,
                        period = DatePeriod(MONDAY, MONDAY.plusDays(6)),
                        accentColor = ACCENT,
                        summaryText = "28,000",
                        dateTimeFormatterProvider = formatter,
                    )
                }
            }
        }

        labels.forEach { composeRule.onNodeWithText(it).assertExists() }

        composeRule.onNodeWithTag(CHART).performTouchInput { pinchApart() }
        composeRule.waitForIdle()

        // Pinched about the middle of the week, neither end is on the plot any
        // more, so neither end may be on the row.
        composeRule.onNodeWithText(labels.first()).assertDoesNotExist()
        composeRule.onNodeWithText(labels.last()).assertDoesNotExist()
    }

    @Test
    fun pinchingAYearLineChart_zoomsItsDateRowToo() {
        // The year chart pinches like the day one. Unzoomed the date axis is an
        // even row of slots; zoomed it lays its surviving labels over their own
        // slots and drops the rest — so a month at the far end of the year stops
        // being named once it is off the plot.
        val formatter = DateTimeFormatterProvider()
        val year = 2024
        val points = (1..12).map { month ->
            val date = LocalDate.of(year, month, 15)
            MetricLinePoint(
                date = date,
                value = 60.0 + (month % 4) * 3.0,
            )
        }
        val december = formatter.chartMonth().format(LocalDate.of(year, 12, 31))

        composeRule.setContent {
            OpenVitalsTheme {
                Box(Modifier.testTag(CHART).fillMaxWidth()) {
                    MetricLineChart(
                        title = "Resting heart rate",
                        points = points,
                        selectedRange = TimeRange.YEAR,
                        period = DatePeriod(LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31)),
                        dateTimeFormatterProvider = formatter,
                        accentColor = ACCENT,
                        summaryText = "avg 63",
                    )
                }
            }
        }

        assertTrue(
            "precondition: the whole year is on show, so December names a slot",
            composeRule.onAllNodesWithText(december).fetchSemanticsNodes().isNotEmpty(),
        )

        composeRule.onNodeWithTag(CHART).performTouchInput { pinchApart() }
        composeRule.waitForIdle()

        assertTrue(
            "the end of the year has scrolled off the plot, so it must leave the row",
            composeRule.onAllNodesWithText(december).fetchSemanticsNodes().isEmpty(),
        )
    }

    private fun TouchInjectionScope.pinchApart() {
        val left = center - Offset(40f, 0f)
        val right = center + Offset(40f, 0f)
        down(0, left)
        down(1, right)
        repeat(6) { step ->
            val spread = 40f * (step + 1)
            moveTo(0, left - Offset(spread, 0f))
            moveTo(1, right + Offset(spread, 0f))
        }
        up(0)
        up(1)
    }

    private companion object {
        const val CHART = "chart-pinch-under-test"
        const val MIDNIGHT = "00:00"
        val ACCENT = Color(0xFF4CAF50)

        /** A fixed past week, so the labels are the same every run. */
        val MONDAY: LocalDate = LocalDate.of(2026, 7, 13)
    }
}
