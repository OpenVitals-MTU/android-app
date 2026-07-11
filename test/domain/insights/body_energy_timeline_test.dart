import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';
import 'package:openvitals/domain/preferences/body_profile.dart';

void main() {
  final date = LocalDate.now();
  final dayStart = date.atTimeInstant(0);

  List<HeartRateSample> heartRateSamples(
    DateTime start,
    DateTime end,
    int bpm,
  ) {
    final samples = <HeartRateSample>[];
    var time = start;
    while (time.isBefore(end)) {
      samples.add(
        HeartRateSample(time: time, beatsPerMinute: bpm, source: 'test'),
      );
      time = time.add(const Duration(minutes: 5));
    }
    return samples;
  }

  ExerciseData workout(DateTime start, DateTime end) => ExerciseData(
        id: 'workout',
        title: null,
        exerciseType: 0,
        startTime: start,
        endTime: end,
        durationMs: end.difference(start).inMilliseconds,
        source: 'test',
      );

  SleepData sleep(DateTime start, DateTime end) => SleepData(
        id: 'sleep',
        startTime: start,
        endTime: end,
        durationMs: end.difference(start).inMilliseconds,
        source: 'test',
      );

  BodyEnergyTimelineInputs inputs({
    required DateTime now,
    required int previousEndScore,
    required List<HeartRateSample> samples,
    required BodyProfile bodyProfile,
    BodyEnergyCalibration calibration = const BodyEnergyCalibration(),
    List<ExerciseData> workouts = const <ExerciseData>[],
    List<SleepData> sleepSessions = const <SleepData>[],
  }) =>
      BodyEnergyTimelineInputs(
        date: date,
        heartRateSamples: samples,
        sleepSessions: sleepSessions,
        workouts: workouts,
        restingHeartRateBpm: bodyProfile.restingHeartRateBpm,
        observedMaxHeartRateBpm: bodyProfile.maxHeartRateBpm,
        previousEndScore: previousEndScore,
        calibration: calibration,
        bodyProfile: bodyProfile,
        now: now,
      );

  test('manual zones classify sustained exercise as high confidence drain', () {
    final start = dayStart;
    final end = start.add(const Duration(minutes: 90));

    final timeline = calculateBodyEnergyTimeline(
      inputs(
        now: end,
        previousEndScore: 90,
        samples: heartRateSamples(start, end, 165),
        workouts: [workout(start, end)],
        bodyProfile:
            const BodyProfile(restingHeartRateBpm: 60, maxHeartRateBpm: 190),
        calibration: const BodyEnergyCalibration(
          manualZoneThresholdsBpm: HeartZoneThresholds(
            zone1LowerBpm: 95,
            zone2LowerBpm: 115,
            zone3LowerBpm: 135,
            zone4LowerBpm: 155,
            zone5LowerBpm: 175,
          ),
          useManualZones: true,
        ),
      ),
    );

    expect(timeline.startScore, 90);
    expect(timeline.currentScore < 65, isTrue);
    expect(timeline.drained >= 25, isTrue);
    expect(timeline.confidence, BodyEnergyConfidence.high);
    expect(timeline.points.any((p) => p.intensityDrain > 0.0), isTrue);
    expect(
      timeline.points
          .any((p) => p.primaryInfluence == BodyEnergyPrimaryInfluence.exertion),
      isTrue,
    );
  });

  test('long continuous activity adds fatigue beyond simple duration', () {
    final start = dayStart;
    final shortEnd = start.add(const Duration(minutes: 40));
    final longEnd = start.add(const Duration(minutes: 100));
    const bodyProfile =
        BodyProfile(restingHeartRateBpm: 60, maxHeartRateBpm: 190);

    final shortTimeline = calculateBodyEnergyTimeline(
      inputs(
        now: shortEnd,
        previousEndScore: 90,
        samples: heartRateSamples(start, shortEnd, 130),
        workouts: [workout(start, shortEnd)],
        bodyProfile: bodyProfile,
      ),
    );
    final longTimeline = calculateBodyEnergyTimeline(
      inputs(
        now: longEnd,
        previousEndScore: 90,
        samples: heartRateSamples(start, longEnd, 130),
        workouts: [workout(start, longEnd)],
        bodyProfile: bodyProfile,
      ),
    );

    expect(longTimeline.drained > shortTimeline.drained * 2, isTrue);
    expect(longTimeline.currentScore < shortTimeline.currentScore, isTrue);
  });

  test('sleep charges body energy from the previous score', () {
    final start = dayStart;
    final end = start.add(const Duration(hours: 6));

    final timeline = calculateBodyEnergyTimeline(
      inputs(
        now: end,
        previousEndScore: 40,
        samples: heartRateSamples(start, end, 55),
        sleepSessions: [sleep(start, end)],
        bodyProfile:
            const BodyProfile(restingHeartRateBpm: 58, maxHeartRateBpm: 188),
      ),
    );

    expect(timeline.startScore, 40);
    expect(timeline.currentScore > 70, isTrue);
    expect(timeline.charged > 30, isTrue);
    expect(timeline.points.any((p) => p.charge > 0.0), isTrue);
    expect(
      timeline.points.any(
        (p) => p.primaryInfluence == BodyEnergyPrimaryInfluence.sleepRecovery,
      ),
      isTrue,
    );
  });

  test('awake elevated heart rate suppresses charging and adds stress drain',
      () {
    final start = dayStart;
    final end = start.add(const Duration(minutes: 60));

    final timeline = calculateBodyEnergyTimeline(
      inputs(
        now: end,
        previousEndScore: 70,
        samples: heartRateSamples(start, end, 88),
        bodyProfile:
            const BodyProfile(restingHeartRateBpm: 60, maxHeartRateBpm: 190),
      ),
    );

    expect(timeline.charged, 0);
    expect(timeline.drained > 0, isTrue);
    expect(timeline.currentScore < 70, isTrue);
    expect(timeline.points.any((p) => p.stressDrain > 0.0), isTrue);
    expect(
      timeline.points.any(
        (p) =>
            p.primaryInfluence == BodyEnergyPrimaryInfluence.elevatedHeartRate,
      ),
      isTrue,
    );
  });

  test('recovery debt drain is reported after harder effort', () {
    final start = dayStart;
    final workoutEnd = start.add(const Duration(minutes: 30));
    final end = start.add(const Duration(minutes: 90));

    final timeline = calculateBodyEnergyTimeline(
      inputs(
        now: end,
        previousEndScore: 90,
        samples: heartRateSamples(start, workoutEnd, 165) +
            heartRateSamples(workoutEnd, end, 62),
        workouts: [workout(start, workoutEnd)],
        bodyProfile:
            const BodyProfile(restingHeartRateBpm: 60, maxHeartRateBpm: 190),
        calibration: const BodyEnergyCalibration(
          manualZoneThresholdsBpm: HeartZoneThresholds(
            zone1LowerBpm: 95,
            zone2LowerBpm: 115,
            zone3LowerBpm: 135,
            zone4LowerBpm: 155,
            zone5LowerBpm: 175,
          ),
          useManualZones: true,
        ),
      ),
    );

    expect(timeline.drained > 0, isTrue);
    expect(timeline.points.any((p) => p.recoveryDebtDrain > 0.0), isTrue);
    expect(
      timeline.points.any(
        (p) => p.primaryInfluence == BodyEnergyPrimaryInfluence.recoveryDebt,
      ),
      isTrue,
    );
  });
}
