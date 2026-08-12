package tech.mmarca.openvitals.devices.core

import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/**
 * How the app should treat a discovered device: which integration owns it
 * (`null` = a plain live sensor, owned by no file-sync integration) and as
 * what [BleDeviceKind]. The authoritative mapping from a scanned device to how
 * it is registered and driven.
 */
data class DeviceClassification(
    val integration: DeviceIntegration? = null,
    val kind: BleDeviceKind,
) {
    companion object {
        /** The default: a live BLE sensor belonging to no file-sync integration. */
        val SENSOR = DeviceClassification(kind = BleDeviceKind.SENSOR)
    }
}

/**
 * One integration's verdict on a scanned device — its [DeviceClassification],
 * or `null` when the device is not its own. Each integration (Garmin, WearOS,
 * …) supplies one; the scanner/onboarding asks them all, so no generic code
 * names a protocol. Mirrors the advertisement-shaped [DeviceScanClassifier],
 * but decides the whole (integration, kind) mapping.
 */
fun interface DeviceClassifier {
    fun classify(device: BleDiscoveredDevice): DeviceClassification?
}

/**
 * Maps [device] to how the app should treat it: the first [classifiers]
 * verdict that claims it, else a plain [DeviceClassification.SENSOR]. Order
 * matters — pass the stronger signal first (Garmin's member service beats a
 * name match).
 */
fun classifyDevice(
    device: BleDiscoveredDevice,
    classifiers: Iterable<DeviceClassifier>,
): DeviceClassification {
    for (classifier in classifiers) {
        val verdict = classifier.classify(device)
        if (verdict != null) return verdict
    }
    return DeviceClassification.SENSOR
}
