package tech.mmarca.openvitals.features.nutrition

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.insights.DailyGoalDay
import tech.mmarca.openvitals.domain.insights.DailyGoalDirection
import tech.mmarca.openvitals.domain.insights.DailyGoalProgress
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The populated half of Flutter's "Protein metric screen renders hero, chart,
 * goal card, statistics and meals" (`nutrition_screen_test.dart`).
 *
 * The empty case is already pinned by `NutritionContentTest`; what is left is
 * the one that says a tracked day is worth opening. Each of those five pieces
 * answers a different question — how much today, how the week went, whether the
 * target was hit, what the period averages, and which meals it came from — and
 * the screen builds them from separate parts of the display state, so any of
 * them can go missing on its own.
 */
class NutritionMetricScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aTrackedProteinPeriodRendersItsHeroAndItsChart() {
        setMetricContent(trackedState())

        // The hero names the metric and the chart repeats it as its title, so
        // the second node is the chart the hero alone would not prove.
        val protein = string(R.string.metric_protein)
        composeRule.onAllNodesWithText(protein).onFirst().assertIsDisplayed()
        assertTrue(
            "the hero should be followed by a chart carrying the same title",
            composeRule.onAllNodesWithText(protein).fetchSemanticsNodes().size >= 2,
        )
        // The hero's own number: the period total, in grams. The same total is
        // legitimately repeated in the chart's summary line and in the
        // statistics grid, so this pins that it is rendered and on screen, not
        // that it is rendered once.
        composeRule
            .onAllNodesWithText(FORMATTER.count(TOTAL_PROTEIN_GRAMS.toInt()), substring = true)
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun aTrackedProteinPeriodRendersItsGoalCardStatisticsAndMeals() {
        setMetricContent(trackedState())

        scrollTo(string(R.string.daily_goal))
        composeRule.onNodeWithText(string(R.string.daily_goal)).assertIsDisplayed()
        // Two of the three tracked days met the target — the count is the whole
        // point of the card.
        composeRule
            .onNodeWithText(string(R.string.goal_progress, GOAL_MET_DAYS, TRACKED_DAYS))
            .assertIsDisplayed()

        scrollTo(string(R.string.section_statistics))
        composeRule.onNodeWithText(string(R.string.section_statistics)).assertIsDisplayed()

        scrollTo(string(R.string.section_meals))
        composeRule.onNodeWithText(string(R.string.section_meals)).assertIsDisplayed()
        composeRule.onNodeWithText(MEAL_NAME).assertIsDisplayed()
    }

    private fun trackedState(): NutritionUiState {
        val days = (0 until TRACKED_DAYS).map { ANCHOR.minusDays(it.toLong()) }
        return NutritionUiState(
            isLoading = false,
            selectedRange = TimeRange.WEEK,
            selectedDate = ANCHOR,
            dailyMacros = days.map { date ->
                DailyMacros(
                    date = date,
                    nutrientValues = mapOf(NutritionNutrient.PROTEIN to DAILY_PROTEIN_GRAMS),
                )
            },
            entries = listOf(
                NutritionEntry(
                    time = ANCHOR.atStartOfDay(ZoneId.systemDefault()).plusHours(13).toInstant(),
                    mealType = 0,
                    name = MEAL_NAME,
                    energyKcal = 620.0,
                    proteinGrams = DAILY_PROTEIN_GRAMS,
                    carbsGrams = 40.0,
                    fatGrams = 18.0,
                    fiberGrams = 6.0,
                    sugarGrams = 4.0,
                    source = "tech.mmarca.openvitals",
                    id = "meal-1",
                    isOpenVitalsEntry = true,
                ),
            ),
            display = NutritionDisplayState(
                selectedPeriod = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
                hasData = true,
                metric = NutritionMetricDisplay(
                    nutrient = NutritionNutrient.PROTEIN,
                    hasData = true,
                    totalValue = TOTAL_PROTEIN_GRAMS,
                    values = days.map { date ->
                        NutritionDayValue(date = date, value = DAILY_PROTEIN_GRAMS)
                    },
                    goalProgress = DailyGoalProgress(
                        target = GOAL_GRAMS,
                        direction = DailyGoalDirection.AT_LEAST,
                        days = days.mapIndexed { index, date ->
                            DailyGoalDay(
                                date = date,
                                value = DAILY_PROTEIN_GRAMS,
                                isTracked = true,
                                // One day short of the target, so the card has a
                                // ratio to get wrong rather than a clean sweep.
                                isMet = index < GOAL_MET_DAYS,
                            )
                        },
                    ),
                    loggedDays = TRACKED_DAYS,
                    averageValue = DAILY_PROTEIN_GRAMS,
                    bestDayValue = DAILY_PROTEIN_GRAMS,
                ),
                trackedDates = days,
                sampleCount = TRACKED_DAYS,
            ),
        )
    }

    private fun setMetricContent(state: NutritionUiState) = setContent { sectionContext ->
        nutritionMetricContent(
            sectionContext = sectionContext,
            metric = NutritionMetric.PROTEIN,
            state = state,
            period = state.display.selectedPeriod,
            unitFormatter = FORMATTER,
            dateTimeFormatterProvider = DateTimeFormatterProvider(),
            chartDaySelection = ChartDaySelection(selectedDate = null, onDateSelected = {}),
            onDecreaseGoal = {},
            onIncreaseGoal = {},
        )
    }

    private fun setContent(content: LazyListScope.(MetricDetailSectionContext) -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                val sectionContext = MetricDetailSectionContext(
                    listState = rememberMetricDetailSectionListState(),
                    order = DefaultMetricDetailSectionOrder,
                    isEditingSections = false,
                    onMoveSectionToTarget = { _, _ -> },
                    onMoveSection = { _, _ -> },
                )
                LazyColumn { content(sectionContext) }
            }
        }
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text))
    }

    private companion object {
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })

        const val MEAL_NAME = "Chicken and rice"
        const val TRACKED_DAYS = 3
        const val GOAL_MET_DAYS = 2
        const val DAILY_PROTEIN_GRAMS = 110.0
        const val TOTAL_PROTEIN_GRAMS = 330.0
        const val GOAL_GRAMS = 120.0
    }
}
