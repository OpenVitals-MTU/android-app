import '../../data/repository/contract/ble_device_repository.dart';

/// Re-reads the sensor registry from storage so its stream re-emits.
///
/// Synchronous, like everything else about the registry: it is a preferences-backed
/// list, and the "refresh" is a re-parse, not a network call.
class RefreshBleDeviceRegistryUseCase {
  const RefreshBleDeviceRegistryUseCase(this._bleDeviceRepository);

  final BleDeviceRepository _bleDeviceRepository;

  void call() => _bleDeviceRepository.refresh();
}
