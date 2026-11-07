package tech.mmarca.openvitals.devices.notifications

import android.content.Context
import android.content.SharedPreferences

/**
 * Process-wide state shared by the listener service and the forwarder. An
 * `object`, because the service is bound before any UI or Hilt exists. The
 * buffer is memory-only and bounded; the config is persisted so the service
 * can filter after a reboot. File and key names are the Flutter build's.
 */
object NotificationStore {

    private const val PREFS = "openvitals_notification_listener"
    private const val KEY_ENABLED = "forwarding_enabled"
    private const val KEY_BLOCKED = "blocked_packages"
    private const val KEY_WATCH_ADDRESS = "watch_address"

    /** Notifications waiting for the forwarder. Twenty is generous against the watch's ten. */
    private const val MAX_PENDING = 20

    private val pending = ArrayDeque<NotificationMsg>()

    /**
     * Notifications that stay actionable. A `PendingIntent` cannot be rebuilt
     * from an id. Matches the ten the GNCS handler keeps.
     */
    private const val MAX_ACTIONABLE = 10

    /** Our stable id → the live notification, oldest first. */
    private val actionable = LinkedHashMap<Long, android.service.notification.StatusBarNotification>()

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Config.

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
        // A config change can only narrow forwarding, so the buffer is suspect.
        if (!config.enabled) clearPending()
    }

    // The pending buffer.

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

    // Actionable notifications.

    /** Remembers [sbn] under [id] so its actions can be fired later. */
    @Synchronized
    fun retain(id: Long, sbn: android.service.notification.StatusBarNotification) {
        // Re-inserted, so an edited notification does not age out early.
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
