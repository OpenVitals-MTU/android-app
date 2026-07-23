import '../../../domain/model/ble_sensor_models.dart';

/// How the app should treat a discovered device: which integration owns it
/// (`null` = a plain live sensor, owned by no file-sync integration) and as what
/// [BleDeviceKind]. The authoritative mapping from a scanned device to how it is
/// registered and driven.
class DeviceClassification {
  const DeviceClassification({this.integration, required this.kind});

  final DeviceIntegration? integration;
  final BleDeviceKind kind;

  /// The default: a live BLE sensor belonging to no file-sync integration.
  static const DeviceClassification sensor =
      DeviceClassification(kind: BleDeviceKind.sensor);
}

/// One integration's verdict on a scanned device — its [DeviceClassification],
/// or `null` when the device is not its own. Each integration (Garmin, WearOS,
/// …) supplies one; the scanner/onboarding asks them all, so no generic code
/// names a protocol. Mirrors the advertisement-shaped [DeviceScanClassifier]
/// already in `core/ble`, but decides the whole (integration, kind) mapping.
abstract interface class DeviceClassifier {
  DeviceClassification? classify(BleDiscoveredDevice device);
}

/// Maps [device] to how the app should treat it: the first [classifiers] verdict
/// that claims it, else a plain [DeviceClassification.sensor]. Order matters —
/// pass the stronger signal first (Garmin's member service beats a name match).
DeviceClassification classifyDevice(
  BleDiscoveredDevice device,
  Iterable<DeviceClassifier> classifiers,
) {
  for (final classifier in classifiers) {
    final verdict = classifier.classify(device);
    if (verdict != null) return verdict;
  }
  return DeviceClassification.sensor;
}
