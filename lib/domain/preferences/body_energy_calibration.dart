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
  }) = _BodyEnergyCalibration;

  BodyEnergyCalibration normalized() {
    final normalizedZones = manualZoneThresholdsBpm?.normalized();
    return BodyEnergyCalibration(
      manualZoneThresholdsBpm: normalizedZones,
      useManualZones: useManualZones && normalizedZones != null,
      setupCompleted: setupCompleted,
    );
  }

  String signature() {
    final normalizedCalibration = normalized();
    return [
      normalizedCalibration.useManualZones,
      normalizedCalibration.manualZoneThresholdsBpm?.toPreferenceString() ??
          'auto',
    ].join('|');
  }

  static const BodyEnergyCalibration automatic = BodyEnergyCalibration();
}
