package tech.mmarca.openvitals.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/** Back used to stop a running import without a word. */
class ConfirmLeaveWhileImportingTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun backDuringAnImportAsksFirst_andStayingKeepsTheScreen() {
        composeRule.setContent {
            OpenVitalsTheme {
                ConfirmLeaveWhileImporting(importing = true)
                Text("importing")
            }
        }

        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }

        composeRule.onNodeWithText(string(R.string.import_leave_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.import_leave_stay)).performClick()
        composeRule.onNodeWithText("importing").assertIsDisplayed()
    }
}
