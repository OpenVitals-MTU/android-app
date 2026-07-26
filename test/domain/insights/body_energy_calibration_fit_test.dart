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

  group('an observation moves the gain that scales the drain it blames', () {
    test('recovery debt moves the activity gain, not basal', () {
      // Recovery-debt drain is scaled by activityDrainGain. Routing it to basal
      // — as this used to — aimed at a gain that scales the waking floor and not
      // recovery debt at all, so it could never fix the error while corrupting
      // the basal figure trying.
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        [check(3, 70, BodyEnergyPrimaryInfluence.recoveryDebt)],
      );

      expect(fitted.activityDrainGain, greaterThan(1.0));
      expect(fitted.basalDrainGain, 1.0);
    });

    test('steady still moves basal — the one influence it answers for', () {
      // `_primaryInfluence` reports steady exactly when every competing drain is
      // zero, which leaves the basal floor as the only thing that moved.
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        [check(3, 70, BodyEnergyPrimaryInfluence.steady)],
      );

      expect(fitted.basalDrainGain, greaterThan(1.0));
      expect(fitted.activityDrainGain, 1.0);
    });

    test('elevated heart rate moves the stress gain alone', () {
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        [check(3, 70, BodyEnergyPrimaryInfluence.elevatedHeartRate)],
      );

      expect(fitted.stressDrainGain, greaterThan(1.0));
      expect(fitted.activityDrainGain, 1.0);
      expect(fitted.basalDrainGain, 1.0);
    });

    test('quiet rest moves the sleep gain, which scales the rest charge', () {
      // The v8 waking-rest charge is scaled by sleepChargeGain, so that is the
      // gain a quiet-rest mismatch has to move. Felt BETTER than predicted, so
      // resting recharged more than modelled and the gain goes up.
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        [check(9, 60, BodyEnergyPrimaryInfluence.quietRest)],
      );

      expect(fitted.sleepChargeGain, greaterThan(1.0));
      expect(fitted.activityDrainGain, 1.0);
      expect(fitted.basalDrainGain, 1.0);
      expect(fitted.stressDrainGain, 1.0);
      expect(fitted.feelCheckCount, 1);
    });
  });
}
