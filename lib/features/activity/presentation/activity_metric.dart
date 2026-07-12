import 'package:flutter/material.dart';

import '../../../core/presentation/display_value.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/query/activity_period_data.dart';
import '../../../data/source/health/health_permissions.dart';
import '../../../ui/charts/bar_chart.dart';
import '../../../ui/theme/app_colors.dart';

/// The movement-metric family surfaced by the shared period-detail screen.
///
/// Port of the Kotlin `ActivityMetric` enum (`ActivityScreen.kt`) plus the
/// per-metric configuration the Kotlin `ActivityMetricContent` /
/// `ActivityPresentationMapper` spread across many `*Content`/`*Display`
/// functions. Each constant knows its route id, chrome (title/icon/accent), the
/// Health Connect read permission it needs, which slices of an
/// [ActivityPeriodData] to load, and how to extract + format its daily values.
enum ActivityMetric {
  steps('STEPS', 'Steps', Icons.directions_walk, AppColors.steps),
  distance('DISTANCE', 'Distance', Icons.straighten, AppColors.distance),
  caloriesOut(
    'CALORIES_OUT',
    'Calories burned',
    Icons.local_fire_department,
    AppColors.calories,
  ),
  activeCalories(
    'ACTIVE_CALORIES',
    'Active calories',
    Icons.local_fire_department,
    AppColors.activeCalories,
  ),
  floors('FLOORS', 'Floors climbed', Icons.stairs, AppColors.floors),
  elevation('ELEVATION', 'Elevation gained', Icons.terrain, AppColors.elevation),
  wheelchair(
    'WHEELCHAIR_PUSHES',
    'Wheelchair pushes',
    Icons.accessible_forward,
    AppColors.wheelchairPushes,
  );

  const ActivityMetric(this.routeName, this.title, this.icon, this.accentColor);

  /// The `DashboardMetricId.storageName` (SCREAMING_SNAKE_CASE) this metric is
  /// reached through on the `/metric/:metricId` route.
  final String routeName;
  final String title;
  final IconData icon;
  final Color accentColor;

  /// The Health Connect read permission the [HealthConnectGate] requires.
  String get readPermission {
    switch (this) {
      case ActivityMetric.steps:
        return HcPermissions.readSteps;
      case ActivityMetric.distance:
        return HcPermissions.readDistance;
      case ActivityMetric.caloriesOut:
        return HcPermissions.readTotalCalories;
      case ActivityMetric.activeCalories:
        return HcPermissions.readActiveCalories;
      case ActivityMetric.floors:
        return HcPermissions.readFloors;
      case ActivityMetric.elevation:
        return HcPermissions.readElevation;
      case ActivityMetric.wheelchair:
        return HcPermissions.readWheelchairPushes;
    }
  }

  /// Mirrors the Kotlin `usesDailySteps` (false for calories-burned and
  /// wheelchair pushes, which come from other slices).
  bool get usesDailySteps =>
      this != ActivityMetric.caloriesOut && this != ActivityMetric.wheelchair;

  bool get usesNutrition => this == ActivityMetric.caloriesOut;

  bool get usesWheelchairPushes => this == ActivityMetric.wheelchair;

  /// Message shown when the selected period has no readings.
  String get emptyMessage {
    switch (this) {
      case ActivityMetric.steps:
        return 'No step updates for this period.';
      case ActivityMetric.distance:
        return 'No distance updates for this period.';
      case ActivityMetric.caloriesOut:
        return 'No calories burned for this period.';
      case ActivityMetric.activeCalories:
        return 'No active calories for this period.';
      case ActivityMetric.floors:
        return 'No floors climbed for this period.';
      case ActivityMetric.elevation:
        return 'No elevation gained for this period.';
      case ActivityMetric.wheelchair:
        return 'No wheelchair pushes for this period.';
    }
  }

  /// The dated daily values for the period chart, pulled from the right slice of
  /// [data] (nutrition for calories-burned, daily steps otherwise).
  List<PeriodChartValue> chartValues(ActivityPeriodData data) {
    if (this == ActivityMetric.caloriesOut) {
      return [
        for (final entry in data.nutrition)
          PeriodChartValue(entry.date, entry.caloriesBurnedKcal),
      ];
    }
    return [
      for (final entry in data.dailySteps)
        PeriodChartValue(entry.date, _dailyValue(entry)),
    ];
  }

  double _dailyValue(DailySteps entry) {
    switch (this) {
      case ActivityMetric.steps:
        return entry.steps.toDouble();
      case ActivityMetric.distance:
        return entry.distanceMeters;
      case ActivityMetric.activeCalories:
        return entry.activeCaloriesKcal ?? 0.0;
      case ActivityMetric.floors:
        return (entry.floorsClimbed ?? 0).toDouble();
      case ActivityMetric.elevation:
        return entry.elevationGainedMeters ?? 0.0;
      case ActivityMetric.wheelchair:
        return (entry.wheelchairPushes ?? 0).toDouble();
      case ActivityMetric.caloriesOut:
        return 0.0; // handled in chartValues via nutrition
    }
  }

  /// Formats a metric value + unit for a hero/statistics tile.
  DisplayValue format(UnitFormatter formatter, double value) {
    switch (this) {
      case ActivityMetric.steps:
        return DisplayValue(formatter.count(value.round()), 'steps');
      case ActivityMetric.distance:
        return formatter.distance(value);
      case ActivityMetric.caloriesOut:
      case ActivityMetric.activeCalories:
        return formatter.energy(value);
      case ActivityMetric.floors:
        return DisplayValue(formatter.count(value.round()), 'floors');
      case ActivityMetric.elevation:
        return formatter.elevation(value);
      case ActivityMetric.wheelchair:
        return DisplayValue(formatter.count(value.round()), 'pushes');
    }
  }

  /// The compact form used for the bar-chart value labels + summary.
  String formatChartValue(UnitFormatter formatter, double value) =>
      format(formatter, value).text;

  /// Resolves the movement metric a `/metric/:metricId` route argument maps to,
  /// or null when it belongs to another family (Kotlin `activityMetricFromRoute`
  /// only recognises these ids).
  static ActivityMetric? fromRouteName(String? routeName) {
    if (routeName == null) return null;
    for (final metric in values) {
      if (metric.routeName == routeName) return metric;
    }
    return null;
  }
}
