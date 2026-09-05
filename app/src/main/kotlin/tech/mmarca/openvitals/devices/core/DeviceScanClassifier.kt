package tech.mmarca.openvitals.devices.core

/**
 * Decides, per integration, whether an advertisement is a file-sync device
 * rather than a live sensor. A port, so the generic scanner carries no
 * protocol knowledge.
 */
fun interface DeviceScanClassifier {
    /** True when this integration claims [advertisedServiceUuids] (lowercase 128-bit). */
    fun advertisesSyncService(advertisedServiceUuids: Iterable<String>): Boolean
}
