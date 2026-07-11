import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/repository/impl/ble_device_repository_impl.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/features/settings/ble_devices_notifier.dart';
import 'package:openvitals/sensors/ble/ble_sensor_coordinator.dart';

/// Fake coordinator that returns canned capability-discovery results and never
/// touches flutter_blue_plus.
class _FakeCoordinator extends BleSensorCoordinator {
  _FakeCoordinator(super.repository);

  Set<BleSensorCapability> discoverResult = const {};

  @override
  Stream<List<BleDiscoveredDevice>> get discoveredDevicesStream =>
      const Stream.empty();

  @override
  Future<Set<BleSensorCapability>> discoverCapabilities(String address) async =>
      discoverResult;

  @override
  Future<void> startScan({bool showAllDevices = false}) async {}

  @override
  Future<void> stopScan() async {}
}

BleDiscoveredDevice _discovered({
  Set<BleSensorCapability> suggested = const {BleSensorCapability.heartRate},
}) =>
    BleDiscoveredDevice(
      address: 'AA:BB:CC:DD:EE:FF',
      name: 'Wahoo TICKR',
      rssi: -50,
      suggestedCapabilities: suggested,
    );

void main() {
  late BleDeviceRepositoryImpl repo;
  late _FakeCoordinator coordinator;
  late ProviderContainer container;

  Future<void> setUp0() async {
    SharedPreferences.setMockInitialValues(const {});
    final prefs = await SharedPreferences.getInstance();
    repo = BleDeviceRepositoryImpl(prefs);
    coordinator = _FakeCoordinator(repo);
    container = ProviderContainer(overrides: [
      bleDeviceRepositoryProvider.overrideWithValue(repo),
      bleSensorCoordinatorProvider.overrideWithValue(coordinator),
    ]);
    addTearDown(container.dispose);
  }

  BleDevicesNotifier notifier() =>
      container.read(bleDevicesNotifierProvider.notifier);
  BleDevicesUiState state() => container.read(bleDevicesNotifierProvider);

  test('selecting a device auto-discovers capabilities via the GATT probe',
      () async {
    await setUp0();
    coordinator.discoverResult = {
      BleSensorCapability.heartRate,
      BleSensorCapability.cyclingPower,
    };

    await notifier().selectDiscoveredDevice(_discovered());

    expect(state().addCapabilities, {
      BleSensorCapability.heartRate,
      BleSensorCapability.cyclingPower,
    });
    expect(state().isDiscoveringCapabilities, isFalse);
  });

  test('falls back to advertised capabilities when the probe finds none',
      () async {
    await setUp0();
    coordinator.discoverResult = const {};

    await notifier().selectDiscoveredDevice(
      _discovered(suggested: const {BleSensorCapability.runningSpeedCadence}),
    );

    expect(state().addCapabilities, {BleSensorCapability.runningSpeedCadence});
  });

  test('flags a capability conflict against an already-paired device',
      () async {
    await setUp0();
    // Pre-pair an enabled HR strap that owns HEART_RATE.
    repo.addDevice(
      displayName: 'Old strap',
      address: '11:22:33:44:55:66',
      bluetoothName: 'Old strap',
      capabilities: const {BleSensorCapability.heartRate},
    );
    coordinator.discoverResult = const {BleSensorCapability.heartRate};

    await notifier().selectDiscoveredDevice(_discovered());

    expect(state().capabilityConflicts.keys,
        contains(BleSensorCapability.heartRate));
    expect(state().capabilityConflicts[BleSensorCapability.heartRate]!.address,
        '11:22:33:44:55:66');
  });

  test('saving a speed sensor persists the wheel circumference', () async {
    await setUp0();
    coordinator.discoverResult = const {
      BleSensorCapability.cyclingSpeedDistance,
    };
    await notifier().selectDiscoveredDevice(
      _discovered(suggested: const {BleSensorCapability.cyclingSpeedDistance}),
    );
    notifier().updateAddWheelCircumference('2200');

    notifier().saveAddedDevice();

    final saved = repo.devices.single;
    expect(saved.capabilities, {BleSensorCapability.cyclingSpeedDistance});
    expect(saved.wheelCircumferenceMm, 2200);
    // Add flow closed after a successful save.
    expect(state().showAddFlow, isFalse);
  });

  test('non-speed sensors are saved without a wheel circumference', () async {
    await setUp0();
    coordinator.discoverResult = const {BleSensorCapability.heartRate};
    await notifier().selectDiscoveredDevice(_discovered());

    notifier().saveAddedDevice();

    expect(repo.devices.single.wheelCircumferenceMm, isNull);
  });

  test('saving with no capabilities surfaces an error and does not persist',
      () async {
    await setUp0();
    coordinator.discoverResult = const {};
    await notifier().selectDiscoveredDevice(
      _discovered(suggested: const {}),
    );

    notifier().saveAddedDevice();

    expect(state().errorMessage, isNotNull);
    expect(repo.devices, isEmpty);
  });

  test('toggling a capability recomputes conflicts', () async {
    await setUp0();
    repo.addDevice(
      displayName: 'Power meter',
      address: '11:22:33:44:55:66',
      bluetoothName: 'Power meter',
      capabilities: const {BleSensorCapability.cyclingPower},
    );
    coordinator.discoverResult = const {BleSensorCapability.heartRate};
    await notifier().selectDiscoveredDevice(_discovered());
    expect(state().capabilityConflicts, isEmpty);

    notifier().toggleAddCapability(BleSensorCapability.cyclingPower);

    expect(state().capabilityConflicts.keys,
        contains(BleSensorCapability.cyclingPower));
    expect(state().addCapabilities, contains(BleSensorCapability.cyclingPower));
  });

  test('edit flow loads the device and saves changes', () async {
    await setUp0();
    final device = repo.addDevice(
      displayName: 'Strap',
      address: '11:22:33:44:55:66',
      bluetoothName: 'Strap',
      capabilities: const {BleSensorCapability.heartRate},
    );

    notifier().openEditDevice(device.id);
    expect(state().editDisplayName, 'Strap');
    expect(state().editCapabilities, {BleSensorCapability.heartRate});

    notifier().updateEditDisplayName('My chest strap');
    notifier().setEditEnabled(false);
    notifier().saveEditedDevice();

    final updated = repo.devices.single;
    expect(updated.displayName, 'My chest strap');
    expect(updated.enabled, isFalse);
    expect(state().editingDeviceId, isNull);
  });

  test('removing the edited device closes the edit flow', () async {
    await setUp0();
    final device = repo.addDevice(
      displayName: 'Strap',
      address: '11:22:33:44:55:66',
      bluetoothName: 'Strap',
      capabilities: const {BleSensorCapability.heartRate},
    );
    notifier().openEditDevice(device.id);

    notifier().removeDevice(device.id);

    expect(repo.devices, isEmpty);
    expect(state().editingDeviceId, isNull);
  });
}
