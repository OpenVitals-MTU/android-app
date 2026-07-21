import 'package:flutter/material.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../domain/model/ble_sensor_models.dart';
import '../../../domain/usecase/onboard_garmin_watch_use_case.dart';
import '../../../l10n/app_localizations.dart';
import '../application/ble_devices_view_model.dart';
import '../../../ui/components/screen_scroll_padding.dart';

/// The Sensors settings screen: list paired BLE sensors (enable / edit / remove)
/// and add new ones through a scan → capability-discovery → pair flow.
///
/// Port of the Kotlin `BleDevicesSettingsScreen` + `BleDevicesViewModel`. The
/// scan/connect stack is device-dependent; this screen drives the ported
/// [BleDevicesViewModel] over `flutter_blue_plus`.
class BleDevicesScreen extends ConsumerStatefulWidget {
  const BleDevicesScreen({super.key});

  @override
  ConsumerState<BleDevicesScreen> createState() => _BleDevicesScreenState();
}

class _BleDevicesScreenState extends ConsumerState<BleDevicesScreen> {
  late final BleDevicesViewModel _notifier;

  @override
  void initState() {
    super.initState();
    // Capture the (long-lived) notifier now: `ref` is unsafe in dispose().
    _notifier = ref.read(bleDevicesViewModelProvider.notifier);
    // Kotlin `DisposableEffect(Unit) { refresh() }`.
    WidgetsBinding.instance.addPostFrameCallback((_) => _notifier.refresh());
  }

  @override
  void dispose() {
    // Kotlin `onDispose { stopScan() }`; also reset any open flow so re-entry
    // starts clean (hiltViewModel is screen-scoped in Kotlin).
    _notifier
      ..closeAddFlow()
      ..closeEditDevice();
    super.dispose();
  }

  Future<void> _startAddFlow() async {
    if (!await _notifier.beginAddFlow()) return;
    if (!mounted) return;
    await showDialog<void>(
      context: context,
      builder: (_) => const _AddDeviceDialog(),
    );
    // Told once, after the sheet closes, rather than inside it: the user has
    // already answered the companion dialog by then, and a declined association
    // costs them nothing until a sync actually runs in the background.
    final withoutCompanion = ref
        .read(bleDevicesViewModelProvider)
        .watchOnboardedWithoutCompanion;
    _notifier.closeAddFlow();
    if (withoutCompanion && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            AppLocalizations.of(context).settingsWatchNoCompanionNotice,
          ),
        ),
      );
    }
  }

  Future<void> _startEditFlow(String deviceId) async {
    _notifier.openEditDevice(deviceId);
    await showDialog<void>(
      context: context,
      builder: (_) => _EditDeviceDialog(deviceId: deviceId),
    );
    _notifier.closeEditDevice();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final devices = ref.watch(
      bleDevicesViewModelProvider.select((s) => s.devices),
    );

    return Scaffold(
      appBar: AppBar(title: Text(l10n.settingsSensorsGroupTitle)),
      body: ListView(
        padding: screenScrollPadding(context, vertical: 12),
        children: [
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            child: Text(
              l10n.settingsSensorsGroupBody,
              style: theme.textTheme.bodyMedium
                  ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ),
          const SizedBox(height: 12),
          if (devices.isEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        l10n.settingsSensorsEmptyTitle,
                        style: theme.textTheme.titleSmall,
                      ),
                      const SizedBox(height: 8),
                      Text(
                        l10n.settingsSensorsEmptyBody,
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.onSurfaceVariant,
                        ),
                      ),
                      const SizedBox(height: 12),
                      FilledButton.icon(
                        onPressed: _startAddFlow,
                        icon: const Icon(Icons.add, size: 18),
                        label: Text(l10n.settingsSensorsAddDevice),
                      ),
                    ],
                  ),
                ),
              ),
            )
          else ...[
            for (final device in devices)
              _BleDeviceRow(
                device: device,
                onToggleEnabled: (enabled) =>
                    _notifier.setDeviceEnabled(device.id, enabled),
                onEdit: () => _startEditFlow(device.id),
                onRemove: () => _notifier.removeDevice(device.id),
              ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 4, 16, 0),
              child: OutlinedButton.icon(
                onPressed: _startAddFlow,
                icon: const Icon(Icons.add, size: 18),
                label: Text(l10n.settingsSensorsAddDevice),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _BleDeviceRow extends StatelessWidget {
  const _BleDeviceRow({
    required this.device,
    required this.onToggleEnabled,
    required this.onEdit,
    required this.onRemove,
  });

  final BleSensorDevice device;
  final ValueChanged<bool> onToggleEnabled;
  final VoidCallback onEdit;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final battery = device.batteryPercent;
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
      child: Card(
        child: InkWell(
          onTap: onEdit,
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(device.displayName,
                              style: theme.textTheme.titleSmall),
                          Text(
                            device.bluetoothName ?? device.address,
                            style: theme.textTheme.bodySmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                          Text(
                            // A watch reports its battery over GFDI during a
                            // sync, not over the standard battery service the
                            // sensor path polls — so until it has synced once
                            // there is genuinely nothing to show, and its sync
                            // time is the more useful line anyway.
                            device.isWatch
                                ? _lastSyncedLabel(context, l10n, device)
                                : battery != null
                                    ? l10n.settingsSensorsBatteryPercent(battery)
                                    : l10n.settingsSensorsBatteryUnknown,
                            style: theme.textTheme.labelSmall?.copyWith(
                              color: theme.colorScheme.onSurfaceVariant,
                            ),
                          ),
                        ],
                      ),
                    ),
                    Switch(value: device.enabled, onChanged: onToggleEnabled),
                  ],
                ),
                if (!device.isWatch) ...[
                  const SizedBox(height: 8),
                  Wrap(
                    spacing: 8,
                    children: [
                      for (final capability in device.capabilities)
                        Chip(label: Text(capabilityLabel(l10n, capability))),
                    ],
                  ),
                ],
                Align(
                  alignment: Alignment.centerLeft,
                  child: TextButton.icon(
                    onPressed: onRemove,
                    icon: const Icon(Icons.delete_outline, size: 18),
                    label: Text(
                      device.isWatch
                          ? l10n.settingsWatchRemove
                          : l10n.settingsSensorsRemoveDevice,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

/// A watch's sync line: the local date and time of the last successful sync, or
/// "Never synced". Stored in UTC (as everything in the registry is), rendered in
/// the user's own zone.
String _lastSyncedLabel(
  BuildContext context,
  AppLocalizations l10n,
  BleSensorDevice device,
) {
  final syncedAt = device.lastSyncedAt;
  if (syncedAt == null) return l10n.settingsWatchNeverSynced;
  final locale = Localizations.localeOf(context).toLanguageTag();
  final local = syncedAt.toLocal();
  return l10n.settingsWatchLastSynced(
    '${DateFormat.yMMMd(locale).format(local)} '
    '${DateFormat.jm(locale).format(local)}',
  );
}

class _AddDeviceDialog extends ConsumerStatefulWidget {
  const _AddDeviceDialog();

  @override
  ConsumerState<_AddDeviceDialog> createState() => _AddDeviceDialogState();
}

class _AddDeviceDialogState extends ConsumerState<_AddDeviceDialog> {
  final _nameController = TextEditingController();
  final _wheelController = TextEditingController();

  @override
  void initState() {
    super.initState();
    final state = ref.read(bleDevicesViewModelProvider);
    _nameController.text = state.addDisplayName;
    _wheelController.text = state.addWheelCircumferenceMm;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _wheelController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final notifier = ref.read(bleDevicesViewModelProvider.notifier);
    final state = ref.watch(bleDevicesViewModelProvider);

    // Keep the display-name field in sync when selecting a device sets it,
    // without clobbering the user's own edits.
    if (state.addDisplayName != _nameController.text) {
      _nameController.value = TextEditingValue(
        text: state.addDisplayName,
        selection:
            TextSelection.collapsed(offset: state.addDisplayName.length),
      );
    }

    return AlertDialog(
      title: Text(l10n.settingsSensorsAddDevice),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Flexible(
                  child: Text(
                    state.isScanning
                        ? l10n.settingsSensorsScanning
                        : l10n.settingsSensorsScanStopped,
                    style: theme.textTheme.bodySmall,
                  ),
                ),
                FilterChip(
                  selected: state.showAllDevices,
                  onSelected: notifier.setShowAllDevices,
                  label: Text(l10n.settingsSensorsShowAllDevices),
                ),
              ],
            ),
            const SizedBox(height: 8),
            if (state.discoveredDevices.isEmpty)
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(Icons.bluetooth_searching,
                      color: theme.colorScheme.primary),
                  const SizedBox(height: 8),
                  Text(l10n.settingsSensorsScanEmpty,
                      style: theme.textTheme.bodySmall),
                  TextButton(
                    onPressed: () async {
                      try {
                        await FlutterBluePlus.turnOn();
                      } catch (_) {
                        // Best-effort; ignore if unsupported/denied.
                      }
                    },
                    child: Text(l10n.settingsSensorsOpenBluetooth),
                  ),
                ],
              )
            else
              for (final device in state.discoveredDevices)
                Padding(
                  padding: const EdgeInsets.only(bottom: 8),
                  child: OutlinedButton(
                    onPressed: () => notifier.selectDiscoveredDevice(device),
                    child: SizedBox(
                      width: double.infinity,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(device.name ?? device.address),
                          Text(device.address,
                              style: theme.textTheme.labelSmall),
                        ],
                      ),
                    ),
                  ),
                ),
            if (state.selectedDevice != null) ...[
              const SizedBox(height: 8),
              TextField(
                controller: _nameController,
                onChanged: notifier.updateAddDisplayName,
                decoration: InputDecoration(
                  labelText: l10n.settingsSensorsDeviceName,
                ),
              ),
              const SizedBox(height: 12),
              // A watch has no capabilities to pick and no conflicts to resolve
              // — it is a file source, not a live sensor. What it needs instead
              // is the two OS dialogs, named before they appear.
              if (state.isAddingWatch)
                _WatchPairSteps(step: state.onboardStep)
              else if (state.isDiscoveringCapabilities)
                Text(l10n.settingsSensorsDiscovering)
              else ...[
                Text(l10n.settingsSensorsCapabilitiesTitle,
                    style: theme.textTheme.labelLarge),
                const SizedBox(height: 4),
                _CapabilityChips(
                  selected: state.addCapabilities,
                  onToggle: notifier.toggleAddCapability,
                ),
                _ConflictMessages(conflicts: state.capabilityConflicts),
                if (state.addCapabilities
                    .contains(BleSensorCapability.cyclingSpeedDistance)) ...[
                  const SizedBox(height: 8),
                  TextField(
                    controller: _wheelController,
                    keyboardType: TextInputType.number,
                    onChanged: notifier.updateAddWheelCircumference,
                    decoration: InputDecoration(
                      labelText: l10n.settingsSensorsWheelCircumference,
                    ),
                  ),
                ],
              ],
              if (state.errorMessage != null) ...[
                const SizedBox(height: 8),
                Text(state.errorMessage!,
                    style: theme.textTheme.bodySmall
                        ?.copyWith(color: theme.colorScheme.error)),
              ],
            ],
          ],
        ),
      ),
      actions: [
        TextButton(
          // Bonding puts system dialogs over this one and cannot be taken back
          // halfway; cancelling underneath it would leave the watch half-paired.
          onPressed: state.isOnboardingWatch
              ? null
              : () => Navigator.of(context).pop(),
          child: Text(l10n.actionCancel),
        ),
        if (state.isAddingWatch)
          TextButton(
            onPressed: state.isOnboardingWatch
                ? null
                : () async {
                    // The dialog owns the pop, not the view-model: a refused
                    // pairing must leave the sheet open so the user can retry
                    // without re-scanning.
                    if (await notifier.onboardSelectedWatch() &&
                        context.mounted) {
                      Navigator.of(context).pop();
                    }
                  },
            child: Text(l10n.settingsWatchPairAction),
          )
        else
          TextButton(
            onPressed: (state.selectedDevice != null &&
                    state.addCapabilities.isNotEmpty &&
                    !state.isDiscoveringCapabilities)
                ? () {
                    notifier.saveAddedDevice();
                    Navigator.of(context).pop();
                  }
                : null,
            child: Text(l10n.actionSave),
          ),
      ],
    );
  }
}

/// The two platform steps of watch onboarding, shown as a checklist so the user
/// knows which system dialog is theirs to answer. [step] is null before pairing
/// starts and again once it finishes.
class _WatchPairSteps extends StatelessWidget {
  const _WatchPairSteps({required this.step});

  final GarminOnboardStep? step;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(l10n.settingsWatchPairTitle, style: theme.textTheme.labelLarge),
        const SizedBox(height: 4),
        Text(
          l10n.settingsWatchPairBody,
          style: theme.textTheme.bodySmall
              ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
        ),
        const SizedBox(height: 12),
        _WatchPairStepRow(
          label: l10n.settingsWatchStepBonding,
          active: step == GarminOnboardStep.bonding,
          done: step == GarminOnboardStep.associating,
        ),
        _WatchPairStepRow(
          label: l10n.settingsWatchStepAssociating,
          active: step == GarminOnboardStep.associating,
          done: false,
        ),
        Padding(
          padding: const EdgeInsets.only(left: 32, top: 2),
          child: Text(
            l10n.settingsWatchStepAssociatingHint,
            style: theme.textTheme.labelSmall
                ?.copyWith(color: theme.colorScheme.onSurfaceVariant),
          ),
        ),
      ],
    );
  }
}

class _WatchPairStepRow extends StatelessWidget {
  const _WatchPairStepRow({
    required this.label,
    required this.active,
    required this.done,
  });

  final String label;
  final bool active;
  final bool done;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        children: [
          SizedBox(
            width: 24,
            height: 24,
            child: Center(
              child: switch ((active, done)) {
                (true, _) => const SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                (_, true) => Icon(
                    Icons.check_circle_outline,
                    size: 18,
                    color: theme.colorScheme.primary,
                  ),
                _ => Icon(
                    Icons.radio_button_unchecked,
                    size: 18,
                    color: theme.colorScheme.onSurfaceVariant,
                  ),
              },
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              label,
              style: active
                  ? theme.textTheme.bodyMedium
                  : theme.textTheme.bodyMedium?.copyWith(
                      color: theme.colorScheme.onSurfaceVariant,
                    ),
            ),
          ),
        ],
      ),
    );
  }
}

class _EditDeviceDialog extends ConsumerStatefulWidget {
  const _EditDeviceDialog({required this.deviceId});

  final String deviceId;

  @override
  ConsumerState<_EditDeviceDialog> createState() => _EditDeviceDialogState();
}

class _EditDeviceDialogState extends ConsumerState<_EditDeviceDialog> {
  final _nameController = TextEditingController();
  final _wheelController = TextEditingController();

  @override
  void initState() {
    super.initState();
    final state = ref.read(bleDevicesViewModelProvider);
    _nameController.text = state.editDisplayName;
    _wheelController.text = state.editWheelCircumferenceMm;
  }

  @override
  void dispose() {
    _nameController.dispose();
    _wheelController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    final notifier = ref.read(bleDevicesViewModelProvider.notifier);
    final state = ref.watch(bleDevicesViewModelProvider);

    return AlertDialog(
      title: Text(l10n.settingsSensorsEditDevice),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            TextField(
              controller: _nameController,
              onChanged: notifier.updateEditDisplayName,
              decoration: InputDecoration(
                labelText: l10n.settingsSensorsDeviceName,
              ),
            ),
            const SizedBox(height: 12),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(l10n.settingsSensorsEnabled),
                Switch(
                  value: state.editEnabled,
                  onChanged: notifier.setEditEnabled,
                ),
              ],
            ),
            // A watch owns no capabilities, so the picker below would offer it
            // roles it cannot fill — and saving one would put it into the
            // recording coordinator's assignment map.
            if (!state.isEditingWatch) ...[
              const SizedBox(height: 8),
              Text(l10n.settingsSensorsCapabilitiesTitle,
                  style: theme.textTheme.labelLarge),
              const SizedBox(height: 4),
              _CapabilityChips(
                selected: state.editCapabilities,
                onToggle: notifier.toggleEditCapability,
              ),
              _ConflictMessages(conflicts: state.capabilityConflicts),
              if (state.editCapabilities
                  .contains(BleSensorCapability.cyclingSpeedDistance)) ...[
                const SizedBox(height: 8),
                TextField(
                  controller: _wheelController,
                  keyboardType: TextInputType.number,
                  onChanged: notifier.updateEditWheelCircumference,
                  decoration: InputDecoration(
                    labelText: l10n.settingsSensorsWheelCircumference,
                  ),
                ),
              ],
            ],
            if (state.errorMessage != null) ...[
              const SizedBox(height: 8),
              Text(state.errorMessage!,
                  style: theme.textTheme.bodySmall
                      ?.copyWith(color: theme.colorScheme.error)),
            ],
          ],
        ),
      ),
      actions: [
        TextButton(
          onPressed: () {
            notifier.removeDevice(widget.deviceId);
            Navigator.of(context).pop();
          },
          child: Text(
            state.isEditingWatch
                ? l10n.settingsWatchRemove
                : l10n.settingsSensorsRemoveDevice,
          ),
        ),
        TextButton(
          onPressed: () {
            notifier.saveEditedDevice();
            if (ref.read(bleDevicesViewModelProvider).editingDeviceId == null) {
              Navigator.of(context).pop();
            }
          },
          child: Text(l10n.actionSave),
        ),
      ],
    );
  }
}

class _CapabilityChips extends StatelessWidget {
  const _CapabilityChips({required this.selected, required this.onToggle});

  final Set<BleSensorCapability> selected;
  final ValueChanged<BleSensorCapability> onToggle;

  @override
  Widget build(BuildContext context) {
    final l10n = AppLocalizations.of(context);
    return Wrap(
      spacing: 8,
      children: [
        for (final capability in BleSensorCapability.values)
          FilterChip(
            selected: selected.contains(capability),
            onSelected: (_) => onToggle(capability),
            label: Text(capabilityLabel(l10n, capability)),
          ),
      ],
    );
  }
}

class _ConflictMessages extends StatelessWidget {
  const _ConflictMessages({required this.conflicts});

  final Map<BleSensorCapability, BleSensorDevice> conflicts;

  @override
  Widget build(BuildContext context) {
    if (conflicts.isEmpty) return const SizedBox.shrink();
    final l10n = AppLocalizations.of(context);
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        for (final entry in conflicts.entries)
          Padding(
            padding: const EdgeInsets.only(top: 4),
            child: Text(
              l10n.settingsSensorsCapabilityConflict(
                capabilityLabel(l10n, entry.key),
                entry.value.displayName,
              ),
              style: theme.textTheme.bodySmall
                  ?.copyWith(color: theme.colorScheme.error),
            ),
          ),
      ],
    );
  }
}

/// Kotlin `capabilityLabel`.
String capabilityLabel(AppLocalizations l10n, BleSensorCapability capability) {
  switch (capability) {
    case BleSensorCapability.heartRate:
      return l10n.settingsSensorsCapabilityHeartRate;
    case BleSensorCapability.cyclingCadence:
      return l10n.settingsSensorsCapabilityCyclingCadence;
    case BleSensorCapability.cyclingPower:
      return l10n.settingsSensorsCapabilityCyclingPower;
    case BleSensorCapability.cyclingSpeedDistance:
      return l10n.settingsSensorsCapabilityCyclingSpeed;
    case BleSensorCapability.runningSpeedCadence:
      return l10n.settingsSensorsCapabilityRunningSpeedCadence;
  }
}
