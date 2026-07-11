import 'package:flutter/material.dart';

import '../../../../core/presentation/unit_formatter.dart';
import '../../../../domain/model/ble_sensor_models.dart';
import '../../../../l10n/app_localizations.dart';
import '../../../../ui/components/ov_surface.dart';
import '../../../../ui/theme/app_colors.dart';
import 'activity_recording.dart';

/// Port of the Kotlin `ActivityRecordingSensorUi.kt` (the live half; the
/// post-recording summary lives in `activity_recorded_sensor_summary.dart`).

/// Kotlin `activityRecordingAccentColor()`.
Color activityRecordingAccentColor() => AppColors.workout;

/// Kotlin `statusLabel(BleConnectionStatus)`.
String bleConnectionStatusLabel(
  BleConnectionStatus status,
  AppLocalizations l10n,
) =>
    switch (status) {
      BleConnectionStatus.connected => l10n.activityRecordingSensorsConnected,
      BleConnectionStatus.connecting => l10n.activityRecordingSensorsConnecting,
      BleConnectionStatus.reconnecting =>
        l10n.activityRecordingSensorsReconnecting,
      BleConnectionStatus.disconnected => l10n.activityRecordingSensorsDisabled,
    };

/// Kotlin `statusColor(BleConnectionStatus)`.
Color bleConnectionStatusColor(BleConnectionStatus status, ColorScheme scheme) =>
    switch (status) {
      BleConnectionStatus.connected => activityRecordingAccentColor(),
      BleConnectionStatus.connecting ||
      BleConnectionStatus.reconnecting =>
        scheme.tertiary,
      BleConnectionStatus.disconnected => scheme.error,
    };

/// Kotlin `ActivityRecordingSensorStatusCard`: one chip per paired sensor, or a
/// pointer to Settings when none are paired.
class ActivityRecordingSensorStatusCard extends StatelessWidget {
  const ActivityRecordingSensorStatusCard({
    super.key,
    required this.deviceStatuses,
  });

  final List<BleDeviceConnectionStatus> deviceStatuses;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);

    if (deviceStatuses.isEmpty) {
      return OpenVitalsSurface(
        style: OpenVitalsSurfaceStyle.metric,
        contentPadding: const EdgeInsets.all(12),
        child: Text(
          l10n.activityRecordingSensorsAddInSettings,
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
      );
    }

    return OpenVitalsSurface(
      style: OpenVitalsSurfaceStyle.metric,
      contentPadding: const EdgeInsets.all(12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        spacing: 8,
        children: [
          Text(l10n.activityRecordingSensorsTitle,
              style: theme.textTheme.titleSmall),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              for (final status in deviceStatuses)
                _SensorChip(status: status),
            ],
          ),
        ],
      ),
    );
  }
}

class _SensorChip extends StatelessWidget {
  const _SensorChip({required this.status});

  final BleDeviceConnectionStatus status;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final l10n = AppLocalizations.of(context);
    final battery = status.batteryPercent;
    final label = '${status.displayName} · '
        '${bleConnectionStatusLabel(status.status, l10n)}'
        '${battery == null ? '' : ' · $battery%'}';

    // The Kotlin chip is a disabled AssistChip: it displays, it does not act.
    return Chip(
      avatar: Icon(
        Icons.bluetooth,
        size: 16,
        color: bleConnectionStatusColor(status.status, scheme),
      ),
      label: Text(label),
    );
  }
}

/// Kotlin `ActivityRecordingLiveSensorStats`. Renders nothing without paired
/// sensors; otherwise the status card, then either a waiting hint or the live
/// values.
class ActivityRecordingLiveSensorStats extends StatelessWidget {
  const ActivityRecordingLiveSensorStats({
    super.key,
    required this.state,
    required this.unitFormatter,
  });

  final ActivityRecordingState state;
  final UnitFormatter unitFormatter;

  @override
  Widget build(BuildContext context) {
    if (state.bleDeviceStatuses.isEmpty) return const SizedBox.shrink();

    final theme = Theme.of(context);
    final l10n = AppLocalizations.of(context);
    final stats = _liveStats(l10n);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      spacing: 8,
      children: [
        ActivityRecordingSensorStatusCard(deviceStatuses: state.bleDeviceStatuses),
        OpenVitalsSurface(
          contentPadding: const EdgeInsets.all(12),
          borderRadius: const BorderRadius.all(Radius.circular(16)),
          child: stats.isEmpty
              ? Text(
                  // A connected watch that is not broadcasting looks identical
                  // to one that has simply not sent a packet yet, so the hint
                  // has to say which.
                  state.bleHeartRateNoSignal
                      ? l10n.activityRecordingSensorsGarminBroadcastHint
                      : l10n.activityRecordingSensorsWaitingForData,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                )
              : Row(
                  children: [
                    for (var i = 0; i < stats.length; i++) ...[
                      if (i > 0) const SizedBox(width: 16),
                      Expanded(child: stats[i]),
                    ],
                  ],
                ),
        ),
      ],
    );
  }

  /// In the Kotlin order: cadence, power, speed, running cadence, heart rate.
  List<Widget> _liveStats(AppLocalizations l10n) {
    final stats = <Widget>[];

    final cadence = state.currentCyclingCadenceRpm;
    if (cadence != null) {
      stats.add(_LiveSensorStat(
        label: l10n.activityRecordingLiveCadence,
        value: unitFormatter.count(cadence),
        unit: 'rpm',
      ));
    }
    final power = state.currentPowerWatts;
    if (power != null) {
      stats.add(_LiveSensorStat(
        label: l10n.activityRecordingLivePower,
        value: unitFormatter.count(power.round()),
        unit: 'W',
      ));
    }
    final speed = state.currentSensorSpeedMetersPerSecond;
    if (speed != null) {
      stats.add(_LiveSensorStat(
        label: l10n.activityRecordingLiveSpeed,
        value: unitFormatter.speed(speed).text,
        unit: '',
      ));
    }
    final runningCadence = state.currentRunningCadenceRpm;
    if (runningCadence != null) {
      stats.add(_LiveSensorStat(
        label: l10n.activityRecordingLiveCadence,
        value: unitFormatter.count(runningCadence),
        unit: 'rpm',
      ));
    }
    final bpm = state.currentHeartRateBpm;
    if (bpm != null) {
      stats.add(_LiveSensorStat(
        label: l10n.activityRecordingLiveHeartRate,
        value: unitFormatter.count(bpm),
        unit: 'bpm',
      ));
    }
    return stats;
  }
}

class _LiveSensorStat extends StatelessWidget {
  const _LiveSensorStat({
    required this.label,
    required this.value,
    required this.unit,
  });

  final String label;
  final String value;
  final String unit;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      spacing: 4,
      children: [
        Row(
          children: [
            Icon(Icons.favorite_outline,
                size: 14, color: activityRecordingAccentColor()),
            const SizedBox(width: 4),
            Flexible(
              child: Text(
                label,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.labelMedium
                    ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
              ),
            ),
          ],
        ),
        Text(
          unit.trim().isEmpty ? value : '$value $unit',
          style: theme.textTheme.headlineSmall,
        ),
      ],
    );
  }
}
