package tech.mmarca.openvitals.features.readiness

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollToNode
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.DailyReadinessInsight
import tech.mmarca.openvitals.domain.insights.ReadinessFactorKind
import tech.mmarca.openvitals.domain.insights.ReadinessState
import tech.mmarca.openvitals.testing.readinessFactor
import tech.mmarca.openvitals.testing.readinessInsight
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * The verdict band must read "needs more data" however flattering the score, and the
 * signals list must say it had none rather than render empty.
 */
class TrainingReadinessDetailsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun rendersTheScoreVerdictAndTheTrainingSignalsBehindIt() {
        setContent(
            readinessInsight(
                trainingReadinessScore = 71,
                factors = listOf(
                    readinessFactor(
                        kind = ReadinessFactorKind.HRV_NORMAL,
                        label = HRV_LABEL,
                        detail = HRV_DETAIL,
                    ),
                ),
            ),
        )

        composeRule.onNodeWithText(string(R.string.screen_training_readiness)).assertIsDisplayed()
        composeRule.onNodeWithText("71/100").assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.readiness_details_score_steady))
            .assertIsDisplayed()
        scrollTo(string(R.string.readiness_details_signals_used))
        composeRule.onNodeWithText("$HRV_LABEL: $HRV_DETAIL").assertIsDisplayed()
    }

    @Test
    fun withNoTrainingSideSignalsItSaysSoRatherThanShowingAnEmptyCard() {
        // An empty "Signals used" card would present the score as if it rested on something.
        setContent(
            readinessInsight(
                factors = listOf(
                    readinessFactor(kind = ReadinessFactorKind.HYDRATION_LOW, label = "Hydration"),
                    readinessFactor(kind = ReadinessFactorKind.NUTRITION_LOGGED, label = "Nutrition"),
                ),
            ),
        )

        scrollTo(string(R.string.training_readiness_details_no_signals))
        composeRule
            .onNodeWithText(string(R.string.training_readiness_details_no_signals))
            .assertIsDisplayed()
    }

    @Test
    fun anUnknownStateReadsAsNeedsMoreDataHoweverHighTheScore() {
        // An unknown state banded as "Strong" is an instruction to train hard on data the app does not have.
        setContent(
            readinessInsight(
                state = ReadinessState.UNKNOWN,
                trainingReadinessScore = 88,
            ),
        )

        composeRule
            .onNodeWithText(string(R.string.readiness_details_score_needs_more_data))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.readiness_details_score_strong))
            .assertDoesNotExist()
    }

    private fun scrollTo(text: String) {
        composeRule.onNode(hasScrollAction()).performScrollToNode(hasText(text))
    }

    private fun setContent(insight: DailyReadinessInsight) {
        composeRule.setContent {
            OpenVitalsTheme {
                ReadinessScoreDetailsContent(
                    state = DailyReadinessUiState(
                        // A fixed past day, so the forward arrow and date header do not depend on the run date.
                        selectedDate = LocalDate.of(2026, 6, 23),
                        insight = insight,
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

    private companion object {
        const val HRV_LABEL = "HRV"
        const val HRV_DETAIL = "Within your usual range"
    }
}
