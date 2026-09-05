package tech.mmarca.openvitals.devices.notifications

import android.util.Log
import tech.mmarca.openvitals.BuildConfig

/**
 * Whether this build may narrate what it is doing:
 * `BuildConfig.OPENVITALS_DIAGNOSTICS`, true in debug and nightly builds.
 * What this package logs is derived from notifications and must not reach
 * a shipped logcat. Errors and warnings are exempt.
 */
internal object Diagnostics {

    val isEnabled: Boolean get() = BuildConfig.OPENVITALS_DIAGNOSTICS

    /** A message worth having while developing, and nowhere else. */
    fun d(tag: String, message: String) {
        if (isEnabled) Log.d(tag, message)
    }

    /** Lifecycle worth seeing on a nightly: listeners binding, links opening. */
    fun i(tag: String, message: String) {
        if (isEnabled) Log.i(tag, message)
    }
}
