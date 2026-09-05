package tech.mmarca.openvitals.features.readiness

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
import tech.mmarca.openvitals.domain.insights.PhysiologicalStressEstimate
import tech.mmarca.openvitals.testing.readinessInsight
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Physiological stress is an HRV-and-heart-rate strain estimate, easily over-read. The card
 * carries its level and confidence together, and the inputs list says when it had nothing.
 */
class StressDetailsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theStressCardRendersItsScoreLevelAndConfidenceOnceLoaded() {
        setContent(
            readinessInsight().physiologicalStress
                .copy(confidenceReason = "hrv_resting_hr_average_hr"),
        )

        composeRule.onNodeWithText(string(R.string.screen_stress_tracking)).assertIsDisplayed()
        composeRule.onNodeWithText("28/100").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.stress_label_low)).assertIsDisplayed()
        // Level and confidence travel together.
        composeRule
            .onNodeWithText(
                "${string(R.string.cardio_load_confidence_high)} · " +
                    string(R.string.stress_reason_all_signals),
            )
            .assertIsDisplayed()
    }

    @Test
    fun anEstimateWithNoListedInputsSaysSoInsteadOfShowingAnEmptyCard() {
        // No factor lines and no coverage lines, the ordinary case for thin HRV data. Both cards must name the absence.
        setContent()

        scrollTo(string(R.string.stress_details_no_inputs))
        composeRule.onNodeWithText(string(R.string.stress_details_no_inputs)).assertIsDisplayed()
        scrollTo(string(R.string.stress_details_no_data_coverage))
        composeRule
            .onNodeWithText(string(R.string.stress_details_no_data_coverage))
            .assertIsDisplayed()
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    private fun setContent(
        stress: PhysiologicalStressEstimate = readinessInsight().physiologicalStress,
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                StressDetailsContent(
                    state = DailyReadinessUiState(
                        selectedDate = LocalDate.of(2026, 6, 23),
                        insight = readinessInsight().copy(physiologicalStress = stress),
                        isLoading = false,
                    ),
                    canGoForward = true,
                    onPreviousDay = {},
                    onNextDay = {},
                    onOpenCalendar = {},
                )
            }
        }
    }
}
