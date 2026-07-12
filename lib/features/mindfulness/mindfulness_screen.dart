import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:intl/intl.dart';

import '../../core/period/period_range_preference_key.dart';
import '../../core/period/period_titles.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../core/time/local_date.dart';
import '../../domain/model/mindfulness_models.dart';
import '../../data/source/health/health_permissions.dart';
import '../../l10n/app_localizations.dart';
import '../../navigation/app_routes.dart';
import '../../state/app_providers.dart';
import '../../ui/charts/period_chart.dart';
import '../../ui/components/health_connect_gate.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/metric_detail_scaffold.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/theme/app_colors.dart';
import 'mindfulness_notifier.dart';
import 'reminders/mindfulness_reminder_card.dart';
import '../../ui/components/section_padding.dart';

/// Mindfulness read-detail screen, ported from the Kotlin `MindfulnessScreen` +
/// `MindfulnessPeriodContent`. Shows the period's total mindfulness time, a
/// daily-minutes chart, statistics, and the session list.
///
/// The timer-based logging flow and bell/ambient sounds are Phase 6; the "+ Log
/// session" action routes to the manual mindfulness add-entry route.
class MindfulnessScreen extends ConsumerWidget {
  const MindfulnessScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(mindfulnessProvider);
    final notifier = ref.read(mindfulnessProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Mindfulness')),
      floatingActionButton: FloatingActionButton.extended(
        // TODO(phase6): the timer-based session logging + bell/ambient sounds
        // land in Phase 6; for now route to the manual add-entry flow.
        onPressed: () => context.push(AppRoutes.mindfulnessEntry),
        icon: const Icon(Icons.add),
        label: const Text('Log session'),
      ),
      body: HealthConnectGate(
        requiredPermissions: {HcPermissions.readMindfulness},
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.mindfulness,
          onRefresh: notifier.refresh,
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: notifier.load,
          content: (period) =>
              _content(context, state, formatter, period, weekMode),
        ),
      ),
    );
  }
}

List<Widget> _content(
  BuildContext context,
  MindfulnessMetricState state,
  UnitFormatter formatter,
  DatePeriod period,
  WeekPeriodMode weekPeriodMode,
) {
  final sessions = state.sessions;
  if (sessions.isEmpty) {
    if (state.isLoading && state.data == null) {
      return const [
        Padding(
          padding: EdgeInsets.symmetric(vertical: 48),
          child: Center(child: CircularProgressIndicator()),
        ),
      ];
    }
    return [
      sectionPadded(
        const MetricCardPlaceholder(
          title: 'Mindfulness',
          icon: Icons.self_improvement,
          accentColor: AppColors.mindfulness,
          message: 'No mindfulness sessions for this period.',
        ),
      ),
      // Reminders are configurable with no sessions logged yet — that is exactly
      // when a user wants to switch them on.
      sectionPadded(const MindfulnessReminderCard()),
    ];
  }

  final l10n = AppLocalizations.of(context);
  final summary = _MindfulnessSummary.of(sessions);
  final chartValues = _chartValues(sessions);
  final total = formatter.minutes(summary.totalMinutes);
  final sorted = [...sessions]
    ..sort((a, b) => b.startTime.compareTo(a.startTime));

  return [
    sectionPadded(
      Row(
        children: [
          Expanded(
            child: MetricCard(
              title: 'Total mindfulness',
              value: total.value,
              unit: total.unit,
              icon: Icons.self_improvement,
              accentColor: AppColors.mindfulness,
              subtitle: periodTitle(
                l10n,
                state.selectedRange,
                period,
                weekPeriodMode: weekPeriodMode,
              ),
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: MetricCard(
              title: 'Sessions',
              value: formatter.count(summary.sessionCount),
              unit: 'total',
              icon: Icons.check_circle_outline,
              accentColor: AppColors.mindfulness,
              subtitle: 'Selected period',
            ),
          ),
        ],
      ),
    ),
    sectionPadded(
      MetricBarChart(
        title: 'Mindfulness',
        values: chartValues,
        selectedRange: state.selectedRange,
        period: period,
        accentColor: AppColors.mindfulness,
        summaryValue: total.text,
        weekPeriodMode: weekPeriodMode,
        valueFormatter: (value) =>
            formatter.minutes(value.round()).text,
      ),
    ),
    sectionPadded(
      _MindfulnessStatisticsCard(
        rows: [
          ('Total', formatter.duration(summary.totalMs)),
          ('Sessions', formatter.count(summary.sessionCount)),
          ('Average duration', formatter.duration(summary.averageDurationMs)),
          ('Longest session', formatter.duration(summary.longestSessionMs)),
        ],
      ),
    ),
    const SectionHeader('Sessions'),
    for (final session in sorted)
      sectionPadded(
        _MindfulnessSessionRow(session: session, formatter: formatter),
      ),
    sectionPadded(const MindfulnessReminderCard()),
  ];
}

List<PeriodChartValue> _chartValues(List<MindfulnessSession> sessions) {
  final minutesByDate = <LocalDate, double>{};
  for (final session in sessions) {
    final date = instantToLocalDate(session.startTime);
    final minutes = math.max(0, session.durationMs) / 60000.0;
    minutesByDate.update(date, (value) => value + minutes,
        ifAbsent: () => minutes);
  }
  return [
    for (final entry in minutesByDate.entries)
      PeriodChartValue(entry.key, entry.value),
  ];
}

class _MindfulnessSummary {
  const _MindfulnessSummary({
    required this.totalMs,
    required this.sessionCount,
    required this.averageDurationMs,
    required this.longestSessionMs,
  });

  factory _MindfulnessSummary.of(List<MindfulnessSession> sessions) {
    final totalMs =
        sessions.fold<int>(0, (sum, session) => sum + session.durationMs);
    final longest = sessions.fold<int>(
      0,
      (m, session) => math.max(m, session.durationMs),
    );
    final count = sessions.length;
    return _MindfulnessSummary(
      totalMs: totalMs,
      sessionCount: count,
      averageDurationMs: count > 0 ? totalMs ~/ count : 0,
      longestSessionMs: longest,
    );
  }

  final int totalMs;
  final int sessionCount;
  final int averageDurationMs;
  final int longestSessionMs;

  int get totalMinutes => totalMs ~/ 60000;
}

class _MindfulnessSessionRow extends StatelessWidget {
  const _MindfulnessSessionRow({required this.session, required this.formatter});

  final MindfulnessSession session;
  final UnitFormatter formatter;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final start = session.startTime.toLocal();
    final dateLabel = DateFormat('EEE d MMM').format(start);
    final timeLabel = DateFormat.jm().format(start);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            const Icon(Icons.self_improvement,
                color: AppColors.mindfulness, size: 24),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    session.title ?? 'Mindfulness',
                    style: theme.textTheme.titleSmall,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    '$dateLabel  ·  $timeLabel',
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 8),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              children: [
                Text(
                  formatter.duration(session.durationMs),
                  style: theme.textTheme.labelLarge,
                ),
                const SizedBox(height: 4),
                SourceChip(source: session.source),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

/// A total/sessions/average/longest statistics card. A trimmed port of the
/// Kotlin mindfulness statistics `InsightStatGrid`.
class _MindfulnessStatisticsCard extends StatelessWidget {
  const _MindfulnessStatisticsCard({required this.rows});

  final List<(String, String)> rows;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Icon(Icons.insights,
                    color: AppColors.mindfulness, size: 20),
                const SizedBox(width: 8),
                Text(
                  'Statistics',
                  style: theme.textTheme.labelMedium
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                ),
              ],
            ),
            const SizedBox(height: 12),
            for (final row in rows)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 4),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(row.$1, style: theme.textTheme.bodyMedium),
                    Text(
                      row.$2,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        fontWeight: FontWeight.w600,
                        color: theme.colorScheme.onSurface,
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}

