package tech.mmarca.openvitals.devices.wearos

import android.util.Log
import tech.mmarca.openvitals.BuildConfig

/**
 * Logging for the Wear OS link, debug builds only, like `GarminLog`. Without
 * an installed sink it is a no-op, which is also what a unit test sees: the
 * JVM `Log` stub throws.
 */
internal object WearOsLog {

    private const val TAG = "OpenVitalsWearOs"

    @Volatile
    private var sink: ((String) -> Unit)? = null

    /** Routes logs to logcat. A no-op in a release build. */
    fun installLogcatSink() {
        if (BuildConfig.DEBUG) sink = { message -> runCatching { Log.d(TAG, message) } }
    }

    fun log(message: String) {
        sink?.invoke(message)
    }
}
