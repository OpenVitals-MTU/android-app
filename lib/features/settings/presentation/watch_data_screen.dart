import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../data/local/open_vitals_database.dart';
import '../../../l10n/app_localizations.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import '../application/ble_devices_view_model.dart';
import '../application/watch_metrics_view_model.dart';
import 'watch_common.dart';

/// Everything the watch measures that Health Connect has no type for.
///
/// Grouped by WHEN the measurement happened, not by which file carried it — the
/// sleep score and Sleep Coach arrive in the metrics file rather than the sleep
/// one, and nobody using the app should ever have to know that.
///
/// A metric the watch has never sent is absent rather than blank. Permanent
/// em-dash rows teach people to stop reading a screen, so what is missing is
/// named once at the foot instead.
class WatchDataScreen extends ConsumerWidget {
  const WatchDataScreen({required this.deviceId, super.key});

  final String deviceId;

  /// What this screen would show if the watch sent everything — the list the
  /// footer diffs against to say what is missing.
  static const List<GarminWellnessMetric> _expected = [
    GarminWellnessMetric.stress,
    GarminWellnessMetric.bodyEnergy,
    GarminWellnessMetric.moderateMinutes,
    GarminWellnessMetric.sleepScore,
    GarminWellnessMetric.sleepAwakeSeconds,
    GarminWellnessMetric.sleepAwakenings,
    GarminWellnessMetric.sleepNeedMinutes,
    GarminWellnessMetric.recoveryTime,
    GarminWellnessMetric.trainingReadiness,
    GarminWellnessMetric.trainingLoadAcute,
  ];

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final async = ref.watch(watchMetricsProvider);
    final device = ref
        .watch(bleDevicesViewModelProvider.select((s) => s.devices))
        .where((d) => d.id == deviceId)
        .firstOrNull;

    return Scaffold(
      appBar: AppBar(title: Text(l10n.settingsWatchDataTitle)),
      body: async.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (_, _) => Center(child: Text(l10n.settingsWatchDataEmpty)),
        data: (metrics) {
          if (metrics.isEmpty) {
            return Padding(
              padding: const EdgeInsets.all(24),
              child: Center(
                child: Text(
                  l10n.settingsWatchDataEmpty,
                  textAlign: TextAlign.center,
                  style: theme.textTheme.bodyMedium?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
            );
          }
          return ListView(
            padding: screenScrollPadding(context),
            children: [
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
                child: Text(
                  l10n.settingsWatchDataIntro,
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
                ),
              ),
              ..._today(context, metrics),
              ..._lastNight(context, metrics),
              ..._training(context, metrics),
              _missingFooter(context, metrics, device?.displayName),
            ],
          );
        },
      ),
    );
  }

  List<Widget> _today(BuildContext context, WatchMetrics metrics) {
    final l10n = AppLocalizations.of(context);
    final rows = <Widget>[];

    final stress = metrics[GarminWellnessMetric.stress];
    if (stress != null) {
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricStress,
        supporting: _averageOf(l10n, metrics.stressToday),
        value: '${stress.value}',
      ));
    }
    final energy = metrics[GarminWellnessMetric.bodyEnergy];
    if (energy != null) {
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricBodyBattery,
        supporting: _peakOf(metrics.bodyEnergyToday),
        value: '${energy.value}',
      ));
    }
    final moderate = metrics.valueOf(GarminWellnessMetric.moderateMinutes);
    final vigorous = metrics.valueOf(GarminWellnessMetric.vigorousMinutes);
    if (moderate != null || vigorous != null) {
      // Garmin's own convention: vigorous minutes count double towards the
      // weekly goal, which is why a bare sum would understate the week.
      final today = (moderate ?? 0) + 2 * (vigorous ?? 0);
      // The goal is weekly, so its progress must be the week's total, not
      // today's — the watch stores a running daily total that resets nightly.
      final week = metrics.intensityMinutesWeek ?? today;
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricIntensityMinutes,
        supporting: l10n.settingsWatchMetricIntensityGoal('$week', '150'),
        value: '$today',
      ));
    }

    if (rows.isEmpty) return const [];
    return [_Header(title: l10n.dashboardSummaryToday), ...rows];
  }

  List<Widget> _lastNight(BuildContext context, WatchMetrics metrics) {
    final l10n = AppLocalizations.of(context);
    final rows = <Widget>[];

    final score = metrics[GarminWellnessMetric.sleepScore];
    if (score != null) {
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricSleepScore,
        value: '${score.value}',
      ));
    }
    final awake = metrics.valueOf(GarminWellnessMetric.sleepAwakeSeconds);
    if (awake != null) {
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricAwake,
        value: formatWatchDuration(l10n, Duration(seconds: awake)),
      ));
    }
    final awakenings = metrics.valueOf(GarminWellnessMetric.sleepAwakenings);
    if (awakenings != null) {
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricAwakenings,
        value: '$awakenings',
      ));
    }
    final needed = metrics.valueOf(GarminWellnessMetric.sleepNeedMinutes);
    final usual = metrics.valueOf(GarminWellnessMetric.sleepNeedNormalMinutes);
    if (needed != null) {
      // A comparison, not a number: "8h 40m needed" alone says nothing, but
      // against the usual 7h 50m it says what the day's strain cost.
      final neededText = formatWatchDuration(l10n, Duration(minutes: needed));
      String? supporting;
      if (usual != null) {
        final usualText = formatWatchDuration(l10n, Duration(minutes: usual));
        supporting = needed > usual
            ? l10n.settingsWatchMetricSleepCoachBody(
                formatWatchDuration(l10n, Duration(minutes: needed - usual)),
                usualText,
              )
            : l10n.settingsWatchMetricSleepCoachEqual(usualText);
      }
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricSleepCoach,
        supporting: supporting,
        value: neededText,
      ));
    }

    if (rows.isEmpty) return const [];
    return [_Header(title: l10n.settingsWatchDataLastNight), ...rows];
  }

  List<Widget> _training(BuildContext context, WatchMetrics metrics) {
    final l10n = AppLocalizations.of(context);
    final rows = <Widget>[];

    final recovery = metrics.valueOf(GarminWellnessMetric.recoveryTime);
    if (recovery != null) {
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricRecoveryTime,
        value: formatWatchDuration(l10n, Duration(minutes: recovery)),
      ));
    }
    final readiness = metrics.valueOf(GarminWellnessMetric.trainingReadiness);
    if (readiness != null) {
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricTrainingReadiness,
        value: '$readiness',
      ));
    }
    final acute = metrics.valueOf(GarminWellnessMetric.trainingLoadAcute);
    final chronic = metrics.valueOf(GarminWellnessMetric.trainingLoadChronic);
    if (acute != null) {
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricTrainingLoad,
        supporting: chronic == null ? null : '$chronic',
        value: '$acute',
      ));
    }

    if (rows.isEmpty) return const [];
    return [_Header(title: l10n.settingsWatchDataTraining), ...rows];
  }

  Widget _missingFooter(
    BuildContext context,
    WatchMetrics metrics,
    String? deviceName,
  ) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final missing = metrics.missingFrom(_expected);
    if (missing.isEmpty) return const SizedBox.shrink();
    final names = [
      for (final m in missing) _labelFor(l10n, m),
    ].join(', ');
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 16, 16, 8),
      child: Text(
        l10n.settingsWatchDataMissing(names),
        style: theme.textTheme.bodySmall?.copyWith(
          color: theme.colorScheme.onSurfaceVariant,
        ),
      ),
    );
  }

  String _labelFor(AppLocalizations l10n, GarminWellnessMetric metric) =>
      switch (metric) {
        GarminWellnessMetric.stress => l10n.settingsWatchMetricStress,
        GarminWellnessMetric.bodyEnergy => l10n.settingsWatchMetricBodyBattery,
        GarminWellnessMetric.moderateMinutes ||
        GarminWellnessMetric.vigorousMinutes =>
          l10n.settingsWatchMetricIntensityMinutes,
        GarminWellnessMetric.sleepScore => l10n.settingsWatchMetricSleepScore,
        GarminWellnessMetric.sleepAwakeSeconds => l10n.settingsWatchMetricAwake,
        GarminWellnessMetric.sleepAwakenings =>
          l10n.settingsWatchMetricAwakenings,
        GarminWellnessMetric.sleepNeedMinutes ||
        GarminWellnessMetric.sleepNeedNormalMinutes =>
          l10n.settingsWatchMetricSleepCoach,
        GarminWellnessMetric.recoveryTime =>
          l10n.settingsWatchMetricRecoveryTime,
        GarminWellnessMetric.trainingReadiness =>
          l10n.settingsWatchMetricTrainingReadiness,
        GarminWellnessMetric.trainingLoadAcute ||
        GarminWellnessMetric.trainingLoadChronic =>
          l10n.settingsWatchMetricTrainingLoad,
        // Deliberately unshown: its scale is undocumented — no units, no range,
        // no direction — so the number would be decoration.
        GarminWellnessMetric.sleepPressure => l10n.settingsWatchDataTitle,
      };

  String? _averageOf(AppLocalizations l10n, List<WatchMetricReading> series) {
    if (series.isEmpty) return null;
    final sum = series.fold<int>(0, (a, r) => a + r.value);
    return l10n.settingsWatchAveragePrefix('${(sum / series.length).round()}');
  }

  String? _peakOf(List<WatchMetricReading> series) {
    if (series.isEmpty) return null;
    final peak = series.map((r) => r.value).reduce((a, b) => a > b ? a : b);
    return '$peak';
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
      child: Text(
        title,
        style: theme.textTheme.titleSmall?.copyWith(
          color: theme.colorScheme.onSurfaceVariant,
        ),
      ),
    );
  }
}
