package tech.mmarca.openvitals.features.bodyenergy

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.BodyEnergyConfidence
import tech.mmarca.openvitals.domain.insights.BodyEnergyReasonCode
import tech.mmarca.openvitals.domain.insights.BodyEnergyTimeline
import tech.mmarca.openvitals.testing.bodyEnergyInputSummary
import tech.mmarca.openvitals.testing.bodyEnergyTimeline
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Body Energy is a number the app invents from partial signals, so the cases that matter are
 * where it must admit that: no data says so, the inputs card names the missing signal,
 * and "What moved it" does not go blank. Each card is exercised on the mapper's display state.
 */
class BodyEnergyCardsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theDayCardRendersTheTimelineOnceLoaded() {
        setContent {
            BodyEnergyCard(display = bodyEnergyTimeline().toBodyEnergyDisplayState())
        }

        composeRule.onNodeWithText(string(R.string.screen_body_energy)).assertIsDisplayed()
        composeRule.onAllNodesWithText("62").onFirst().assertIsDisplayed()
        composeRule.onNodeWithText("+14").assertIsDisplayed()
        composeRule.onNodeWithText("-2").assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.body_energy_timeline_day_title))
            .performScrollTo()
            .assertIsDisplayed()
        // The hour row only exists inside the timeline chart, so "24:00" proves the chart composed.
        composeRule.onNodeWithText("24:00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.body_energy_timeline_no_data)).assertDoesNotExist()
    }

    @Test
    fun withNoTimelineAtAllTheCardShowsNoScoreRatherThanAZero() {
        // The score must read as absent, not as a measured nought.
        setContent { BodyEnergyCard(display = NO_TIMELINE.toBodyEnergyDisplayState()) }

        composeRule.onAllNodesWithText("--").onFirst().assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.body_energy_confidence_no_data))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.body_energy_timeline_no_data))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("24:00").assertDoesNotExist()
    }

    @Test
    fun aDayWithNothingToComputeFromStillNamesItsScoreAsUncomputed() {
        // A day the model could not compute still carries yesterday's score. The confidence line
        // is the only thing separating it from a measured 50.
        setContent {
            BodyEnergyCard(
                display = BodyEnergyTimeline
                    .empty(
                        date = LocalDate.of(2026, 6, 23),
                        reason = "Heart rate or sleep data is needed for Body Energy.",
                        reasonCode = BodyEnergyReasonCode.NEEDS_HEART_RATE_OR_SLEEP,
                        inputSummary = bodyEnergyInputSummary(
                            heartRateSampleCount = 0,
                            sleepSessionCount = 0,
                        ),
                    )
                    .toBodyEnergyDisplayState(),
            )
        }

        composeRule
            .onNodeWithText(string(R.string.body_energy_confidence_no_data))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.body_energy_reason_needs_hr_or_sleep))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.body_energy_timeline_no_data))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText("24:00").assertDoesNotExist()
    }

    @Test
    fun aLowConfidenceDayNamesWhyItIsLowRatherThanJustScoringLower() {
        // A number with no caveat is indistinguishable from a measured one.
        setContent {
            BodyEnergyCard(
                display = bodyEnergyTimeline(
                    confidence = BodyEnergyConfidence.LOW,
                    confidenceReasonCode = BodyEnergyReasonCode.INCOMPLETE_CALIBRATION,
                ).toBodyEnergyDisplayState(),
            )
        }

        composeRule
            .onNodeWithText(string(R.string.body_energy_confidence_low))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.body_energy_reason_incomplete_calibration))
            .assertIsDisplayed()
    }

    @Test
    fun theInputsCardNamesTheSignalTheDayWentWithout() {
        // The one place a user learns the estimate was made without their resting heart rate.
        setContent {
            BodyEnergyInputsCard(
                display = bodyEnergyTimeline(
                    inputSummary = bodyEnergyInputSummary(hasRestingHeartRate = false),
                ).toBodyEnergyDisplayState(),
            )
        }

        composeRule.onNodeWithText(string(R.string.body_energy_inputs_title)).assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.body_energy_input_resting_hr))
            .performScrollTo()
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.body_energy_input_missing)).assertIsDisplayed()
    }

    @Test
    fun theExplainerCardsSayWhatMovedItAndHowItIsEstimated() {
        // A quiet day still has to answer "what moved it", or the card reads as a rendering failure.
        setContent {
            Column {
                BodyEnergyReasonsCard(reasons = emptyList(), hasTimeline = false)
                BodyEnergyCalculationCard()
            }
        }

        composeRule.onNodeWithText(string(R.string.body_energy_why_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.body_energy_why_empty)).assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.body_energy_calculation_title))
            .performScrollTo()
            .assertIsDisplayed()
    }

    private fun setContent(content: @Composable () -> Unit) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) { content() }
            }
        }
    }

    private companion object {
        /** No day was loaded at all. */
        val NO_TIMELINE: BodyEnergyTimeline? = null
    }
}
