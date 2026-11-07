package tech.mmarca.openvitals.devices.garmin

import tech.mmarca.openvitals.devices.core.DeviceClassification
import tech.mmarca.openvitals.devices.core.DeviceClassifier
import tech.mmarca.openvitals.devices.core.DeviceScanClassifier
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration
import tech.mmarca.openvitals.sensors.ble.BleUuids

/**
 * Classifies a scanned device for Garmin by its advertised name: a watch
 * family is a WATCH, an Edge a BIKE_COMPUTER, anything else null. The
 * member service only surfaces a device; an unknown family stays a sensor.
 */
class GarminDeviceClassifier : DeviceClassifier {

    override fun classify(device: BleDiscoveredDevice): DeviceClassification? {
        val name = device.name
        if (GarminDeviceNames.isGarminBikeComputerName(name)) {
            return DeviceClassification(
                integration = DeviceIntegration.GARMIN,
                kind = BleDeviceKind.BIKE_COMPUTER,
            )
        }
        if (GarminDeviceNames.isGarminWatchName(name)) {
            return DeviceClassification(
                integration = DeviceIntegration.GARMIN,
                kind = BleDeviceKind.WATCH,
            )
        }
        return null
    }
}

/** Classifies an advertisement as a Garmin sync watch by its member service UUID (`0xFE1F`). */
class GarminScanClassifier : DeviceScanClassifier {

    override fun advertisesSyncService(advertisedServiceUuids: Iterable<String>): Boolean =
        advertisedServiceUuids.any { it == BleUuids.GARMIN_MEMBER_SERVICE.toString() }
}
