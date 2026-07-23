import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/devices/core/registry/ble_device_repository_impl.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';

void main() {
  late BleDeviceRepositoryImpl repo;

  Future<BleSensorDevice> setUpOneDevice() async {
    SharedPreferences.setMockInitialValues(const {});
    final prefs = await SharedPreferences.getInstance();
    repo = BleDeviceRepositoryImpl(prefs);
    return repo.addDevice(
      displayName: 'Strap',
      address: 'AA:BB:CC:DD:EE:FF',
      bluetoothName: 'Strap',
      capabilities: const {BleSensorCapability.heartRate},
    );
  }

  test('updateBatteryLevel stores a changed value', () async {
    final device = await setUpOneDevice();

    repo.updateBatteryLevel(device.id, 80);

    final stored = repo.devices.single;
    expect(stored.batteryPercent, 80);
    expect(stored.batteryUpdatedAt, isNotNull);
  });

  test('updateBatteryLevel clamps out-of-range values', () async {
    final device = await setUpOneDevice();

    repo.updateBatteryLevel(device.id, 150);

    expect(repo.devices.single.batteryPercent, 100);
  });

  test('an identical battery reading does not re-persist or advance the stamp',
      () async {
    final device = await setUpOneDevice();
    repo.updateBatteryLevel(device.id, 75);
    final firstStamp = repo.devices.single.batteryUpdatedAt;

    // A repeated identical read must be a no-op (Kotlin persists only if
    // changed): no new stream emission, no advanced timestamp.
    var emissions = 0;
    final sub = repo.devicesStream.listen((_) => emissions++);
    addTearDown(sub.cancel);

    repo.updateBatteryLevel(device.id, 75);

    expect(repo.devices.single.batteryUpdatedAt, firstStamp);
    expect(emissions, 0, reason: 'no-op should not emit on the stream');
  });

  test('updateBatteryLevel ignores an unknown device id', () async {
    await setUpOneDevice();

    repo.updateBatteryLevel('does-not-exist', 50);

    expect(repo.devices.single.batteryPercent, isNull);
  });

  group('watches', () {
    test('kind and lastSyncedAt survive a storage round-trip', () async {
      SharedPreferences.setMockInitialValues(const {});
      final prefs = await SharedPreferences.getInstance();
      repo = BleDeviceRepositoryImpl(prefs);
      final watch = repo.addDevice(
        displayName: 'vívoactive 5',
        address: 'E0:48:24:D5:F7:10',
        bluetoothName: 'vívoactive 5',
        capabilities: const {},
        kind: BleDeviceKind.watch,
      );
      final syncedAt = DateTime.utc(2026, 7, 21, 9, 30);
      repo.markSynced(watch.id, syncedAt);

      // A second repository over the same prefs IS the round-trip: it re-reads
      // the JSON the first one wrote.
      final reloaded = BleDeviceRepositoryImpl(prefs).devices.single;

      expect(reloaded.kind, BleDeviceKind.watch);
      expect(reloaded.isWatch, isTrue);
      expect(reloaded.lastSyncedAt, syncedAt);
    });

    test('a device stored before watches existed reads back as a sensor',
        () async {
      // The exact JSON shape the previous version persisted — no `kind` key.
      SharedPreferences.setMockInitialValues(const {
        'flutter.ble_sensor_devices': '[{"id":"ble-1","displayName":"Strap",'
            '"address":"AA:BB:CC:DD:EE:FF","bluetoothName":"Strap",'
            '"capabilities":["HEART_RATE"],"enabled":true,'
            '"wheelCircumferenceMm":null,"addedAt":1700000000000}]',
      });
      final prefs = await SharedPreferences.getInstance();

      final stored = BleDeviceRepositoryImpl(prefs).devices.single;

      expect(stored.kind, BleDeviceKind.sensor);
      expect(stored.lastSyncedAt, isNull);
      expect(stored.capabilities, {BleSensorCapability.heartRate});
    });

    test('markSynced ignores an unknown device id', () async {
      await setUpOneDevice();

      // A sync can outlive the user forgetting the watch it ran against; that
      // race must not throw the way updateDevice does.
      expect(
        () => repo.markSynced('does-not-exist', DateTime.utc(2026)),
        returnsNormally,
      );
      expect(repo.devices.single.lastSyncedAt, isNull);
    });

    test('an enabled watch is kept out of capability assignment', () async {
      final sensor = await setUpOneDevice();
      repo.addDevice(
        displayName: 'vívoactive 5',
        address: 'E0:48:24:D5:F7:10',
        bluetoothName: 'vívoactive 5',
        // Defensive: even if a watch somehow carried capabilities, it must not
        // be handed to the recording coordinator, which would connect to it and
        // wait for notifications it never sends.
        capabilities: const {BleSensorCapability.heartRate},
        kind: BleDeviceKind.watch,
      );

      final assignments = repo.resolveCapabilityAssignments();

      expect(assignments[BleSensorCapability.heartRate]?.id, sensor.id);
      expect(assignments.values.any((d) => d.isWatch), isFalse);
    });
  });
}
