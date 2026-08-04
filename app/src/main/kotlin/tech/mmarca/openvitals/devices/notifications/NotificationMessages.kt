package tech.mmarca.openvitals.devices.notifications

/**
 * A captured phone notification, reduced to what the Garmin stack can use.
 *
 * These types were the Pigeon wire messages of the Flutter build
 * (`Messages.g.kt` / `messages.g.dart`); the codec is gone, but the shape is
 * kept verbatim because everything downstream — the filter, the store, the
 * GNCS action mapping — was written against it.
 */
data class NotificationMsg(
    /**
     * Stable within a boot, derived from the notification's own key so an
     * update to the same notification carries the same id — which is what
     * makes the watch redraw one card instead of buzzing twice.
     */
    val id: Long,
    /** The posting app's package name. Sent to the watch as APP_IDENTIFIER. */
    val packageName: String,
    /**
     * The posting app's human-readable label, or null when the launcher query
     * cannot resolve it. Never used as APP_IDENTIFIER — some watch faces
     * resolve an icon from the package name and would fail on a label.
     */
    val appLabel: String? = null,
    val title: String? = null,
    val subtitle: String? = null,
    val body: String? = null,
    val whenEpochMillis: Long,
    /**
     * Pre-mapped by [NotificationFilter.categoryOrdinal] to
     * `GarminNotificationCategory`'s ordinal. Mapped on the capture side
     * because it is derived from Android's own category constants and the
     * posting package, both of which only exist there.
     */
    val categoryOrdinal: Int,
    /**
     * True when this is a dismissal rather than a post — the watch is told to
     * withdraw the card.
     */
    val removed: Boolean,
    /** The actions the posting app attached, in its own order. */
    val actions: List<NotificationActionMsg> = emptyList(),
    /**
     * Whether the notification can be cleared at all. False for an ongoing
     * one — offering a dismiss that silently fails is the dead button this
     * feature exists to remove.
     */
    val dismissable: Boolean = true,
)

/** One action the posting app attached to a notification. */
data class NotificationActionMsg(
    /**
     * Position in the Android notification's own action list. Carried to the
     * watch and back verbatim, so the phone never has to re-derive which
     * action was meant — Gadgetbridge re-walks the list counting action types
     * to map one back, and its own comment calls that fragile.
     */
    val index: Int,
    /** What the posting app called it: "Reply", "Mark as read", "Snooze". */
    val title: String,
    /**
     * Whether it expects text (a `RemoteInput`), in which case the watch
     * offers its keyboard or canned replies and sends the result back.
     */
    val isReply: Boolean,
    /**
     * Whether invoking it from a background service actually does anything.
     *
     * False for an ACTIVITY `PendingIntent`. Some apps — a stock SMS app among
     * them — publish a "Reply" that opens their compose screen with the text
     * prefilled rather than sending it, and Android blocks background activity
     * launches outright, so firing one throws nothing and does nothing.
     * Offering it on the wrist would be exactly the dead button this feature
     * exists to remove.
     */
    val fireableFromBackground: Boolean,
)
