package tech.mmarca.openvitals.devices.garmin

/**
 * Recognises a Garmin device family from its advertised name: a watch
 * family is a GFDI watch, an Edge a bike computer, anything else a sensor.
 * Families, not Gadgetbridge's exact models, so a future model matches.
 * The member service UUID only surfaces a device in the scan; the name
 * decides. HRM chest straps are absent: they are live sensors.
 */
object GarminDeviceNames {

    /** Watch families. Accented and unaccented spellings both appear. */
    private val garminWatchFamilies = listOf(
        Regex("^v[íi]voactive\\b", RegexOption.IGNORE_CASE),
        Regex("^v[íi]vomove\\b", RegexOption.IGNORE_CASE),
        Regex("^v[íi]vosmart\\b", RegexOption.IGNORE_CASE),
        Regex("^v[íi]vosport\\b", RegexOption.IGNORE_CASE),
        Regex("^f[ēe]nix\\b", RegexOption.IGNORE_CASE),
        Regex("^forerunner\\b", RegexOption.IGNORE_CASE),
        Regex("^instinct\\b", RegexOption.IGNORE_CASE),
        Regex("^venu\\b", RegexOption.IGNORE_CASE),
        Regex("^epix\\b", RegexOption.IGNORE_CASE),
        Regex("^enduro\\b", RegexOption.IGNORE_CASE),
        Regex("^descent\\b", RegexOption.IGNORE_CASE),
        Regex("^tactix\\b", RegexOption.IGNORE_CASE),
        Regex("^quatix\\b", RegexOption.IGNORE_CASE),
        Regex("^lily\\b", RegexOption.IGNORE_CASE),
        Regex("^swim \\d", RegexOption.IGNORE_CASE),
    )

    /**
     * Bike-computer families. Same protocol as watches, but classified as
     * BIKE_COMPUTER. `^edge\b` covers every Edge model.
     */
    private val garminBikeComputerFamilies = listOf(
        Regex("^edge\\b", RegexOption.IGNORE_CASE),
    )

    /** Some models advertise a `Garmin ` prefix; strip it before matching. */
    private val garminPrefix = Regex("^garmin\\s+", RegexOption.IGNORE_CASE)

    /** True when [name] is a Garmin smartwatch. Disjoint from bike computers. Blank is no match. */
    fun isGarminWatchName(name: String?): Boolean {
        val trimmed = strippedGarminName(name) ?: return false
        return garminWatchFamilies.any { it.containsMatchIn(trimmed) }
    }

    /** True when [name] is a Garmin Edge. Disjoint from watches. */
    fun isGarminBikeComputerName(name: String?): Boolean {
        val trimmed = strippedGarminName(name) ?: return false
        return garminBikeComputerFamilies.any { it.containsMatchIn(trimmed) }
    }

    /** True when [name] is any Garmin GFDI sync device. */
    fun isGarminSyncDeviceName(name: String?): Boolean =
        isGarminWatchName(name) || isGarminBikeComputerName(name)

    /** The name with a leading `Garmin ` stripped, or null when blank. */
    private fun strippedGarminName(name: String?): String? {
        if (name == null) return null
        val trimmed = name.trim().replaceFirst(garminPrefix, "")
        return trimmed.ifEmpty { null }
    }
}
