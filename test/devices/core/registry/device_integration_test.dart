import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/devices/core/registry/ble_device_repository_impl.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';

void main() {
  group('watch ownership helpers', () {
    BleSensorDevice watch({DeviceIntegration? integration}) => BleSensorDevice(
          id: 'w',
          displayName: 'w',
          address: 'AA',
          bluetoothName: 'w',
          capabilities: const {},
          enabled: true,
          wheelCircumferenceMm: null,
          addedAt: DateTime.utc(2026),
          kind: BleDeviceKind.watch,
          integration: integration,
        );

    test('a null-integration watch reads as Garmin (legacy)', () {
      final w = watch();
      expect(w.isGarminWatch, isTrue);
      expect(w.isWearosWatch, isFalse);
    });

    test('an explicit Garmin watch is a Garmin watch', () {
      final w = watch(integration: DeviceIntegration.garmin);
      expect(w.isGarminWatch, isTrue);
      expect(w.isWearosWatch, isFalse);
    });

    test('a WearOS watch is not a Garmin watch (the sync-port ownership fix)', () {
      final w = watch(integration: DeviceIntegration.wearos);
      expect(w.isWearosWatch, isTrue);
      expect(w.isGarminWatch, isFalse);
    });

    test('a sensor is neither, whatever the integration', () {
      final sensor = BleSensorDevice(
        id: 's',
        displayName: 's',
        address: 'BB',
        bluetoothName: 's',
        capabilities: const {BleSensorCapability.heartRate},
        enabled: true,
        wheelCircumferenceMm: null,
        addedAt: DateTime.utc(2026),
      );
      expect(sensor.isGarminWatch, isFalse);
      expect(sensor.isWearosWatch, isFalse);
      expect(sensor.isBikeComputer, isFalse);
      expect(sensor.isGarminGfdi, isFalse);
      expect(sensor.isLiveSensorCapable, isTrue);
    });

    test('an Edge bike computer: GFDI + live-sensor, but never a watch', () {
      final edge = BleSensorDevice(
        id: 'e',
        displayName: 'Edge 840',
        address: 'CC',
        bluetoothName: 'Edge 840',
        capabilities: const {},
        enabled: true,
        wheelCircumferenceMm: null,
        addedAt: DateTime.utc(2026),
        kind: BleDeviceKind.bikeComputer,
        integration: DeviceIntegration.garmin,
      );
      expect(edge.isBikeComputer, isTrue);
      expect(edge.isGarminGfdi, isTrue, reason: 'pulls FIT files over GFDI');
      expect(edge.isLiveSensorCapable, isTrue, reason: 'can broadcast live');
      expect(edge.isWatch, isFalse);
      expect(edge.isGarminWatch, isFalse);
      expect(edge.isWearosWatch, isFalse);
    });

    test('a watch is GFDI but never live-sensor-capable', () {
      final w = watch(integration: DeviceIntegration.garmin);
      expect(w.isGarminGfdi, isTrue);
      expect(w.isLiveSensorCapable, isFalse);
    });
  });

  test('a bike computer survives a persistence round-trip', () async {
    SharedPreferences.setMockInitialValues(const {});
    final prefs = await SharedPreferences.getInstance();
    final repo = BleDeviceRepositoryImpl(prefs);
    repo.addDevice(
      displayName: 'Edge 840',
      address: 'E0:48:24:D5:F7:20',
      bluetoothName: 'Edge 840',
      capabilities: const {},
      kind: BleDeviceKind.bikeComputer,
      integration: DeviceIntegration.garmin,
    );

    final reloaded = BleDeviceRepositoryImpl(prefs).devices.single;
    expect(reloaded.kind, BleDeviceKind.bikeComputer);
    expect(reloaded.isBikeComputer, isTrue);
    expect(reloaded.isGarminGfdi, isTrue);
  });

  test('the integration survives a persistence round-trip', () async {
    SharedPreferences.setMockInitialValues(const {});
    final prefs = await SharedPreferences.getInstance();
    final repo = BleDeviceRepositoryImpl(prefs);
    repo.addDevice(
      displayName: 'Galaxy Watch8',
      address: 'A8:D1:62:BE:3A:3B',
      bluetoothName: 'Galaxy Watch8',
      capabilities: const {BleSensorCapability.heartRate},
      kind: BleDeviceKind.watch,
      integration: DeviceIntegration.wearos,
    );

    // A fresh repo over the same prefs re-reads from storage.
    final reloaded = BleDeviceRepositoryImpl(prefs).devices.single;
    expect(reloaded.integration, DeviceIntegration.wearos);
    expect(reloaded.isWearosWatch, isTrue);
    expect(reloaded.isGarminWatch, isFalse);
  });
}
