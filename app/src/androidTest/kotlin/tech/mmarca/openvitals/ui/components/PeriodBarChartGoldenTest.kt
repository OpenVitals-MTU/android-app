// The chart composables live under ui/charts/ but declare ui.components; this file mirrors that.
package tech.mmarca.openvitals.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.OpenVitalsVisualTestSurface
import tech.mmarca.openvitals.testing.assertVisualRootMatchesGolden
import tech.mmarca.openvitals.ui.theme.MindfulnessColor

/**
 * [PeriodBarChart]: the bars and the value labels on them. `measureBarLabelLines` drops a
 * label whole when it will not fit, and the bar reserves height for it.
 */
class PeriodBarChartGoldenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun week_sevenSlotsAndANumberOnEveryBar() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 300.dp) {
                MindfulnessWeekBars()
            }
        }

        composeRule.assertVisualRootMatchesGolden("period_bar_chart_week")
    }

    @Test
    fun week_withADaySelected() {
        // The selection highlight is only drawn when the chart can be tapped.
        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 300.dp) {
                MindfulnessWeekBars(
                    selectedDate = LocalDate.of(2026, 6, 20),
                    onDateSelected = {},
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("period_bar_chart_week_selected")
    }

    @Test
    fun month_thirtySlotsTooNarrowForALabelToSurvive() {
        // A deterministic sawtooth, not a random walk.
        val month = DatePeriod(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30))
        val values = (1..30).map { day ->
            PeriodChartValue(
                date = LocalDate.of(2026, 6, day),
                value = if (day % 4 == 0) 0.0 else (10 + (day * 7) % 40).toDouble(),
            )
        }

        composeRule.setContent {
            OpenVitalsVisualTestSurface(width = 360.dp, height = 300.dp) {
                PeriodBarChart(
                    title = "Mindfulness",
                    values = values,
                    selectedRange = TimeRange.MONTH,
                    period = month,
                    accentColor = MindfulnessColor,
                    summaryText = "June · 11h 05m",
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    valueFormatter = { FORMATTER.minutes(it.toLong()).text },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("period_bar_chart_month")
    }

    @androidx.compose.runtime.Composable
    private fun MindfulnessWeekBars(
        selectedDate: LocalDate? = null,
        onDateSelected: ((LocalDate) -> Unit)? = null,
    ) {
        PeriodBarChart(
            title = "Mindfulness",
            values = WEEK_MINUTES,
            selectedRange = TimeRange.WEEK,
            period = WEEK,
            accentColor = MindfulnessColor,
            summaryText = "This week · 2h 30m",
            dateTimeFormatterProvider = DateTimeFormatterProvider(),
            selectedDate = selectedDate,
            onDateSelected = onDateSelected,
            valueFormatter = { FORMATTER.minutes(it.toLong()).text },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }

    private companion object {
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        // A meditation week in minutes: "20 min" is the two-token string `splitBarValueLabel` breaks over two lines.
        val WEEK = DatePeriod(LocalDate.of(2026, 6, 16), LocalDate.of(2026, 6, 22))

        // Wednesday was missed. A zero bucket is drawn as nothing, not a stub.
        val WEEK_MINUTES = listOf(
            PeriodChartValue(LocalDate.of(2026, 6, 16), 20.0),
            PeriodChartValue(LocalDate.of(2026, 6, 17), 35.0),
            PeriodChartValue(LocalDate.of(2026, 6, 19), 15.0),
            PeriodChartValue(LocalDate.of(2026, 6, 20), 45.0),
            PeriodChartValue(LocalDate.of(2026, 6, 21), 10.0),
            PeriodChartValue(LocalDate.of(2026, 6, 22), 25.0),
        )
    }
}
