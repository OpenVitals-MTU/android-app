package tech.mmarca.openvitals.features.sleep

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.periodFor
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme
import java.time.LocalDate

class SleepScreenWeekTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun sleepWeekView_showsPeriodNavigatorAndWeekContent() {
        val anchorDate = LocalDate.of(2026, 6, 23)
        val period = periodFor(TimeRange.WEEK, anchorDate, today = anchorDate)
        val display = SleepDisplayState(
            durationPoints = listOf(
                SleepDurationPoint(date = anchorDate.minusDays(1), hours = 7.5),
                SleepDurationPoint(date = anchorDate, hours = 8.0),
            ),
        )
        val state = SleepUiState(
            isLoading = false,
            selectedRange = TimeRange.WEEK,
            selectedDate = anchorDate,
            sessions = emptyList(),
            display = display,
        )
        val unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
        val dateTimeFormatterProvider = DateTimeFormatterProvider()
        val chartDaySelection = ChartDaySelection(
            selectedDate = null,
            onDateSelected = {},
        )

        composeRule.setContent {
            OpenVitalsTheme {
                MetricDetailScaffold(
                    isLoading = false,
                    selectedRange = TimeRange.WEEK,
                    selectedDate = anchorDate,
                    onRefresh = {},
                    onSelectRange = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onSelectDate = {},
                ) { period ->
                    sleepPeriodContent(
                        state = state,
                        display = display,
                        period = period,
                        chartDaySelection = chartDaySelection,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onOpenSleepSession = {},
                        onDecreaseGoal = {},
                        onIncreaseGoal = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Week").assertExists()
        composeRule.onNodeWithTag("sleep_week_period_content").assertExists()
    }
}
