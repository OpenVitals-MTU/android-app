import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../devices/core/ble/ble_sensor_repository.dart';
import '../../../di/providers.dart';
import '../../../domain/model/ble_sensor_models.dart';
import '../../../domain/model/garmin_device_names.dart';
import '../../../domain/model/garmin_transport.dart';
import '../../../devices/core/pairing/watch_pairing_port.dart';
import '../../../domain/usecase/edit_ble_device_registry_use_case.dart';
import '../../../domain/usecase/onboard_garmin_watch_use_case.dart';

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

    /// Whether [editingDeviceId] names a watch. Stored rather than derived from
    /// [devices]: that list arrives over a stream, so a getter would read false
    /// for the frame between opening the sheet and the stream's first emission —
    /// long enough to render the capability picker for a watch. Set from the
    /// same synchronous registry snapshot [openEditDevice] prefills from.
    @Default(false) bool isEditingWatch,
    @Default('') String editDisplayName,
    @Default(<BleSensorCapability>{}) Set<BleSensorCapability> editCapabilities,
    @Default(true) bool editEnabled,
    @Default('') String editWheelCircumferenceMm,
    String? errorMessage,
    @Default(false) bool showAddFlow,

    /// A Garmin watch is being bonded/associated right now. Distinct from
    /// [isDiscoveringCapabilities]: that probe is silent and cancellable, this
    /// one puts two OS dialogs in front of the user and must not be interrupted.
    @Default(false) bool isOnboardingWatch,

    /// Which OS dialog the onboarding is waiting on, so the sheet can name it
    /// before it appears over the app.
    GarminOnboardStep? onboardStep,

    /// Set when a watch onboarded but the companion association was declined or
    /// unavailable. Not an error — a note that background syncs are likelier to
    /// be interrupted.
    @Default(false) bool watchOnboardedWithoutCompanion,

    /// Which GFDI transport the last onboarded watch speaks. Null until one is
    /// onboarded. Surfaced so an unsupported watch says so instead of sitting in
    /// the list looking healthy and never syncing.
    GarminTransportVariant? watchTransport,
  }) = _BleDevicesUiState;

  int get enabledDeviceCount => devices.where((d) => d.enabled).length;

  /// True while the add sheet is showing a watch rather than a sensor — the two
  /// share the sheet but ask completely different questions.
  bool get isAddingWatch {
    final device = selectedDevice;
    return device != null && isGarminSyncDevice(device);
  }
}

/// Riverpod port of the Kotlin `BleDevicesViewModel`.
///
/// The paired-device list and scan results come from the repository and
/// coordinator streams (merged in [build]); every other field is local UI state
/// held in [_local] and preserved across stream emissions.
class BleDevicesViewModel extends Notifier<BleDevicesUiState> {
  BleDevicesUiState _local = const BleDevicesUiState();
  int _discoverGeneration = 0;

  BleSensorRepository get _coordinator =>
      ref.read(bleSensorRepositoryProvider);

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
      // Describes the LAST onboarding's outcome, and the screen reads it after
      // the sheet pops — so it is cleared when a new flow starts, not when the
      // old one closes.
      watchOnboardedWithoutCompanion: false,
      watchTransport: null,
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

  /// Opens the add-device flow, asking for the scan permission first. Returns
  /// false when the user refuses it — there is nothing to scan with, so the
  /// caller must not open the sheet.
  Future<bool> beginAddFlow() async {
    final granted = await ref.read(bleScanPermissionGateProvider)();
    if (!granted || !ref.mounted) return false;
    openAddFlow();
    startScan();
    return true;
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
    // A watch answers a different question. It streams nothing live, so probing
    // it for capabilities would connect, find no standard service and report
    // nothing — the sheet asks the user to bond it instead.
    if (isGarminSyncDevice(device)) {
      _setLocal(_local.copyWith(
        selectedDevice: device,
        addDisplayName: device.name ?? device.address,
        addCapabilities: const {},
        discoveredCapabilities: const {},
        capabilityConflicts: const {},
        isDiscoveringCapabilities: false,
        errorMessage: null,
      ));
      // The scan competes with the connect that bonding needs, and the user has
      // chosen — nothing more will be picked from the list.
      stopScan();
      return;
    }
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

  /// Bonds and registers the selected Garmin watch. Returns false when the user
  /// refused the pairing dialog or the watch could not be reached — the sheet
  /// stays open in that case so they can retry without re-scanning.
  ///
  /// The companion association is not part of that verdict: a watch onboards
  /// successfully whether or not it was granted (see [OnboardGarminWatchUseCase]).
  Future<bool> onboardSelectedWatch() async {
    final selected = state.selectedDevice;
    if (selected == null || state.isOnboardingWatch) return false;

    _setLocal(_local.copyWith(
      isOnboardingWatch: true,
      onboardStep: GarminOnboardStep.bonding,
      errorMessage: null,
      watchOnboardedWithoutCompanion: false,
    ));

    final displayName = state.addDisplayName.trim().isEmpty
        ? (selected.name ?? selected.address)
        : state.addDisplayName.trim();

    final outcome = await ref.read(onboardGarminWatchUseCaseProvider)(
      selected,
      displayName: displayName,
      onStep: (step) {
        // The sheet may be gone by the time a step fires — the OS dialogs run
        // over it and the user can dismiss everything.
        if (ref.mounted && _local.isOnboardingWatch) {
          _setLocal(_local.copyWith(onboardStep: step));
        }
      },
    );
    if (!ref.mounted) return false;

    switch (outcome) {
      case GarminOnboardFailed(:final reason):
        _setLocal(_local.copyWith(
          isOnboardingWatch: false,
          onboardStep: null,
          errorMessage: switch (reason) {
            WatchBondResult.unreachable =>
              'Could not reach the watch. Wake it up and keep it close, then try again.',
            _ =>
              'Pairing was not completed. Confirm the code on the watch to finish.',
          },
        ));
        return false;
      case GarminOnboardSucceeded(:final associated, :final transport):
        _setLocal(_local.copyWith(
          isOnboardingWatch: false,
          onboardStep: null,
          watchOnboardedWithoutCompanion: !associated,
          watchTransport: transport.variant,
        ));
        closeAddFlow();
        return true;
    }
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
      isEditingWatch: device.isWatch,
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
      isEditingWatch: false,
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
    // A watch is legitimately capability-less, so the sensor rule would reject
    // every edit to one — renaming it included.
    if (s.editCapabilities.isEmpty && !s.isEditingWatch) {
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
    // Read the device BEFORE forgetting it — the OS-level cleanup below needs
    // its address, which the registry is about to stop holding.
    final device = ref
        .read(readPairedBleDevicesUseCaseProvider)()
        .where((d) => d.id == deviceId)
        .firstOrNull;
    _edit(ForgetBleDevice(deviceId));
    if (device != null && device.isWatch) {
      // The watch's Garmin-specific state used to be cleared inside
      // removeDevice; now that it lives in its own store, this watch-forget
      // branch — the single path every device removal funnels through — is what
      // clears it, so a re-pairing starts clean.
      ref.read(garminDeviceStateStoreProvider).clear(deviceId);
      // Fire-and-forget, like every other registry mutation here: dropping the
      // bond and association is housekeeping the user does not wait on, and a
      // failure leaves nothing worse than a stale OS pairing they can clear in
      // Android's own Bluetooth settings.
      unawaited(
        ref.read(onboardGarminWatchUseCaseProvider).forget(device.address),
      );
    }
    if (state.editingDeviceId == deviceId) closeEditDevice();
  }

  void setDeviceEnabled(String deviceId, bool enabled) =>
      _edit(SetBleDeviceEnabled(deviceId, enabled));

  /// Renames a device without touching anything else it holds.
  ///
  /// Separate from the edit sheet on purpose: a watch has no capabilities to
  /// pick, so its whole "edit" is a name, and routing that through
  /// [saveEditedDevice] would drag the capability rules along with it. Every
  /// other field is left null, which the repository reads as unchanged.
  void renameDevice(String deviceId, String displayName) {
    final trimmed = displayName.trim();
    if (trimmed.isEmpty) return;
    _edit(UpdateBleDevice(deviceId: deviceId, displayName: trimmed));
  }
}

final bleDevicesViewModelProvider =
    NotifierProvider<BleDevicesViewModel, BleDevicesUiState>(
  BleDevicesViewModel.new,
);
