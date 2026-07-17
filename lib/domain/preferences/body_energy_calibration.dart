import 'package:freezed_annotation/freezed_annotation.dart';

part 'body_energy_calibration.freezed.dart';

const String _preferenceSeparator = ',';

@freezed
abstract class HeartZoneThresholds with _$HeartZoneThresholds {
  const HeartZoneThresholds._();

  const factory HeartZoneThresholds({
    required int zone1LowerBpm,
    required int zone2LowerBpm,
    required int zone3LowerBpm,
    required int zone4LowerBpm,
    required int zone5LowerBpm,
  }) = _HeartZoneThresholds;

  List<int> get _values =>
      [zone1LowerBpm, zone2LowerBpm, zone3LowerBpm, zone4LowerBpm, zone5LowerBpm];

  HeartZoneThresholds? normalized() {
    final values = _values;
    if (values.any((v) => v < minZoneBpm || v > maxZoneBpm)) return null;
    for (var i = 0; i < values.length - 1; i++) {
      if (values[i + 1] <= values[i]) return null;
    }
    return this;
  }

  String toPreferenceString() => _values.join(_preferenceSeparator);

  static const int minZoneBpm = 40;
  static const int maxZoneBpm = 240;

  static HeartZoneThresholds? fromPreferenceString(String? value) {
    if (value == null) return null;
    final parts = value
        .split(_preferenceSeparator)
        .map((it) => int.tryParse(it))
        .whereType<int>()
        .toList();
    if (parts.length != 5) return null;
    return HeartZoneThresholds(
      zone1LowerBpm: parts[0],
      zone2LowerBpm: parts[1],
      zone3LowerBpm: parts[2],
      zone4LowerBpm: parts[3],
      zone5LowerBpm: parts[4],
    ).normalized();
  }
}

@freezed
abstract class BodyEnergyCalibration with _$BodyEnergyCalibration {
  const BodyEnergyCalibration._();

  const factory BodyEnergyCalibration({
    HeartZoneThresholds? manualZoneThresholdsBpm,
    @Default(false) bool useManualZones,
    @Default(false) bool setupCompleted,
    // Personal gains: each scales one drain/charge component of the objective
    // model. 1.0 is the neutral default; the feel-check fit nudges them within
    // [minGain, maxGain] so every adjustment stays one legible number.
    @Default(1.0) double sleepChargeGain,
    @Default(1.0) double activityDrainGain,
    @Default(1.0) double basalDrainGain,
    @Default(1.0) double stressDrainGain,
    // How many feel-checks have informed the gains, for display ("learned from
    // N check-ins").
    @Default(0) int feelCheckCount,
  }) = _BodyEnergyCalibration;

  static const double minGain = 0.5;
  static const double maxGain = 2.0;

  double get _clampedSleepChargeGain => sleepChargeGain.clamp(minGain, maxGain);
  double get _clampedActivityDrainGain =>
      activityDrainGain.clamp(minGain, maxGain);
  double get _clampedBasalDrainGain => basalDrainGain.clamp(minGain, maxGain);
  double get _clampedStressDrainGain => stressDrainGain.clamp(minGain, maxGain);

  BodyEnergyCalibration normalized() {
    final normalizedZones = manualZoneThresholdsBpm?.normalized();
    return BodyEnergyCalibration(
      manualZoneThresholdsBpm: normalizedZones,
      useManualZones: useManualZones && normalizedZones != null,
      setupCompleted: setupCompleted,
      sleepChargeGain: _clampedSleepChargeGain,
      activityDrainGain: _clampedActivityDrainGain,
      basalDrainGain: _clampedBasalDrainGain,
      stressDrainGain: _clampedStressDrainGain,
      feelCheckCount: feelCheckCount < 0 ? 0 : feelCheckCount,
    );
  }

  /// Whether the gains differ from the neutral defaults.
  bool get hasPersonalGains =>
      _clampedSleepChargeGain != 1.0 ||
      _clampedActivityDrainGain != 1.0 ||
      _clampedBasalDrainGain != 1.0 ||
      _clampedStressDrainGain != 1.0;

  String signature() {
    final normalizedCalibration = normalized();
    return [
      normalizedCalibration.useManualZones,
      normalizedCalibration.manualZoneThresholdsBpm?.toPreferenceString() ??
          'auto',
      normalizedCalibration._clampedSleepChargeGain.toStringAsFixed(3),
      normalizedCalibration._clampedActivityDrainGain.toStringAsFixed(3),
      normalizedCalibration._clampedBasalDrainGain.toStringAsFixed(3),
      normalizedCalibration._clampedStressDrainGain.toStringAsFixed(3),
    ].join('|');
  }

  static const BodyEnergyCalibration automatic = BodyEnergyCalibration();
}
