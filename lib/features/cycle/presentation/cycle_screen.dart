import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../core/period/period_range_preference_key.dart';
import '../../../core/period/period_titles.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/unit_formatter.dart';
import '../../../domain/health/health_permissions.dart';
import '../../../l10n/app_localizations.dart';
import '../../../state/app_providers.dart';
import '../../../ui/components/health_connect_gate.dart';
import '../../../ui/components/metric_card.dart';
import '../../../ui/components/metric_detail_scaffold.dart';
import '../../../ui/components/ov_card.dart';
import '../../../ui/theme/app_colors.dart';
import '../application/cycle_display.dart';
import '../application/cycle_view_model.dart';
import '../../../ui/components/section_padding.dart';

/// Menstrual-cycle read-detail screen, ported from the Kotlin `CycleScreen` +
/// `CyclePeriodContent`. Shows the period's summary (period days, ovulation
/// tests, basal body temperature readings, total entries), statistics, and the
/// dated cycle observations (flow, ovulation, cervical mucus, BBT, …) — all of
/// them precomputed by the view-model into a [CycleDisplay]; this screen only
/// renders it.
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
  final display = state.display;
  if (display == null || !display.hasData) {
    if (state.isLoading && state.result == null) {
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
          title: 'Cycle tracking',
          icon: Icons.calendar_month,
          accentColor: AppColors.cycle,
          message: 'No cycle tracking data for this period.',
        ),
      ),
    ];
  }

  final l10n = AppLocalizations.of(context);

  return [
    sectionPadded(
      Row(
        children: [
          Expanded(
            child: MetricCard(
              title: 'Period days',
              value: formatter.count(display.periodDays),
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
              value: formatter.count(display.totalEntryCount),
              unit: 'total',
              icon: Icons.star_outline,
              accentColor: AppColors.cycle,
              subtitle: 'Selected period',
            ),
          ),
        ],
      ),
    ),
    sectionPadded(
      _CycleStatisticsCard(
        rows: [
          ('Period days', formatter.count(display.periodDays)),
          ('Ovulation tests', formatter.count(display.ovulationTestCount)),
          ('BBT readings', formatter.count(display.bbtReadingCount)),
          if (display.latestBbtCelsius != null)
            (
              'Latest basal temperature',
              formatter.temperature(display.latestBbtCelsius!).text,
            ),
          ('Entries', formatter.count(display.totalEntryCount)),
        ],
      ),
    ),
    const SectionHeader('Entries'),
    for (final observation in display.observations)
      sectionPadded(
        _CycleObservationRow(observation: observation, formatter: formatter),
      ),
  ];
}

// ── Observation labels (Kotlin `CyclePresentation`) ──────────────────────────

/// The row title for an observation's kind.
String _observationTitle(CycleObservationKind kind) {
  switch (kind) {
    case CycleObservationKind.menstruationPeriod:
      return 'Menstruation period';
    case CycleObservationKind.menstruationFlow:
      return 'Menstruation flow';
    case CycleObservationKind.ovulationTest:
      return 'Ovulation test';
    case CycleObservationKind.cervicalMucus:
      return 'Cervical mucus';
    case CycleObservationKind.basalBodyTemperature:
      return 'Basal body temperature';
    case CycleObservationKind.intermenstrualBleeding:
      return 'Intermenstrual bleeding';
    case CycleObservationKind.sexualActivity:
      return 'Sexual activity';
  }
}

/// The row's value line: the Health Connect codes the observation carries, read
/// back as words (and, for a basal temperature, in the user's unit system).
String _observationValue(CycleObservation observation, UnitFormatter formatter) {
  switch (observation.kind) {
    case CycleObservationKind.menstruationPeriod:
      final days = observation.days ?? 1;
      return days == 1 ? '1 day' : '$days days';
    case CycleObservationKind.menstruationFlow:
      return _flowLabel(observation.flow ?? 0);
    case CycleObservationKind.ovulationTest:
      return _ovulationLabel(observation.ovulationResult ?? 0);
    case CycleObservationKind.cervicalMucus:
      return '${_mucusAppearance(observation.mucusAppearance ?? 0)} · '
          '${_mucusSensation(observation.mucusSensation ?? 0)}';
    case CycleObservationKind.basalBodyTemperature:
      final celsius = observation.temperatureCelsius ?? 0.0;
      return '${formatter.temperature(celsius).text}'
          ' · ${_measurementLocation(observation.measurementLocation ?? 0)}';
    case CycleObservationKind.intermenstrualBleeding:
      return 'Recorded';
    case CycleObservationKind.sexualActivity:
      return _protectionLabel(observation.protectionUsed ?? 0);
  }
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
  const _CycleObservationRow({
    required this.observation,
    required this.formatter,
  });

  final CycleObservation observation;
  final UnitFormatter formatter;

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
                  Text(
                    _observationTitle(observation.kind),
                    style: theme.textTheme.titleSmall,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    _observationValue(observation, formatter),
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

