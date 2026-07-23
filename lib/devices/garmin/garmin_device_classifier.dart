import '../../domain/model/ble_sensor_models.dart';
import '../core/registry/device_classification.dart';
import 'garmin_device_names.dart';

/// Claims a scanned device for the Garmin integration when it is a Garmin
/// sync-watch — the advertised member service (`0xFE1F`) or a Garmin product
/// name (see [isGarminSyncDevice]). Everything else it leaves to the next
/// classifier.
class GarminDeviceClassifier implements DeviceClassifier {
  const GarminDeviceClassifier();

  @override
  DeviceClassification? classify(BleDiscoveredDevice device) =>
      isGarminSyncDevice(device)
          ? const DeviceClassification(
              integration: DeviceIntegration.garmin,
              kind: BleDeviceKind.watch,
            )
          : null;
}
