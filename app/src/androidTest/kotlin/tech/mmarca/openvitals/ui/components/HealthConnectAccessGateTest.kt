package tech.mmarca.openvitals.ui.components

import androidx.compose.material3.Text
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

/**
 * The access gate every metric screen shows instead of its content. Screens wrap
 * themselves in `HealthConnectScreenShell`, which delegates here, so it is pinned once.
 * A screen that forgets to wrap itself shows up in its own content tests.
 */
class HealthConnectAccessGateTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noGateMode_showsTheContent() {
        setGate(mode = null)

        composeRule.onNodeWithText(CONTENT).assertIsDisplayed()
    }

    @Test
    fun insufficientAccess_replacesTheContentAndOffersToGrant() {
        // Replaces rather than overlays: empty charts behind a gate say "no data" when it is "no permission".
        var granted = 0
        setGate(HealthConnectAccessGateMode.INSUFFICIENT_ACCESS, onGrant = { granted++ })

        composeRule.onNodeWithText(CONTENT).assertDoesNotExist()
        composeRule
            .onNodeWithText(string(R.string.health_connect_access_insufficient_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.action_grant_permission)).performClick()

        assertEquals(1, granted)
    }

    @Test
    fun doubleCancelRecovery_sendsTheUserToSettingsRatherThanAskingAgain() {
        // After two declines the dialog stops showing, so the gate offers the settings screen instead.
        var openedSettings = 0
        var granted = 0
        setGate(
            HealthConnectAccessGateMode.DOUBLE_CANCEL_RECOVERY,
            onGrant = { granted++ },
            onOpenSettings = { openedSettings++ },
        )

        composeRule
            .onNodeWithText(string(R.string.health_connect_access_double_cancel_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_open_health_permissions)).performClick()

        assertEquals(1, openedSettings)
        assertEquals("it must not re-request the permission", 0, granted)
    }

    @Test
    fun syncPaused_saysSoRatherThanShowingStaleData() {
        // Paused sync is not missing permission, and stale data would present as current.
        var openedSettings = 0
        setGate(HealthConnectAccessGateMode.SYNC_PAUSED, onOpenSettings = { openedSettings++ })

        composeRule.onNodeWithText(CONTENT).assertDoesNotExist()
        composeRule
            .onNodeWithText(string(R.string.settings_health_connect_sync_title))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText(string(R.string.settings_health_connect_manage_access))
            .performClick()

        assertEquals(1, openedSettings)
    }

    private fun setGate(
        mode: HealthConnectAccessGateMode?,
        onGrant: () -> Unit = {},
        onOpenSettings: () -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                HealthConnectAccessGate(
                    mode = mode,
                    onGrant = onGrant,
                    onOpenHealthConnectSettings = onOpenSettings,
                ) {
                    Text(CONTENT)
                }
            }
        }
    }

    private companion object {
        const val CONTENT = "the screen's own content"
    }
}
