import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../data/local/open_vitals_database.dart';
import '../../../domain/model/ble_sensor_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../navigation/app_routes.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import '../application/ble_devices_view_model.dart';
import '../application/garmin_sync_view_model.dart';
import '../application/watch_metrics_view_model.dart';
import 'watch_common.dart';

/// One watch, and everything about it.
///
/// Reached from the Watches list and from the summary tile — deliberately the
/// same screen, so a watch has one home rather than controls scattered across a
/// list row, a tile and a sheet.
///
/// The order is fixed and means something: **status** (what you came to check),
/// **actions** (verbs, as icons), **latest** (values, as rows), **configuration**
/// (least often touched, so last, with removal at the bottom where a mis-tap
/// cannot reach it).
class WatchDeviceScreen extends ConsumerWidget {
  const WatchDeviceScreen({required this.deviceId, super.key});

  final String deviceId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final devices = ref.watch(
      bleDevicesViewModelProvider.select((s) => s.devices),
    );
    final device = devices.where((d) => d.id == deviceId).firstOrNull;

    if (device == null) {
      // Removed while this screen was open, or opened from a stale tile.
      return Scaffold(
        appBar: AppBar(title: Text(l10n.settingsSensorsGroupTitle)),
        body: Center(child: Text(l10n.noData)),
      );
    }

    final sync = ref.watch(garminSyncViewModelProvider);
    final metrics = ref.watch(watchMetricsProvider).asData?.value;

    return Scaffold(
      appBar: AppBar(title: Text(device.displayName)),
      body: ListView(
        padding: screenScrollPadding(context),
        children: [
          _StatusCard(device: device),
          const SizedBox(height: 8),
          _Actions(device: device, sync: sync),
          if (metrics != null && !metrics.isEmpty) ...[
            _SectionHeader(title: l10n.settingsWatchSectionLatest),
            ..._latestRows(context, metrics),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
              child: OutlinedButton(
                onPressed: () =>
                    context.push(AppRoutes.watchDataLocation(device.id)),
                child: Text(l10n.settingsWatchAllData),
              ),
            ),
          ],
          _SectionHeader(title: l10n.settingsWatchSectionDevice),
          _DeviceSettings(device: device),
        ],
      ),
    );
  }

  List<Widget> _latestRows(BuildContext context, WatchMetrics metrics) {
    final l10n = AppLocalizations.of(context);
    final rows = <Widget>[];

    final score = metrics[GarminWellnessMetric.sleepScore];
    if (score != null) {
      final awake = metrics.valueOf(GarminWellnessMetric.sleepAwakeSeconds);
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricSleepScore,
        supporting: awake == null
            ? null
            : '${l10n.settingsWatchMetricAwake} '
                '${formatWatchDuration(l10n, Duration(seconds: awake))}',
        value: '${score.value}',
      ));
    }
    final energy = metrics[GarminWellnessMetric.bodyEnergy];
    if (energy != null) {
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricBodyBattery,
        value: '${energy.value}',
      ));
    }
    final stress = metrics[GarminWellnessMetric.stress];
    if (stress != null) {
      rows.add(WatchValueRow(
        label: l10n.settingsWatchMetricStress,
        value: '${stress.value}',
      ));
    }
    return rows;
  }
}

class _StatusCard extends StatelessWidget {
  const _StatusCard({required this.device});

  final BleSensorDevice device;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final battery = device.batteryPercent;
    final syncedAt = device.lastSyncedAt;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              const WatchAvatar(),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      device.enabled
                          ? l10n.settingsWatchConnected
                          : l10n.settingsWatchNotConnected,
                      style: theme.textTheme.titleMedium,
                    ),
                    Text(
                      syncedAt == null
                          ? l10n.settingsWatchNeverSynced
                          : l10n.settingsWatchLastSynced(
                              formatWatchSyncTime(context, syncedAt),
                            ),
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              if (battery != null)
                Text('$battery%', style: theme.textTheme.titleMedium),
            ],
          ),
        ),
      ),
    );
  }
}

class _Actions extends ConsumerWidget {
  const _Actions({required this.device, required this.sync});

  final BleSensorDevice device;
  final GarminSyncState sync;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final busy = sync.isSyncing;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 0),
      child: Row(
        children: [
          WatchAction(
            icon: Icons.insights_outlined,
            label: l10n.settingsWatchActionData,
            onPressed: () =>
                context.push(AppRoutes.watchDataLocation(device.id)),
          ),
          const SizedBox(width: 12),
          WatchAction(
            icon: Icons.sync,
            label: l10n.settingsWatchActionSync,
            busy: sync.isSyncingDevice(device.id),
            // One radio, one sync: disabled while ANY watch is syncing, the
            // same rule the summary tile uses, because they share this state.
            onPressed: busy
                ? null
                : () => ref
                    .read(garminSyncViewModelProvider.notifier)
                    .syncDevice(device.id),
            // Debug-only diagnostic: sync, then hold the link open so what the
            // watch sends unprompted can be read from the log. Deliberately
            // undiscoverable — it pins the radio for minutes.
            onLongPress: !kDebugMode || busy
                ? null
                : () => ref
                    .read(garminSyncViewModelProvider.notifier)
                    .syncDevice(device.id,
                        listenAfter: const Duration(minutes: 10)),
          ),
        ],
      ),
    );
  }
}

class _DeviceSettings extends ConsumerWidget {
  const _DeviceSettings({required this.device});

  final BleSensorDevice device;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final notifier = ref.read(bleDevicesViewModelProvider.notifier);
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
          child: Card(
            child: SwitchListTile(
              title: Text(l10n.settingsWatchEnabled),
              subtitle: Text(l10n.settingsWatchEnabledBody),
              value: device.enabled,
              onChanged: (value) => notifier.setDeviceEnabled(device.id, value),
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
          child: Card(
            child: ListTile(
              title: Text(l10n.settingsWatchNameLabel),
              subtitle: Text(device.displayName),
              trailing: const Icon(Icons.chevron_right),
              onTap: () => _rename(context, ref, device),
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
          child: Card(
            child: ListTile(
              title: Text(
                l10n.settingsWatchRemove,
                style: TextStyle(color: theme.colorScheme.error),
              ),
              onTap: () {
                notifier.removeDevice(device.id);
                context.pop();
              },
            ),
          ),
        ),
      ],
    );
  }

  Future<void> _rename(
    BuildContext context,
    WidgetRef ref,
    BleSensorDevice device,
  ) async {
    final l10n = AppLocalizations.of(context);
    final controller = TextEditingController(text: device.displayName);
    final name = await showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(l10n.settingsWatchRename),
        content: TextField(
          controller: controller,
          autofocus: true,
          decoration: InputDecoration(labelText: l10n.settingsWatchNameLabel),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: Text(l10n.actionCancel),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(controller.text.trim()),
            child: Text(l10n.actionSave),
          ),
        ],
      ),
    );
    controller.dispose();
    if (name == null || name.isEmpty) return;
    ref.read(bleDevicesViewModelProvider.notifier).renameDevice(device.id, name);
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 20, 16, 8),
      child: Text(
        title,
        style: theme.textTheme.titleSmall?.copyWith(
          color: theme.colorScheme.onSurfaceVariant,
        ),
      ),
    );
  }
}
