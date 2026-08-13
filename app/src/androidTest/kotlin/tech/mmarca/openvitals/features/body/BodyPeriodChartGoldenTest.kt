package tech.mmarca.openvitals.features.body

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.roundToInt
import kotlin.math.sin
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.theme.WeightColor

/**
 * Weight over a week, a month and a year.
 *
 * Month and year used to be drawn by the calendar heatmap, which colours a day
 * by how big its number is. That reads well for a quantity that can be zero and
 * can be huge — steps, minutes — and reads as nothing at all for one that
 * spends its life inside a two-kilo band: forty dots of near-identical colour,
 * answering "did you weigh yourself on the 14th" while hiding which way the
 * line is going.
 *
 * Photographed rather than asserted because "the trend is legible" is not a
 * property any assertion holds. A test that the chart contains 31 points passes
 * just as happily when they are all flattened onto an axis that starts at zero.
 */
class BodyPeriodChartGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun week_theShapeOfSevenDays() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 393.dp, height = 320.dp) {
                Chart(
                    range = TimeRange.WEEK,
                    period = DatePeriod(MONDAY, MONDAY.plusDays(6)),
                    values = weights(from = MONDAY, days = 7, everyNthDay = 1),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("body_period_chart_week")
    }

    @Test
    fun month_aTrendWhereThereWasACalendar() {
        // Weighed most mornings but not all: the gaps are what the heatmap was
        // good at showing and the line has to survive.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 393.dp, height = 320.dp) {
                Chart(
                    range = TimeRange.MONTH,
                    period = DatePeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31)),
                    values = weights(from = LocalDate.of(2026, 5, 1), days = 31, everyNthDay = 2),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("body_period_chart_month")
    }

    @Test
    fun year_aYearOfWeighIns() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 393.dp, height = 320.dp) {
                Chart(
                    range = TimeRange.YEAR,
                    period = DatePeriod(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)),
                    values = weights(from = LocalDate.of(2026, 1, 1), days = 365, everyNthDay = 7),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("body_period_chart_year")
    }

    @androidx.compose.runtime.Composable
    private fun Chart(
        range: TimeRange,
        period: DatePeriod,
        values: List<PeriodChartValue>,
    ) {
        BodyPeriodMetricChart(
            metricData = BodyMetricData(
                metric = BodyMetric.WEIGHT,
                titleRes = R.string.metric_weight,
                latest = DisplayValue(
                    value = "%.1f".format(values.last().value),
                    unit = "kg",
                ),
                values = values,
                color = WeightColor,
                icon = Icons.Outlined.MonitorWeight,
                valueDisplayFormatter = { DisplayValue("%.1f".format(it), "kg") },
            ),
            selectedRange = range,
            period = period,
            dateTimeFormatterProvider = DateTimeFormatterProvider(),
            selectedDate = null,
            onDateSelected = {},
        )
    }

    /**
     * A slow slide with the daily noise a real scale produces — the signal the
     * heatmap could not show, and the reason the axis must not start at zero.
     */
    private fun weights(from: LocalDate, days: Int, everyNthDay: Int): List<PeriodChartValue> =
        (0 until days step everyNthDay).map { day ->
            val drift = START_KG - day * DRIFT_KG_PER_DAY
            val noise = sin(day * 0.45) * 0.3 + sin(day * 0.13) * 0.25
            PeriodChartValue(
                date = from.plusDays(day.toLong()),
                value = ((drift + noise) * 10).roundToInt() / 10.0,
            )
        }

    private companion object {
        // A fixed week, never `LocalDate.now()`: a golden that moves with the
        // calendar draws a different picture every day the suite runs.
        val MONDAY: LocalDate = LocalDate.of(2026, 5, 4)
        const val START_KG = 78.4
        const val DRIFT_KG_PER_DAY = 0.012
    }
}
