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
 * Port of Flutter's
 * `test/features/readiness/training_readiness_details_screen_test.dart`.
 *
 * This screen exists to justify a number the app made up about how hard someone
 * should train today, so the cases worth pinning are the ones where the
 * justification is thin: a verdict band that must read "needs more data" however
 * flattering the score is, and a signals list that has to say it had no signals
 * rather than render as an empty card.
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
        // The insight is never short of factors — it is short of factors this
        // screen is allowed to cite. A silently empty "Signals used" card would
        // present the score as if it rested on something.
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
        // The dangerous case. An unknown state still carries a number, and a
        // number banded as "Strong" is an instruction to go and train hard on
        // the strength of data the app knows it does not have.
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
                        // A fixed past day: the screen's forward arrow and its
                        // date header must not depend on the day the suite runs.
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
