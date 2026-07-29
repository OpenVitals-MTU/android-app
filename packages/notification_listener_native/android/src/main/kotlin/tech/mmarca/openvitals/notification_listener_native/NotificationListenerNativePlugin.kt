package tech.mmarca.openvitals.notification_listener_native

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.core.app.RemoteInput
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding

/**
 * Flutter plugin for reading the phone's notifications and mirroring them to a
 * paired Garmin watch.
 *
 * Holds NO protocol logic and touches NO Bluetooth: the capture and the filter
 * are here because they can only be here, and everything that reaches the watch
 * is produced in pure Dart under `lib/devices/garmin/`.
 *
 * Two things live here that look unrelated but are not:
 *
 *  * The notification bridge itself.
 *  * [RadioLeases], the process-wide lock on a watch's radio. It belongs
 *    alongside the thing that made it necessary — this plugin is what introduced
 *    a SECOND Flutter engine, and therefore a second `flutter_blue_plus`
 *    instance with its own GATT map that no Dart mutex can see.
 *
 * This plugin is attached to both engines (the app's and the headless
 * forwarder's), because `FlutterEngine(context)` registers every plugin
 * automatically. Everything it owns is therefore in process-wide `object`s
 * rather than instance fields — an instance field would give the forwarder a
 * different buffer and a different lease table from the app.
 */
class NotificationListenerNativePlugin :
    FlutterPlugin,
    ActivityAware,
    NotificationListenerHostApi {

    private companion object {
        const val TAG = "OVNotifyPlugin"

        /**
         * The lease TTL is renewed from Dart while a link is held. It is short
         * on purpose: an isolate that dies holding one must not wedge the radio
         * for longer than the user would wait before trying again.
         */
        const val DEFAULT_LEASE_TTL_MILLIS = 15_000L
    }

    private var applicationContext: Context? = null
    private var activity: Activity? = null

    private fun context(): Context =
        applicationContext ?: error("plugin is not attached to an engine")

    // -------------------------------------------------------------------------
    // FlutterPlugin / ActivityAware
    // -------------------------------------------------------------------------

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        applicationContext = binding.applicationContext
        NotificationListenerHostApi.setUp(binding.binaryMessenger, this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        NotificationListenerHostApi.setUp(binding.binaryMessenger, null)
        applicationContext = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    // -------------------------------------------------------------------------
    // Notification access
    // -------------------------------------------------------------------------

    override fun isNotificationAccessGranted(): Boolean {
        val context = context()
        // The canonical check: the OS keeps a colon-separated list of enabled
        // listener components in Secure settings. There is no API that answers
        // this for your own package.
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        val ours = context.packageName
        return enabled.split(':').any { entry ->
            entry.substringBefore('/') == ours
        }
    }

    override fun openNotificationAccessSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        val host = activity
        if (host != null) {
            host.startActivity(intent)
            return
        }
        // No Activity bound (the forwarder engine has none). Falling back to a
        // new task keeps this callable rather than throwing, though in practice
        // only the UI ever asks.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context().startActivity(intent)
    }

    // -------------------------------------------------------------------------
    // Configuration
    // -------------------------------------------------------------------------

    override fun setDiagnosticsEnabled(enabled: Boolean) {
        NotificationStore.writeDiagnostics(context(), enabled)
        Diagnostics.refresh(context())
    }

    override fun registerForwarderCallback(callbackHandle: Long) {
        NotificationStore.writeCallbackHandle(context(), callbackHandle)
    }

    override fun setForwardingConfig(
        enabled: Boolean,
        blockedPackages: List<String>,
        watchAddress: String?,
        diagnostics: Boolean,
    ) {
        // Written before the config, and refreshed immediately, so the log line
        // below obeys the flag it is being told about rather than the previous one.
        NotificationStore.writeDiagnostics(context(), diagnostics)
        Diagnostics.refresh(context())
        NotificationStore.writeConfig(
            context(),
            NotificationFilter.Config(
                enabled = enabled,
                blockedPackages = blockedPackages.toSet(),
                watchAddress = watchAddress,
            ),
        )
        Diagnostics.i(
            context(),
            TAG,
            "forwarding ${if (enabled) "on" else "off"}, " +
                "${blockedPackages.size} blocked, watch ${if (watchAddress == null) "unset" else "set"}",
        )
    }

    override fun takePendingNotifications(): List<NotificationMsg> =
        NotificationStore.takePending()

    override fun stopForwarder() {
        ForwarderEngine.stop()
    }

    // -------------------------------------------------------------------------
    // Acting on a notification from the wrist
    // -------------------------------------------------------------------------

    override fun performNotificationAction(
        id: Long,
        actionIndex: Long,
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
            .getOrNull(actionIndex.toInt())
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
                // expects to read it — a results bundle keyed by the RemoteInput's
                // own result key, written into the intent by RemoteInput itself.
                // Sending the PendingIntent without it makes most apps post an
                // empty message.
                val results = Bundle()
                for (remoteInput in remoteInputs) {
                    results.putCharSequence(remoteInput.resultKey, replyText ?: "")
                }
                // DEBUG, not info: this is derived from a notification, and the
                // Garmin stack's rule is that nothing notification- or
                // watch-derived reaches a release build's logcat, where it
                // survives in bug reports. See `garmin_log.dart`.
                Diagnostics.d(
                    context(),
                    TAG,
                    "replying via keys=[${remoteInputs.joinToString(",") { it.resultKey }}] " +
                        "chars=${replyText?.length ?: 0} " +
                        "intent=${if (android.os.Build.VERSION.SDK_INT >= 31 &&
                            action.actionIntent!!.isImmutable) "IMMUTABLE" else "mutable"}",
                )
                // FLAG_ACTIVITY_NEW_TASK because the intent is filled in from a
                // service with no task of its own; an app that answers a reply by
                // starting an activity fails without it.
                val intent = Intent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                RemoteInput.addResultsToIntent(remoteInputs, intent, results)
                // Some apps read the reply as free-form text rather than a
                // choice; saying so costs nothing and fixes those.
                RemoteInput.setResultsSource(intent, RemoteInput.SOURCE_FREE_FORM_INPUT)
                action.actionIntent!!.send(context(), 0, intent)
            }
            // The label is notification-derived, so it goes to debug; the fact
            // that something was performed is operational and stays at info.
            Diagnostics.i(context(), TAG, "performed action $actionIndex on $id")
            Diagnostics.d(context(), TAG, "  that action was \"${action.title}\"")
            true
        } catch (error: Throwable) {
            // A cancelled PendingIntent is the normal outcome for a notification
            // the app has already torn down.
            Log.w(TAG, "could not perform action $actionIndex on $id: $error")
            false
        }
    }

    override fun dismissNotification(id: Long): Boolean {
        val sbn = NotificationStore.actionable(id)
        if (sbn == null) {
            Log.w(TAG, "no notification $id left to dismiss")
            return false
        }
        val service = OpenVitalsNotificationListenerService.instance
        if (service == null) {
            // Only possible if the OS has unbound us, in which case notification
            // access has been revoked and the whole feature is off anyway.
            Log.w(TAG, "not bound; cannot dismiss $id")
            return false
        }
        val dismissed = service.dismiss(sbn.key)
        if (dismissed) NotificationStore.forget(id)
        return dismissed
    }

    override fun listLaunchableApps(): List<InstalledAppMsg> {
        val manager = context().packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return manager.queryIntentActivities(intent, 0)
            .mapNotNull { resolved ->
                val info = resolved.activityInfo?.applicationInfo ?: return@mapNotNull null
                InstalledAppMsg(
                    packageName = info.packageName,
                    label = manager.getApplicationLabel(info).toString(),
                )
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    // -------------------------------------------------------------------------
    // Radio lease
    // -------------------------------------------------------------------------

    override fun acquireRadio(address: String, owner: String, ttlMillis: Long): Boolean {
        val ttl = if (ttlMillis > 0) ttlMillis else DEFAULT_LEASE_TTL_MILLIS
        val granted = RadioLeases.acquire(address, owner, ttl)
        if (!granted) {
            Diagnostics.i(context(), TAG, "radio busy: $owner refused, held by ${RadioLeases.owner(address)}")
        }
        return granted
    }

    override fun requestRadio(address: String, owner: String) {
        RadioLeases.request(address, owner)
        Diagnostics.i(context(), TAG, "radio requested by $owner, held by ${RadioLeases.owner(address)}")
    }

    override fun renewRadio(address: String, owner: String): Boolean =
        RadioLeases.renew(address, owner, DEFAULT_LEASE_TTL_MILLIS)

    override fun releaseRadio(address: String, owner: String) {
        RadioLeases.release(address, owner)
    }

    override fun radioOwner(address: String): String? = RadioLeases.owner(address)
}
