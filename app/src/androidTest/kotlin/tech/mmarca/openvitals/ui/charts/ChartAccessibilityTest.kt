// The charts live under ui/charts/ but declare ui.components; mirrored here.
package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.testing.plural
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** What a screen reader gets from a chart: one node per day it can select, a summary, and the zoom moves. */
class ChartAccessibilityTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aBarChart_exposesOneNodePerDay_andSelectsTheDayTapped() {
        val start = LocalDate.of(2026, 9, 14)
        var selected: LocalDate? = null
        composeRule.setContent {
            OpenVitalsTheme {
                PeriodBarChart(
                    title = "Steps",
                    values = (0 until 7).map { PeriodChartValue(start.plusDays(it.toLong()), 1000.0 * (it + 1)) },
                    selectedRange = TimeRange.WEEK,
                    period = DatePeriod(start = start, end = start.plusDays(6)),
                    accentColor = Color.Blue,
                    summaryText = "7 days",
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    selectedDate = selected,
                    onDateSelected = { selected = it },
                )
            }
        }

        val days = composeRule.onAllNodes(hasClickAction() and hasContentDescription("2026", substring = true))
        days.assertCountEquals(7)

        days[2].performClick()
        composeRule.waitForIdle()

        assertEquals(start.plusDays(2), selected)
    }

    @Test
    fun aBarChartDay_isSpokenAsItsDateAndValue_andOnlyTheChosenDayIsSelected() {
        val start = LocalDate.of(2026, 9, 14)
        val formatter = DateTimeFormatterProvider()
        composeRule.setContent {
            OpenVitalsTheme {
                PeriodBarChart(
                    title = "Steps",
                    values = listOf(PeriodChartValue(start, 1234.0), PeriodChartValue(start.plusDays(1), 0.0)),
                    selectedRange = TimeRange.WEEK,
                    period = DatePeriod(start = start, end = start.plusDays(1)),
                    accentColor = Color.Blue,
                    summaryText = "2 days",
                    dateTimeFormatterProvider = formatter,
                    selectedDate = start,
                    onDateSelected = {},
                    valueFormatter = { it.toInt().toString() },
                )
            }
        }

        val spokenDate = formatter.mediumDate()
        composeRule
            .onNodeWithContentDescription("${spokenDate.format(start)}, 1234")
            .assertIsSelected()
        // A day without a value says so rather than reading a zero.
        composeRule
            .onNodeWithContentDescription("${spokenDate.format(start.plusDays(1))}, ${string(R.string.no_data)}")
            .assertExists()
    }

    @Test
    fun aLinePlot_speaksItsTitleAndTheShapeOfTheSeries() {
        composeRule.setContent {
            OpenVitalsTheme {
                MetricLinePlot(
                    points = listOf(
                        MetricLinePlotPoint(0f, 40.0),
                        MetricLinePlotPoint(0.5f, 90.0),
                        MetricLinePlotPoint(1f, 60.0),
                    ),
                    minValue = 0.0,
                    maxValue = 100.0,
                    accentColor = Color.Red,
                    chartHeight = 120.dp,
                    valueFormatter = { it.toInt().toString() },
                    title = "Heart rate",
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Heart rate, ${plural(R.plurals.chart_plot_summary, 3, 3, "40", "90", "60")}")
            .assertExists()
    }

    @Test
    fun chartZoom_offersZoomInThenOutAndReset_asCustomActions() {
        var viewport = ChartViewport.Full
        composeRule.setContent {
            OpenVitalsTheme {
                ChartZoom(modifier = Modifier.testTag(CHART).fillMaxWidth().height(200.dp)) { zoom ->
                    viewport = zoom.viewport
                    Box(Modifier.fillMaxWidth().height(200.dp))
                }
            }
        }

        val chart = composeRule.onNodeWithTag(CHART)
        assertEquals(listOf(string(R.string.chart_zoom_in)), chart.actionLabels())

        chart.perform(string(R.string.chart_zoom_in))
        composeRule.waitForIdle()
        assertTrue("zoom in must narrow the viewport", viewport.isZoomed)
        assertEquals(
            listOf(string(R.string.chart_zoom_in), string(R.string.chart_zoom_out), string(R.string.chart_zoom_reset)),
            chart.actionLabels(),
        )

        chart.perform(string(R.string.chart_zoom_reset))
        composeRule.waitForIdle()
        assertEquals(ChartViewport.Full, viewport)
    }

    @Test
    fun chartScrubber_stepsThroughTheSamples_asCustomActions() {
        var scrubbed: Int? = null
        val targets = listOf(0.1f, 0.5f, 0.9f).map { x ->
            ScrubTarget(xFraction = x, yFraction = 0.5f) { "$x" to null }
        }
        composeRule.setContent {
            OpenVitalsTheme {
                ChartScrubber(
                    targets = targets,
                    accentColor = Color.Red,
                    modifier = Modifier.testTag(CHART).fillMaxWidth().height(200.dp),
                    onScrub = { scrubbed = it },
                ) {
                    Box(Modifier.fillMaxWidth().height(200.dp))
                }
            }
        }

        val chart = composeRule.onNodeWithTag(CHART)
        chart.perform(string(R.string.chart_next_point))
        composeRule.waitForIdle()
        assertEquals(0, scrubbed)

        chart.perform(string(R.string.chart_next_point))
        composeRule.waitForIdle()
        assertEquals(1, scrubbed)

        chart.perform(string(R.string.chart_previous_point))
        composeRule.waitForIdle()
        assertEquals(0, scrubbed)
    }

    private fun androidx.compose.ui.test.SemanticsNodeInteraction.actionLabels(): List<String> =
        fetchSemanticsNode().config[SemanticsActions.CustomActions].map { it.label }

    /** Runs the custom action a screen reader would offer under [label]. */
    private fun androidx.compose.ui.test.SemanticsNodeInteraction.perform(label: String) {
        val action = fetchSemanticsNode().config[SemanticsActions.CustomActions].first { it.label == label }
        composeRule.runOnUiThread { action.action() }
    }

    private companion object {
        const val CHART = "chart"
    }
}
