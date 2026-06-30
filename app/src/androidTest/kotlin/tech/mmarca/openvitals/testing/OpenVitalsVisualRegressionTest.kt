package tech.mmarca.openvitals.testing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.features.dashboard.DashboardContent
import tech.mmarca.openvitals.features.dashboard.DashboardDailyGoals
import tech.mmarca.openvitals.features.dashboard.DashboardPresentationMapper
import tech.mmarca.openvitals.features.hydration.HydrationDisplayState
import tech.mmarca.openvitals.features.hydration.HydrationPeriodSummary
import tech.mmarca.openvitals.features.hydration.HydrationUiState
import tech.mmarca.openvitals.features.hydration.hydrationPeriodContent
import tech.mmarca.openvitals.features.manualentry.ManualEntryWidgetGrid
import tech.mmarca.openvitals.features.manualentry.ManualEntryWidgetId
import tech.mmarca.openvitals.features.manualentry.manualEntryWidgetSpecs
import tech.mmarca.openvitals.features.settings.SettingsCategoryCard
import tech.mmarca.openvitals.features.settings.SettingsSection
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import java.time.LocalDate

class OpenVitalsVisualRegressionTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboard_matchesCurrentBaseline() {
        val unitFormatter = testUnitFormatter()
        val dateTimeFormatterProvider = DateTimeFormatterProvider()
        val data = dashboardFixtureData()
        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = DashboardDailyGoals(),
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )

        composeRule.setContent {
            OpenVitalsVisualTestSurface {
                DashboardContent(
                    data = data,
                    display = display,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    canGoForward = false,
                    unacknowledgedWidgetPermissions = emptySet(),
                    showHealthConnectPromo = false,
                    healthConnectAvailability = HealthConnectAvailability.AVAILABLE,
                    healthConnectSyncEnabled = true,
                    dashboardWidgets = dashboardFlowWidgetIds,
                    isEditingDashboard = false,
                    onPreviousDay = {},
                    onNextDay = {},
                    onOpenCalendar = {},
                    onGrantWidgetPermissions = {},
                    onDismissWidgetPermissions = {},
                    onMoveWidgetToTarget = { _, _ -> },
                    onRemoveWidget = {},
                    onAddWidget = {},
                    onOpenMetric = {},
                    onOpenActivities = {},
                    onOpenActivity = {},
                    onEditActivity = {},
                    onDeleteActivity = {},
                    onOpenLog = {},
                    onStartActivity = {},
                    onToggleDashboardEdit = {},
                    onHealthConnectPromoAction = {},
                )
            }
        }

        composeRule.assertVisualRootMatchesGolden("dashboard_current")
    }

    @Test
    fun manualEntryGrid_matchesCurrentBaseline() {
        val visibleIds = listOf(
            ManualEntryWidgetId.HYDRATION,
            ManualEntryWidgetId.CARBS,
            ManualEntryWidgetId.ACTIVITY,
            ManualEntryWidgetId.MINDFULNESS,
            ManualEntryWidgetId.WEIGHT,
            ManualEntryWidgetId.HEIGHT,
            ManualEntryWidgetId.BODY_FAT,
            ManualEntryWidgetId.BLOOD_PRESSURE,
        )

        composeRule.setContent {
            OpenVitalsVisualTestSurface(height = 520.dp) {
                val specs = manualEntryWidgetSpecs(
                    isEditingWidgets = false,
                    onOpenHydrationEntry = {},
                    onOpenCarbsEntry = {},
                    onOpenActivityEntry = {},
                    onOpenMindfulnessEntry = {},
                    onOpenBodyMeasurementEntry = {},
                    onOpenVitalsMeasurementEntry = {},
                )
                Column(Modifier.fillMaxSize()) {
                    ManualEntryWidgetGrid(
                        visibleIds = visibleIds,
                        specsById = specs.associateBy { it.id },
                        isEditingWidgets = false,
                        onMoveWidgetToTarget = { _, _ -> },
                        onRemoveWidget = {},
                    )
                }
            }
        }

        composeRule.assertVisualRootMatchesGolden("manual_entry_grid_current")
    }

    @Test
    fun hydrationWeekDetail_matchesCurrentBaseline() {
        val anchorDate = LocalDate.of(2026, 6, 23)
        val unitFormatter = testUnitFormatter()
        val dateTimeFormatterProvider = DateTimeFormatterProvider()
        val chartDaySelection = ChartDaySelection(
            selectedDate = null,
            onDateSelected = {},
        )
        val state = HydrationUiState(
            isLoading = false,
            selectedRange = TimeRange.WEEK,
            selectedDate = anchorDate,
            dailyHydration = listOf(
                DailyHydration(date = anchorDate.minusDays(2), liters = 1.6),
                DailyHydration(date = anchorDate.minusDays(1), liters = 2.0),
                DailyHydration(date = anchorDate, liters = 2.2),
            ),
            display = HydrationDisplayState(
                hasData = true,
                summary = HydrationPeriodSummary(totalLiters = 12.8, trackedDays = 7),
            ),
        )

        composeRule.setContent {
            OpenVitalsVisualTestSurface {
                val sectionContext = MetricDetailSectionContext(
                    listState = rememberMetricDetailSectionListState(),
                    order = DefaultMetricDetailSectionOrder,
                    isEditingSections = false,
                    onMoveSectionToTarget = { _, _ -> },
                    onMoveSection = { _, _ -> },
                )

                MetricDetailScaffold(
                    isLoading = false,
                    selectedRange = TimeRange.WEEK,
                    selectedDate = anchorDate,
                    onRefresh = {},
                    onSelectRange = {},
                    onPreviousPeriod = {},
                    onNextPeriod = {},
                    onSelectDate = {},
                    sectionListState = sectionContext.listState,
                ) { period ->
                    hydrationPeriodContent(
                        sectionContext = sectionContext,
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

        composeRule.assertVisualRootMatchesGolden("hydration_week_detail_current")
    }

    @Test
    fun settingsCategories_matchesCurrentBaseline() {
        composeRule.setContent {
            OpenVitalsVisualTestSurface(height = 760.dp) {
                LazyColumn {
                    SettingsSection.entries.forEach { section ->
                        item {
                            SettingsCategoryCard(
                                section = section,
                                onClick = {},
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        composeRule.assertVisualRootMatchesGolden("settings_categories_current")
    }
}
