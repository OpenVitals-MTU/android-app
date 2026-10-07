package tech.mmarca.openvitals.devices.wearos

import tech.mmarca.openvitals.devices.core.DeviceClassification
import tech.mmarca.openvitals.devices.core.DeviceClassifier
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/**
 * Claims a device for WearOS when its name looks like a wrist smartwatch or
 * its SDP record lists the OpenVitals Wear OS app. The last one holds whatever
 * the watch is called. A wrist-watch Bluetooth class alone is not enough:
 * Garmin, Fitbit and Huawei watches report it too.
 * Classified as `(WEAROS, WATCH)`, off the Garmin sync path.
 */
class WearOsDeviceClassifier : DeviceClassifier {

    override fun classify(device: BleDiscoveredDevice): DeviceClassification? =
        if (
            WearOsDeviceNames.isSmartwatchName(device.name) ||
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

private val OpenVitalsAppUuid = WearLinkProtocol.SERVICE_UUID.toString().lowercase()
