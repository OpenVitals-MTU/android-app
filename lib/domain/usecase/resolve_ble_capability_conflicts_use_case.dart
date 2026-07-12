import '../../data/repository/contract/ble_device_repository.dart';
import '../model/ble_sensor_models.dart';

/// What a set of capabilities would take away from the sensors already paired.
///
/// **Synchronous, and must stay that way.** This runs on every tick of a
/// capability checkbox: the conflict warning has to be on screen in the same frame
/// as the tick that caused it. Made a `Future`, it would resolve a frame later and
/// the checkbox would visibly lag its own consequence.
///
/// That is affordable because the registry is a SharedPreferences-backed list held
/// in memory — no Health Connect, no I/O. It is the one place in this layer where
/// the absence of a `Future` is a deliberate design decision rather than an
/// oversight.
///
/// A capability belongs to one sensor at a time, so pairing a second chest strap
/// takes heart rate away from the first. [excludingDeviceId] is what stops a device
/// being reported as conflicting with itself while it is being edited.
class ResolveBleCapabilityConflictsUseCase {
  const ResolveBleCapabilityConflictsUseCase(this._bleDeviceRepository);

  final BleDeviceRepository _bleDeviceRepository;

  Map<BleSensorCapability, BleSensorDevice> call(
    Set<BleSensorCapability> capabilities, {
    String? excludingDeviceId,
  }) =>
      _bleDeviceRepository.capabilityConflicts(
        capabilities,
        excludingDeviceId: excludingDeviceId,
      );
}
