package tech.mmarca.openvitals.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** A cancelled unlock prompt used to leave a blank screen with no way on. */
class AppLockedScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theLockedScreenSaysSoAndOffersUnlock() {
        var asked = 0
        composeRule.setContent { OpenVitalsTheme { AppLockedScreen(onUnlock = { asked += 1 }) } }

        composeRule.onNodeWithText(string(R.string.app_lock_locked_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.app_lock_unlock_action)).performClick()

        assertEquals(1, asked)
    }
}
