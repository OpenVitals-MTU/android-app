import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../core/period/period_range_preference_key.dart';
import '../../core/period/period_titles.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/metric_detail_sections.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../domain/model/sleep_models.dart';
import '../../domain/preferences/metric_detail_section_id.dart';
import '../../health/health_permissions.dart';
import '../../l10n/app_localizations.dart';
import '../../navigation/app_routes.dart';
import '../../state/app_providers.dart';
import '../../ui/charts/period_chart.dart';
import '../../ui/components/daily_goal_components.dart';
import '../../ui/components/health_connect_gate.dart';
import '../../ui/components/data_source_education_item.dart';
import '../../ui/components/metric_detail_scaffold.dart';
import '../../ui/theme/app_colors.dart';
import 'sleep_cards.dart';
import 'sleep_metric_sections.dart';
import 'sleep_notifier.dart';
import 'sleep_presentation.dart';
import 'sleep_schedule_chart.dart';

/// The sleep period-detail screen, a port of the Kotlin `SleepScreen` +
/// `SleepMetricOrderedSections`.
///
/// Day view renders the night's stage timeline; week/month/year render the
/// duration bar chart. Both then share the overview, daily goal, statistics
/// (goal stats, period grid, sleep-target reading, sleep-vs-HRV correlation),
/// data confidence and the session list, in the user's section order.
class SleepScreen extends ConsumerWidget {
  const SleepScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(sleepNotifierProvider);
    final notifier = ref.read(sleepNotifierProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);
    final isEditingSections = ref.watch(metricDetailSectionEditProvider);
    final l10n = AppLocalizations.of(context);

    return Scaffold(
      appBar: AppBar(
        title: Text(l10n.metricSleep),
        actions: [
          IconButton(
            onPressed: ref.read(metricDetailSectionEditProvider.notifier).toggle,
            tooltip: isEditingSections
                ? l10n.cdFinishMetricSectionEditing
                : l10n.cdEditMetricSections,
            icon: Icon(isEditingSections ? Icons.check : Icons.tune),
          ),
        ],
      ),
      body: HealthConnectGate(
        requiredPermissions: {HcPermissions.readSleep},
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.sleep,
          onRefresh: notifier.refresh,
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: notifier.load,
          content: (period) => [
            _SleepContent(
              state: state,
              period: period,
              formatter: formatter,
              onOpenSession: (id) =>
                  context.push(AppRoutes.sleepDetailLocation(id)),
              onDecreaseGoal: notifier.decreaseDailyGoal,
              onIncreaseGoal: notifier.increaseDailyGoal,
            ),
          ],
        ),
      ),
    );
  }
}

class _SleepContent extends StatelessWidget {
  const _SleepContent({
    required this.state,
    required this.period,
    required this.formatter,
    required this.onOpenSession,
    required this.onDecreaseGoal,
    required this.onIncreaseGoal,
  });

  final SleepState state;
  final DatePeriod period;
  final UnitFormatter formatter;
  final ValueChanged<String> onOpenSession;
  final VoidCallback onDecreaseGoal;
  final VoidCallback onIncreaseGoal;

  @override
  Widget build(BuildContext context) {
    final result = state.result;
    if (result == null) {
      if (state.isLoading) return const _LoadingBlock();
      return const _SleepMessage('No sleep data for this period.');
    }

    final display = buildSleepDisplay(
      result: result,
      selectedRange: state.selectedRange,
      selectedDate: state.selectedDate,
      sleepRangeMode: state.sleepRangeMode,
      weekPeriodMode: state.weekPeriodMode,
    );
    final isDay = state.selectedRange == TimeRange.day;

    if (isDay && display.dailySummary == null) {
      if (state.isLoading) return const _LoadingBlock();
      return const _SleepMessage('No sleep recorded for the selected day.');
    }
    if (!isDay && result.sessions.isEmpty) {
      if (state.isLoading) return const _LoadingBlock();
      return const _SleepMessage('No sleep recorded for this period.');
    }

    return ChartDaySelectionScope(
      selectedRange: state.selectedRange,
      selectedDate: state.selectedDate,
      builder: (context, daySelection) =>
          _sections(context, display, daySelection, isDay),
    );
  }

  Widget _sections(
    BuildContext context,
    SleepDisplay display,
    ChartDaySelection daySelection,
    bool isDay,
  ) {
    final l10n = AppLocalizations.of(context);
    final goalProgress = sleepGoalProgress(
      durationPoints: display.durationPoints,
      period: period,
      targetHours: state.dailyGoalHours,
    );
    final selectedDay = daySelection.selectedDate;
    final nights = sleepNights(display.durationPoints);
    final averageHours = sleepAverageHours(nights);
    final summaryValue =
        '${l10n.summaryAvgValue('${formatter.decimal(averageHours, 1)}h')} · '
        '${l10n.summaryNights(formatter.count(nights.length))}';

    final scheduleDays = toSleepScheduleDays(display.sessionsByDate);
    // Kotlin `useScheduleChart`: week/month only, and only once some night has
    // a bedtime — the schedule axis is meaningless without one.
    final useScheduleChart = (state.selectedRange == TimeRange.week ||
            state.selectedRange == TimeRange.month) &&
        scheduleDays.any((day) => day.inBedStart != null);

    // The sessions of the pinned day, for SELECTED_DAY_ENTRIES.
    final daySessions = selectedDay == null
        ? const <SleepData>[]
        : display.sessionsByDate[selectedDay] ?? const <SleepData>[];

    final orderedSections = OrderedMetricDetailSections(
      sections: [
        MetricDetailSection(
          MetricDetailSectionId.intradayChart,
          visible: isDay && display.dailySummary != null,
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              _padded(SleepSessionTimelineCard(
                session: display.dailySummary!,
                formatter: formatter,
                timeRangeText: _dayTimeRangeText(display.dailySessions),
              )),
              _padded(SleepStageShareCard(
                summary: display.overviewSummary,
                formatter: formatter,
              )),
            ],
          ),
        ),
        MetricDetailSection(
          MetricDetailSectionId.activitySummary,
          _padded(SleepOverviewCard(
            summary: display.overviewSummary,
            formatter: formatter,
            periodTitle: periodTitle(
              l10n,
              state.selectedRange,
              period,
              weekPeriodMode: state.weekPeriodMode,
            ),
          )),
        ),
        MetricDetailSection(
          MetricDetailSectionId.periodChart,
          visible: !isDay,
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Kotlin prefers the schedule chart on week/month whenever at
              // least one night knows when you went to bed; otherwise there is
              // nothing to place on a clock axis and it falls back to durations.
              if (useScheduleChart)
                _padded(SleepScheduleStageChart(
                  title: l10n.metricSleep,
                  summaryText: '${periodTitle(
                    l10n,
                    state.selectedRange,
                    period,
                    weekPeriodMode: state.weekPeriodMode,
                  )} · $summaryValue',
                  days: scheduleDays,
                  selectedRange: state.selectedRange,
                  averageSchedule: display.overviewSummary.schedule,
                  selectedDate: selectedDay,
                  onDateSelected: daySelection.onDateSelected,
                ))
              else
                _padded(MetricBarChart(
                  title: l10n.metricSleep,
                  values: [
                    for (final point in display.durationPoints)
                      PeriodChartValue(point.date, point.hours),
                  ],
                  selectedRange: state.selectedRange,
                  period: period,
                  accentColor: AppColors.sleep,
                  accentAlpha: 0.75,
                  summaryValue: summaryValue,
                  weekPeriodMode: state.weekPeriodMode,
                  // A year of nights averages; summing them would be meaningless.
                  yearAggregation: PeriodBarAggregation.averageNonZero,
                  selectedDate: selectedDay,
                  onDateSelected: daySelection.onDateSelected,
                  valueFormatter: (value) => '${formatter.decimal(value, 1)}h',
                )),
              _padded(SleepStageShareCard(
                summary: display.overviewSummary,
                formatter: formatter,
              )),
            ],
          ),
        ),
        MetricDetailSection(
          MetricDetailSectionId.selectedDayEntries,
          visible: selectedDay != null && daySessions.isNotEmpty,
          SleepSessionsSection(
            title: DateFormat.yMMMd(Localizations.localeOf(context).toLanguageTag())
                .format(DateTime(
                    selectedDay?.year ?? 0,
                    selectedDay?.month ?? 1,
                    selectedDay?.day ?? 1)),
            sessions: daySessions,
            formatter: formatter,
            onOpenSession: onOpenSession,
          ),
        ),
        MetricDetailSection(
          MetricDetailSectionId.dailyGoal,
          _padded(DailyGoalCard(
            goal: sleepHoursDisplay(state.dailyGoalHours, formatter),
            progress: goalProgress,
            icon: kSleepIcon,
            accentColor: AppColors.sleep,
            onDecreaseGoal: onDecreaseGoal,
            onIncreaseGoal: onIncreaseGoal,
          )),
        ),
        MetricDetailSection(
          MetricDetailSectionId.statistics,
          SleepStatisticsSectionContent(
            display: display,
            period: period,
            selectedRange: state.selectedRange,
            formatter: formatter,
            goalProgress: goalProgress,
            targetHours: state.dailyGoalHours,
          ),
        ),
        MetricDetailSection(
          MetricDetailSectionId.dataConfidence,
          visible: period.start != period.end,
          SleepDataConfidenceSection(
            sessions: isDay ? display.dailySessions : display.periodSessions,
            durationPoints: display.durationPoints,
            period: period,
          ),
        ),
        MetricDetailSection(
          MetricDetailSectionId.entries,
          // A single night is already spelled out by the timeline card above.
          visible: isDay ? display.dailySessions.length > 1 : true,
          SleepSessionsSection(
            title: l10n.sectionSleepSessions,
            sessions: isDay ? display.dailySessions : display.periodSessions,
            formatter: formatter,
            onOpenSession: onOpenSession,
          ),
        ),
      ],
    );

    // Kotlin appends the data-source education link only to the DAY content
    // (`sleepDayContent`), after the ordered sections.
    if (!isDay) return orderedSections;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        orderedSections,
        const DataSourceEducationItem(),
      ],
    );
  }
}

final DateFormat _timeFormat = DateFormat('HH:mm');

String? _dayTimeRangeText(List<SleepData> sessions) {
  if (sessions.isEmpty) return null;
  final sorted = [...sessions]
    ..sort((a, b) => a.startTime.compareTo(b.startTime));
  return sorted
      .map((s) =>
          '${_timeFormat.format(s.startTime.toLocal())} - ${_timeFormat.format(s.endTime.toLocal())}')
      .join(' | ');
}

Widget _padded(Widget child) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: child,
    );

class _LoadingBlock extends StatelessWidget {
  const _LoadingBlock();

  @override
  Widget build(BuildContext context) => const Padding(
        padding: EdgeInsets.symmetric(vertical: 48),
        child: Center(child: CircularProgressIndicator()),
      );
}

class _SleepMessage extends StatelessWidget {
  const _SleepMessage(this.message);

  final String message;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.all(16),
      child: Text(
        message,
        style: theme.textTheme.bodyMedium
            ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
      ),
    );
  }
}
