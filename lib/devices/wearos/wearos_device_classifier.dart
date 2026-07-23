import '../../domain/model/ble_sensor_models.dart';
import '../core/registry/device_classification.dart';
import 'wearos_device_names.dart';

/// Claims a scanned device for the WearOS integration when its Bluetooth name
/// looks like a wrist smartwatch (Galaxy Watch, Pixel Watch, Wear OS, …; see
/// [isSmartwatchName]).
///
/// A WearOS watch shares no protocol with Garmin — no GFDI/FIT sync. It is a
/// BLE-discoverable live heart-rate source whose recorded data arrives through
/// Health Connect. Classifying it as `(wearos, watch)` keeps it off the Garmin
/// sync path (see `BleSensorDevice.isGarminWatch`) while still presenting it as a
/// watch. See docs/reference/wearos-phase3-decision.md.
class WearosDeviceClassifier implements DeviceClassifier {
  const WearosDeviceClassifier();

  @override
  DeviceClassification? classify(BleDiscoveredDevice device) =>
      isSmartwatchName(device.name)
          ? const DeviceClassification(
              integration: DeviceIntegration.wearos,
              kind: BleDeviceKind.watch,
            )
          : null;
}
