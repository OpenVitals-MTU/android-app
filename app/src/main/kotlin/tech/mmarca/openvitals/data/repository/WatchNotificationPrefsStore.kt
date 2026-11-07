package tech.mmarca.openvitals.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which phone notifications are mirrored to a Garmin watch: a blocklist. The disclosure
 * flag is stored so toggling does not re-prompt. Mirrored into `NotificationStore`'s
 * file, since the listener runs before any UI.
 *
 * Key names are Flutter's. Values the migrator left in the main file are adopted once.
 */
@Singleton
class WatchNotificationPrefsStore(
    private val prefs: SharedPreferences,
    legacyMainPrefs: SharedPreferences? = null,
) {

    @Inject
    constructor(@ApplicationContext context: Context) : this(
        prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE),
        legacyMainPrefs = context.getSharedPreferences(
            PreferencesRepository.PREFS_FILE,
            Context.MODE_PRIVATE,
        ),
    )

    init {
        if (legacyMainPrefs != null) adoptMigratedValues(legacyMainPrefs)
    }

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    /** Packages the user has silenced. */
    var blockedPackages: Set<String>
        get() = prefs.getStringSet(KEY_BLOCKED, emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit { putStringSet(KEY_BLOCKED, value) }

    fun setBlocked(packageName: String, blocked: Boolean) {
        val next = blockedPackages.toMutableSet()
        if (blocked) next.add(packageName) else next.remove(packageName)
        blockedPackages = next
    }

    /** Whether the user accepted the disclosure. Required by Google Play before the request. */
    var disclosureAccepted: Boolean
        get() = prefs.getBoolean(KEY_DISCLOSURE_ACCEPTED, false)
        set(value) = prefs.edit { putBoolean(KEY_DISCLOSURE_ACCEPTED, value) }

    /** One-time adoption of values the migrator left in the main file. Own-file keys win. */
    private fun adoptMigratedValues(main: SharedPreferences) {
        prefs.edit {
            if (!prefs.contains(KEY_ENABLED) && main.contains(KEY_ENABLED)) {
                putBoolean(KEY_ENABLED, main.getBoolean(KEY_ENABLED, false))
            }
            if (!prefs.contains(KEY_BLOCKED) && main.contains(KEY_BLOCKED)) {
                putStringSet(KEY_BLOCKED, main.getStringSet(KEY_BLOCKED, emptySet()))
            }
            if (!prefs.contains(KEY_DISCLOSURE_ACCEPTED) &&
                main.contains(KEY_DISCLOSURE_ACCEPTED)
            ) {
                putBoolean(
                    KEY_DISCLOSURE_ACCEPTED,
                    main.getBoolean(KEY_DISCLOSURE_ACCEPTED, false),
                )
            }
        }
    }

    companion object {
        const val PREFS_FILE = "garmin_watch_notifications"
        private const val KEY_ENABLED = "garmin_notifications_enabled"
        private const val KEY_BLOCKED = "garmin_notifications_blocked_packages"
        private const val KEY_DISCLOSURE_ACCEPTED = "garmin_notifications_disclosure_accepted"
    }
}
