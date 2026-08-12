package tech.mmarca.openvitals.devices.garmin

import tech.mmarca.openvitals.devices.core.DeviceClassification
import tech.mmarca.openvitals.devices.core.DeviceClassifier
import tech.mmarca.openvitals.devices.core.DeviceScanClassifier
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration
import tech.mmarca.openvitals.sensors.ble.BleUuids

/**
 * Classifies a scanned device for the Garmin integration by its advertised
 * NAME: a known watch family → a GFDI [BleDeviceKind.WATCH]; a Garmin Edge →
 * a [BleDeviceKind.BIKE_COMPUTER]; anything else → `null`, which leaves it to
 * the next classifier and ultimately a plain BLE sensor.
 *
 * Name-driven on purpose: the advertised member service (`0xFE1F`) surfaces a
 * device in the scan, but a device that carries it without matching a known
 * Garmin family is NOT swept up as a watch — it stays a plain sensor. So a
 * Garmin watch onboards as a watch, an Edge as a bike computer, and everything
 * else (Garmin or not) as a live sensor.
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

/**
 * Classifies a scanned advertisement as a Garmin file-sync watch by its member
 * service UUID (`0xFE1F`, [BleUuids.GARMIN_MEMBER_SERVICE]).
 *
 * The member service is what a Garmin watch puts in its ADVERTISEMENT (the
 * GFDI transport service is GATT-only, invisible until connected), so it is
 * the one Garmin UUID the shared scanner already carries in its scan filter —
 * this classifier reuses it rather than duplicating the constant.
 */
class GarminScanClassifier : DeviceScanClassifier {

    override fun advertisesSyncService(advertisedServiceUuids: Iterable<String>): Boolean =
        advertisedServiceUuids.any { it == BleUuids.GARMIN_MEMBER_SERVICE.toString() }
}
