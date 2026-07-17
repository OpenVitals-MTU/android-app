import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../../../core/period/time_range.dart';
import '../../../core/presentation/display_value.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/insights/metric_interpretations.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/charts/chart_axis.dart';
import '../../../ui/charts/day_axis.dart';
import '../../../ui/charts/metric_day_chart.dart';
import '../../../ui/charts/period_chart.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/metric_interpretation_card.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/paginated_entry_list.dart';
import '../../../ui/components/swipe_to_delete_entry_row.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/nutrition_display.dart';
import 'nutrition_formatting.dart';

/// Shared building blocks for the nutrition metric-detail sections, ported from
/// the Kotlin `NutritionPeriodContent.kt` + `NutritionRows.kt`.
///
/// Every series they draw arrives precomputed on the [NutritionDisplay]; these
/// widgets format and lay out, and derive nothing.

Widget nutritionPadded(Widget child) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: child,
    );

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
  WeekPeriodMode weekPeriodMode = WeekPeriodMode.mondayToSunday,
  DateTime Function() now = DateTime.now,
}) {
  if (selectedRange == TimeRange.day) {
    return NutritionIntradayChartCard(
      day: day,
      nutrient: series.nutrient,
      samples: series.cumulativeSamples,
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

/// The day's cumulative intake curve for one nutrient.
///
/// The curve arrives precomputed (`cumulativeNutritionPoints`, built by the
/// view-model); this card only paints it.
class NutritionIntradayChartCard extends StatelessWidget {
  const NutritionIntradayChartCard({
    super.key,
    required this.day,
    required this.nutrient,
    required this.samples,
    required this.formatter,
    this.now = DateTime.now,
  });

  final LocalDate day;
  final NutritionNutrient nutrient;
  final List<DaySample> samples;
  final UnitFormatter formatter;
  final DateTime Function() now;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    DisplayValue format(double value) =>
        nutrientDisplayValue(nutrient, value, formatter);

    final total = samples.isEmpty ? 0.0 : samples.last.value;

    return MetricDayChart(
      axis: DayAxis(day, now: now()),
      samples: samples,
      shape: DaySeriesShape.cumulative,
      range: ChartRange(0, math.max(total, 1)),
      accentColor: nutrientColor(nutrient),
      metricName: nutrientTitle(nutrient, l10n),
      emptyLabel: l10n.screenNutrition,
      headlineText: format(total).text,
      valueFormatter: (value) => format(value).text,
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
///
/// An OpenVitals-authored meal swipes to delete; nutrition entries are never
/// edited (the Kotlin screens offered no edit). Foreign records stay read-only.
class NutritionEntriesContent extends StatelessWidget {
  const NutritionEntriesContent({
    super.key,
    required this.title,
    required this.entries,
    required this.formatter,
    this.onDelete,
  });

  final String title;
  final List<NutritionEntry> entries;
  final UnitFormatter formatter;
  final void Function(NutritionEntry entry)? onDelete;

  @override
  Widget build(BuildContext context) => PaginatedEntryList<NutritionEntry>(
        title: title,
        entries: entries,
        rowBuilder: (context, entry) {
          final row = NutritionEntryRow(entry: entry, formatter: formatter);
          if (onDelete == null ||
              !entry.isOpenVitalsEntry ||
              entry.id.isEmpty) {
            return row;
          }
          return SwipeToDeleteEntryRow(
            key: ValueKey('nutrition-${entry.id}'),
            onDelete: () => onDelete!(entry),
            child: row,
          );
        },
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
