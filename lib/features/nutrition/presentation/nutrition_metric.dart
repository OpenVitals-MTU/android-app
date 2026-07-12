import 'package:flutter/material.dart';

import '../../../domain/insights/daily_goals.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../l10n/app_localizations.dart';
import 'nutrition_formatting.dart';

/// The four keyed nutrition metrics surfaced by the shared metric-detail screen,
/// ported from the Kotlin `NutritionMetric` enum. Each constant maps to a
/// [NutritionNutrient] series, its `/metric/:metricId` route id, and its daily
/// goal key (Kotlin `NutritionMetric.nutrient` / `dailyGoalKey`).
enum NutritionMetric {
  caloriesIn(
    'CALORIES_IN',
    NutritionNutrient.energy,
    MetricDailyGoalKey.caloriesInKcal,
  ),
  protein(
    'PROTEIN',
    NutritionNutrient.protein,
    MetricDailyGoalKey.proteinGrams,
  ),
  carbs(
    'CARBS',
    NutritionNutrient.totalCarbohydrate,
    MetricDailyGoalKey.carbsGrams,
  ),
  fat(
    'FAT',
    NutritionNutrient.totalFat,
    MetricDailyGoalKey.fatGrams,
  );

  const NutritionMetric(this.routeName, this.nutrient, this.dailyGoalKey);

  /// The `DashboardMetricId.storageName` (SCREAMING_SNAKE_CASE) this metric is
  /// reached through on the `/metric/:metricId` route.
  final String routeName;
  final NutritionNutrient nutrient;
  final MetricDailyGoalKey dailyGoalKey;

  /// Localized display title. Takes [l10n] because the nutrient names come from
  /// the shared `metric_*` strings (Kotlin `titleRes`).
  String title(AppLocalizations l10n) => nutrientTitle(nutrient, l10n);

  Color get accentColor => nutrientColor(nutrient);

  IconData get icon => Icons.restaurant_outlined;

  /// Resolves the nutrition metric a `/metric/:metricId` route argument maps to,
  /// or null when it belongs to another family (Kotlin `nutritionMetricFromRoute`
  /// recognises only these four ids).
  static NutritionMetric? fromRouteName(String? routeName) {
    if (routeName == null) return null;
    for (final metric in values) {
      if (metric.routeName == routeName) return metric;
    }
    return null;
  }
}
