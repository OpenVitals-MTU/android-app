package tech.mmarca.openvitals.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Which phone notifications are mirrored to a paired Garmin watch.
 *
 * A **blocklist**, not an allow-list: the feature forwards everything the
 * structural filter lets through, and the user silences the apps they do not
 * want. An allow-list would quietly contradict what the switch says it does,
 * and would leave a newly-installed messaging app silent for no visible
 * reason.
 *
 * The disclosure flag is stored here rather than derived from [enabled]
 * because the two are not the same: consent is given once and remembered, and
 * switching the feature off and on again must not re-prompt.
 *
 * Everything here is mirrored into `NotificationStore`'s own config file by
 * the view-model that writes it — the listener's filter runs before any UI
 * exists, so it cannot read these keys itself.
 *
 * **Key names are Flutter's, verbatim** (`garmin_notifications_enabled`,
 * `garmin_notifications_blocked_packages`,
 * `garmin_notifications_disclosure_accepted`): the phase-5 migrator copies the
 * Flutter values across under exactly these names. Deliberately its OWN
 * SharedPreferences file rather than a corner of [PreferencesRepository] — the
 * main prefs file is a coordination point six workstreams already touch, and
 * this store has exactly one reader and one writer. Because the migrator's
 * typed-copy fallback lands unrouted `garmin_notifications_*` keys in the MAIN
 * file (`openvitals_prefs`), this store adopts any values found there, once,
 * on first construction — so a Flutter-era configuration survives the upgrade
 * without this store ever writing to the shared file.
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

    /**
     * Packages the user has silenced, as a set for the membership test the
     * filter and the picker both do.
     */
    var blockedPackages: Set<String>
        get() = prefs.getStringSet(KEY_BLOCKED, emptySet())?.toSet() ?: emptySet()
        set(value) = prefs.edit { putStringSet(KEY_BLOCKED, value) }

    fun setBlocked(packageName: String, blocked: Boolean) {
        val next = blockedPackages.toMutableSet()
        if (blocked) next.add(packageName) else next.remove(packageName)
        blockedPackages = next
    }

    /**
     * Whether the user has been shown what notification access means and
     * agreed to it. Required by Google Play before the permission is
     * requested, and remembered so turning the feature off and on again does
     * not re-prompt.
     */
    var disclosureAccepted: Boolean
        get() = prefs.getBoolean(KEY_DISCLOSURE_ACCEPTED, false)
        set(value) = prefs.edit { putBoolean(KEY_DISCLOSURE_ACCEPTED, value) }

    /**
     * One-time adoption of values the phase-5 migrator left in the main prefs
     * file. Read-only towards that file — the copies simply become this
     * store's initial state, and a value the user has since changed here is
     * never overwritten (own-file keys win).
     */
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
