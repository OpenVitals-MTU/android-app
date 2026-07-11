import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../core/period/period_range_preference_key.dart';
import '../../core/period/period_titles.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/unit_formatter.dart';
import '../../core/time/local_date.dart';
import '../../domain/model/cycle_models.dart';
import '../../health/health_permissions.dart';
import '../../l10n/app_localizations.dart';
import '../../state/app_providers.dart';
import '../../ui/components/health_connect_gate.dart';
import '../../ui/components/metric_card.dart';
import '../../ui/components/metric_detail_scaffold.dart';
import '../../ui/components/ov_card.dart';
import '../../ui/theme/app_colors.dart';
import 'cycle_notifier.dart';

/// Menstrual-cycle read-detail screen, ported from the Kotlin `CycleScreen` +
/// `CyclePeriodContent`. Shows the period's summary (period days, ovulation
/// tests, basal body temperature readings, total entries), statistics, and the
/// dated cycle observations (flow, ovulation, cervical mucus, BBT, …).
///
/// This is sensitive/opt-in data: the screen is gated behind the cycle read
/// permission (Kotlin `HealthConnectFeature.CYCLE`).
class CycleScreen extends ConsumerWidget {
  const CycleScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(cycleProvider);
    final notifier = ref.read(cycleProvider.notifier);
    final formatter = ref.watch(unitFormatterProvider);
    final weekMode = ref.watch(weekPeriodModeProvider);
    final syncPaused = !ref.watch(healthConnectSyncEnabledProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Cycle tracking')),
      body: HealthConnectGate(
        // The cycle read permission; sensitive/opt-in, so the whole screen is
        // gated (Kotlin `HealthConnectFeature.CYCLE`).
        requiredPermissions: {HcPermissions.readMenstruationPeriod},
        showInlineSyncBanner: false,
        child: MetricDetailScaffold(
          rangePreferenceKey: PeriodRangePreferenceKey.cycle,
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
  CycleMetricState state,
  UnitFormatter formatter,
  DatePeriod period,
  WeekPeriodMode weekPeriodMode,
) {
  final data = state.data;
  if (!data.hasData) {
    if (state.isLoading && state.result == null) {
      return const [
        Padding(
          padding: EdgeInsets.symmetric(vertical: 48),
          child: Center(child: CircularProgressIndicator()),
        ),
      ];
    }
    return [
      _padded(
        const MetricCardPlaceholder(
          title: 'Cycle tracking',
          icon: Icons.calendar_month,
          accentColor: AppColors.cycle,
          message: 'No cycle tracking data for this period.',
        ),
      ),
    ];
  }

  final l10n = AppLocalizations.of(context);
  final summary = _CycleSummary.of(data);
  final observations = _observations(data, formatter);

  return [
    _padded(
      Row(
        children: [
          Expanded(
            child: MetricCard(
              title: 'Period days',
              value: formatter.count(summary.periodDays),
              unit: 'days',
              icon: Icons.calendar_month,
              accentColor: AppColors.cycle,
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
              title: 'Entries',
              value: formatter.count(summary.totalEntryCount),
              unit: 'total',
              icon: Icons.star_outline,
              accentColor: AppColors.cycle,
              subtitle: 'Selected period',
            ),
          ),
        ],
      ),
    ),
    _padded(
      _CycleStatisticsCard(
        rows: [
          ('Period days', formatter.count(summary.periodDays)),
          ('Ovulation tests', formatter.count(summary.ovulationTestCount)),
          ('BBT readings', formatter.count(summary.bbtReadingCount)),
          if (summary.latestBbt != null)
            (
              'Latest basal temperature',
              formatter.temperature(summary.latestBbt!).text,
            ),
          ('Entries', formatter.count(summary.totalEntryCount)),
        ],
      ),
    ),
    const SectionHeader('Entries'),
    for (final observation in observations)
      _padded(_CycleObservationRow(observation: observation)),
  ];
}

class _CycleSummary {
  const _CycleSummary({
    required this.periodDays,
    required this.ovulationTestCount,
    required this.bbtReadingCount,
    required this.totalEntryCount,
    required this.latestBbt,
  });

  factory _CycleSummary.of(CycleData data) {
    final periodDates = <LocalDate>{};
    for (final period in data.menstruationPeriods) {
      final start = instantToLocalDate(period.startTime);
      final end = instantToLocalDate(
        period.endTime.subtract(const Duration(milliseconds: 1)),
      );
      var date = start;
      while (!date.isAfter(end)) {
        periodDates.add(date);
        date = date.plusDays(1);
      }
    }
    BasalBodyTemperatureEntry? latest;
    for (final entry in data.basalBodyTemperature) {
      if (latest == null || entry.time.isAfter(latest.time)) {
        latest = entry;
      }
    }
    final total = data.menstruationFlows.length +
        data.menstruationPeriods.length +
        data.ovulationTests.length +
        data.cervicalMucus.length +
        data.basalBodyTemperature.length +
        data.intermenstrualBleeding.length +
        data.sexualActivity.length;
    return _CycleSummary(
      periodDays: periodDates.length,
      ovulationTestCount: data.ovulationTests.length,
      bbtReadingCount: data.basalBodyTemperature.length,
      totalEntryCount: total,
      latestBbt: latest?.temperatureCelsius,
    );
  }

  final int periodDays;
  final int ovulationTestCount;
  final int bbtReadingCount;
  final int totalEntryCount;
  final double? latestBbt;
}

class _CycleObservation {
  const _CycleObservation({
    required this.time,
    required this.title,
    required this.value,
    required this.source,
  });

  final DateTime time;
  final String title;
  final String value;
  final String source;
}

List<_CycleObservation> _observations(CycleData data, UnitFormatter formatter) {
  final observations = <_CycleObservation>[];
  for (final period in data.menstruationPeriods) {
    final start = instantToLocalDate(period.startTime);
    final end = instantToLocalDate(
      period.endTime.subtract(const Duration(milliseconds: 1)),
    );
    final days = (end.epochDay - start.epochDay + 1).clamp(1, 1 << 30);
    observations.add(_CycleObservation(
      time: period.startTime,
      title: 'Menstruation period',
      value: days == 1 ? '1 day' : '$days days',
      source: period.source,
    ));
  }
  for (final flow in data.menstruationFlows) {
    observations.add(_CycleObservation(
      time: flow.time,
      title: 'Menstruation flow',
      value: _flowLabel(flow.flow),
      source: flow.source,
    ));
  }
  for (final test in data.ovulationTests) {
    observations.add(_CycleObservation(
      time: test.time,
      title: 'Ovulation test',
      value: _ovulationLabel(test.result),
      source: test.source,
    ));
  }
  for (final mucus in data.cervicalMucus) {
    observations.add(_CycleObservation(
      time: mucus.time,
      title: 'Cervical mucus',
      value: '${_mucusAppearance(mucus.appearance)} · '
          '${_mucusSensation(mucus.sensation)}',
      source: mucus.source,
    ));
  }
  for (final temperature in data.basalBodyTemperature) {
    observations.add(_CycleObservation(
      time: temperature.time,
      title: 'Basal body temperature',
      value: '${formatter.temperature(temperature.temperatureCelsius).text}'
          ' · ${_measurementLocation(temperature.measurementLocation)}',
      source: temperature.source,
    ));
  }
  for (final bleeding in data.intermenstrualBleeding) {
    observations.add(_CycleObservation(
      time: bleeding.time,
      title: 'Intermenstrual bleeding',
      value: 'Recorded',
      source: bleeding.source,
    ));
  }
  for (final activity in data.sexualActivity) {
    observations.add(_CycleObservation(
      time: activity.time,
      title: 'Sexual activity',
      value: _protectionLabel(activity.protectionUsed),
      source: activity.source,
    ));
  }
  observations.sort((a, b) => b.time.compareTo(a.time));
  return observations;
}

// ── Health Connect enum → label maps (Kotlin `CyclePresentation`) ───────────
String _flowLabel(int flow) {
  switch (flow) {
    case 1:
      return 'Light';
    case 2:
      return 'Medium';
    case 3:
      return 'Heavy';
    default:
      return 'Unknown';
  }
}

String _ovulationLabel(int result) {
  switch (result) {
    case 1:
      return 'Positive';
    case 2:
      return 'High';
    case 3:
      return 'Negative';
    default:
      return 'Inconclusive';
  }
}

String _mucusAppearance(int appearance) {
  switch (appearance) {
    case 1:
      return 'Dry';
    case 2:
      return 'Sticky';
    case 3:
      return 'Creamy';
    case 4:
      return 'Watery';
    case 5:
      return 'Egg white';
    case 6:
      return 'Unusual';
    default:
      return 'Unknown';
  }
}

String _mucusSensation(int sensation) {
  switch (sensation) {
    case 1:
      return 'Light';
    case 2:
      return 'Medium';
    case 3:
      return 'Heavy';
    default:
      return 'Unknown';
  }
}

String _measurementLocation(int location) {
  switch (location) {
    case 1:
      return 'Armpit';
    case 2:
      return 'Finger';
    case 3:
      return 'Forehead';
    case 4:
      return 'Mouth';
    case 5:
      return 'Rectum';
    case 6:
      return 'Temporal artery';
    case 7:
      return 'Toe';
    case 8:
      return 'Ear';
    case 9:
      return 'Wrist';
    case 10:
      return 'Vagina';
    default:
      return 'Unknown';
  }
}

String _protectionLabel(int protectionUsed) {
  switch (protectionUsed) {
    case 1:
      return 'Protected';
    case 2:
      return 'Unprotected';
    default:
      return 'Unknown';
  }
}

class _CycleObservationRow extends StatelessWidget {
  const _CycleObservationRow({required this.observation});

  final _CycleObservation observation;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final local = observation.time.toLocal();
    final dateLabel = DateFormat('EEE d MMM').format(local);
    final timeLabel = DateFormat.jm().format(local);
    return OpenVitalsCard(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(observation.title, style: theme.textTheme.titleSmall),
                  const SizedBox(height: 2),
                  Text(
                    observation.value,
                    style: theme.textTheme.bodyMedium,
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
            SourceChip(source: observation.source),
          ],
        ),
      ),
    );
  }
}

class _CycleStatisticsCard extends StatelessWidget {
  const _CycleStatisticsCard({required this.rows});

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
                const Icon(Icons.insights, color: AppColors.cycle, size: 20),
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
                    Expanded(
                        child: Text(row.$1, style: theme.textTheme.bodyMedium)),
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

Widget _padded(Widget child) => Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
      child: child,
    );
