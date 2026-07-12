import '../../data/repository/contract/ble_device_repository.dart';
import '../model/ble_sensor_models.dart';

/// The paired sensors, as the registry has them right now — **synchronously**.
///
/// The device list also arrives as a stream, and that is what the list UI renders.
/// This is the other question: "what is stored for this device *at this instant*",
/// which is what opening the edit sheet needs. Answering it from the streamed copy
/// would prefill the form from whatever the last emission happened to carry.
class ReadPairedBleDevicesUseCase {
  const ReadPairedBleDevicesUseCase(this._bleDeviceRepository);

  final BleDeviceRepository _bleDeviceRepository;

  List<BleSensorDevice> call() => _bleDeviceRepository.devices;
}
