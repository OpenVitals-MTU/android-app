package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.testing.goldenInstantAt
import tech.mmarca.openvitals.ui.components.ChartRange
import tech.mmarca.openvitals.ui.components.ChartTokens
import tech.mmarca.openvitals.ui.components.ChartXAxisWithYAxis
import tech.mmarca.openvitals.ui.components.DayAxisLabels
import tech.mmarca.openvitals.ui.components.DayTimelineLinePlot
import tech.mmarca.openvitals.ui.components.MetricLinePlot
import tech.mmarca.openvitals.ui.components.cumulativeDayPlotPoints
import tech.mmarca.openvitals.ui.theme.HydrationColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.WeightColor

/**
 * Kotlin split the day chart in two: [IntradayActivityChartCard] for a running total and
 * [DayTimelineLinePlot] for raw readings. The maths is unit-tested; this proves it reaches the screen.
 */
class MetricDayChartGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun cumulative_aDayThatIsOver() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 400.dp) {
                IntradayActivityChartCard(
                    selectedDate = DAY,
                    title = "Steps",
                    valueText = "11,200",
                    emptyText = "steps",
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    points = STEPS,
                    accentColor = StepsColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("metric_day_chart_cumulative")
    }

    @Test
    fun cumulative_aDayThatHasNotFinishedHappening() {
        // The card reads the wall clock, so the plot takes the end fraction directly:
        // the line stops at 14:30 because the rest of the day has not happened.
        val endFraction = (14 * 60 + 30) / (24f * 60f)
        val fractions = STEPS.take(4).map { (time, value) -> dayFraction(time) to value }

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 240.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                ) {
                    MetricLinePlot(
                        points = cumulativeDayPlotPoints(fractions, endFraction),
                        minValue = 0.0,
                        maxValue = 11_200.0,
                        accentColor = StepsColor,
                        chartHeight = ChartTokens.heightDay,
                        valueFormatter = { it.toInt().toString() },
                        lineStrokeWidth = 3.dp,
                        drawPoints = false,
                    )
                    Spacer(Modifier.height(8.dp))
                    ChartXAxisWithYAxis { DayAxisLabels() }
                }
            }
        }

        composeRule.assertVisualRootMatchesGolden("metric_day_chart_cumulative_partial")
    }

    @Test
    fun rawReadings_plottedWhereTheyWereTaken() {
        // A weight at 06:00: no zero anchor, no trailing hold, axis padded around the data.
        val weights = listOf(
            goldenInstantAt(7) to 74.2,
            goldenInstantAt(13) to 74.6,
            goldenInstantAt(21) to 74.1,
        )
        val range = ChartRange.padded(weights.map { it.second })
        val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 240.dp) {
                DayTimelineLinePlot(
                    samples = weights,
                    dayStart = DAY_START,
                    dayEnd = DAY_END,
                    minValue = range.min,
                    maxValue = range.max,
                    accentColor = WeightColor,
                    valueFormatter = { String.format(java.util.Locale.US, "%.1f", it) },
                    timeLabel = { timeFormatter.format(it.atZone(ZONE)) },
                    time = { it.first },
                    value = { it.second },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("metric_day_chart_raw")
    }

    @Test
    fun aDayWithNothingInIt() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 260.dp) {
                IntradayActivityChartCard(
                    selectedDate = DAY,
                    title = "Water",
                    valueText = "0 L",
                    emptyText = "water",
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    points = emptyList(),
                    accentColor = HydrationColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("metric_day_chart_empty")
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.systemDefault()
        val DAY: LocalDate = LocalDate.of(2026, 6, 22)
        val DAY_START: Instant = DAY.atStartOfDay(ZONE).toInstant()
        val DAY_END: Instant = DAY.plusDays(1).atStartOfDay(ZONE).toInstant()

        val STEPS = listOf(
            goldenInstantAt(7) to 800.0,
            goldenInstantAt(9) to 2_400.0,
            goldenInstantAt(12) to 5_100.0,
            goldenInstantAt(14) to 7_300.0,
            goldenInstantAt(18) to 9_600.0,
            goldenInstantAt(21) to 11_200.0,
        )

        fun dayFraction(time: Instant): Float {
            val span = DAY_END.toEpochMilli() - DAY_START.toEpochMilli()
            return (time.toEpochMilli() - DAY_START.toEpochMilli()).toFloat() / span
        }
    }
}
