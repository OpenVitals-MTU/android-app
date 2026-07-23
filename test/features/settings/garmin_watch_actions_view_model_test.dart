import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/devices/core/registry/ble_device_repository_impl.dart';
import 'package:openvitals/devices/garmin/garmin_file_store.dart';
import 'package:openvitals/devices/garmin/garmin_session.dart';
import 'package:openvitals/devices/garmin/garmin_capabilities.dart';
import 'package:openvitals/devices/garmin/garmin_device_state_store.dart';
import 'package:openvitals/devices/garmin/garmin_watch_sync_service.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/features/manualentry/activity/activity_entry_providers.dart';
import 'package:openvitals/features/settings/application/device_sync_view_model.dart';
import 'package:openvitals/features/settings/application/garmin_watch_actions_view_model.dart';

/// Stands in for the radio + protocol stack. Only the find path matters here;
/// the sync path is present so a running sync can gate a find.
class _FakeSyncService implements GarminWatchSyncService {
  @override
  GarminFileStore? get fileStore => null;

  /// Records a find, so the toggle can be asserted without a radio.
  String? seenFindAddress;
  bool findAccepted = true;

  /// Blocks the sync until released, so a find can be attempted mid-sync.
  final _syncGate = Completer<void>();
  bool syncStarted = false;

  @override
  Future<int> probeSettings({
    required String address,
    required String phoneName,
    required String manufacturer,
    required String model,
    String language = 'en_US',
    String region = 'us',
  }) async =>
      3;

  @override
  Future<bool> findWatch({
    required String address,
    required String phoneName,
    required String manufacturer,
    required String model,
    Duration timeout = const Duration(seconds: 60),
    Future<void>? cancelled,
  }) async {
    seenFindAddress = address;
    // Ends when the caller cancels, as the real one does — a find that returned
    // immediately would never exercise the toggle's stop path.
    if (cancelled != null) await cancelled;
    return findAccepted;
  }

  @override
  Future<List<GarminDownloadedFile>> sync({
    required String address,
    required String phoneName,
    required String manufacturer,
    required String model,
    Set<String> alreadySynced = const {},
    void Function(GarminSyncProgress)? onProgress,
    Duration listenAfter = Duration.zero,
    void Function(Set<GarminCapability>)? onCapabilities,
  }) async {
    syncStarted = true;
    await _syncGate.future;
    return const [];
  }

  void releaseSync() => _syncGate.complete();
}

void main() {
  late BleDeviceRepositoryImpl repo;
  late GarminDeviceStateStore store;
  late _FakeSyncService service;
  late ProviderContainer container;
  late BleSensorDevice watch;

  Future<void> setUp0() async {
    SharedPreferences.setMockInitialValues(const {});
    final prefs = await SharedPreferences.getInstance();
    repo = BleDeviceRepositoryImpl(prefs);
    store = GarminDeviceStateStore(prefs);
    watch = repo.addDevice(
      displayName: 'vívoactive 5',
      address: 'E0:48:24:D5:F7:10',
      bluetoothName: 'vívoactive 5',
      capabilities: const {},
      kind: BleDeviceKind.watch,
    );
    service = _FakeSyncService();
    container = ProviderContainer(overrides: [
      bleDeviceRepositoryProvider.overrideWithValue(repo),
      garminDeviceStateStoreProvider.overrideWithValue(store),
      garminWatchSyncServiceProvider.overrideWithValue(service),
      isRecordingActiveProvider.overrideWithValue(() => false),
    ]);
    addTearDown(container.dispose);
  }

  GarminWatchActionsViewModel notifier() =>
      container.read(garminWatchActionsViewModelProvider.notifier);
  GarminWatchActionsState state() =>
      container.read(garminWatchActionsViewModelProvider);

  test('find is a toggle: a second tap stops it', () async {
    // The watch alerts for a minute unless cancelled, so the control that
    // starts it has to be the one that stops it.
    await setUp0();

    final running = notifier().toggleFind(watch.id);
    await Future<void>.delayed(Duration.zero);
    expect(state().isFindingDevice(watch.id), isTrue);
    expect(service.seenFindAddress, watch.address);

    await notifier().toggleFind(watch.id); // stop
    await running;
    expect(state().findingDeviceId, isNull);
  });

  test('stopping twice before the watch answers does not throw', () async {
    // Stop stays enabled until the watch acknowledges the cancel — a full round
    // trip — so an impatient second tap lands inside that window. Completing an
    // already-completed completer throws, and it threw straight out of the
    // button's callback.
    await setUp0();

    final running = notifier().toggleFind(watch.id);
    await Future<void>.delayed(Duration.zero);
    expect(state().isFindingDevice(watch.id), isTrue);

    // Both taps before the first stop has come back.
    final first = notifier().toggleFind(watch.id);
    final second = notifier().toggleFind(watch.id);
    await Future.wait([first, second, running]);

    expect(state().findingDeviceId, isNull);
  });

  test('a refused find is reported as a flag, not a message', () async {
    // The wording belongs to the screen; this layer has no localizations, and
    // one that invented an English string would leak it into every locale.
    await setUp0();
    service.findAccepted = false;

    final running = notifier().toggleFind(watch.id);
    await Future<void>.delayed(Duration.zero);
    await notifier().toggleFind(watch.id);
    await running;

    expect(state().findFailed, isTrue);
    expect(state().errorMessage, isNull);
  });

  test('a find is refused while a sync holds the radio', () async {
    // One radio: the sync state lives in the generic view-model now, and a find
    // must still stand down while a sync is running.
    await setUp0();
    final syncing =
        container.read(deviceSyncViewModelProvider.notifier).syncDevice(watch.id);
    // The sync sets its state synchronously before the first await, so the
    // radio reads as busy the instant we try to find.
    expect(container.read(deviceSyncViewModelProvider).isSyncing, isTrue);

    await notifier().toggleFind(watch.id);

    expect(state().findingDeviceId, isNull);
    expect(service.seenFindAddress, isNull); // find never reached the radio
    service.releaseSync();
    await syncing;
  });
}
