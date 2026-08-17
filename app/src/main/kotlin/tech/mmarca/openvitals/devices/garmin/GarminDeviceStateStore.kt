package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import tech.mmarca.openvitals.devices.core.sync.AutoSyncInterval

/**
 * Garmin's own per-device state, kept out of the generic `BleDeviceRepository`
 * so that registry carries no Garmin knowledge: the GFDI capability bitmap a
 * watch declared in its last handshake, and which of its files a previous sync
 * already pulled.
 *
 * SharedPreferences-backed and keyed by the registry's `deviceId`. The KEY
 * names (`ble_synced_files_<deviceId>`, `garmin_capabilities_<deviceId>`) are
 * exactly what the Flutter build used — there they lived in
 * `FlutterSharedPreferences` under a `flutter.` prefix; this store uses its
 * OWN prefs file with the un-prefixed names, and phase 5's migrator copies the
 * Flutter values across. Do NOT read `FlutterSharedPreferences` here.
 *
 * Fire-and-forget like the registry (SharedPreferences' async apply), so there
 * is nothing to Result-type. Ordered string lists are encoded as JSON arrays —
 * a `StringSet` would lose the insertion order the synced-file cap depends on.
 */
class GarminDeviceStateStore(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE),
    )

    /**
     * What the watch declared it can do, from the last handshake. Empty when a
     * watch has never synced.
     */
    fun capabilities(deviceId: String): Set<GarminCapability> {
        val raw = prefs.readStringList(capabilitiesPrefsKey(deviceId)) ?: return emptySet()
        // Matched by WIRE NAME, not index: the enum's order is the bitmap's
        // order, so storing indexes would rot the moment a flag is named.
        val byName = GarminCapability.entries.associateBy { it.wireName }
        return raw.mapNotNull { byName[it] }.toSet()
    }

    fun recordCapabilities(deviceId: String, capabilities: Set<GarminCapability>) {
        if (capabilities.isEmpty()) return
        prefs.writeStringList(
            capabilitiesPrefsKey(deviceId),
            capabilities.map { it.wireName },
        )
    }

    /**
     * Which of a watch's files a previous sync already pulled, keyed by
     * `GarminDirectoryEntry.dedupKey`.
     */
    fun syncedFileKeys(deviceId: String): Set<String> =
        prefs.readStringList(syncedKeysPrefsKey(deviceId))?.toSet() ?: emptySet()

    fun recordSyncedFileKeys(deviceId: String, keys: Iterable<String>) {
        if (!keys.any()) return
        val prefsKey = syncedKeysPrefsKey(deviceId)
        // Order matters for the cap: a list keeps insertion order, so trimming
        // from the front drops the OLDEST keys, which are the least likely to
        // be re-offered by the watch.
        val existing = prefs.readStringList(prefsKey).orEmpty()
        val existingSet = existing.toHashSet()
        val merged = existing + keys.filterNot { it in existingSet }
        val trimmed = if (merged.size > MAX_SYNCED_FILE_KEYS) {
            merged.subList(merged.size - MAX_SYNCED_FILE_KEYS, merged.size)
        } else {
            merged
        }
        prefs.writeStringList(prefsKey, trimmed)
    }

    fun clearSyncedFileKeys(deviceId: String) {
        prefs.edit { remove(syncedKeysPrefsKey(deviceId)) }
    }

    /**
     * Drops everything this store holds for [deviceId] — capabilities and
     * synced file keys. Called when a watch is forgotten, so a re-pairing
     * starts clean (re-learning capabilities from a fresh handshake and
     * re-fetching files rather than trusting a record of a device that is no
     * longer here).
     */
    /**
     * Whether this watch should be held connected whenever it is in range —
     * the companion mode the watch was designed around. Default off: a held
     * link spends watch and phone battery, and that trade is the user's.
     */
    /**
     * Whether the watch still needs the pair-flow completion trio
     * (PAIR_COMPLETE / SYNC_COMPLETE / SETUP_WIZARD_COMPLETE). Set on
     * onboarding, cleared once a session has sent it — a factory-fresh watch
     * sits on its "connect to the app" wizard screen until it hears this.
     */
    fun setupWizardPending(deviceId: String): Boolean =
        prefs.getBoolean(setupWizardPrefsKey(deviceId), false)

    fun setSetupWizardPending(deviceId: String, pending: Boolean) {
        prefs.edit { putBoolean(setupWizardPrefsKey(deviceId), pending) }
    }

    /**
     * Whether the watch should stream live readings over the held link.
     * Off by default: an open stream keeps the watch's sensor and radio busy,
     * which is the wearer's battery to spend.
     */
    fun liveReadings(deviceId: String): Boolean =
        prefs.getBoolean(liveReadingsPrefsKey(deviceId), false)

    fun setLiveReadings(deviceId: String, enabled: Boolean) {
        prefs.edit { putBoolean(liveReadingsPrefsKey(deviceId), enabled) }
    }

    /**
     * Whether the watch may read the phone's calendar. Off by default: this
     * is the single most personal thing the app can hand a watch, and it must
     * never happen because a default said so.
     */
    fun calendarSync(deviceId: String): Boolean =
        prefs.getBoolean(calendarSyncPrefsKey(deviceId), false)

    fun setCalendarSync(deviceId: String, enabled: Boolean) {
        prefs.edit { putBoolean(calendarSyncPrefsKey(deviceId), enabled) }
    }

    fun stayConnected(deviceId: String): Boolean =
        prefs.getBoolean(stayConnectedPrefsKey(deviceId), false)

    fun setStayConnected(deviceId: String, enabled: Boolean) {
        prefs.edit { putBoolean(stayConnectedPrefsKey(deviceId), enabled) }
    }

    /**
     * How often this watch is synced on its own. Off by default: a sync wakes
     * both radios and spends the wearer's battery, so it stays something they
     * asked for.
     *
     * Stored as MINUTES rather than an ordinal, so an interval this build
     * offers and a later one drops degrades to off instead of silently
     * becoming a different schedule.
     */
    fun autoSyncInterval(deviceId: String): AutoSyncInterval =
        AutoSyncInterval.fromMinutes(prefs.getInt(autoSyncPrefsKey(deviceId), 0))

    fun setAutoSyncInterval(deviceId: String, interval: AutoSyncInterval) {
        prefs.edit { putInt(autoSyncPrefsKey(deviceId), interval.minutes) }
    }

    fun clear(deviceId: String) {
        clearSyncedFileKeys(deviceId)
        prefs.edit {
            remove(capabilitiesPrefsKey(deviceId))
            remove(stayConnectedPrefsKey(deviceId))
            remove(setupWizardPrefsKey(deviceId))
            remove(liveReadingsPrefsKey(deviceId))
            remove(calendarSyncPrefsKey(deviceId))
            remove(autoSyncPrefsKey(deviceId))
        }
    }

    private fun syncedKeysPrefsKey(deviceId: String) = "ble_synced_files_$deviceId"

    private fun capabilitiesPrefsKey(deviceId: String) = "garmin_capabilities_$deviceId"

    private fun stayConnectedPrefsKey(deviceId: String) = "garmin_stay_connected_$deviceId"

    private fun setupWizardPrefsKey(deviceId: String) = "garmin_setup_wizard_pending_$deviceId"

    private fun liveReadingsPrefsKey(deviceId: String) = "garmin_live_readings_$deviceId"

    private fun calendarSyncPrefsKey(deviceId: String) = "garmin_calendar_sync_$deviceId"

    private fun autoSyncPrefsKey(deviceId: String) = "garmin_auto_sync_minutes_$deviceId"

    companion object {
        const val PREFS_FILE = "garmin_device_state"

        /**
         * Cap on remembered file keys per watch. A few years of daily monitor,
         * sleep and HRV files plus activities lands well inside this; the cap
         * only exists so the list cannot grow without bound.
         */
        private const val MAX_SYNCED_FILE_KEYS = 4000

        private fun SharedPreferences.readStringList(key: String): List<String>? {
            val raw = getString(key, null) ?: return null
            return runCatching {
                val array = JSONArray(raw)
                List(array.length()) { array.getString(it) }
            }.getOrNull()
        }

        private fun SharedPreferences.writeStringList(key: String, values: List<String>) {
            edit { putString(key, JSONArray(values).toString()) }
        }
    }
}
