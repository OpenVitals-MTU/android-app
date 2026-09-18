package tech.mmarca.openvitals.devices.garmin

/** Where a one-shot send is, for the screen's progress line. */
enum class GarminSendStage { CONNECTING, SENDING }

/** How a one-shot send to the watch ended. The wording is the screen's. */
sealed interface GarminSendFileResult {
    data object Sent : GarminSendFileResult

    /** The watch answered and said no. */
    data class Refused(val reason: GarminUploadRefusal) : GarminSendFileResult

    /** The watch went quiet part-way. */
    data object NoAnswer : GarminSendFileResult

    /** Something else holds the radio. [holder] is its lease tag. */
    data class Busy(val holder: String) : GarminSendFileResult
    data object SyncRunning : GarminSendFileResult
    data object RecordingActive : GarminSendFileResult

    /** No connection: out of range, Bluetooth off, or a missing permission. */
    data object Unreachable : GarminSendFileResult
    data object HandshakeTimeout : GarminSendFileResult
    data object LinkLost : GarminSendFileResult
}
