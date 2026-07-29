package tech.mmarca.openvitals.notification_listener_native

import android.content.Context
import android.util.Log

/**
 * Whether this build is allowed to narrate what it is doing.
 *
 * The Dart side already decides this once, for the whole app, as
 * `kDiagnosticsEnabled` (`lib/core/diagnostics/diagnostics_build_config.dart`):
 * true in a debug build via `kDebugMode`, true in a nightly because
 * `scripts/ci-release-context.sh` compiles it with
 * `--dart-define=OPENVITALS_DIAGNOSTICS=true`, and false in a store release,
 * which passes neither. This mirrors that decision on the native side rather
 * than inventing a second answer to the same question.
 *
 * **Why it is read from preferences rather than from `BuildConfig`.** The
 * listener service is bound by the system long before any Flutter engine
 * exists — after a reboot, or with the app force-stopped — so at the moment it
 * most wants to log there is nothing to ask. And this Gradle module has its own
 * `BuildConfig`, not the app's, so the flag the app compiles in is not visible
 * here anyway. The app pushes it down with the rest of the configuration, and it
 * persists.
 *
 * **Why it matters.** What this plugin logs is derived from notifications: which
 * apps sent them, what their action buttons are called, how long a reply was.
 * None of that belongs in a shipped app's logcat, where it survives in bug
 * reports — the same rule `garminLog` enforces on the Dart side, and for the
 * same reason. Errors and warnings are exempt: they carry no notification
 * content and are how a broken install gets diagnosed.
 */
internal object Diagnostics {

    /**
     * Cached so a notification storm does not hit SharedPreferences per log
     * line. Refreshed whenever the app pushes its configuration, and read
     * lazily on first use for the case where the service starts first.
     */
    @Volatile
    private var enabled: Boolean? = null

    fun refresh(context: Context) {
        enabled = NotificationStore.readDiagnostics(context)
    }

    fun isEnabled(context: Context): Boolean =
        enabled ?: NotificationStore.readDiagnostics(context).also { enabled = it }

    /** A message worth having while developing, and nowhere else. */
    fun d(context: Context, tag: String, message: String) {
        if (isEnabled(context)) Log.d(tag, message)
    }

    /** Lifecycle worth seeing on a nightly: engines starting, links opening. */
    fun i(context: Context, tag: String, message: String) {
        if (isEnabled(context)) Log.i(tag, message)
    }
}
