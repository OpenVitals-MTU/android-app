import '../model/ble_sensor_models.dart';

/// Asks a sensor, over the air, what it can actually do.
///
/// A **port**: the domain declares the one capability it needs from the BLE stack
/// and owns the interface, so the dependency points inward. Without it, a use case
/// in `domain/` would have to import `BleSensorCoordinator` out of
/// `data/source/sensors/` — the domain layer reaching down into a concrete data
/// source, which is the dependency rule backwards.
///
/// It is deliberately one method wide. `BleSensorCoordinator` is a large class
/// (scanning, connections, live metric streams, battery); a use case that only
/// needs to probe a device should not be handed all of that, nor be forced to
/// fake it in a test.
abstract interface class BleCapabilityProbe {
  /// Connects to [address], enumerates its GATT services and returns the
  /// capabilities they map to, then disconnects.
  ///
  /// Empty when the device could not be reached or advertises nothing we
  /// understand — the caller decides what to do about that.
  Future<Set<BleSensorCapability>> discoverCapabilities(String address);
}
