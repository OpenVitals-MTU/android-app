package tech.mmarca.openvitals.devices.core

/**
 * Decides, per integration, whether a scanned advertisement belongs to a
 * **file-sync device** (a watch to onboard and pull files from) rather than a
 * live-streaming sensor.
 *
 * A **port**, deliberately: the generic BLE scanner must not know that
 * `0xFE1F` means "Garmin watch". Each integration supplies one of these, and
 * the scanner asks them all — so the shared scan model carries no protocol
 * knowledge and a second integration (WearOS, …) plugs in without touching the
 * coordinator.
 */
fun interface DeviceScanClassifier {
    /**
     * True when this integration claims a device advertising
     * [advertisedServiceUuids] (lowercase 128-bit) as a file-sync device.
     */
    fun advertisesSyncService(advertisedServiceUuids: Iterable<String>): Boolean
}
