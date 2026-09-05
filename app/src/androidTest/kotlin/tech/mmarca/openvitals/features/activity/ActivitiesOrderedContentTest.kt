package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The activities overview: the sessions, the five key-metric cards, and the way through
 * from each card to its metric. Nothing else on the screen links there.
 */
class ActivitiesOrderedContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aPeriodRendersItsWorkoutsAndTheKeyMetricCards() {
        setContent()

        // ACTIVITY_SUMMARY: the session the user actually did.
        scrollTo(WORKOUT_TITLE)
        composeRule.onAllNodesWithText(WORKOUT_TITLE).onFirst().assertIsDisplayed()

        // ACTIVITY_KEY_METRICS: the five cards, each a link to its own metric.
        scrollTo(string(R.string.activities_key_metrics))
        listOf(
            R.string.metric_cardio_load,
            R.string.metric_energy_burned,
            R.string.metric_steps,
            R.string.metric_distance,
            R.string.metric_hrv,
        ).forEach { titleRes ->
            scrollTo(string(titleRes))
            composeRule.onAllNodesWithText(string(titleRes)).onFirst().assertIsDisplayed()
        }

        // DAILY_GOAL, STATISTICS and DATA_CONFIDENCE round out the period.
        listOf(
            R.string.daily_goal,
            R.string.stat_total,
            R.string.data_confidence_title,
        ).forEach { titleRes ->
            scrollTo(string(titleRes))
            composeRule.onAllNodesWithText(string(titleRes)).onFirst().assertIsDisplayed()
        }
    }

    @Test
    fun theStepsKeyMetricCardOpensTheStepsMetric() {
        var openedSteps = 0
        setContent(onOpenSteps = { openedSteps++ })

        scrollTo(string(R.string.metric_steps))
        composeRule.onAllNodesWithText(string(R.string.metric_steps)).onFirst().performClick()

        composeRule.runOnIdle { assertEquals(1, openedSteps) }
    }

    @Test
    fun theCardioLoadCardOpensTheCardioLoadDetail() {
        // Cardio load has no other entry point in the app: this card is it.
        var openedCardioLoad = 0
        setContent(onOpenCardioLoad = { openedCardioLoad++ })

        scrollTo(string(R.string.metric_cardio_load))
        composeRule.onAllNodesWithText(string(R.string.metric_cardio_load)).onFirst().performClick()

        composeRule.runOnIdle { assertEquals(1, openedCardioLoad) }
    }

    @Test
    fun theKeyMetricSparklineLabelsEveryWeekdayBucket() {
        // The sparkline has no axis, so the weekday row underneath is what names the peak.
        val dates = (0L until 7L).map { WEEK_START.plusDays(it) }
        composeRule.setContent {
            OpenVitalsTheme {
                ActivityMetricSparkline(
                    values = dates.mapIndexed { index, _ -> 6_000.0 + index * 500.0 },
                    dates = dates,
                    selectedRange = TimeRange.WEEK,
                    accentColor = androidx.compose.ui.graphics.Color.Red,
                )
            }
        }

        val locale = java.util.Locale.getDefault()
        val expected = dates.map { activityOverviewBucketLabel(it, TimeRange.WEEK, locale) }
        // Weekday initials collide (Tuesday/Thursday), so the count per distinct label is what pins one label per bucket.
        expected.distinct().forEach { label ->
            assertEquals(
                "\"$label\" is drawn once for each day it names",
                expected.count { it == label },
                composeRule.onAllNodesWithText(label).fetchSemanticsNodes().size,
            )
        }
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    private fun setContent(
        onOpenCardioLoad: () -> Unit = {},
        onOpenSteps: () -> Unit = {},
    ) {
        val workouts = listOf(workout())
        val state = ActivitiesUiState(
            isLoading = false,
            selectedRange = TimeRange.WEEK,
            selectedDate = ANCHOR,
            workouts = workouts,
            overviewDays = (0L until 7L).map { offset ->
                val date = WEEK_START.plusDays(offset)
                ActivityOverviewDay(
                    date = date,
                    steps = 6_000L + offset * 500L,
                    distanceMeters = 4_200.0 + offset * 350.0,
                    energyBurnedKcal = 2_200.0,
                    energyBurnedSource = CaloriesBurnedSource.RECORDED_TOTAL,
                    workouts = workouts.filter {
                        it.startTime.atZone(ZoneId.systemDefault()).toLocalDate() == date
                    },
                    hrvRmssdMs = 48.0,
                )
            },
        )
        composeRule.setContent {
            OpenVitalsTheme {
                val sectionContext = MetricDetailSectionContext(
                    listState = rememberMetricDetailSectionListState(),
                    order = DefaultMetricDetailSectionOrder,
                    isEditingSections = false,
                    onMoveSectionToTarget = { _, _ -> },
                    onMoveSection = { _, _ -> },
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    renderActivitiesOrderedContent(
                        sectionContext = sectionContext,
                        state = state,
                        period = PERIOD,
                        chartDaySelection = ChartDaySelection(
                            selectedDate = null,
                            onDateSelected = {},
                        ),
                        selectedActivityType = null,
                        availableActivityTypes = emptyList(),
                        onSelectActivityType = {},
                        unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        onOpenActivity = {},
                        onEditActivity = {},
                        onDeleteActivity = {},
                        onStartPlannedWorkout = {},
                        onOpenCardioLoad = onOpenCardioLoad,
                        onOpenSteps = onOpenSteps,
                        onOpenDistance = {},
                        onOpenEnergyBurned = {},
                        onOpenHrv = {},
                        onDecreaseGoal = {},
                        onIncreaseGoal = {},
                    )
                }
            }
        }
    }

    private fun workout(): ExerciseData {
        val start = ANCHOR.atStartOfDay(ZoneId.systemDefault()).plusHours(8).toInstant()
        return ExerciseData(
            id = "workout-1",
            title = WORKOUT_TITLE,
            exerciseType = RUNNING_EXERCISE_TYPE,
            startTime = start,
            endTime = start.plusSeconds(40 * 60),
            durationMs = 40 * 60 * 1_000L,
            source = "tech.mmarca.openvitals",
            totalDistanceMeters = 6_000.0,
        )
    }

    private companion object {
        /** A fixed, fully elapsed past week, so nothing depends on the run date. */
        val WEEK_START: LocalDate = LocalDate.of(2026, 6, 22)
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 24)
        val PERIOD = DatePeriod(WEEK_START, WEEK_START.plusDays(6))
        const val WORKOUT_TITLE = "Morning run"
        const val RUNNING_EXERCISE_TYPE = 56
    }
}
