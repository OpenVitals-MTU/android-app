import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/repository/impl/ble_device_repository_impl.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/domain/port/watch_pairing_port.dart';
import 'package:openvitals/domain/usecase/onboard_garmin_watch_use_case.dart';

/// Records what the use case asked the platform to do, and answers with whatever
/// the test set up. No radio, no Activity, no plugin.
class _FakePairing implements WatchPairingPort {
  WatchBondResult bondResult = WatchBondResult.bonded;
  bool associateResult = true;

  final List<String> calls = [];

  @override
  Future<WatchBondResult> bond(String address) async {
    calls.add('bond:$address');
    return bondResult;
  }

  @override
  Future<void> removeBond(String address) async => calls.add('removeBond:$address');

  @override
  Future<bool> associateCompanion(String address, String? displayName) async {
    calls.add('associate:$address');
    return associateResult;
  }

  @override
  Future<void> disassociateCompanion(String address) async =>
      calls.add('disassociate:$address');
}

const _watch = BleDiscoveredDevice(
  address: 'E0:48:24:D5:F7:10',
  name: 'vívoactive 5',
  rssi: -55,
  suggestedCapabilities: {},
  advertisesGarminService: true,
);

void main() {
  late BleDeviceRepositoryImpl repo;
  late _FakePairing pairing;
  late OnboardGarminWatchUseCase useCase;

  Future<void> setUp0() async {
    SharedPreferences.setMockInitialValues(const {});
    repo = BleDeviceRepositoryImpl(await SharedPreferences.getInstance());
    pairing = _FakePairing();
    useCase = OnboardGarminWatchUseCase(pairing, repo);
  }

  test('registers a bonded watch with no capabilities', () async {
    await setUp0();

    final outcome = await useCase(_watch, displayName: 'My watch');

    expect(outcome, isA<GarminOnboardSucceeded>());
    final registered = (outcome as GarminOnboardSucceeded).device;
    expect(registered.kind, BleDeviceKind.watch);
    expect(registered.isWatch, isTrue);
    expect(registered.capabilities, isEmpty);
    expect(registered.displayName, 'My watch');
    expect(registered.address, 'E0:48:24:D5:F7:10');
    expect(outcome.associated, isTrue);
  });

  test('a registered watch never takes part in capability assignment', () async {
    await setUp0();
    // A real sensor owning heart rate, so the assignment map is not empty for
    // the wrong reason.
    repo.addDevice(
      displayName: 'Chest strap',
      address: 'AA:BB:CC:DD:EE:FF',
      bluetoothName: 'Wahoo TICKR',
      capabilities: const {BleSensorCapability.heartRate},
    );

    await useCase(_watch, displayName: 'vívoactive 5');

    final assignments = repo.resolveCapabilityAssignments();
    expect(assignments[BleSensorCapability.heartRate]?.displayName,
        'Chest strap');
    expect(
      assignments.values.any((d) => d.isWatch),
      isFalse,
      reason: 'a watch in the assignment map would be connected to and polled '
          'by the recording coordinator, which it cannot answer',
    );
  });

  test('a refused pairing writes nothing to the registry', () async {
    await setUp0();
    pairing.bondResult = WatchBondResult.refused;

    final outcome = await useCase(_watch, displayName: 'vívoactive 5');

    expect(outcome, isA<GarminOnboardFailed>());
    expect((outcome as GarminOnboardFailed).step, GarminOnboardStep.bonding);
    expect(repo.devices, isEmpty);
    // The association is never even attempted — there is nothing to associate.
    expect(pairing.calls, ['bond:E0:48:24:D5:F7:10']);
  });

  test('an unreachable watch writes nothing to the registry', () async {
    await setUp0();
    pairing.bondResult = WatchBondResult.unreachable;

    final outcome = await useCase(_watch, displayName: 'vívoactive 5');

    expect((outcome as GarminOnboardFailed).reason,
        WatchBondResult.unreachable);
    expect(repo.devices, isEmpty);
  });

  test('a declined companion association still onboards the watch', () async {
    await setUp0();
    pairing.associateResult = false;

    final outcome = await useCase(_watch, displayName: 'vívoactive 5');

    // The whole point: the association buys background priority, not access.
    expect(outcome, isA<GarminOnboardSucceeded>());
    expect((outcome as GarminOnboardSucceeded).associated, isFalse);
    expect(repo.devices, hasLength(1));
  });

  test('an already-bonded watch is registered without re-prompting', () async {
    await setUp0();
    pairing.bondResult = WatchBondResult.alreadyBonded;

    final outcome = await useCase(_watch, displayName: 'vívoactive 5');

    expect(outcome, isA<GarminOnboardSucceeded>());
    expect(repo.devices, hasLength(1));
  });

  test('reports each platform step before it shows its dialog', () async {
    await setUp0();
    final steps = <GarminOnboardStep>[];

    await useCase(_watch, displayName: 'vívoactive 5', onStep: steps.add);

    expect(steps, [GarminOnboardStep.bonding, GarminOnboardStep.associating]);
  });

  test('forget drops the association and the bond, in that order', () async {
    await setUp0();

    await useCase.forget('E0:48:24:D5:F7:10');

    expect(pairing.calls, [
      'disassociate:E0:48:24:D5:F7:10',
      'removeBond:E0:48:24:D5:F7:10',
    ]);
  });
}
