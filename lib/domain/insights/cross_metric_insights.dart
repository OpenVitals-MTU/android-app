import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';

part 'cross_metric_insights.freezed.dart';

const int _minimumCrossMetricPairs = 3;
const double _correlationTolerance = 0.0001;
const double _moderateCorrelationThreshold = 0.35;
const double _strongCorrelationThreshold = 0.7;

@freezed
abstract class CrossMetricValue with _$CrossMetricValue {
  const factory CrossMetricValue({
    required LocalDate date,
    required double value,
  }) = _CrossMetricValue;
}

enum CrossMetricDirection {
  positive,
  negative,
  neutral,
}

enum CrossMetricStrength {
  weak,
  moderate,
  strong,
}

@freezed
abstract class CrossMetricInsight with _$CrossMetricInsight {
  const CrossMetricInsight._();

  const factory CrossMetricInsight({
    required double correlation,
    required int pairedDays,
  }) = _CrossMetricInsight;

  CrossMetricDirection get direction {
    if (correlation > _correlationTolerance) return CrossMetricDirection.positive;
    if (correlation < -_correlationTolerance) {
      return CrossMetricDirection.negative;
    }
    return CrossMetricDirection.neutral;
  }

  CrossMetricStrength get strength {
    if (correlation.abs() >= _strongCorrelationThreshold) {
      return CrossMetricStrength.strong;
    }
    if (correlation.abs() >= _moderateCorrelationThreshold) {
      return CrossMetricStrength.moderate;
    }
    return CrossMetricStrength.weak;
  }
}

CrossMetricInsight? crossMetricInsight(
  List<CrossMetricValue> primaryValues,
  List<CrossMetricValue> secondaryValues,
) {
  final primaryByDate = <LocalDate, CrossMetricValue>{
    for (final value in primaryValues.where((value) => value.value > 0.0))
      value.date: value,
  };
  final pairs = <(double, double)>[];
  for (final secondary in secondaryValues.where((value) => value.value > 0.0)) {
    final primary = primaryByDate[secondary.date];
    if (primary != null) pairs.add((primary.value, secondary.value));
  }

  if (pairs.length < _minimumCrossMetricPairs) return null;

  final primaryAverage =
      _average(pairs.map((pair) => pair.$1).toList());
  final secondaryAverage =
      _average(pairs.map((pair) => pair.$2).toList());
  final primaryVariance = pairs.fold<double>(
    0.0,
    (sum, pair) => sum + (pair.$1 - primaryAverage) * (pair.$1 - primaryAverage),
  );
  final secondaryVariance = pairs.fold<double>(
    0.0,
    (sum, pair) =>
        sum + (pair.$2 - secondaryAverage) * (pair.$2 - secondaryAverage),
  );
  if (primaryVariance <= _correlationTolerance ||
      secondaryVariance <= _correlationTolerance) {
    return CrossMetricInsight(correlation: 0.0, pairedDays: pairs.length);
  }

  final covariance = pairs.fold<double>(
    0.0,
    (sum, pair) =>
        sum + (pair.$1 - primaryAverage) * (pair.$2 - secondaryAverage),
  );
  final correlation =
      covariance / math.sqrt(primaryVariance * secondaryVariance);
  return CrossMetricInsight(
    correlation: correlation.clamp(-1.0, 1.0).toDouble(),
    pairedDays: pairs.length,
  );
}

double _average(List<double> values) =>
    values.fold<double>(0.0, (sum, value) => sum + value) / values.length;
