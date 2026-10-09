package tech.mmarca.openvitals.devices.wearos

import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

/**
 * Why the phone could not get what it wanted from the watch, one case per
 * thing the user can do about it. A missing Bluetooth permission is not
 * here: it stays a `SecurityException`, which the app turns into
 * `ScreenError.PermissionDenied` and a grant affordance.
 */
sealed class WearOsLinkFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    /** The phone's Bluetooth is off. */
    class BluetoothOff : WearOsLinkFailure("Bluetooth is off")

    /** No bonded device matches the registered watch. */
    class NotBonded : WearOsLinkFailure("No bonded Wear OS watch matches")

    /** The watch is off, out of range, or its app is not installed or not running. */
    class NoAnswer(cause: Throwable? = null) : WearOsLinkFailure("The watch did not answer", cause)

    /** The watch is asking the wearer whether to allow this phone. */
    class PendingConfirmation : WearOsLinkFailure("Waiting for the wearer to allow this phone")

    /** The watch refused this phone's token. */
    class Unauthorized(val reason: WearLinkProtocol.UnauthorizedReason) : WearOsLinkFailure("Unauthorized: ${reason.word}")

    /** The watch speaks versions [min] to [max]; this phone speaks another. */
    class VersionMismatch(val min: Int, val max: Int) : WearOsLinkFailure("Protocol version mismatch: watch speaks $min..$max")

    /** The watch answered something the protocol does not allow. */
    class Protocol(detail: String) : WearOsLinkFailure("Protocol error: $detail")

    /** The watch's refusal code, such as busy. */
    class Refused(val code: WearLinkProtocol.ErrorCode) : WearOsLinkFailure("Refused: ${code.word}")

    /** The status row this failure stands for. */
    fun toAppStatus(): WearOsAppStatus = when (this) {
        is BluetoothOff -> WearOsAppStatus.BLUETOOTH_OFF
        is NotBonded -> WearOsAppStatus.NOT_PAIRED
        is NoAnswer -> WearOsAppStatus.NO_ANSWER
        is PendingConfirmation -> WearOsAppStatus.PENDING_CONFIRMATION
        is Unauthorized -> when (reason) {
            WearLinkProtocol.UnauthorizedReason.MISMATCH -> WearOsAppStatus.UNAUTHORIZED_MISMATCH
            WearLinkProtocol.UnauthorizedReason.BLOCKED -> WearOsAppStatus.UNAUTHORIZED_BLOCKED
        }
        is VersionMismatch -> if (WearLinkProtocol.VERSION < min) WearOsAppStatus.UPDATE_PHONE else WearOsAppStatus.UPDATE_WATCH
        is Protocol -> WearOsAppStatus.PROTOCOL_ERROR
        is Refused -> WearOsAppStatus.NO_ANSWER
    }

    /** The sentence the screen shows for this failure. */
    fun toScreenError(): ScreenError = ScreenError.Text(toAppStatus().messageRes())
}

/** The sentence for a status that is not success. */
fun WearOsAppStatus.messageRes(): Int = when (this) {
    WearOsAppStatus.BLUETOOTH_OFF -> R.string.settings_watch_wearos_status_bluetooth_off
    WearOsAppStatus.NOT_PAIRED -> R.string.settings_watch_wearos_bt_not_paired
    WearOsAppStatus.NO_ANSWER -> R.string.settings_watch_wearos_app_no_answer
    WearOsAppStatus.PENDING_CONFIRMATION -> R.string.settings_watch_wearos_status_pending
    WearOsAppStatus.UNAUTHORIZED_MISMATCH -> R.string.settings_watch_wearos_status_unauthorized_mismatch
    WearOsAppStatus.UNAUTHORIZED_BLOCKED -> R.string.settings_watch_wearos_status_unauthorized_blocked
    WearOsAppStatus.UPDATE_WATCH -> R.string.settings_watch_wearos_status_update_watch
    WearOsAppStatus.UPDATE_PHONE -> R.string.settings_watch_wearos_status_update_phone
    WearOsAppStatus.PROTOCOL_ERROR -> R.string.settings_watch_wearos_status_protocol
    WearOsAppStatus.APP_RUNNING -> R.string.settings_watch_wearos_app_running
}
