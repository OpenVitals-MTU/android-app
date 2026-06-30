package tech.mmarca.openvitals.testing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.IntensityMinutesConfidence
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoadTargetSource
import tech.mmarca.openvitals.domain.model.DashboardWeeklyIntensityMinutes
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.features.dashboard.DashboardContent
import tech.mmarca.openvitals.features.dashboard.DashboardDailyGoals
import tech.mmarca.openvitals.features.dashboard.DashboardPresentationMapper
import tech.mmarca.openvitals.features.dashboard.DashboardWidgetId
import tech.mmarca.openvitals.features.manualentry.ManualEntryWidgetGrid
import tech.mmarca.openvitals.features.manualentry.ManualEntryWidgetId
import tech.mmarca.openvitals.features.manualentry.manualEntryWidgetSpecs
import tech.mmarca.openvitals.features.settings.SettingsCategoryCard
import tech.mmarca.openvitals.features.settings.SettingsSection
import tech.mmarca.openvitals.ui.components.MetricAction
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.OpenVitalsAdaptiveScaffold
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme
import java.time.LocalDate
import java.time.ZoneOffset

class OpenVitalsConnectedFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboardPrimaryFlow_opensLogStartEditAndMetricCards() {
        val unitFormatter = testUnitFormatter()
        val dateTimeFormatterProvider = DateTimeFormatterProvider()
        val data = dashboardFixtureData()
        val display = DashboardPresentationMapper.build(
            data = data,
            dailyGoals = DashboardDailyGoals(),
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
        var openedMetric: DashboardWidgetId? = null
        var openLogCount = 0
        var startActivityCount = 0
        var editToggleCount = 0

        composeRule.setContent {
            OpenVitalsTheme {
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
                    visibleWidgetLoadToken = 0L,
                    isEditingDashboard = false,
                    onPreviousDay = {},
                    onNextDay = {},
                    onOpenCalendar = {},
                    onGrantWidgetPermissions = {},
                    onDismissWidgetPermissions = {},
                    onMoveWidgetToTarget = { _, _ -> },
                    onRemoveWidget = {},
                    onAddWidget = {},
                    onVisibleWidgetsChanged = {},
                    onOpenMetric = { openedMetric = it },
                    onOpenActivities = {},
                    onOpenActivity = {},
                    onEditActivity = {},
                    onDeleteActivity = {},
                    onOpenLog = { openLogCount += 1 },
                    onStartActivity = { startActivityCount += 1 },
                    onToggleDashboardEdit = { editToggleCount += 1 },
                    onHealthConnectPromoAction = {},
                )
            }
        }

        composeRule.onNodeWithText("Log").performClick()
        composeRule.onNodeWithText("Start").performClick()
        composeRule.onNodeWithContentDescription("Edit summary").performClick()
        composeRule.onAllNodesWithText("Steps").onFirst().performClick()

        composeRule.runOnIdle {
            assertEquals(1, openLogCount)
            assertEquals(1, startActivityCount)
            assertEquals(1, editToggleCount)
            assertEquals(DashboardWidgetId.STEPS, openedMetric)
        }
    }

    @Test
    fun manualEntryWidgetFlow_routesVisibleTilesAndEditControls() {
        var opened = ""
        var isEditingWidgets by mutableStateOf(false)
        val removed = mutableListOf<ManualEntryWidgetId>()
        val visibleIds = listOf(
            ManualEntryWidgetId.HYDRATION,
            ManualEntryWidgetId.CARBS,
            ManualEntryWidgetId.ACTIVITY,
            ManualEntryWidgetId.MINDFULNESS,
            ManualEntryWidgetId.WEIGHT,
            ManualEntryWidgetId.BLOOD_PRESSURE,
        )

        composeRule.setContent {
            OpenVitalsTheme {
                val specs = manualEntryWidgetSpecs(
                    isEditingWidgets = isEditingWidgets,
                    onOpenHydrationEntry = { opened = "hydration" },
                    onOpenCarbsEntry = { opened = "carbs" },
                    onOpenActivityEntry = { opened = "activity" },
                    onOpenMindfulnessEntry = { opened = "mindfulness" },
                    onOpenBodyMeasurementEntry = { opened = it.name },
                    onOpenVitalsMeasurementEntry = { opened = it.name },
                )
                ManualEntryWidgetGrid(
                    visibleIds = visibleIds,
                    specsById = specs.associateBy { it.id },
                    isEditingWidgets = isEditingWidgets,
                    onMoveWidgetToTarget = { _, _ -> },
                    onRemoveWidget = { removed += it },
                )
            }
        }

        composeRule.onNodeWithText("Hydration").performClick()
        composeRule.runOnIdle { assertEquals("hydration", opened) }

        composeRule.onNodeWithText("Carbs").performClick()
        composeRule.runOnIdle { assertEquals("carbs", opened) }

        composeRule.onNodeWithText("Activity").performClick()
        composeRule.runOnIdle { assertEquals("activity", opened) }

        composeRule.onNodeWithText("Weight").performClick()
        composeRule.runOnIdle { assertEquals("WEIGHT", opened) }

        composeRule.runOnIdle { isEditingWidgets = true }
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("Hydration").onFirst().assertExists()
        composeRule.runOnIdle { assertTrue(removed.isEmpty()) }
    }

    @Test
    fun metricDetailScaffoldFlow_coversPeriodNavigationAndTopBarActions() {
        var selectedRange by mutableStateOf(TimeRange.WEEK)
        var previousCount = 0
        var nextCount = 0
        var refreshCount = 0
        var addCount = 0
        var navigatedRoute: String? = null
        val anchorDate = LocalDate.now().minusWeeks(2)

        composeRule.setContent {
            OpenVitalsTheme {
                OpenVitalsAdaptiveScaffold(
                    title = "Steps",
                    navigationDestinations = emptyList(),
                    currentRoute = null,
                    showTopBar = true,
                    showNavigation = false,
                    canNavigateBack = true,
                    onNavigateBack = { navigatedRoute = "back" },
                    onNavigate = { navigatedRoute = it },
                    navigationIcon = Icons.AutoMirrored.Outlined.ArrowBack,
                    navigationContentDescription = "Back",
                    action = MetricAction(
                        labelRes = R.string.action_add,
                        icon = Icons.Outlined.Add,
                        onClick = { addCount += 1 },
                    ),
                ) {
                    MetricDetailScaffold(
                        isLoading = false,
                        selectedRange = selectedRange,
                        selectedDate = anchorDate,
                        onRefresh = { refreshCount += 1 },
                        onSelectRange = { selectedRange = it },
                        onPreviousPeriod = { previousCount += 1 },
                        onNextPeriod = { nextCount += 1 },
                        onSelectDate = {},
                    ) { period ->
                        item {
                            Text("Period ${period.start} - ${period.end}")
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithContentDescription("Previous period").performClick()
        composeRule.onNodeWithContentDescription("Next period").performClick()
        composeRule.onNodeWithText("Add", useUnmergedTree = true).performClick()
        composeRule.onNodeWithContentDescription("Back").performClick()

        composeRule.runOnIdle {
            assertEquals(TimeRange.WEEK, selectedRange)
            assertEquals(1, previousCount)
            assertEquals(1, nextCount)
            assertEquals(0, refreshCount)
            assertEquals(1, addCount)
            assertEquals("back", navigatedRoute)
        }
    }

    @Test
    fun settingsCategoryFlow_routesEverySection() {
        val openedSections = mutableListOf<SettingsSection>()

        composeRule.setContent {
            OpenVitalsTheme {
                LazyColumn {
                    SettingsSection.entries.forEach { section ->
                        item {
                            SettingsCategoryCard(
                                section = section,
                                onClick = { openedSections += section },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("Display").performClick()
        composeRule.onNodeWithText("Activities").performClick()
        composeRule.onNodeWithText("Health Connect").performClick()
        composeRule.onNodeWithText("Permissions").performClick()

        composeRule.runOnIdle {
            assertEquals(
                listOf(
                    SettingsSection.DISPLAY,
                    SettingsSection.ACTIVITIES,
                    SettingsSection.HEALTH_CONNECT,
                    SettingsSection.PERMISSIONS,
                ),
                openedSections,
            )
        }
    }
}

internal val dashboardFlowWidgetIds = listOf(
    DashboardWidgetId.STEPS,
    DashboardWidgetId.WEEKLY_CARDIO_LOAD,
    DashboardWidgetId.DISTANCE,
    DashboardWidgetId.CALORIES_OUT,
    DashboardWidgetId.HYDRATION,
    DashboardWidgetId.AVG_HEART_RATE,
    DashboardWidgetId.WEIGHT,
    DashboardWidgetId.CARBS,
)

internal fun testUnitFormatter(): UnitFormatter =
    UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

internal fun dashboardFixtureData(): DashboardData =
    DashboardData(
        date = LocalDate.of(2026, 6, 29),
        steps = 8_742,
        distanceMeters = 6_380.0,
        caloriesKcal = 2_340.0,
        activeCaloriesKcal = 640.0,
        hydrationLiters = 1.8,
        weightKg = 78.4,
        weightTime = LocalDate.of(2026, 6, 29)
            .atTime(7, 15)
            .toInstant(ZoneOffset.UTC),
        avgHeartRateBpm = 71,
        heartRateSampleCount = 96,
        restingHeartRateBpm = 57,
        hrvRmssdMs = 42.0,
        bodyFatPercent = 18.6,
        leanMassKg = 63.8,
        caloriesInKcal = 1_920.0,
        proteinGrams = 132.0,
        carbsGrams = 210.0,
        fatGrams = 62.0,
        latestSystolicMmHg = 118,
        latestDiastolicMmHg = 76,
        latestSpO2Percent = 98.0,
        avgRespiratoryRate = 14.5,
        weeklyCardioLoad = DashboardWeeklyCardioLoad(
            currentScore = 426,
            targetScore = 620,
            todayScore = 64,
            confidence = CardioLoadConfidence.HIGH,
            targetSource = DashboardWeeklyCardioLoadTargetSource.RECENT_HISTORY,
        ),
        weeklyIntensityMinutes = DashboardWeeklyIntensityMinutes(
            moderateMinutes = 118,
            vigorousMinutes = 24,
            moderateEquivalentMinutes = 166,
            targetMinutes = 150,
            todayModerateEquivalentMinutes = 32,
            daysElapsed = 2,
            confidence = IntensityMinutesConfidence.HIGH,
        ),
        floorsClimbed = 11,
        elevationGainedMeters = 83.0,
        mindfulnessMinutes = 12,
        loadedMetrics = DashboardMetric.entries.toSet(),
    )
