import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/devices/core/registry/ble_device_repository_impl.dart';
import 'package:openvitals/devices/core/ble/ble_sensor_repository.dart';
import 'package:openvitals/devices/garmin/garmin_capabilities.dart';
import 'package:openvitals/devices/garmin/garmin_device_state_store.dart';
import 'package:openvitals/di/providers.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/devices/garmin/garmin_transport.dart';
import 'package:openvitals/devices/garmin/garmin_transport_probe.dart';
import 'package:openvitals/devices/core/pairing/watch_pairing_port.dart';
import 'package:openvitals/features/settings/application/ble_devices_view_model.dart';

/// Fake coordinator that returns canned capability-discovery results and never
/// touches flutter_blue_plus.
class _FakeCoordinator implements BleSensorRepository {
  _FakeCoordinator();

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

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// Stands in for `flutter_blue_plus` bonding + the CompanionDeviceManager
/// plugin, so watch onboarding is testable with no radio and no Activity.
class _FakePairing implements WatchPairingPort {
  WatchBondResult bondResult = WatchBondResult.bonded;
  bool associateResult = true;

  final List<String> calls = [];

  @override
  Future<WatchBondResult> bond(String address) async => bondResult;

  @override
  Future<void> removeBond(String address) async =>
      calls.add('removeBond:$address');

  @override
  Future<bool> associateCompanion(String address, String? displayName) async =>
      associateResult;

  @override
  Future<void> disassociateCompanion(String address) async =>
      calls.add('disassociate:$address');
}

/// Canned GATT verdict; never opens a connection.
class _FakeProbe implements GarminTransportProbe {
  GarminTransportVariant variant = GarminTransportVariant.v1;

  @override
  Future<GarminGattReport> probe(String address) async => GarminGattReport(
        address: address,
        variant: variant,
        services: const [],
      );
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

/// The device in the screenshots this feature was built from.
BleDiscoveredDevice _watch() => const BleDiscoveredDevice(
      address: 'E0:48:24:D5:F7:10',
      name: 'vívoactive 5',
      rssi: -55,
      suggestedCapabilities: {},
      advertisesSyncService: true,
    );

/// A Garmin Edge bike computer — GFDI like a watch, but its own kind.
BleDiscoveredDevice _bikeComputer() => const BleDiscoveredDevice(
      address: 'E0:48:24:D5:F7:20',
      name: 'Edge 840',
      rssi: -55,
      suggestedCapabilities: {},
      advertisesSyncService: true,
    );

void main() {
  late BleDeviceRepositoryImpl repo;
  late _FakeCoordinator coordinator;
  late _FakePairing pairing;
  late _FakeProbe probe;
  late SharedPreferences prefs;
  late ProviderContainer container;

  Future<void> setUp0() async {
    SharedPreferences.setMockInitialValues(const {});
    prefs = await SharedPreferences.getInstance();
    repo = BleDeviceRepositoryImpl(prefs);
    coordinator = _FakeCoordinator();
    pairing = _FakePairing();
    probe = _FakeProbe();
    container = ProviderContainer(overrides: [
      // The Garmin state store reads this; the watch-forget path clears it.
      sharedPreferencesProvider.overrideWithValue(prefs),
      bleDeviceRepositoryProvider.overrideWithValue(repo),
      bleSensorRepositoryProvider.overrideWithValue(coordinator),
      watchPairingPortProvider.overrideWithValue(pairing),
      garminTransportProbeProvider.overrideWithValue(probe),
    ]);
    addTearDown(container.dispose);
  }

  BleDevicesViewModel notifier() =>
      container.read(bleDevicesViewModelProvider.notifier);
  BleDevicesUiState state() => container.read(bleDevicesViewModelProvider);

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

  // The state is @freezed now (it used to be a hand-written class with an
  // `_unset` sentinel in copyWith). These pin what the conversion had to keep.

  test('the freezed state keeps its defaults and its derived getter', () {
    const state = BleDevicesUiState();

    expect(state.devices, isEmpty);
    expect(state.discoveredDevices, isEmpty);
    expect(state.isScanning, isFalse);
    expect(state.showAllDevices, isFalse);
    expect(state.selectedDevice, isNull);
    expect(state.discoveredCapabilities, isEmpty);
    expect(state.isDiscoveringCapabilities, isFalse);
    expect(state.addDisplayName, '');
    expect(state.addCapabilities, isEmpty);
    expect(state.addWheelCircumferenceMm, '');
    expect(state.capabilityConflicts, isEmpty);
    expect(state.editingDeviceId, isNull);
    expect(state.editDisplayName, '');
    expect(state.editCapabilities, isEmpty);
    // The one non-false default.
    expect(state.editEnabled, isTrue);
    expect(state.editWheelCircumferenceMm, '');
    expect(state.errorMessage, isNull);
    expect(state.showAddFlow, isFalse);
    expect(state.enabledDeviceCount, 0);
  });

  test('enabledDeviceCount survives the conversion', () async {
    await setUp0();
    repo.addDevice(
      displayName: 'On',
      address: 'AA:AA:AA:AA:AA:AA',
      bluetoothName: 'On',
      capabilities: const {BleSensorCapability.heartRate},
    );
    final off = repo.addDevice(
      displayName: 'Off',
      address: 'BB:BB:BB:BB:BB:BB',
      bluetoothName: 'Off',
      capabilities: const {BleSensorCapability.heartRate},
    );
    notifier().setDeviceEnabled(off.id, false);

    final devices = repo.devices;
    expect(devices, hasLength(2));
    expect(BleDevicesUiState(devices: devices).enabledDeviceCount, 1);
  });

  test('copyWith still clears a nullable field when passed null', () {
    const state = BleDevicesUiState(
      editingDeviceId: 'device-1',
      errorMessage: 'boom',
      showAddFlow: true,
    );

    // Omitted -> unchanged (what the old `_unset` sentinel bought).
    expect(state.copyWith(showAddFlow: false).editingDeviceId, 'device-1');
    expect(state.copyWith(showAddFlow: false).errorMessage, 'boom');
    // Passed null -> cleared.
    expect(state.copyWith(editingDeviceId: null).editingDeviceId, isNull);
    expect(state.copyWith(errorMessage: null).errorMessage, isNull);
  });

  test('the freezed state compares by value', () {
    expect(
      const BleDevicesUiState(addDisplayName: 'TICKR'),
      const BleDevicesUiState(addDisplayName: 'TICKR'),
    );
    expect(
      const BleDevicesUiState(addDisplayName: 'TICKR'),
      isNot(const BleDevicesUiState(addDisplayName: 'Polar')),
    );
  });

  group('Garmin watch onboarding', () {
    test('selecting a watch skips the capability probe entirely', () async {
      await setUp0();
      // Armed so the assertion below is about the branch, not about the probe
      // happening to return nothing.
      coordinator.discoverResult = {BleSensorCapability.heartRate};

      await notifier().selectDiscoveredDevice(_watch());

      expect(state().isAddingGfdiDevice, isTrue);
      expect(state().isDiscoveringCapabilities, isFalse);
      expect(state().addCapabilities, isEmpty);
      expect(state().capabilityConflicts, isEmpty);
      expect(state().addDisplayName, 'vívoactive 5');
    });

    test('a sensor still goes through the probe', () async {
      await setUp0();
      coordinator.discoverResult = {BleSensorCapability.heartRate};

      await notifier().selectDiscoveredDevice(_discovered());

      expect(state().isAddingGfdiDevice, isFalse);
      expect(state().addCapabilities, {BleSensorCapability.heartRate});
    });

    test('selecting an Edge skips the probe like a watch', () async {
      await setUp0();
      coordinator.discoverResult = {BleSensorCapability.heartRate};

      await notifier().selectDiscoveredDevice(_bikeComputer());

      expect(state().isAddingGfdiDevice, isTrue);
      expect(state().isDiscoveringCapabilities, isFalse);
      expect(state().addCapabilities, isEmpty);
      expect(state().addDisplayName, 'Edge 840');
    });

    test('onboarding registers the watch and closes the sheet', () async {
      await setUp0();
      await notifier().selectDiscoveredDevice(_watch());

      expect(await notifier().onboardSelectedWatch(), isTrue);

      expect(repo.devices, hasLength(1));
      expect(repo.devices.single.kind, BleDeviceKind.watch);
      expect(state().isOnboardingWatch, isFalse);
      expect(state().onboardStep, isNull);
      expect(state().showAddFlow, isFalse);
    });

    test('onboarding an Edge registers it as a bike computer', () async {
      await setUp0();
      await notifier().selectDiscoveredDevice(_bikeComputer());

      expect(await notifier().onboardSelectedWatch(), isTrue);

      expect(repo.devices, hasLength(1));
      expect(repo.devices.single.kind, BleDeviceKind.bikeComputer);
      expect(repo.devices.single.isBikeComputer, isTrue);
      expect(repo.devices.single.isGarminGfdi, isTrue);
      expect(repo.devices.single.capabilities, isEmpty);
    });

    test('a refused pairing keeps the sheet open and explains why', () async {
      await setUp0();
      pairing.bondResult = WatchBondResult.refused;
      await notifier().selectDiscoveredDevice(_watch());

      expect(await notifier().onboardSelectedWatch(), isFalse);

      expect(repo.devices, isEmpty);
      // The sheet must survive: re-scanning to retry a mistyped code is a
      // pointless round trip.
      expect(state().selectedDevice, isNotNull);
      expect(state().isOnboardingWatch, isFalse);
      expect(state().errorMessage, isNotNull);
    });

    test('a declined companion association is recorded, not failed', () async {
      await setUp0();
      pairing.associateResult = false;
      await notifier().selectDiscoveredDevice(_watch());

      expect(await notifier().onboardSelectedWatch(), isTrue);

      expect(repo.devices, hasLength(1));
      expect(state().watchOnboardedWithoutCompanion, isTrue);
    });

    test('the no-companion flag survives the sheet closing, then clears',
        () async {
      await setUp0();
      pairing.associateResult = false;
      await notifier().selectDiscoveredDevice(_watch());

      await notifier().onboardSelectedWatch();

      // onboardSelectedWatch closes the add flow itself, and the screen reads
      // the flag AFTER the sheet pops to raise its notice — so closing must not
      // consume it.
      expect(state().showAddFlow, isFalse);
      expect(state().watchOnboardedWithoutCompanion, isTrue);

      // Starting a fresh add is what clears it, so the notice fires once.
      notifier().openAddFlow();
      expect(state().watchOnboardedWithoutCompanion, isFalse);
    });

    test('a blank name falls back to the advertised one', () async {
      await setUp0();
      await notifier().selectDiscoveredDevice(_watch());
      notifier().updateAddDisplayName('   ');

      await notifier().onboardSelectedWatch();

      expect(repo.devices.single.displayName, 'vívoactive 5');
    });

    test('forgetting a watch also drops its bond and association', () async {
      await setUp0();
      await notifier().selectDiscoveredDevice(_watch());
      await notifier().onboardSelectedWatch();
      final id = repo.devices.single.id;
      pairing.calls.clear();
      // Give the watch some Garmin state, so we can prove forgetting drops it.
      GarminDeviceStateStore(prefs)
        ..recordSyncedFileKeys(id, ['128/49/1'])
        ..recordCapabilities(id, {GarminCapability.sync});

      notifier().removeDevice(id);
      // The cleanup is fire-and-forget, so let the microtasks drain.
      await Future<void>.delayed(Duration.zero);

      expect(repo.devices, isEmpty);
      expect(pairing.calls, [
        'disassociate:E0:48:24:D5:F7:10',
        'removeBond:E0:48:24:D5:F7:10',
      ]);
      // The registry no longer clears Garmin state; the watch-forget path does.
      final leftover = GarminDeviceStateStore(prefs);
      expect(leftover.syncedFileKeys(id), isEmpty);
      expect(leftover.capabilities(id), isEmpty);
    });

    test('forgetting a sensor touches neither bond nor association', () async {
      await setUp0();
      final sensor = repo.addDevice(
        displayName: 'Chest strap',
        address: 'AA:BB:CC:DD:EE:FF',
        bluetoothName: 'Wahoo TICKR',
        capabilities: const {BleSensorCapability.heartRate},
      );

      notifier().removeDevice(sensor.id);
      await Future<void>.delayed(Duration.zero);

      expect(repo.devices, isEmpty);
      expect(pairing.calls, isEmpty);
    });

    test('a watch can be renamed even though it has no capabilities', () async {
      await setUp0();
      await notifier().selectDiscoveredDevice(_watch());
      await notifier().onboardSelectedWatch();
      final id = repo.devices.single.id;

      notifier()
        ..openEditDevice(id)
        ..updateEditDisplayName('Running watch')
        ..saveEditedDevice();

      // The sensor rule ("select at least one capability") must not fire here.
      expect(state().errorMessage, isNull);
      expect(state().editingDeviceId, isNull);
      expect(repo.devices.single.displayName, 'Running watch');
    });
  });
}
