package tech.mmarca.openvitals.devices.garmin

import android.util.Log
import tech.mmarca.openvitals.BuildConfig

/**
 * Protocol logging for the Garmin stack, debug builds only. It logs
 * addresses, names, settings contents and raw dumps, none of which belongs
 * in a shipped logcat. Without an installed sink, logging is a no-op.
 */
object GarminLog {

    private const val TAG = "GarminGfdi"

    @Volatile
    private var sink: ((String) -> Unit)? = null

    /** Whether anything is listening — lets callers skip expensive formatting. */
    val enabled: Boolean get() = sink != null

    /** Routes logs to logcat. A no-op in a release build: that is the whole redaction policy. */
    fun installLogcatSink() {
        // Guarded: the JVM `Log` stub throws, and this sink is global across tests.
        if (BuildConfig.DEBUG) sink = { message -> runCatching { Log.d(TAG, message) } }
    }

    /** Replaces the sink — for tests that assert on protocol logs. */
    fun installSink(newSink: ((String) -> Unit)?) {
        sink = newSink
    }

    fun log(message: String) {
        sink?.invoke(message)
    }

    /** The same, for a message that is expensive to build. A release build never builds it. */
    inline fun logLazy(message: () -> String) {
        if (enabled) log(message())
    }
}
