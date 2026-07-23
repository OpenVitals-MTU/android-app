import '../../devices/core/registry/ble_device_repository.dart';
import '../model/ble_sensor_models.dart';
import '../model/garmin_transport.dart';
import '../port/garmin_transport_probe.dart';
import '../port/watch_pairing_port.dart';

/// Which platform step the onboarding is on, so the sheet can tell the user
/// which OS dialog is about to appear over it. The first two show a system
/// dialog the app does not own, and an unexplained one reads as the app
/// misbehaving; [probing] shows nothing but takes a few seconds over the air.
enum GarminOnboardStep { bonding, associating, probing }

/// How onboarding ended.
sealed class GarminOnboardOutcome {
  const GarminOnboardOutcome();
}

/// The watch is bonded and in the registry. [associated] records whether the
/// companion association was also granted — worth surfacing, because without it
/// a long sync is likelier to be killed in the background, but never a reason to
/// fail the onboarding.
class GarminOnboardSucceeded extends GarminOnboardOutcome {
  const GarminOnboardSucceeded({
    required this.device,
    required this.associated,
    required this.transport,
  });

  final BleSensorDevice device;
  final bool associated;

  /// Which GFDI transport the watch turned out to speak. Recorded, never
  /// enforced: an [GarminTransportVariant.unknown] watch still onboards, because
  /// the user's watch being unsupported is a thing to TELL them, not a reason to
  /// refuse a pairing they just confirmed on the device.
  final GarminGattReport transport;
}

/// Onboarding stopped at [step]. Nothing was written to the registry.
class GarminOnboardFailed extends GarminOnboardOutcome {
  const GarminOnboardFailed({required this.step, required this.reason});

  final GarminOnboardStep step;
  final WatchBondResult reason;
}

/// Turns a scanned Garmin watch into a registered device.
///
/// Three steps, in this order and no other:
///
///   1. **Bond.** The OS pairing dialog. Mandatory — GFDI carries no
///      authentication of its own (Gadgetbridge's `AuthNegotiationMessage`
///      answers every challenge with zeroes), so the Bluetooth bond IS the
///      security boundary for the watch's health data. No bond, no onboarding.
///   2. **Associate.** The companion dialog. Optional in every direction: the
///      user may decline it, and the platform may not offer it at all. A false
///      here is recorded, not raised.
///   3. **Probe.** Enumerate the GATT services to learn which GFDI transport
///      this watch speaks. Must come after the bond — the characteristics sit
///      behind an encrypted link. Also non-fatal: the answer is recorded.
///   4. **Register.** Only now, so a refused pairing cannot leave a watch in the
///      list that the app can never actually reach.
///
/// The watch is registered with NO capabilities and [BleDeviceKind.watch]: it
/// streams nothing live, and must never be picked up by the recording
/// coordinator's capability assignment.
class OnboardGarminWatchUseCase {
  const OnboardGarminWatchUseCase(
    this._pairing,
    this._bleDeviceRepository,
    this._transportProbe,
  );

  final WatchPairingPort _pairing;
  final BleDeviceRepository _bleDeviceRepository;
  final GarminTransportProbe _transportProbe;

  /// [onStep] fires as each platform dialog is about to be shown.
  Future<GarminOnboardOutcome> call(
    BleDiscoveredDevice device, {
    required String displayName,
    void Function(GarminOnboardStep)? onStep,
  }) async {
    onStep?.call(GarminOnboardStep.bonding);
    final bond = await _pairing.bond(device.address);
    switch (bond) {
      case WatchBondResult.refused:
      case WatchBondResult.unreachable:
        return GarminOnboardFailed(
          step: GarminOnboardStep.bonding,
          reason: bond,
        );
      case WatchBondResult.bonded:
      case WatchBondResult.alreadyBonded:
        break;
    }

    onStep?.call(GarminOnboardStep.associating);
    final associated = await _pairing.associateCompanion(
      device.address,
      displayName,
    );

    onStep?.call(GarminOnboardStep.probing);
    final transport = await _transportProbe.probe(device.address);

    final registered = _bleDeviceRepository.addDevice(
      displayName: displayName,
      address: device.address,
      bluetoothName: device.name,
      capabilities: const {},
      kind: BleDeviceKind.watch,
    );
    return GarminOnboardSucceeded(
      device: registered,
      associated: associated,
      transport: transport,
    );
  }

  /// Undoes [call] at the OS level. The registry entry is removed by
  /// [ForgetBleDevice] as usual; this drops the bond and association that would
  /// otherwise outlive it, leaving the watch paired to an app that no longer
  /// knows about it.
  Future<void> forget(String address) async {
    await _pairing.disassociateCompanion(address);
    await _pairing.removeBond(address);
  }
}
