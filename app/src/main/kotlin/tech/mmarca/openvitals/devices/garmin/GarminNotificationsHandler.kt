package tech.mmarca.openvitals.devices.garmin

/**
 * The session's seam to the notification-forwarding logic.
 *
 * [GarminSession] routes the GNCS traffic it decodes — the subscription
 * handshake, control requests and chunk statuses — through this interface,
 * and a session constructed without one (the default) answers the
 * subscription DISABLED, exactly as every sync, find and settings session
 * does.
 *
 * The implementation is sub-milestone 7e's port of the Flutter build's
 * `garmin_notifications_handler.dart` (which also carries `post`, `enabled`
 * and the chunked attribute upload); only the four calls the session makes
 * are part of this contract.
 */
interface GarminNotificationsHandler {

    /**
     * The WATCH's current state — whether it is presently accepting
     * notifications — as reported in its subscription request. Distinct from
     * the phone's willingness to forward, which is the session's reply.
     */
    fun setEnabled(enabled: Boolean)

    /**
     * Announces anything held while the watch was not yet subscribed. Called
     * AFTER the subscription status is sent, never before — an announcement
     * ahead of the status is addressed to a watch that is not listening for
     * it.
     */
    suspend fun flushHeld()

    /** A control request (5034) — the watch asking about a notification. */
    suspend fun handleControl(message: GarminNotificationControl)

    /** The watch's verdict on one uploaded chunk — the transfer's flow control. */
    suspend fun handleDataStatus(message: GarminNotificationDataStatus)
}
