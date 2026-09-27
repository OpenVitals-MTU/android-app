package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.cycle.CycleEstimate
import tech.mmarca.openvitals.domain.cycle.CycleEstimateResult
import tech.mmarca.openvitals.domain.cycle.RecordedCycle
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The content draws the branch the display state chose. The derivations are
 * in `CyclePresentationMapperTest`. The today cards come first, so every
 * section below them is reached by scrolling.
 */
class CyclePeriodContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTheStatisticsOnceLoaded() {
        setContent(state(hasData = true, summary = CyclePeriodSummary(periodDays = 5)))

        scrollTo(hasText(string(R.string.metric_period_days)))
        composeRule.onNodeWithText(string(R.string.metric_period_days)).assertIsDisplayed()
    }

    @Test
    fun showsTheEstimateForARecordedCycleWithEnoughHistory() {
        val start = ANCHOR.minusDays(11)
        val estimate = CycleEstimate(
            earliestDate = ANCHOR.plusDays(15),
            centralDate = ANCHOR.plusDays(17),
            latestDate = ANCHOR.plusDays(19),
            cycleCount = 3,
            variabilityDays = 2,
        )
        setContent(
            state(
                hasData = true,
                today = CycleTodayDisplay(
                    today = ANCHOR,
                    currentCycle = RecordedCycle(startDate = start, endDate = null, flowDays = mapOf(start to 2)),
                    cycleDay = 12,
                    estimate = CycleEstimateResult.Available(estimate),
                    recentIntervalLengths = listOf(28, 27, 29),
                    totalCycles = 3,
                ),
            ),
        )

        scrollTo(hasText(string(R.string.cycle_estimate_disclaimer)))
        composeRule.onNodeWithText(string(R.string.cycle_estimate_disclaimer)).assertIsDisplayed()
        // The period-start button is offered while today has no flow.
        scrollTo(hasText(string(R.string.cycle_start_period)))
        composeRule.onNodeWithText(string(R.string.cycle_start_period)).assertIsDisplayed()
    }

    @Test
    fun saysItIsWaitingForHistoryWithOneStart() {
        setContent(state(hasData = true, today = CycleTodayDisplay(today = ANCHOR, estimate = CycleEstimateResult.NeedsMoreHistory)))

        scrollTo(hasText(string(R.string.cycle_estimate_needs_history_title)))
        composeRule.onNodeWithText(string(R.string.cycle_estimate_needs_history_title)).assertIsDisplayed()
    }

    @Test
    fun onlyAnOwnObservationGetsTheEditPencil() {
        setContent(
            state(hasData = true),
            observations = listOf(
                CycleObservation(
                    time = Instant.parse("2026-06-20T10:00:00Z"),
                    title = "Ours",
                    value = "Light",
                    source = "OpenVitals",
                    id = "uid-1",
                    kind = CycleEntryKind.MENSTRUATION_FLOW,
                    isOpenVitalsEntry = true,
                    dayLogDate = LocalDate.of(2026, 6, 20),
                ),
                CycleObservation(
                    time = Instant.parse("2026-06-21T10:00:00Z"),
                    title = "Theirs",
                    value = "Medium",
                    source = "Gadgetbridge",
                    id = "uid-2",
                    kind = CycleEntryKind.MENSTRUATION_FLOW,
                    isOpenVitalsEntry = false,
                ),
            ),
        )

        scrollTo(hasContentDescription(string(R.string.cd_edit_entry)))
        composeRule.onAllNodesWithContentDescription(string(R.string.cd_edit_entry)).assertCountEquals(1)
    }

    @Test
    fun showsTheEmptyPlaceholderWithNoData() {
        setContent(state(hasData = false))

        scrollTo(hasText(string(R.string.message_no_cycle_period)))
        composeRule.onNodeWithText(string(R.string.message_no_cycle_period)).assertIsDisplayed()
    }

    private fun scrollTo(matcher: androidx.compose.ui.test.SemanticsMatcher) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(matcher)
    }

    private fun state(
        hasData: Boolean,
        summary: CyclePeriodSummary = CyclePeriodSummary(),
        today: CycleTodayDisplay = CycleTodayDisplay(today = ANCHOR),
    ) = CycleUiState(
        isLoading = false,
        selectedRange = TimeRange.MONTH,
        selectedDate = ANCHOR,
        display = CycleDisplayState(
            selectedPeriod = DatePeriod(ANCHOR.withDayOfMonth(1), ANCHOR),
            hasData = hasData,
            summary = summary,
            today = today,
        ),
    )

    private fun setContent(
        state: CycleUiState,
        observations: List<CycleObservation> = emptyList(),
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                LazyColumn {
                    cyclePeriodContent(
                        state = state,
                        period = state.display.selectedPeriod,
                        unitFormatter = UnitFormatter(unitSystemProvider = { UnitSystem.METRIC }),
                        dateTimeFormatterProvider = DateTimeFormatterProvider(),
                        observations = observations,
                    )
                }
            }
        }
    }

    private companion object {
        /** A fixed past date, so the period never straddles today. */
        val ANCHOR: LocalDate = LocalDate.of(2026, 6, 23)
    }
}
