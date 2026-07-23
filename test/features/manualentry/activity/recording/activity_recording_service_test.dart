import 'dart:async';

import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/presentation/unit_formatter.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/devices/core/ble/ble_sensor_repository.dart';
import 'package:openvitals/domain/model/activity_entry_types.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/domain/preferences/unit_system.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_device_support.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_serialization.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_service.dart';
import 'package:openvitals/features/manualentry/activity/recording/activity_recording_task_handler.dart';

/// The recording SERVICE — the platform orchestrator the screen tests fake.
/// Device I/O (GPS, motion sensors, TTS, the foreground service itself) is
/// best-effort and stays untested here by design; what these pin is the
/// orchestration the user loses workouts to when it breaks: the
/// notification-button relay, pause/resume bookkeeping, and the
/// restore-after-process-death path.
///
/// Timed recordings throughout — they exercise the full lifecycle without a
/// single satellite.
class _FakeBleCoordinator implements BleSensorRepository {
  final StreamController<BleRecordingMetrics> _metrics =
      StreamController<BleRecordingMetrics>.broadcast();

  final List<String> calls = [];

  @override
  Stream<BleRecordingMetrics> get metricsStream => _metrics.stream;

  @override
  BleRecordingMetrics get metrics => const BleRecordingMetrics();

  @override
  void startRecording() => calls.add('startRecording');

  @override
  BleRecordingSampleBuffer stopRecording() {
    calls.add('stopRecording');
    return const BleRecordingSampleBuffer();
  }

  @override
  void refreshConnections() => calls.add('refreshConnections');

  @override
  void disconnectAll() => calls.add('disconnectAll');

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeDeviceSupport implements ActivityRecordingDeviceSupport {
  bool notificationPermission = true;

  @override
  Future<bool> hasNotificationPermission() async => notificationPermission;

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// A type with no GPS route and no repetition unit → a timed recording.
final ActivityEntryType _stationaryBike = defaultActivityEntryTypes
    .singleWhere((type) => type.id == 'stationary_bike');

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late SharedPreferences prefs;
  late _FakeBleCoordinator ble;
  late _FakeDeviceSupport support;

  Future<ActivityRecordingService> service() async {
    final s = ActivityRecordingService(
      preferencesRepository: PreferencesRepository(prefs),
      bleSensorCoordinator: ble,
      recordingStore: ActivityRecordingStore(prefs),
      unitFormatter: UnitFormatter(unitSystemProvider: () => UnitSystem.metric),
      deviceSupport: support,
    );
    addTearDown(s.dispose);
    return s;
  }

  setUp(() async {
    SharedPreferences.setMockInitialValues(const {});
    prefs = await SharedPreferences.getInstance();
    ble = _FakeBleCoordinator();
    support = _FakeDeviceSupport();
  });

  test('notification buttons drive the recording: pause, resume, discard',
      () async {
    final s = await service();
    expect(await s.startRecording(_stationaryBike, null), isTrue);
    expect(s.state.value.status, ActivityRecordingStatus.recording);

    s.onNotificationAction(kActivityRecordingActionPause);
    expect(s.state.value.status, ActivityRecordingStatus.paused);
    expect(s.state.value.pausedStartedAt, isNotNull);

    s.onNotificationAction(kActivityRecordingActionResume);
    expect(s.state.value.status, ActivityRecordingStatus.recording);
    expect(s.state.value.pausedStartedAt, isNull,
        reason: 'resume must close the open pause interval');
    expect(s.state.value.pauseIntervals, hasLength(1));

    s.onNotificationAction(kActivityRecordingActionDiscard);
    expect(s.state.value.isActive, isFalse);
  });

  test('an unknown notification command is ignored', () async {
    final s = await service();
    await s.startRecording(_stationaryBike, null);

    s.onNotificationAction('open_the_pod_bay_doors');

    expect(s.state.value.status, ActivityRecordingStatus.recording);
  });

  test('a recording restored after process death re-enters instead of '
      'going numb', () async {
    // The first service records and the process "dies" — nothing is stopped,
    // exactly like a swipe-away. Only the SharedPreferences survive. The
    // persist is fire-and-forget, so let it land before dying: mid-write
    // tearing is a different (and here untested) concern.
    final first = await service();
    await first.startRecording(_stationaryBike, null);
    await pumpEventQueue();

    // A second service booting on the survived prefs must come up already
    // recording — and re-attach its BLE collection, or the session continues
    // with no sensors and the workout is silently hollow.
    final revived = _FakeBleCoordinator();
    ble = revived;
    final second = await service();

    expect(second.state.value.status, ActivityRecordingStatus.recording);
    expect(second.state.value.activityTypeId, _stationaryBike.id);
    expect(revived.calls, contains('startRecording'),
        reason: 'the restored recording never re-attached its BLE streams');
  });

  test('discard clears the persisted draft, so a restart stays idle',
      () async {
    final s = await service();
    await s.startRecording(_stationaryBike, null);
    s.discardRecording();
    // The persist is fire-and-forget; let it land.
    await Future<void>.delayed(Duration.zero);

    expect(ActivityRecordingStore(prefs).restore().isActive, isFalse);
    expect(ble.calls, contains('stopRecording'));
  });

  test('a denied notification permission refuses to start and says why',
      () async {
    // The recording lives in a foreground service, which cannot post its
    // notification without the permission — starting anyway would record with
    // no ongoing notification and die with the activity.
    support.notificationPermission = false;
    final s = await service();

    expect(await s.startRecording(_stationaryBike, null), isFalse);
    expect(s.state.value.isActive, isFalse);
    expect(s.state.value.errorMessage, isNotNull);
  });

  test('finishRecording snapshots the session and hands back the BLE buffer',
      () async {
    final s = await service();
    await s.startRecording(_stationaryBike, null);

    final snapshot = s.finishRecording();

    expect(snapshot, isNotNull);
    expect(snapshot!.recordingKind, ActivityRecordingKind.timed);
    expect(snapshot.endTime.isAfter(snapshot.startTime), isTrue,
        reason: 'a zero-length session is clamped to at least one second');
    expect(ble.calls, contains('stopRecording'));
  });
}
