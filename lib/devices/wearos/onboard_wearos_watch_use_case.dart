import '../../domain/model/ble_sensor_models.dart';
import '../core/pairing/watch_pairing_port.dart';
import '../core/registry/ble_device_repository.dart';

/// The one platform step WearOS onboarding shows — the companion association
/// dialog. There is no bond step (a WearOS watch has no GFDI auth boundary to
/// protect) and no transport probe (no GFDI to enumerate).
enum WearosOnboardStep { associating }

/// WearOS onboarding always succeeds once the user has picked the watch — there
/// is nothing that can fail the way a Garmin bond can. [associated] records
/// whether the optional companion association was granted.
class WearosOnboardOutcome {
  const WearosOnboardOutcome({required this.device, required this.associated});

  final BleSensorDevice device;
  final bool associated;
}

/// Turns a scanned WearOS smartwatch (Galaxy, Pixel, …) into a registered
/// `(watch, wearos)` device — the sibling of [OnboardGarminWatchUseCase] built
/// on the same [WatchPairingPort] + [BleDeviceRepository] seam.
///
/// Two steps, not the Garmin four:
///   1. **Associate.** The companion dialog — optional in every direction; a
///      false (declined, or unsupported) is recorded, never raised. It is the
///      "pair like Garmin" parity, minus the security bond Garmin needs.
///   2. **Register.** As [BleDeviceKind.watch] with [DeviceIntegration.wearos]
///      and NO capabilities, so it is off the Garmin sync path
///      ([BleSensorDevice.isGarminWatch]) and out of capability assignment.
///
/// No bond and no GFDI probe: a WearOS watch speaks neither. Its live heart rate
/// comes over standard GATT and its recorded data through Health Connect.
class OnboardWearosWatchUseCase {
  const OnboardWearosWatchUseCase(this._pairing, this._bleDeviceRepository);

  final WatchPairingPort _pairing;
  final BleDeviceRepository _bleDeviceRepository;

  Future<WearosOnboardOutcome> call(
    BleDiscoveredDevice device, {
    required String displayName,
    void Function(WearosOnboardStep)? onStep,
  }) async {
    onStep?.call(WearosOnboardStep.associating);
    var associated = false;
    try {
      associated = await _pairing.associateCompanion(device.address, displayName);
    } catch (_) {
      // The companion association is a best-effort nicety; the watch onboards
      // whether or not the platform granted (or even offered) it.
    }

    final registered = _bleDeviceRepository.addDevice(
      displayName: displayName,
      address: device.address,
      bluetoothName: device.name,
      capabilities: const {},
      kind: BleDeviceKind.watch,
      integration: DeviceIntegration.wearos,
    );
    return WearosOnboardOutcome(device: registered, associated: associated);
  }

  /// Undoes [call] at the OS level — drops the companion association. The
  /// registry entry is removed by the usual forget path.
  Future<void> forget(String address) =>
      _pairing.disassociateCompanion(address);
}
