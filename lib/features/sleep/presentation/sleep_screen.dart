import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../../core/period/period_range_preference_key.dart';
import '../../../core/period/period_titles.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/metric_detail_sections.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/model/sleep_models.dart';
import '../../../domain/preferences/metric_detail_section_id.dart';
import '../../../domain/health/health_permissions.dart';
import '../../../l10n/app_localizations.dart';
import '../../../navigation/app_routes.dart';
import '../../../state/app_providers.dart';
import '../../../ui/charts/period_chart.dart';
import '../../../ui/components/daily_goal_components.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/data_source_education_item.dart';
import '../../../ui/components/metric_detail_scaffold.dart';
import '../../../ui/theme/app_colors.dart';
import 'sleep_cards.dart';
import 'sleep_metric_sections.dart';
import '../application/sleep_display.dart';
import '../application/sleep_view_model.dart';
import 'sleep_schedule_chart.dart';
import '../../../ui/components/loading_state.dart';
import '../../../ui/components/section_padding.dart';

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
    final state = ref.watch(sleepProvider);
    final notifier = ref.read(sleepProvider.notifier);
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
    final display = state.display;
    if (result == null || display == null) {
      if (state.isLoading) return const SectionLoading();
      return const _SleepMessage('No sleep data for this period.');
    }

    final isDay = display.isDay;

    if (isDay && display.dailySummary == null) {
      if (state.isLoading) return const SectionLoading();
      return const _SleepMessage('No sleep recorded for the selected day.');
    }
    if (!isDay && result.sessions.isEmpty) {
      if (state.isLoading) return const SectionLoading();
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
    final selectedDay = daySelection.selectedDate;
    final summaryValue =
        '${l10n.summaryAvgValue('${formatter.decimal(display.averageHours, 1)}h')} · '
        '${l10n.summaryNights(formatter.count(display.nights.length))}';

    // The pinned day, for SELECTED_DAY_ENTRIES — the one merged night (not its
    // raw segments), with the day's naps reported apart.
    final daySessions = selectedDay == null
        ? const <SleepData>[]
        : [if (display.nightByDate[selectedDay] != null) display.nightByDate[selectedDay]!];
    final selectedDayNaps = selectedDay == null
        ? const <SleepData>[]
        : display.napsByDate[selectedDay] ?? const <SleepData>[];

    // Held in a local, because `visible:` does NOT guard the child.
    //
    // A section's child is an ordinary Widget argument, so it is BUILT whether the
    // section is shown or not — `visible: false` only hides it afterwards. Reading
    // `display.dailySummary!` in there therefore ran even on week/month/year, where
    // the summary is of the SELECTED DAY and is null whenever that day has no sleep.
    // Opening the app after midnight and switching off the day view crashed on it:
    // the day view itself was fine (it returns the "no sleep recorded" message
    // before ever reaching here), and so was any day that HAD sleep, which is why it
    // hid for so long.
    final dailySummary = display.dailySummary;
    final openableDailySessionId = display.openableDailySessionId;

    final orderedSections = OrderedMetricDetailSections(
      sections: [
        MetricDetailSection(
          MetricDetailSectionId.intradayChart,
          visible: isDay && dailySummary != null,
          dailySummary == null
              ? const SizedBox.shrink()
              : Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    sectionPadded(SleepSessionTimelineCard(
                      session: dailySummary,
                      selectedDate: state.selectedDate,
                      formatter: formatter,
                      timeRangeText: display.dayTimeRangeText,
                      onTap: openableDailySessionId == null
                          ? null
                          : () => onOpenSession(openableDailySessionId),
                    )),
                    sectionPadded(SleepStageShareCard(
                      shares: display.stageShares,
                      formatter: formatter,
                    )),
                    // Daytime naps, reported apart from the night above.
                    if (display.dayNaps.isNotEmpty)
                      SleepSessionsSection(
                        title: l10n.sleepNaps,
                        sessions: display.dayNaps,
                        formatter: formatter,
                        onOpenSession: onOpenSession,
                      ),
                  ],
                ),
        ),
        MetricDetailSection(
          MetricDetailSectionId.activitySummary,
          sectionPadded(SleepOverviewCard(
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
              if (display.useScheduleChart)
                sectionPadded(SleepScheduleStageChart(
                  title: l10n.metricSleep,
                  summaryText: '${periodTitle(
                    l10n,
                    state.selectedRange,
                    period,
                    weekPeriodMode: state.weekPeriodMode,
                  )} · $summaryValue',
                  days: display.scheduleDays,
                  selectedRange: state.selectedRange,
                  averageSchedule: display.overviewSummary.schedule,
                  selectedDate: selectedDay,
                  onDateSelected: daySelection.onDateSelected,
                ))
              else
                sectionPadded(MetricBarChart(
                  title: l10n.metricSleep,
                  values: display.chartValues,
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
              sectionPadded(SleepStageShareCard(
                shares: display.stageShares,
                formatter: formatter,
              )),
            ],
          ),
        ),
        MetricDetailSection(
          MetricDetailSectionId.selectedDayEntries,
          visible: selectedDay != null &&
              (daySessions.isNotEmpty || selectedDayNaps.isNotEmpty),
          Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (daySessions.isNotEmpty)
                SleepSessionsSection(
                  title: DateFormat.yMMMd(
                          Localizations.localeOf(context).toLanguageTag())
                      .format(DateTime(
                          selectedDay?.year ?? 0,
                          selectedDay?.month ?? 1,
                          selectedDay?.day ?? 1)),
                  sessions: daySessions,
                  formatter: formatter,
                  onOpenSession: onOpenSession,
                ),
              if (selectedDayNaps.isNotEmpty)
                SleepSessionsSection(
                  title: l10n.sleepNaps,
                  sessions: selectedDayNaps,
                  formatter: formatter,
                  onOpenSession: onOpenSession,
                ),
            ],
          ),
        ),
        MetricDetailSection(
          MetricDetailSectionId.dailyGoal,
          sectionPadded(DailyGoalCard(
            goal: sleepHoursDisplay(state.dailyGoalHours, formatter),
            progress: display.goalProgress,
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
            selectedRange: state.selectedRange,
            formatter: formatter,
          ),
        ),
        MetricDetailSection(
          MetricDetailSectionId.dataConfidence,
          visible: period.start != period.end,
          SleepDataConfidenceSection(confidence: display.dataConfidence),
        ),
        MetricDetailSection(
          MetricDetailSectionId.entries,
          // Day view: the night is already the timeline card above, so this
          // period list is week/month only — one merged night per date, never
          // the raw segments.
          visible: !isDay,
          SleepSessionsSection(
            title: l10n.sectionSleepSessions,
            sessions: display.periodNights,
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
