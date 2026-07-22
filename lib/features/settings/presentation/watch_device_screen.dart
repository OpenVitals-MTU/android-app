import 'package:flutter/foundation.dart' show kDebugMode;
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../../domain/model/ble_sensor_models.dart';
import '../../../l10n/app_localizations.dart';
import '../../../navigation/app_routes.dart';
import '../../../ui/components/screen_scroll_padding.dart';
import '../application/ble_devices_view_model.dart';
import '../application/garmin_sync_view_model.dart';
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

    return Scaffold(
      appBar: AppBar(
        title: Text(device.displayName),
        actions: [
          IconButton(
            onPressed: () => _rename(context, ref, device),
            tooltip: l10n.settingsWatchRename,
            icon: const Icon(Icons.edit_outlined),
          ),
        ],
      ),
      body: ListView(
        padding: screenScrollPadding(context),
        children: [
          _StatusCard(device: device, sync: sync),
          const SizedBox(height: 12),
          _Actions(device: device, sync: sync),
          // No "Latest" band: it showed the same numbers the Data action opens,
          // one tap away, so the screen said everything twice.
          _SectionHeader(title: l10n.settingsWatchSettingsSection),
          const _OnDeviceSettingsRow(),
          _SectionHeader(title: l10n.settingsWatchSectionDevice),
          _DeviceSettings(device: device),
        ],
      ),
    );
  }

  Future<void> _rename(
    BuildContext context,
    WidgetRef ref,
    BleSensorDevice device,
  ) async {
    final name = await showDialog<String>(
      context: context,
      builder: (_) => _RenameDialog(initialName: device.displayName),
    );
    if (name == null || name.isEmpty) return;
    ref.read(bleDevicesViewModelProvider.notifier).renameDevice(device.id, name);
  }

}

class _StatusCard extends StatelessWidget {
  const _StatusCard({required this.device, required this.sync});

  final BleSensorDevice device;
  final GarminSyncState sync;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final battery = device.batteryPercent;
    final syncedAt = device.lastSyncedAt;
    final files = sync.lastFileCount;

    final buffer = StringBuffer();
    if (syncedAt == null) {
      buffer.write(l10n.settingsWatchNeverSynced);
    } else {
      buffer.write(
        l10n.settingsWatchLastSynced(formatWatchSyncTime(context, syncedAt)),
      );
      // Only after a sync THIS session — the count is not persisted, and an
      // invented one would be worse than none.
      if (files != null && files > 0) {
        buffer.write(' · ${l10n.settingsWatchSyncedFiles(files)}');
      }
    }

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Row(
            children: [
              const WatchAvatar(size: 44),
              const SizedBox(width: 14),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      device.enabled
                          ? l10n.settingsWatchConnected
                          : l10n.settingsWatchNotConnected,
                      style: theme.textTheme.titleMedium
                          ?.copyWith(fontWeight: FontWeight.w600),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      buffer.toString(),
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: theme.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
              if (battery != null) ...[
                const SizedBox(width: 12),
                Text(
                  '$battery%',
                  style: theme.textTheme.headlineSmall
                      ?.copyWith(fontWeight: FontWeight.w600),
                ),
              ],
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
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          WatchAction(
            icon: Icons.insights_outlined,
            label: l10n.settingsWatchActionData,
            onPressed: () =>
                context.push(AppRoutes.watchDataLocation(device.id)),
          ),
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
          // Present but disabled, not hidden: setting an alarm means uploading a
          // FIT settings file and finding the watch means a protobuf request,
          // and neither transport exists yet. Showing them greyed says the watch
          // can do this and the app cannot — which is true — where hiding them
          // would say the watch cannot.
          WatchAction(
            icon: Icons.alarm,
            label: l10n.settingsWatchActionAlarms,
            onPressed: null,
          ),
          WatchAction(
            icon: Icons.wifi_tethering,
            label: l10n.settingsWatchActionFind,
            onPressed: null,
          ),
        ],
      ),
    );
  }
}

/// The watch's own settings tree, which arrives over the protobuf settings
/// service. Confirmed present on the device (its handshake asks us about it),
/// but the protobuf layer is not written, so the row is disabled.
class _OnDeviceSettingsRow extends StatelessWidget {
  const _OnDeviceSettingsRow();

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final muted = theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.6);
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
      child: Card(
        child: ListTile(
          enabled: false,
          leading: Icon(Icons.brightness_5_outlined, color: muted),
          title: Text(
            l10n.settingsWatchOnDeviceSettings,
            style: TextStyle(color: muted),
          ),
          subtitle: Text(
            l10n.settingsWatchNotAvailableYet,
            style: theme.textTheme.bodySmall?.copyWith(color: muted),
          ),
          trailing: Icon(Icons.chevron_right, color: muted),
        ),
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
              value: device.enabled,
              onChanged: (value) => notifier.setDeviceEnabled(device.id, value),
            ),
          ),
        ),
        // Last, and its own card: a destructive action wants distance from the
        // switch above it, not adjacency.
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
          child: Card(
            child: ListTile(
              title: Text(
                l10n.settingsWatchRemove,
                style: TextStyle(color: theme.colorScheme.error),
              ),
              onTap: () async {
                final confirmed = await confirmRemoveDevice(
                  context,
                  deviceName: device.displayName,
                  isWatch: true,
                );
                if (!confirmed || !context.mounted) return;
                notifier.removeDevice(device.id);
                context.pop();
              },
            ),
          ),
        ),
      ],
    );
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
      child: Row(
        children: [
          Text(
            title,
            style: theme.textTheme.titleSmall?.copyWith(
              color: theme.colorScheme.onSurfaceVariant,
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Divider(
              height: 1,
              thickness: 1,
              color: theme.colorScheme.outlineVariant,
            ),
          ),
        ],
      ),
    );
  }
}


/// The rename dialog, owning its own controller.
///
/// A StatefulWidget and not a bare `showDialog` closure because the controller
/// has to outlive the route's exit animation: disposing it the moment
/// `showDialog` returned tore it down while the still-mounted TextField was
/// depending on it, which surfaced as an `_dependents.isEmpty` assertion during
/// the dialog's own teardown. Owning it here ties its lifetime to exactly the
/// element that uses it.
class _RenameDialog extends StatefulWidget {
  const _RenameDialog({required this.initialName});

  final String initialName;

  @override
  State<_RenameDialog> createState() => _RenameDialogState();
}

class _RenameDialogState extends State<_RenameDialog> {
  late final TextEditingController _controller =
      TextEditingController(text: widget.initialName);

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _submit() => Navigator.of(context).pop(_controller.text.trim());

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return AlertDialog(
      title: Text(l10n.settingsWatchRename),
      content: TextField(
        controller: _controller,
        autofocus: true,
        textInputAction: TextInputAction.done,
        onSubmitted: (_) => _submit(),
        decoration: InputDecoration(labelText: l10n.settingsWatchNameLabel),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: Text(l10n.actionCancel),
        ),
        TextButton(onPressed: _submit, child: Text(l10n.actionSave)),
      ],
    );
  }
}
