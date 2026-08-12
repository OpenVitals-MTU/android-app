package tech.mmarca.openvitals.devices.wearos

import tech.mmarca.openvitals.devices.core.DeviceClassification
import tech.mmarca.openvitals.devices.core.DeviceClassifier
import tech.mmarca.openvitals.domain.model.BleDeviceKind
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration

/**
 * Claims a scanned device for the WearOS integration when its Bluetooth name
 * looks like a wrist smartwatch (Galaxy Watch, Pixel Watch, Wear OS, …; see
 * [WearOsDeviceNames.isSmartwatchName]).
 *
 * A WearOS watch shares no protocol with Garmin — no GFDI/FIT sync. It is a
 * BLE-discoverable live heart-rate source whose recorded data arrives through
 * Health Connect. Classifying it as `(WEAROS, WATCH)` keeps it off the Garmin
 * sync path (see `BleSensorDevice.isGarminWatch`) while still presenting it
 * as a watch.
 */
class WearOsDeviceClassifier : DeviceClassifier {

    override fun classify(device: BleDiscoveredDevice): DeviceClassification? =
        if (WearOsDeviceNames.isSmartwatchName(device.name)) {
            DeviceClassification(
                integration = DeviceIntegration.WEAROS,
                kind = BleDeviceKind.WATCH,
            )
        } else {
            null
        }
}
