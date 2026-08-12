package tech.mmarca.openvitals.devices.garmin

import android.util.Log
import tech.mmarca.openvitals.BuildConfig

/**
 * Protocol logging for the Garmin stack — DEBUG BUILDS ONLY.
 *
 * Port of the Flutter build's `garmin_log.dart`, whose policy this keeps:
 * what this layer logs is a watch's Bluetooth address, its name and firmware,
 * the contents of its settings screens — alarm times, profile rows — and raw
 * protocol dumps. None of that belongs in a shipped app's logcat, where it
 * survives in bug reports. So nothing here ever logs in a release build:
 * [installLogcatSink] is a no-op unless `BuildConfig.DEBUG`, and until a sink
 * is installed at all (unit tests never install one) logging is a no-op too.
 *
 * Errors reach the person using the app through the UI, which is where they
 * can actually be seen. This is for the developer holding the watch.
 */
object GarminLog {

    private const val TAG = "GarminGfdi"

    @Volatile
    private var sink: ((String) -> Unit)? = null

    /** Whether anything is listening — lets callers skip expensive formatting. */
    val enabled: Boolean get() = sink != null

    /**
     * Routes Garmin protocol logs to logcat. Call once from app wiring; does
     * nothing in a release build, which is the whole redaction policy — MACs,
     * serials and notification text never reach a shipped logcat because the
     * sink is never installed there.
     */
    fun installLogcatSink() {
        // Guarded because a log line must never be able to take down the
        // protocol stack that emitted it. On a device Log.d does not throw; in
        // a JVM unit test the android.util.Log stub does, and this sink is
        // global — one test constructing a service that installs it would
        // otherwise fail every later test in the same fork.
        if (BuildConfig.DEBUG) sink = { message -> runCatching { Log.d(TAG, message) } }
    }

    /** Replaces the sink — for tests that assert on protocol logs. */
    fun installSink(newSink: ((String) -> Unit)?) {
        sink = newSink
    }

    fun log(message: String) {
        sink?.invoke(message)
    }

    /**
     * The same, for a message that is expensive to BUILD.
     *
     * A hex dump was being formatted for every frame the sync did not
     * recognise before anything decided whether anyone wanted it. Passing the
     * work as a lambda means a release build never does it at all.
     */
    inline fun logLazy(message: () -> String) {
        if (enabled) log(message())
    }
}
