import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/body_energy_calibration_fit.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/insights/body_energy_watch_observations.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';

BodyEnergyTimelinePoint _point({
  required DateTime time,
  required int score,
  BodyEnergyBucketState state = BodyEnergyBucketState.rest,
  double charge = 0,
  double intensityDrain = 0,
  double activityEnergyDrain = 0,
  double basalDrain = 0,
  double stressDrain = 0,
  double recoveryDebtDrain = 0,
  BodyEnergyPrimaryInfluence primaryInfluence =
      BodyEnergyPrimaryInfluence.steady,
}) =>
    BodyEnergyTimelinePoint.build(
      time: time,
      score: score,
      delta: 0,
      state: state,
      confidence: BodyEnergyConfidence.high,
      charge: charge,
      intensityDrain: intensityDrain,
      activityEnergyDrain: activityEnergyDrain,
      basalDrain: basalDrain,
      stressDrain: stressDrain,
      recoveryDebtDrain: recoveryDebtDrain,
      primaryInfluence: primaryInfluence,
    );

BodyEnergyTimeline _timeline(List<BodyEnergyTimelinePoint> points) =>
    BodyEnergyTimeline(
      date: LocalDate(2026, 7, 22),
      startScore: 80,
      currentScore: points.isEmpty ? 80 : points.last.score,
      charged: 0,
      drained: 0,
      points: points,
      confidence: BodyEnergyConfidence.high,
      confidenceReason: 'test',
    );

void main() {
  final day = DateTime.utc(2026, 7, 22);

  group('buildWatchObservations', () {
    test('downsamples to one reading per bucket', () {
      // The watch emits ~1/minute; feeding all of them in would let one day
      // outvote months of the user's own check-ins.
      final samples = [
        for (var i = 0; i < 180; i++)
          WatchBodyEnergySample(
            time: day.add(Duration(minutes: i)),
            score: 70,
          ),
      ];
      final timeline = _timeline([
        for (var i = 0; i < 180; i += 10)
          _point(time: day.add(Duration(minutes: i)), score: 60),
      ]);

      final readings = buildWatchObservations(
        samples: samples,
        timeline: timeline,
      );

      // Three hours of samples → three observations.
      expect(readings, hasLength(3));
    });

    test('pairs each reading with the model score at that moment', () {
      final samples = [
        WatchBodyEnergySample(time: day.add(const Duration(hours: 1)), score: 65),
      ];
      final timeline = _timeline([
        _point(time: day, score: 90),
        _point(time: day.add(const Duration(hours: 1)), score: 80),
      ]);

      final reading = buildWatchObservations(
        samples: samples,
        timeline: timeline,
      ).single;

      expect(reading.observedScore, 65);
      expect(reading.predictedScore, 80);
    });

    test('drops readings with no nearby point', () {
      // Attributing an error to a gain the model was not exercising then would
      // teach it the wrong lesson.
      final samples = [
        WatchBodyEnergySample(time: day.add(const Duration(hours: 12)), score: 50),
      ];
      final timeline = _timeline([_point(time: day, score: 90)]);

      expect(
        buildWatchObservations(samples: samples, timeline: timeline),
        isEmpty,
      );
    });

    test('skips points the model could not measure', () {
      final samples = [WatchBodyEnergySample(time: day, score: 50)];
      final timeline = _timeline([
        _point(
          time: day,
          score: 50,
          state: BodyEnergyBucketState.unmeasurable,
        ),
      ]);

      expect(
        buildWatchObservations(samples: samples, timeline: timeline),
        isEmpty,
      );
    });

    test('no samples, or no timeline, yields nothing', () {
      expect(
        buildWatchObservations(samples: const [], timeline: _timeline([])),
        isEmpty,
      );
      expect(
        buildWatchObservations(
          samples: [WatchBodyEnergySample(time: day, score: 50)],
          timeline: _timeline([]),
        ),
        isEmpty,
      );
    });
  });

  test('the influence comes from the timeline, not a re-derivation', () {
    // The point already carries the influence the timeline computed, with the
    // zone/workout context that reconstructing it from drain components alone
    // would lose.
    final samples = [WatchBodyEnergySample(time: day, score: 50)];
    final timeline = _timeline([
      _point(
        time: day,
        score: 80,
        stressDrain: 9, // would look like elevatedHeartRate if re-derived
        primaryInfluence: BodyEnergyPrimaryInfluence.everydayActivity,
      ),
    ]);

    final reading =
        buildWatchObservations(samples: samples, timeline: timeline).single;

    expect(reading.dominantInfluence,
        BodyEnergyPrimaryInfluence.everydayActivity);
  });

  group('fitBodyEnergyGains with watch readings', () {
    BodyEnergyWatchReading reading(int observed, int predicted) =>
        BodyEnergyWatchReading(
          time: day,
          observedScore: observed,
          predictedScore: predicted,
          dominantInfluence: BodyEnergyPrimaryInfluence.exertion,
        );

    test('no readings and no checks leaves the gains untouched', () {
      const start = BodyEnergyCalibration(activityDrainGain: 1.3);
      expect(
        fitBodyEnergyGains(start, const []).activityDrainGain,
        1.3,
      );
    });

    test('a watch reading below prediction raises the drain gain', () {
      // Observed lower than predicted → drained harder than modelled.
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        const [],
        watchReadings: [reading(50, 70)],
      );
      expect(fitted.activityDrainGain, greaterThan(1.0));
    });

    test('a watch reading counts far less than a feel-check', () {
      final byWatch = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        const [],
        watchReadings: [reading(50, 70)],
      );
      final byCheck = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        [
          BodyEnergyFeelCheck(
            time: day,
            rating: 5,
            predictedScore: 70,
            dominantInfluence: BodyEnergyPrimaryInfluence.exertion,
          ),
        ],
      );

      // The user's lived experience must outrank a vendor's model.
      expect(
        byWatch.activityDrainGain - 1.0,
        lessThan(byCheck.activityDrainGain - 1.0),
      );
    });

    test('watch readings are counted separately from check-ins', () {
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        [
          BodyEnergyFeelCheck(
            time: day,
            rating: 5,
            predictedScore: 70,
            dominantInfluence: BodyEnergyPrimaryInfluence.exertion,
          ),
        ],
        watchReadings: [reading(50, 70), reading(55, 70)],
      );

      // "Learned from N check-ins" must keep meaning what it says.
      expect(fitted.feelCheckCount, 1);
      expect(fitted.watchObservationCount, 2);
      expect(fitted.hasWatchObservations, isTrue);
    });

    test('a realistic day of disagreement converges without saturating', () {
      // 24 hourly readings each ~10 points off — the everyday case. The gain
      // should move usefully in a day without pinning to its limit.
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        const [],
        watchReadings: [for (var i = 0; i < 24; i++) reading(60, 70)],
      );

      expect(fitted.activityDrainGain, greaterThan(1.1));
      expect(fitted.activityDrainGain,
          lessThan(BodyEnergyCalibration.maxGain));
    });

    test('a day of MAXIMAL disagreement does reach the clamp', () {
      // 24 readings each 100 points wrong. Documented, not accidental: at this
      // learning rate such a day means the model is badly wrong, and a large
      // correction is the right answer. The clamp is what stops it running away.
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        const [],
        watchReadings: [for (var i = 0; i < 24; i++) reading(0, 100)],
      );

      expect(fitted.activityDrainGain, BodyEnergyCalibration.maxGain);
    });

    test('gains stay within their bounds however extreme the disagreement', () {
      final fitted = fitBodyEnergyGains(
        const BodyEnergyCalibration(),
        const [],
        watchReadings: [for (var i = 0; i < 5000; i++) reading(0, 100)],
      );
      expect(fitted.activityDrainGain,
          lessThanOrEqualTo(BodyEnergyCalibration.maxGain));
      expect(fitted.activityDrainGain,
          greaterThanOrEqualTo(BodyEnergyCalibration.minGain));
    });
  });
}
