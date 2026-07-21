/// View-model for the "Sync with another phone" wizard.
///
/// Orchestrates the Bluetooth service (discoverability / discovery / connect) and
/// the pure-Dart [SyncSession] over the live RFCOMM transport, reading and
/// writing Health Connect through [HealthConnectSyncStore]. One [Notifier] drives
/// the whole state-machine flow the screen renders.
///
/// The transfer currently runs in-process while the screen is foregrounded; the
/// plan's foreground-service variant (surviving backgrounding) is a follow-up.
library;

import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/source/sync/bluetooth_sync_service.dart';
import '../../../data/source/sync/health_connect_sync_store.dart';
import '../../../data/source/sync/sync_pairing.dart';
import '../../../data/source/sync/sync_report.dart';
import '../../../data/source/sync/sync_session.dart';
import '../../../di/data_providers.dart';
import 'device_sync_permissions.dart';

/// How far back the user chose to sync.
enum SyncRange { days30, months6, year1, all }

/// The steps of the pairing + sync wizard.
enum DeviceSyncStep {
  role,
  hostWaiting,
  guestScanning,
  guestCode,
  range,
  types,
  syncing,
  report,
}

/// Every record type the sync can move, mapped to its Health Connect permission
/// suffix (`READ_<suffix>` / `WRITE_<suffix>`). The generic native read/write
/// (`readImportRecords` / `insertImportedRecords`) covers all of these; the
/// device-support gate then hides any a given provider doesn't define.
const Map<String, String> kSyncableTypePermissionSuffix = <String, String>{
  // Activity
  'StepsRecord': 'STEPS',
  'DistanceRecord': 'DISTANCE',
  'ActiveCaloriesBurnedRecord': 'ACTIVE_CALORIES_BURNED',
  'FloorsClimbedRecord': 'FLOORS_CLIMBED',
  'ElevationGainedRecord': 'ELEVATION_GAINED',
  'WheelchairPushesRecord': 'WHEELCHAIR_PUSHES',
  'SpeedRecord': 'SPEED',
  // Heart
  'HeartRateRecord': 'HEART_RATE',
  'RestingHeartRateRecord': 'RESTING_HEART_RATE',
  'HeartRateVariabilityRmssdRecord': 'HEART_RATE_VARIABILITY',
  // Body
  'WeightRecord': 'WEIGHT',
  'HeightRecord': 'HEIGHT',
  'BodyFatRecord': 'BODY_FAT',
  'LeanBodyMassRecord': 'LEAN_BODY_MASS',
  'BasalMetabolicRateRecord': 'BASAL_METABOLIC_RATE',
  'BoneMassRecord': 'BONE_MASS',
  'BodyWaterMassRecord': 'BODY_WATER_MASS',
  // Hydration / Nutrition
  'HydrationRecord': 'HYDRATION',
  'NutritionRecord': 'NUTRITION',
  // Vitals
  'BloodPressureRecord': 'BLOOD_PRESSURE',
  'OxygenSaturationRecord': 'OXYGEN_SATURATION',
  'RespiratoryRateRecord': 'RESPIRATORY_RATE',
  'BodyTemperatureRecord': 'BODY_TEMPERATURE',
  'Vo2MaxRecord': 'VO2_MAX',
  'BloodGlucoseRecord': 'BLOOD_GLUCOSE',
  'BasalBodyTemperatureRecord': 'BASAL_BODY_TEMPERATURE',
  // Sleep / Workouts / Mindfulness
  'SleepSessionRecord': 'SLEEP',
  'ExerciseSessionRecord': 'EXERCISE',
  'MindfulnessSessionRecord': 'MINDFULNESS',
  // Cycle
  'MenstruationFlowRecord': 'MENSTRUATION',
  'OvulationTestRecord': 'OVULATION_TEST',
  'CervicalMucusRecord': 'CERVICAL_MUCUS',
  'IntermenstrualBleedingRecord': 'INTERMENSTRUAL_BLEEDING',
  'SexualActivityRecord': 'SEXUAL_ACTIVITY',
};

/// Every syncable record type (the keys of [kSyncableTypePermissionSuffix]).
final List<String> kSyncableRecordTypes =
    kSyncableTypePermissionSuffix.keys.toList();

class DeviceSyncState {
  const DeviceSyncState({
    this.step = DeviceSyncStep.role,
    this.role,
    this.code = '',
    this.devices = const <DiscoveredSyncDevice>[],
    this.selectedDevice,
    this.codeEntry = '',
    this.codeError = false,
    this.range = SyncRange.year1,
    this.availableTypes = const <String>{},
    this.selectedTypes = const <String>{},
    this.progress,
    this.report,
    this.errorMessage,
    this.discoverableSeconds = 0,
    this.bluetoothUnavailable = false,
  });

  final DeviceSyncStep step;
  final SyncRole? role;
  final String code;
  final List<DiscoveredSyncDevice> devices;
  final DiscoveredSyncDevice? selectedDevice;
  final String codeEntry;
  final bool codeError;
  final SyncRange range;

  /// The syncable types this device's Health Connect provider actually supports
  /// (a subset of [kSyncableRecordTypes]). The picker only offers these, so a
  /// device that lacks e.g. MindfulnessSession never shows it.
  final Set<String> availableTypes;
  final Set<String> selectedTypes;
  final SyncProgress? progress;
  final SyncReport? report;
  final String? errorMessage;
  final int discoverableSeconds;
  final bool bluetoothUnavailable;

  DeviceSyncState copyWith({
    DeviceSyncStep? step,
    SyncRole? role,
    String? code,
    List<DiscoveredSyncDevice>? devices,
    DiscoveredSyncDevice? selectedDevice,
    String? codeEntry,
    bool? codeError,
    SyncRange? range,
    Set<String>? availableTypes,
    Set<String>? selectedTypes,
    SyncProgress? progress,
    SyncReport? report,
    String? errorMessage,
    int? discoverableSeconds,
    bool? bluetoothUnavailable,
  }) =>
      DeviceSyncState(
        step: step ?? this.step,
        role: role ?? this.role,
        code: code ?? this.code,
        devices: devices ?? this.devices,
        selectedDevice: selectedDevice ?? this.selectedDevice,
        codeEntry: codeEntry ?? this.codeEntry,
        codeError: codeError ?? this.codeError,
        range: range ?? this.range,
        availableTypes: availableTypes ?? this.availableTypes,
        selectedTypes: selectedTypes ?? this.selectedTypes,
        progress: progress ?? this.progress,
        report: report ?? this.report,
        errorMessage: errorMessage,
        discoverableSeconds: discoverableSeconds ?? this.discoverableSeconds,
        bluetoothUnavailable: bluetoothUnavailable ?? this.bluetoothUnavailable,
      );
}

class DeviceSyncViewModel extends Notifier<DeviceSyncState> {
  BluetoothSyncService? _service;
  StreamSubscription<DiscoveredSyncDevice>? _deviceSub;
  StreamSubscription<SyncConnectionState>? _connSub;
  StreamSubscription<SyncProgress>? _progressSub;
  final Completer<void> _connected = Completer<void>();

  @override
  DeviceSyncState build() {
    ref.onDispose(_teardown);
    // Optimistic default; narrowed to what this provider supports once a role is
    // chosen (see _ensureHealthPermissions).
    return DeviceSyncState(
      availableTypes: kSyncableRecordTypes.toSet(),
      selectedTypes: kSyncableRecordTypes.toSet(),
    );
  }

  BluetoothSyncService _ensureService() =>
      _service ??= BluetoothSyncService();

  // ── Step 1: role ──────────────────────────────────────────────────────────

  Future<void> chooseHost() async {
    if (!await _ensurePermissions()) return;
    await _ensureHealthPermissions();
    final service = _ensureService();
    if (!await service.isBluetoothEnabled()) {
      state = state.copyWith(bluetoothUnavailable: true);
      return;
    }
    final code = generatePairingCode();
    _listenConnection();
    final granted = await service.requestDiscoverable(120);
    if (granted <= 0) {
      state = state.copyWith(errorMessage: 'discoverable_declined');
      return;
    }
    await service.startServer();
    debugPrint('[devicesync] host: discoverable ${granted}s, server listening, code=$code');
    state = state.copyWith(
      role: SyncRole.host,
      code: code,
      step: DeviceSyncStep.hostWaiting,
      discoverableSeconds: granted,
    );
  }

  Future<void> chooseGuest() async {
    if (!await _ensurePermissions()) return;
    await _ensureHealthPermissions();
    final service = _ensureService();
    if (!await service.isBluetoothEnabled()) {
      state = state.copyWith(bluetoothUnavailable: true);
      return;
    }
    state = state.copyWith(
      role: SyncRole.guest,
      step: DeviceSyncStep.guestScanning,
      devices: const [],
    );
    _deviceSub?.cancel();
    _deviceSub = service.devices.listen((device) {
      // Dedup by address; newest name wins.
      final next = [
        for (final d in state.devices)
          if (d.address != device.address) d,
        device,
      ];
      state = state.copyWith(devices: next);
    });
    await service.startDiscovery();
  }

  // ── Step 2 (guest): select + code ─────────────────────────────────────────

  void selectDevice(DiscoveredSyncDevice device) {
    state = state.copyWith(
      selectedDevice: device,
      step: DeviceSyncStep.guestCode,
      codeEntry: '',
      codeError: false,
    );
  }

  void enterDigit(String digit) {
    if (state.codeEntry.length >= kPairingCodeDigits) return;
    state = state.copyWith(
      codeEntry: state.codeEntry + digit,
      codeError: false,
    );
  }

  void deleteDigit() {
    if (state.codeEntry.isEmpty) return;
    state = state.copyWith(
      codeEntry: state.codeEntry.substring(0, state.codeEntry.length - 1),
    );
  }

  Future<void> submitCode() async {
    final device = state.selectedDevice;
    if (device == null || state.codeEntry.length != kPairingCodeDigits) return;
    final service = _ensureService();
    _listenConnection();
    try {
      await service.cancelDiscovery();
      debugPrint('[devicesync] guest: connecting to ${device.address}');
      await service.connect(device.address);
      debugPrint('[devicesync] guest: connected, advancing to range');
      state = state.copyWith(code: state.codeEntry, step: DeviceSyncStep.range);
    } catch (e) {
      debugPrint('[devicesync] guest: connect failed: $e');
      state = state.copyWith(errorMessage: 'connect_failed');
    }
  }

  // ── Steps 3-4: range + types ──────────────────────────────────────────────

  void setRange(SyncRange range) => state = state.copyWith(range: range);

  void toggleType(String recordType) {
    final next = Set<String>.of(state.selectedTypes);
    if (!next.remove(recordType)) next.add(recordType);
    state = state.copyWith(selectedTypes: next);
  }

  void goToTypes() => state = state.copyWith(step: DeviceSyncStep.types);

  // ── Step 5: sync ──────────────────────────────────────────────────────────

  Future<void> startSync() async {
    final service = _service;
    final role = state.role;
    if (service == null || role == null) return;
    debugPrint('[devicesync] startSync role=$role types=${state.selectedTypes.length} range=${state.range}');
    state = state.copyWith(step: DeviceSyncStep.syncing);

    // Wait until the RFCOMM socket is actually connected before the handshake.
    if (!_connected.isCompleted) {
      try {
        await _connected.future.timeout(const Duration(seconds: 30));
      } on TimeoutException {
        state = state.copyWith(errorMessage: 'connect_timeout');
        return;
      }
    }

    final window = _window(state.range);
    final store = HealthConnectSyncStore(
      dataSource: ref.read(healthDataSourceProvider),
      windowStart: window.$1,
      windowEnd: window.$2,
    );
    final session = SyncSession(
      transport: service.transport,
      store: store,
      config: SyncSessionConfig(
        role: role,
        code: state.code,
        deviceName: 'OpenVitals phone',
        supportedTypes: state.availableTypes.toList(),
        selectedTypes: state.selectedTypes.toList(),
        // Real datasets can be large (a CGM alone is ~100k readings/year). Bigger
        // batches cut the number of stop-and-wait round-trips, and a generous ack
        // timeout tolerates the slow side writing a big batch to Health Connect.
        batchSize: 500,
        batchTimeout: const Duration(minutes: 3),
      ),
    );
    _progressSub = session.progress.listen(
      (p) => state = state.copyWith(progress: p),
    );
    try {
      final report = await session.run();
      debugPrint('[devicesync] session done: completed=${report.completed} '
          'sent=${report.itemsSent} received=${report.itemsReceived} '
          'imported=${report.imported} abort=${report.abortReason}');
      if (!report.completed &&
          (report.abortReason?.contains('code') ?? false)) {
        // Wrong code — back to code entry with an error.
        state = state.copyWith(
          step: DeviceSyncStep.guestCode,
          codeError: true,
          codeEntry: '',
        );
        return;
      }
      state = state.copyWith(step: DeviceSyncStep.report, report: report);
    } catch (e) {
      debugPrint('[devicesync] session threw: $e');
      state = state.copyWith(errorMessage: 'sync_failed', report: null);
    }
  }

  void reset() {
    _teardown();
    _service = null;
    state = DeviceSyncState(
      availableTypes: kSyncableRecordTypes.toSet(),
      selectedTypes: kSyncableRecordTypes.toSet(),
    );
  }

  // ── Internals ─────────────────────────────────────────────────────────────

  void _listenConnection() {
    final service = _ensureService();
    _connSub ??= service.connectionState.listen((s) {
      debugPrint('[devicesync] connectionState=$s role=${state.role} step=${state.step}');
      if (s == SyncConnectionState.connected) {
        if (!_connected.isCompleted) _connected.complete();
        // The host sits on a static "waiting" screen until a peer connects.
        // Advance it into the range/type picker so it runs its own half of the
        // (bidirectional) session — without this the host never starts a session
        // and the guest's handshake finds no peer.
        if (state.role == SyncRole.host &&
            state.step == DeviceSyncStep.hostWaiting) {
          state = state.copyWith(step: DeviceSyncStep.range);
        }
      }
    });
  }

  Future<bool> _ensurePermissions() async {
    final granted = await ensureSyncBluetoothPermissions();
    if (!granted) {
      state = state.copyWith(errorMessage: 'permission_denied');
    }
    return granted;
  }

  /// Requests Health Connect READ + WRITE for the syncable types, so the guest
  /// can actually write received records (an unpermitted write throws and, since
  /// a batch insert is atomic, drops the whole batch). Filtered through the
  /// provider's supported set first — requesting a permission the provider does
  /// not define throws.
  Future<void> _ensureHealthPermissions() async {
    String read(String s) => 'android.permission.health.READ_$s';
    String write(String s) => 'android.permission.health.WRITE_$s';
    final wanted = <String>{
      for (final s in kSyncableTypePermissionSuffix.values) ...[read(s), write(s)],
    };
    final ds = ref.read(healthDataSourceProvider);
    try {
      final supported = await ds.filterSupportedPermissions(wanted);
      // A type is syncable on THIS device only if its provider defines both a
      // read (to send) and a write (to receive) permission — so an old provider
      // that lacks e.g. MindfulnessSession drops out of the picker entirely.
      final available = <String>{
        for (final entry in kSyncableTypePermissionSuffix.entries)
          if (supported.contains(read(entry.value)) &&
              supported.contains(write(entry.value)))
            entry.key,
      };
      final granted = await ds.requestPermissions(supported);
      debugPrint('[devicesync] HC permissions: requested ${supported.length}, '
          'allGranted=$granted, availableTypes=${available.length}');
      state = state.copyWith(
        availableTypes: available,
        selectedTypes: available,
      );
    } catch (e) {
      debugPrint('[devicesync] HC permission request failed: $e');
    }
  }

  static (DateTime, DateTime) _window(SyncRange range) {
    final now = DateTime.now().toUtc();
    final start = switch (range) {
      SyncRange.days30 => now.subtract(const Duration(days: 30)),
      SyncRange.months6 => now.subtract(const Duration(days: 182)),
      SyncRange.year1 => now.subtract(const Duration(days: 365)),
      SyncRange.all => DateTime.utc(2000),
    };
    return (start, now);
  }

  void _teardown() {
    _deviceSub?.cancel();
    _connSub?.cancel();
    _progressSub?.cancel();
    unawaited(_service?.dispose());
  }
}

final deviceSyncProvider =
    NotifierProvider<DeviceSyncViewModel, DeviceSyncState>(
  DeviceSyncViewModel.new,
);
