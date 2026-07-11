import 'package:freezed_annotation/freezed_annotation.dart';

part 'period_comparison.freezed.dart';

const double _comparisonTolerance = 0.0001;

enum PeriodComparisonDirection {
  up,
  down,
  same,
}

@freezed
abstract class PeriodComparison with _$PeriodComparison {
  const PeriodComparison._();

  const factory PeriodComparison({
    required double currentValue,
    required double previousValue,
  }) = _PeriodComparison;

  double get change => currentValue - previousValue;

  double get absoluteChange => change.abs();

  double? get percentChange => previousValue.abs() > _comparisonTolerance
      ? change / previousValue * 100.0
      : null;

  PeriodComparisonDirection get direction {
    if (change > _comparisonTolerance) return PeriodComparisonDirection.up;
    if (change < -_comparisonTolerance) return PeriodComparisonDirection.down;
    return PeriodComparisonDirection.same;
  }
}

PeriodComparison periodComparison(
  double currentValue,
  double previousValue,
) =>
    PeriodComparison(
      currentValue: currentValue,
      previousValue: previousValue,
    );
