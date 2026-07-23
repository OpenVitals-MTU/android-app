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

  test('a Garmin watch product name → (garmin, watch)', () {
    final c = classify(device(name: 'vívoactive 5'));
    expect(c.integration, DeviceIntegration.garmin);
    expect(c.kind, BleDeviceKind.watch);
  });

  test('the member service alone does NOT make an unknown name a watch', () {
    // 0xFE1F surfaces a device in the scan, but the NAME decides the kind — an
    // unrecognised name stays a plain sensor rather than being swept up.
    final c = classify(device(name: 'anything', advertisesSyncService: true));
    expect(c.integration, isNull);
    expect(c.kind, BleDeviceKind.sensor);
  });

  test('a Garmin Edge name → (garmin, bikeComputer)', () {
    final c = classify(device(name: 'Edge 840'));
    expect(c.integration, DeviceIntegration.garmin);
    expect(c.kind, BleDeviceKind.bikeComputer);
  });

  test('a prefixed Edge name → (garmin, bikeComputer)', () {
    final c = classify(device(name: 'Garmin Edge 1040'));
    expect(c.integration, DeviceIntegration.garmin);
    expect(c.kind, BleDeviceKind.bikeComputer);
  });

  test('a member-service-only advert (no distinguishing name) → sensor', () {
    // Without a recognised name there is nothing to classify, so it falls
    // through to a plain live sensor rather than being assumed a watch.
    final c = classify(device(advertisesSyncService: true));
    expect(c.integration, isNull);
    expect(c.kind, BleDeviceKind.sensor);
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

  test('the NAME decides, so a WearOS name is WearOS even with 0xFE1F', () {
    // Contrived: a device advertising the Garmin member service but named like a
    // WearOS watch. Classification is name-based now, so it is WearOS — the
    // member service does not override a recognised name.
    final c =
        classify(device(name: 'Galaxy Watch', advertisesSyncService: true));
    expect(c.integration, DeviceIntegration.wearos);
  });

  test('an unnamed, unremarkable device → sensor', () {
    expect(classify(device()).integration, isNull);
  });
}
