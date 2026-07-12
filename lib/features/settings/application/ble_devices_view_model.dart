import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../di/providers.dart';
import '../../../domain/model/ble_sensor_models.dart';
import '../../../domain/usecase/edit_ble_device_registry_use_case.dart';
import '../../../data/source/sensors/ble/ble_sensor_coordinator.dart';

part 'ble_devices_view_model.freezed.dart';

/// Riverpod port of the Kotlin `BleDevicesUiState`.
///
/// Freezed's `copyWith` sets a nullable field to null when you PASS null, so the
/// hand-written `_unset` sentinel this class used to carry is gone: `copyWith`
/// alone now expresses both "leave unchanged" (omit) and "clear" (pass null).
@freezed
abstract class BleDevicesUiState with _$BleDevicesUiState {
  const BleDevicesUiState._();

  const factory BleDevicesUiState({
    @Default(<BleSensorDevice>[]) List<BleSensorDevice> devices,
    @Default(<BleDiscoveredDevice>[])
    List<BleDiscoveredDevice> discoveredDevices,
    @Default(false) bool isScanning,
    @Default(false) bool showAllDevices,
    BleDiscoveredDevice? selectedDevice,
    @Default(<BleSensorCapability>{})
    Set<BleSensorCapability> discoveredCapabilities,
    @Default(false) bool isDiscoveringCapabilities,
    @Default('') String addDisplayName,
    @Default(<BleSensorCapability>{}) Set<BleSensorCapability> addCapabilities,
    @Default('') String addWheelCircumferenceMm,
    @Default(<BleSensorCapability, BleSensorDevice>{})
    Map<BleSensorCapability, BleSensorDevice> capabilityConflicts,
    String? editingDeviceId,
    @Default('') String editDisplayName,
    @Default(<BleSensorCapability>{}) Set<BleSensorCapability> editCapabilities,
    @Default(true) bool editEnabled,
    @Default('') String editWheelCircumferenceMm,
    String? errorMessage,
    @Default(false) bool showAddFlow,
  }) = _BleDevicesUiState;

  int get enabledDeviceCount => devices.where((d) => d.enabled).length;
}

/// Riverpod port of the Kotlin `BleDevicesViewModel`.
///
/// The paired-device list and scan results come from the repository and
/// coordinator streams (merged in [build]); every other field is local UI state
/// held in [_local] and preserved across stream emissions.
class BleDevicesViewModel extends Notifier<BleDevicesUiState> {
  BleDevicesUiState _local = const BleDevicesUiState();
  int _discoverGeneration = 0;

  BleSensorCoordinator get _coordinator =>
      ref.read(bleSensorCoordinatorProvider);

  /// Every registry call below is synchronous, deliberately: the conflict map has
  /// to land in the same frame as the checkbox that caused it. See
  /// [ResolveBleCapabilityConflictsUseCase].
  Map<BleSensorCapability, BleSensorDevice> _conflicts(
    Set<BleSensorCapability> capabilities, {
    String? excludingDeviceId,
  }) =>
      ref.read(resolveBleCapabilityConflictsUseCaseProvider)(
        capabilities,
        excludingDeviceId: excludingDeviceId,
      );

  void _edit(BleDeviceRegistryEdit edit) =>
      ref.read(editBleDeviceRegistryUseCaseProvider)(edit);

  @override
  BleDevicesUiState build() {
    final devices =
        ref.watch(bleDevicesProvider).value ?? const <BleSensorDevice>[];
    final discovered = ref.watch(bleDiscoveredDevicesProvider).value ??
        const <BleDiscoveredDevice>[];
    return _local.copyWith(devices: devices, discoveredDevices: discovered);
  }

  void _setLocal(BleDevicesUiState next) {
    _local = next;
    state = next.copyWith(
      devices: state.devices,
      discoveredDevices: state.discoveredDevices,
    );
  }

  void refresh() => ref.read(refreshBleDeviceRegistryUseCaseProvider)();

  void openAddFlow() {
    _setLocal(_local.copyWith(
      showAddFlow: true,
      selectedDevice: null,
      discoveredCapabilities: const {},
      addDisplayName: '',
      addCapabilities: const {},
      addWheelCircumferenceMm:
          BleSensorDevice.defaultWheelCircumferenceMm.toString(),
      capabilityConflicts: const {},
      errorMessage: null,
    ));
  }

  void closeAddFlow() {
    stopScan();
    _setLocal(_local.copyWith(
      showAddFlow: false,
      selectedDevice: null,
      discoveredCapabilities: const {},
      isDiscoveringCapabilities: false,
      errorMessage: null,
    ));
  }

  void setShowAllDevices(bool enabled) {
    _setLocal(_local.copyWith(showAllDevices: enabled));
    if (state.isScanning) startScan();
  }

  void startScan() {
    _setLocal(_local.copyWith(isScanning: true, errorMessage: null));
    unawaited(_coordinator.startScan(showAllDevices: state.showAllDevices));
  }

  void stopScan() {
    unawaited(_coordinator.stopScan());
    _setLocal(_local.copyWith(isScanning: false));
  }

  Future<void> selectDiscoveredDevice(BleDiscoveredDevice device) async {
    _setLocal(_local.copyWith(
      selectedDevice: device,
      addDisplayName: device.name ?? device.address,
      addCapabilities: device.suggestedCapabilities,
      isDiscoveringCapabilities: true,
      errorMessage: null,
    ));
    final generation = ++_discoverGeneration;
    // Connecting to read the device's real capabilities, falling back to the
    // advertised ones, and pricing them against the already-paired sensors is
    // the use case's business — see [DiscoverBleDeviceCapabilitiesUseCase].
    final discovery =
        await ref.read(discoverBleDeviceCapabilitiesUseCaseProvider)(device);
    if (!ref.mounted || generation != _discoverGeneration) return;
    _setLocal(_local.copyWith(
      discoveredCapabilities: discovery.capabilities,
      addCapabilities: discovery.capabilities,
      capabilityConflicts: discovery.conflicts,
      isDiscoveringCapabilities: false,
    ));
  }

  void updateAddDisplayName(String value) =>
      _setLocal(_local.copyWith(addDisplayName: value));

  void toggleAddCapability(BleSensorCapability capability) {
    final next = {...state.addCapabilities};
    if (!next.remove(capability)) next.add(capability);
    _setLocal(_local.copyWith(
      addCapabilities: next,
      capabilityConflicts: _conflicts(next),
    ));
  }

  void updateAddWheelCircumference(String value) =>
      _setLocal(_local.copyWith(addWheelCircumferenceMm: value));

  void saveAddedDevice() {
    final s = state;
    final selected = s.selectedDevice;
    if (selected == null) return;
    if (s.addCapabilities.isEmpty) {
      _setLocal(_local.copyWith(errorMessage: 'Select at least one capability.'));
      return;
    }
    final wheelCircumference =
        s.addCapabilities.contains(BleSensorCapability.cyclingSpeedDistance)
            ? (int.tryParse(s.addWheelCircumferenceMm) ??
                BleSensorDevice.defaultWheelCircumferenceMm)
            : null;
    _edit(PairBleDevice(
      displayName: s.addDisplayName,
      address: selected.address,
      bluetoothName: selected.name,
      capabilities: s.addCapabilities,
      wheelCircumferenceMm: wheelCircumference,
    ));
    stopScan();
    closeAddFlow();
  }

  void openEditDevice(String deviceId) {
    // The registry snapshot, not the streamed copy: the form must prefill from
    // what is stored right now.
    final devices = ref.read(readPairedBleDevicesUseCaseProvider)();
    final matches = devices.where((d) => d.id == deviceId);
    if (matches.isEmpty) return;
    final device = matches.first;
    _setLocal(_local.copyWith(
      editingDeviceId: device.id,
      editDisplayName: device.displayName,
      editCapabilities: device.capabilities,
      editEnabled: device.enabled,
      editWheelCircumferenceMm: device.wheelCircumferenceMm?.toString() ??
          BleSensorDevice.defaultWheelCircumferenceMm.toString(),
      capabilityConflicts: _conflicts(
        device.capabilities,
        excludingDeviceId: device.id,
      ),
      errorMessage: null,
    ));
  }

  void closeEditDevice() {
    _setLocal(_local.copyWith(
      editingDeviceId: null,
      capabilityConflicts: const {},
      errorMessage: null,
    ));
  }

  void updateEditDisplayName(String value) =>
      _setLocal(_local.copyWith(editDisplayName: value));

  void toggleEditCapability(BleSensorCapability capability) {
    final next = {...state.editCapabilities};
    if (!next.remove(capability)) next.add(capability);
    _setLocal(_local.copyWith(
      editCapabilities: next,
      capabilityConflicts: _conflicts(
        next,
        excludingDeviceId: state.editingDeviceId,
      ),
    ));
  }

  void setEditEnabled(bool enabled) =>
      _setLocal(_local.copyWith(editEnabled: enabled));

  void updateEditWheelCircumference(String value) =>
      _setLocal(_local.copyWith(editWheelCircumferenceMm: value));

  void saveEditedDevice() {
    final s = state;
    final deviceId = s.editingDeviceId;
    if (deviceId == null) return;
    if (s.editCapabilities.isEmpty) {
      _setLocal(_local.copyWith(errorMessage: 'Select at least one capability.'));
      return;
    }
    final wheelCircumference =
        s.editCapabilities.contains(BleSensorCapability.cyclingSpeedDistance)
            ? (int.tryParse(s.editWheelCircumferenceMm) ??
                BleSensorDevice.defaultWheelCircumferenceMm)
            : null;
    _edit(UpdateBleDevice(
      deviceId: deviceId,
      displayName: s.editDisplayName,
      capabilities: s.editCapabilities,
      enabled: s.editEnabled,
      wheelCircumferenceMm: wheelCircumference,
    ));
    closeEditDevice();
  }

  void removeDevice(String deviceId) {
    _edit(ForgetBleDevice(deviceId));
    if (state.editingDeviceId == deviceId) closeEditDevice();
  }

  void setDeviceEnabled(String deviceId, bool enabled) =>
      _edit(SetBleDeviceEnabled(deviceId, enabled));
}

final bleDevicesViewModelProvider =
    NotifierProvider<BleDevicesViewModel, BleDevicesUiState>(
  BleDevicesViewModel.new,
);
