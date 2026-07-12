import 'package:flutter/material.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../domain/model/ble_sensor_models.dart';
import '../../../l10n/app_localizations.dart';
import '../application/ble_devices_view_model.dart';

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
    _notifier.closeAddFlow();
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
        padding: const EdgeInsets.symmetric(vertical: 12),
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
                            battery != null
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
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  children: [
                    for (final capability in device.capabilities)
                      Chip(label: Text(capabilityLabel(l10n, capability))),
                  ],
                ),
                Align(
                  alignment: Alignment.centerLeft,
                  child: TextButton.icon(
                    onPressed: onRemove,
                    icon: const Icon(Icons.delete_outline, size: 18),
                    label: Text(l10n.settingsSensorsRemoveDevice),
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
              if (state.isDiscoveringCapabilities)
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
          onPressed: () => Navigator.of(context).pop(),
          child: Text(l10n.actionCancel),
        ),
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
          child: Text(l10n.settingsSensorsRemoveDevice),
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
