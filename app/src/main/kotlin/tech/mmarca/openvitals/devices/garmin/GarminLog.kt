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
        // Guarded because a log line must never be able to take down the
        // protocol stack that emitted it. On a device Log.d does not throw; in
        // a JVM unit test the android.util.Log stub does, and this sink is
        // global — one test constructing a service that installs it would
        // otherwise fail every later test in the same fork.
        if (BuildConfig.DEBUG) sink = { message ->
            runCatching { Log.d(TAG, redactSensitiveValues(message)) }
        }
    }

    /** Replaces the sink — for tests that assert on protocol logs. */
    fun installSink(newSink: ((String) -> Unit)?) {
        sink = newSink?.let { target ->
            { message -> target(redactSensitiveValues(message)) }
        }
    }

    fun log(message: String) {
        sink?.invoke(message)
    }

    /** The same, for a message that is expensive to build. A release build never builds it. */
    inline fun logLazy(message: () -> String) {
        if (enabled) log(message())
    }

    /**
     * Last-line protection for debug logging. Protocol payloads can contain
     * fabricated OAuth values and, as the integration grows, may eventually
     * carry real credentials. Call-site redaction remains useful for keeping
     * logs readable, but the sink itself must never trust every caller to
     * remember it.
     */
    private fun redactSensitiveValues(message: String): String {
        var redacted = BearerCredential.replace(message, "Bearer [redacted]")
        redacted = SensitiveAssignment.replace(redacted) { match ->
            "${match.groupValues[1]}${match.groupValues[2]}[redacted]"
        }
        // Covers a secret printed without a label, including the 35-character
        // fake keys used by the Garmin authentication responder. UUIDs retain
        // their dashes and are therefore not mistaken for opaque secrets.
        return OpaqueCredential.replace(redacted, "[redacted]")
    }

    private val SensitiveAssignment = Regex(
        pattern = """(?i)\b(authorization|access[_-]?token|refresh[_-]?token|client[_-]?secret|auth(?:entication)?[_-]?key|password|credentials?)\b(\s*[:=]\s*)(?:\"[^\"]*\"|'[^']*'|[^\s,;&}]+)""",
    )
    private val BearerCredential = Regex(
        pattern = """(?i)\bBearer\s+[A-Za-z0-9._~+/=-]+""",
    )
    private val OpaqueCredential = Regex(
        pattern = """(?<![A-Za-z0-9])[A-Za-z0-9+/=_-]{32,}(?![A-Za-z0-9])""",
    )
}
