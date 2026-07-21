import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/data/repository/impl/ble_device_repository_impl.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/domain/model/garmin_transport.dart';
import 'package:openvitals/domain/port/garmin_transport_probe.dart';
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

/// Answers with a canned GATT verdict; never opens a connection.
class _FakeProbe implements GarminTransportProbe {
  GarminTransportVariant variant = GarminTransportVariant.v1;
  final List<String> calls = [];

  @override
  Future<GarminGattReport> probe(String address) async {
    calls.add('probe:$address');
    return GarminGattReport(
      address: address,
      variant: variant,
      services: const [],
    );
  }
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
  late _FakeProbe probe;
  late OnboardGarminWatchUseCase useCase;

  Future<void> setUp0() async {
    SharedPreferences.setMockInitialValues(const {});
    repo = BleDeviceRepositoryImpl(await SharedPreferences.getInstance());
    pairing = _FakePairing();
    probe = _FakeProbe();
    useCase = OnboardGarminWatchUseCase(pairing, repo, probe);
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

    expect(steps, [
      GarminOnboardStep.bonding,
      GarminOnboardStep.associating,
      GarminOnboardStep.probing,
    ]);
  });

  test('the probe runs only after bonding succeeds', () async {
    await setUp0();
    pairing.bondResult = WatchBondResult.refused;

    await useCase(_watch, displayName: 'vívoactive 5');

    // Probing an unbonded watch enumerates a link with no encryption, so the
    // GFDI characteristics are absent and the verdict would read "unknown" —
    // a false negative that looks exactly like an unsupported device.
    expect(probe.calls, isEmpty);
  });

  test('an unsupported transport still onboards, and is reported', () async {
    await setUp0();
    probe.variant = GarminTransportVariant.unknown;

    final outcome = await useCase(_watch, displayName: 'vívoactive 5');

    // Refusing a pairing the user just confirmed on the watch would be worse
    // than registering one that cannot sync yet and saying so.
    expect(outcome, isA<GarminOnboardSucceeded>());
    final succeeded = outcome as GarminOnboardSucceeded;
    expect(succeeded.transport.variant, GarminTransportVariant.unknown);
    expect(succeeded.transport.isSupported, isFalse);
    expect(repo.devices, hasLength(1));
  });

  test('a v2 watch reports as supported', () async {
    await setUp0();
    probe.variant = GarminTransportVariant.v2;

    final outcome = await useCase(_watch, displayName: 'vívoactive 5');

    expect((outcome as GarminOnboardSucceeded).transport.isSupported, isTrue);
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
