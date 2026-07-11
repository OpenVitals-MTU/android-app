import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/repository/impl/ble_device_repository_impl.dart';
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
}
