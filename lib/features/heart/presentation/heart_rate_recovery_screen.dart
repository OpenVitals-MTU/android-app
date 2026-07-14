import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../core/period/period_range_preference_key.dart';
import '../../../core/period/time_range.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/health/health_permissions.dart';
import '../../../domain/usecase/load_heart_rate_recovery_period_use_case.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/charts/charts.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/metric_detail_scaffold.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/components/section_padding.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/heart_rate_recovery_view_model.dart';

/// Heart-rate recovery over time: how far the heart rate fell one minute after each hard
/// effort stopped, and whether it is falling further as the weeks go by.
///
/// The screen's hardest job is the EMPTY case, which for most people is the usual one. A
/// watch stops recording heart rate the moment a workout ends, so the fall cannot be
/// measured from readings that were never taken — and an empty chart with no explanation
/// reads as a broken app rather than as the truth about the data. So the workouts that
/// could not be measured are counted and shown, not silently dropped.
class HeartRateRecoveryScreen extends ConsumerWidget {
  const HeartRateRecoveryScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final state = ref.watch(heartRateRecoveryProvider);
    final notifier = ref.read(heartRateRecoveryProvider.notifier);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);

    return Scaffold(
      appBar: AppBar(title: Text(l10n.heartRateRecoveryHistoryTitle)),
      body: HealthConnectGate(
        requiredPermissions: {
          HcPermissions.readExercise,
          HcPermissions.readHeartRate,
        },
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.heartRateRecovery,
          onRefresh: notifier.refresh,
          isLoading: state.isLoading,
          screenError: state.error,
          weekPeriodMode: weekMode,
          syncPaused: syncPaused,
          onSelectionChanged: notifier.load,
          content: (period) => _content(state, l10n, period, weekMode),
        ),
      ),
    );
  }

  List<Widget> _content(
    HeartRateRecoveryState state,
    AppLocalizations l10n,
    DatePeriod period,
    WeekPeriodMode weekMode,
  ) {
    final comparable = state.comparable;

    // Hard workouts whose recovery could not be measured. NOT dropped: a screen that
    // quietly showed only the measurable ones would look as though the user had barely
    // trained, when in fact their device simply stopped recording.
    final unmeasured = state.readings.length - comparable.length;

    return [
      if (comparable.isEmpty)
        sectionPadded(_EmptyCard(l10n: l10n))
      else
        sectionPadded(
          MetricBarChart(
            title: l10n.heartRateRecoveryTrendTitle,
            values: [
              for (final entry in comparable)
                PeriodChartValue(
                  LocalDate.fromDateTime(entry.startTime),
                  entry.reading.headlineDropBpm!.toDouble(),
                ),
            ],
            selectedRange: state.selectedRange,
            period: period,
            accentColor: AppColors.heart,
            // The average of the falls we could actually measure — never of the ones we
            // could not, which are counted separately below rather than averaged as
            // zeroes and made to look like a collapse in fitness.
            summaryValue: '${_averageDrop(comparable).round()} bpm',
            weekPeriodMode: weekMode,
            valueFormatter: (value) => '${value.round()} bpm',
          ),
        ),
      if (unmeasured > 0)
        sectionPadded(_NoteCard(
          text: l10n.heartRateRecoveryUnmeasured('$unmeasured'),
        )),
      if (state.data?.truncated ?? false)
        sectionPadded(_NoteCard(
          text: l10n.heartRateRecoveryTruncated('$maxHeartRateRecoverySessions'),
        )),
    ];
  }
}

double _averageDrop(List<HeartRateRecoverySessionReading> readings) {
  if (readings.isEmpty) return 0;
  final total = readings
      .map((entry) => entry.reading.headlineDropBpm!)
      .reduce((a, b) => a + b);
  return total / readings.length;
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard({required this.l10n});

  final AppLocalizations l10n;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              l10n.heartRateRecoveryEmpty,
              style: theme.textTheme.titleMedium
                  ?.copyWith(fontWeight: FontWeight.w600),
            ),
            const SizedBox(height: 8),
            // Why it is empty, and what would fill it. Without this the screen looks
            // broken to the very people it is most often empty for.
            Text(
              l10n.heartRateRecoveryEmptyWatch,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ],
        ),
      ),
    );
  }
}

class _NoteCard extends StatelessWidget {
  const _NoteCard({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Text(
          text,
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      ),
    );
  }
}
