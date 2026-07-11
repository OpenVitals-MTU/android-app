import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/heart_models.dart';
import 'cardio_load.dart';

part 'intensity_minutes.freezed.dart';

const int defaultWeeklyIntensityMinutesTarget = 150;

const double _moderateHeartRateReserveThreshold = 0.40;
const double _vigorousHeartRateReserveThreshold = 0.60;
const double _goodHeartRateCoverageMinutes = 10.0;
const double _goodHeartRateCoverageRatio = 0.60;
const double _maxHeartRateSampleGapMinutes = 5.0;
const double _vigorousKcalPerMinute = 8.0;
const double _moderateKcalPerMinute = 3.0;
const double _dailyActiveCaloriesModerateKcalPerMinute = 5.0;
const double _cardioLoadToModerateEquivalentMinutes = 4.0;
const int _observedMaxHeartRateMinimumBpm = 150;
const int _observedMaxHeartRateRestingDeltaBpm = 60;

enum IntensityMinutesConfidence {
  high('HIGH'),
  medium('MEDIUM'),
  low('LOW'),
  noData('NO_DATA');

  const IntensityMinutesConfidence(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static IntensityMinutesConfidence? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

enum IntensityMinutesMethod {
  heartRateReserve,
  workoutActiveCalories,
  workoutDuration,
  dailyActiveCalories,
  cardioLoad,
  noData,
}

@freezed
abstract class IntensityWorkoutInput with _$IntensityWorkoutInput {
  const factory IntensityWorkoutInput({
    required double durationMinutes,
    double? activeCaloriesKcal,
  }) = _IntensityWorkoutInput;
}

@freezed
abstract class IntensityMinutesEstimate with _$IntensityMinutesEstimate {
  const factory IntensityMinutesEstimate({
    @Default(0) int moderateMinutes,
    @Default(0) int vigorousMinutes,
    @Default(0) int moderateEquivalentMinutes,
    @Default(IntensityMinutesConfidence.noData)
    IntensityMinutesConfidence confidence,
    @Default(IntensityMinutesMethod.noData) IntensityMinutesMethod method,
    @Default(0.0) double coveredHeartRateMinutes,
    @Default(0.0) double expectedHeartRateMinutes,
    @Default(0) int heartRateSampleCount,
  }) = _IntensityMinutesEstimate;

  static const IntensityMinutesEstimate noData = IntensityMinutesEstimate();
}

class _IntensityMaxHeartRateContext {
  const _IntensityMaxHeartRateContext({
    required this.bpm,
    required this.isObservedAvailable,
  });

  final int bpm;
  final bool isObservedAvailable;
}

class _IntensityMinuteAccumulator {
  const _IntensityMinuteAccumulator({
    required this.moderateMinutes,
    required this.vigorousMinutes,
    required this.coveredHeartRateMinutes,
    required this.expectedHeartRateMinutes,
  });

  final double moderateMinutes;
  final double vigorousMinutes;
  final double coveredHeartRateMinutes;
  final double expectedHeartRateMinutes;

  double get moderateEquivalentMinutes =>
      moderateMinutes + (vigorousMinutes * 2.0);

  bool get hasGoodCoverage =>
      coveredHeartRateMinutes >= _goodHeartRateCoverageMinutes &&
      (expectedHeartRateMinutes <= 0.0 ||
          coveredHeartRateMinutes / expectedHeartRateMinutes >=
              _goodHeartRateCoverageRatio);
}

IntensityMinutesEstimate calculateIntensityMinutes(
  List<HeartRateSample> samples,
  int? restingHeartRate,
  int? baselineRestingHeartRate,
  int? observedMaxHeartRate,
  List<CardioLoadTimeWindow> activityWindows,
  List<IntensityWorkoutInput> workouts,
  double? dailyActiveCaloriesKcal,
  int? cardioLoadScore,
) {
  final resting = restingHeartRate ??
      baselineRestingHeartRate ??
      _estimatedRestingHeartRate(samples);
  final maxHeartRate = resting != null
      ? _maxHeartRateContext(observedMaxHeartRate, samples, resting)
      : null;
  if (resting != null && maxHeartRate != null) {
    final heartRateEstimate = _calculateHeartRateReserveIntensity(
      samples,
      resting,
      maxHeartRate.bpm,
      maxHeartRate.isObservedAvailable,
      restingHeartRate != null,
      activityWindows,
    );
    if (heartRateEstimate != null) return heartRateEstimate;
  }

  final workoutEstimate = _workoutFallbackIntensity(workouts);
  if (workoutEstimate != null) return workoutEstimate;
  final dailyEstimate =
      _dailyActiveCaloriesFallbackIntensity(dailyActiveCaloriesKcal);
  if (dailyEstimate != null) return dailyEstimate;
  final cardioEstimate = _cardioLoadFallbackIntensity(cardioLoadScore);
  if (cardioEstimate != null) return cardioEstimate;
  return IntensityMinutesEstimate.noData;
}

IntensityMinutesEstimate? _calculateHeartRateReserveIntensity(
  List<HeartRateSample> samples,
  int restingHeartRate,
  int maxHeartRate,
  bool maxHeartRateObserved,
  bool restingHeartRateObserved,
  List<CardioLoadTimeWindow> activityWindows,
) {
  final sortedSamples = _sortedDistinctByTime(samples);
  if (sortedSamples.length < 2 || maxHeartRate <= restingHeartRate) return null;

  var moderateMinutes = 0.0;
  var vigorousMinutes = 0.0;
  var coveredMinutes = 0.0;
  final double? expectedMinutes = activityWindows.isNotEmpty
      ? activityWindows.fold<double>(
          0.0, (sum, window) => sum + window.durationMinutes)
      : null;

  for (var i = 0; i < sortedSamples.length - 1; i++) {
    final start = sortedSamples[i];
    final end = sortedSamples[i + 1];
    final rawMinutes =
        math.max(0, end.time.difference(start.time).inSeconds).toDouble() / 60.0;
    if (rawMinutes <= 0.0 || rawMinutes > _maxHeartRateSampleGapMinutes) continue;

    final interval = CardioLoadTimeWindow(start: start.time, end: end.time);
    final intervalMinutes = activityWindows.isNotEmpty
        ? activityWindows.fold<double>(
            0.0, (sum, window) => sum + _overlapMinutes(interval, window))
        : rawMinutes;
    if (intervalMinutes <= 0.0) continue;

    final averageBpm = (start.beatsPerMinute + end.beatsPerMinute) / 2.0;
    final heartRateReserve =
        ((averageBpm - restingHeartRate) / (maxHeartRate - restingHeartRate))
            .clamp(0.0, 1.0)
            .toDouble();
    if (heartRateReserve >= _vigorousHeartRateReserveThreshold) {
      vigorousMinutes += intervalMinutes;
      coveredMinutes += intervalMinutes;
    } else if (heartRateReserve >= _moderateHeartRateReserveThreshold) {
      moderateMinutes += intervalMinutes;
      coveredMinutes += intervalMinutes;
    }
  }

  final accumulator = _IntensityMinuteAccumulator(
    moderateMinutes: moderateMinutes,
    vigorousMinutes: vigorousMinutes,
    coveredHeartRateMinutes: coveredMinutes,
    expectedHeartRateMinutes: expectedMinutes ?? coveredMinutes,
  );
  if (accumulator.moderateEquivalentMinutes <= 0.0) return null;

  final IntensityMinutesConfidence confidence;
  if (accumulator.hasGoodCoverage &&
      restingHeartRateObserved &&
      maxHeartRateObserved) {
    confidence = IntensityMinutesConfidence.high;
  } else if (accumulator.hasGoodCoverage) {
    confidence = IntensityMinutesConfidence.medium;
  } else {
    confidence = IntensityMinutesConfidence.low;
  }
  return IntensityMinutesEstimate(
    moderateMinutes: accumulator.moderateMinutes.round(),
    vigorousMinutes: accumulator.vigorousMinutes.round(),
    moderateEquivalentMinutes: accumulator.moderateEquivalentMinutes.round(),
    confidence: confidence,
    method: IntensityMinutesMethod.heartRateReserve,
    coveredHeartRateMinutes: accumulator.coveredHeartRateMinutes,
    expectedHeartRateMinutes: accumulator.expectedHeartRateMinutes,
    heartRateSampleCount: sortedSamples.length,
  );
}

IntensityMinutesEstimate? _workoutFallbackIntensity(
  List<IntensityWorkoutInput> workouts,
) {
  var moderateMinutes = 0.0;
  var vigorousMinutes = 0.0;
  var durationOnlyMinutes = 0.0;
  for (final workout in workouts) {
    final duration = math.max(0.0, workout.durationMinutes);
    if (duration <= 0.0) continue;
    final activeCalories = workout.activeCaloriesKcal;
    if (activeCalories != null && activeCalories > 0.0) {
      final kcalPerMinute = activeCalories / duration;
      if (kcalPerMinute >= _vigorousKcalPerMinute) {
        vigorousMinutes += duration;
      } else if (kcalPerMinute >= _moderateKcalPerMinute) {
        moderateMinutes += duration;
      }
    } else {
      durationOnlyMinutes += duration * 0.5;
    }
  }

  final moderateEquivalent = moderateMinutes + vigorousMinutes * 2.0;
  if (moderateEquivalent > 0.0) {
    return IntensityMinutesEstimate(
      moderateMinutes: moderateMinutes.round(),
      vigorousMinutes: vigorousMinutes.round(),
      moderateEquivalentMinutes: moderateEquivalent.round(),
      confidence: IntensityMinutesConfidence.low,
      method: IntensityMinutesMethod.workoutActiveCalories,
    );
  }

  if (durationOnlyMinutes >= 5.0) {
    final minutes = durationOnlyMinutes.round();
    return IntensityMinutesEstimate(
      moderateMinutes: minutes,
      moderateEquivalentMinutes: minutes,
      confidence: IntensityMinutesConfidence.low,
      method: IntensityMinutesMethod.workoutDuration,
    );
  }
  return null;
}

IntensityMinutesEstimate? _dailyActiveCaloriesFallbackIntensity(
  double? dailyActiveCaloriesKcal,
) {
  if (dailyActiveCaloriesKcal == null || dailyActiveCaloriesKcal <= 0.0) {
    return null;
  }
  final raw = dailyActiveCaloriesKcal / _dailyActiveCaloriesModerateKcalPerMinute;
  if (raw < 5.0) return null;
  final minutes = raw.round();
  return IntensityMinutesEstimate(
    moderateMinutes: minutes,
    moderateEquivalentMinutes: minutes,
    confidence: IntensityMinutesConfidence.low,
    method: IntensityMinutesMethod.dailyActiveCalories,
  );
}

IntensityMinutesEstimate? _cardioLoadFallbackIntensity(int? cardioLoadScore) {
  if (cardioLoadScore == null || cardioLoadScore <= 0) return null;
  final raw = cardioLoadScore * _cardioLoadToModerateEquivalentMinutes;
  if (raw < 5.0) return null;
  final minutes = raw.round();
  return IntensityMinutesEstimate(
    moderateMinutes: minutes,
    moderateEquivalentMinutes: minutes,
    confidence: IntensityMinutesConfidence.low,
    method: IntensityMinutesMethod.cardioLoad,
  );
}

_IntensityMaxHeartRateContext? _maxHeartRateContext(
  int? observedMaxHeartRate,
  List<HeartRateSample> samples,
  int restingHeartRate,
) {
  final sampleMax = samples.isEmpty
      ? null
      : samples.map((sample) => sample.beatsPerMinute).reduce(math.max);
  final candidates = <int>[
    ?observedMaxHeartRate,
    ?sampleMax,
  ];
  if (candidates.isEmpty) return null;
  final observedMax = candidates.reduce(math.max);
  final observedAvailable = observedMax >=
      math.max(
        _observedMaxHeartRateMinimumBpm,
        restingHeartRate + _observedMaxHeartRateRestingDeltaBpm,
      );
  final estimatedMax = math.max(
    observedMax + 10,
    restingHeartRate + 70,
  );
  return _IntensityMaxHeartRateContext(
    bpm: observedAvailable ? observedMax : estimatedMax,
    isObservedAvailable: observedAvailable,
  );
}

int? _estimatedRestingHeartRate(List<HeartRateSample> samples) {
  if (samples.isEmpty) return null;
  final sorted = samples.map((sample) => sample.beatsPerMinute).toList()..sort();
  final index =
      (((sorted.length - 1) * 0.1).round()).clamp(0, sorted.length - 1);
  return sorted[index].clamp(40, 100).toInt();
}

double _overlapMinutes(CardioLoadTimeWindow a, CardioLoadTimeWindow b) {
  final overlapStart = a.start.isAfter(b.start) ? a.start : b.start;
  final overlapEnd = a.end.isBefore(b.end) ? a.end : b.end;
  if (!overlapEnd.isAfter(overlapStart)) return 0.0;
  return overlapEnd.difference(overlapStart).inSeconds.toDouble() / 60.0;
}

List<HeartRateSample> _sortedDistinctByTime(List<HeartRateSample> samples) {
  final sorted = [...samples]..sort((a, b) => a.time.compareTo(b.time));
  final seen = <DateTime>{};
  final result = <HeartRateSample>[];
  for (final sample in sorted) {
    if (seen.add(sample.time)) result.add(sample);
  }
  return result;
}
