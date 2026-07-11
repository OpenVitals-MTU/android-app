import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';

part 'personal_baseline.freezed.dart';

const int _minimumBaselineSamples = 3;
const double _anomalyZScoreThreshold = 2.0;
const double _baselineTolerance = 0.0001;
const List<int> personalBaselineWindows = [30, 60, 90];

@freezed
abstract class BaselineValue with _$BaselineValue {
  const factory BaselineValue({
    required LocalDate date,
    required double value,
  }) = _BaselineValue;
}

@freezed
abstract class BaselineSummary with _$BaselineSummary {
  const BaselineSummary._();

  const factory BaselineSummary({
    required int windowDays,
    required double average,
    required double standardDeviation,
    required int sampleCount,
  }) = _BaselineSummary;

  double get usualLow => average - standardDeviation;
  double get usualHigh => average + standardDeviation;
}

enum BaselineStatus {
  usual,
  above,
  below,
  unusualHigh,
  unusualLow,
}

@freezed
abstract class PersonalBaselineInsight with _$PersonalBaselineInsight {
  const PersonalBaselineInsight._();

  const factory PersonalBaselineInsight({
    required double currentValue,
    required BaselineSummary primarySummary,
    required List<BaselineSummary> summaries,
    required BaselineStatus status,
  }) = _PersonalBaselineInsight;

  double get deviation => currentValue - primarySummary.average;
  double get absoluteDeviation => deviation.abs();
  double? get percentDeviation =>
      primarySummary.average.abs() > _baselineTolerance
          ? deviation / primarySummary.average * 100.0
          : null;
}

PersonalBaselineInsight? personalBaselineInsight(
  double currentValue,
  List<BaselineValue> values,
  LocalDate referenceDate, {
  List<int> windows = personalBaselineWindows,
}) {
  final sortedWindows = [...windows]..sort();
  final summaries = <BaselineSummary>[];
  for (final windowDays in sortedWindows) {
    final summary = _baselineSummary(windowDays, values, referenceDate);
    if (summary != null) summaries.add(summary);
  }

  if (summaries.isEmpty) return null;
  final primarySummary = summaries.last;
  final status = _baselineStatus(currentValue, primarySummary);
  return PersonalBaselineInsight(
    currentValue: currentValue,
    primarySummary: primarySummary,
    summaries: summaries,
    status: status,
  );
}

BaselineSummary? _baselineSummary(
  int windowDays,
  List<BaselineValue> values,
  LocalDate referenceDate,
) {
  final start = referenceDate.minusDays(windowDays - 1);
  final windowValues = values
      .where((value) => value.date.isBetween(start, referenceDate))
      .map((value) => value.value)
      .where((value) => value > 0.0)
      .toList();

  if (windowValues.length < _minimumBaselineSamples) return null;

  final average = _average(windowValues);
  final standardDeviation = _standardDeviation(windowValues, average);
  return BaselineSummary(
    windowDays: windowDays,
    average: average,
    standardDeviation: standardDeviation,
    sampleCount: windowValues.length,
  );
}

BaselineStatus _baselineStatus(double currentValue, BaselineSummary summary) {
  final standardDeviation = summary.standardDeviation;
  if (standardDeviation <= _baselineTolerance) {
    if (currentValue > summary.average + _baselineTolerance) {
      return BaselineStatus.above;
    }
    if (currentValue < summary.average - _baselineTolerance) {
      return BaselineStatus.below;
    }
    return BaselineStatus.usual;
  }

  final zScore = (currentValue - summary.average) / standardDeviation;
  if (zScore >= _anomalyZScoreThreshold) return BaselineStatus.unusualHigh;
  if (zScore <= -_anomalyZScoreThreshold) return BaselineStatus.unusualLow;
  if (currentValue > summary.usualHigh) return BaselineStatus.above;
  if (currentValue < summary.usualLow) return BaselineStatus.below;
  return BaselineStatus.usual;
}

double _average(List<double> values) =>
    values.fold<double>(0.0, (sum, value) => sum + value) / values.length;

double _standardDeviation(List<double> values, double average) {
  final variance = values.fold<double>(
        0.0,
        (sum, value) => sum + (value - average) * (value - average),
      ) /
      values.length;
  return math.sqrt(variance);
}
