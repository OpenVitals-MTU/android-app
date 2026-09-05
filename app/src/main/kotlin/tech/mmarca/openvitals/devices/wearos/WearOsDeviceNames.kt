package tech.mmarca.openvitals.devices.wearos

/**
 * Recognises a wrist smartwatch by its advertised name: the allow-list for
 * the Watches screen. A match is not a Garmin sync watch. The Sensors
 * screen never asks.
 */
object WearOsDeviceNames {

    /**
     * Explicit families only: a bare `watch` fragment would offer anything
     * named like one. A device missing here can still be added as a sensor.
     */
    private val smartwatchFamilies = listOf(
        Regex("galaxy\\s*watch", RegexOption.IGNORE_CASE),
        Regex("pixel\\s*watch", RegexOption.IGNORE_CASE),
        Regex("ticwatch", RegexOption.IGNORE_CASE),
        Regex("amazfit", RegexOption.IGNORE_CASE),
    )

    /** True when [name] is a known smartwatch family. */
    fun isSmartwatchName(name: String?): Boolean {
        if (name == null) return false
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        return smartwatchFamilies.any { it.containsMatchIn(trimmed) }
    }
}
