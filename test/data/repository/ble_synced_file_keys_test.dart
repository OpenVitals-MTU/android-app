import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/repository/impl/ble_device_repository_impl.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';

void main() {
  late BleDeviceRepositoryImpl repo;
  late SharedPreferences prefs;

  Future<BleSensorDevice> setUpWatch() async {
    SharedPreferences.setMockInitialValues(const {});
    prefs = await SharedPreferences.getInstance();
    repo = BleDeviceRepositoryImpl(prefs);
    return repo.addDevice(
      displayName: 'vívoactive 5',
      address: 'E0:48:24:D5:F7:10',
      bluetoothName: 'vívoactive 5',
      capabilities: const {},
      kind: BleDeviceKind.watch,
    );
  }

  test('starts empty and round-trips through storage', () async {
    final watch = await setUpWatch();
    expect(repo.syncedFileKeys(watch.id), isEmpty);

    repo.recordSyncedFileKeys(watch.id, ['128/49/1', '128/32/2']);

    // A second repository over the same prefs is the real round-trip.
    expect(
      BleDeviceRepositoryImpl(prefs).syncedFileKeys(watch.id),
      {'128/49/1', '128/32/2'},
    );
  });

  test('merges without duplicating across runs', () async {
    final watch = await setUpWatch();

    repo
      ..recordSyncedFileKeys(watch.id, ['128/49/1'])
      ..recordSyncedFileKeys(watch.id, ['128/49/1', '128/32/2']);

    expect(repo.syncedFileKeys(watch.id), {'128/49/1', '128/32/2'});
  });

  test('keys are scoped per device', () async {
    final watch = await setUpWatch();
    final other = repo.addDevice(
      displayName: 'Forerunner',
      address: 'AA:BB:CC:DD:EE:FF',
      bluetoothName: 'Forerunner',
      capabilities: const {},
      kind: BleDeviceKind.watch,
    );

    repo.recordSyncedFileKeys(watch.id, ['128/49/1']);

    expect(repo.syncedFileKeys(other.id), isEmpty);
  });

  test('forgetting a watch clears its history', () async {
    final watch = await setUpWatch();
    repo.recordSyncedFileKeys(watch.id, ['128/49/1']);

    repo.removeDevice(watch.id);

    // A re-added watch must start clean, not silently skip files that a
    // previous pairing had seen.
    expect(repo.syncedFileKeys(watch.id), isEmpty);
  });

  test('an empty write is a no-op', () async {
    final watch = await setUpWatch();
    repo.recordSyncedFileKeys(watch.id, const []);
    expect(repo.syncedFileKeys(watch.id), isEmpty);
  });

  test('the set is capped, dropping the oldest keys first', () async {
    final watch = await setUpWatch();

    // Push past the 4000 cap in two batches so ordering is observable.
    repo
      ..recordSyncedFileKeys(
        watch.id,
        [for (var i = 0; i < 3999; i++) 'old/$i'],
      )
      ..recordSyncedFileKeys(watch.id, ['new/a', 'new/b']);

    final keys = repo.syncedFileKeys(watch.id);
    expect(keys, hasLength(4000));
    // Newest survive; the very oldest was dropped.
    expect(keys, contains('new/a'));
    expect(keys, contains('new/b'));
    expect(keys, isNot(contains('old/0')));
  });
}
