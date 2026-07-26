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

  test('a data gap after the day has shown life keeps draining basal', () {
    // Watch worn 08:00-12:00, then on the charger until 18:00. The wearer's
    // metabolism does not pause with the watch: the gap buckets keep the
    // basal drain, so the line keeps easing down instead of flat-lining
    // (the "put the tracker away for a couple of hours" report).
    final wornStart = dayStart.add(const Duration(hours: 8));
    final wornEnd = wornStart.add(const Duration(hours: 4));
    final now = dayStart.add(const Duration(hours: 18));
    final timeline = calculateBodyEnergyTimeline(
      inputs(
        now: now,
        previousEndScore: 80,
        samples: heartRateSamples(wornStart, wornEnd, 58),
        bodyProfile: restfulProfile,
      ),
    );

    final gapPoints = timeline.points
        .where((p) => p.time.isAfter(wornEnd.add(const Duration(minutes: 5))))
        .toList();
    expect(gapPoints, isNotEmpty);
    expect(
      gapPoints.any((p) => p.basalDrain > 0.0),
      isTrue,
      reason: 'an unmeasured awake bucket after the first signal still pays '
          'the basal cost',
    );
    expect(gapPoints.last.score < gapPoints.first.score, isTrue);
  });

  test('a gap before the first signal of the day stays frozen', () {
    // Nothing recorded until 14:00: the untracked night and morning hold the
    // seed. A device-less stretch must not slide toward zero with nothing to
    // ever charge it back, and an untracked night must not be billed as
    // hours of wakefulness.
    final firstData = dayStart.add(const Duration(hours: 14));
    final now = dayStart.add(const Duration(hours: 16));
    final timeline = calculateBodyEnergyTimeline(
      inputs(
        now: now,
        previousEndScore: 80,
        samples: heartRateSamples(
            firstData, firstData.add(const Duration(hours: 2)), 58),
        bodyProfile: restfulProfile,
      ),
    );

    final beforeData = timeline.points
        .where((p) => p.time.isBefore(firstData))
        .toList();
    expect(beforeData, isNotEmpty);
    expect(beforeData.every((p) => p.basalDrain == 0.0), isTrue);
    expect(beforeData.last.score, timeline.startScore);
  });

  test('steps without active calories still drain through a gap', () {
    // The reported 4k-step walk: phone-recorded steps land in Health Connect
    // with no active-calorie series and no heart rate. The steps stand in for
    // the calories, so the walk drains instead of moving nothing.
    final wornStart = dayStart.add(const Duration(hours: 8));
    final wornEnd = wornStart.add(const Duration(hours: 4));
    final now = dayStart.add(const Duration(hours: 18));
    final walkHourEnd = dayStart.add(const Duration(hours: 15));
    final base = calculateBodyEnergyTimeline(
      inputs(
        now: now,
        previousEndScore: 80,
        samples: heartRateSamples(wornStart, wornEnd, 58),
        bodyProfile: restfulProfile,
      ),
    );
    final withWalk = calculateBodyEnergyTimeline(
      inputs(
        now: now,
        previousEndScore: 80,
        samples: heartRateSamples(wornStart, wornEnd, 58),
        bodyProfile: restfulProfile,
        progress: [
          // Cumulative steps: 0 by 14:00, 4000 by 15:00 — no kcal series.
          ActivityProgressPoint(
            time: dayStart.add(const Duration(hours: 14)),
            totalSteps: 0,
            totalDistanceMeters: null,
            totalCaloriesBurnedKcal: null,
          ),
          ActivityProgressPoint(
            time: walkHourEnd,
            totalSteps: 4000,
            totalDistanceMeters: null,
            totalCaloriesBurnedKcal: null,
          ),
        ],
      ),
    );

    expect(
      withWalk.currentScore < base.currentScore,
      isTrue,
      reason: 'phone-recorded steps must drain even with no kcal and no HR',
    );
    final walkPoints = withWalk.points.where((p) =>
        !p.time.isBefore(dayStart.add(const Duration(hours: 14))) &&
        p.time.isBefore(walkHourEnd));
    expect(walkPoints.any((p) => p.activityEnergyDrain > 0.0), isTrue);
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

  test('a higher activity-drain gain drains more', () {
    final start = dayStart.add(const Duration(hours: 8));
    final end = start.add(const Duration(hours: 8));
    final samples = heartRateSamples(start, end, 72);
    final progress = activityProgress(List<double>.filled(8, 80.0), fromHour: 8);

    // Seeded full, so neither variant reaches the floor — see the assertions.
    final neutral = calculateBodyEnergyTimeline(
      inputs(
        now: end,
        previousEndScore: 100,
        samples: samples,
        bodyProfile: restfulProfile,
        progress: progress,
      ),
    );
    final amplified = calculateBodyEnergyTimeline(
      inputs(
        now: end,
        previousEndScore: 100,
        samples: samples,
        bodyProfile: restfulProfile,
        progress: progress,
        calibration: const BodyEnergyCalibration(activityDrainGain: 1.5),
      ),
    );

    // Both variants must stay off the floor, or this stops measuring the gain:
    // seeded at 80 the amplified day bottoms out at 0, and its drain is capped
    // by the fall rather than by the model, leaving a one-point margin that
    // would vanish the moment the neutral day drained a shade harder.
    expect(neutral.currentScore, greaterThan(0));
    expect(amplified.currentScore, greaterThan(0));
    expect(amplified.drained > neutral.drained, isTrue);
  });

  group('the carry-over seed', () {
    // Body Energy is a chain, so the seed is the day's most consequential
    // input: it is what makes midnight not a reset.
    List<HeartRateSample> quiet() =>
        heartRateSamples(dayStart, dayStart.add(const Duration(hours: 1)), 60);

    BodyEnergyTimeline seeded(int? previousEndScore) =>
        calculateBodyEnergyTimeline(BodyEnergyTimelineInputs(
          date: date,
          heartRateSamples: quiet(),
          previousEndScore: previousEndScore,
          bodyProfile: const BodyProfile(restingHeartRateBpm: 60),
          now: dayStart.add(const Duration(hours: 1)),
        ));

    test('a carried score below the floor is raised, and says so', () {
      final timeline = seeded(0);

      expect(timeline.startScore, bodyEnergyCarryOverFloor);
      expect(timeline.inputSummary.carryOverFloorApplied, isTrue);
      expect(timeline.inputSummary.previousEndScore, 0,
          reason: 'the raw carried score is kept so the UI can show both');
    });

    test('a carried score above the floor passes through untouched', () {
      final timeline = seeded(40);

      expect(timeline.startScore, 40);
      expect(timeline.inputSummary.carryOverFloorApplied, isFalse);
      expect(timeline.inputSummary.seedSource,
          BodyEnergySeedSource.carriedOver);
    });

    test('no previous day starts neutral, and the floor does not apply', () {
      final timeline = seeded(null);

      expect(timeline.startScore, bodyEnergyNeutralStartScore);
      expect(timeline.inputSummary.carryOverFloorApplied, isFalse,
          reason: 'the floor is for carried scores, not for a cold start');
      expect(timeline.inputSummary.seedSource, BodyEnergySeedSource.neutral);
    });

    test('a day with no usable data carries the seed instead of resetting', () {
      // The regression this guards: empty() used to hardcode 50, so a single
      // data-less day silently reset the whole chain.
      final timeline = calculateBodyEnergyTimeline(BodyEnergyTimelineInputs(
        date: date,
        heartRateSamples: const [],
        previousEndScore: 30,
        bodyProfile: const BodyProfile(restingHeartRateBpm: 60),
        now: dayStart.add(const Duration(hours: 1)),
      ));

      expect(timeline.confidence, BodyEnergyConfidence.noData);
      expect(timeline.startScore, 30);
      expect(timeline.currentScore, 30);
    });

    test('a data-less day with a sub-floor seed still floors it', () {
      final timeline = calculateBodyEnergyTimeline(BodyEnergyTimelineInputs(
        date: date,
        heartRateSamples: const [],
        previousEndScore: 2,
        bodyProfile: const BodyProfile(restingHeartRateBpm: 60),
        now: dayStart.add(const Duration(hours: 1)),
      ));

      expect(timeline.startScore, bodyEnergyCarryOverFloor);
      expect(timeline.currentScore, bodyEnergyCarryOverFloor);
    });
  });

  group('the day totals reconcile', () {
    // The contract this group pins: Start + Charged - Drained == the score the
    // day ended on. It used to be false by design -- charged/drained were gross
    // unclamped sums, so a day that drained twice its available energy reported
    // the whole figure and the summary card did not add up.
    void expectReconciles(BodyEnergyTimeline timeline) {
      expect(
        timeline.startScore + timeline.charged - timeline.drained,
        timeline.currentScore,
        reason: 'Start ${timeline.startScore} + ${timeline.charged} '
            '- ${timeline.drained} should equal ${timeline.currentScore}',
      );
    }

    /// What "What moved it" and the influence strip display: the per-bucket
    /// components. They must sum to the same headline the card shows.
    ({double charge, double drain}) componentTotals(
      BodyEnergyTimeline timeline,
    ) {
      var charge = 0.0;
      var drain = 0.0;
      for (final point in timeline.points) {
        charge += point.charge;
        drain += point.basalDrain +
            point.appliedActivityDrain +
            point.stressDrain +
            point.recoveryDebtDrain;
      }
      return (charge: charge, drain: drain);
    }

    test('an ordinary day adds up', () {
      final start = dayStart.add(const Duration(hours: 8));
      final end = start.add(const Duration(hours: 8));

      final timeline = calculateBodyEnergyTimeline(
        inputs(
          now: end,
          previousEndScore: 70,
          samples: heartRateSamples(start, end, 72),
          bodyProfile: restfulProfile,
        ),
      );

      expect(timeline.currentScore, greaterThan(0));
      expect(timeline.drained, greaterThan(0));
      expectReconciles(timeline);
    });

    test('a day that bottoms out reports the fall, not the model', () {
      // The 25 Jul case: seeded low and drained far harder than there was
      // energy for. Previously this reported the whole gross drain (e.g. -200
      // against a start of 50) and the card was unreadable as a ledger.
      final start = dayStart.add(const Duration(hours: 8));
      final end = start.add(const Duration(hours: 8));

      final timeline = calculateBodyEnergyTimeline(
        inputs(
          now: end,
          previousEndScore: 30,
          samples: heartRateSamples(start, end, 72),
          bodyProfile: restfulProfile,
          progress: activityProgress(List<double>.filled(8, 120.0), fromHour: 8),
          calibration: const BodyEnergyCalibration(activityDrainGain: 2.0),
        ),
      );

      expect(timeline.currentScore, 0, reason: 'the fixture must clamp');
      // The cap is what was AVAILABLE, not the starting score: a day can drain
      // everything it began with plus anything it earned back along the way.
      expect(timeline.drained, timeline.startScore + timeline.charged,
          reason: 'a floored day drains exactly what it had');
      expectReconciles(timeline);
    });

    test('a day that tops out reports the rise, not the model', () {
      final start = dayStart;
      final end = start.add(const Duration(hours: 10));

      final timeline = calculateBodyEnergyTimeline(
        inputs(
          now: end,
          previousEndScore: 90,
          samples: heartRateSamples(start, end, 55),
          sleepSessions: [sleep(start, end)],
          bodyProfile: const BodyProfile(restingHeartRateBpm: 58),
        ),
      );

      expect(timeline.currentScore, 100, reason: 'the fixture must clamp');
      expect(timeline.charged, 10,
          reason: 'a day starting at 90 cannot charge more than 10');
      expectReconciles(timeline);
    });

    test('the breakdown sums to the headline on a clamped day', () {
      // The regression this guards: scaling only the two totals would leave
      // "What moved it" listing the model's full drain directly beneath a
      // header showing the clamped one, in the same +N/-N typography.
      final start = dayStart.add(const Duration(hours: 8));
      final end = start.add(const Duration(hours: 8));

      final timeline = calculateBodyEnergyTimeline(
        inputs(
          now: end,
          previousEndScore: 30,
          samples: heartRateSamples(start, end, 72),
          bodyProfile: restfulProfile,
          progress: activityProgress(List<double>.filled(8, 120.0), fromHour: 8),
          calibration: const BodyEnergyCalibration(activityDrainGain: 2.0),
        ),
      );

      final totals = componentTotals(timeline);
      expect(timeline.currentScore, 0);
      expect(totals.drain, closeTo(timeline.drained.toDouble(), 1.0));
      expect(totals.charge, closeTo(timeline.charged.toDouble(), 1.0));
    });

    test('a bucket that both charges and drains still feeds both totals', () {
      // Waking mid-bucket: sleep charge and basal drain in the same bucket.
      // Net-only accounting would post the difference to one side and lose the
      // other, which is why the clamp is attributed proportionally instead.
      final wake = dayStart.add(const Duration(hours: 6, minutes: 2));
      final end = dayStart.add(const Duration(hours: 8));

      final timeline = calculateBodyEnergyTimeline(
        inputs(
          now: end,
          previousEndScore: 50,
          samples: heartRateSamples(dayStart, end, 62),
          sleepSessions: [sleep(dayStart, wake)],
          bodyProfile: const BodyProfile(restingHeartRateBpm: 58),
        ),
      );

      expect(timeline.charged, greaterThan(0));
      expect(timeline.drained, greaterThan(0));
      expectReconciles(timeline);
    });

    test('a fully clamped bucket keeps a truthful driver at zero magnitude',
        () {
      // primaryInfluence stays computed from the raw magnitudes: scaling it to
      // zero alongside them would label a hard workout "steady".
      final start = dayStart.add(const Duration(hours: 8));
      final end = start.add(const Duration(hours: 8));

      final timeline = calculateBodyEnergyTimeline(
        inputs(
          now: end,
          previousEndScore: 30,
          samples: heartRateSamples(start, end, 72),
          bodyProfile: restfulProfile,
          progress: activityProgress(List<double>.filled(8, 120.0), fromHour: 8),
          calibration: const BodyEnergyCalibration(activityDrainGain: 2.0),
        ),
      );

      final flattened = timeline.points
          .where((point) => point.score == 0 && point.delta == 0.0)
          .toList();
      expect(flattened, isNotEmpty,
          reason: 'the fixture must run past the floor');
      expect(
        flattened.every((point) =>
            point.basalDrain == 0.0 && point.appliedActivityDrain == 0.0),
        isTrue,
        reason: 'a bucket that moved nothing must contribute nothing',
      );
      expect(
        flattened.any((point) =>
            point.primaryInfluence != BodyEnergyPrimaryInfluence.steady),
        isTrue,
        reason: 'the driver must survive the scaling',
      );
    });
  });

  group('the recovery-debt drain is correctable', () {
    // A hard workout arms recovery debt for the buckets after it. Until the
    // wiring fix that drain was scaled by no gain at all, so an error the model
    // attributed to it could never be corrected however hard the fit tried.
    BodyEnergyTimeline afterHardWorkout(double activityGain) {
      final start = dayStart.add(const Duration(hours: 8));
      final workoutEnd = start.add(const Duration(minutes: 40));
      final end = start.add(const Duration(hours: 3));
      return calculateBodyEnergyTimeline(
        inputs(
          now: end,
          previousEndScore: 100,
          samples: [
            ...heartRateSamples(start, workoutEnd, 170),
            ...heartRateSamples(workoutEnd, end, 62),
          ],
          workouts: [workout(start, workoutEnd)],
          bodyProfile:
              const BodyProfile(restingHeartRateBpm: 60, maxHeartRateBpm: 190),
          calibration: BodyEnergyCalibration(activityDrainGain: activityGain),
        ),
      );
    }

    double totalRecoveryDebt(BodyEnergyTimeline timeline) =>
        timeline.points.fold(0.0, (sum, p) => sum + p.recoveryDebtDrain);
    double totalBasal(BodyEnergyTimeline timeline) =>
        timeline.points.fold(0.0, (sum, p) => sum + p.basalDrain);

    test('it scales with the activity gain', () {
      final neutral = afterHardWorkout(1.0);
      final amplified = afterHardWorkout(2.0);

      expect(totalRecoveryDebt(neutral), greaterThan(0.0),
          reason: 'the fixture must actually arm recovery debt');
      expect(
        totalRecoveryDebt(amplified),
        closeTo(totalRecoveryDebt(neutral) * 2.0, 0.01),
      );
    });

    test('and does not drag the basal drain with it', () {
      // Basal answers for the waking floor only; the activity gain must not
      // move it, or the two would be inseparable in the fit.
      expect(
        totalBasal(afterHardWorkout(2.0)),
        closeTo(totalBasal(afterHardWorkout(1.0)), 0.01),
      );
    });
  });

  test('no bucket is ever labelled quiet rest', () {
    // `_primaryInfluence` returned it when charge > 0 with no sleep, but charge
    // has been sleep-only since v3 — so the branch was unreachable and is gone.
    final start = dayStart;
    final wake = dayStart.add(const Duration(hours: 7));
    final workoutStart = dayStart.add(const Duration(hours: 9));
    final end = dayStart.add(const Duration(hours: 14));

    final timeline = calculateBodyEnergyTimeline(
      inputs(
        now: end,
        previousEndScore: 80,
        samples: [
          ...heartRateSamples(start, wake, 52),
          // A gap between wake and the workout leaves unmeasurable buckets.
          ...heartRateSamples(workoutStart, end, 150),
        ],
        sleepSessions: [sleep(start, wake)],
        workouts: [workout(workoutStart, end)],
        bodyProfile:
            const BodyProfile(restingHeartRateBpm: 55, maxHeartRateBpm: 190),
      ),
    );

    expect(timeline.points, isNotEmpty);
    expect(
      timeline.points.map((p) => p.primaryInfluence),
      isNot(contains(BodyEnergyPrimaryInfluence.quietRest)),
    );
    // The fixture must exercise the branches that could have reached it.
    expect(
      timeline.points.map((p) => p.primaryInfluence),
      contains(BodyEnergyPrimaryInfluence.sleepRecovery),
    );
  });

  group('the waking-rest charge', () {
    // v3 removed the waking charge because it under-drained active days.
    // Removing it entirely overshot: measured against a real week the model
    // lost ~10 points EVERY day and the chain sat pinned on the floor, because
    // with charge sleep-only a quiet day can only decline at the basal rate.
    BodyEnergyTimeline quietDay({required int wakingBpm, int restingBpm = 58}) {
      final wake = dayStart.add(const Duration(hours: 7));
      final end = dayStart.add(const Duration(hours: 22));
      return calculateBodyEnergyTimeline(
        inputs(
          now: end,
          previousEndScore: 50,
          samples: heartRateSamples(dayStart, end, wakingBpm),
          sleepSessions: [sleep(dayStart, wake)],
          bodyProfile: BodyProfile(restingHeartRateBpm: restingBpm),
        ),
      );
    }

    test('a quiet waking day now recovers instead of only declining', () {
      final timeline = quietDay(wakingBpm: 60);

      expect(timeline.currentScore, greaterThan(timeline.startScore),
          reason: 'a day spent resting should end higher than it began');
      expect(
        timeline.points.any((p) =>
            p.charge > 0.0 &&
            p.state != BodyEnergyBucketState.sleep),
        isTrue,
        reason: 'the charge must come from waking buckets, not only sleep',
      );
    });

    test('the ceiling is a share of reserve, not a fixed offset', () {
      // Measured over a real week, the old resting-plus-8 band earned ZERO rest
      // charge on six days of seven. A reserve fraction widens it and, more
      // importantly, moves with the person — clearing a manual resting or max
      // override must not silently retune the charge model.
      //
      // resting 60, max 190 -> reserve 130. 15% is 79.5 bpm.
      BodyEnergyTimeline at(int bpm) {
        final wake = dayStart.add(const Duration(hours: 7));
        final end = dayStart.add(const Duration(hours: 20));
        return calculateBodyEnergyTimeline(
          inputs(
            now: end,
            previousEndScore: 50,
            samples: heartRateSamples(dayStart, end, bpm),
            sleepSessions: [sleep(dayStart, wake)],
            bodyProfile: const BodyProfile(
              restingHeartRateBpm: 60,
              maxHeartRateBpm: 190,
            ),
          ),
        );
      }

      bool chargesAwake(BodyEnergyTimeline t) => t.points.any((p) =>
          p.charge > 0.0 && p.state != BodyEnergyBucketState.sleep);

      // 75 bpm is 15 above resting — outside the old band, inside this one.
      expect(chargesAwake(at(75)), isTrue);
      // 88 bpm is only 21% of reserve, so zone 1 would have allowed it — but it
      // is 28 beats above resting and in the top stress tier.
      expect(chargesAwake(at(88)), isFalse);
    });

    test('it does not fire once the heart rate leaves the resting band', () {
      // The gate is what makes this safe where the v3 version was not: an
      // active day must gain nothing from it.
      final resting = quietDay(wakingBpm: 60);
      final busy = quietDay(wakingBpm: 95);

      expect(busy.charged, lessThan(resting.charged));
      expect(
        busy.points.any((p) =>
            p.charge > 0.0 && p.state != BodyEnergyBucketState.sleep),
        isFalse,
      );
    });

    test('a trickle of activity drain does not block it', () {
      // The regression this pins: requiring ZERO activity drain in the bucket
      // made the charge almost inert. The activity series is hourly and
      // cumulative and gets interpolated across every 5-minute bucket, so a
      // sliver of drain lands nearly everywhere — on a real week the charge
      // fired for ~100 minutes total and contributed under two points. The
      // wrist, not the smeared calorie curve, decides whether you are resting.
      final wake = dayStart.add(const Duration(hours: 7));
      final end = dayStart.add(const Duration(hours: 22));
      final timeline = calculateBodyEnergyTimeline(
        inputs(
          now: end,
          previousEndScore: 50,
          samples: heartRateSamples(dayStart, end, 60),
          sleepSessions: [sleep(dayStart, wake)],
          // A sedentary day still logs a slow drip of active calories.
          progress: activityProgress(List<double>.filled(15, 4.0), fromHour: 7),
          bodyProfile: const BodyProfile(restingHeartRateBpm: 58),
        ),
      );

      final charging = timeline.points
          .where((p) => p.charge > 0.0 && p.state != BodyEnergyBucketState.sleep)
          .toList();
      expect(charging, isNotEmpty);
      expect(
        charging.any((p) => p.appliedActivityDrain > 0.0),
        isTrue,
        reason: 'the point of the fix: charge and a small drain coexist',
      );
      expect(timeline.currentScore, greaterThan(timeline.startScore));
    });

    test('it is suppressed while recovery debt is still being billed', () {
      // Sitting quietly after a hard session is exactly the state recovery debt
      // models. Charging through it would overstate the recovery and, since the
      // rest rate exceeds the debt rate, hide recovery debt as an influence.
      final start = dayStart.add(const Duration(hours: 8));
      final workoutEnd = start.add(const Duration(minutes: 40));
      final end = start.add(const Duration(hours: 3));

      final timeline = calculateBodyEnergyTimeline(
        inputs(
          now: end,
          previousEndScore: 80,
          samples: heartRateSamples(start, workoutEnd, 170) +
              heartRateSamples(workoutEnd, end, 60),
          workouts: [workout(start, workoutEnd)],
          bodyProfile:
              const BodyProfile(restingHeartRateBpm: 58, maxHeartRateBpm: 190),
        ),
      );

      final debtBuckets =
          timeline.points.where((p) => p.recoveryDebtDrain > 0.0).toList();
      expect(debtBuckets, isNotEmpty,
          reason: 'the fixture must arm recovery debt');
      expect(
        debtBuckets.every((p) => p.charge == 0.0),
        isTrue,
        reason: 'no bucket may both carry recovery debt and charge',
      );
    });

    test('a charging waking bucket reports quiet rest', () {
      final timeline = quietDay(wakingBpm: 60);

      expect(
        timeline.points.map((p) => p.primaryInfluence),
        contains(BodyEnergyPrimaryInfluence.quietRest),
      );
    });

    test('but a larger drain still outranks it', () {
      // quietRest competes rather than short-circuiting: whichever actually
      // moved the score more is the influence reported.
      final start = dayStart.add(const Duration(hours: 8));
      final workoutEnd = start.add(const Duration(minutes: 40));
      final end = start.add(const Duration(hours: 3));

      final timeline = calculateBodyEnergyTimeline(
        inputs(
          now: end,
          previousEndScore: 80,
          samples: heartRateSamples(start, workoutEnd, 170) +
              heartRateSamples(workoutEnd, end, 60),
          workouts: [workout(start, workoutEnd)],
          bodyProfile:
              const BodyProfile(restingHeartRateBpm: 58, maxHeartRateBpm: 190),
        ),
      );

      expect(
        timeline.points.map((p) => p.primaryInfluence),
        contains(BodyEnergyPrimaryInfluence.recoveryDebt),
      );
    });
  });

  test('the age-derived max heart rate uses Tanaka, like the rest of the app',
      () {
    // Body Energy used 220 - age while heart-rate recovery used Tanaka
    // (208 - 0.7*age) off the same birth year, so the app disagreed with itself
    // by a couple of bpm — and this is the one feeding the zone ladder the
    // whole drain model rests on.
    //
    // The bpm values matter. `_resolveIntensityContext` prefers an OBSERVED max
    // once the samples reach max(150, resting + 60), so anything at or above
    // 150 never exercises the age formula at all. Below that, with age 33 and
    // resting 60, zone 3 starts at 60% of heart-rate reserve:
    //   Tanaka   max 185 -> reserve 125 -> zone 3 from 135 bpm
    //   220-age  max 187 -> reserve 127 -> zone 3 from 136.2 bpm
    // So 136 bpm is zone 3 under Tanaka and still zone 2 under the old formula,
    // while 140 is zone 3 under both.
    final start = dayStart.add(const Duration(hours: 8));
    final end = start.add(const Duration(hours: 2));

    int drainAt(int bpm) => calculateBodyEnergyTimeline(
          inputs(
            now: end,
            previousEndScore: 100,
            samples: heartRateSamples(start, end, bpm),
            // Birth year only, so the max has to be derived.
            bodyProfile: BodyProfile(
              birthYear: date.year - 33,
              restingHeartRateBpm: 60,
            ),
          ),
        ).drained;

    expect(drainAt(136), drainAt(140),
        reason: '136 bpm must already be zone 3, as Tanaka puts it');
    expect(drainAt(130), lessThan(drainAt(136)),
        reason: 'and 130 must still be zone 2, or the fixture spans one zone '
            'and the assertion above proves nothing');
  });
}
