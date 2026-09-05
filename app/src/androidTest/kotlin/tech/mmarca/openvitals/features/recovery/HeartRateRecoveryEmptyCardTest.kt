package tech.mmarca.openvitals.features.recovery

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * A watch stops recording heart rate when a workout ends, so most weeks are empty. The card
 * must explain that a deliberate test is what the number is measured from.
 * The screen choosing this card over a chart has no seam to test.
 */
class HeartRateRecoveryEmptyCardTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theEmptyPeriodCardSaysBothThatThereIsNothingAndWhy() {
        composeRule.setContent {
            OpenVitalsTheme { HeartRateRecoveryEmptyCard() }
        }

        composeRule.onNodeWithText(string(R.string.heart_rate_recovery_empty)).assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.heart_rate_recovery_empty_watch))
            .assertIsDisplayed()
    }
}
