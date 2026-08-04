package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.RemoteInput
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.data.repository.BleDeviceRepository
import tech.mmarca.openvitals.devices.notifications.NotificationMsg
import tech.mmarca.openvitals.devices.notifications.NotificationStore
import tech.mmarca.openvitals.devices.notifications.OpenVitalsNotificationListenerService

/**
 * The seam between Android's notification listener and the Garmin stack.
 *
 * In the Flutter build this was three things: a persisted callback handle, a
 * headless engine the native side had to spin (`ForwarderEngine`), and a Dart
 * `GarminNotificationBridge` that drained the native buffer once the engine
 * was up. A single-process Kotlin app needs none of the machinery — the
 * listener service calls [onNotificationsPending] directly and this singleton
 * owns the one [GarminNotificationForwarder] — but the responsibilities are
 * the same: drain the buffer, resolve the paired watch, convert each captured
 * notification into a [GarminNotification], and perform what the wearer chose
 * on the wrist.
 *
 * **No Health Connect, no database.** This reads the device registry and the
 * notification buffer, and writes nothing — notification text never touches
 * disk on its way to the watch.
 *
 * Everything runs on a SINGLE-threaded scope: the forwarder is a ported Dart
 * state machine whose invariants assume sequential dispatch.
 */
@Singleton
class GarminNotificationBridge @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceRepository: BleDeviceRepository,
) {

    private companion object {
        const val TAG = "OVNotifyBridge"
    }

    /**
     * Sequential on purpose — see the class doc. `limitedParallelism(1)` is a
     * FIFO queue, so posts drain in the order they were captured.
     */
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default.limitedParallelism(1),
    )

    private var forwarder: GarminNotificationForwarder? = null

    /** The address the current forwarder was built for. */
    private var forwarderAddress: String? = null

    private val identity = GarminPhoneIdentity()

    /**
     * Called by the listener service after it buffered something. Cheap and
     * thread-safe: the work happens on the bridge's own scope.
     */
    fun onNotificationsPending() {
        scope.launch { drain() }
    }

    /**
     * Called after the watch-notifications configuration changed.
     *
     * Turning the feature off (or unpairing the watch) tears the held link
     * down; anything else takes effect on the next notification, exactly as
     * the Flutter build's engine was only ever spun by one arriving.
     */
    fun onConfigChanged() {
        scope.launch {
            val config = NotificationStore.readConfig(context)
            val current = forwarder ?: return@launch
            if (!config.enabled || forwarderAddress != pairedWatchAddress()) {
                GarminLog.log("[GARMIN-NOTIFY] configuration changed; stopping the forwarder")
                current.dispose()
                forwarder = null
                forwarderAddress = null
            }
        }
    }

    /** Moves everything the listener buffered into the forwarder. */
    private fun drain() {
        val pending = NotificationStore.takePending()
        if (pending.isEmpty()) return
        val target = forwarderFor(pairedWatchAddress()) ?: run {
            GarminLog.log("[GARMIN-NOTIFY] no Garmin watch paired; nothing to forward to")
            return
        }
        GarminLog.log("[GARMIN-NOTIFY] draining ${pending.size} notification(s)")
        for (message in pending) {
            if (message.removed) {
                target.withdraw(message.id)
            } else {
                target.post(toNotification(message))
            }
        }
    }

    /**
     * The address of the paired Garmin watch, or null.
     *
     * Read through the same registry the app uses — one source of truth, no
     * second decode of the stored JSON.
     */
    private fun pairedWatchAddress(): String? =
        deviceRepository.devices.firstOrNull { it.isGarminGfdi }?.address

    private fun forwarderFor(address: String?): GarminNotificationForwarder? {
        if (address == null) return null
        val current = forwarder
        if (current != null && forwarderAddress == address) return current
        // The watch changed underneath a live forwarder: stop the old one
        // before pointing at the new address.
        current?.dispose()
        val created = GarminNotificationForwarder(
            scope = scope,
            address = address,
            phoneName = identity.bluetoothName,
            manufacturer = identity.manufacturer,
            model = identity.model,
            lease = SharedGarminRadioLease,
            openLink = { request -> GarminBleNotificationLink.open(context, scope, request) },
            onAction = ::performAction,
            onIdle = {
                // Reached only when there is genuinely nothing left to do — the
                // held-link design never idles while a watch is reachable. The
                // Flutter build tore its engine down here; this just forgets
                // the forwarder so the next notification builds a fresh one.
                scope.launch {
                    forwarder?.dispose()
                    forwarder = null
                    forwarderAddress = null
                }
            },
        )
        forwarder = created
        forwarderAddress = address
        return created
    }

    private fun toNotification(message: NotificationMsg): GarminNotification =
        GarminNotification(
            id = message.id,
            packageName = message.packageName,
            title = message.title ?: message.appLabel ?: message.packageName,
            subtitle = message.subtitle ?: "",
            body = message.body ?: "",
            category = categoryOf(message.categoryOrdinal),
            postedAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(message.whenEpochMillis),
                ZoneId.systemDefault(),
            ),
            actions = garminActionsFor(message),
        )

    /**
     * The capture side pre-maps Android's category constants onto this enum's
     * ordinals. An ordinal outside the enum means the two have drifted apart,
     * and [GarminNotificationCategory.OTHER] is the safe reading — a
     * notification mislabelled as an incoming call would be worse than one
     * left unlabelled.
     */
    private fun categoryOf(ordinal: Int): GarminNotificationCategory =
        GarminNotificationCategory.entries.getOrElse(ordinal) {
            GarminNotificationCategory.OTHER
        }

    // -------------------------------------------------------------------------
    // Acting on a notification from the wrist
    // -------------------------------------------------------------------------

    /**
     * Performs what the wearer chose on the wrist.
     *
     * Dismiss is OURS, not the posting app's — there is no notification action
     * for "clear this", so it maps onto the listener's own cancel rather than
     * a PendingIntent. Everything else is the app's own button, fired by
     * index. Ported from the Flutter plugin's `performNotificationAction` /
     * `dismissNotification`.
     */
    private suspend fun performAction(request: GarminNotificationActionRequest) {
        val performed = if (request.action.isSynthetic) {
            dismissNotification(request.notificationId)
        } else {
            performNotificationAction(
                request.notificationId,
                request.action.androidIndex,
                request.replyText,
            )
        }
        if (!performed) {
            // The notification is gone from the phone, or its intent was
            // cancelled. There is no way to tell the watch, so this is only
            // worth logging.
            GarminLog.log(
                "[GARMIN-NOTIFY] \"${request.action.label}\" could not be " +
                    "performed on ${request.notificationId}",
            )
        }
    }

    private fun performNotificationAction(
        id: Long,
        actionIndex: Int,
        replyText: String?,
    ): Boolean {
        val sbn = NotificationStore.actionable(id)
        if (sbn == null) {
            Log.w(TAG, "no notification $id left to act on")
            return false
        }
        val notification = sbn.notification
        if (notification == null) {
            Log.w(TAG, "notification $id has no payload")
            return false
        }
        // The SAME enumeration the actions were captured from, or the index
        // would select a different button than the one the wearer saw.
        val action = OpenVitalsNotificationListenerService
            .wristActions(notification)
            .getOrNull(actionIndex)
        if (action?.actionIntent == null) {
            Log.w(TAG, "notification $id has no action at $actionIndex")
            return false
        }
        return try {
            val remoteInputs = action.remoteInputs
            if (remoteInputs.isNullOrEmpty()) {
                // A plain button: firing its PendingIntent is exactly what
                // tapping it on the phone does.
                action.actionIntent!!.send()
            } else {
                // A reply. The text has to be delivered the way the posting app
                // expects to read it — a results bundle keyed by the
                // RemoteInput's own result key, written into the intent by
                // RemoteInput itself. Sending the PendingIntent without it
                // makes most apps post an empty message.
                val results = Bundle()
                for (remoteInput in remoteInputs) {
                    results.putCharSequence(remoteInput.resultKey, replyText ?: "")
                }
                // FLAG_ACTIVITY_NEW_TASK because the intent is filled in from a
                // service with no task of its own; an app that answers a reply
                // by starting an activity fails without it.
                val intent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                RemoteInput.addResultsToIntent(remoteInputs, intent, results)
                // Some apps read the reply as free-form text rather than a
                // choice; saying so costs nothing and fixes those.
                RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_FREE_FORM_INPUT)
                action.actionIntent!!.send(context, 0, intent)
            }
            // The label is notification-derived, and GarminLog is silent in a
            // release build by construction — so both lines are safe.
            GarminLog.log("[GARMIN-NOTIFY] performed action $actionIndex on $id")
            GarminLog.log("[GARMIN-NOTIFY]   that action was \"${action.title}\"")
            true
        } catch (error: Throwable) {
            // A cancelled PendingIntent is the normal outcome for a
            // notification the app has already torn down.
            Log.w(TAG, "could not perform action $actionIndex on $id: $error")
            false
        }
    }

    private fun dismissNotification(id: Long): Boolean {
        val sbn = NotificationStore.actionable(id)
        if (sbn == null) {
            Log.w(TAG, "no notification $id left to dismiss")
            return false
        }
        val service = OpenVitalsNotificationListenerService.instance
        if (service == null) {
            // Only possible if the OS has unbound us, in which case
            // notification access has been revoked and the whole feature is
            // off anyway.
            Log.w(TAG, "not bound; cannot dismiss $id")
            return false
        }
        val dismissed = service.dismiss(sbn.key)
        if (dismissed) NotificationStore.forget(id)
        return dismissed
    }
}
