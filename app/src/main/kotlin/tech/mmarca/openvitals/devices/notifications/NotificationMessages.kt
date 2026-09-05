package tech.mmarca.openvitals.devices.notifications

/** A captured phone notification, reduced to what the Garmin stack uses. */
data class NotificationMsg(
    /** Stable within a boot, from the notification's key, so an update redraws one card. */
    val id: Long,
    /** The posting app's package name. Sent to the watch as APP_IDENTIFIER. */
    val packageName: String,
    /** The posting app's label, or null. Never used as APP_IDENTIFIER. */
    val appLabel: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val body: String? = null,
    val whenEpochMillis: Long,
    /** Pre-mapped by [NotificationFilter.categoryOrdinal]; the inputs only exist on the capture side. */
    val categoryOrdinal: Int,
    /** A dismissal rather than a post. */
    val removed: Boolean,
    /** The actions the posting app attached, in its own order. */
    val actions: List<NotificationActionMsg> = emptyList(),
    /** Whether it can be cleared. False for an ongoing one, or the dismiss button is dead. */
    val dismissable: Boolean = true,
)

/** One action the posting app attached to a notification. */
data class NotificationActionMsg(
    /** Position in the Android action list, carried to the watch and back verbatim. */
    val index: Int,
    /** What the posting app called it: "Reply", "Mark as read", "Snooze". */
    val title: String,
    /** Whether it expects text; the watch then offers its keyboard. */
    val isReply: Boolean,
    /**
     * Whether firing it from a background service does anything. False for
     * an activity `PendingIntent`, which Android blocks silently.
     */
    val fireableFromBackground: Boolean,
)
