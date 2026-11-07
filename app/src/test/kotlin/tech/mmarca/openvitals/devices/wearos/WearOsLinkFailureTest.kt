package tech.mmarca.openvitals.devices.wearos

import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

class WearOsLinkFailureTest {

    @Test
    fun `every failure names a status and a sentence`() {
        val cases = listOf(
            WearOsLinkFailure.BluetoothOff() to WearOsAppStatus.BLUETOOTH_OFF,
            WearOsLinkFailure.NotBonded() to WearOsAppStatus.NOT_PAIRED,
            WearOsLinkFailure.NoAnswer() to WearOsAppStatus.NO_ANSWER,
            WearOsLinkFailure.PendingConfirmation() to WearOsAppStatus.PENDING_CONFIRMATION,
            WearOsLinkFailure.Unauthorized(WearLinkProtocol.UnauthorizedReason.MISMATCH) to WearOsAppStatus.UNAUTHORIZED_MISMATCH,
            WearOsLinkFailure.Unauthorized(WearLinkProtocol.UnauthorizedReason.BLOCKED) to WearOsAppStatus.UNAUTHORIZED_BLOCKED,
            WearOsLinkFailure.VersionMismatch(2, 3) to WearOsAppStatus.UPDATE_WATCH,
            WearOsLinkFailure.VersionMismatch(5, 6) to WearOsAppStatus.UPDATE_PHONE,
            WearOsLinkFailure.Protocol("garbage") to WearOsAppStatus.PROTOCOL_ERROR,
            WearOsLinkFailure.Refused(WearLinkProtocol.ErrorCode.BUSY) to WearOsAppStatus.NO_ANSWER,
        )

        for ((failure, status) in cases) {
            assertEquals(failure.message, status, failure.toAppStatus())
            assertEquals(ScreenError.Text(status.messageRes()), failure.toScreenError())
        }
        assertEquals(R.string.settings_watch_wearos_status_pending, WearOsAppStatus.PENDING_CONFIRMATION.messageRes())
    }
}
