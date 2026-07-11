import 'package:freezed_annotation/freezed_annotation.dart';

part 'ble_sensor_models.freezed.dart';

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
  }) = _BleSensorDevice;

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
  const factory BleDiscoveredDevice({
    required String address,
    required String? name,
    required int? rssi,
    required Set<BleSensorCapability> suggestedCapabilities,
  }) = _BleDiscoveredDevice;
}
