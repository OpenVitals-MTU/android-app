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
 * watch.
 *
 * Bound by the system whenever the process is alive, once the user has granted
 * notification access in system settings — there is no runtime prompt for this
 * permission, and no way to ask for it from inside the app.
 *
 * This class does as little as possible, on purpose:
 *
 *  1. Reduce the notification to the handful of fields GNCS can carry.
 *  2. Ask [NotificationFilter] whether it is worth anything. Most are not, and
 *     rejecting them here — before any Bluetooth is touched — is the single
 *     biggest battery decision in the feature.
 *  3. Buffer the survivors and poke the in-process forwarder.
 *
 * Lifted from the Flutter build's `notification_listener_native` plugin with
 * ONE structural change: step 3 used to persist a callback handle and spin a
 * headless Flutter engine (`ForwarderEngine.wake`); in a single-process Kotlin
 * app the forwarder is an ordinary singleton, so the wake-up became a direct
 * call into [GarminNotificationBridge] and the whole headless-engine problem
 * evaporated.
 *
 * It touches no Bluetooth and holds no protocol knowledge. Nothing is written
 * to disk: notification text lives in a bounded in-memory buffer until the
 * forwarder drains it, and the app has no INTERNET permission, so it cannot
 * leave the phone by any route other than the one Bluetooth link.
 */
@AndroidEntryPoint
class OpenVitalsNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var bridge: GarminNotificationBridge

    companion object {
        private const val TAG = "OVNotifyListener"

        /**
         * The bound instance, or null when the system has not bound us.
         *
         * Needed because dismissing a notification is
         * [NotificationListenerService.cancelNotification], an instance method on
         * the bound service — and the request arrives from the forwarder, which
         * has no reference to it. Cleared on disconnect so a stale instance is
         * never used.
         */
        @Volatile
        var instance: OpenVitalsNotificationListenerService? = null
            private set

        /**
         * The actions a WATCH should be offered, wearable ones first.
         *
         * A messaging app publishes two different action lists. The one in
         * `notification.actions` drives the phone's own shade; the one in the
         * wearable extender is the one built for a remote device — and it is the
         * only one whose reply reliably works, because the phone's is typically
         * an immutable `PendingIntent` on Android 12+, which silently discards
         * the extras a reply is carried in. A reply sent through the wrong one
         * reports success and delivers nothing, which is exactly what happened.
         *
         * Both lists are returned as androidx `NotificationCompat.Action`, so the
         * caller does not have to care which it got. Gadgetbridge makes the same
         * wearable-first choice for the same reason.
         *
         * Must be the ONLY way actions are enumerated — see [actionsOf].
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

    /**
     * Clears a notification from the phone, as swiping it away would.
     *
     * Returns false when we are not bound — the caller logs it, because there is
     * nothing to tell the watch.
     */
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

            // A dismissal is judged on the same rules minus the content ones:
            // withdrawing a card the watch never received is harmless, and
            // skipping the check would leak the existence of blocked apps.
            val verdict = NotificationFilter.verdict(
                candidate = if (removed) candidate.copy(title = "-", body = "-") else candidate,
                config = config,
                ownPackage = packageName,
                interruptionFilter = interruptionFilterOrUnknown(),
            )
            if (verdict != NotificationFilter.Verdict.KEEP) {
                // Logged at debug and WITHOUT any content: the reason is useful
                // when the watch stays silent, the text never is.
                Diagnostics.d(TAG, "dropped ${sbn.packageName}: $verdict")
                return
            }

            val id = stableId(sbn)
            if (removed) {
                // Gone from the phone, so its actions can no longer be fired.
                NotificationStore.forget(id)
            } else {
                // Kept so the wrist can act on it later: an action is a
                // PendingIntent owned by the posting app and cannot be rebuilt
                // from an id.
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
            // The Flutter build persisted a callback handle here and woke a
            // headless engine. Single process: the forwarder is right there.
            bridge.onNotificationsPending()
        } catch (error: Throwable) {
            // A listener that throws is killed and unbound by the system, and
            // the user then has to re-grant access. Nothing here is worth that.
            Log.w(TAG, "failed to handle a notification: $error")
        }
    }

    /**
     * The actions the posting app attached, in its own order.
     *
     * The INDEX is what travels to the watch and back, so an action fired from
     * the wrist resolves to the same action without anyone having to re-derive
     * it. (Gadgetbridge maps its custom actions back by re-walking the list and
     * counting types, and its own comment calls that fragile.) That only holds
     * because capture and execution both go through [wristActions] — index into
     * a differently-built list and the wearer gets a different button.
     *
     * An action with a `RemoteInput` expects text — the watch offers its
     * keyboard or canned replies for those.
     */
    private fun actionsOf(notification: Notification): List<NotificationActionMsg> {
        describeActions(notification)
        return wristActions(notification).mapIndexedNotNull { index, action ->
            val title = action.title?.toString()
            if (title.isNullOrBlank()) return@mapIndexedNotNull null
            // No intent means nothing can be fired, so offering it would be a
            // button that silently does nothing — which is the thing being fixed.
            if (action.actionIntent == null) return@mapIndexedNotNull null
            NotificationActionMsg(
                index = index,
                title = title,
                isReply = !action.remoteInputs.isNullOrEmpty(),
                // An ACTIVITY intent cannot be fired from here — see the field's
                // own doc. Reported rather than filtered so the decision about
                // what to offer stays in one place, next to the slot mapping.
                fireableFromBackground = action.actionIntent!!.isFireableFromBackground(),
            )
        }
    }

    /**
     * Dumps what a notification actually offers, so a reply that "sends" and
     * never arrives can be diagnosed from a log instead of guessed at.
     *
     * Deliberately logs STRUCTURE only — labels, result keys, mutability — and
     * never a word of the message. Debug level, so it costs nothing in release.
     */
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
            // The KIND matters more than anything else here. An Activity
            // PendingIntent opens the app's compose screen rather than sending,
            // and firing one from a background service is silently blocked by
            // Android's background-activity-launch rules — no exception, no
            // effect, which is indistinguishable from a reply that vanished.
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

    /**
     * The notification's text, preferring the expanded form.
     *
     * `EXTRA_BIG_TEXT` is what the user sees when they pull a message down, and
     * it is what they expect on the wrist; `EXTRA_TEXT` is often truncated with
     * an ellipsis by the posting app.
     */
    private fun bodyOf(notification: Notification): String? {
        val extras = notification.extras
        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        if (!big.isNullOrBlank()) return big
        return extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
    }

    /**
     * A stable 32-bit id for this notification.
     *
     * Derived from the system's own key, which is stable across updates to the
     * same notification — that is what makes an edited message redraw one card
     * on the watch instead of buzzing a second time. Masked to 31 bits because
     * GNCS carries it as an unsigned 32-bit value and a negative id would wrap.
     */
    private fun stableId(sbn: StatusBarNotification): Long =
        (sbn.key.hashCode().toLong() and 0x7FFFFFFFL)

    /**
     * The importance the user (or the app) gave this notification's channel.
     *
     * Read off the ranking rather than the channel object: `Ranking.getChannel`
     * needs API 28, `Ranking.getImportance` has been there since 24, and this
     * app supports 26. Anything unreadable is UNSPECIFIED, which the filter
     * treats as "allow" — a notification getting through is a better failure
     * than one silently swallowed.
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

    /**
     * The posting app's display name, or null.
     *
     * Resolved through the `<queries>` launcher-intent declaration in the app
     * manifest, never QUERY_ALL_PACKAGES — that one is Play-restricted and its
     * mere presence blocks upload.
     */
    private fun appLabel(packageName: String): String? = try {
        val manager: PackageManager = packageManager
        manager.getApplicationLabel(manager.getApplicationInfo(packageName, 0)).toString()
    } catch (error: Throwable) {
        null
    }
}

/**
 * Whether this action's intent can actually be fired from a background service.
 *
 * An ACTIVITY PendingIntent is silently dropped by Android's
 * background-activity-launch rules — no exception and no effect, which is
 * indistinguishable from a reply that vanished — so the watch is told not to
 * offer it.
 *
 * `PendingIntent.isActivity` only exists from API 31. Below that the kind
 * cannot be asked for at all, and the launch restrictions it guards against
 * were not yet in force, so every action is reported as fireable: that is what
 * the platform of the day actually did, and the alternative would hide working
 * reply actions on every device older than Android 12.
 */
private fun PendingIntent.isFireableFromBackground(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) !isActivity else true
