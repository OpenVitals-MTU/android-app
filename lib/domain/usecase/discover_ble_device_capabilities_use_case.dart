import '../../data/repository/contract/ble_device_repository.dart';
import '../port/ble_capability_probe.dart';
import '../model/ble_sensor_models.dart';

/// What a scanned sensor turned out to be, and what it would collide with.
class BleDeviceCapabilityDiscovery {
  const BleDeviceCapabilityDiscovery({
    required this.capabilities,
    required this.conflicts,
  });

  final Set<BleSensorCapability> capabilities;

  /// The already-paired devices that would lose a capability to this one — a
  /// capability can only be served by a single sensor at a time.
  final Map<BleSensorCapability, BleSensorDevice> conflicts;
}

/// Works out what a discovered sensor can actually do, and what that costs.
///
/// The scan record only *advertises* a device's services, and it lies by
/// omission: plenty of sensors advertise nothing useful and only reveal their
/// GATT services once connected. So the real answer needs a connection — that is
/// [BleCapabilityProbe.discoverCapabilities] — and the advertised
/// `suggestedCapabilities` are kept only as the fallback for when the connection
/// comes back with nothing.
///
/// Pairing is then checked against the registry in the same pass: a capability
/// belongs to one device at a time, so adding a second chest strap would take
/// heart rate away from the first. The conflicts come back with the capabilities
/// rather than after them, because the user is being asked one question ("add
/// this sensor?") and needs both halves of the answer at once.
class DiscoverBleDeviceCapabilitiesUseCase {
  const DiscoverBleDeviceCapabilitiesUseCase(
    this._probe,
    this._bleDeviceRepository,
  );

  final BleCapabilityProbe _probe;
  final BleDeviceRepository _bleDeviceRepository;

  Future<BleDeviceCapabilityDiscovery> call(
    BleDiscoveredDevice device,
  ) async {
    final discovered = await _probe.discoverCapabilities(device.address);
    final capabilities =
        discovered.isNotEmpty ? discovered : device.suggestedCapabilities;
    return BleDeviceCapabilityDiscovery(
      capabilities: capabilities,
      conflicts: _bleDeviceRepository.capabilityConflicts(capabilities),
    );
  }
}
