import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/insights/body_energy_calibration_fit.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';

/// The routing half of the fit: which gain an observation is allowed to move.
///
/// These cases were written against the manual "How's your energy" check-in,
/// which has been removed. They are kept, ported to watch readings, because the
/// behaviour they pin has nothing to do with where the observation came from —
/// an influence must move the gain that scales the component it blames, or the
/// gain drifts to answer for something it does not control. Deleting them with
/// the check-in would have thrown away the only coverage of that mapping.
void main() {
  final now = DateTime.utc(2026, 7, 15, 20);

  /// A watch reading disagreeing with the model, blamed on [influence].
  BodyEnergyWatchReading reading(
    int observed,
    int predicted,
    BodyEnergyPrimaryInfluence influence,
  ) =>
      BodyEnergyWatchReading(
        time: now,
        observedScore: observed,
        predictedScore: predicted,
        dominantInfluence: influence,
      );

  /// Enough identical readings to clear the per-reading step, which is small on
  /// purpose — one watch reading is not meant to swing the model.
  List<BodyEnergyWatchReading> repeated(
    int count,
    int observed,
    int predicted,
    BodyEnergyPrimaryInfluence influence,
  ) =>
      List.generate(count, (_) => reading(observed, predicted, influence));

  test('no readings leaves the gains at their defaults', () {
    final fitted = fitBodyEnergyGains(const BodyEnergyCalibration());
    expect(fitted.activityDrainGain, 1.0);
    expect(fitted.watchObservationCount, 0);
  });

  test('reading lower than predicted after activity raises the activity gain',
      () {
    // Predicted 70, the watch says 30 after a big walk.
    final fitted = fitBodyEnergyGains(
      const BodyEnergyCalibration(),
      watchReadings: repeated(
          10, 30, 70, BodyEnergyPrimaryInfluence.everydayActivity),
    );
    expect(fitted.activityDrainGain, greaterThan(1.0));
    expect(fitted.watchObservationCount, 10);
  });

  test('reading higher than predicted after sleep raises the sleep gain', () {
    final fitted = fitBodyEnergyGains(
      const BodyEnergyCalibration(),
      watchReadings:
          repeated(10, 90, 60, BodyEnergyPrimaryInfluence.sleepRecovery),
    );
    expect(fitted.sleepChargeGain, greaterThan(1.0));
  });

  test('gains never escape the bounded range', () {
    // Many extreme mismatches all pushing the same way.
    final fitted = fitBodyEnergyGains(
      const BodyEnergyCalibration(),
      watchReadings:
          repeated(200, 0, 100, BodyEnergyPrimaryInfluence.exertion),
    );
    expect(fitted.activityDrainGain, lessThanOrEqualTo(BodyEnergyCalibration.maxGain));
    expect(fitted.activityDrainGain,
        greaterThanOrEqualTo(BodyEnergyCalibration.minGain));
  });

  group('an observation moves the gain that scales the drain it blames', () {
    test('recovery debt moves the activity gain, not basal', () {
      // Recovery-debt drain is scaled by activityDrainGain. Routing it to basal
      // — as this used to — aimed at a gain that scales the waking floor and not
      // recovery debt at all, so it could never fix the error while corrupting
      // the basal figure trying.
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        watchReadings:
            repeated(10, 30, 70, BodyEnergyPrimaryInfluence.recoveryDebt),
      );

      expect(fitted.activityDrainGain, greaterThan(1.0));
      expect(fitted.basalDrainGain, 1.0);
    });

    test('steady still moves basal — the one influence it answers for', () {
      // `_primaryInfluence` reports steady exactly when every competing drain is
      // zero, which leaves the basal floor as the only thing that moved.
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        watchReadings:
            repeated(10, 30, 70, BodyEnergyPrimaryInfluence.steady),
      );

      expect(fitted.basalDrainGain, greaterThan(1.0));
      expect(fitted.activityDrainGain, 1.0);
    });

    test('elevated heart rate moves the stress gain alone', () {
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        watchReadings: repeated(
            10, 30, 70, BodyEnergyPrimaryInfluence.elevatedHeartRate),
      );

      expect(fitted.stressDrainGain, greaterThan(1.0));
      expect(fitted.activityDrainGain, 1.0);
      expect(fitted.basalDrainGain, 1.0);
    });

    test('quiet rest moves the sleep gain, which scales the rest charge', () {
      // The v8 waking-rest charge is scaled by sleepChargeGain, so that is the
      // gain a quiet-rest mismatch has to move. Read HIGHER than predicted, so
      // resting recharged more than modelled and the gain goes up.
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        watchReadings:
            repeated(10, 90, 60, BodyEnergyPrimaryInfluence.quietRest),
      );

      expect(fitted.sleepChargeGain, greaterThan(1.0));
      expect(fitted.activityDrainGain, 1.0);
      expect(fitted.basalDrainGain, 1.0);
      expect(fitted.stressDrainGain, 1.0);
      expect(fitted.watchObservationCount, 10);
    });
  });
}
