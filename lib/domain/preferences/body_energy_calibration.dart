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
    // How many watch readings (Garmin Body Battery) have informed the gains, for
    // display ("learned from N watch readings"). The only evidence there is:
    // the manual "How's your energy" check-in was removed, so nothing else
    // contributes.
    @Default(0) int watchObservationCount,
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
      watchObservationCount:
          watchObservationCount < 0 ? 0 : watchObservationCount,
    );
  }

  /// Whether a watch has contributed to the gains, for the calibration copy.
  bool get hasWatchObservations => watchObservationCount > 0;

  /// Whether the gains differ from the neutral defaults.
  bool get hasPersonalGains =>
      _clampedSleepChargeGain != 1.0 ||
      _clampedActivityDrainGain != 1.0 ||
      _clampedBasalDrainGain != 1.0 ||
      _clampedStressDrainGain != 1.0;

  /// The half a user sets: whether zones are manual, and what they are.
  ///
  /// Split from [gainSignature] because the two change on completely different
  /// timescales. This one moves when someone edits a setting; the gains move on
  /// their own every time the watch teaches the model something. A cache keyed
  /// on both together is invalidated by the learner doing its job, so anything
  /// that only needs to know "is this still the same person's configuration"
  /// wants this half alone.
  String zoneSignature() {
    final normalizedCalibration = normalized();
    return [
      normalizedCalibration.useManualZones,
      normalizedCalibration.manualZoneThresholdsBpm?.toPreferenceString() ??
          'auto',
    ].join('|');
  }

  /// The half the watch fit moves, in steps far smaller than three decimals.
  String gainSignature() {
    final normalizedCalibration = normalized();
    return [
      normalizedCalibration._clampedSleepChargeGain.toStringAsFixed(3),
      normalizedCalibration._clampedActivityDrainGain.toStringAsFixed(3),
      normalizedCalibration._clampedBasalDrainGain.toStringAsFixed(3),
      normalizedCalibration._clampedStressDrainGain.toStringAsFixed(3),
    ].join('|');
  }

  /// Both halves, unchanged: a timeline really was computed with these gains, so
  /// serving a cached one still requires all of it to match.
  String signature() => '${zoneSignature()}|${gainSignature()}';

  static const BodyEnergyCalibration automatic = BodyEnergyCalibration();
}

/// The generation of the Body Energy SETUP requirements.
///
/// Bumped when setup starts demanding something it did not before, so installs
/// that completed setup under the old rules are asked once for the missing
/// piece instead of running on a value the model can no longer derive.
///
/// 1 — automatic zones need a birth year. The manual maximum heart rate was
/// removed, leaving Tanaka from age as the only estimate; without it the model
/// falls back to resting + 70, which for a resting 60 claims a maximum of 130
/// and reads ordinary effort as zone 5.
const int bodyEnergySetupEpoch = 1;
