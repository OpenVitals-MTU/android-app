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

  /// Hourly cumulative active-calorie progress: [hourlyActiveKcal] is the burn
  /// during each hour from [fromHour], accumulated into the cumulative series
  /// the algorithm expects.
  List<ActivityProgressPoint> activityProgress(
    List<double> hourlyActiveKcal, {
    int fromHour = 0,
  }) {
    final points = <ActivityProgressPoint>[];
    var cumulative = 0.0;
    for (var i = 0; i < hourlyActiveKcal.length; i++) {
      cumulative += hourlyActiveKcal[i];
      points.add(
        ActivityProgressPoint(
          time: dayStart.add(Duration(hours: fromHour + i + 1)),
          totalSteps: 0,
          totalDistanceMeters: null,
          totalCaloriesBurnedKcal: null,
          totalActiveCaloriesKcal: cumulative,
        ),
      );
    }
    return points;
  }

  BodyEnergyTimelineInputs inputs({
    required DateTime now,
    required int previousEndScore,
    required List<HeartRateSample> samples,
    required BodyProfile bodyProfile,
    BodyEnergyCalibration calibration = const BodyEnergyCalibration(),
    List<ExerciseData> workouts = const <ExerciseData>[],
    List<SleepData> sleepSessions = const <SleepData>[],
    List<ActivityProgressPoint> progress = const <ActivityProgressPoint>[],
    double? basalMetabolicRate,
  }) =>
      BodyEnergyTimelineInputs(
        date: date,
        heartRateSamples: samples,
        sleepSessions: sleepSessions,
        workouts: workouts,
        activityProgress: progress,
        basalMetabolicRateKcalPerDay: basalMetabolicRate,
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

  // ── V3 energy-balance behaviour ─────────────────────────────────────────

  const restfulProfile =
      BodyProfile(restingHeartRateBpm: 55, maxHeartRateBpm: 190);

  test('an idle waking day declines rather than staying flat', () {
    final start = dayStart.add(const Duration(hours: 8));
    final end = start.add(const Duration(hours: 8));
    // Calm, resting heart rate all day, no activity: only basal drain.
    final timeline = calculateBodyEnergyTimeline(
      inputs(
        now: end,
        previousEndScore: 80,
        samples: heartRateSamples(start, end, 58),
        bodyProfile: restfulProfile,
      ),
    );

    expect(
      timeline.currentScore < timeline.startScore,
      isTrue,
      reason: 'basal drain should pull an idle day down, never up',
    );
    expect(timeline.drained > 0, isTrue);
    expect(
      timeline.points.any((p) => p.basalDrain > 0.0),
      isTrue,
      reason: 'awake buckets carry a basal cost',
    );
  });

  test('a low-heart-rate high-step day out-drains a sedentary day', () {
    final start = dayStart.add(const Duration(hours: 8));
    final end = start.add(const Duration(hours: 8));
    final samples = heartRateSamples(start, end, 72); // brisk but low zone

    final sedentary = calculateBodyEnergyTimeline(
      inputs(
        now: end,
        previousEndScore: 80,
        samples: heartRateSamples(start, end, 58),
        bodyProfile: restfulProfile,
      ),
    );
    // Eight hours of walking/chores: ~80 active kcal/hour, heart rate stays low.
    final active = calculateBodyEnergyTimeline(
      inputs(
        now: end,
        previousEndScore: 80,
        samples: samples,
        bodyProfile: restfulProfile,
        progress: activityProgress(
          List<double>.filled(8, 80.0),
          fromHour: 8,
        ),
      ),
    );

    expect(
      active.drained > sedentary.drained,
      isTrue,
      reason: 'active calories must register even without elevated heart rate',
    );
    expect(
      active.points.any(
        (p) =>
            p.primaryInfluence == BodyEnergyPrimaryInfluence.everydayActivity,
      ),
      isTrue,
      reason: 'low-heart-rate movement should read as everyday activity',
    );
  });

  test('a run out-drains a walk of the same duration', () {
    final start = dayStart.add(const Duration(hours: 9));
    final end = start.add(const Duration(hours: 1));

    final walk = calculateBodyEnergyTimeline(
      inputs(
        now: end,
        previousEndScore: 80,
        samples: heartRateSamples(start, end, 75),
        bodyProfile: restfulProfile,
        progress: activityProgress(const [120.0], fromHour: 9),
      ),
    );
    final run = calculateBodyEnergyTimeline(
      inputs(
        now: end,
        previousEndScore: 80,
        samples: heartRateSamples(start, end, 165),
        bodyProfile: restfulProfile,
        workouts: [workout(start, end)],
        progress: activityProgress(const [600.0], fromHour: 9),
      ),
    );

    expect(run.drained > walk.drained, isTrue);
  });
}
