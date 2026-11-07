package tech.mmarca.openvitals.features.watches

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * These rows are the vocabulary of what a paired device can do. A wrist-wellness "Data" view
 * for a bike computer is empty by construction; losing "Sync" strands every ride on the device.
 */
class WatchDeviceRowsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun aWatchOffersBothDataAndSync() {
        setActions(device = watch())

        composeRule.onNodeWithText(string(R.string.settings_watch_action_data)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_watch_action_sync)).assertIsDisplayed()
    }

    @Test
    fun aBikeComputerSyncsButHasNoWellnessDataView() {
        // An Edge records rides, not sleep: its "Data" view would be empty, so it is not offered.
        setActions(device = bikeComputer())

        composeRule.onNodeWithText(string(R.string.settings_watch_action_sync)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.settings_watch_action_data)).assertDoesNotExist()
    }

    @Test
    fun aBikeComputerOffersItsLiveSensorRole() {
        // Broadcast mode is usually only on during a ride, so the role must be detectable here.
        var detected = 0
        composeRule.setContent {
            OpenVitalsTheme {
                LiveSensorSection(
                    device = bikeComputer(),
                    isDetecting = false,
                    foundNothing = false,
                    onDetect = { detected++ },
                )
            }
        }

        composeRule.onNodeWithText(string(R.string.settings_bike_detect_sensors))
            .assertIsEnabled()
            .performClick()

        composeRule.runOnIdle { assertEquals(1, detected) }
    }

    @Test
    fun alarmsAndFindAreLiveActions() {
        // Both reach the watch's settings service. A dead Alarms icon looks like a watch that ignored the request.
        var alarms = 0
        var find = 0
        setActions(device = watch(), onOpenAlarms = { alarms++ }, onToggleFind = { find++ })

        watchActionButton(R.string.settings_watch_action_alarms).assertIsEnabled().performClick()
        watchActionButton(R.string.settings_watch_action_find).assertIsEnabled().performClick()

        composeRule.runOnIdle {
            assertEquals(1, alarms)
            assertEquals(1, find)
        }
    }

    @Test
    fun theOnDeviceSettingsRowOpensTheWatchsOwnSettingsTree() {
        // The row opens the tree's root; a row that does not open leaves the whole tree unreachable.
        var opened = 0
        composeRule.setContent {
            OpenVitalsTheme { OnDeviceSettingsRow(onOpen = { opened++ }) }
        }

        composeRule.onNodeWithText(string(R.string.settings_watch_on_device_settings))
            .assertHasClickAction()
            .performClick()

        composeRule.runOnIdle { assertEquals(1, opened) }
    }

    /** The action button, found by its icon's accessible name. Every action flattens into one semantics parent. */
    private fun watchActionButton(labelRes: Int): SemanticsNodeInteraction =
        composeRule.onNodeWithContentDescription(string(labelRes))

    private fun setActions(
        device: BleSensorDevice,
        onOpenAlarms: () -> Unit = {},
        onToggleFind: () -> Unit = {},
    ) {
        composeRule.setContent {
            OpenVitalsTheme {
                Column {
                    ActionsRow(
                        device = device,
                        // Empty capabilities means "never synced", which the screen reads as show.
                        state = WatchDeviceUiState(device = device),
                        isBikeComputer = device.isBikeComputer,
                        onOpenData = {},
                        onSync = {},
                        onOpenAlarms = onOpenAlarms,
                        onToggleFind = onToggleFind,
                    )
                }
            }
        }
    }

    private fun watch() = device(kind = BleDeviceKind.WATCH, name = "vivoactive 5")

    private fun bikeComputer() = device(kind = BleDeviceKind.BIKE_COMPUTER, name = "Edge 840")

    private fun device(kind: BleDeviceKind, name: String) = BleSensorDevice(
        id = "device-1",
        displayName = name,
        address = "E0:48:24:D5:F7:10",
        bluetoothName = name,
        capabilities = emptySet(),
        enabled = true,
        wheelCircumferenceMm = null,
        addedAt = Instant.parse("2026-06-23T08:00:00Z"),
        kind = kind,
        integration = DeviceIntegration.GARMIN,
    )
}
