import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../domain/query/activity_period_data.dart';
import '../../../ui/charts/bar_chart.dart';
import '../presentation/activity_metric.dart';

part 'calories_display.freezed.dart';

/// One of the two calories series the screen stacks: its dated bar values, its
/// period total, and whether anything was recorded at all (which is what swaps
/// the card for its placeholder).
@freezed
abstract class CaloriesMetricSeries with _$CaloriesMetricSeries {
  const factory CaloriesMetricSeries({
    required List<PeriodChartValue> values,
    required bool hasData,
    required double total,
  }) = _CaloriesMetricSeries;
}

/// The screen-ready derivation of one loaded calories period. Built once per
/// load by [buildCaloriesDisplay] — the screen folds nothing.
@freezed
abstract class CaloriesDisplay with _$CaloriesDisplay {
  const factory CaloriesDisplay({
    required CaloriesMetricSeries caloriesOut,
    required CaloriesMetricSeries activeCalories,
  }) = _CaloriesDisplay;
}

/// Pure derivation from the loaded period to its display model.
CaloriesDisplay buildCaloriesDisplay(ActivityPeriodData data) => CaloriesDisplay(
      caloriesOut: _series(ActivityMetric.caloriesOut, data),
      activeCalories: _series(ActivityMetric.activeCalories, data),
    );

CaloriesMetricSeries _series(ActivityMetric metric, ActivityPeriodData data) {
  final values = metric.chartValues(data);
  return CaloriesMetricSeries(
    values: values,
    hasData: values.any((value) => value.value > 0.0),
    total: values.fold<double>(0.0, (sum, value) => sum + value.value),
  );
}
