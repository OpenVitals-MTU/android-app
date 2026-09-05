package tech.mmarca.openvitals.devices.garmin

/**
 * The session's seam to notification forwarding. A session without one
 * answers the subscription DISABLED. Only the calls the session makes are here.
 */
interface GarminNotificationsHandler {

    /** The watch's own state: whether it accepts notifications. Not the phone's willingness. */
    fun setEnabled(enabled: Boolean)

    /** Announces anything held before the watch subscribed. Called after the status is sent. */
    suspend fun flushHeld()

    /** A control request (5034) — the watch asking about a notification. */
    suspend fun handleControl(message: GarminNotificationControl)

    /** The watch's verdict on one uploaded chunk — the transfer's flow control. */
    suspend fun handleDataStatus(message: GarminNotificationDataStatus)
}
