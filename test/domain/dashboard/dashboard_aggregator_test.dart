import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/dashboard/dashboard_aggregator.dart';
import 'package:openvitals/domain/model/dashboard_data.dart';
import 'package:openvitals/domain/model/dashboard_query.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';

void main() {
  test('weekly cardio target prefers recent history median', () {
    final target = DashboardAggregator.weeklyCardioTarget(
      currentScore: 120,
      daysElapsed: 3,
      previousWeekScores: [0, 100, 110, 105],
    );

    expect(target?.score, 105);
    expect(target?.source, DashboardWeeklyCardioLoadTargetSource.recentHistory);
  });

  test('merge derived projection keeps base calories unless estimated loaded',
      () {
    final base = DashboardData(
      date: LocalDate(2026, 6, 1),
      caloriesKcal: 100.0,
      caloriesKcalSource: CaloriesBurnedSource.noData,
    );
    final projection = DashboardData(
      date: LocalDate(2026, 6, 1),
      caloriesKcal: 456.0,
      caloriesKcalSource: CaloriesBurnedSource.estimatedActiveAndBmr,
      loadedMetrics: const {DashboardMetric.caloriesOut},
    );

    final merged =
        DashboardAggregator.mergeDerivedDashboardProjection(base, projection);

    expect(merged.caloriesKcal, closeTo(456.0, 0.01));
    expect(merged.caloriesKcalSource,
        CaloriesBurnedSource.estimatedActiveAndBmr);
    expect(merged.loadedMetrics, {DashboardMetric.caloriesOut});
  });

  test('median long returns middle value', () {
    expect(DashboardAggregator.medianLongOrNull([1, 5, 9]), 5);
    expect(DashboardAggregator.medianLongOrNull([]), isNull);
  });
}
