package tech.mmarca.openvitals.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.WeightEntry
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.features.activity.ActivitiesUiState
import tech.mmarca.openvitals.features.activity.renderActivitiesOrderedContent
import tech.mmarca.openvitals.features.body.BodyUiState
import tech.mmarca.openvitals.features.body.bodyContent
import tech.mmarca.openvitals.features.dashboard.DashboardContent
import tech.mmarca.openvitals.features.dashboard.DashboardDailyGoals
import tech.mmarca.openvitals.features.dashboard.DashboardPresentationMapper
import tech.mmarca.openvitals.features.heart.HeartDisplayState
import tech.mmarca.openvitals.features.heart.HeartMetricDisplay
import tech.mmarca.openvitals.features.heart.HeartUiState
import tech.mmarca.openvitals.features.hydration.HydrationDisplayState
import tech.mmarca.openvitals.features.hydration.HydrationPeriodSummary
import tech.mmarca.openvitals.features.hydration.HydrationUiState
import tech.mmarca.openvitals.features.hydration.hydrationPeriodContent
import tech.mmarca.openvitals.features.mindfulness.MindfulnessDisplayState
import tech.mmarca.openvitals.features.mindfulness.MindfulnessPeriodSummary
import tech.mmarca.openvitals.features.mindfulness.MindfulnessUiState
import tech.mmarca.openvitals.features.mindfulness.mindfulnessPeriodContent
import tech.mmarca.openvitals.features.settings.SettingsCategoryCard
import tech.mmarca.openvitals.features.settings.SettingsSection
import tech.mmarca.openvitals.features.settings.SettingsVersionText
import tech.mmarca.openvitals.features.settings.SupportOpenVitalsCard
import tech.mmarca.openvitals.features.sleep.SleepDisplayState
import tech.mmarca.openvitals.features.sleep.SleepDurationPoint
import tech.mmarca.openvitals.features.sleep.SleepUiState
import tech.mmarca.openvitals.features.sleep.sleepPeriodContent
import tech.mmarca.openvitals.features.vitals.VitalsOverviewContent
import tech.mmarca.openvitals.testing.TextScaleSurface
import tech.mmarca.openvitals.testing.assertScaledScreenFitsItsWidth
import tech.mmarca.openvitals.testing.dashboardFixtureData
import tech.mmarca.openvitals.testing.dashboardFlowWidgetIds
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.testing.testUnitFormatter
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState

/**
 * Port of Flutter's `test/ui/text_scaling_sweep_test.dart`.
 *
 * Every top-level screen, on a phone-sized surface, with the system font scale
 * at the largest setting Android offers.
 *
 * A health app skews toward users who run large fonts, and a layout that breaks
 * at 2.0 is invisible at 1.0 — nothing else in the suite would ever catch it.
 * A screen failing here is a layout bug, not a test problem: the fix is
 * wrapping, ellipsis or scrolling, never shrinking the user's text.
 *
 * Where Flutter can assert "no RenderFlex overflowed", Compose has no such
 * exception — it squeezes and clips instead. So the assertion here is the one
 * that still means something: the screen composes, it renders content, and no
 * piece of that content is laid out past the side of the phone.
 *
 * Dates are anchored in the past so the screens render DATA rather than only
 * their empty states — a populated card is where text collides.
 */
class TextScalingSweepTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun dashboardScreenSurvivesTheLargestFontScale() {
        val unitFormatter = testUnitFormatter()
        val provider = DateTimeFormatterProvider()
        val data = dashboardFixtureData()

        setScaled {
            DashboardContent(
                data = data,
                display = DashboardPresentationMapper.build(
                    data = data,
                    dailyGoals = DashboardDailyGoals(),
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = provider,
                ),
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = provider,
                canGoForward = false,
                isRefreshing = false,
                dashboardWidgets = dashboardFlowWidgetIds,
                isEditingDashboard = false,
                onPreviousDay = {},
                onNextDay = {},
                onOpenCalendar = {},
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
            )
        }

        composeRule.assertScaledScreenFitsItsWidth()
        // The tile the user opens the app for is still legible, not scaled out
        // of its own card.
        composeRule.onAllNodesWithText(string(R.string.metric_steps)).onFirst().assertIsDisplayed()
    }

    @Test
    fun sleepScreenSurvivesTheLargestFontScale() {
        val state = SleepUiState(
            isLoading = false,
            selectedRange = TimeRange.WEEK,
            selectedDate = ANCHOR,
            display = SleepDisplayState(
                durationPoints = (0..6).map {
                    SleepDurationPoint(date = ANCHOR.minusDays(it.toLong()), hours = 6.5 + it * 0.25)
                },
            ),
        )

        setScaled {
            val sectionContext = sectionContext()
            MetricDetailScaffold(
                isLoading = false,
                selectedRange = TimeRange.WEEK,
                selectedDate = ANCHOR,
                onRefresh = {},
                onSelectRange = {},
                onPreviousPeriod = {},
                onNextPeriod = {},
                onSelectDate = {},
                sectionListState = sectionContext.listState,
            ) { period ->
                sleepPeriodContent(
                    sectionContext = sectionContext,
                    state = state,
                    display = state.display,
                    period = period,
                    chartDaySelection = noDaySelected(),
                    unitFormatter = testUnitFormatter(),
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    onOpenSleepSession = {},
                    onOpenSleepScore = null,
                    onOpenSleepEfficiency = null,
                    onDecreaseGoal = {},
                    onIncreaseGoal = {},
                )
            }
        }

        composeRule.assertScaledScreenFitsItsWidth()
        // The range switcher is the one control on the screen; losing it to the
        // scaled-up title strands the user in whichever period opened first.
        composeRule.onNodeWithText(string(R.string.range_week)).assertIsDisplayed()
    }

    @Test
    fun hydrationScreenSurvivesTheLargestFontScale() {
        val state = HydrationUiState(
            isLoading = false,
            selectedRange = TimeRange.WEEK,
            selectedDate = ANCHOR,
            dailyHydration = (0..6).map {
                DailyHydration(ANCHOR.minusDays(it.toLong()), 1.4 + it * 0.2)
            },
            hydrationEntries = listOf(hydrationEntry(9, 0.5)),
            display = HydrationDisplayState(
                selectedPeriod = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
                hasData = true,
                summary = HydrationPeriodSummary(totalLiters = 12.5, trackedDays = 7, loggedDays = 5),
            ),
        )

        setScaled {
            val sectionContext = sectionContext()
            LazyColumn {
                hydrationPeriodContent(
                    sectionContext = sectionContext,
                    state = state,
                    period = state.display.selectedPeriod,
                    unitFormatter = testUnitFormatter(),
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    chartDaySelection = noDaySelected(),
                    hasNotificationPermission = true,
                    onDecreaseGoal = {},
                    onIncreaseGoal = {},
                    onToggleReminders = {},
                    onRequestNotificationPermission = {},
                    onDecreaseInterval = {},
                    onIncreaseInterval = {},
                    onSelectActiveStartTime = { _: LocalTime -> },
                    onSelectActiveEndTime = { _: LocalTime -> },
                    onEditHydrationEntry = {},
                    onDeleteHydrationEntry = {},
                )
            }
        }

        composeRule.assertScaledScreenFitsItsWidth()
        composeRule.onNodeWithText(string(R.string.metric_total_hydration)).assertIsDisplayed()
    }

    @Test
    fun activitiesScreenSurvivesTheLargestFontScale() {
        val state = ActivitiesUiState(
            isLoading = false,
            selectedRange = TimeRange.WEEK,
            selectedDate = ANCHOR,
            workouts = listOf(workout()),
        )

        setScaled {
            val sectionContext = sectionContext()
            LazyColumn {
                renderActivitiesOrderedContent(
                    sectionContext = sectionContext,
                    state = state,
                    period = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
                    chartDaySelection = noDaySelected(),
                    selectedActivityType = null,
                    availableActivityTypes = emptyList(),
                    onSelectActivityType = {},
                    unitFormatter = testUnitFormatter(),
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    onOpenActivity = {},
                    onEditActivity = {},
                    onDeleteActivity = {},
                    onStartPlannedWorkout = {},
                    onOpenCardioLoad = null,
                    onOpenSteps = null,
                    onOpenDistance = null,
                    onOpenEnergyBurned = null,
                    onOpenHrv = null,
                    onDecreaseGoal = {},
                    onIncreaseGoal = {},
                )
            }
        }

        composeRule.assertScaledScreenFitsItsWidth()
        // The workout the user recorded is still named, not squeezed out by the
        // metrics beside it.
        composeRule.onNodeWithText(WORKOUT_TITLE).assertIsDisplayed()
    }

    @Test
    fun bodyScreenSurvivesTheLargestFontScale() {
        val state = BodyUiState(
            isLoading = false,
            selectedRange = TimeRange.MONTH,
            selectedDate = ANCHOR,
            weightEntries = (0..3).map { weight(it) },
        )

        setScaled {
            val sectionContext = sectionContext()
            LazyColumn {
                bodyContent(
                    state = state,
                    period = DatePeriod(ANCHOR.withDayOfMonth(1), ANCHOR),
                    unitFormatter = testUnitFormatter(),
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    chartDaySelection = noDaySelected(),
                    sectionContext = sectionContext,
                    onEditBodyMeasurement = { _, _ -> },
                    onDeleteBodyMeasurement = { _, _ -> },
                )
            }
        }

        composeRule.assertScaledScreenFitsItsWidth()
    }

    @Test
    fun mindfulnessScreenSurvivesTheLargestFontScale() {
        val state = MindfulnessUiState(
            isLoading = false,
            selectedRange = TimeRange.WEEK,
            selectedDate = ANCHOR,
            sessions = listOf(mindfulnessSession()),
            display = MindfulnessDisplayState(
                selectedPeriod = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
                hasData = true,
                summary = MindfulnessPeriodSummary(
                    totalMinutes = 45L,
                    totalMs = 45L * 60_000L,
                    sessionCount = 1,
                    averageDurationMs = 15L * 60_000L,
                    longestSessionMs = 20L * 60_000L,
                ),
            ),
        )

        setScaled {
            LazyColumn {
                mindfulnessPeriodContent(
                    state = state,
                    period = state.display.selectedPeriod,
                    unitFormatter = testUnitFormatter(),
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    chartDaySelection = noDaySelected(),
                    hasNotificationPermission = true,
                    onDecreaseGoal = {},
                    onIncreaseGoal = {},
                    onToggleReminders = {},
                    onRequestNotificationPermission = {},
                    onSelectReminderTime = { _: LocalTime -> },
                    onEditMindfulnessSession = {},
                    onDeleteMindfulnessSession = {},
                )
            }
        }

        composeRule.assertScaledScreenFitsItsWidth()
        composeRule.onNodeWithText(string(R.string.metric_mindfulness)).assertIsDisplayed()
    }

    @Test
    fun heartVitalsOverviewScreenSurvivesTheLargestFontScale() {
        val state = HeartUiState(
            isLoading = false,
            selectedRange = TimeRange.WEEK,
            selectedDate = ANCHOR,
            bloodPressure = listOf(bloodPressure()),
            spO2 = listOf(oxygenSaturation()),
            display = HeartDisplayState(
                selectedPeriod = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
                metric = HeartMetricDisplay(hasData = true, hasVitalsEntries = true),
            ),
        )

        setScaled {
            val sectionContext = sectionContext()
            LazyColumn {
                VitalsOverviewContent(
                    state = state,
                    period = state.display.selectedPeriod,
                    unitFormatter = testUnitFormatter(),
                    dateTimeFormatterProvider = DateTimeFormatterProvider(),
                    chartDaySelection = noDaySelected(),
                    sectionContext = sectionContext,
                    onOpenMetric = {},
                )
            }
        }

        composeRule.assertScaledScreenFitsItsWidth()
    }

    @Test
    fun settingsScreenSurvivesTheLargestFontScale() {
        setScaled {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SettingsSection.entries.forEach { section ->
                    SettingsCategoryCard(section = section, onClick = {})
                }
                SupportOpenVitalsCard(
                    onOpenIssues = {},
                    onOpenDiscussion = {},
                    onOpenSupport = {},
                )
                SettingsVersionText()
            }
        }

        composeRule.assertScaledScreenFitsItsWidth()
        // Settings is a routing surface: a section title that runs off the side
        // is a feature the user can no longer identify before tapping into it.
        composeRule
            .onNodeWithText(string(SettingsSection.entries.first().titleRes))
            .assertIsDisplayed()
    }

    private fun setScaled(content: @Composable () -> Unit) {
        composeRule.setContent {
            TextScaleSurface { content() }
        }
    }

    @Composable
    private fun sectionContext() = MetricDetailSectionContext(
        listState = rememberMetricDetailSectionListState(),
        order = DefaultMetricDetailSectionOrder,
        isEditingSections = false,
        onMoveSectionToTarget = { _, _ -> },
        onMoveSection = { _, _ -> },
    )

    private fun noDaySelected() = ChartDaySelection(selectedDate = null, onDateSelected = {})

    private fun instantAt(day: LocalDate, hour: Int): Instant =
        day.atStartOfDay(ZoneId.systemDefault()).plusHours(hour.toLong()).toInstant()

    private fun hydrationEntry(hour: Int, liters: Double) = HydrationEntry(
        id = "hydration-$hour",
        startTime = instantAt(ANCHOR, hour),
        endTime = instantAt(ANCHOR, hour),
        liters = liters,
        source = "tech.mmarca.openvitals",
    )

    private fun workout() = ExerciseData(
        id = "workout-1",
        title = WORKOUT_TITLE,
        exerciseType = 56,
        startTime = instantAt(ANCHOR, 7),
        endTime = instantAt(ANCHOR, 8),
        durationMs = 60L * 60_000L,
        source = "tech.mmarca.openvitals",
        totalDistanceMeters = 9_400.0,
        totalCaloriesKcal = 620.0,
        averageHeartRateBpm = 148,
    )

    private fun weight(daysAgo: Int) = WeightEntry(
        id = "weight-$daysAgo",
        time = instantAt(ANCHOR.minusDays(daysAgo.toLong()), 7),
        weightKg = 72.5 + daysAgo * 0.3,
        source = "tech.mmarca.openvitals",
        isOpenVitalsEntry = true,
    )

    private fun mindfulnessSession() = MindfulnessSession(
        id = "session-1",
        title = "Morning sit",
        startTime = instantAt(ANCHOR, 7),
        endTime = instantAt(ANCHOR, 7).plusSeconds(15 * 60),
        durationMs = 15L * 60_000L,
        source = "tech.mmarca.openvitals",
        isOpenVitalsEntry = true,
    )

    private fun bloodPressure() = BloodPressureEntry(
        time = instantAt(ANCHOR, 8),
        systolicMmHg = 118,
        diastolicMmHg = 76,
        source = "tech.mmarca.openvitals",
    )

    private fun oxygenSaturation() = SpO2Entry(
        time = instantAt(ANCHOR, 8),
        percent = 98.0,
        source = "tech.mmarca.openvitals",
    )

    private companion object {
        /** A fixed past date, so no period straddles today and no chart ends at "now". */
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
        const val WORKOUT_TITLE = "Morning run"
    }
}
