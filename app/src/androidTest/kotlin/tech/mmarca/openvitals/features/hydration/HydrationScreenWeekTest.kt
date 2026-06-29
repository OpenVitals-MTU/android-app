package tech.mmarca.openvitals.features.hydration

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme
import java.time.LocalDate

class HydrationScreenWeekTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hydrationWeekView_showsPeriodNavigatorAndWeekContent() {
        val anchorDate = LocalDate.of(2026, 6, 23)
        val display = HydrationDisplayState(
            hasData = true,
            summary = HydrationPeriodSummary(totalLiters = 12.5, trackedDays = 7),
        )
        val state = HydrationUiState(
            isLoading = false,
            selectedRange = TimeRange.WEEK,
            selectedDate = anchorDate,
            dailyHydration = listOf(
                DailyHydration(date = anchorDate.minusDays(1), liters = 1.8),
                DailyHydration(date = anchorDate, liters = 2.1),
            ),
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
                    hydrationPeriodContent(
                        state = state,
                        period = period,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        chartDaySelection = chartDaySelection,
                        hasNotificationPermission = true,
                        onDecreaseGoal = {},
                        onIncreaseGoal = {},
                        onToggleReminders = {},
                        onRequestNotificationPermission = {},
                        onDecreaseInterval = {},
                        onIncreaseInterval = {},
                        onSelectActiveStartTime = {},
                        onSelectActiveEndTime = {},
                        onEditHydrationEntry = {},
                        onDeleteHydrationEntry = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Week").assertExists()
        composeRule.onNodeWithTag("hydration_week_period_content").assertExists()
    }
}
