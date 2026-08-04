package tech.mmarca.openvitals.devices.garmin

/**
 * The subset of Garmin's `FileType.FILETYPE` table this app cares about.
 *
 * Gadgetbridge enumerates ~90 file types; OpenVitals only needs the FIT files
 * its wellness importer can turn into Health Connect records, plus the two
 * virtual types the transport itself uses (the root DIRECTORY, and
 * DEVICE_XML). Every FIT file has data type 128; the sub-type distinguishes
 * them.
 *
 * Ported from `FileType.java` (AGPLv3). The [wanted] flag is narrower than
 * Gadgetbridge's `pull`: it marks only the types the FIT wellness import can
 * actually consume, so a sync does not spend airtime pulling golf scorecards
 * it would only skip.
 *
 * [label] is the Flutter build's enum identifier for the type, kept because
 * it is a persisted spelling: [GarminFileStore] names saved files with it, so
 * files written by the Flutter app and by this one sort together.
 */
enum class GarminFileType(
    val dataType: Int,
    val subType: Int,
    val label: String,
    /**
     * Whether the sync should download this type. False for the virtual
     * types, which are handled by the transport itself rather than imported.
     */
    val wanted: Boolean = true,
) {
    /** The root directory listing (file index 0). Not a FIT file. */
    DIRECTORY(0, 0, "directory", wanted = false),

    /** Per-device metadata XML (file index 0xFFFD). Not pulled by the sync. */
    DEVICE_XML(8, 255, "deviceXml", wanted = false),

    /** Recorded activity/exercise session — the exercise import path. */
    ACTIVITY(128, 4, "activity"),

    /**
     * Intra-day monitoring (steps, HR, respiration, calories) — the three
     * sub-types the watch may split it across.
     */
    MONITOR_A(128, 15, "monitorA"),
    MONITOR_DAILY(128, 28, "monitorDaily"),
    MONITOR(128, 32, "monitor"),

    /** Sleep session with stages. */
    SLEEP(128, 49, "sleep"),

    /**
     * Fitness metrics: VO2 max, recovery time, training readiness and load.
     *
     * The watch keeps these listed and re-offers them every sync — they were
     * being skipped as an unrecognised type long after the transport worked.
     */
    METRICS(128, 44, "metrics"),

    /** HRV status readings. */
    HRV_STATUS(128, 68, "hrvStatus"),

    /**
     * Health Snapshot: a two-minute on-demand recording of SpO2, stress,
     * respiration and Body Battery, each as packed sample arrays.
     *
     * Only written when the wearer runs Health Snapshot on the watch, so an
     * empty directory here means "none recorded", not "not supported".
     */
    HSA(128, 70, "hsa"),
    ;

    companion object {
        /**
         * The type for a directory entry's `(dataType, subType)`, or null
         * when it is one this app does not handle — which the caller skips,
         * exactly as the bulk importer skips an unmappable FIT file (skipped,
         * not failed).
         */
        fun fromCodes(dataType: Int, subType: Int): GarminFileType? =
            entries.firstOrNull { it.dataType == dataType && it.subType == subType }
    }
}
