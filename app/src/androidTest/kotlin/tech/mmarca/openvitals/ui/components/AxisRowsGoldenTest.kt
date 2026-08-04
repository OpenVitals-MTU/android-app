// The chart composables live under ui/charts/ but declare ui.components; this
// file mirrors that rather than adding an import that looks like a mistake.
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
 * Port of Flutter's `test/goldens/charts/axis_rows_golden_test.dart`.
 *
 * The axis rows, photographed WITHOUT a chart on top of them. Every one is a strip
 * of text that only means something in relation to the plot above it, which is why
 * they are worth shooting alone: the bug they exist to prevent is a row that is
 * internally perfect and lines up with nothing.
 *
 * Kotlin says "inset" differently from Flutter: there is no `inset` parameter, the
 * row is wrapped in [ChartXAxisWithYAxis] when the plot above has a y-axis gutter
 * and left bare when it does not. Both conventions are shot, same as Flutter.
 */
class AxisRowsGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun yAxisColumn_matchesGolden() {
        // Three labels, top-to-bottom: max, mid, min. `chartYAxisLabels` steps up to a
        // finer format when the compact one would print the same string twice.
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
        // The body-energy strip's case: the plot starts at the card's edge, so the row
        // does too — no [ChartXAxisWithYAxis] wrapper.
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
        // A 1h 12m session, so the quarter labels are 0:00 / 18:00 / 36:00 / 54:00 /
        // 1:12:00 — the point at which `formatElapsedChartLabel` starts printing an
        // hour field and the row's five labels stop being the same width.
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
        // Twelve buckets, so `isPeriodChartLabelVisible` shows all of them; a year fed
        // raw DAYS instead would thin to every thirtieth.
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

/**
 * A plot that draws nothing but [drawYAxisGuides] — the shared primitive every real
 * painter opens with. The axis rows need SOMETHING above them to be aligned against,
 * and the real painters are all private to their charts.
 */
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
