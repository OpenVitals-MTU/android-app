package tech.mmarca.openvitals.devices.wearos

/**
 * Recognises a wrist smartwatch (WearOS or otherwise) by its advertised
 * Bluetooth name, so the device list can present it as a smartwatch rather
 * than a generic heart-rate sensor.
 *
 * **Presentation only.** A smartwatch discovered this way is still handled on
 * the live BLE-sensor path — it streams heart rate like any GATT sensor — and
 * is NOT a Garmin GFDI sync watch. Its all-day data (sleep, HRV, steps)
 * reaches the app through Health Connect, not here.
 */
object WearOsDeviceNames {

    /**
     * Name fragments that mark a device as a wrist smartwatch. Garmin sync
     * watches never reach here — they are classified as `BleDeviceKind.WATCH`
     * upstream — so this is the fallback for smartwatches the app treats as
     * live sensors.
     */
    private val smartwatchFamilies = listOf(
        Regex("galaxy\\s*watch", RegexOption.IGNORE_CASE),
        Regex("pixel\\s*watch", RegexOption.IGNORE_CASE),
        Regex("ticwatch", RegexOption.IGNORE_CASE),
        Regex("\\bwatch\\b", RegexOption.IGNORE_CASE),
        Regex("\\bwear\\s*os\\b", RegexOption.IGNORE_CASE),
        Regex("amazfit", RegexOption.IGNORE_CASE),
    )

    /**
     * True when [name] looks like a wrist smartwatch. Presentational — a false
     * positive only swaps a sensor's icon, never its behaviour.
     */
    fun isSmartwatchName(name: String?): Boolean {
        if (name == null) return false
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        return smartwatchFamilies.any { it.containsMatchIn(trimmed) }
    }
}
