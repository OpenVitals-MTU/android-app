import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../ui/charts/chart_skeleton.dart';
import '../../../ui/theme/chart_tokens.dart';

import '../../../core/period/period_range_preference_key.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/metric_detail_sections.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/preferences/metric_detail_section_id.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/data_confidence_card.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/insight_cards.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/metric_detail_scaffold.dart';
import '../../../ui/theme/app_colors.dart';
import '../../../domain/health/health_permissions.dart';
import 'nutrition_formatting.dart';
import 'nutrition_metric.dart';
import '../application/nutrition_display.dart';
import '../application/nutrition_view_model.dart';
import 'nutrition_sections.dart';

/// The nutrition overview / nutrient-breakdown screen, ported from the Kotlin
/// `NutritionScreen` + `nutritionContent`. Reuses the [NutritionMetric.caloriesIn]
/// notifier for its data and renders user-reorderable ordered sections: the
/// all-nutrient statistics (primary macros + grouped totals), one trend chart
/// per tracked nutrient, the pinned day's meals, data confidence, the macro-split
/// context, and every logged meal.
class NutritionScreen extends ConsumerWidget {
  const NutritionScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final provider = nutritionMetricProvider(NutritionMetric.caloriesIn);
    final state = ref.watch(provider);
    final notifier = ref.read(provider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);
    final isEditingSections = ref.watch(metricDetailSectionEditProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.screenNutrition),
        actions: [
          IconButton(
            onPressed:
                ref.read(metricDetailSectionEditProvider.notifier).toggle,
            tooltip: isEditingSections
                ? l10n.cdFinishMetricSectionEditing
                : l10n.cdEditMetricSections,
            icon: Icon(isEditingSections ? Icons.check : Icons.tune),
          ),
        ],
      ),
      body: HealthConnectGate(
        requiredPermissions: {HcPermissions.readNutrition},
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.nutrition,
          onRefresh: notifier.refresh,
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: notifier.load,
          content: (period) => [
            _NutritionOverviewContent(
              state: state,
              period: period,
              formatter: formatter,
              weekPeriodMode: weekMode,
              onDeleteEntry: notifier.deleteNutritionEntry,
            ),
          ],
        ),
      ),
    );
  }
}

class _NutritionOverviewContent extends StatelessWidget {
  const _NutritionOverviewContent({
    required this.state,
    required this.period,
    required this.formatter,
    required this.weekPeriodMode,
    required this.onDeleteEntry,
  });

  final NutritionState state;
  final DatePeriod period;
  final UnitFormatter formatter;
  final WeekPeriodMode weekPeriodMode;
  final void Function(String entryId) onDeleteEntry;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final display = state.display;
    if (display == null || !display.hasData) {
      if (state.isLoading) {
        return const ChartSkeleton(shape: ChartSkeletonShape.bars, height: kChartHeightPeriodBar);
      }
      return _placeholder(l10n);
    }

    return ChartDaySelectionScope(
      selectedRange: state.selectedRange,
      selectedDate: state.selectedDate,
      builder: (context, daySelection) =>
          _sections(context, l10n, display, daySelection),
    );
  }

  Widget _placeholder(AppLocalizations l10n) => OrderedMetricDetailSections(
        sections: [
          MetricDetailSection(
            MetricDetailSectionId.activitySummary,
            nutritionPadded(MetricCardPlaceholder(
              title: l10n.screenNutrition,
              icon: Icons.restaurant_outlined,
              accentColor: AppColors.nutrition,
              message: l10n.messageNoNutritionPeriod,
            )),
          ),
        ],
      );

  Widget _sections(
    BuildContext context,
    AppLocalizations l10n,
    NutritionDisplay display,
    ChartDaySelection daySelection,
  ) {
    final macroSplit = display.macroSplit;
    final selectedDay = daySelection.selectedDate;
    final selectedEntries = selectedDay == null
        ? const <NutritionEntry>[]
        : (display.entriesByDay[selectedDay] ?? const <NutritionEntry>[]);
    final hasMacros = display.hasMacros;

    return OrderedMetricDetailSections(
      sections: [
        // Kotlin section ACTIVITY_SUMMARY: the all-nutrient statistics — primary
        // macros first, then each nutrient group's tracked totals.
        MetricDetailSection(
          MetricDetailSectionId.activitySummary,
          visible: hasMacros &&
              (display.primarySeries.isNotEmpty ||
                  display.additionalSeries.isNotEmpty),
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (display.primarySeries.isNotEmpty) ...[
                nutritionPadded(SectionHeader(l10n.sectionStatistics)),
                nutritionPadded(_totalsGrid(display.primarySeries, l10n)),
              ],
              for (final group in NutritionNutrientGroup.values)
                if (group != NutritionNutrientGroup.overview)
                  ..._groupTotals(
                    group,
                    display.additionalSeriesByGroup[group] ??
                        const <NutritionSeries>[],
                    l10n,
                  ),
            ],
          ),
        ),
        // Kotlin section PERIOD_CHART: one trend chart per tracked nutrient.
        MetricDetailSection(
          MetricDetailSectionId.periodChart,
          visible: hasMacros && display.trackedSeries.isNotEmpty,
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              nutritionPadded(SectionHeader(l10n.sectionNutritionTrends)),
              for (final series in display.trackedSeries)
                nutritionPadded(nutritionTrendChart(
                  series: series,
                  selectedRange: state.selectedRange,
                  period: period,
                  formatter: formatter,
                  l10n: l10n,
                  selectedDate: selectedDay,
                  onDateSelected: daySelection.onDateSelected,
                  day: state.selectedDate,
                  weekPeriodMode: weekPeriodMode,
                )),
            ],
          ),
        ),
        // Kotlin section SELECTED_DAY_ENTRIES.
        MetricDetailSection(
          MetricDetailSectionId.selectedDayEntries,
          visible: hasMacros && selectedDay != null && selectedEntries.isNotEmpty,
          nutritionPadded(NutritionEntriesContent(
            title: nutritionEntryListTitle(
              selectedDay,
              Localizations.localeOf(context).toLanguageTag(),
              l10n,
            ),
            entries: selectedEntries,
            formatter: formatter,
            onDelete: (entry) => onDeleteEntry(entry.id),
          )),
        ),
        // Kotlin section DATA_CONFIDENCE (hidden on a single-day period).
        MetricDetailSection(
          MetricDetailSectionId.dataConfidence,
          visible: hasMacros && period.start != period.end,
          nutritionPadded(DataConfidenceCard(
            confidence: display.overviewConfidence,
            accentColor: AppColors.nutrition,
          )),
        ),
        // Kotlin section METRIC_CONTEXT: the macro-split interpretation.
        MetricDetailSection(
          MetricDetailSectionId.metricContext,
          visible: macroSplit != null,
          macroSplit == null
              ? const SizedBox.shrink()
              : nutritionMacroSplitContext(
                  context: context,
                  split: macroSplit,
                  formatter: formatter,
                  accentColor: AppColors.nutrition,
                ),
        ),
        // Kotlin section ENTRIES: every logged meal, newest first.
        MetricDetailSection(
          MetricDetailSectionId.entries,
          visible: display.entriesNewestFirst.isNotEmpty,
          nutritionPadded(NutritionEntriesContent(
            title: l10n.sectionMeals,
            entries: display.entriesNewestFirst,
            formatter: formatter,
            onDelete: (entry) => onDeleteEntry(entry.id),
          )),
        ),
      ],
    );
  }

  List<Widget> _groupTotals(
    NutritionNutrientGroup group,
    List<NutritionSeries> groupMetrics,
    AppLocalizations l10n,
  ) {
    if (groupMetrics.isEmpty) return const [];
    return [
      nutritionPadded(SectionHeader(_groupTitle(group, l10n))),
      nutritionPadded(_totalsGrid(groupMetrics, l10n)),
    ];
  }

  Widget _totalsGrid(List<NutritionSeries> seriesList, AppLocalizations l10n) =>
      InsightStatGrid(
        stats: [
          for (final series in seriesList)
            () {
              final total = nutrientDisplayValue(
                  series.nutrient, series.total, formatter);
              return InsightStat(
                title: nutrientTitle(series.nutrient, l10n),
                value: total.value,
                unit: total.unit,
                icon: Icons.restaurant_outlined,
                accentColor: nutrientColor(series.nutrient),
              );
            }(),
        ],
      );
}

/// Kotlin `NutritionNutrientGroup.titleRes()` — the non-overview group headers.
String _groupTitle(NutritionNutrientGroup group, AppLocalizations l10n) =>
    switch (group) {
      NutritionNutrientGroup.overview => l10n.screenNutrition,
      NutritionNutrientGroup.carbohydrates => l10n.sectionCarbohydrates,
      NutritionNutrientGroup.fats => l10n.sectionFats,
      NutritionNutrientGroup.vitamins => l10n.sectionVitamins,
      NutritionNutrientGroup.minerals => l10n.sectionMinerals,
      NutritionNutrientGroup.other => l10n.sectionOtherNutrients,
    };
