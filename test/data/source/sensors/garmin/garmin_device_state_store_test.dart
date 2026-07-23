import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/source/sensors/garmin/garmin_capabilities.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_device_state_store.dart';

void main() {
  late GarminDeviceStateStore store;
  late SharedPreferences prefs;
  const deviceId = 'ble-watch-1';

  Future<void> setUpStore() async {
    SharedPreferences.setMockInitialValues(const {});
    prefs = await SharedPreferences.getInstance();
    store = GarminDeviceStateStore(prefs);
  }

  group('synced file keys', () {
    test('starts empty and round-trips through storage', () async {
      await setUpStore();
      expect(store.syncedFileKeys(deviceId), isEmpty);

      store.recordSyncedFileKeys(deviceId, ['128/49/1', '128/32/2']);

      // A second store over the same prefs is the real round-trip — this is
      // what proves the exact key/format survives a restart.
      expect(
        GarminDeviceStateStore(prefs).syncedFileKeys(deviceId),
        {'128/49/1', '128/32/2'},
      );
    });

    test('merges without duplicating across runs', () async {
      await setUpStore();

      store
        ..recordSyncedFileKeys(deviceId, ['128/49/1'])
        ..recordSyncedFileKeys(deviceId, ['128/49/1', '128/32/2']);

      expect(store.syncedFileKeys(deviceId), {'128/49/1', '128/32/2'});
    });

    test('keys are scoped per device', () async {
      await setUpStore();
      store.recordSyncedFileKeys(deviceId, ['128/49/1']);

      expect(store.syncedFileKeys('ble-watch-2'), isEmpty);
    });

    test('an empty write is a no-op', () async {
      await setUpStore();
      store.recordSyncedFileKeys(deviceId, const []);
      expect(store.syncedFileKeys(deviceId), isEmpty);
    });

    test('the set is capped, dropping the oldest keys first', () async {
      await setUpStore();

      // Push past the 4000 cap in two batches so ordering is observable.
      store
        ..recordSyncedFileKeys(
          deviceId,
          [for (var i = 0; i < 3999; i++) 'old/$i'],
        )
        ..recordSyncedFileKeys(deviceId, ['new/a', 'new/b']);

      final keys = store.syncedFileKeys(deviceId);
      expect(keys, hasLength(4000));
      // Newest survive; the very oldest was dropped.
      expect(keys, contains('new/a'));
      expect(keys, contains('new/b'));
      expect(keys, isNot(contains('old/0')));
    });
  });

  group('capabilities', () {
    test('round-trip through storage by wire name', () async {
      await setUpStore();
      expect(store.capabilities(deviceId), isEmpty);

      store.recordCapabilities(
        deviceId,
        {GarminCapability.sync, GarminCapability.findMyWatch},
      );

      // Second store over the same prefs — proves the wireName format persists.
      expect(
        GarminDeviceStateStore(prefs).capabilities(deviceId),
        {GarminCapability.sync, GarminCapability.findMyWatch},
      );
    });

    test('an empty write is a no-op', () async {
      await setUpStore();
      store.recordCapabilities(deviceId, const {});
      expect(store.capabilities(deviceId), isEmpty);
    });
  });

  test('clear drops both capabilities and synced-file history', () async {
    // What forgetting a watch must do: a re-pairing starts clean, re-learning
    // capabilities from a fresh handshake and re-fetching files rather than
    // trusting a record of a device that is no longer here.
    await setUpStore();
    store
      ..recordSyncedFileKeys(deviceId, ['128/49/1'])
      ..recordCapabilities(deviceId, {GarminCapability.sync});

    store.clear(deviceId);

    expect(store.syncedFileKeys(deviceId), isEmpty);
    expect(store.capabilities(deviceId), isEmpty);
    // And it survives a reload — the keys are gone from storage, not just the
    // in-memory view.
    final reloaded = GarminDeviceStateStore(prefs);
    expect(reloaded.syncedFileKeys(deviceId), isEmpty);
    expect(reloaded.capabilities(deviceId), isEmpty);
  });
}
