import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/insights/body_energy_calibration_fit.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';

void main() {
  final now = DateTime.utc(2026, 7, 15, 20);

  BodyEnergyFeelCheck check(
    int rating,
    int predicted,
    BodyEnergyPrimaryInfluence influence,
  ) =>
      BodyEnergyFeelCheck(
        time: now,
        rating: rating,
        predictedScore: predicted,
        dominantInfluence: influence,
      );

  test('no feel-checks leaves the gains at their defaults', () {
    final fitted = fitBodyEnergyGains(
      const BodyEnergyCalibration(),
      const [],
    );
    expect(fitted.activityDrainGain, 1.0);
    expect(fitted.feelCheckCount, 0);
  });

  test('feeling worse than predicted after activity raises the activity gain',
      () {
    // Predicted 70, but the user felt like 30 after a big walk.
    final fitted = fitBodyEnergyGains(
      const BodyEnergyCalibration(),
      [check(3, 70, BodyEnergyPrimaryInfluence.everydayActivity)],
    );
    expect(fitted.activityDrainGain > 1.0, isTrue);
    expect(fitted.feelCheckCount, 1);
  });

  test('feeling better than predicted after sleep raises the sleep gain', () {
    final fitted = fitBodyEnergyGains(
      const BodyEnergyCalibration(),
      [check(9, 60, BodyEnergyPrimaryInfluence.sleepRecovery)],
    );
    expect(fitted.sleepChargeGain > 1.0, isTrue);
  });

  test('gains never escape the bounded range', () {
    // Many extreme mismatches all pushing the same way.
    final checks = List.generate(
      50,
      (_) => check(0, 100, BodyEnergyPrimaryInfluence.exertion),
    );
    final fitted = fitBodyEnergyGains(const BodyEnergyCalibration(), checks);
    expect(fitted.activityDrainGain <= BodyEnergyCalibration.maxGain, isTrue);
    expect(fitted.activityDrainGain >= BodyEnergyCalibration.minGain, isTrue);
  });
}
