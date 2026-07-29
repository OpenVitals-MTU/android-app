package tech.mmarca.openvitals.notification_listener_native

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import io.flutter.FlutterInjector
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.embedding.engine.dart.DartExecutor
import io.flutter.view.FlutterCallbackInformation

/**
 * Runs the Dart forwarder in a headless Flutter engine.
 *
 * **Why an engine at all.** Android binds a `NotificationListenerService`
 * whenever the process is alive, which is usually with no Flutter engine
 * anywhere — the app has not been opened, or was killed hours ago. But the whole
 * Garmin GFDI stack is Dart on top of `flutter_blue_plus`. So the notification
 * has to reach Dart, and the only way to reach Dart with no app is to start an
 * engine and call a stored entry point. This is the same mechanism
 * `android_alarm_manager_plus` and `flutter_local_notifications` use for their
 * background callbacks.
 *
 * **Why this works for Bluetooth.** `FlutterEngine(context)` registers every
 * native plugin automatically, so this engine gets a fully functional
 * `FlutterBluePlusPlugin` of its own. That plugin is `ActivityAware`, but it only
 * dereferences its Activity binding for two things — the enable-Bluetooth intent
 * and runtime permission requests — and the Garmin stack calls neither. It also
 * never SCANS: it connects directly to a bonded address, and background scanning
 * is the restricted API, not background GATT.
 *
 * **No foreground service.** A bound `NotificationListenerService` already keeps
 * the process out of the cached bucket for as long as it is bound, and the app's
 * CompanionDeviceManager association reinforces it. A foreground service would
 * buy process priority we already have, at the cost of a permanent notification
 * and a fight over the app's single service slot with GPS recording and Apple
 * Health import.
 *
 * At most ONE engine exists at a time. It is kept alive between notifications
 * because a burst should share one Bluetooth link rather than pay a fresh
 * handshake each — the Dart side owns that timing and tells us when it is done.
 */
object ForwarderEngine {

    private const val TAG = "OVNotifyEngine"

    private var engine: FlutterEngine? = null
    private val main = Handler(Looper.getMainLooper())

    /**
     * Held so teardown can still ask whether it may log. Application context, so
     * this leaks nothing an `object` was not already going to outlive.
     */
    private var appContext: Context? = null

    /** The API bound to the live engine, or null when none is running. */
    var flutterApi: NotificationListenerFlutterApi? = null
        private set

    @Synchronized
    fun isRunning(): Boolean = engine != null

    /**
     * Starts the engine if it is not already running, then tells Dart there is
     * something waiting.
     *
     * Safe to call on every notification: when the engine is already up this is
     * just the notify, which is what makes a burst collapse into one link.
     */
    fun wake(context: Context) {
        // FlutterEngine must be created and driven from the main thread; the
        // listener service's callbacks arrive on it, but a config change or a
        // re-entrant call might not.
        main.post {
            // NOTHING may escape here. This runs on the main looper of a process
            // whose only reason to exist is a bound NotificationListenerService,
            // so an uncaught throw kills the process, the system rebinds, and the
            // next notification kills it again — a loop that costs the user their
            // notifications with no sign of why.
            try {
                wakeOnMain(context.applicationContext)
            } catch (error: Throwable) {
                Log.e(TAG, "could not start the forwarder engine", error)
            }
        }
    }

    @Synchronized
    private fun wakeOnMain(context: Context) {
        val existing = engine
        if (existing != null) {
            notifyPending()
            return
        }

        val handle = NotificationStore.readCallbackHandle(context)
        if (handle == 0L) {
            // The app has never run since install or update, so there is no
            // entry point to call. The notification stays buffered; the next app
            // start re-registers and the buffer drains then.
            Log.w(TAG, "no forwarder callback registered yet")
            return
        }

        // The loader FIRST, and specifically before looking the callback up.
        //
        // `FlutterCallbackInformation.lookupCallbackInformation` is a JNI call
        // into libflutter.so, and in this process nothing Flutter has run — so
        // the library is not loaded and the method has no implementation. Calling
        // it first threw UnsatisfiedLinkError on the main looper and killed the
        // process on every single notification.
        val loader = FlutterInjector.instance().flutterLoader()
        if (!loader.initialized()) {
            loader.startInitialization(context)
        }
        loader.ensureInitializationComplete(context, null)

        val callback = FlutterCallbackInformation.lookupCallbackInformation(handle)
        if (callback == null) {
            // A stored handle does not survive an app update — the Dart entry
            // point moves. Clear it so we stop retrying until re-registered.
            Log.w(TAG, "stored callback handle no longer resolves; clearing")
            NotificationStore.writeCallbackHandle(context, 0L)
            return
        }

        val created = FlutterEngine(context)
        engine = created
        appContext = context
        flutterApi = NotificationListenerFlutterApi(created.dartExecutor.binaryMessenger)
        created.dartExecutor.executeDartCallback(
            DartExecutor.DartCallback(context.assets, loader.findAppBundlePath(), callback)
        )
        Diagnostics.i(context, TAG, "forwarder engine started")
        // The entry point drains the buffer itself as it boots, so a notify that
        // arrives before it has registered its Flutter API costs nothing.
        notifyPending()
    }

    private fun notifyPending() {
        val api = flutterApi ?: return
        if (!NotificationStore.hasPending()) return
        api.onNotificationsPending { result ->
            result.exceptionOrNull()?.let {
                Log.w(TAG, "could not tell Dart about pending notifications: $it")
            }
        }
    }

    /**
     * Tears the engine down.
     *
     * Called by Dart once it has closed the watch link and has nothing left to
     * do. Leaving it running would hold a Dart isolate for the life of the
     * process to no purpose.
     */
    fun stop() {
        main.post { stopOnMain() }
    }

    @Synchronized
    private fun stopOnMain() {
        val running = engine ?: return
        val context = appContext
        engine = null
        flutterApi = null
        running.destroy()
        if (context != null) Diagnostics.i(context, TAG, "forwarder engine stopped")
    }
}
