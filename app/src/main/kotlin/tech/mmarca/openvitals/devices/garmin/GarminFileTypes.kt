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

    /** Per-device metadata XML (file index 0xFFFD). Not pulled by the sync. */
    DEVICE_XML(8, 255, "deviceXml", wanted = false),

    /** Recorded activity/exercise session — the exercise import path. */
    ACTIVITY(128, 4, "activity"),

    /** Intra-day monitoring: the three sub-types the watch may split it across. */
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
    ;

    companion object {
        /** The type for `(dataType, subType)`, or null when unhandled, which the caller skips. */
        fun fromCodes(dataType: Int, subType: Int): GarminFileType? =
            entries.firstOrNull { it.dataType == dataType && it.subType == subType }
    }
}
