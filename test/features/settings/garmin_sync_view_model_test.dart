import 'dart:typed_data';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/repository/impl/ble_device_repository_impl.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_ble_transport.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_directory.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_file_store.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_file_types.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_capabilities.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_session.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_watch_sync_service.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/features/settings/application/garmin_sync_view_model.dart';

/// Stands in for the whole radio + protocol stack.
class _FakeSyncService implements GarminWatchSyncService {
  /// The real service keeps a copy of every download before archiving; this
  /// fake never touches the radio, so there is nothing to keep.
  @override
  GarminFileStore? get fileStore => null;

  /// Set by a test to control what the fake radio comes back with.
  List<GarminDownloadedFile> files = const [];
  Object? error;

  /// What the view-model asked for, so the dedup wiring can be asserted.
  Set<String>? seenAlreadySynced;
  String? seenAddress;
  Duration? seenListenAfter;

  /// What the fake watch declares, so the persistence can be asserted.
  Set<GarminCapability> reportCapabilities = const {};
  int calls = 0;

  /// The locale the settings probe was opened with, so it can be asserted to
  /// follow the phone rather than a hard-coded en_US.
  String? seenSettingsLanguage;

  @override
  Future<int> probeSettings({
    required String address,
    required String phoneName,
    required String manufacturer,
    required String model,
    String language = 'en_US',
    String region = 'us',
  }) async {
    seenSettingsLanguage = language;
    return 3;
  }

  /// Records a find, so the toggle can be asserted without a radio.
  String? seenFindAddress;
  bool findAccepted = true;

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
    onCapabilities?.call(reportCapabilities);
    calls++;
    seenAddress = address;
    seenAlreadySynced = alreadySynced;
    seenListenAfter = listenAfter;
    if (error != null) throw error!;
    onProgress?.call(const GarminSyncProgress(
      phase: GarminSyncPhase.downloading,
      filesTotal: 2,
      filesDone: 1,
    ));
    return files;
  }
}

GarminDownloadedFile _file(int number, {GarminFileType? type}) =>
    GarminDownloadedFile(
      entry: GarminDirectoryEntry(
        fileIndex: number,
        type: type ?? GarminFileType.sleep,
        fileNumber: number,
        specificFlags: 0,
        fileFlags: 0,
        fileSize: 3,
        fileDate: null,
      ),
      // Not a real FIT file: the importer is stubbed out of this container, so
      // these bytes only have to survive the plumbing.
      bytes: Uint8List.fromList([1, 2, 3]),
    );

void main() {
  late BleDeviceRepositoryImpl repo;
  late _FakeSyncService service;
  late ProviderContainer container;
  late BleSensorDevice watch;

  Future<void> setUp0() async {
    SharedPreferences.setMockInitialValues(const {});
    repo = BleDeviceRepositoryImpl(await SharedPreferences.getInstance());
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
      garminWatchSyncServiceProvider.overrideWithValue(service),
    ]);
    addTearDown(container.dispose);
  }

  GarminSyncViewModel notifier() =>
      container.read(garminSyncViewModelProvider.notifier);
  GarminSyncState state() => container.read(garminSyncViewModelProvider);

  test('a sync with nothing new still stamps the device', () async {
    await setUp0();

    final count = await notifier().syncDevice(watch.id);

    expect(count, 0);
    expect(state().lastFileCount, 0);
    expect(repo.devices.single.lastSyncedAt, isNotNull);
    expect(service.seenAddress, 'E0:48:24:D5:F7:10');
  });

  test('passes the previously-synced keys down to the service', () async {
    await setUp0();
    repo.recordSyncedFileKeys(watch.id, ['128/49/1']);

    await notifier().syncDevice(watch.id);

    expect(service.seenAlreadySynced, {'128/49/1'});
  });

  test('reports progress scoped to the syncing device', () async {
    await setUp0();
    final other = repo.addDevice(
      displayName: 'Other watch',
      address: 'AA:BB:CC:DD:EE:FF',
      bluetoothName: 'Other',
      capabilities: const {},
      kind: BleDeviceKind.watch,
    );

    final future = notifier().syncDevice(watch.id);
    // Mid-flight the syncing row is this one, never the other.
    expect(state().isSyncingDevice(watch.id), isTrue);
    expect(state().isSyncingDevice(other.id), isFalse);
    await future;
  });

  test('a transport failure surfaces its message and stamps nothing', () async {
    await setUp0();
    service.error =
        const GarminBleTransportException('Could not connect: timeout');

    final count = await notifier().syncDevice(watch.id);

    expect(count, 0);
    expect(state().errorMessage, contains('Could not connect'));
    expect(state().isSyncing, isFalse);
    expect(repo.devices.single.lastSyncedAt, isNull);
  });

  test('refuses a second sync while one is running', () async {
    await setUp0();

    final first = notifier().syncDevice(watch.id);
    final second = await notifier().syncDevice(watch.id);
    await first;

    // One radio: the second call is dropped rather than queued.
    expect(second, 0);
    expect(service.calls, 1);
  });

  test('ignores a device that is not a watch', () async {
    await setUp0();
    final sensor = repo.addDevice(
      displayName: 'Chest strap',
      address: '11:22:33:44:55:66',
      bluetoothName: 'TICKR',
      capabilities: const {BleSensorCapability.heartRate},
    );

    expect(await notifier().syncDevice(sensor.id), 0);
    expect(service.calls, 0);
  });

  test('ignores an unknown device id', () async {
    await setUp0();
    expect(await notifier().syncDevice('does-not-exist'), 0);
    expect(service.calls, 0);
  });

  test('clear() resets the banner', () async {
    await setUp0();
    await notifier().syncDevice(watch.id);
    expect(state().lastFileCount, isNotNull);

    notifier().clear();

    expect(state().lastFileCount, isNull);
    expect(state().errorMessage, isNull);
  });

  test('an unavailable write path fails cleanly instead of hanging the row',
      () async {
    await setUp0();
    service.files = [_file(1), _file(2, type: GarminFileType.monitor)];

    // This container has no Health Connect platform, so the import path is
    // unreachable — the same shape as permissions being revoked mid-run.
    final count = await notifier().syncDevice(watch.id);

    expect(service.calls, 1);
    // The row must return to idle with an explanation. Before this was caught,
    // the throw escaped syncDevice and left the spinner up forever.
    expect(state().isSyncing, isFalse);
    expect(state().errorMessage, isNotNull);
    expect(count, 0);
    // Nothing reached Health Connect, so nothing is remembered as done: the
    // next run must fetch these files again.
    expect(repo.syncedFileKeys(watch.id), isEmpty);
    expect(repo.devices.single.lastSyncedAt, isNull);
  });

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
}
