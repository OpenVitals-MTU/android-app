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
 * Port of the rendering cases of Flutter's
 * `test/features/devicesync/device_sync_screen_test.dart`.
 *
 * Phone-to-phone sync has no second chance to explain itself: the two devices
 * are in the same room and the user is watching both. A step that shows a
 * spinner when it has stopped scanning, or a success tick over an aborted
 * session, sends someone away believing their data moved when it did not.
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
        // Both roles need Bluetooth. Letting the user pick one and fail later
        // wastes the trip to the other phone.
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
        // Not a permanent empty state: scanning windows close, and the phones
        // are often a second out of step with each other.
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
        // An aborted session still produces a report. Rendering the success
        // path from it would tell the user their records merged when the
        // transfer stopped halfway.
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
        // Copy alone puts the report on a clipboard the user then has to find
        // somewhere to paste. Share is what actually gets it off the phone, to
        // a messenger or an email, which is the whole point of producing one.
        // (Flutter also offers "Save report"; Kotlin deliberately does not —
        // saving on Android lands in app documents, out of reach.)
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
        // The control for the case above: both actions hang off the report
        // text, so a report that produced none must not leave two buttons that
        // would copy and share an empty string.
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
        // "Something went wrong" sends the user looking for a problem with
        // their data. "Could not connect, make sure it's nearby" sends them to
        // the other phone, which is where the problem is.
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
