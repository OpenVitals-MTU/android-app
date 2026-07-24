// ignore_for_file: prefer_initializing_formals
import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';
import '../model/heart_models.dart';
import '../model/activity_models.dart';
import '../model/sleep_models.dart';
import '../model/vitals_models.dart';
import '../preferences/body_energy_calibration.dart';
import '../preferences/body_profile.dart';

part 'body_energy_timeline.freezed.dart';

/// The 5-minute-bucket Body Energy timeline algorithm.
///
/// Deliberate deviation from Kotlin parity (AGENTS.md): the original model was
/// a faithful port that derived drain purely from heart-rate zones, with no
/// basal cost and a daytime rest charge. That under-drained active days (a
/// 20k-step, low-heart-rate day read ~75 when it felt like 10%). V3 reframes it
/// as an energy balance — a basal waking floor plus an activity drain that is
/// the stronger of the heart-rate-zone estimate and an active-calorie estimate,
/// with waking rest no longer charging. See the design proposal for the reason.
///
/// Research: the activity-drain component is heart-rate-zone training load
/// (Banister TRIMP, https://pmc.ncbi.nlm.nih.gov/articles/PMC6561225/;
/// training-load monitoring review, https://pmc.ncbi.nlm.nih.gov/articles/PMC4213373/),
/// taken as the stronger of the zone estimate and an active-calorie estimate; the
/// basal floor is resting metabolism. The energy-balance framing is a documented
/// product design, not a single published model.
const int bodyEnergyTimelineBucketMinutes = 5;
const int bodyEnergyTimelineAlgorithmVersion = 4;

/// Points of Body Energy drained per minute of basal metabolism while awake.
/// ~0.022 accrues roughly 20 points across a 16-hour waking day, so the line
/// always trends gently down when nothing else is happening.
const double _basalPointsPerMinute = 0.022;

/// Reference BMR the basal drain is calibrated around; a higher measured BMR
/// drains proportionally faster (bounded).
const double _referenceBmrKcalPerDay = 1600.0;

/// Points of Body Energy drained per kilocalorie of active energy expenditure.
/// Chosen so a heavy ~700 active-kcal day contributes ~40 points of drain.
const double _activeKcalToPoints = 0.06;

/// Fallback conversion for buckets whose activity progress carries STEPS but no
/// active-calorie figure (a phone pedometer writing bare step counts into
/// Health Connect). ~0.04 kcal per step is the common walking approximation;
/// it only substitutes when the calorie series is silent, never adds to it.
const double _kcalPerStep = 0.04;

enum BodyEnergyConfidence {
  high('HIGH'),
  medium('MEDIUM'),
  low('LOW'),
  noData('NO_DATA');

  const BodyEnergyConfidence(this.storageName);

  final String storageName;

  static BodyEnergyConfidence? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

enum BodyEnergyBucketState {
  sleep('SLEEP'),
  rest('REST'),
  activity('ACTIVITY'),
  stress('STRESS'),
  unmeasurable('UNMEASURABLE');

  const BodyEnergyBucketState(this.storageName);

  final String storageName;

  static BodyEnergyBucketState? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

enum BodyEnergyPrimaryInfluence {
  sleepRecovery('SLEEP_RECOVERY'),
  quietRest('QUIET_REST'),
  everydayActivity('EVERYDAY_ACTIVITY'),
  exertion('EXERTION'),
  elevatedHeartRate('ELEVATED_HEART_RATE'),
  recoveryDebt('RECOVERY_DEBT'),
  noData('NO_DATA'),
  steady('STEADY');

  const BodyEnergyPrimaryInfluence(this.storageName);

  final String storageName;

  static BodyEnergyPrimaryInfluence? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

enum BodyEnergyCalibrationMode {
  automatic('AUTOMATIC'),
  manualValues('MANUAL_VALUES'),
  manualZones('MANUAL_ZONES');

  const BodyEnergyCalibrationMode(this.storageName);

  final String storageName;

  static BodyEnergyCalibrationMode? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

@freezed
abstract class BodyEnergyTimelinePoint with _$BodyEnergyTimelinePoint {
  const factory BodyEnergyTimelinePoint.build({
    required DateTime time,
    required int score,
    required double delta,
    required BodyEnergyBucketState state,
    required BodyEnergyConfidence confidence,
    required double charge,
    // Heart-rate-zone estimate of the activity drain (the backstop signal).
    required double intensityDrain,
    // Active-calorie estimate of the activity drain. The drain actually applied
    // for activity is max(intensityDrain, activityEnergyDrain).
    required double activityEnergyDrain,
    // Basal metabolic drain while awake.
    required double basalDrain,
    required double stressDrain,
    required double recoveryDebtDrain,
    required BodyEnergyPrimaryInfluence primaryInfluence,
  }) = _BodyEnergyTimelinePoint;

  factory BodyEnergyTimelinePoint({
    required DateTime time,
    required int score,
    required double delta,
    required BodyEnergyBucketState state,
    required BodyEnergyConfidence confidence,
    double? charge,
    double intensityDrain = 0.0,
    double activityEnergyDrain = 0.0,
    double basalDrain = 0.0,
    double stressDrain = 0.0,
    double recoveryDebtDrain = 0.0,
    BodyEnergyPrimaryInfluence primaryInfluence =
        BodyEnergyPrimaryInfluence.steady,
  }) =>
      BodyEnergyTimelinePoint.build(
        time: time,
        score: score,
        delta: delta,
        state: state,
        confidence: confidence,
        charge: charge ?? math.max(delta, 0.0),
        intensityDrain: intensityDrain,
        activityEnergyDrain: activityEnergyDrain,
        basalDrain: basalDrain,
        stressDrain: stressDrain,
        recoveryDebtDrain: recoveryDebtDrain,
        primaryInfluence: primaryInfluence,
      );

  const BodyEnergyTimelinePoint._();

  /// The activity drain actually applied: the stronger of the heart-rate-zone
  /// and active-calorie estimates (never their sum).
  double get appliedActivityDrain => math.max(intensityDrain, activityEnergyDrain);
}

@freezed
abstract class BodyEnergyInputSummary with _$BodyEnergyInputSummary {
  const factory BodyEnergyInputSummary({
    @Default(bodyEnergyTimelineAlgorithmVersion) int algorithmVersion,
    @Default(bodyEnergyTimelineBucketMinutes) int bucketMinutes,
    @Default(0) int heartRateSampleCount,
    @Default(0) int hrvSampleCount,
    @Default(0) int sleepSessionCount,
    @Default(0) int workoutCount,
    @Default(0) int respiratorySampleCount,
    @Default(false) bool hasRestingHeartRate,
    @Default(false) bool hasBaselineRestingHeartRate,
    @Default(false) bool hasObservedMaxHeartRate,
    @Default(false) bool hasHrvBaseline,
    @Default(false) bool hasRespiratoryBaseline,
    int? previousEndScore,
    @Default(BodyEnergyCalibrationMode.automatic)
    BodyEnergyCalibrationMode calibrationMode,
  }) = _BodyEnergyInputSummary;
}

@freezed
abstract class BodyEnergyTimeline with _$BodyEnergyTimeline {
  const factory BodyEnergyTimeline({
    required LocalDate date,
    required int startScore,
    required int currentScore,
    required int charged,
    required int drained,
    required List<BodyEnergyTimelinePoint> points,
    required BodyEnergyConfidence confidence,
    required String confidenceReason,
    @Default(BodyEnergyInputSummary()) BodyEnergyInputSummary inputSummary,
    DateTime? generatedAt,
    @Default('') String signature,
  }) = _BodyEnergyTimeline;

  static BodyEnergyTimeline empty({
    required LocalDate date,
    required String reason,
    BodyEnergyInputSummary inputSummary = const BodyEnergyInputSummary(),
  }) =>
      BodyEnergyTimeline(
        date: date,
        startScore: 50,
        currentScore: 50,
        charged: 0,
        drained: 0,
        points: const [],
        confidence: BodyEnergyConfidence.noData,
        confidenceReason: reason,
        inputSummary: inputSummary,
      );
}

/// Inputs for [calculateBodyEnergyTimeline]. The Kotlin `zone` parameter is
/// dropped in favour of device-local conversions (consistent with the rest of
/// the port).
class BodyEnergyTimelineInputs {
  BodyEnergyTimelineInputs({
    required this.date,
    required this.heartRateSamples,
    this.hrvSamples = const <HrvSample>[],
    this.sleepSessions = const <SleepData>[],
    this.workouts = const <ExerciseData>[],
    this.respiratoryRateSamples = const <RespiratoryRateEntry>[],
    this.activityProgress = const <ActivityProgressPoint>[],
    this.basalMetabolicRateKcalPerDay,
    this.restingHeartRateBpm,
    this.baselineRestingHeartRateBpm,
    this.observedMaxHeartRateBpm,
    this.hrvBaselineRmssdMs,
    this.respiratoryRateBaseline,
    this.previousEndScore,
    this.calibration = BodyEnergyCalibration.automatic,
    this.bodyProfile = const BodyProfile(),
    DateTime? now,
  }) : now = now ?? DateTime.now().toUtc();

  final LocalDate date;
  final List<HeartRateSample> heartRateSamples;
  final List<HrvSample> hrvSamples;
  final List<SleepData> sleepSessions;
  final List<ExerciseData> workouts;
  final List<RespiratoryRateEntry> respiratoryRateSamples;

  /// Hourly, cumulative activity progress (steps + active calories). Used to
  /// estimate the active-calorie drain the heart-rate-zone signal misses.
  final List<ActivityProgressPoint> activityProgress;

  /// Latest basal metabolic rate in kcal/day, if the device reports it.
  final double? basalMetabolicRateKcalPerDay;
  final int? restingHeartRateBpm;
  final int? baselineRestingHeartRateBpm;
  final int? observedMaxHeartRateBpm;
  final double? hrvBaselineRmssdMs;
  final double? respiratoryRateBaseline;
  final int? previousEndScore;
  final BodyEnergyCalibration calibration;
  final BodyProfile bodyProfile;
  final DateTime now;
}

BodyEnergyTimeline calculateBodyEnergyTimeline(
  BodyEnergyTimelineInputs inputs,
) {
  final dayStart = inputs.date.atTimeInstant(0);
  final dayEnd = inputs.date.plusDays(1).atTimeInstant(0);
  final usableEnd = _minInstant(
    dayEnd,
    inputs.date == LocalDate.now() ? inputs.now : dayEnd,
  );
  final inputSummary = _inputSummary(
    inputs,
    heartRateSampleCount: inputs.heartRateSamples
        .where(
          (sample) =>
              !sample.time.isBefore(dayStart) && sample.time.isBefore(dayEnd),
        )
        .length,
  );
  final totalMinutes = math.max(0, usableEnd.difference(dayStart).inMinutes);
  final bucketCount =
      (totalMinutes + bodyEnergyTimelineBucketMinutes - 1) ~/
          bodyEnergyTimelineBucketMinutes;
  if (bucketCount <= 0) {
    return BodyEnergyTimeline.empty(
      date: inputs.date,
      reason: 'No timeline window is available.',
      inputSummary: inputSummary,
    );
  }

  final sortedHeartRate = inputs.heartRateSamples
      .where(
        (sample) =>
            !sample.time.isBefore(dayStart) && sample.time.isBefore(dayEnd),
      )
      .toList()
    ..sort((a, b) => a.time.compareTo(b.time));
  final heartRateAverages = _bucketedAverages<HeartRateSample>(
    sortedHeartRate,
    bucketCount: bucketCount,
    dayStart: dayStart,
    time: (sample) => sample.time,
    value: (sample) => sample.beatsPerMinute.toDouble(),
  );
  final hrvAverages = _bucketedAverages<HrvSample>(
    inputs.hrvSamples,
    bucketCount: bucketCount,
    dayStart: dayStart,
    time: (sample) => sample.time,
    value: (sample) => sample.rmssdMs,
  );
  final respiratoryAverages = _bucketedAverages<RespiratoryRateEntry>(
    inputs.respiratoryRateSamples,
    bucketCount: bucketCount,
    dayStart: dayStart,
    time: (sample) => sample.time,
    value: (sample) => sample.breathsPerMinute,
  );
  final intensityContext = _resolveIntensityContext(inputs, sortedHeartRate);
  final hasSleep = inputs.sleepSessions.any(
    (session) =>
        session.endTime.isAfter(dayStart) && session.startTime.isBefore(dayEnd),
  );
  if (sortedHeartRate.isEmpty && !hasSleep) {
    return BodyEnergyTimeline.empty(
      date: inputs.date,
      reason: 'Heart rate or sleep data is needed for Body Energy.',
      inputSummary: inputSummary,
    );
  }

  final activeKcalPerBucket = _activeCaloriesPerBucket(
    inputs.activityProgress,
    bucketCount: bucketCount,
    dayStart: dayStart,
  );
  final stepsPerBucket = _stepsPerBucket(
    inputs.activityProgress,
    bucketCount: bucketCount,
    dayStart: dayStart,
  );
  final basalScale = inputs.basalMetabolicRateKcalPerDay != null
      ? (inputs.basalMetabolicRateKcalPerDay! / _referenceBmrKcalPerDay)
          .clamp(0.5, 2.0)
      : 1.0;

  // Personal gains (clamped by normalized()); 1.0 leaves the objective model
  // untouched.
  final gains = inputs.calibration.normalized();

  var score = (inputs.previousEndScore ?? 50).clamp(0, 100).toDouble();
  final startScore = score.round();
  var charged = 0.0;
  var drained = 0.0;
  var continuousActivityMinutes = 0.0;
  var recoveryDebtBuckets = 0;
  var daySignalSeen = false;
  var highConfidenceBuckets = 0;
  var mediumConfidenceBuckets = 0;
  var lowConfidenceBuckets = 0;

  final points = <BodyEnergyTimelinePoint>[];
  for (var index = 0; index < bucketCount; index++) {
    final bucketStart = dayStart.add(
      Duration(minutes: index * bodyEnergyTimelineBucketMinutes),
    );
    final bucketEnd = _minInstant(
      bucketStart.add(
        const Duration(minutes: bodyEnergyTimelineBucketMinutes),
      ),
      usableEnd,
    );
    final bucketMinutes =
        bucketEnd.difference(bucketStart).inSeconds.toDouble() / 60.0;
    if (bucketMinutes <= 0.0) continue;

    final avgHeartRate = heartRateAverages[index];
    final sleepMinutes = inputs.sleepSessions.fold<double>(
      0.0,
      (sum, session) =>
          sum +
          _overlapMinutes(
            session.startTime,
            session.endTime,
            bucketStart,
            bucketEnd,
          ),
    );
    final workoutMinutes = inputs.workouts.fold<double>(
      0.0,
      (sum, workout) =>
          sum +
          _overlapMinutes(
            workout.startTime,
            workout.endTime,
            bucketStart,
            bucketEnd,
          ),
    );
    final hrvFactor = _hrvRecoveryFactor(
      inputs.hrvBaselineRmssdMs,
      hrvAverages[index],
    );
    final respirationFactor = _respiratoryStressFactor(
      inputs.respiratoryRateBaseline,
      respiratoryAverages[index],
    );
    final zone = avgHeartRate != null ? intensityContext.zoneFor(avgHeartRate) : 0;
    final activeByHeartRate = zone >= 2;
    final active = workoutMinutes > 0.0 || activeByHeartRate;

    continuousActivityMinutes =
        active ? continuousActivityMinutes + bucketMinutes : 0.0;
    final double fatigueMultiplier;
    if (continuousActivityMinutes >= 90.0) {
      fatigueMultiplier = 1.5;
    } else if (continuousActivityMinutes >= 45.0) {
      fatigueMultiplier = 1.2;
    } else {
      fatigueMultiplier = 1.0;
    }
    final exerciseMultiplier = workoutMinutes > 0.0 ? 1.15 : 1.0;
    final notSleeping = sleepMinutes <= 0.0;
    final recordedKcal = index < activeKcalPerBucket.length
        ? activeKcalPerBucket[index]
        : 0.0;
    final bucketSteps =
        index < stepsPerBucket.length ? stepsPerBucket[index] : 0.0;
    // Steps stand in for active calories only when the calorie series is
    // silent for the bucket — a phone pedometer writing bare step counts (the
    // 4k-step walk that used to move nothing because the watch was off).
    final activeKcal =
        recordedKcal > 0.0 ? recordedKcal : bucketSteps * _kcalPerStep;
    // A day that has shown life keeps burning through its gaps: once any
    // signal (heart rate, sleep, activity) has been seen today, an unmeasured
    // awake bucket still pays the basal drain — a watch on the charger does
    // not pause the wearer's metabolism. BEFORE the first signal the line
    // stays frozen, which keeps two cases honest: a device-less day holds its
    // seed instead of sliding to zero with nothing to ever charge it back, and
    // an untracked night is not billed as hours of wakefulness.
    daySignalSeen = daySignalSeen ||
        avgHeartRate != null ||
        sleepMinutes > 0.0 ||
        activeKcal > 0.0;
    // Awake-and-present: heart rate is being sampled, active energy was
    // spent, or the day's data has started and this is a mid-day gap.
    final awakePresent = notSleeping &&
        (avgHeartRate != null || activeKcal > 0.0 || daySignalSeen);

    // Elevated heart rate while awake and not working out. Strengthened from the
    // original so ordinary sympathetic stress registers.
    final double rawStressDrain;
    if (avgHeartRate == null) {
      rawStressDrain = 0.0;
    } else {
      final resting = intensityContext.restingHeartRateBpm;
      if (resting == null || workoutMinutes > 0.0 || sleepMinutes > 0.0) {
        rawStressDrain = 0.0;
      } else if (avgHeartRate >= resting + 25) {
        rawStressDrain = 0.07 * bucketMinutes;
      } else if (avgHeartRate >= resting + 15) {
        rawStressDrain = 0.04 * bucketMinutes;
      } else if (avgHeartRate >= resting + 8) {
        rawStressDrain = 0.02 * bucketMinutes;
      } else {
        rawStressDrain = 0.0;
      }
    }
    // Heart-rate-zone estimate of the activity drain (the backstop signal).
    final double rawIntensityDrain;
    if (avgHeartRate != null) {
      rawIntensityDrain = _drainRateForZone(zone) *
          bucketMinutes *
          exerciseMultiplier *
          fatigueMultiplier;
    } else if (workoutMinutes >= 2.0) {
      rawIntensityDrain = 0.05 * workoutMinutes;
    } else {
      rawIntensityDrain = 0.0;
    }
    // Active-calorie estimate — captures walking, chores and other movement that
    // never lifts heart rate out of the low zones.
    final rawActivityEnergyDrain =
        notSleeping ? activeKcal * _activeKcalToPoints : 0.0;
    final rawRecoveryDebtDrain =
        recoveryDebtBuckets > 0 ? 0.015 * bucketMinutes : 0.0;
    // Baseline metabolic cost of being awake — the floor that keeps the line
    // trending down when nothing else is happening.
    final rawBasalDrain =
        awakePresent ? _basalPointsPerMinute * bucketMinutes * basalScale : 0.0;
    final drainMultiplier = math.max(
      hrvFactor.drainMultiplier,
      respirationFactor.drainMultiplier,
    );
    final intensityDrain =
        rawIntensityDrain * drainMultiplier * gains.activityDrainGain;
    final activityEnergyDrain =
        rawActivityEnergyDrain * drainMultiplier * gains.activityDrainGain;
    final stressDrain =
        rawStressDrain * drainMultiplier * gains.stressDrainGain;
    final recoveryDebtDrain = rawRecoveryDebtDrain * drainMultiplier;
    // Basal is a metabolic constant, not a stress response — no HRV/respiration
    // modifier, just the personal gain.
    final basalDrain = rawBasalDrain * gains.basalDrainGain;
    // Activity is the stronger of the two estimates, never their sum.
    final appliedActivityDrain = math.max(intensityDrain, activityEnergyDrain);
    final drain =
        basalDrain + appliedActivityDrain + stressDrain + recoveryDebtDrain;

    if (zone >= 3 && workoutMinutes > 0.0) {
      recoveryDebtBuckets =
          math.max(recoveryDebtBuckets, math.min(zone * 6, 36));
    } else if (recoveryDebtBuckets > 0) {
      recoveryDebtBuckets -= 1;
    }

    final bool restEligible;
    if (avgHeartRate == null) {
      restEligible = false;
    } else {
      final resting = intensityContext.restingHeartRateBpm;
      restEligible = resting != null && avgHeartRate <= resting + 8;
    }
    // Only sleep charges now. Waking rest reads as a slow basal decline, not a
    // climb — the pivotal fix over the previous model.
    final double charge;
    if (sleepMinutes > 0.0) {
      charge = 0.10 *
          sleepMinutes *
          hrvFactor.chargeMultiplier /
          respirationFactor.chargePenalty *
          gains.sleepChargeGain;
    } else {
      charge = 0.0;
    }

    final delta = charge - drain;
    score = (score + delta).clamp(0.0, 100.0);
    // Gross totals: charged is all charge, drained is all drain — the same
    // components "What moved it" breaks down. (Clamping at 0/100 means these
    // need not net exactly to end-minus-start.)
    charged += charge;
    drained += drain;

    final BodyEnergyBucketState state;
    if (sleepMinutes > 0.0) {
      state = BodyEnergyBucketState.sleep;
    } else if (workoutMinutes > 0.0 || zone >= 2) {
      state = BodyEnergyBucketState.activity;
    } else if (stressDrain > 0.0) {
      state = BodyEnergyBucketState.stress;
    } else if (restEligible) {
      state = BodyEnergyBucketState.rest;
    } else if (avgHeartRate == null) {
      state = BodyEnergyBucketState.unmeasurable;
    } else {
      state = BodyEnergyBucketState.rest;
    }
    final primaryInfluence = _primaryInfluence(
      charge: charge,
      appliedActivityDrain: appliedActivityDrain,
      energyDriven: activityEnergyDrain >= intensityDrain,
      stressDrain: stressDrain,
      recoveryDebtDrain: recoveryDebtDrain,
      sleepMinutes: sleepMinutes,
      workoutMinutes: workoutMinutes,
      zone: zone,
      state: state,
    );
    final BodyEnergyConfidence confidence;
    if (avgHeartRate == null && sleepMinutes <= 0.0) {
      confidence = BodyEnergyConfidence.low;
    } else if (intensityContext.confidence == BodyEnergyConfidence.high) {
      confidence = BodyEnergyConfidence.high;
    } else if (intensityContext.confidence == BodyEnergyConfidence.medium) {
      confidence = BodyEnergyConfidence.medium;
    } else {
      confidence = BodyEnergyConfidence.low;
    }
    switch (confidence) {
      case BodyEnergyConfidence.high:
        highConfidenceBuckets += 1;
      case BodyEnergyConfidence.medium:
        mediumConfidenceBuckets += 1;
      case BodyEnergyConfidence.low:
        lowConfidenceBuckets += 1;
      case BodyEnergyConfidence.noData:
        break;
    }
    points.add(
      BodyEnergyTimelinePoint.build(
        time: bucketStart,
        score: score.round().clamp(0, 100),
        delta: delta,
        state: state,
        confidence: confidence,
        charge: charge,
        intensityDrain: intensityDrain,
        activityEnergyDrain: activityEnergyDrain,
        basalDrain: basalDrain,
        stressDrain: stressDrain,
        recoveryDebtDrain: recoveryDebtDrain,
        primaryInfluence: primaryInfluence,
      ),
    );
  }

  final confidence = _overallConfidence(
    high: highConfidenceBuckets,
    medium: mediumConfidenceBuckets,
    low: lowConfidenceBuckets,
    total: points.length,
  );
  return BodyEnergyTimeline(
    date: inputs.date,
    startScore: startScore,
    currentScore: points.isEmpty ? startScore : points.last.score,
    charged: charged.round(),
    drained: drained.round(),
    points: points,
    confidence: confidence,
    confidenceReason: _confidenceReason(confidence, intensityContext),
    inputSummary: inputSummary,
  );
}

BodyEnergyInputSummary _inputSummary(
  BodyEnergyTimelineInputs inputs, {
  required int heartRateSampleCount,
}) =>
    BodyEnergyInputSummary(
      algorithmVersion: bodyEnergyTimelineAlgorithmVersion,
      bucketMinutes: bodyEnergyTimelineBucketMinutes,
      heartRateSampleCount: heartRateSampleCount,
      hrvSampleCount: inputs.hrvSamples.length,
      sleepSessionCount: inputs.sleepSessions.length,
      workoutCount: inputs.workouts.length,
      respiratorySampleCount: inputs.respiratoryRateSamples.length,
      hasRestingHeartRate: inputs.restingHeartRateBpm != null,
      hasBaselineRestingHeartRate: inputs.baselineRestingHeartRateBpm != null,
      hasObservedMaxHeartRate: inputs.observedMaxHeartRateBpm != null,
      hasHrvBaseline: inputs.hrvBaselineRmssdMs != null,
      hasRespiratoryBaseline: inputs.respiratoryRateBaseline != null,
      previousEndScore: inputs.previousEndScore,
      calibrationMode: _calibrationMode(
        inputs.calibration,
        inputs.bodyProfile,
        inputs.date,
      ),
    );

BodyEnergyCalibrationMode _calibrationMode(
  BodyEnergyCalibration calibration,
  BodyProfile bodyProfile,
  LocalDate date,
) {
  final normalizedCalibration = calibration.normalized();
  final normalizedProfile = bodyProfile.normalized(today: date);
  if (normalizedCalibration.useManualZones &&
      normalizedCalibration.manualZoneThresholdsBpm != null) {
    return BodyEnergyCalibrationMode.manualZones;
  }
  if (normalizedProfile.maxHeartRateBpm != null ||
      normalizedProfile.restingHeartRateBpm != null ||
      normalizedProfile.birthYear != null) {
    return BodyEnergyCalibrationMode.manualValues;
  }
  return BodyEnergyCalibrationMode.automatic;
}

BodyEnergyPrimaryInfluence _primaryInfluence({
  required double charge,
  required double appliedActivityDrain,
  required bool energyDriven,
  required double stressDrain,
  required double recoveryDebtDrain,
  required double sleepMinutes,
  required double workoutMinutes,
  required int zone,
  required BodyEnergyBucketState state,
}) {
  if (state == BodyEnergyBucketState.unmeasurable) {
    return BodyEnergyPrimaryInfluence.noData;
  }
  if (charge > 0.0 && sleepMinutes > 0.0) {
    return BodyEnergyPrimaryInfluence.sleepRecovery;
  }
  if (charge > 0.0) return BodyEnergyPrimaryInfluence.quietRest;

  // Basal drain is deliberately excluded from the competition: it is the
  // ever-present floor, reported as steady, never as the notable influence.
  final maxDrain = [
    appliedActivityDrain,
    stressDrain,
    recoveryDebtDrain,
  ].reduce(math.max);
  if (maxDrain <= 0.0) return BodyEnergyPrimaryInfluence.steady;
  if (maxDrain == appliedActivityDrain) {
    // Low-heart-rate movement with no workout is everyday activity; anything
    // heart-rate- or workout-driven is exertion.
    final everyday = energyDriven && zone < 2 && workoutMinutes <= 0.0;
    return everyday
        ? BodyEnergyPrimaryInfluence.everydayActivity
        : BodyEnergyPrimaryInfluence.exertion;
  }
  if (maxDrain == stressDrain) {
    return BodyEnergyPrimaryInfluence.elevatedHeartRate;
  }
  return BodyEnergyPrimaryInfluence.recoveryDebt;
}

class _IntensityContext {
  const _IntensityContext({
    required this.restingHeartRateBpm,
    required this.maxHeartRateBpm,
    required this.manualZones,
    required this.confidence,
  });

  final int? restingHeartRateBpm;
  final int? maxHeartRateBpm;
  final HeartZoneThresholds? manualZones;
  final BodyEnergyConfidence confidence;

  int zoneFor(double heartRateBpm) {
    final zones = manualZones;
    if (zones != null) {
      if (heartRateBpm >= zones.zone5LowerBpm) return 5;
      if (heartRateBpm >= zones.zone4LowerBpm) return 4;
      if (heartRateBpm >= zones.zone3LowerBpm) return 3;
      if (heartRateBpm >= zones.zone2LowerBpm) return 2;
      if (heartRateBpm >= zones.zone1LowerBpm) return 1;
      return 0;
    }
    final resting = restingHeartRateBpm;
    if (resting == null) return 0;
    final max = maxHeartRateBpm;
    if (max == null) return 0;
    if (max <= resting) return 0;
    final reserve = ((heartRateBpm - resting) / (max - resting).toDouble())
        .clamp(0.0, 1.0);
    if (reserve >= 0.90) return 5;
    if (reserve >= 0.75) return 4;
    if (reserve >= 0.60) return 3;
    if (reserve >= 0.45) return 2;
    if (reserve >= 0.30) return 1;
    return 0;
  }
}

_IntensityContext _resolveIntensityContext(
  BodyEnergyTimelineInputs inputs,
  List<HeartRateSample> heartRateSamples,
) {
  final calibration = inputs.calibration.normalized();
  final profile = inputs.bodyProfile.normalized(today: inputs.date);
  if (calibration.useManualZones &&
      calibration.manualZoneThresholdsBpm != null) {
    return _IntensityContext(
      restingHeartRateBpm: profile.restingHeartRateBpm ??
          inputs.restingHeartRateBpm ??
          inputs.baselineRestingHeartRateBpm ??
          _estimatedRestingHeartRate(heartRateSamples),
      maxHeartRateBpm: profile.maxHeartRateBpm,
      manualZones: calibration.manualZoneThresholdsBpm,
      confidence: BodyEnergyConfidence.high,
    );
  }

  final resting = profile.restingHeartRateBpm ??
      inputs.restingHeartRateBpm ??
      inputs.baselineRestingHeartRateBpm ??
      _estimatedRestingHeartRate(heartRateSamples);
  final observedMaxCandidates = <int>[
    if (profile.maxHeartRateBpm != null) profile.maxHeartRateBpm!,
    if (inputs.observedMaxHeartRateBpm != null) inputs.observedMaxHeartRateBpm!,
    if (heartRateSamples.isNotEmpty)
      heartRateSamples
          .map((sample) => sample.beatsPerMinute)
          .reduce(math.max),
  ];
  final observedMax = observedMaxCandidates.isEmpty
      ? null
      : observedMaxCandidates.reduce(math.max);
  final ageYears = profile.ageYears(today: inputs.date);
  final ageMax = ageYears != null ? 220 - ageYears : null;
  final int? maxHeartRate;
  if (profile.maxHeartRateBpm != null) {
    maxHeartRate = profile.maxHeartRateBpm;
  } else if (resting != null &&
      observedMax != null &&
      observedMax >= math.max(150, resting + 60)) {
    maxHeartRate = observedMax;
  } else if (ageMax != null) {
    maxHeartRate = ageMax;
  } else if (resting != null && observedMax != null) {
    maxHeartRate = math.max(observedMax + 10, resting + 70);
  } else if (resting != null) {
    maxHeartRate = resting + 70;
  } else {
    maxHeartRate = null;
  }
  final BodyEnergyConfidence confidence;
  if (profile.maxHeartRateBpm != null && resting != null) {
    confidence = BodyEnergyConfidence.high;
  } else if (resting != null &&
      observedMax != null &&
      maxHeartRate == observedMax) {
    confidence = BodyEnergyConfidence.medium;
  } else if (resting != null && ageMax != null) {
    confidence = BodyEnergyConfidence.medium;
  } else if (resting != null && maxHeartRate != null) {
    confidence = BodyEnergyConfidence.low;
  } else {
    confidence = BodyEnergyConfidence.low;
  }
  return _IntensityContext(
    restingHeartRateBpm: resting,
    maxHeartRateBpm: maxHeartRate,
    manualZones: null,
    confidence: confidence,
  );
}

class _HrvFactor {
  const _HrvFactor(this.drainMultiplier, this.chargeMultiplier);

  final double drainMultiplier;
  final double chargeMultiplier;
}

class _RespiratoryFactor {
  const _RespiratoryFactor(this.drainMultiplier, this.chargePenalty);

  final double drainMultiplier;
  final double chargePenalty;
}

_HrvFactor _hrvRecoveryFactor(double? baseline, double? average) {
  if (baseline == null || average == null) return const _HrvFactor(1.0, 1.0);
  if (average < baseline * 0.75) return const _HrvFactor(1.18, 0.75);
  if (average < baseline * 0.90) return const _HrvFactor(1.08, 0.90);
  if (average > baseline * 1.10) return const _HrvFactor(0.96, 1.12);
  return const _HrvFactor(1.0, 1.0);
}

_RespiratoryFactor _respiratoryStressFactor(double? baseline, double? average) {
  if (baseline == null || average == null) {
    return const _RespiratoryFactor(1.0, 1.0);
  }
  if (average >= baseline + 3.0) return const _RespiratoryFactor(1.12, 1.15);
  if (average >= baseline + 1.5) return const _RespiratoryFactor(1.05, 1.06);
  return const _RespiratoryFactor(1.0, 1.0);
}

double _drainRateForZone(int zone) {
  switch (zone) {
    case 1:
      return 0.03;
    case 2:
      return 0.07;
    case 3:
      return 0.14;
    case 4:
      return 0.25;
    case 5:
      return 0.40;
    default:
      return 0.0;
  }
}

BodyEnergyConfidence _overallConfidence({
  required int high,
  required int medium,
  required int low,
  required int total,
}) {
  if (total == 0) return BodyEnergyConfidence.noData;
  final covered = high + medium + low;
  if (covered == 0) return BodyEnergyConfidence.noData;
  final highRatio = high / total.toDouble();
  final mediumOrHighRatio = (high + medium) / total.toDouble();
  if (highRatio >= 0.55) return BodyEnergyConfidence.high;
  if (mediumOrHighRatio >= 0.55) return BodyEnergyConfidence.medium;
  return BodyEnergyConfidence.low;
}

String _confidenceReason(
  BodyEnergyConfidence confidence,
  _IntensityContext context,
) {
  switch (confidence) {
    case BodyEnergyConfidence.high:
      return 'Heart-rate intensity has strong calibration.';
    case BodyEnergyConfidence.medium:
      return 'Heart-rate intensity uses observed or age-based calibration.';
    case BodyEnergyConfidence.low:
      if (context.restingHeartRateBpm == null ||
          context.maxHeartRateBpm == null) {
        return 'Calibration is incomplete, so automatic estimates are conservative.';
      }
      return 'Some timeline buckets have sparse Health Connect data.';
    case BodyEnergyConfidence.noData:
      return 'No usable Health Connect data was available.';
  }
}

int? _estimatedRestingHeartRate(List<HeartRateSample> samples) {
  if (samples.isEmpty) return null;
  final sorted = samples.map((sample) => sample.beatsPerMinute).toList()..sort();
  final index =
      ((sorted.length - 1) * 0.1).round().clamp(0, sorted.length - 1);
  return sorted[index].clamp(40, 100);
}

double _overlapMinutes(
  DateTime sourceStart,
  DateTime sourceEnd,
  DateTime start,
  DateTime end,
) {
  final overlapStart = sourceStart.isAfter(start) ? sourceStart : start;
  final overlapEnd = sourceEnd.isBefore(end) ? sourceEnd : end;
  if (!overlapEnd.isAfter(overlapStart)) return 0.0;
  return overlapEnd.difference(overlapStart).inSeconds.toDouble() / 60.0;
}

List<double?> _bucketedAverages<T>(
  List<T> samples, {
  required int bucketCount,
  required DateTime dayStart,
  required DateTime Function(T) time,
  required double Function(T) value,
}) {
  if (bucketCount <= 0 || samples.isEmpty) {
    return List<double?>.filled(math.max(bucketCount, 0), null);
  }
  final sums = List<double>.filled(bucketCount, 0.0);
  final counts = List<int>.filled(bucketCount, 0);
  for (final sample in samples) {
    final minutesFromStart = time(sample).difference(dayStart).inMinutes;
    if (minutesFromStart < 0) continue;
    final bucketIndex = minutesFromStart ~/ bodyEnergyTimelineBucketMinutes;
    if (bucketIndex >= 0 && bucketIndex < bucketCount) {
      final sampleValue = value(sample);
      if (sampleValue.isFinite) {
        sums[bucketIndex] += sampleValue;
        counts[bucketIndex] += 1;
      }
    }
  }
  return List<double?>.generate(
    bucketCount,
    (index) => counts[index] > 0 ? sums[index] / counts[index] : null,
  );
}

DateTime _minInstant(DateTime a, DateTime b) => a.isBefore(b) ? a : b;

/// Active kilocalories attributed to each 5-minute bucket.
///
/// [progress] is hourly and cumulative (each point's `totalActiveCaloriesKcal`
/// is the running total at that hour's end). Treating the cumulative series as
/// piecewise-linear and differencing per bucket spreads each hour's burn evenly
/// across its buckets — the intended hourly→5-minute mapping.
List<double> _activeCaloriesPerBucket(
  List<ActivityProgressPoint> progress, {
  required int bucketCount,
  required DateTime dayStart,
}) =>
    _cumulativePerBucket(
      progress,
      bucketCount: bucketCount,
      dayStart: dayStart,
      value: (point) => point.totalActiveCaloriesKcal,
    );

List<double> _stepsPerBucket(
  List<ActivityProgressPoint> progress, {
  required int bucketCount,
  required DateTime dayStart,
}) =>
    _cumulativePerBucket(
      progress,
      bucketCount: bucketCount,
      dayStart: dayStart,
      value: (point) => point.totalSteps.toDouble(),
    );

List<double> _cumulativePerBucket(
  List<ActivityProgressPoint> progress, {
  required int bucketCount,
  required DateTime dayStart,
  required double? Function(ActivityProgressPoint) value,
}) {
  final result = List<double>.filled(math.max(bucketCount, 0), 0.0);
  if (bucketCount <= 0 || progress.isEmpty) return result;

  // Cumulative knots, minutes-from-start → running value, starting at 0.
  final knots = <(double, double)>[(0.0, 0.0)];
  final sorted = progress.where((point) => value(point) != null).toList()
    ..sort((a, b) => a.time.compareTo(b.time));
  for (final point in sorted) {
    final minute = point.time.difference(dayStart).inSeconds / 60.0;
    if (minute <= 0.0) continue;
    var v = math.max(0.0, value(point)!);
    // Guard against a non-monotonic cumulative series.
    if (v < knots.last.$2) v = knots.last.$2;
    knots.add((minute, v));
  }
  if (knots.length < 2) return result;

  double cumulativeAt(double minute) {
    if (minute <= knots.first.$1) return knots.first.$2;
    if (minute >= knots.last.$1) return knots.last.$2;
    for (var i = 1; i < knots.length; i++) {
      if (minute <= knots[i].$1) {
        final (t0, v0) = knots[i - 1];
        final (t1, v1) = knots[i];
        if (t1 <= t0) return v1;
        return v0 + (v1 - v0) * ((minute - t0) / (t1 - t0));
      }
    }
    return knots.last.$2;
  }

  for (var i = 0; i < bucketCount; i++) {
    final start = (i * bodyEnergyTimelineBucketMinutes).toDouble();
    final end = ((i + 1) * bodyEnergyTimelineBucketMinutes).toDouble();
    result[i] = math.max(0.0, cumulativeAt(end) - cumulativeAt(start));
  }
  return result;
}
