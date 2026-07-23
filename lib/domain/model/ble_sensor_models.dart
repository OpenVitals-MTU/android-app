import 'package:freezed_annotation/freezed_annotation.dart';

part 'ble_sensor_models.freezed.dart';

/// What a registered Bluetooth device IS, which decides how the app talks to it.
///
/// A [sensor] streams live values over standard GATT services while a recording
/// runs (heart-rate strap, power meter) and owns [BleSensorCapability]s. A
/// [watch] streams nothing: it holds recorded FIT files that are pulled over
/// GFDI on demand, so it carries no capabilities. A [bikeComputer] (Garmin Edge)
/// does BOTH: it pulls recorded ride FIT files over GFDI like a watch AND can
/// broadcast live standard-GATT sensor values (heart rate, speed/cadence, power)
/// into a recording like a sensor. The two roles are independent — file-sync
/// keys off [BleSensorDevice.isGarminGfdi] (kind + integration), the live role
/// off a non-empty [BleSensorDevice.capabilities] — so a device can hold either
/// or both.
enum BleDeviceKind {
  sensor('SENSOR'),
  watch('WATCH'),
  bikeComputer('BIKE_COMPUTER');

  const BleDeviceKind(this.storageName);

  /// Persisted form, so renaming the Dart identifier can't orphan stored
  /// devices. Same convention as [BleSensorCapability.storageName].
  final String storageName;

  static BleDeviceKind? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

/// Which integration owns a [BleDeviceKind.watch]. A Garmin watch speaks GFDI
/// over BLE (FIT-file sync, settings tree, find). A WearOS watch (Galaxy, Pixel,
/// …) shares none of that protocol: it is a BLE-discoverable live heart-rate
/// source whose recorded data arrives through Health Connect, not a FIT pull.
///
/// Null for a plain sensor, and for a Garmin watch stored before this field
/// existed — [BleSensorDevice.isGarminWatch] treats a null-integration watch as
/// Garmin, the only watch integration that existed then. See
/// docs/reference/wearos-phase3-decision.md.
enum DeviceIntegration {
  garmin('GARMIN'),
  wearos('WEAROS');

  const DeviceIntegration(this.storageName);

  final String storageName;

  static DeviceIntegration? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

enum BleSensorCapability {
  heartRate('HEART_RATE'),
  cyclingCadence('CYCLING_CADENCE'),
  cyclingPower('CYCLING_POWER'),
  cyclingSpeedDistance('CYCLING_SPEED_DISTANCE'),
  runningSpeedCadence('RUNNING_SPEED_CADENCE');

  const BleSensorCapability(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static BleSensorCapability? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

enum BleConnectionStatus {
  disconnected,
  connecting,
  connected,
  reconnecting,
}

@freezed
abstract class BleSensorDevice with _$BleSensorDevice {
  const BleSensorDevice._();

  const factory BleSensorDevice({
    required String id,
    required String displayName,
    required String address,
    required String? bluetoothName,
    required Set<BleSensorCapability> capabilities,
    required bool enabled,
    required int? wheelCircumferenceMm,
    int? batteryPercent,
    DateTime? batteryUpdatedAt,
    required DateTime addedAt,
    /// Defaulted rather than required so every existing call site — and every
    /// device already in storage, written before this field existed — keeps
    /// meaning what it meant.
    @Default(BleDeviceKind.sensor) BleDeviceKind kind,

    /// Which integration owns this device when it is a [BleDeviceKind.watch].
    /// Null for a sensor, and for a Garmin watch stored before this field
    /// existed — [isGarminWatch] treats a null-integration watch as Garmin.
    DeviceIntegration? integration,

    /// When this device's recorded files were last pulled. Null for a watch
    /// that has never synced, and always null for a [BleDeviceKind.sensor].
    DateTime? lastSyncedAt,
  }) = _BleSensorDevice;

  /// Literally a watch — deliberately NOT a bike computer, so an Edge never
  /// renders with watch-only UI (the avatar, the wellness "Data" view).
  bool get isWatch => kind == BleDeviceKind.watch;

  /// A Garmin Edge bike computer: a GFDI file-sync device (like a watch) that is
  /// also a candidate live BLE sensor (unlike a watch).
  bool get isBikeComputer => kind == BleDeviceKind.bikeComputer;

  /// A device the app drives over Garmin's GFDI protocol (FIT sync, settings,
  /// find) — a watch OR a bike computer, but never a WearOS watch. This is the
  /// file-sync eligibility concept; it depends on [kind] + [integration] and is
  /// independent of [capabilities]. A null-integration watch is legacy Garmin —
  /// the sole GFDI integration before WearOS.
  bool get isGarminGfdi =>
      (kind == BleDeviceKind.watch || kind == BleDeviceKind.bikeComputer) &&
      integration != DeviceIntegration.wearos;

  /// A watch the app drives over Garmin's GFDI protocol. Use where the UI
  /// genuinely means "a watch"; for file-sync eligibility use [isGarminGfdi],
  /// which also admits an Edge bike computer.
  bool get isGarminWatch =>
      isWatch && integration != DeviceIntegration.wearos;

  /// A WearOS smartwatch (Galaxy, Pixel, …): a watch with no Garmin protocol —
  /// live heart rate over BLE, recorded data via Health Connect.
  bool get isWearosWatch =>
      isWatch && integration == DeviceIntegration.wearos;

  /// Can hold live [BleSensorCapability]s and take part in a recording: a plain
  /// [sensor], or a [bikeComputer] broadcasting standard GATT. A watch cannot
  /// (scoped out for now). Gates the Sensors-screen listing and capability UI.
  bool get isLiveSensorCapable =>
      kind == BleDeviceKind.sensor || kind == BleDeviceKind.bikeComputer;

  BleSensorDevice normalized() {
    final trimmedDisplayName = displayName.trim();
    final bluetooth = bluetoothName ?? '';
    final bluetoothFallback = bluetooth.trim().isEmpty ? address : bluetooth;
    return copyWith(
      displayName:
          trimmedDisplayName.isEmpty ? bluetoothFallback : trimmedDisplayName,
      wheelCircumferenceMm: wheelCircumferenceMm
          ?.clamp(defaultWheelCircumferenceMm, maxWheelCircumferenceMm)
          .toInt(),
      batteryPercent: batteryPercent?.clamp(0, 100).toInt(),
    );
  }

  static const int defaultWheelCircumferenceMm = 2100;
  static const int maxWheelCircumferenceMm = 3000;
}

@freezed
abstract class BleDeviceConnectionStatus with _$BleDeviceConnectionStatus {
  const factory BleDeviceConnectionStatus({
    required String deviceId,
    required String displayName,
    required String address,
    required BleConnectionStatus status,
    required Set<BleSensorCapability> capabilities,
    int? batteryPercent,
  }) = _BleDeviceConnectionStatus;
}

@freezed
abstract class BleRecordingMetrics with _$BleRecordingMetrics {
  const factory BleRecordingMetrics({
    int? heartRateBpm,
    int? cyclingCadenceRpm,
    double? powerWatts,
    double? cyclingSpeedMetersPerSecond,
    double? runningSpeedMetersPerSecond,
    int? runningCadenceRpm,
    @Default(false) bool heartRateNoSignal,
    @Default(<BleDeviceConnectionStatus>[])
    List<BleDeviceConnectionStatus> deviceStatuses,
  }) = _BleRecordingMetrics;
}

@freezed
abstract class BleHeartRateSample with _$BleHeartRateSample {
  const factory BleHeartRateSample({
    required DateTime time,
    required int beatsPerMinute,
  }) = _BleHeartRateSample;
}

@freezed
abstract class BlePowerSample with _$BlePowerSample {
  const factory BlePowerSample({
    required DateTime time,
    required double watts,
  }) = _BlePowerSample;
}

@freezed
abstract class BleCyclingCadenceSample with _$BleCyclingCadenceSample {
  const factory BleCyclingCadenceSample({
    required DateTime time,
    required int rpm,
  }) = _BleCyclingCadenceSample;
}

@freezed
abstract class BleSpeedSample with _$BleSpeedSample {
  const factory BleSpeedSample({
    required DateTime time,
    required double metersPerSecond,
    required bool isRunning,
  }) = _BleSpeedSample;
}

@freezed
abstract class BleStepsCadenceSample with _$BleStepsCadenceSample {
  const factory BleStepsCadenceSample({
    required DateTime time,
    required int stepsPerMinute,
  }) = _BleStepsCadenceSample;
}

@freezed
abstract class BleRecordingSampleBuffer with _$BleRecordingSampleBuffer {
  const BleRecordingSampleBuffer._();

  const factory BleRecordingSampleBuffer({
    @Default(<BleHeartRateSample>[]) List<BleHeartRateSample> heartRateSamples,
    @Default(<BlePowerSample>[]) List<BlePowerSample> powerSamples,
    @Default(<BleCyclingCadenceSample>[])
    List<BleCyclingCadenceSample> cyclingCadenceSamples,
    @Default(<BleSpeedSample>[]) List<BleSpeedSample> speedSamples,
    @Default(<BleStepsCadenceSample>[])
    List<BleStepsCadenceSample> stepsCadenceSamples,
  }) = _BleRecordingSampleBuffer;

  bool isEmpty() =>
      heartRateSamples.isEmpty &&
      powerSamples.isEmpty &&
      cyclingCadenceSamples.isEmpty &&
      speedSamples.isEmpty &&
      stepsCadenceSamples.isEmpty;

  /// The span the recorded samples actually cover, or null when there are none.
  ///
  /// The session written to Health Connect has to CONTAIN this. Health Connect clamps a
  /// sample that falls outside its session into the session's bounds, so a session that
  /// ends even a second early does not drop the samples past its end — it stacks every
  /// one of them onto the closing instant, which is worse than losing them.
  DateTime? firstSampleTime() => _sampleTimes().fold<DateTime?>(
        null,
        (earliest, time) =>
            earliest == null || time.isBefore(earliest) ? time : earliest,
      );

  DateTime? lastSampleTime() => _sampleTimes().fold<DateTime?>(
        null,
        (latest, time) => latest == null || time.isAfter(latest) ? time : latest,
      );

  Iterable<DateTime> _sampleTimes() => [
        ...heartRateSamples.map((sample) => sample.time),
        ...powerSamples.map((sample) => sample.time),
        ...cyclingCadenceSamples.map((sample) => sample.time),
        ...speedSamples.map((sample) => sample.time),
        ...stepsCadenceSamples.map((sample) => sample.time),
      ];

  int? averageHeartRateBpm() {
    if (heartRateSamples.isEmpty) return null;
    final total = heartRateSamples
        .map((sample) => sample.beatsPerMinute)
        .reduce((a, b) => a + b);
    return (total / heartRateSamples.length).toInt();
  }

  double? averagePowerWatts() {
    if (powerSamples.isEmpty) return null;
    final total =
        powerSamples.map((sample) => sample.watts).reduce((a, b) => a + b);
    return total / powerSamples.length;
  }

  BleRecordingSampleBuffer withHeartRateSample(DateTime time, int bpm) =>
      copyWith(
        heartRateSamples: [
          ...heartRateSamples,
          BleHeartRateSample(time: time, beatsPerMinute: bpm),
        ],
      );

  BleRecordingSampleBuffer withPowerSample(DateTime time, double watts) =>
      copyWith(
        powerSamples: [
          ...powerSamples,
          BlePowerSample(time: time, watts: watts),
        ],
      );

  BleRecordingSampleBuffer withCyclingCadenceSample(DateTime time, int rpm) =>
      copyWith(
        cyclingCadenceSamples: [
          ...cyclingCadenceSamples,
          BleCyclingCadenceSample(time: time, rpm: rpm),
        ],
      );

  BleRecordingSampleBuffer withSpeedSample(
    DateTime time,
    double metersPerSecond,
    bool isRunning,
  ) =>
      copyWith(
        speedSamples: [
          ...speedSamples,
          BleSpeedSample(
            time: time,
            metersPerSecond: metersPerSecond,
            isRunning: isRunning,
          ),
        ],
      );

  BleRecordingSampleBuffer withStepsCadenceSample(
    DateTime time,
    int stepsPerMinute,
  ) =>
      copyWith(
        stepsCadenceSamples: [
          ...stepsCadenceSamples,
          BleStepsCadenceSample(time: time, stepsPerMinute: stepsPerMinute),
        ],
      );

  BleRecordingSampleBuffer trimmed({
    int maxSamplesPerSeries = BleRecordingSampleBuffer.maxSamplesPerSeries,
  }) =>
      copyWith(
        heartRateSamples: _takeLast(heartRateSamples, maxSamplesPerSeries),
        powerSamples: _takeLast(powerSamples, maxSamplesPerSeries),
        cyclingCadenceSamples:
            _takeLast(cyclingCadenceSamples, maxSamplesPerSeries),
        speedSamples: _takeLast(speedSamples, maxSamplesPerSeries),
        stepsCadenceSamples:
            _takeLast(stepsCadenceSamples, maxSamplesPerSeries),
      );

  // ~6 hours at 1 Hz; applied only when finishing a recording as a safety cap.
  static const int maxSamplesPerSeries = 21600;
}

List<T> _takeLast<T>(List<T> items, int count) {
  if (count <= 0) return <T>[];
  if (items.length <= count) return items;
  return items.sublist(items.length - count);
}

@freezed
abstract class BleDiscoveredDevice with _$BleDiscoveredDevice {
  const BleDiscoveredDevice._();

  const factory BleDiscoveredDevice({
    required String address,
    required String? name,
    required int? rssi,
    required Set<BleSensorCapability> suggestedCapabilities,

    /// The advertisement carried a member service that an integration's
    /// `DeviceScanClassifier` recognised — the scanner's signal that this is a
    /// file-sync watch to onboard rather than a live sensor. A single integration
    /// (Garmin) claims these today; the per-integration verdict lives in the
    /// classifier, so this generic model holds the evidence, not the classification.
    ///
    /// Deliberately the ADVERTISED member service, not a GFDI/transport UUID:
    /// those are GATT services, invisible until connected, so no advertisement
    /// ever carries them.
    @Default(false) bool advertisesSyncService,
  }) = _BleDiscoveredDevice;
}
