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
 * Port of the card-rendering cases of Flutter's
 * `test/features/bodyenergy/body_energy_details_screen_test.dart`.
 *
 * Body Energy is a number the app invents about someone's body from signals it
 * only partly has, so the cases that matter are the ones where it has to admit
 * that: a day with nothing to compute from must say so rather than draw a
 * confident flat line, the inputs card must name the signal it went without, and
 * "What moved it" must not go blank when nothing did.
 *
 * The screen itself hands `bodyEnergyContent` a `DailyReadinessViewModel`, so
 * the composition of the cards is not reachable here; each card is exercised on
 * the display state the production mapper builds.
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
        // The hour row only exists inside the timeline chart, so a day that ends
        // at 24:00 is the cheapest proof the chart itself composed.
        composeRule.onNodeWithText("24:00").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.body_energy_timeline_no_data)).assertDoesNotExist()
    }

    @Test
    fun withNoTimelineAtAllTheCardShowsNoScoreRatherThanAZero() {
        // Nothing was loaded for the day. The score has to read as absent, not
        // as a measured nought — a battery drawn at zero is a claim about the
        // user's body that nobody made.
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
        // The subtler failure. Body Energy is a chain, so a day the model could
        // not compute still carries yesterday's closing score and prints it —
        // and printed alone that 50 is indistinguishable from a measured 50. The
        // confidence line is the only thing standing between the two, so it has
        // to say "No data" and say why.
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
        // A low-confidence day still shows a number, and a number with no
        // caveat next to it is indistinguishable from a measured one.
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
        // The one place a user can find out that the estimate was made without
        // their resting heart rate. Without it, a lower score looks like a worse
        // day rather than a thinner day of data.
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
        // A day where nothing stood out still has to answer "what moved it" —
        // an empty card would read as a rendering failure rather than as a
        // quiet day — and the method card is the only place the estimate
        // explains itself at all.
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
        /** No day was loaded at all — the nullable-receiver mapper's own case. */
        val NO_TIMELINE: BodyEnergyTimeline? = null
    }
}
