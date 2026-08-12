package tech.mmarca.openvitals.devices.wearos

/**
 * Recognises a wrist smartwatch (WearOS or otherwise) by its advertised
 * Bluetooth name — the allow-list deciding which devices the Watches screen
 * offers to onboard.
 *
 * A match is NOT a Garmin GFDI sync watch: a WearOS watch streams heart rate
 * like any GATT sensor, and its all-day data (sleep, HRV, steps) reaches the
 * app through Health Connect rather than a FIT pull. What onboarding here buys
 * is the companion association.
 *
 * This is the only place a name decides anything. The Sensors screen adds
 * whatever the user picks, watch or not, and never asks.
 */
object WearOsDeviceNames {

    /**
     * Product families that mark a device as a wrist smartwatch. Garmin sync
     * watches never reach here — they are classified as `BleDeviceKind.WATCH`
     * upstream.
     *
     * Explicit families only: this list is the Watches screen's allow-list, so
     * a bare `\bwatch\b` fragment would offer anything merely NAMED like a
     * watch as an onboardable one. Adding a device as a sensor is the Sensors
     * screen's job and consults no name at all — a device missing here is not
     * shut out of the app, only out of the watch path.
     */
    private val smartwatchFamilies = listOf(
        Regex("galaxy\\s*watch", RegexOption.IGNORE_CASE),
        Regex("pixel\\s*watch", RegexOption.IGNORE_CASE),
        Regex("ticwatch", RegexOption.IGNORE_CASE),
        Regex("amazfit", RegexOption.IGNORE_CASE),
    )

    /**
     * True when [name] is a known smartwatch family — the gate for offering it
     * on the watch path.
     */
    fun isSmartwatchName(name: String?): Boolean {
        if (name == null) return false
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        return smartwatchFamilies.any { it.containsMatchIn(trimmed) }
    }
}
