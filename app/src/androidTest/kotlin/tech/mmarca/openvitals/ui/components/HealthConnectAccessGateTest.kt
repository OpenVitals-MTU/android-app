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
 * The access gate every metric screen shows instead of its content.
 *
 * Flutter tests this once per screen — `Hydration screen shows the access gate
 * when permission missing`, and the same case again for mindfulness, nutrition,
 * caffeine, cycle and heart. In Kotlin those screens do not each own a gate:
 * they wrap their content in `HealthConnectScreenShell`, which delegates here.
 * So the behaviour is pinned once, where it lives, rather than six times over
 * six screens that would all be exercising this same composable.
 *
 * What that leaves uncovered is a screen that forgets to wrap itself at all.
 * That is a wiring mistake rather than a gate mistake, and the per-screen
 * content tests are where it shows up — a screen with no shell renders its
 * content when it should be showing a gate.
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
        // Replaces rather than overlays: a screen that renders empty charts
        // behind a gate is telling the user they have no data, when what they
        // have is no permission.
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
        // Asking a third time is how a permission dialog stops being shown at
        // all. Once the user has declined twice, the only route left is the
        // Health Connect settings screen, so the gate offers that instead.
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
        // Paused sync is not missing permission, and showing whatever was last
        // read would present it as current.
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
