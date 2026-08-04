// The chart composables live under ui/charts/ but declare ui.components; this
// file mirrors that rather than adding an import that looks like a mistake.
package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.ui.theme.HeartColor

/**
 * Port of Flutter's `test/goldens/charts/metric_line_chart_golden_test.dart`.
 *
 * [MetricLineChart] — the heart screen's chart, and the only one with a legend. It
 * pads its own y axis (8% of the span), so unlike the bar charts the caller hands it
 * no range at all, which means the padding rule is only ever visible in a picture.
 * Two series is not a cosmetic variant either: the legend row exists only when more
 * than one series survives the period filter.
 */
class MetricLineChartGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun oneSeries_noLegend() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 340.dp) {
                HeartLineChart(
                    title = "Resting heart rate",
                    series = listOf(MetricLineSeries(restingPoints(), HeartColor)),
                    summary = "This week · 55 bpm avg (52-58 bpm)",
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("metric_line_chart_single")
    }

    @Test
    fun twoSeries_andTheLegendThatComesWithThem() {
        // The day-average line against its daily low, minus the third (highest) line,
        // so the legend stays two columns wide and the y padding has a real spread.
        val averageBpm = listOf(72, 74, 71, 78, 80, 73, 69)

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 380.dp) {
                HeartLineChart(
                    title = "Average heart rate",
                    series = listOf(
                        MetricLineSeries(pointsFrom(averageBpm), HeartColor, "Average"),
                        // The dimmer sibling: same hue, less weight, so the eye reads
                        // the average as the subject and the low as its floor.
                        MetricLineSeries(restingPoints(), HeartColor.copy(alpha = 0.55f), "Lowest"),
                    ),
                    summary = "This week · 74 bpm avg (52-80 bpm)",
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("metric_line_chart_two_series")
    }

    @Test
    fun twoSeries_withADaySelected() {
        // Selection is a full-height column wash behind the lines, not a bar
        // highlight — the only chart that draws it this way.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 380.dp) {
                HeartLineChart(
                    title = "Resting heart rate",
                    series = listOf(
                        MetricLineSeries(restingPoints(), HeartColor, "Resting"),
                        MetricLineSeries(
                            pointsFrom(RESTING_BPM.map { it + 12 }),
                            HeartColor.copy(alpha = 0.9f),
                            "Sleeping",
                        ),
                    ),
                    summary = "This week · 55 bpm avg (52-58 bpm)",
                    selectedDate = LocalDate.of(2026, 6, 19),
                    onDateSelected = {},
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("metric_line_chart_selected")
    }

    @Composable
    private fun HeartLineChart(
        title: String,
        series: List<MetricLineSeries>,
        summary: String,
        selectedDate: LocalDate? = null,
        onDateSelected: ((LocalDate) -> Unit)? = null,
    ) {
        MetricLineChart(
            title = title,
            series = series,
            selectedRange = TimeRange.WEEK,
            period = WEEK,
            accentColor = HeartColor,
            summaryText = summary,
            dateTimeFormatterProvider = DateTimeFormatterProvider(),
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            valueFormatter = { "${it.toInt()} bpm" },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }

    private companion object {
        val WEEK = DatePeriod(LocalDate.of(2026, 6, 16), LocalDate.of(2026, 6, 22))

        // Resting heart rate over the week, one reading a night. Real RHR moves in a
        // band of a few beats, which is exactly the case the 8% padding exists for: a
        // zero-based axis would draw this as a flat line along the top of the card.
        val RESTING_BPM = listOf(54, 55, 53, 57, 58, 55, 52)

        fun pointsFrom(values: List<Int>): List<MetricLinePoint> =
            values.mapIndexed { index, bpm ->
                MetricLinePoint(date = WEEK.start.plusDays(index.toLong()), value = bpm.toDouble())
            }

        fun restingPoints(): List<MetricLinePoint> = pointsFrom(RESTING_BPM)
    }
}
