package tech.mmarca.openvitals.features.devicesync

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.devicesync.protocol.SyncReport
import tech.mmarca.openvitals.testing.string
import tech.mmarca.openvitals.ui.theme.OpenVitalsTheme

/**
 * Phone-to-phone sync has no second chance to explain itself. A spinner after scanning stopped,
 * or a success tick over an aborted session, sends someone away believing their data moved.
 */
class DeviceSyncStepsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun theRoleStepOffersHostAndGuest() {
        setContent { DeviceSyncRoleStep(DeviceSyncState(), onChooseHost = {}, onChooseGuest = {}) }

        composeRule.onNodeWithText(string(R.string.device_sync_host_option)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.device_sync_guest_option)).assertIsDisplayed()
    }

    @Test
    fun theRoleStepBannersAnUnavailableRadio() {
        // Both roles need Bluetooth.
        setContent {
            DeviceSyncRoleStep(
                DeviceSyncState(bluetoothUnavailable = true),
                onChooseHost = {},
                onChooseGuest = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.device_sync_bluetooth_off)).assertIsDisplayed()
    }

    @Test
    fun aFinishedScanWithNoDevicesOffersARescan() {
        // Not a permanent empty state: scanning windows close.
        setContent {
            DeviceSyncScanStep(
                state = DeviceSyncState(scanning = false, devices = emptyList()),
                onSelectDevice = {},
                onRescan = {},
                onCancel = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.device_sync_no_devices)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.device_sync_rescan)).assertIsDisplayed()
    }

    @Test
    fun aScanInProgressDoesNotYetOfferARescan() {
        setContent {
            DeviceSyncScanStep(
                state = DeviceSyncState(scanning = true, devices = emptyList()),
                onSelectDevice = {},
                onRescan = {},
                onCancel = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.device_sync_no_devices)).assertDoesNotExist()
    }

    @Test
    fun theReportStepShowsAFailureNotASuccessCheckmark() {
        // An aborted session still produces a report; rendering the success path from it lies.
        var done = 0
        setContent {
            DeviceSyncReportStep(
                state = DeviceSyncState(report = report(completed = false)),
                onDone = { done++ },
            )
        }

        composeRule.onNodeWithText(string(R.string.device_sync_error_heading)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.device_sync_done)).performClick()

        assertEquals(1, done)
    }

    @Test
    fun aCompletedReportShowsWhatWasImported() {
        setContent {
            DeviceSyncReportStep(
                state = DeviceSyncState(report = report(completed = true, imported = 42)),
                onDone = {},
            )
        }

        composeRule
            .onNodeWithText(string(R.string.device_sync_report_heading, 42))
            .assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.device_sync_error_heading)).assertDoesNotExist()
    }

    @Test
    fun aFinishedSyncOffersToShareTheReportNotOnlyToCopyIt() {
        // Share is what gets the report off the phone. Kotlin has no "Save report": app documents are out of reach.
        setContent {
            DeviceSyncReportStep(
                state = DeviceSyncState(
                    report = report(completed = true, imported = 4),
                    reportText = "OpenVitals sync report\nImported: 4\n",
                ),
                onDone = {},
            )
        }

        val share = string(R.string.device_sync_share_report)
        composeRule.onAllNodes(hasScrollAction()).onFirst().performScrollToNode(hasText(share))
        composeRule.onNodeWithText(share).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.device_sync_copy_report)).assertIsDisplayed()
    }

    @Test
    fun aReportWithNothingToShareOffersNeitherAction() {
        // A report with no text must not leave two buttons that copy and share an empty string.
        setContent {
            DeviceSyncReportStep(
                state = DeviceSyncState(report = report(completed = true, imported = 4)),
                onDone = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.device_sync_share_report)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.device_sync_copy_report)).assertDoesNotExist()
    }

    @Test
    fun aConnectTimeoutSaysTheConnectionFailedRatherThanSomethingGeneric() {
        // "Could not connect, make sure it's nearby" sends the user to the other phone, where the problem is.
        setContent {
            DeviceSyncReportStep(
                state = DeviceSyncState(error = DeviceSyncError.CONNECT_TIMEOUT),
                onDone = {},
            )
        }

        composeRule.onNodeWithText(string(R.string.device_sync_error_connect)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.device_sync_error_generic)).assertDoesNotExist()
    }

    private fun report(completed: Boolean, imported: Int = 0) = SyncReport(
        completed = completed,
        peerDeviceName = "Pixel 7",
        negotiatedTypes = listOf("Steps"),
        itemsSent = 10,
        itemsReceived = imported,
        imported = imported,
        duplicateSkipped = 0,
        typeSummaries = emptyList(),
    )

    private fun setContent(content: @androidx.compose.runtime.Composable () -> Unit) {
        composeRule.setContent { OpenVitalsTheme { content() } }
    }
}
