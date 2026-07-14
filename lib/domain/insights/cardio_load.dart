import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/activity_models.dart';
import '../model/heart_models.dart';
import 'max_heart_rate.dart';

part 'cardio_load.freezed.dart';

const double _minimumTrimpMinutes = 5.0;
const double _goodHeartRateCoverageMinutes = 10.0;
const double _goodHeartRateCoverageRatio = 0.6;
const double _maxHeartRateSampleGapMinutes = 5.0;
const double _activeHeartRateReserveThreshold = 0.3;
const double _minimumMovementFallbackLoad = 0.25;

enum CardioLoadConfidence {
  high('HIGH'),
  medium('MEDIUM'),
  low('LOW'),
  noData('NO_DATA');

  const CardioLoadConfidence(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static CardioLoadConfidence? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

enum CardioLoadMethod {
  trimpActivityWindows,
  trimpElevatedHeartRate,
  movementFallback,
  noData,
}

@freezed
abstract class CardioLoadEstimate with _$CardioLoadEstimate {
  const factory CardioLoadEstimate({
    @Default(0) int score,
    @Default(CardioLoadConfidence.noData) CardioLoadConfidence confidence,
    @Default(CardioLoadMethod.noData) CardioLoadMethod method,
    double? trimpScore,
    @Default(0.0) double coveredMinutes,
    @Default(0.0) double expectedMinutes,
    int? restingHeartRateBpm,
    @Default(false) bool restingHeartRateObserved,
    int? maxHeartRateBpm,
    @Default(false) bool maxHeartRateObserved,
    @Default(0) int heartRateSampleCount,
    @Default(0) int activityWindowCount,
    @Default(0.0) double activityWindowMinutes,
    @Default(0) int movementFallbackScore,
  }) = _CardioLoadEstimate;

  static const CardioLoadEstimate noData = CardioLoadEstimate();
}

@freezed
abstract class CardioLoadTimeWindow with _$CardioLoadTimeWindow {
  const CardioLoadTimeWindow._();

  const factory CardioLoadTimeWindow({
    required DateTime start,
    required DateTime end,
  }) = _CardioLoadTimeWindow;

  double get durationMinutes =>
      math.max(0, end.difference(start).inSeconds).toDouble() / 60.0;
}

class _MaxHeartRateContext {
  const _MaxHeartRateContext({
    required this.bpm,
    required this.isObservedAvailable,
  });

  final int bpm;
  final bool isObservedAvailable;
}

class _TrimpResult {
  const _TrimpResult({
    required this.score,
    required this.coveredMinutes,
    required this.expectedMinutes,
  });

  final double score;
  final double coveredMinutes;
  final double expectedMinutes;

  bool get hasGoodCoverage =>
      coveredMinutes >= _goodHeartRateCoverageMinutes &&
      (expectedMinutes <= 0.0 ||
          coveredMinutes / expectedMinutes >= _goodHeartRateCoverageRatio);
}

CardioLoadEstimate calculateCardioLoad(
  DailySteps? steps,
  List<HeartRateSample> samples,
  int? restingHeartRate,
  int? baselineRestingHeartRate,
  int? observedMaxHeartRate,
  List<CardioLoadTimeWindow> activityWindows,
) {
  final fallback = _movementFallbackCardioLoad(steps);
  final resting = restingHeartRate ??
      baselineRestingHeartRate ??
      _estimatedRestingHeartRate(samples);
  final maxHeartRate =
      resting != null ? _maxHeartRateContext(observedMaxHeartRate, samples, resting) : null;
  final trimp = (resting != null && maxHeartRate != null)
      ? _calculateTrimp(
          samples,
          resting,
          maxHeartRate.bpm,
          activityWindows,
        )
      : null;

  final activityWindowMinutes = activityWindows.fold<double>(
    0.0,
    (sum, window) => sum + window.durationMinutes,
  );
  if (trimp != null &&
      trimp.coveredMinutes >= _minimumTrimpMinutes &&
      trimp.score > 0.0) {
    final CardioLoadConfidence confidence;
    if (trimp.hasGoodCoverage &&
        restingHeartRate != null &&
        maxHeartRate?.isObservedAvailable == true) {
      confidence = CardioLoadConfidence.high;
    } else if (trimp.hasGoodCoverage) {
      confidence = CardioLoadConfidence.medium;
    } else {
      confidence = CardioLoadConfidence.low;
    }
    return CardioLoadEstimate(
      score: math.max(1, trimp.score.round()),
      confidence: confidence,
      method: activityWindows.isNotEmpty
          ? CardioLoadMethod.trimpActivityWindows
          : CardioLoadMethod.trimpElevatedHeartRate,
      trimpScore: trimp.score,
      coveredMinutes: trimp.coveredMinutes,
      expectedMinutes: trimp.expectedMinutes,
      restingHeartRateBpm: resting,
      restingHeartRateObserved: restingHeartRate != null,
      maxHeartRateBpm: maxHeartRate?.bpm,
      maxHeartRateObserved: maxHeartRate?.isObservedAvailable == true,
      heartRateSampleCount: samples.length,
      activityWindowCount: activityWindows.length,
      activityWindowMinutes: activityWindowMinutes,
      movementFallbackScore: fallback,
    );
  }

  if (fallback > 0) {
    return CardioLoadEstimate(
      score: fallback,
      confidence: CardioLoadConfidence.low,
      method: CardioLoadMethod.movementFallback,
      restingHeartRateBpm: resting,
      restingHeartRateObserved: restingHeartRate != null,
      maxHeartRateBpm: maxHeartRate?.bpm,
      maxHeartRateObserved: maxHeartRate?.isObservedAvailable == true,
      heartRateSampleCount: samples.length,
      activityWindowCount: activityWindows.length,
      activityWindowMinutes: activityWindowMinutes,
      movementFallbackScore: fallback,
    );
  }
  return CardioLoadEstimate.noData;
}

_TrimpResult? _calculateTrimp(
  List<HeartRateSample> samples,
  int restingHeartRate,
  int maxHeartRate,
  List<CardioLoadTimeWindow> activityWindows,
) {
  final sortedSamples = _sortedDistinctByTime(samples);
  if (sortedSamples.length < 2 || maxHeartRate <= restingHeartRate) return null;

  var score = 0.0;
  var coveredMinutes = 0.0;
  final double? expectedMinutes = activityWindows.isNotEmpty
      ? activityWindows.fold<double>(
          0.0, (sum, window) => sum + window.durationMinutes)
      : null;

  for (var i = 0; i < sortedSamples.length - 1; i++) {
    final start = sortedSamples[i];
    final end = sortedSamples[i + 1];
    final interval = CardioLoadTimeWindow(start: start.time, end: end.time);
    final rawMinutes = interval.durationMinutes;
    if (rawMinutes <= 0.0 || rawMinutes > _maxHeartRateSampleGapMinutes) continue;

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
    if (activityWindows.isEmpty &&
        heartRateReserve < _activeHeartRateReserveThreshold) {
      continue;
    }

    coveredMinutes += intervalMinutes;
    score += intervalMinutes *
        heartRateReserve *
        0.64 *
        math.exp(1.92 * heartRateReserve);
  }

  if (coveredMinutes <= 0.0) return null;
  return _TrimpResult(
    score: score,
    coveredMinutes: coveredMinutes,
    expectedMinutes: expectedMinutes ?? coveredMinutes,
  );
}

int _movementFallbackCardioLoad(DailySteps? steps) {
  if (steps == null) return 0;
  final rawLoad = math.max(
    steps.steps.toDouble() / 3000.0,
    math.max(
      steps.distanceMeters / 1500.0,
      (steps.activeCaloriesKcal ?? 0.0) / 75.0,
    ),
  );
  if (rawLoad >= _minimumMovementFallbackLoad) {
    return math.max(1, rawLoad.round());
  }
  return 0;
}

_MaxHeartRateContext? _maxHeartRateContext(
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
  final observedAvailable =
      isObservedMaxHeartRateTrustworthy(observedMax, restingHeartRate);
  final estimatedMax = math.max(
    observedMax + 10,
    restingHeartRate + 70,
  );
  return _MaxHeartRateContext(
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
