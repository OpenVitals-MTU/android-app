package tech.mmarca.openvitals.devices.notifications

import android.util.Log
import tech.mmarca.openvitals.BuildConfig

/**
 * Whether this build is allowed to narrate what it is doing.
 *
 * Lifted from the Flutter build's `notification_listener_native/Diagnostics.kt`
 * and simplified: there the flag had to be pushed down from Dart and persisted,
 * because the listener service was bound long before any Flutter engine existed
 * and the plugin's own Gradle module could not see the app's `BuildConfig`. In
 * a single-module Kotlin app, `BuildConfig.OPENVITALS_DIAGNOSTICS` is the one
 * answer to the one question — true in a debug build and in a nightly, false in
 * a store release — so it is read directly and nothing is stored.
 *
 * **Why it matters.** What this package logs is derived from notifications:
 * which apps sent them, what their action buttons are called, how long a reply
 * was. None of that belongs in a shipped app's logcat, where it survives in bug
 * reports — the same rule `GarminLog` enforces for the protocol stack, and for
 * the same reason. Errors and warnings are exempt: they carry no notification
 * content and are how a broken install gets diagnosed.
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
