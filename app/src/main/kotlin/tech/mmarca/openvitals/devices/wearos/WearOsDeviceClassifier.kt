package tech.mmarca.openvitals.devices.wearos

import tech.mmarca.openvitals.devices.core.DeviceClassification
import tech.mmarca.openvitals.devices.core.DeviceClassifier
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/**
 * Claims a device for WearOS when its name looks like a wrist smartwatch, its
 * bond's class is a wrist watch, or its SDP record lists the OpenVitals Wear
 * OS app. The last one holds whatever the watch is called.
 * Classified as `(WEAROS, WATCH)`, off the Garmin sync path.
 */
class WearOsDeviceClassifier : DeviceClassifier {

    override fun classify(device: BleDiscoveredDevice): DeviceClassification? =
        if (
            WearOsDeviceNames.isSmartwatchName(device.name) ||
            device.isWristWatchClass ||
            OpenVitalsAppUuid in device.classicServiceUuids
        ) {
            DeviceClassification(
                integration = DeviceIntegration.WEAROS,
                kind = BleDeviceKind.WATCH,
            )
        } else {
            null
        }
}

private val OpenVitalsAppUuid = BluetoothWearOsNodePort.OPENVITALS_WEAR_APP_UUID.toString().lowercase()
