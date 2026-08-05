package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import java.time.Instant
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import androidx.compose.ui.test.onNodeWithContentDescription
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Port of the rendering cases of Flutter's
 * `test/features/cycle/cycle_screen_test.dart`.
 *
 * The derivations are covered on the JVM by `CyclePresentationMapperTest`; what
 * is asserted here is that the content draws the branch the display state
 * chose. The permission-gate case from that file lives in
 * `HealthConnectAccessGateTest` — see its doc for why it is pinned once.
 */
class CyclePeriodContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersSummaryAndObservationsOnceLoaded() {
        setContent(
            state(hasData = true, summary = CyclePeriodSummary(periodDays = 5)),
        )

        // The label appears on both the summary card and the statistics row
        // below it, so the first is the summary the loaded branch owes.
        composeRule.onAllNodesWithText(string(R.string.metric_period_days))
            .onFirst()
            .assertIsDisplayed()
    }

    @Test
    fun showsThePredictionCardWhenStatisticsCarryAWindow() {
        setContent(
            state(
                hasData = true,
                statistics = CycleStatistics(
                    currentCycleDay = 12,
                    predictedWindows = listOf(ANCHOR.plusDays(10)..ANCHOR.plusDays(12)),
                ),
            ),
        )

        composeRule.onNodeWithText(string(R.string.cycle_prediction_next_period)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.cycle_stat_cycle_day)).assertIsDisplayed()
    }

    @Test
    fun onlyAnOpenVitalsObservationGetsTheEditPencil() {
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

        composeRule.onNodeWithContentDescription(string(R.string.cd_edit_entry)).assertIsDisplayed()
    }

    @Test
    fun showsTheEmptyPlaceholderWithNoData() {
        setContent(state(hasData = false))

        composeRule.onNodeWithText(string(R.string.message_no_cycle_period)).assertIsDisplayed()
    }

    private fun state(
        hasData: Boolean,
        summary: CyclePeriodSummary = CyclePeriodSummary(),
        statistics: CycleStatistics? = null,
    ) = CycleUiState(
        isLoading = false,
        selectedRange = TimeRange.MONTH,
        selectedDate = ANCHOR,
        statistics = statistics,
        display = CycleDisplayState(
            selectedPeriod = DatePeriod(ANCHOR.withDayOfMonth(1), ANCHOR),
            hasData = hasData,
            summary = summary,
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
