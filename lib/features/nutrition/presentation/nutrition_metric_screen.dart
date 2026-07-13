import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../ui/components/loading_state.dart';

import '../../../core/period/period_range_preference_key.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/display_value.dart';
import '../../../core/presentation/metric_detail_sections.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/preferences/metric_detail_section_id.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/daily_goal_components.dart';
import '../../../ui/components/data_confidence_card.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/insight_cards.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/metric_detail_scaffold.dart';
import '../../../ui/components/period_comparison_stat.dart';
import '../../../ui/components/personal_baseline_stat.dart';
import '../../../domain/health/health_permissions.dart';
import 'nutrition_formatting.dart';
import 'nutrition_metric.dart';
import '../application/nutrition_display.dart';
import '../application/nutrition_view_model.dart';
import 'nutrition_sections.dart';

/// The shared period-detail screen for the four keyed nutrition metrics
/// (calories-in / protein / carbs / fat), ported from the Kotlin
/// `NutritionMetricScreen` + `nutritionMetricContent`. Every metric renders the
/// same user-reorderable ordered sections; only the accent, goal key and value
/// formatting differ.
class NutritionMetricScreen extends ConsumerWidget {
  const NutritionMetricScreen({super.key, required this.metric});

  final NutritionMetric metric;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final provider = nutritionMetricProvider(metric);
    final state = ref.watch(provider);
    final notifier = ref.read(provider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);
    final isEditingSections = ref.watch(metricDetailSectionEditProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(metric.title(l10n)),
        actions: [
          // Kotlin hoists this toggle into the host app bar through
          // `onSectionEditStateChanged`; the same affordance, wired locally.
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
            _NutritionMetricContent(
              metric: metric,
              state: state,
              period: period,
              formatter: formatter,
              weekPeriodMode: weekMode,
              onDecreaseGoal: notifier.decreaseDailyGoal,
              onIncreaseGoal: notifier.increaseDailyGoal,
            ),
          ],
        ),
      ),
    );
  }
}

class _NutritionMetricContent extends StatelessWidget {
  const _NutritionMetricContent({
    required this.metric,
    required this.state,
    required this.period,
    required this.formatter,
    required this.weekPeriodMode,
    required this.onDecreaseGoal,
    required this.onIncreaseGoal,
  });

  final NutritionMetric metric;
  final NutritionState state;
  final DatePeriod period;
  final UnitFormatter formatter;
  final WeekPeriodMode weekPeriodMode;
  final VoidCallback onDecreaseGoal;
  final VoidCallback onIncreaseGoal;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final display = state.display;
    if (display == null || !display.hasData) {
      if (state.isLoading) {
        return const SectionLoading();
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
              title: metric.title(l10n),
              icon: metric.icon,
              accentColor: metric.accentColor,
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
    final nutrient = metric.nutrient;
    final color = metric.accentColor;
    final series = display.metricSeries;

    DisplayValue format(double value) =>
        nutrientDisplayValue(nutrient, value, formatter);

    final goalProgress = display.goalProgress;
    final comparison = display.comparison;
    final macroSplit = display.macroSplit;

    final selectedDay = daySelection.selectedDate;
    final selectedEntries = selectedDay == null
        ? const <NutritionEntry>[]
        : (display.entriesByDay[selectedDay] ?? const <NutritionEntry>[]);
    final hasMacros = display.hasMacros;

    return OrderedMetricDetailSections(
      sections: [
        // Kotlin section ACTIVITY_SUMMARY: the hero total card.
        MetricDetailSection(
          MetricDetailSectionId.activitySummary,
          visible: hasMacros,
          nutritionPadded(MetricCard(
            title: metric.title(l10n),
            value: format(series.total).value,
            unit: format(series.total).unit,
            icon: metric.icon,
            accentColor: color,
            subtitle: display.entriesNewestFirst.isNotEmpty
                ? l10n.summaryEntries(
                    formatter.count(display.entriesNewestFirst.length))
                : l10n.summaryAcrossSelectedPeriod,
          )),
        ),
        // Kotlin section PERIOD_CHART: the metric's daily trend.
        MetricDetailSection(
          MetricDetailSectionId.periodChart,
          visible: hasMacros && series.values.isNotEmpty,
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
        ),
        // Kotlin section SELECTED_DAY_ENTRIES: meals on the pinned chart day.
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
          )),
        ),
        // Kotlin section DATA_CONFIDENCE (hidden on a single-day period).
        MetricDetailSection(
          MetricDetailSectionId.dataConfidence,
          visible: hasMacros && period.start != period.end,
          nutritionPadded(DataConfidenceCard(
            confidence: display.metricConfidence,
            accentColor: color,
          )),
        ),
        // Kotlin section DAILY_GOAL.
        MetricDetailSection(
          MetricDetailSectionId.dailyGoal,
          visible: hasMacros,
          nutritionPadded(DailyGoalCard(
            goal: format(state.dailyGoal),
            progress: goalProgress,
            icon: metric.icon,
            accentColor: color,
            onDecreaseGoal: onDecreaseGoal,
            onIncreaseGoal: onIncreaseGoal,
          )),
        ),
        // Kotlin section STATISTICS.
        MetricDetailSection(
          MetricDetailSectionId.statistics,
          visible: hasMacros,
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              nutritionPadded(SectionHeader(l10n.sectionStatistics)),
              nutritionPadded(DailyGoalStatistics(
                progress: goalProgress,
                averageGap: format(goalProgress.averageGapToGoal),
                unitFormatter: formatter,
                icon: metric.icon,
                accentColor: color,
              )),
              nutritionPadded(InsightStatGrid(
                stats: [
                  InsightStat(
                    title: l10n.statTotal,
                    value: format(series.total).value,
                    unit: format(series.total).unit,
                    icon: metric.icon,
                    accentColor: color,
                  ),
                  InsightStat(
                    title: l10n.statDailyAverage,
                    value: format(series.average).value,
                    unit: format(series.average).unit,
                    icon: Icons.star_outline,
                    accentColor: color,
                  ),
                  InsightStat(
                    title: l10n.statBestDay,
                    value: format(series.best).value,
                    unit: format(series.best).unit,
                    icon: Icons.calendar_month_outlined,
                    accentColor: color,
                  ),
                  InsightStat(
                    title: l10n.metricLoggedDays,
                    value: formatter.count(series.loggedDays),
                    unit: l10n.unitDays,
                    icon: Icons.check_circle_outline,
                    accentColor: color,
                  ),
                  previousPeriodInsightStat(
                    comparison: comparison,
                    selectedRange: state.selectedRange,
                    unitFormatter: formatter,
                    valueFormatter: format,
                    accentColor: color,
                    l10n: l10n,
                  ),
                  ...personalBaselineInsightStats(
                    insight: display.baselineInsight,
                    unitFormatter: formatter,
                    valueFormatter: format,
                    accentColor: color,
                    l10n: l10n,
                  ),
                ],
              )),
            ],
          ),
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
                  accentColor: color,
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
          )),
        ),
      ],
    );
  }
}

// ── Route-facing per-metric wrappers (Kotlin `CaloriesInScreen`, …). ──────────

class CaloriesInScreen extends StatelessWidget {
  const CaloriesInScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const NutritionMetricScreen(metric: NutritionMetric.caloriesIn);
}

class ProteinScreen extends StatelessWidget {
  const ProteinScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const NutritionMetricScreen(metric: NutritionMetric.protein);
}

class CarbsScreen extends StatelessWidget {
  const CarbsScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const NutritionMetricScreen(metric: NutritionMetric.carbs);
}

class FatScreen extends StatelessWidget {
  const FatScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const NutritionMetricScreen(metric: NutritionMetric.fat);
}
