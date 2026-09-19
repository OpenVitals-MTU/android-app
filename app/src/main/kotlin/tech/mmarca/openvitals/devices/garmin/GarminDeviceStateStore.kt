package tech.mmarca.openvitals.devices.garmin

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import java.time.DayOfWeek
import org.json.JSONArray
import tech.mmarca.openvitals.devices.core.sync.AutoSyncInterval

/**
 * Garmin's per-device state, kept out of the generic registry: the
 * capability bitmap from the last handshake, and which files a previous
 * sync pulled. SharedPreferences-backed, keyed by `deviceId`. The key names
 * are the Flutter build's; the migrator copies the values across. Do not
 * read `FlutterSharedPreferences` here. Lists are JSON arrays to keep order.
 */
class GarminDeviceStateStore(private val prefs: SharedPreferences) {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE),
    )

    /** What the watch declared in its last handshake. Empty if never synced. */
    fun capabilities(deviceId: String): Set<GarminCapability> {
        val raw = prefs.readStringList(capabilitiesPrefsKey(deviceId)) ?: return emptySet()
        // Matched by wire name, not index, so naming a flag cannot rot stored data.
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

    /** Files a previous sync pulled, by `GarminDirectoryEntry.dedupKey`. */
    fun syncedFileKeys(deviceId: String): Set<String> =
        prefs.readStringList(syncedKeysPrefsKey(deviceId))?.toSet() ?: emptySet()

    fun recordSyncedFileKeys(deviceId: String, keys: Iterable<String>) {
        if (!keys.any()) return
        val prefsKey = syncedKeysPrefsKey(deviceId)
        // A list keeps insertion order, so trimming from the front drops the oldest keys.
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
     * Whether the watch still needs the pair-flow completion trio. Set on
     * onboarding, cleared once a session has sent it.
     */
    fun setupWizardPending(deviceId: String): Boolean =
        prefs.getBoolean(setupWizardPrefsKey(deviceId), false)

    fun setSetupWizardPending(deviceId: String, pending: Boolean) {
        prefs.edit { putBoolean(setupWizardPrefsKey(deviceId), pending) }
    }

    /** Whether the watch streams live readings over the held link. Off by default: it costs battery. */
    fun liveReadings(deviceId: String): Boolean =
        prefs.getBoolean(liveReadingsPrefsKey(deviceId), false)

    fun setLiveReadings(deviceId: String, enabled: Boolean) {
        prefs.edit { putBoolean(liveReadingsPrefsKey(deviceId), enabled) }
    }

    /** Whether the watch may read the phone's calendar. Off by default. */
    fun calendarSync(deviceId: String): Boolean =
        prefs.getBoolean(calendarSyncPrefsKey(deviceId), false)

    fun setCalendarSync(deviceId: String, enabled: Boolean) {
        prefs.edit { putBoolean(calendarSyncPrefsKey(deviceId), enabled) }
    }

    /** Whether CoMaps guidance is shown on the watch. Off by default. */
    fun navigationOnWatch(deviceId: String): Boolean =
        prefs.getBoolean(navigationOnWatchPrefsKey(deviceId), false)

    fun setNavigationOnWatch(deviceId: String, enabled: Boolean) {
        prefs.edit { putBoolean(navigationOnWatchPrefsKey(deviceId), enabled) }
    }

    /** Whether the watch's music controls drive the phone's player. Off by default. */
    fun musicControls(deviceId: String): Boolean =
        prefs.getBoolean(musicControlsPrefsKey(deviceId), false)

    fun setMusicControls(deviceId: String, enabled: Boolean) {
        prefs.edit { putBoolean(musicControlsPrefsKey(deviceId), enabled) }
    }

    /**
     * Whether the link is held open whenever the watch is in range. On by
     * default: weather, find-my-phone, live readings and guidance all ride it.
     * A wearer who turns it off has that written down.
     */
    fun stayConnected(deviceId: String): Boolean =
        prefs.getBoolean(stayConnectedPrefsKey(deviceId), true)

    fun setStayConnected(deviceId: String, enabled: Boolean) {
        prefs.edit { putBoolean(stayConnectedPrefsKey(deviceId), enabled) }
    }

    /**
     * How often the watch syncs on its own. Off by default. Stored as
     * minutes, so a dropped interval degrades to off, not another schedule.
     */
    fun autoSyncInterval(deviceId: String): AutoSyncInterval =
        AutoSyncInterval.fromMinutes(prefs.getInt(autoSyncPrefsKey(deviceId), 0))

    fun setAutoSyncInterval(deviceId: String, interval: AutoSyncInterval) {
        prefs.edit { putInt(autoSyncPrefsKey(deviceId), interval.minutes) }
    }

    /** The listing protocol a previous sync proved for this watch. */
    fun syncProtocol(deviceId: String): GarminSyncProtocol {
        val stored = prefs.getString(syncProtocolPrefsKey(deviceId), null)
        return GarminSyncProtocol.entries.firstOrNull { it.name == stored }
            ?: GarminSyncProtocol.UNKNOWN
    }

    fun recordSyncProtocol(deviceId: String, protocol: GarminSyncProtocol) {
        if (protocol == GarminSyncProtocol.UNKNOWN) return
        prefs.edit { putString(syncProtocolPrefsKey(deviceId), protocol.name) }
    }

    /** The alarms set here for a watch with no settings tree. The watch's own are never read. */
    fun alarms(deviceId: String): List<GarminAlarm> =
        prefs.readStringList(alarmsPrefsKey(deviceId)).orEmpty().mapNotNull(::decodeAlarm)

    fun setAlarms(deviceId: String, alarms: List<GarminAlarm>) {
        prefs.writeStringList(alarmsPrefsKey(deviceId), alarms.map(::encodeAlarm))
    }

    /** The list the watch last accepted, or null when none was sent. */
    fun sentAlarms(deviceId: String): List<GarminAlarm>? =
        prefs.readStringList(sentAlarmsPrefsKey(deviceId))?.mapNotNull(::decodeAlarm)

    fun recordSentAlarms(deviceId: String, alarms: List<GarminAlarm>) {
        prefs.writeStringList(sentAlarmsPrefsKey(deviceId), alarms.map(::encodeAlarm))
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
            remove(syncProtocolPrefsKey(deviceId))
            remove(musicControlsPrefsKey(deviceId))
            remove(alarmsPrefsKey(deviceId))
            remove(sentAlarmsPrefsKey(deviceId))
        }
    }

    private fun syncedKeysPrefsKey(deviceId: String) = "ble_synced_files_$deviceId"

    private fun capabilitiesPrefsKey(deviceId: String) = "garmin_capabilities_$deviceId"

    private fun stayConnectedPrefsKey(deviceId: String) = "garmin_stay_connected_$deviceId"

    private fun setupWizardPrefsKey(deviceId: String) = "garmin_setup_wizard_pending_$deviceId"

    private fun liveReadingsPrefsKey(deviceId: String) = "garmin_live_readings_$deviceId"

    private fun calendarSyncPrefsKey(deviceId: String) = "garmin_calendar_sync_$deviceId"

    private fun navigationOnWatchPrefsKey(deviceId: String) = "garmin_navigation_on_watch_$deviceId"

    private fun musicControlsPrefsKey(deviceId: String) = "garmin_music_controls_$deviceId"

    private fun autoSyncPrefsKey(deviceId: String) = "garmin_auto_sync_minutes_$deviceId"

    private fun syncProtocolPrefsKey(deviceId: String) =
        "garmin_sync_protocol_$deviceId"

    private fun alarmsPrefsKey(deviceId: String) = "garmin_alarms_$deviceId"

    private fun sentAlarmsPrefsKey(deviceId: String) = "garmin_alarms_sent_$deviceId"

    companion object {
        const val PREFS_FILE = "garmin_device_state"

        /** Cap on remembered file keys per watch, so the list cannot grow without bound. */
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

/** One alarm as `minuteOfDay,repeatMask,enabled,sound,backlight,label`. Enums go by name. */
private fun encodeAlarm(alarm: GarminAlarm): String = listOf(
    alarm.minuteOfDay,
    alarm.repeatMask,
    alarm.enabled,
    alarm.sound.name,
    alarm.backlight,
    alarm.label.name,
).joinToString(",")

/** Null for a line this build cannot read, so one bad entry does not lose the rest. */
private fun decodeAlarm(line: String): GarminAlarm? {
    val parts = line.split(",")
    if (parts.size != 6) return null
    val minuteOfDay = parts[0].toIntOrNull()?.takeIf { it in 0 until 24 * 60 } ?: return null
    val mask = parts[1].toLongOrNull() ?: return null
    return GarminAlarm(
        hour = minuteOfDay / 60,
        minute = minuteOfDay % 60,
        days = DayOfWeek.entries.filter { mask and (1L shl (it.value - 1)) != 0L }.toSet(),
        enabled = parts[2].toBoolean(),
        sound = GarminAlarmSound.entries.firstOrNull { it.name == parts[3] } ?: return null,
        backlight = parts[4].toBoolean(),
        label = GarminAlarmLabel.entries.firstOrNull { it.name == parts[5] } ?: return null,
    )
}
