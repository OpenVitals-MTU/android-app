package tech.mmarca.openvitals.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** Port of Flutter's `test/ui/components/permission_callout_test.dart`. */
class PermissionCalloutTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun showsTitleBodyAndGrantAction_withNoDismissUntilOneIsGiven() {
        var granted = false

        composeRule.setContent {
            OpenVitalsTheme {
                PermissionCallout(
                    title = "Steps access",
                    body = "Grant access to read your steps.",
                    onGrant = { granted = true },
                )
            }
        }

        composeRule.onNodeWithText("Steps access").assertIsDisplayed()
        composeRule.onNodeWithText("Grant access to read your steps.").assertIsDisplayed()
        // Without an onDismiss there is no way to decline, so no button.
        composeRule.onNodeWithText(string(R.string.action_not_now)).assertDoesNotExist()

        composeRule.onNodeWithText(string(R.string.action_grant_permission)).performClick()

        assertTrue(granted)
    }

    @Test
    fun showsDismissWhenProvided_andHonoursACustomActionLabel() {
        var dismissed = false

        composeRule.setContent {
            OpenVitalsTheme {
                PermissionCallout(
                    title = "Steps access",
                    body = "Grant access to read your steps.",
                    actionLabel = "Allow",
                    onGrant = {},
                    onDismiss = { dismissed = true },
                )
            }
        }

        composeRule.onNodeWithText("Allow").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_not_now)).performClick()

        assertTrue(dismissed)
    }
}
