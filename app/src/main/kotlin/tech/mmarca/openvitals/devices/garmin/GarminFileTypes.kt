package tech.mmarca.openvitals.devices.garmin

/** Every FIT file carries this data type; the sub-type tells them apart. */
private const val FIT_DATA_TYPE = 128

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

    /** Per-device metadata XML (file index 0xFFFD). Not pulled by the sync. */
    DEVICE_XML(8, 255, "deviceXml", wanted = false),

    /** Recorded activity/exercise session — the exercise import path. */
    ACTIVITY(FIT_DATA_TYPE, 4, "activity"),

    /** Scale readings the watch relays from a paired Garmin scale. Imported as weight. */
    WEIGHT(FIT_DATA_TYPE, 9, "weight"),

    /** Intra-day monitoring: the three sub-types the watch may split it across. */
    MONITOR_A(FIT_DATA_TYPE, 15, "monitorA"),
    MONITOR_DAILY(FIT_DATA_TYPE, 28, "monitorDaily"),
    MONITOR(FIT_DATA_TYPE, 32, "monitor"),

    /** Sleep session with stages. */
    SLEEP(FIT_DATA_TYPE, 49, "sleep"),

    /** Fitness metrics: VO2 max, recovery time, training readiness and load. */
    METRICS(FIT_DATA_TYPE, 44, "metrics"),

    /** HRV status readings. */
    HRV_STATUS(FIT_DATA_TYPE, 68, "hrvStatus"),

    /**
     * Health Snapshot: a two-minute recording of SpO2, stress, respiration
     * and Body Battery. Only written when the wearer runs it.
     */
    HSA(FIT_DATA_TYPE, 70, "hsa"),

    /** Fitness-band tracking backups of the watch's daily state. */
    FBT_BACKUP(FIT_DATA_TYPE, 72, "fbtBackup"),
    FBT_PTD_BACKUP(FIT_DATA_TYPE, 74, "fbtPtdBackup"),

    /** Sleep disruption events the watch records beside the sleep file. */
    SLEEP_DISRUPTION(FIT_DATA_TYPE, 79, "sleepDisruption"),
    ;

    companion object {
        /** The type for `(dataType, subType)`, or null when unhandled, which the caller skips. */
        fun fromCodes(dataType: Int, subType: Int): GarminFileType? =
            entries.firstOrNull { it.dataType == dataType && it.subType == subType }

        /** The type for a FileSyncService name, which spells the sub-type as `FIT_TYPE_<n>`. */
        fun fromSyncName(name: String): GarminFileType? {
            val subtype = name.removePrefix("FIT_TYPE_").toIntOrNull()
            return subtype?.let { fromCodes(FIT_DATA_TYPE, it) }
        }
    }
}
