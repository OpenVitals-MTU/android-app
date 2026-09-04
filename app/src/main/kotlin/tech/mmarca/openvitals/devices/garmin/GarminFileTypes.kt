package tech.mmarca.openvitals.devices.garmin

/**
 * The Garmin file types this app cares about, from `FileType.java`
 * (AGPLv3). Every FIT file has data type 128; the sub-type distinguishes
 * them. [wanted] marks what the wellness import can consume. [label] is the
 * Flutter build's identifier, persisted in file names by [GarminFileStore].
 */
enum class GarminFileType(
    val dataType: Int,
    val subType: Int,
    val label: String,
    /** Whether the sync downloads this type. False for the virtual types. */
    val wanted: Boolean = true,
) {
    /** The root directory listing (file index 0). Not a FIT file. */
    DIRECTORY(0, 0, "directory", wanted = false),

    /** Undocumented placeholder emitted by Venu/Instinct directory listings. */
    UNKNOWN_1_0(1, 0, "unknown1_0", wanted = false),

    /** Per-device metadata XML (file index 0xFFFD). Not pulled by the sync. */
    DEVICE_XML(8, 255, "deviceXml", wanted = false),

    /** Recorded activity/exercise session — the exercise import path. */
    ACTIVITY(128, 4, "activity"),

    /** Body weight from the FIT weight-scale profile. */
    WEIGHT(128, 9, "weight"),

    /**
     * Intra-day monitoring (steps, HR, respiration, calories) — the three
     * sub-types the watch may split it across.
     */
    MONITOR_A(128, 15, "monitorA"),
    MONITOR_DAILY(128, 28, "monitorDaily"),
    MONITOR(128, 32, "monitor"),

    /** Sleep session with stages. */
    SLEEP(128, 49, "sleep"),

    /** Fitness metrics: VO2 max, recovery time, training readiness and load. */
    METRICS(128, 44, "metrics"),

    /** HRV status readings. */
    HRV_STATUS(128, 68, "hrvStatus"),

    /**
     * Health Snapshot: a two-minute recording of SpO2, stress, respiration
     * and Body Battery. Only written when the wearer runs it.
     */
    HSA(128, 70, "hsa"),

    /** Physiology backups can carry the same monitoring messages as live files. */
    FBT_BACKUP(128, 72, "fbtBackup"),
    FBT_PTD_BACKUP(128, 74, "fbtPtdBackup"),

    /** Additional sleep-stage/disruption data on newer Instinct firmware. */
    SLEEP_DISRUPTION(128, 79, "sleepDisruption"),
    ;

    companion object {
        /** The type for `(dataType, subType)`, or null when unhandled, which the caller skips. */
        fun fromCodes(dataType: Int, subType: Int): GarminFileType? =
            entries.firstOrNull { it.dataType == dataType && it.subType == subType }

        /** New FileSyncService names FIT files as `FIT_TYPE_<subtype>`. */
        fun fromSyncName(name: String): GarminFileType? {
            val subtype = name.removePrefix("FIT_TYPE_").toIntOrNull()
            return subtype?.let { fromCodes(128, it) }
        }
    }
}
