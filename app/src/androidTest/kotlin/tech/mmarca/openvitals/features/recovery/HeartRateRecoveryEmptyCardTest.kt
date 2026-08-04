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
 * Partial port of Flutter's
 * `test/features/heart/heart_rate_recovery_screen_test.dart` — the assertion the
 * Flutter case actually turns on.
 *
 * A watch stops recording heart rate the moment a workout ends, so for most
 * people every week is an empty one: the fall cannot be measured from readings
 * nobody took. An empty chart with no explanation reads as a broken app, and the
 * user's conclusion — "this feature does not work" — is wrong in a way that
 * costs them the one thing that would fix it, which is knowing a deliberate test
 * is what the number is measured from.
 *
 * What is NOT covered here is the screen CHOOSING this card over a chart:
 * `HeartRateRecoveryScreen` takes a `HeartRateRecoveryViewModel` and builds its
 * `LazyListScope` inline, so there is no seam to drive that branch from a test.
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
