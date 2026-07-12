import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/period/period_range_preference_key.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/display_value.dart';
import '../../../core/presentation/metric_detail_sections.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/insights/data_confidence.dart';
import '../../../domain/insights/personal_baseline.dart';
import '../../../domain/preferences/metric_detail_section_id.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/charts/period_chart.dart';
import '../../../ui/components/daily_goal_components.dart';
import '../../../ui/components/data_confidence_card.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/insight_cards.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/metric_detail_scaffold.dart';
import '../../../ui/components/period_comparison_stat.dart';
import '../../../ui/components/personal_baseline_stat.dart';
import 'activity_daily_entries.dart';
import 'activity_intraday_chart_card.dart';
import 'activity_metric.dart';
import 'activity_metric_display.dart';
import '../application/activity_metric_notifier.dart';
import '../../../ui/components/section_padding.dart';

/// The shared period-detail screen for the seven movement metrics, a port of the
/// Kotlin `ActivityMetricScreen` (`ActivityScreen.kt`) and the per-metric
/// `*Content` functions in `ActivityMetricContent.kt`.
///
/// Every metric renders the same ordered sections; only the accent, the goal key
/// and the value formatting differ, all of which live on [ActivityMetric].
class ActivityMetricScreen extends ConsumerWidget {
  const ActivityMetricScreen({super.key, required this.metric});

  final ActivityMetric metric;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final provider = activityMetricProvider(metric);
    final state = ref.watch(provider);
    final notifier = ref.read(provider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);
    final isEditingSections = ref.watch(metricDetailSectionEditProvider);

    return Scaffold(
      appBar: AppBar(
        title: Text(metric.title),
        actions: [
          // Kotlin hoists this toggle into the host app bar through
          // `onSectionEditStateChanged`; the same affordance, wired locally.
          IconButton(
            onPressed:
                ref.read(metricDetailSectionEditProvider.notifier).toggle,
            tooltip: isEditingSections
                ? AppLocalizations.of(context).cdFinishMetricSectionEditing
                : AppLocalizations.of(context).cdEditMetricSections,
            icon: Icon(isEditingSections ? Icons.check : Icons.tune),
          ),
        ],
      ),
      body: HealthConnectGate(
        requiredPermissions: {metric.readPermission},
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          // The Kotlin `ActivityViewModel` keys every movement metric's
          // remembered range on `PeriodRangePreferenceKey.STEPS`.
          rangePreferenceKey: PeriodRangePreferenceKey.steps,
          onRefresh: notifier.refresh,
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: notifier.load,
          content: (period) => [
            _ActivityMetricContent(
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

class _ActivityMetricContent extends StatelessWidget {
  const _ActivityMetricContent({
    required this.metric,
    required this.state,
    required this.period,
    required this.formatter,
    required this.weekPeriodMode,
    required this.onDecreaseGoal,
    required this.onIncreaseGoal,
  });

  final ActivityMetric metric;
  final ActivityMetricState state;
  final DatePeriod period;
  final UnitFormatter formatter;
  final WeekPeriodMode weekPeriodMode;
  final VoidCallback onDecreaseGoal;
  final VoidCallback onIncreaseGoal;

  @override
  Widget build(BuildContext context) {
    final data = state.data;
    if (data == null) {
      if (state.isLoading) {
        return const Padding(
          padding: EdgeInsets.symmetric(vertical: 48),
          child: Center(child: CircularProgressIndicator()),
        );
      }
      return _placeholder();
    }

    final display = activityMetricDisplay(
      metric: metric,
      data: data,
      range: state.selectedRange,
      period: period,
      dailyGoal: state.dailyGoal,
    );
    // Kotlin: `if (display.hasData) ... else if (!isLoading) noMetricData(...)`.
    if (!display.hasData) return _placeholder();

    return ChartDaySelectionScope(
      selectedRange: state.selectedRange,
      selectedDate: state.selectedDate,
      builder: (context, daySelection) => _sections(context, display, daySelection),
    );
  }

  Widget _placeholder() => sectionPadded(
        MetricCardPlaceholder(
          title: metric.title,
          icon: metric.icon,
          accentColor: metric.accentColor,
          message: metric.emptyMessage,
        ),
      );

  Widget _sections(
    BuildContext context,
    ActivityMetricDisplay display,
    ChartDaySelection daySelection,
  ) {
    final l10n = AppLocalizations.of(context);
    final goalProgress = display.goalProgress;
    final comparison = display.periodComparison;
    // Kotlin bails out of the whole ordered block when either is absent.
    if (goalProgress == null || comparison == null) return const SizedBox.shrink();

    final isDay = state.selectedRange == TimeRange.day;
    final selectedDay = daySelection.selectedDate;
    DisplayValue goalFormatter(double value) => metric.format(formatter, value);

    final entries = [
      for (var i = 0; i < display.goalValues.length; i++)
        if (display.goalValues[i].value > 0.0)
          ActivityDailyEntry(
            date: display.goalValues[i].date,
            value: goalFormatter(display.goalValues[i].value),
          ),
    ];

    return OrderedMetricDetailSections(
      sections: [
        MetricDetailSection(
          MetricDetailSectionId.intradayChart,
          visible: isDay,
          sectionPadded(IntradayActivityChartCard(
            selectedDate: state.selectedDate,
            title: metric.title,
            valueText: goalFormatter(display.dayTotal).text,
            emptyText: metric.emptyMessage,
            points: display.intradayPoints,
            accentColor: metric.accentColor,
            valueFormatter: (value) => goalFormatter(value).value,
          )),
        ),
        MetricDetailSection(
          MetricDetailSectionId.periodChart,
          visible: !isDay,
          sectionPadded(MetricBarChart(
            title: metric.title,
            values: metric.chartValues(state.data!),
            selectedRange: state.selectedRange,
            period: period,
            accentColor: metric.accentColor,
            summaryValue: goalFormatter(display.total).text,
            weekPeriodMode: weekPeriodMode,
            selectedDate: selectedDay,
            onDateSelected: daySelection.onDateSelected,
            valueFormatter: (value) => metric.formatChartValue(formatter, value),
          )),
        ),
        MetricDetailSection(
          MetricDetailSectionId.selectedDayEntries,
          visible: selectedDay != null,
          sectionPadded(ActivityDailyEntriesContent(
            entries: [
              for (final entry in entries)
                if (entry.date == selectedDay) entry,
            ],
            accentColor: metric.accentColor,
            titleDate: selectedDay,
          )),
        ),
        MetricDetailSection(
          MetricDetailSectionId.dailyGoal,
          sectionPadded(DailyGoalCard(
            goal: goalFormatter(state.dailyGoal),
            progress: goalProgress,
            icon: metric.icon,
            accentColor: metric.accentColor,
            onDecreaseGoal: onDecreaseGoal,
            onIncreaseGoal: onIncreaseGoal,
          )),
        ),
        MetricDetailSection(
          MetricDetailSectionId.statistics,
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              sectionPadded(SectionHeader(l10n.sectionStatistics)),
              sectionPadded(DailyGoalStatistics(
                progress: goalProgress,
                averageGap: goalFormatter(goalProgress.averageGapToGoal),
                unitFormatter: formatter,
                icon: metric.icon,
                accentColor: metric.accentColor,
              )),
              sectionPadded(InsightStatGrid(
                stats: [
                  InsightStat(
                    title: l10n.statTotal,
                    value: goalFormatter(display.total).value,
                    unit: goalFormatter(display.total).unit,
                    icon: metric.icon,
                    accentColor: metric.accentColor,
                  ),
                  InsightStat(
                    title: l10n.statDailyAverage,
                    value: goalFormatter(
                        averageOrZero(display.total, display.activeDays)).value,
                    unit: goalFormatter(
                        averageOrZero(display.total, display.activeDays)).unit,
                    icon: Icons.star_outline,
                    accentColor: metric.accentColor,
                  ),
                  InsightStat(
                    title: l10n.statBestDay,
                    value: goalFormatter(display.best).value,
                    unit: goalFormatter(display.best).unit,
                    icon: Icons.calendar_month_outlined,
                    accentColor: metric.accentColor,
                  ),
                  InsightStat(
                    title: l10n.statActiveDays,
                    value: formatter.count(display.activeDays),
                    unit: l10n.unitDays,
                    icon: Icons.check_circle_outline,
                    accentColor: metric.accentColor,
                  ),
                  previousPeriodInsightStat(
                    comparison: comparison,
                    selectedRange: state.selectedRange,
                    unitFormatter: formatter,
                    valueFormatter: goalFormatter,
                    accentColor: metric.accentColor,
                    l10n: l10n,
                  ),
                  ...personalBaselineInsightStats(
                    insight: personalBaselineInsight(
                      display.baselineCurrentValue,
                      display.baselineValues,
                      period.start.minusDays(1),
                    ),
                    unitFormatter: formatter,
                    valueFormatter: goalFormatter,
                    accentColor: metric.accentColor,
                    l10n: l10n,
                  ),
                ],
              )),
            ],
          ),
        ),
        MetricDetailSection(
          MetricDetailSectionId.dataConfidence,
          // A single-day period has no coverage story to tell.
          visible: period.start != period.end,
          sectionPadded(DataConfidenceCard(
            confidence: dataConfidence(
              period,
              display.trackedDates,
              display.sampleCount,
              valueKind: DataValueKind.aggregated,
            ),
            accentColor: metric.accentColor,
          )),
        ),
        MetricDetailSection(
          MetricDetailSectionId.entries,
          sectionPadded(ActivityDailyEntriesContent(
            entries: entries,
            accentColor: metric.accentColor,
          )),
        ),
      ],
    );
  }
}


// ── Route-facing per-metric wrappers (Kotlin `StepsScreen`, `DistanceScreen`, …).

class StepsScreen extends StatelessWidget {
  const StepsScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const ActivityMetricScreen(metric: ActivityMetric.steps);
}

class DistanceScreen extends StatelessWidget {
  const DistanceScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const ActivityMetricScreen(metric: ActivityMetric.distance);
}

class CaloriesOutScreen extends StatelessWidget {
  const CaloriesOutScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const ActivityMetricScreen(metric: ActivityMetric.caloriesOut);
}

class ActiveCaloriesScreen extends StatelessWidget {
  const ActiveCaloriesScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const ActivityMetricScreen(metric: ActivityMetric.activeCalories);
}

class FloorsScreen extends StatelessWidget {
  const FloorsScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const ActivityMetricScreen(metric: ActivityMetric.floors);
}

class ElevationScreen extends StatelessWidget {
  const ElevationScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const ActivityMetricScreen(metric: ActivityMetric.elevation);
}

class WheelchairScreen extends StatelessWidget {
  const WheelchairScreen({super.key});

  @override
  Widget build(BuildContext context) =>
      const ActivityMetricScreen(metric: ActivityMetric.wheelchair);
}
