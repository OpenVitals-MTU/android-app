package tech.mmarca.openvitals.features.heart

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.preferences.DefaultMetricDetailSectionOrder
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.components.ChartDaySelection
import tech.mmarca.openvitals.ui.components.rememberMetricDetailSectionListState
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of the rendering cases of Flutter's
 * `test/features/heart/heart_metric_screen_test.dart`.
 *
 * This content is shared by every heart and vitals metric, so two of its
 * branches are worth pinning here rather than per metric: the empty state, and
 * the data-source education link, which is the only route from a reading a user
 * disputes to the app that wrote it.
 */
class HeartMetricContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTheEmptyPlaceholderWithNoData() {
        setContent(state(hasData = false))

        composeRule.onNodeWithText(string(R.string.message_no_heart_period)).assertIsDisplayed()
    }

    @Test
    fun aLoadingPeriodDoesNotClaimThereIsNothingToShow() {
        setContent(state(hasData = false, isLoading = true))

        composeRule.onNodeWithText(string(R.string.message_no_heart_period)).assertDoesNotExist()
    }

    @Test
    fun aLoadedPeriodCarriesTheDataSourceEducationLink() {
        // "Whose reading is this?" is the first question an unexpected number
        // provokes, and this link is the only answer the screen offers.
        setContent(state(hasData = true, summaries = listOf(summary())))

        composeRule.onNode(hasScrollAction())
            .performScrollToNode(hasText(string(R.string.health_connect_data_source_manage)))
        composeRule
            .onNodeWithText(string(R.string.health_connect_data_source_manage))
            .assertIsDisplayed()
    }

    @Test
    fun aLoadedPeriodRendersTheOrderedSections() {
        // A week view is not one card: it is the chart, the threshold checks a
        // user tunes, the statistics, the per-day breakdown and the confidence
        // note, in that order. Losing any one of them removes a whole answer
        // from the screen with nothing to say it is missing.
        setContent(state(hasData = true, summaries = listOf(summary())))

        listOf(
            // PERIOD_CHART
            R.string.metric_average_heart_rate,
            // DAILY_GOAL — the high/low threshold checks live in that slot.
            R.string.heart_rate_health_checks_title,
            R.string.section_statistics,
            // ENTRIES
            R.string.section_daily_breakdown,
            R.string.data_confidence_title,
        ).forEach { titleRes ->
            composeRule.onNode(hasScrollAction())
                .performScrollToNode(hasText(string(titleRes)))
            composeRule.onNodeWithText(string(titleRes)).assertIsDisplayed()
        }
    }

    private fun state(
        hasData: Boolean,
        isLoading: Boolean = false,
        summaries: List<HeartRateSummary> = emptyList(),
    ) = HeartUiState(
        isLoading = isLoading,
        selectedRange = TimeRange.WEEK,
        selectedDate = ANCHOR,
        dailySummaries = summaries,
        display = HeartDisplayState(
            selectedPeriod = DatePeriod(ANCHOR.minusDays(6), ANCHOR),
            metric = HeartMetricDisplay(
                hasData = hasData,
                hasPeriodHeartRateSummaries = summaries.isNotEmpty(),
                sortedDailySummaries = summaries,
                heartRateTrackedDates = summaries.map { it.date },
                heartRateSampleCount = summaries.size,
            ),
        ),
    )

    private fun summary() = HeartRateSummary(
        date = ANCHOR,
        avgBpm = 71,
        minBpm = 52,
        maxBpm = 140,
    )

    private fun setContent(state: HeartUiState) {
        composeRule.setContent {
            OpenVitalsTheme {
                val sectionContext = MetricDetailSectionContext(
                    listState = rememberMetricDetailSectionListState(),
                    order = DefaultMetricDetailSectionOrder,
                    isEditingSections = false,
                    onMoveSectionToTarget = { _, _ -> },
                    onMoveSection = { _, _ -> },
                )
                LazyColumn {
                    averageHeartRateContent(
                        state = state,
                        period = state.display.selectedPeriod,
                        unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        chartDaySelection = ChartDaySelection(selectedDate = null, onDateSelected = {}),
                        sectionContext = sectionContext,
                        onDecreaseHighHeartRateThreshold = {},
                        onIncreaseHighHeartRateThreshold = {},
                        onDecreaseLowHeartRateThreshold = {},
                        onIncreaseLowHeartRateThreshold = {},
                    )
                }
            }
        }
    }

    private companion object {
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
    }
}
