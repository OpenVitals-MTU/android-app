import '../../domain/model/ble_sensor_models.dart';
import '../core/registry/device_classification.dart';
import 'garmin_device_names.dart';

/// Classifies a scanned device for the Garmin integration by its advertised
/// NAME: a known watch family → a GFDI [BleDeviceKind.watch]; a Garmin Edge → a
/// [BleDeviceKind.bikeComputer]; anything else → `null`, which leaves it to the
/// next classifier and ultimately a plain BLE sensor.
///
/// Name-driven on purpose: the advertised member service (`0xFE1F`) surfaces a
/// device in the scan, but a device that carries it without matching a known
/// Garmin family is NOT swept up as a watch — it stays a plain sensor. So a
/// Garmin watch onboards as a watch, an Edge as a bike computer, and everything
/// else (Garmin or not) as a live sensor.
class GarminDeviceClassifier implements DeviceClassifier {
  const GarminDeviceClassifier();

  @override
  DeviceClassification? classify(BleDiscoveredDevice device) {
    final name = device.name;
    if (isGarminBikeComputerName(name)) {
      return const DeviceClassification(
        integration: DeviceIntegration.garmin,
        kind: BleDeviceKind.bikeComputer,
      );
    }
    if (isGarminWatchName(name)) {
      return const DeviceClassification(
        integration: DeviceIntegration.garmin,
        kind: BleDeviceKind.watch,
      );
    }
    return null;
  }
}
