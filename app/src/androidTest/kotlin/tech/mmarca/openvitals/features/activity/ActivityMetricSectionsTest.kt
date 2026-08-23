package tech.mmarca.openvitals.features.activity

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.WeekPeriodMode
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of the rendering cases of Flutter's
 * `test/features/activity/activity_screens_test.dart`.
 *
 * Seven movement metrics share one ordered-section layout, so a break in it is
 * a break in all seven at once. What a user loses is not a chart but the
 * answers around it: how the week compares to the goal, how confident the app
 * is in the numbers, and which day each number came from.
 */
class ActivityMetricSectionsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun stepsPeriodRendersTheChartGoalStatisticsConfidenceAndEntries() {
        setContent(ActivityMetric.STEPS) { state, sectionContext ->
            stepsContent(
                state = state,
                period = state.display.selectedPeriod,
                unitFormatter = METRIC_FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                chartDaySelection = NO_DAY_SELECTED,
                sectionContext = sectionContext,
                onDecreaseGoal = {},
                onIncreaseGoal = {},
            )
        }

        assertSharedSectionsRender(R.string.metric_steps)
    }

    @Test
    fun stepsWithNoDataSaysSoInsteadOfDrawingAnEmptyChart() {
        // An axis with no bars reads as "zero steps", which is a claim about
        // the user's week. The placeholder says the app has nothing, which is
        // the truth when the permission is there but the provider is silent.
        setContent(ActivityMetric.STEPS, rows = emptyList(), nutritionRows = emptyList()) {
                state, sectionContext ->
            stepsContent(
                state = state,
                period = state.display.selectedPeriod,
                unitFormatter = METRIC_FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                chartDaySelection = NO_DAY_SELECTED,
                sectionContext = sectionContext,
                onDecreaseGoal = {},
                onIncreaseGoal = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.message_no_step_updates)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.daily_goal)).assertDoesNotExist()
    }

    @Test
    fun distancePeriodRendersTheSharedSections() {
        setContent(ActivityMetric.DISTANCE) { state, sectionContext ->
            distanceContent(
                state = state,
                period = state.display.selectedPeriod,
                unitFormatter = METRIC_FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                chartDaySelection = NO_DAY_SELECTED,
                sectionContext = sectionContext,
                onDecreaseGoal = {},
                onIncreaseGoal = {},
            )
        }

        assertSharedSectionsRender(R.string.metric_distance)
    }

    @Test
    fun caloriesBurnedPeriodRendersTheSharedSections() {
        setContent(ActivityMetric.CALORIES_BURNED) { state, sectionContext ->
            caloriesContent(
                state = state,
                period = state.display.selectedPeriod,
                unitFormatter = METRIC_FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                chartDaySelection = NO_DAY_SELECTED,
                sectionContext = sectionContext,
                onDecreaseGoal = {},
                onIncreaseGoal = {},
            )
        }

        assertSharedSectionsRender(R.string.metric_calories_burned)
    }

    @Test
    fun activeCaloriesPeriodRendersTheSharedSections() {
        setContent(ActivityMetric.ACTIVE_CALORIES) { state, sectionContext ->
            activeCaloriesContent(
                state = state,
                period = state.display.selectedPeriod,
                unitFormatter = METRIC_FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                chartDaySelection = NO_DAY_SELECTED,
                sectionContext = sectionContext,
                onDecreaseGoal = {},
                onIncreaseGoal = {},
            )
        }

        assertSharedSectionsRender(R.string.metric_active_calories)
    }

    @Test
    fun floorsPeriodRendersTheSharedSections() {
        setContent(ActivityMetric.FLOORS) { state, sectionContext ->
            floorsContent(
                state = state,
                period = state.display.selectedPeriod,
                unitFormatter = METRIC_FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                chartDaySelection = NO_DAY_SELECTED,
                sectionContext = sectionContext,
                onDecreaseGoal = {},
                onIncreaseGoal = {},
            )
        }

        assertSharedSectionsRender(R.string.metric_floors_climbed)
    }

    @Test
    fun elevationPeriodRendersTheSharedSections() {
        setContent(ActivityMetric.ELEVATION) { state, sectionContext ->
            elevationContent(
                state = state,
                period = state.display.selectedPeriod,
                unitFormatter = METRIC_FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                chartDaySelection = NO_DAY_SELECTED,
                sectionContext = sectionContext,
                onDecreaseGoal = {},
                onIncreaseGoal = {},
            )
        }

        assertSharedSectionsRender(R.string.metric_elevation_gained)
    }

    @Test
    fun wheelchairPushesPeriodRendersTheSharedSections() {
        setContent(ActivityMetric.WHEELCHAIR_PUSHES) { state, sectionContext ->
            wheelchairPushesContent(
                state = state,
                period = state.display.selectedPeriod,
                unitFormatter = METRIC_FORMATTER,
                dateTimeFormatterProvider = DateTimeFormatterProvider(),
                chartDaySelection = NO_DAY_SELECTED,
                sectionContext = sectionContext,
                onDecreaseGoal = {},
                onIncreaseGoal = {},
            )
        }

        assertSharedSectionsRender(R.string.metric_wheelchair_pushes)
    }

    /**
     * The five sections every movement metric owes its period view: the chart
     * titled with the metric, the goal card, the goal and period statistics,
     * the confidence note and the per-day entries.
     */
    private fun assertSharedSectionsRender(titleRes: Int) {
        scrollTo(titleRes)
        composeRule.onAllNodesWithText(string(titleRes)).onFirst().assertIsDisplayed()

        listOf(
            R.string.daily_goal,
            R.string.stat_goals_met,
            R.string.stat_goal_balance,
            R.string.stat_total,
            R.string.stat_active_days,
            R.string.data_confidence_title,
            R.string.section_entries,
        ).forEach { sectionRes ->
            scrollTo(sectionRes)
            composeRule.onAllNodesWithText(string(sectionRes)).onFirst().assertIsDisplayed()
        }
    }

    private fun scrollTo(textRes: Int) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(string(textRes)))
    }

    private fun setContent(
        metric: ActivityMetric,
        rows: List<DailySteps> = dailyStepsRows(),
        nutritionRows: List<DailyNutrition> = nutritionRows(),
        content: LazyListScope.(ActivityUiState, MetricDetailSectionContext) -> Unit,
    ) {
        val display = ActivityPresentationMapper.build(
            query = QUERY,
            metric = metric,
            dailyGoal = metric.dailyGoalKey.defaultValue,
            dailySteps = rows,
            previousDailySteps = emptyList(),
            baselineDailySteps = emptyList(),
            nutrition = nutritionRows,
            previousNutrition = emptyList(),
            baselineNutrition = emptyList(),
            activityProgress = emptyList(),
        )
        val state = ActivityUiState(
            isLoading = false,
            selectedRange = TimeRange.WEEK,
            selectedDate = ANCHOR,
            dailyGoal = metric.dailyGoalKey.defaultValue,
            dailySteps = rows,
            nutrition = nutritionRows,
            display = display,
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
                    content(state, sectionContext)
                }
            }
        }
    }

    private fun dailyStepsRows(): List<DailySteps> = listOf(
        dailySteps(WEEK_START.plusDays(1), 9_000),
        dailySteps(WEEK_START.plusDays(2), 7_000),
    )

    private fun dailySteps(date: LocalDate, steps: Long) = DailySteps(
        date = date,
        steps = steps,
        distanceMeters = steps * 0.7,
        wheelchairPushes = steps / 8,
        floorsClimbed = 3,
        activeCaloriesKcal = steps * 0.04,
        elevationGainedMeters = 12.0,
    )

    private fun nutritionRows(): List<DailyNutrition> = listOf(
        DailyNutrition(WEEK_START.plusDays(1), hydrationLiters = 0.0, caloriesBurnedKcal = 2_200.0),
        DailyNutrition(WEEK_START.plusDays(2), hydrationLiters = 0.0, caloriesBurnedKcal = 2_100.0),
    )

    private companion object {
        /** A fixed, fully elapsed past week, so nothing depends on the run date. */
        val WEEK_START: LocalDate = LocalDate.of(2026, 6, 22)
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
        val QUERY = PeriodLoadQuery(
            range = TimeRange.WEEK,
            anchorDate = ANCHOR,
            today = WEEK_START.plusDays(6),
            weekPeriodMode = WeekPeriodMode.MONDAY_TO_SUNDAY,
        )
        val METRIC_FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
        val NO_DAY_SELECTED = ChartDaySelection(selectedDate = null, onDateSelected = {})
    }
}
