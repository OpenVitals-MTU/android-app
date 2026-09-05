package tech.mmarca.openvitals.devices.notifications

import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import tech.mmarca.openvitals.devices.garmin.GarminNotificationBridge

/**
 * Reads the phone's notifications so they can be mirrored to a paired Garmin
 * watch. Bound by the system once the user grants notification access.
 *
 * Does as little as possible: reduce the notification to what GNCS carries,
 * ask [NotificationFilter] whether it is worth anything (the biggest battery
 * decision in the feature), buffer the survivors and poke the forwarder.
 * Touches no Bluetooth and writes nothing to disk.
 */
@AndroidEntryPoint
class OpenVitalsNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var bridge: GarminNotificationBridge

    companion object {
        private const val TAG = "OVNotifyListener"

        /**
         * The bound instance, or null. Dismissing is an instance method, and
         * the request arrives from the forwarder.
         */
        @Volatile
        var instance: OpenVitalsNotificationListenerService? = null
            private set

        /**
         * The actions a watch should be offered, wearable ones first. The
         * wearable extender's list is the one built for a remote device, and the
         * only one whose reply reliably works: the phone's is usually an
         * immutable PendingIntent that discards the reply extras.
         * Must be the only way actions are enumerated; see [actionsOf].
         */
        fun wristActions(
            notification: Notification,
        ): List<NotificationCompat.Action> {
            val wearable = NotificationCompat.WearableExtender(notification).actions
            if (wearable.isNotEmpty()) return wearable
            return (0 until NotificationCompat.getActionCount(notification))
                .mapNotNull { NotificationCompat.getAction(notification, it) }
        }
    }

    private var connected = false

    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = true
        instance = this
        Diagnostics.i(TAG, "notification listener connected")
    }

    override fun onListenerDisconnected() {
        connected = false
        if (instance === this) instance = null
        Diagnostics.i(TAG, "notification listener disconnected")
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    /** Clears a notification from the phone. False when not bound. */
    fun dismiss(key: String): Boolean = try {
        cancelNotification(key)
        true
    } catch (error: Throwable) {
        Log.w(TAG, "could not dismiss: $error")
        false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        handle(sbn, removed = false)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        handle(sbn, removed = true)
    }

    private fun handle(sbn: StatusBarNotification?, removed: Boolean) {
        val notification = sbn?.notification ?: return
        try {
            val config = NotificationStore.readConfig(this)
            val extras = notification.extras

            val candidate = NotificationFilter.Candidate(
                packageName = sbn.packageName,
                title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                body = bodyOf(notification),
                ongoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
                foregroundService =
                    notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
                groupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
                localOnly = notification.flags and Notification.FLAG_LOCAL_ONLY != 0,
                channelImportance = importanceOf(sbn),
            )

            // Same rules minus the content ones, so blocked apps do not leak.
            val verdict = NotificationFilter.verdict(
                candidate = if (removed) candidate.copy(title = "-", body = "-") else candidate,
                config = config,
                ownPackage = packageName,
                interruptionFilter = interruptionFilterOrUnknown(),
            )
            if (verdict != NotificationFilter.Verdict.KEEP) {
                // Debug only, and never the content.
                Diagnostics.d(TAG, "dropped ${sbn.packageName}: $verdict")
                return
            }

            val id = stableId(sbn)
            if (removed) {
                // Gone from the phone, so its actions cannot be fired.
                NotificationStore.forget(id)
            } else {
                // Kept so the wrist can act on it: a PendingIntent cannot be rebuilt from an id.
                NotificationStore.retain(id, sbn)
            }

            NotificationStore.offer(
                NotificationMsg(
                    id = id,
                    packageName = sbn.packageName,
                    appLabel = appLabel(sbn.packageName),
                    title = candidate.title,
                    subtitle = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
                    body = candidate.body,
                    whenEpochMillis = if (notification.`when` != 0L) {
                        notification.`when`
                    } else {
                        sbn.postTime
                    },
                    categoryOrdinal = NotificationFilter.categoryOrdinal(notification.category),
                    removed = removed,
                    actions = if (removed) emptyList() else actionsOf(notification),
                    dismissable = sbn.isClearable,
                ),
            )
            // Single process: the forwarder is right there.
            bridge.onNotificationsPending()
        } catch (error: Throwable) {
            // A listener that throws is unbound, and the user must re-grant access.
            Log.w(TAG, "failed to handle a notification: $error")
        }
    }

    /**
     * The posting app's actions, in its own order. The index travels to the
     * watch and back, so capture and execution must both use [wristActions].
     * An action with a `RemoteInput` expects text.
     */
    private fun actionsOf(notification: Notification): List<NotificationActionMsg> {
        describeActions(notification)
        return wristActions(notification).mapIndexedNotNull { index, action ->
            val title = action.title?.toString()
            if (title.isNullOrBlank()) return@mapIndexedNotNull null
            // No intent means a button that does nothing.
            if (action.actionIntent == null) return@mapIndexedNotNull null
            NotificationActionMsg(
                index = index,
                title = title,
                isReply = !action.remoteInputs.isNullOrEmpty(),
                // An activity intent cannot be fired from here. Reported, not filtered.
                fireableFromBackground = action.actionIntent!!.isFireableFromBackground(),
            )
        }
    }

    /** Logs a notification's action structure, never its text. Debug level. */
    private fun describeActions(notification: Notification) {
        if (!Diagnostics.isEnabled) return
        val wearable = NotificationCompat.WearableExtender(notification).actions
        val standard = NotificationCompat.getActionCount(notification)
        Log.d(
            TAG,
            "actions: ${wearable.size} wearable, $standard standard " +
                "(using ${if (wearable.isNotEmpty()) "wearable" else "standard"})",
        )
        wristActions(notification).forEachIndexed { index, action ->
            val keys = action.remoteInputs
                ?.joinToString(",") { it.resultKey }
                ?: ""
            val intent = action.actionIntent
            val mutability = when {
                intent == null -> "no-intent"
                android.os.Build.VERSION.SDK_INT >= 31 ->
                    if (intent.isImmutable) "IMMUTABLE" else "mutable"
                else -> "unknown"
            }
            // An Activity PendingIntent fired from a background service is silently
            // blocked, which looks exactly like a reply that vanished.
            val kind = when {
                intent == null -> "none"
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S -> "unreportable<31"
                intent.isActivity -> "ACTIVITY"
                intent.isBroadcast -> "broadcast"
                intent.isService -> "service"
                intent.isForegroundService -> "fgservice"
                else -> "unknown"
            }
            Log.d(
                TAG,
                "  [$index] \"${action.title}\" reply=${!action.remoteInputs.isNullOrEmpty()} " +
                    "keys=[$keys] intent=$mutability kind=$kind " +
                    "allowFreeForm=${action.allowGeneratedReplies} " +
                    "semantic=${action.semanticAction}",
            )
        }
    }

    /** The notification's text, preferring the expanded form the user sees. */
    private fun bodyOf(notification: Notification): String? {
        val extras = notification.extras
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (!big.isNullOrBlank()) return big
        return extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
    }

    /**
     * A stable 32-bit id, from the system key, so an edited message redraws
     * one card. Masked to 31 bits: GNCS carries it unsigned.
     */
    private fun stableId(sbn: StatusBarNotification): Long =
        (sbn.key.hashCode().toLong() and 0x7FFFFFFFL)

    /**
     * The channel importance, read off the ranking, which works from API 24.
     * Unreadable is UNSPECIFIED, which the filter allows.
     */
    private fun importanceOf(sbn: StatusBarNotification): Int = try {
        val ranking = Ranking()
        if (currentRanking?.getRanking(sbn.key, ranking) == true) {
            ranking.importance
        } else {
            NotificationFilter.IMPORTANCE_UNSPECIFIED
        }
    } catch (error: Throwable) {
        NotificationFilter.IMPORTANCE_UNSPECIFIED
    }

    private fun interruptionFilterOrUnknown(): Int = try {
        if (connected) currentInterruptionFilter else NotificationFilter.INTERRUPTION_FILTER_UNKNOWN
    } catch (error: Throwable) {
        // Throws when the listener is not connected yet.
        NotificationFilter.INTERRUPTION_FILTER_UNKNOWN
    }

    /** The posting app's display name, via the manifest `<queries>`, never QUERY_ALL_PACKAGES. */
    private fun appLabel(packageName: String): String? = try {
        val manager: PackageManager = packageManager
        manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0)).toString()
    } catch (error: Throwable) {
        null
    }
}

/**
 * Whether this intent can be fired from a background service. An activity
 * intent is silently dropped. Below API 31 the kind cannot be asked, and the
 * restriction did not apply, so every action is fireable.
 */
private fun PendingIntent.isFireableFromBackground(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) !isActivity else true
