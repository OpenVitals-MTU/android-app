import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/devices/core/registry/device_classification.dart';
import 'package:openvitals/devices/garmin/garmin_device_classifier.dart';
import 'package:openvitals/devices/wearos/wearos_device_classifier.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';

void main() {
  const classifiers = [GarminDeviceClassifier(), WearosDeviceClassifier()];

  BleDiscoveredDevice device({
    String? name,
    bool advertisesSyncService = false,
  }) =>
      BleDiscoveredDevice(
        address: 'AA:BB:CC:DD:EE:FF',
        name: name,
        rssi: -50,
        suggestedCapabilities: const {},
        advertisesSyncService: advertisesSyncService,
      );

  DeviceClassification classify(BleDiscoveredDevice d) =>
      classifyDevice(d, classifiers);

  test('a Garmin member-service advertisement → (garmin, watch)', () {
    final c = classify(device(name: 'anything', advertisesSyncService: true));
    expect(c.integration, DeviceIntegration.garmin);
    expect(c.kind, BleDeviceKind.watch);
  });

  test('a Garmin product name → (garmin, watch)', () {
    final c = classify(device(name: 'vívoactive 5'));
    expect(c.integration, DeviceIntegration.garmin);
    expect(c.kind, BleDeviceKind.watch);
  });

  test('a WearOS smartwatch name → (wearos, watch)', () {
    final c = classify(device(name: 'Galaxy Watch8 (89FZ)'));
    expect(c.integration, DeviceIntegration.wearos);
    expect(c.kind, BleDeviceKind.watch);
  });

  test('a live heart-rate strap → a plain sensor', () {
    final c = classify(device(name: 'TICKR'));
    expect(c.integration, isNull);
    expect(c.kind, BleDeviceKind.sensor);
    expect(c, same(DeviceClassification.sensor));
  });

  test('Garmin wins when a device matches both (member service beats a name)', () {
    // Contrived: a device advertising the member service AND named like a watch.
    final c =
        classify(device(name: 'Galaxy Watch', advertisesSyncService: true));
    expect(c.integration, DeviceIntegration.garmin);
  });

  test('an unnamed, unremarkable device → sensor', () {
    expect(classify(device()).integration, isNull);
  });
}
