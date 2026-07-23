import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/devices/core/pairing/watch_pairing_port.dart';
import 'package:openvitals/devices/core/registry/ble_device_repository_impl.dart';
import 'package:openvitals/devices/wearos/onboard_wearos_watch_use_case.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';

/// Records what the use case asked of the pairing layer, and lets a test force a
/// declined or throwing association.
class _FakePairing implements WatchPairingPort {
  bool associateResult = true;
  Object? associateError;
  String? seenAssociateAddress;
  String? seenDisassociateAddress;

  @override
  Future<bool> associateCompanion(String address, String? displayName) async {
    seenAssociateAddress = address;
    if (associateError != null) throw associateError!;
    return associateResult;
  }

  @override
  Future<void> disassociateCompanion(String address) async {
    seenDisassociateAddress = address;
  }

  // A WearOS watch never bonds — these must never be called.
  @override
  Future<WatchBondResult> bond(String address) =>
      throw StateError('WearOS onboarding must not bond');

  @override
  Future<void> removeBond(String address) =>
      throw StateError('WearOS onboarding must not touch bonds');
}

BleDiscoveredDevice _watch() => const BleDiscoveredDevice(
      address: 'A8:D1:62:BE:3A:3B',
      name: 'Galaxy Watch8 (89FZ)',
      rssi: -50,
      suggestedCapabilities: {},
    );

void main() {
  late BleDeviceRepositoryImpl repo;
  late _FakePairing pairing;
  late OnboardWearosWatchUseCase useCase;

  Future<void> setUp0() async {
    SharedPreferences.setMockInitialValues(const {});
    repo = BleDeviceRepositoryImpl(await SharedPreferences.getInstance());
    pairing = _FakePairing();
    useCase = OnboardWearosWatchUseCase(pairing, repo);
  }

  test('registers a (watch, wearos) device, no bond', () async {
    await setUp0();

    final outcome = await useCase(_watch(), displayName: 'My Watch');

    expect(outcome.associated, isTrue);
    expect(pairing.seenAssociateAddress, 'A8:D1:62:BE:3A:3B');
    final device = repo.devices.single;
    expect(device.kind, BleDeviceKind.watch);
    expect(device.integration, DeviceIntegration.wearos);
    expect(device.isWearosWatch, isTrue);
    expect(device.isGarminWatch, isFalse);
    expect(device.capabilities, isEmpty);
  });

  test('a declined association still onboards the watch', () async {
    await setUp0();
    pairing.associateResult = false;

    final outcome = await useCase(_watch(), displayName: 'My Watch');

    expect(outcome.associated, isFalse);
    expect(repo.devices.single.isWearosWatch, isTrue);
  });

  test('a thrown association is swallowed — the watch still registers', () async {
    await setUp0();
    pairing.associateError = StateError('no companion service');

    final outcome = await useCase(_watch(), displayName: 'My Watch');

    expect(outcome.associated, isFalse);
    expect(repo.devices.single.isWearosWatch, isTrue);
  });

  test('forget drops the companion association', () async {
    await setUp0();
    await useCase.forget('A8:D1:62:BE:3A:3B');
    expect(pairing.seenDisassociateAddress, 'A8:D1:62:BE:3A:3B');
  });
}
