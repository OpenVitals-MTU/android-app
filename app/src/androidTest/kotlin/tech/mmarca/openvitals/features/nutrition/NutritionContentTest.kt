package tech.mmarca.openvitals.features.nutrition

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The per-metric screen and the overview share the same empty/loaded contract.
 * The empty-day chart message is chosen inside the card, so it is tested here.
 */
class NutritionContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aMetricScreenShowsThePlaceholderWithNoData() {
        setMetricContent(state(hasData = false))

        composeRule.onNodeWithText(string(R.string.message_no_nutrition_period)).assertIsDisplayed()
    }

    @Test
    fun aMetricScreenLoadingDoesNotClaimThereIsNothingToShow() {
        setMetricContent(state(hasData = false, isLoading = true))

        composeRule
            .onNodeWithText(string(R.string.message_no_nutrition_period))
            .assertDoesNotExist()
    }

    @Test
    fun theOverviewShowsThePlaceholderWithNoData() {
        setOverviewContent(state(hasData = false))

        composeRule.onNodeWithText(string(R.string.message_no_nutrition_period)).assertIsDisplayed()
    }

    @Test
    fun aDayWithNoMealsSaysSoInsteadOfDrawingALineFromNothingToNothing() {
        // An untracked day still opens, and the chart card says it is empty rather than drawing zero.
        setMetricContent(
            NutritionUiState(
                isLoading = false,
                selectedRange = TimeRange.DAY,
                selectedDate = ANCHOR,
                dailyMacros = listOf(DailyMacros(date = ANCHOR)),
                entries = emptyList(),
                display = NutritionDisplayState(
                    selectedPeriod = DatePeriod(ANCHOR, ANCHOR),
                    hasData = true,
                    metric = NutritionMetricDisplay(
                        nutrient = NutritionNutrient.PROTEIN,
                        hasData = true,
                        values = listOf(NutritionDayValue(date = ANCHOR, value = 0.0)),
                    ),
                ),
            ),
        )

        // One intraday card serves every nutrient, so it names the screen, not the metric.
        composeRule
            .onNodeWithText(string(R.string.summary_empty_day, string(R.string.screen_nutrition)))
            .assertIsDisplayed()
    }

    private fun state(hasData: Boolean, isLoading: Boolean = false) = NutritionUiState(
        isLoading = isLoading,
        selectedRange = TimeRange.WEEK,
        selectedDate = ANCHOR,
        display = NutritionDisplayState(
            selectedPeriod = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
            hasData = hasData,
        ),
    )

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

    private fun setOverviewContent(state: NutritionUiState) = setContent { sectionContext ->
        nutritionContent(
            sectionContext = sectionContext,
            state = state,
            period = state.display.selectedPeriod,
            unitFormatter = FORMATTER,
            dateTimeFormatterProvider = DateTimeFormatterProvider(),
            chartDaySelection = ChartDaySelection(selectedDate = null, onDateSelected = {}),
        )
    }

    private fun setContent(
        content: androidx.compose.foundation.lazy.LazyListScope.(MetricDetailSectionContext) -> Unit,
    ) {
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

    private companion object {
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
        val FORMATTER = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC })
    }
}
