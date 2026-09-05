// The chart composables live under ui/charts/ but declare ui.components; this file mirrors that.
package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.testing.goldenDates
import tech.mmarca.openvitals.testing.goldenInstant

/**
 * The axis rows, photographed without a chart on top. The bug they prevent is a row that is
 * internally perfect and lines up with nothing. A row is wrapped in [ChartXAxisWithYAxis]
 * when the plot has a y-axis gutter and left bare when it does not; both are shot.
 */
class AxisRowsGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun yAxisColumn_matchesGolden() {
        // Three labels: max, mid, min. `chartYAxisLabels` steps up to a finer format to avoid duplicates.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 180.dp) {
                GuideYAxisChart()
            }
        }

        composeRule.assertVisualRootMatchesGolden("axis_y_column")
    }

    @Test
    fun dayLabelsUnderAPlotWithAYAxis_matchesGolden() {
        // 12:00 must land halfway across the PLOT, not halfway across the card.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 200.dp) {
                Column(Modifier.fillMaxWidth()) {
                    GuideYAxisChart()
                    Spacer(Modifier.height(8.dp))
                    ChartXAxisWithYAxis { DayAxisLabels() }
                }
            }
        }

        composeRule.assertVisualRootMatchesGolden("axis_day_labels_inset")
    }

    @Test
    fun dayLabelsUnderAPlotWithNoYAxis_matchesGolden() {
        // The body-energy strip's case: the plot starts at the card's edge, so no wrapper.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 200.dp) {
                Column(Modifier.fillMaxWidth()) {
                    GuidePlot()
                    Spacer(Modifier.height(8.dp))
                    DayAxisLabels()
                }
            }
        }

        composeRule.assertVisualRootMatchesGolden("axis_day_labels_inset0")
    }

    @Test
    fun sessionElapsedLabels_insetAndNot_matchGolden() {
        // A 1h 12m session, where `formatElapsedChartLabel` starts printing an hour field.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 100.dp) {
                val axis = SessionAxis(
                    start = goldenInstant(2026, 6, 22, 9, 0),
                    end = goldenInstant(2026, 6, 22, 10, 12),
                )
                Column(Modifier.fillMaxWidth()) {
                    ChartXAxisWithYAxis { SessionAxisLabels(axis = axis) }
                    Spacer(Modifier.height(16.dp))
                    SessionAxisLabels(axis = axis)
                }
            }
        }

        composeRule.assertVisualRootMatchesGolden("axis_session_labels")
    }

    @Test
    fun periodAxis_week_keepsEveryLabel() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 40.dp) {
                PeriodChartXAxis(
                    dates = datesBetween(LocalDate.of(2026, 6, 16), LocalDate.of(2026, 6, 22)),
                    selectedRange = TimeRange.WEEK,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("axis_period_week")
    }

    @Test
    fun periodAxis_month_dropsAllButEveryFifth() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 40.dp) {
                PeriodChartXAxis(
                    dates = datesBetween(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30)),
                    selectedRange = TimeRange.MONTH,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("axis_period_month")
    }

    @Test
    fun periodAxis_year_keepsTheTwelveMonthNames() {
        // Twelve buckets, so every label shows; a year fed raw days would thin to every thirtieth.
        val months = (7..12).map { LocalDate.of(2025, it, 1) } +
            (1..6).map { LocalDate.of(2026, it, 1) }

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 40.dp) {
                PeriodChartXAxis(
                    dates = months,
                    selectedRange = TimeRange.YEAR,
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("axis_period_year")
    }
}

/** A plot that draws only [drawYAxisGuides], so the rows have something to align against. */
@androidx.compose.runtime.Composable
private fun GuidePlot() {
    val gridColor = ChartTokens.grid(MaterialTheme.colorScheme.primary)
    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(GuidePlotHeight),
    ) {
        drawYAxisGuides(gridColor = gridColor, axisColor = axisColor, strokeWidth = 1.dp.toPx())
    }
}

@androidx.compose.runtime.Composable
private fun GuideYAxisChart() {
    val gridColor = ChartTokens.grid(MaterialTheme.colorScheme.primary)
    val axisColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.8f)
    YAxisChart(
        labels = chartYAxisLabels(minValue = 0.0, maxValue = 11_200.0),
        chartHeight = GuidePlotHeight,
    ) {
        drawYAxisGuides(gridColor = gridColor, axisColor = axisColor, strokeWidth = 1.dp.toPx())
    }
}

private val GuidePlotHeight = 150.dp

private fun datesBetween(from: LocalDate, to: LocalDate): List<LocalDate> =
    goldenDates(from, to)
