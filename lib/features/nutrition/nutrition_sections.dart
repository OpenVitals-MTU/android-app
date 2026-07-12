import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../core/period/time_range.dart';
import '../../core/presentation/display_value.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../core/time/local_date.dart';
import '../../domain/insights/metric_interpretations.dart';
import '../../domain/model/nutrition_models.dart';
import '../../l10n/app_localizations.dart';
import '../../ui/charts/day_axis.dart';
import '../../ui/charts/metric_line_plot.dart';
import '../../ui/charts/period_chart.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/metric_interpretation_card.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/components/paginated_entry_list.dart';
import '../../ui/theme/app_colors.dart';
import 'nutrition_formatting.dart';

/// Shared building blocks for the nutrition metric-detail sections, ported from
/// the Kotlin `NutritionPeriodContent.kt` + `NutritionRows.kt`.

Widget nutritionPadded(Widget child) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: child,
    );

/// Per-nutrient daily series across a loaded period (port of the Kotlin
/// `NutritionNutrientSeries` + `NutritionSeriesUiModel`).
class NutritionSeries {
  NutritionSeries(this.nutrient, this.values);

  final NutritionNutrient nutrient;
  final List<PeriodChartValue> values;

  List<double> get _raw => [for (final value in values) value.value];
  double get total => _raw.fold(0.0, (sum, value) => sum + value);
  bool get hasTrackedValues => _raw.any((value) => value > 0.0);
  int get loggedDays => _raw.where((value) => value > 0.0).length;
  double get average => loggedDays > 0 ? total / loggedDays : 0.0;
  double get best => _raw.fold(0.0, (m, value) => math.max(m, value));
}

NutritionSeries nutritionSeriesFor(
  List<DailyMacros> macros,
  NutritionNutrient nutrient,
) =>
    NutritionSeries(nutrient, [
      for (final day in macros)
        PeriodChartValue(day.date, day.valueFor(nutrient)),
    ]);

/// The four primary overview nutrients, always surfaced first (Kotlin
/// `primaryNutritionOverviewNutrients`).
const List<NutritionNutrient> primaryNutritionOverviewNutrients = [
  NutritionNutrient.energy,
  NutritionNutrient.protein,
  NutritionNutrient.totalCarbohydrate,
  NutritionNutrient.totalFat,
];

/// Kotlin `NutritionMetricTrendContent`: on the DAY range the nutrient's
/// cumulative intake curve, otherwise the daily bar chart across the period.
/// Both the overview and the per-metric screen route through here, as Kotlin's
/// two call sites do.
Widget nutritionTrendChart({
  required NutritionSeries series,
  required TimeRange selectedRange,
  required DatePeriod period,
  required UnitFormatter formatter,
  required AppLocalizations l10n,
  required LocalDate? selectedDate,
  required ValueChanged<LocalDate> onDateSelected,
  required LocalDate day,
  required List<NutritionEntry> entries,
  WeekPeriodMode weekPeriodMode = WeekPeriodMode.mondayToSunday,
  DateTime Function() now = DateTime.now,
}) {
  if (selectedRange == TimeRange.day) {
    return NutritionIntradayChartCard(
      day: day,
      series: series,
      entries: entries,
      formatter: formatter,
      now: now,
    );
  }
  final color = nutrientColor(series.nutrient);
  DisplayValue format(double value) =>
      nutrientDisplayValue(series.nutrient, value, formatter);
  return MetricBarChart(
    title: nutrientTitle(series.nutrient, l10n),
    values: series.values,
    selectedRange: selectedRange,
    period: period,
    accentColor: color,
    summaryValue: format(series.total).text,
    weekPeriodMode: weekPeriodMode,
    selectedDate: selectedDate,
    onDateSelected: onDateSelected,
    valueFormatter: (value) => format(value).text,
  );
}

/// Kotlin `List<NutritionEntry>.cumulativeNutritionPoints`: entries sorted by
/// time, zero/absent readings dropped, values accumulated.
List<({DateTime time, double value})> cumulativeNutritionPoints(
  List<NutritionEntry> entries,
  NutritionNutrient nutrient,
) {
  final sorted = [...entries]..sort((a, b) => a.time.compareTo(b.time));
  var cumulative = 0.0;
  final points = <({DateTime time, double value})>[];
  for (final entry in sorted) {
    // Kotlin: `entry.valueFor(nutrient)?.takeIf { it > 0.0 } ?: return@mapNotNull null`
    final value = entry.valueFor(nutrient);
    if (value == null || value <= 0.0) continue;
    cumulative += value;
    points.add((time: entry.time, value: cumulative));
  }
  return points;
}

/// Kotlin `NutritionIntradayChartCard`: the day's cumulative intake curve,
/// anchored at midnight and running to "now" on today (else to midnight).
class NutritionIntradayChartCard extends StatelessWidget {
  const NutritionIntradayChartCard({
    super.key,
    required this.day,
    required this.series,
    required this.entries,
    required this.formatter,
    this.now = DateTime.now,
  });

  final LocalDate day;
  final NutritionSeries series;
  final List<NutritionEntry> entries;
  final UnitFormatter formatter;
  final DateTime Function() now;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final color = nutrientColor(series.nutrient);
    final title = nutrientTitle(series.nutrient, l10n);
    DisplayValue format(double value) =>
        nutrientDisplayValue(series.nutrient, value, formatter);

    // Shared, because this maths was written five times over and four of them
    // plotted the point at the wrong hour. See [DayAxis].
    final axis = DayAxis(day, now: now());
    final isToday = axis.isToday;
    final dayStart = axis.start;

    final points = cumulativeNutritionPoints(entries, series.nutrient);
    final total = points.isEmpty ? 0.0 : points.last.value;

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              format(total).text,
              style: theme.textTheme.headlineMedium?.copyWith(color: color),
            ),
            Text(
              isToday
                  ? l10n.summaryToday(title)
                  : l10n.summaryOnDate(
                      title,
                      DateFormat.yMMMd().format(dayStart),
                    ),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
            const SizedBox(height: 16),
            if (points.isEmpty)
              Text(
                isToday
                    ? l10n.summaryEmptyToday(l10n.screenNutrition)
                    : l10n.summaryEmptyDay(l10n.screenNutrition),
                style: theme.textTheme.bodyMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              )
            else ...[
              MetricLinePlot(
                // Bracketed: zero at midnight, and the total held forward to the
                // end of the axis.
                points: [
                  const MetricLinePlotPoint(xFraction: 0, value: 0),
                  for (final point in points)
                    MetricLinePlotPoint(
                      xFraction: axis.fractionOf(point.time),
                      value: point.value,
                    ),
                  // To now, not to the right edge: the rest of today has not been
                  // eaten yet.
                  MetricLinePlotPoint(xFraction: axis.endFraction, value: total),
                ],
                minValue: 0,
                maxValue: math.max(total, 1),
                accentColor: color,
                valueFormatter: (value) => format(value).text,
              ),
              const SizedBox(height: 8),
              DayAxisLabels(axis: axis),
              const SizedBox(height: 12),
              Text(
                l10n.summaryLastUpdate(
                  DateFormat.jm().format(points.last.time),
                ),
                style: theme.textTheme.bodySmall
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ],
          ],
        ),
      ),
    );
  }
}

/// Kotlin `MacroSplitContextContent`: the METRIC_CONTEXT section body.
Widget nutritionMacroSplitContext({
  required BuildContext context,
  required MacroSplitInterpretation split,
  required UnitFormatter formatter,
  required Color accentColor,
}) {
  final l10n = AppLocalizations.of(context);
  return Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      nutritionPadded(SectionHeader(l10n.sectionMetricContext)),
      nutritionPadded(MetricInterpretationCard(
        title: l10n.interpretationMacroTitle,
        status: split.isWithinReference
            ? l10n.interpretationMacroWithin
            : l10n.interpretationMacroOutside,
        body: l10n.interpretationMacroBody(
          formatter.percent(split.proteinPercent, decimals: 0).text,
          formatter.percent(split.carbsPercent, decimals: 0).text,
          formatter.percent(split.fatPercent, decimals: 0).text,
        ),
        source: l10n.interpretationMacroSource,
        icon: Icons.restaurant_outlined,
        accentColor: accentColor,
        severity: split.severity,
      )),
    ],
  );
}

/// Kotlin `entryListTitle`: the selected-day list is titled by its date; the
/// aggregate meals list uses the generic section title.
String nutritionEntryListTitle(
  LocalDate? titleDate,
  String locale,
  AppLocalizations l10n,
) {
  if (titleDate == null) return l10n.sectionMeals;
  return DateFormat.yMMMd(locale)
      .format(DateTime(titleDate.year, titleDate.month, titleDate.day));
}

/// Kotlin `NutritionEntriesContent`: a paginated list of meal rows.
class NutritionEntriesContent extends StatelessWidget {
  const NutritionEntriesContent({
    super.key,
    required this.title,
    required this.entries,
    required this.formatter,
  });

  final String title;
  final List<NutritionEntry> entries;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) => PaginatedEntryList<NutritionEntry>(
        title: title,
        entries: entries,
        rowBuilder: (context, entry) =>
            NutritionEntryRow(entry: entry, formatter: formatter),
      );
}

/// Kotlin `NutritionEntryRow`: a logged meal — name/meal-type, timestamp, macro
/// line, energy and source. Read-only (nutrition entries cannot be edited or
/// deleted from these screens).
class NutritionEntryRow extends StatelessWidget {
  const NutritionEntryRow({
    super.key,
    required this.entry,
    required this.formatter,
  });

  final NutritionEntry entry;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final locale = Localizations.localeOf(context).toLanguageTag();
    final time = entry.time;
    final dateText = DateFormat.yMMMd(locale).format(time);
    final timeText = DateFormat.jm(locale).format(time);

    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            const Icon(Icons.restaurant_outlined, color: AppColors.nutrition),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    entry.name ?? _mealTypeLabel(entry.mealType, l10n),
                    style: theme.textTheme.titleSmall,
                  ),
                  Text(
                    '$dateText  ·  $timeText',
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                  Text(
                    _macroLine(entry, formatter, l10n),
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  entry.energyKcal != null
                      ? formatter.energy(entry.energyKcal!).text
                      : l10n.messageNoKcal,
                  style: theme.textTheme.labelLarge
                      ?.copyWith(color: AppColors.calories),
                ),
                const SizedBox(height: 4),
                SourceChip(source: entry.source),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

String _macroLine(
  NutritionEntry entry,
  UnitFormatter formatter,
  AppLocalizations l10n,
) {
  final parts = <String>[
    if (entry.proteinGrams != null)
      l10n.macroProteinShort(formatter.count(entry.proteinGrams!.round())),
    if (entry.carbsGrams != null)
      l10n.macroCarbsShort(formatter.count(entry.carbsGrams!.round())),
    if (entry.fatGrams != null)
      l10n.macroFatShort(formatter.count(entry.fatGrams!.round())),
    if (entry.fiberGrams != null)
      l10n.macroFiber(formatter.count(entry.fiberGrams!.round())),
    if (entry.sugarGrams != null)
      l10n.macroSugar(formatter.count(entry.sugarGrams!.round())),
  ];
  if (parts.isEmpty) return _mealTypeLabel(entry.mealType, l10n);
  return parts.join(' · ');
}

/// Health Connect `MealType` constants (breakfast/lunch/dinner/snack).
String _mealTypeLabel(int mealType, AppLocalizations l10n) => switch (mealType) {
      1 => l10n.mealBreakfast,
      2 => l10n.mealLunch,
      3 => l10n.mealDinner,
      4 => l10n.mealSnack,
      _ => l10n.mealGeneric,
    };

/// Meal entries falling on [day], newest first.
List<NutritionEntry> nutritionEntriesForDay(
  List<NutritionEntry> entries,
  LocalDate day,
) {
  final matching = [
    for (final entry in entries)
      if (instantToLocalDate(entry.time) == day) entry,
  ]..sort((a, b) => b.time.compareTo(a.time));
  return matching;
}
