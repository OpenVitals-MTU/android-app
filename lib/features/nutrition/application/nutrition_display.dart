import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/time_range.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/insights/daily_goals.dart';
import '../../../domain/insights/data_confidence.dart';
import '../../../domain/insights/metric_interpretations.dart';
import '../../../domain/insights/period_comparison.dart';
import '../../../domain/insights/personal_baseline.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../ui/charts/bar_chart.dart';
import '../../../ui/charts/metric_day_chart.dart';

part 'nutrition_display.freezed.dart';

/// The four primary overview nutrients, always surfaced first (Kotlin
/// `primaryNutritionOverviewNutrients`).
const List<NutritionNutrient> primaryNutritionOverviewNutrients = [
  NutritionNutrient.energy,
  NutritionNutrient.protein,
  NutritionNutrient.totalCarbohydrate,
  NutritionNutrient.totalFat,
];

/// One nutrient's daily series across the loaded period, with the statistics the
/// cards print (port of the Kotlin `NutritionNutrientSeries` +
/// `NutritionSeriesUiModel`). The screen used to fold the total, the average and
/// the best day out of [values] on every rebuild, per nutrient — 43 of them on
/// the overview.
@freezed
abstract class NutritionSeries with _$NutritionSeries {
  const factory NutritionSeries({
    required NutritionNutrient nutrient,
    required List<PeriodChartValue> values,
    required double total,
    required double average,
    required double best,
    required int loggedDays,
    required bool hasTrackedValues,

    /// The DAY range's cumulative intake curve for this nutrient.
    required List<DaySample> cumulativeSamples,
  }) = _NutritionSeries;
}

/// The screen-ready derivation of one loaded nutrition period, shared by the
/// keyed metric screen (calories-in / protein / carbs / fat) and the overview
/// (which reuses the calories-in notifier's state, so one display carries both).
///
/// Built once per load by [buildNutritionDisplay] and stored on the state — the
/// view-model precomputes, the screens only render (the Kotlin
/// `NutritionPresentationMapper` discipline, restored).
@freezed
abstract class NutritionDisplay with _$NutritionDisplay {
  const factory NutritionDisplay({
    required bool hasData,

    /// Kotlin gates the whole macro-derived block on `dailyMacros.isNotEmpty()`;
    /// only the ENTRIES section renders for an entries-only period.
    required bool hasMacros,

    /// The keyed metric's own series (the one the metric screen renders).
    required NutritionSeries metricSeries,
    required List<NutritionSeries> allSeries,
    required List<NutritionSeries> primarySeries,
    required List<NutritionSeries> trackedSeries,

    /// The tracked non-primary nutrients, which the overview groups by family.
    required List<NutritionSeries> additionalSeries,

    /// The same nutrients, already bucketed into the group headers the overview
    /// prints (vitamins, minerals, …) — the screen looks its group up.
    required Map<NutritionNutrientGroup, List<NutritionSeries>>
        additionalSeriesByGroup,
    required DailyGoalProgress goalProgress,
    required PeriodComparison comparison,
    PersonalBaselineInsight? baselineInsight,
    MacroSplitInterpretation? macroSplit,

    /// Data confidence over the keyed metric's tracked days…
    required DataConfidence metricConfidence,

    /// …and over every day with any nutrition at all (the overview's).
    required DataConfidence overviewConfidence,
    required List<NutritionEntry> entriesNewestFirst,

    /// Meals by day, each list newest first — the chart's pinned-day section
    /// looks its day up rather than scanning for it.
    required Map<LocalDate, List<NutritionEntry>> entriesByDay,
  }) = _NutritionDisplay;
}

/// Pure derivation from the loaded macros + entries to the display model. No
/// clock, no formatter, no l10n — the [period] and the goal arrive as arguments,
/// so this is unit-testable with fixture lists.
NutritionDisplay buildNutritionDisplay({
  required NutritionNutrient nutrient,
  required DailyGoalDirection goalDirection,
  required double dailyGoal,
  required DatePeriod period,
  required List<DailyMacros> dailyMacros,
  required List<DailyMacros> previousDailyMacros,
  required List<DailyMacros> baselineDailyMacros,
  required List<NutritionEntry> entries,
}) {
  // Sorted once, then read by every nutrient's cumulative curve.
  final entriesOldestFirst = [...entries]..sort((a, b) => a.time.compareTo(b.time));

  final allSeries = [
    for (final each in NutritionNutrient.values)
      _series(each, dailyMacros, entriesOldestFirst),
  ];
  final byNutrient = {for (final series in allSeries) series.nutrient: series};
  final primarySeries = [
    for (final each in primaryNutritionOverviewNutrients)
      if (byNutrient[each] != null) byNutrient[each]!,
  ];
  final trackedSeries =
      allSeries.where((series) => series.hasTrackedValues).toList();
  final additionalSeries = trackedSeries
      .where((series) =>
          !primaryNutritionOverviewNutrients.contains(series.nutrient))
      .toList();
  final additionalByGroup = <NutritionNutrientGroup, List<NutritionSeries>>{};
  for (final series in additionalSeries) {
    additionalByGroup
        .putIfAbsent(series.nutrient.group, () => <NutritionSeries>[])
        .add(series);
  }
  final metricSeries = byNutrient[nutrient]!;

  final previousSeries = _series(nutrient, previousDailyMacros, const []);
  final baselineSeries = _series(nutrient, baselineDailyMacros, const []);

  final entriesNewestFirst = [...entries]
    ..sort((a, b) => b.time.compareTo(a.time));
  final entriesByDay = <LocalDate, List<NutritionEntry>>{};
  for (final entry in entriesNewestFirst) {
    entriesByDay
        .putIfAbsent(instantToLocalDate(entry.time), () => <NutritionEntry>[])
        .add(entry);
  }

  final metricTrackedDates = [
    for (final value in metricSeries.values)
      if (value.value > 0.0) value.date,
  ];
  final overviewTrackedDates = [
    for (final day in dailyMacros)
      if (_hasNutritionData(day)) day.date,
  ];
  final sources = [for (final entry in entries) entry.source];

  return NutritionDisplay(
    hasData: entries.isNotEmpty ||
        dailyMacros.any((day) =>
            day.energyKcal > 0.0 ||
            day.proteinGrams > 0.0 ||
            day.carbsGrams > 0.0 ||
            day.fatGrams > 0.0 ||
            day.nutrientValues.values.any((value) => value > 0.0)),
    hasMacros: dailyMacros.isNotEmpty,
    metricSeries: metricSeries,
    allSeries: allSeries,
    primarySeries: primarySeries,
    trackedSeries: trackedSeries,
    additionalSeries: additionalSeries,
    additionalSeriesByGroup: additionalByGroup,
    goalProgress: dailyGoalProgress(
      [
        for (final value in metricSeries.values)
          DailyGoalValue(date: value.date, value: value.value),
      ],
      period,
      dailyGoal,
      goalDirection,
    ),
    comparison: periodComparison(metricSeries.total, previousSeries.total),
    baselineInsight: personalBaselineInsight(
      metricSeries.average,
      [
        for (final value in baselineSeries.values)
          BaselineValue(date: value.date, value: value.value),
      ],
      period.start.minusDays(1),
    ),
    macroSplit: macroSplitInterpretation(
      dailyMacros.fold<double>(0.0, (sum, day) => sum + day.proteinGrams),
      dailyMacros.fold<double>(0.0, (sum, day) => sum + day.carbsGrams),
      dailyMacros.fold<double>(0.0, (sum, day) => sum + day.fatGrams),
    ),
    metricConfidence: dataConfidence(
      period,
      metricTrackedDates,
      entries.isNotEmpty ? entries.length : metricTrackedDates.length,
      sources: sources,
      valueKind: DataValueKind.aggregated,
    ),
    overviewConfidence: dataConfidence(
      period,
      overviewTrackedDates,
      entries.isNotEmpty ? entries.length : overviewTrackedDates.length,
      sources: sources,
      valueKind: DataValueKind.aggregated,
    ),
    entriesNewestFirst: entriesNewestFirst,
    entriesByDay: entriesByDay,
  );
}

/// One nutrient's daily values, and the statistics the cards fold out of them.
NutritionSeries _series(
  NutritionNutrient nutrient,
  List<DailyMacros> macros,
  List<NutritionEntry> entriesOldestFirst,
) {
  final values = [
    for (final day in macros) PeriodChartValue(day.date, day.valueFor(nutrient)),
  ];
  final raw = [for (final value in values) value.value];
  final total = raw.fold(0.0, (sum, value) => sum + value);
  final loggedDays = raw.where((value) => value > 0.0).length;
  return NutritionSeries(
    nutrient: nutrient,
    values: values,
    total: total,
    average: loggedDays > 0 ? total / loggedDays : 0.0,
    best: raw.fold(0.0, (m, value) => math.max(m, value)),
    loggedDays: loggedDays,
    hasTrackedValues: raw.any((value) => value > 0.0),
    cumulativeSamples: _cumulativePoints(entriesOldestFirst, nutrient),
  );
}

/// Kotlin `List<NutritionEntry>.cumulativeNutritionPoints`: entries sorted by
/// time, zero/absent readings dropped, values accumulated.
List<DaySample> cumulativeNutritionPoints(
  List<NutritionEntry> entries,
  NutritionNutrient nutrient,
) =>
    _cumulativePoints(
      [...entries]..sort((a, b) => a.time.compareTo(b.time)),
      nutrient,
    );

List<DaySample> _cumulativePoints(
  List<NutritionEntry> entriesOldestFirst,
  NutritionNutrient nutrient,
) {
  var cumulative = 0.0;
  final points = <DaySample>[];
  for (final entry in entriesOldestFirst) {
    // Kotlin: `entry.valueFor(nutrient)?.takeIf { it > 0.0 } ?: return@mapNotNull null`
    final value = entry.valueFor(nutrient);
    if (value == null || value <= 0.0) continue;
    cumulative += value;
    points.add((time: entry.time, value: cumulative));
  }
  return points;
}

/// Kotlin `DailyMacros.hasNutritionData()`.
bool _hasNutritionData(DailyMacros day) =>
    day.nutrientValues.values.any((value) => value > 0.0) ||
    day.energyKcal > 0.0 ||
    day.proteinGrams > 0.0 ||
    day.carbsGrams > 0.0 ||
    day.fatGrams > 0.0;
