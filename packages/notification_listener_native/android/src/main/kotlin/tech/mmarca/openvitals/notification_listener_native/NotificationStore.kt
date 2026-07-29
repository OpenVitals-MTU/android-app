package tech.mmarca.openvitals.notification_listener_native

import android.content.Context
import android.content.SharedPreferences

/**
 * Process-wide state shared between the listener service and whichever Flutter
 * engine is alive.
 *
 * A Kotlin `object`, deliberately: statics are shared across FlutterEngines
 * within one process, and this plugin exists precisely because the two halves
 * run in different engines. An instance field would give the headless forwarder
 * a different buffer from the one the service filled.
 *
 * The BUFFER is memory-only and bounded. Notification text is never written to
 * disk — it lives here until Dart drains it and nowhere else. The CONFIG is
 * persisted, because the service is bound long before any Dart has run (after a
 * reboot, or after the process was killed) and has to be able to filter without
 * asking anyone.
 */
object NotificationStore {

    private const val PREFS = "openvitals_notification_listener"
    private const val KEY_CALLBACK_HANDLE = "forwarder_callback_handle"
    private const val KEY_ENABLED = "forwarding_enabled"
    private const val KEY_BLOCKED = "blocked_packages"
    private const val KEY_WATCH_ADDRESS = "watch_address"
    private const val KEY_DIAGNOSTICS = "diagnostics"

    /**
     * How many notifications wait for Dart to wake.
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
     * reconstruct one from an id. Matches the ten the Dart side keeps
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

    /**
     * Whether this build may log what it is doing. See [Diagnostics] — persisted
     * because the listener service runs long before any Flutter engine exists.
     */
    fun readDiagnostics(context: Context): Boolean =
        prefs(context).getBoolean(KEY_DIAGNOSTICS, false)

    fun writeDiagnostics(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_DIAGNOSTICS, enabled).apply()
    }

    fun readCallbackHandle(context: Context): Long =
        prefs(context).getLong(KEY_CALLBACK_HANDLE, 0L)

    fun writeCallbackHandle(context: Context, handle: Long) {
        prefs(context).edit().putLong(KEY_CALLBACK_HANDLE, handle).apply()
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
