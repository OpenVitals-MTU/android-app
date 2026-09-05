package tech.mmarca.openvitals.features.readiness

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.DailyReadinessInsight
import tech.mmarca.openvitals.domain.insights.ReadinessConfidence
import tech.mmarca.openvitals.testing.readinessInsight
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** The readiness panel, reachable now that `readinessInsight()` builds the twenty-two-field insight. */
class DailyReadinessPanelTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTheReadinessVerdictForTheHostDay() {
        setPanel(readinessInsight(statusTitle = "Moderate", recommendation = RECOMMENDATION))

        composeRule.onNodeWithText(string(R.string.dashboard_readiness_title)).assertIsDisplayed()
        composeRule.onNodeWithText(RECOMMENDATION).assertIsDisplayed()
    }

    @Test
    fun theConfidenceLineIsRenderedFromResources() {
        // Both halves come from strings.xml now.
        setPanel(
            readinessInsight(
                confidence = ReadinessConfidence.LOW,
                confidenceReason = "missing_hrv_data",
            ),
        )

        composeRule
            .onNodeWithText(
                string(
                    R.string.readiness_confidence_line,
                    string(R.string.data_confidence_low),
                    string(R.string.readiness_confidence_reason_missing_hrv),
                ),
            )
            .assertIsDisplayed()
    }

    @Test
    fun noSelfLink_theCardOffersTrainingButNotBodyEnergy() {
        // Inside the Body Energy screen the panel must not offer a way back to itself.
        var openedTraining = 0
        setPanel(readinessInsight(), onOpenBodyEnergyDetails = null) { openedTraining++ }

        composeRule.onNodeWithText(string(R.string.dashboard_readiness_body_energy)).assertDoesNotExist()
        composeRule
            .onNodeWithText(string(R.string.dashboard_readiness_training))
            .performScrollTo()
            .performClick()

        assertEquals(1, openedTraining)
    }

    @Test
    fun elsewhereItOffersBothWaysIn() {
        var openedBodyEnergy = 0
        setPanel(readinessInsight(), onOpenBodyEnergyDetails = { openedBodyEnergy++ })

        composeRule
            .onNodeWithText(string(R.string.dashboard_readiness_body_energy))
            .performScrollTo()
            .performClick()

        assertEquals(1, openedBodyEnergy)
    }

    private fun setPanel(
        insight: DailyReadinessInsight,
        onOpenBodyEnergyDetails: (() -> Unit)? = {},
        onOpenTraining: () -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    DailyReadinessPanel(
                        insight = insight,
                        onOpenTrainingReadinessDetails = onOpenTraining,
                        onOpenStressDetails = {},
                        onOpenBodyEnergyDetails = onOpenBodyEnergyDetails,
                    )
                }
            }
        }
    }

    private companion object {
        const val RECOMMENDATION = "A steady session is well within reach today."
    }
}
