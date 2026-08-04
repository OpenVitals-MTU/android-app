package tech.mmarca.openvitals.devices.notifications

import android.content.Context
import android.content.SharedPreferences

/**
 * Process-wide state shared between the listener service and the forwarder.
 *
 * A Kotlin `object`, deliberately: the listener service is bound by the system
 * long before any UI (or any Hilt-injected consumer) exists, and in the
 * Flutter build the same statics were what let the service and a headless
 * engine share one buffer. Keeping the shape means keeping the guarantees.
 *
 * The BUFFER is memory-only and bounded. Notification text is never written to
 * disk — it lives here until the forwarder drains it and nowhere else. The
 * CONFIG is persisted, because the service is bound long before any UI has run
 * (after a reboot, or after the process was killed) and has to be able to
 * filter without asking anyone.
 *
 * The prefs FILE and KEY names are exactly the Flutter build's
 * (`openvitals_notification_listener` / `forwarding_enabled` /
 * `blocked_packages` / `watch_address`): that file was written by the same
 * package name into the same app data directory, so an in-place upgrade
 * install inherits the native-side configuration with no migration at all.
 * The Flutter-era `forwarder_callback_handle` and `diagnostics` keys are
 * simply never read again — the headless engine is gone and diagnostics come
 * from `BuildConfig.OPENVITALS_DIAGNOSTICS`.
 */
object NotificationStore {

    private const val PREFS = "openvitals_notification_listener"
    private const val KEY_ENABLED = "forwarding_enabled"
    private const val KEY_BLOCKED = "blocked_packages"
    private const val KEY_WATCH_ADDRESS = "watch_address"

    /**
     * How many notifications wait for the forwarder.
     *
     * Bounded so a phone that posts faster than the watch can be reached cannot
     * grow this without limit. Twenty is generous against the watch's own
     * ten-deep answerable queue: anything older than that could not be answered
     * even if it were kept.
     */
    private const val MAX_PENDING = 20

    private val pending = ArrayDeque<NotificationMsg>()

    /**
     * How many notifications stay ACTIONABLE.
     *
     * The watch can act on a notification long after it was announced, so the
     * `StatusBarNotification` behind it has to be kept — an action is a
     * `PendingIntent` owned by the posting app, and there is no way to
     * reconstruct one from an id. Matches the ten the GNCS handler keeps
     * answerable: a notification it cannot describe is one the watch will not
     * offer actions for either.
     */
    private const val MAX_ACTIONABLE = 10

    /** Our stable id → the live notification, oldest first. */
    private val actionable = LinkedHashMap<Long, android.service.notification.StatusBarNotification>()

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // -------------------------------------------------------------------------
    // Config
    // -------------------------------------------------------------------------

    fun readConfig(context: Context): NotificationFilter.Config {
        val p = prefs(context)
        return NotificationFilter.Config(
            enabled = p.getBoolean(KEY_ENABLED, false),
            blockedPackages = p.getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet(),
            watchAddress = p.getString(KEY_WATCH_ADDRESS, null),
        )
    }

    fun writeConfig(context: Context, config: NotificationFilter.Config) {
        prefs(context).edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putStringSet(KEY_BLOCKED, config.blockedPackages)
            .putString(KEY_WATCH_ADDRESS, config.watchAddress)
            .apply()
        // Config changes can only make forwarding narrower or point at a
        // different watch, so anything already buffered is now suspect.
        if (!config.enabled) clearPending()
    }

    // -------------------------------------------------------------------------
    // The pending buffer
    // -------------------------------------------------------------------------

    /** Buffers [message], dropping the oldest if the buffer is full. */
    @Synchronized
    fun offer(message: NotificationMsg) {
        while (pending.size >= MAX_PENDING) {
            pending.removeFirst()
        }
        pending.addLast(message)
    }

    /** Drains everything buffered. */
    @Synchronized
    fun takePending(): List<NotificationMsg> {
        val taken = pending.toList()
        pending.clear()
        return taken
    }

    @Synchronized
    fun clearPending() {
        pending.clear()
    }

    @Synchronized
    fun hasPending(): Boolean = pending.isNotEmpty()

    // -------------------------------------------------------------------------
    // Actionable notifications
    // -------------------------------------------------------------------------

    /** Remembers [sbn] under [id] so its actions can be fired later. */
    @Synchronized
    fun retain(id: Long, sbn: android.service.notification.StatusBarNotification) {
        // Re-inserted rather than updated, so an edited notification counts as
        // freshly used and does not age out early.
        actionable.remove(id)
        actionable[id] = sbn
        while (actionable.size > MAX_ACTIONABLE) {
            val oldest = actionable.keys.first()
            actionable.remove(oldest)
        }
    }

    @Synchronized
    fun actionable(id: Long): android.service.notification.StatusBarNotification? =
        actionable[id]

    @Synchronized
    fun forget(id: Long) {
        actionable.remove(id)
    }

    @Synchronized
    fun clearActionable() {
        actionable.clear()
    }
}
