import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';

void main() {
  test('manual zones round trip through preference string', () {
    const zones = HeartZoneThresholds(
      zone1LowerBpm: 95,
      zone2LowerBpm: 115,
      zone3LowerBpm: 135,
      zone4LowerBpm: 155,
      zone5LowerBpm: 175,
    );

    expect(
      HeartZoneThresholds.fromPreferenceString(zones.toPreferenceString()),
      zones,
    );
  });

  test('invalid manual zones are ignored and manual zone mode is disabled', () {
    final normalized = const BodyEnergyCalibration(
      manualZoneThresholdsBpm: HeartZoneThresholds(
        zone1LowerBpm: 90,
        zone2LowerBpm: 120,
        zone3LowerBpm: 120,
        zone4LowerBpm: 160,
        zone5LowerBpm: 180,
      ),
      useManualZones: true,
    ).normalized();

    expect(normalized.manualZoneThresholdsBpm, isNull);
    expect(normalized.useManualZones, isFalse);
  });

  test('automatic calibration has no manual zones', () {
    const automatic = BodyEnergyCalibration.automatic;

    expect(automatic.useManualZones, isFalse);
    expect(automatic.signature().contains('auto'), isTrue);
  });

  test('automatic calibration defaults to setup not completed', () {
    expect(BodyEnergyCalibration.automatic.setupCompleted, isFalse);
  });

  test('normalization preserves setupCompleted flag', () {
    final normalized =
        const BodyEnergyCalibration(setupCompleted: true).normalized();

    expect(normalized.setupCompleted, isTrue);
  });
}
