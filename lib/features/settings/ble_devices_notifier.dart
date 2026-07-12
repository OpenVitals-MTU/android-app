import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../data/repository/contract/ble_device_repository.dart';
import '../../di/providers.dart';
import '../../domain/model/ble_sensor_models.dart';
import '../../data/source/sensors/ble/ble_sensor_coordinator.dart';

/// Sentinel so [BleDevicesUiState.copyWith] can distinguish "leave unchanged"
/// from "set to null" on the nullable fields.
const Object _unset = Object();

/// Riverpod port of the Kotlin `BleDevicesUiState`.
class BleDevicesUiState {
  const BleDevicesUiState({
    this.devices = const [],
    this.discoveredDevices = const [],
    this.isScanning = false,
    this.showAllDevices = false,
    this.selectedDevice,
    this.discoveredCapabilities = const {},
    this.isDiscoveringCapabilities = false,
    this.addDisplayName = '',
    this.addCapabilities = const {},
    this.addWheelCircumferenceMm = '',
    this.capabilityConflicts = const {},
    this.editingDeviceId,
    this.editDisplayName = '',
    this.editCapabilities = const {},
    this.editEnabled = true,
    this.editWheelCircumferenceMm = '',
    this.errorMessage,
    this.showAddFlow = false,
  });

  final List<BleSensorDevice> devices;
  final List<BleDiscoveredDevice> discoveredDevices;
  final bool isScanning;
  final bool showAllDevices;
  final BleDiscoveredDevice? selectedDevice;
  final Set<BleSensorCapability> discoveredCapabilities;
  final bool isDiscoveringCapabilities;
  final String addDisplayName;
  final Set<BleSensorCapability> addCapabilities;
  final String addWheelCircumferenceMm;
  final Map<BleSensorCapability, BleSensorDevice> capabilityConflicts;
  final String? editingDeviceId;
  final String editDisplayName;
  final Set<BleSensorCapability> editCapabilities;
  final bool editEnabled;
  final String editWheelCircumferenceMm;
  final String? errorMessage;
  final bool showAddFlow;

  int get enabledDeviceCount => devices.where((d) => d.enabled).length;

  BleDevicesUiState copyWith({
    List<BleSensorDevice>? devices,
    List<BleDiscoveredDevice>? discoveredDevices,
    bool? isScanning,
    bool? showAllDevices,
    Object? selectedDevice = _unset,
    Set<BleSensorCapability>? discoveredCapabilities,
    bool? isDiscoveringCapabilities,
    String? addDisplayName,
    Set<BleSensorCapability>? addCapabilities,
    String? addWheelCircumferenceMm,
    Map<BleSensorCapability, BleSensorDevice>? capabilityConflicts,
    Object? editingDeviceId = _unset,
    String? editDisplayName,
    Set<BleSensorCapability>? editCapabilities,
    bool? editEnabled,
    String? editWheelCircumferenceMm,
    Object? errorMessage = _unset,
    bool? showAddFlow,
  }) {
    return BleDevicesUiState(
      devices: devices ?? this.devices,
      discoveredDevices: discoveredDevices ?? this.discoveredDevices,
      isScanning: isScanning ?? this.isScanning,
      showAllDevices: showAllDevices ?? this.showAllDevices,
      selectedDevice: selectedDevice == _unset
          ? this.selectedDevice
          : selectedDevice as BleDiscoveredDevice?,
      discoveredCapabilities:
          discoveredCapabilities ?? this.discoveredCapabilities,
      isDiscoveringCapabilities:
          isDiscoveringCapabilities ?? this.isDiscoveringCapabilities,
      addDisplayName: addDisplayName ?? this.addDisplayName,
      addCapabilities: addCapabilities ?? this.addCapabilities,
      addWheelCircumferenceMm:
          addWheelCircumferenceMm ?? this.addWheelCircumferenceMm,
      capabilityConflicts: capabilityConflicts ?? this.capabilityConflicts,
      editingDeviceId: editingDeviceId == _unset
          ? this.editingDeviceId
          : editingDeviceId as String?,
      editDisplayName: editDisplayName ?? this.editDisplayName,
      editCapabilities: editCapabilities ?? this.editCapabilities,
      editEnabled: editEnabled ?? this.editEnabled,
      editWheelCircumferenceMm:
          editWheelCircumferenceMm ?? this.editWheelCircumferenceMm,
      errorMessage:
          errorMessage == _unset ? this.errorMessage : errorMessage as String?,
      showAddFlow: showAddFlow ?? this.showAddFlow,
    );
  }
}

/// Riverpod port of the Kotlin `BleDevicesViewModel`.
///
/// The paired-device list and scan results come from the repository and
/// coordinator streams (merged in [build]); every other field is local UI state
/// held in [_local] and preserved across stream emissions.
class BleDevicesNotifier extends Notifier<BleDevicesUiState> {
  BleDevicesUiState _local = const BleDevicesUiState();
  int _discoverGeneration = 0;

  BleSensorCoordinator get _coordinator =>
      ref.read(bleSensorCoordinatorProvider);
  BleDeviceRepository get _repository => ref.read(bleDeviceRepositoryProvider);

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

  void refresh() => _repository.refresh();

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
      capabilityConflicts: _repository.capabilityConflicts(next),
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
    _repository.addDevice(
      displayName: s.addDisplayName,
      address: selected.address,
      bluetoothName: selected.name,
      capabilities: s.addCapabilities,
      wheelCircumferenceMm: wheelCircumference,
    );
    stopScan();
    closeAddFlow();
  }

  void openEditDevice(String deviceId) {
    final matches = _repository.devices.where((d) => d.id == deviceId);
    if (matches.isEmpty) return;
    final device = matches.first;
    _setLocal(_local.copyWith(
      editingDeviceId: device.id,
      editDisplayName: device.displayName,
      editCapabilities: device.capabilities,
      editEnabled: device.enabled,
      editWheelCircumferenceMm: device.wheelCircumferenceMm?.toString() ??
          BleSensorDevice.defaultWheelCircumferenceMm.toString(),
      capabilityConflicts: _repository.capabilityConflicts(
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
      capabilityConflicts: _repository.capabilityConflicts(
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
    _repository.updateDevice(
      deviceId: deviceId,
      displayName: s.editDisplayName,
      capabilities: s.editCapabilities,
      enabled: s.editEnabled,
      wheelCircumferenceMm: wheelCircumference,
    );
    closeEditDevice();
  }

  void removeDevice(String deviceId) {
    _repository.removeDevice(deviceId);
    if (state.editingDeviceId == deviceId) closeEditDevice();
  }

  void setDeviceEnabled(String deviceId, bool enabled) =>
      _repository.setDeviceEnabled(deviceId, enabled);
}

final bleDevicesNotifierProvider =
    NotifierProvider<BleDevicesNotifier, BleDevicesUiState>(
  BleDevicesNotifier.new,
);
