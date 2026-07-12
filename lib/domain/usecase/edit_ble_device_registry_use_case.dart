import '../../data/repository/contract/ble_device_repository.dart';
import '../model/ble_sensor_models.dart';

/// One change to the paired-sensor registry.
sealed class BleDeviceRegistryEdit {
  const BleDeviceRegistryEdit();
}

/// Pairs a scanned device. [wheelCircumferenceMm] is null unless the device is
/// being paired for cycling speed/distance — that is the one capability whose
/// readings are meaningless without it.
class PairBleDevice extends BleDeviceRegistryEdit {
  const PairBleDevice({
    required this.displayName,
    required this.address,
    required this.bluetoothName,
    required this.capabilities,
    this.wheelCircumferenceMm,
  });

  final String displayName;
  final String address;
  final String? bluetoothName;
  final Set<BleSensorCapability> capabilities;
  final int? wheelCircumferenceMm;
}

/// Edits a paired device. Every field is optional: the edit sheet sends what it
/// has, and an omitted field is "leave it alone", not "clear it".
class UpdateBleDevice extends BleDeviceRegistryEdit {
  const UpdateBleDevice({
    required this.deviceId,
    this.displayName,
    this.capabilities,
    this.enabled,
    this.wheelCircumferenceMm,
  });

  final String deviceId;
  final String? displayName;
  final Set<BleSensorCapability>? capabilities;
  final bool? enabled;
  final int? wheelCircumferenceMm;
}

class ForgetBleDevice extends BleDeviceRegistryEdit {
  const ForgetBleDevice(this.deviceId);

  final String deviceId;
}

/// Turns a paired device off without forgetting it — it keeps its capabilities,
/// and keeps holding them against any other device that wants them.
class SetBleDeviceEnabled extends BleDeviceRegistryEdit {
  const SetBleDeviceEnabled(this.deviceId, this.enabled);

  final String deviceId;
  final bool enabled;
}

/// Applies one change to the paired-sensor registry.
///
/// **Synchronous.** The registry is a list in SharedPreferences, and the settings
/// screen writes to it from event handlers — a tapped switch, a saved form. Making
/// these `Future`s would buy nothing and cost the screen its ability to update in
/// the frame it was tapped in. See `ResolveBleCapabilityConflictsUseCase`, which
/// has the same constraint for a sharper reason.
///
/// Four edits, one use case: they are all the same registry, they all invalidate
/// the same paired-device stream, and the difference between them is a shape, not a
/// dependency.
class EditBleDeviceRegistryUseCase {
  const EditBleDeviceRegistryUseCase(this._bleDeviceRepository);

  final BleDeviceRepository _bleDeviceRepository;

  void call(BleDeviceRegistryEdit edit) {
    switch (edit) {
      case PairBleDevice():
        _bleDeviceRepository.addDevice(
          displayName: edit.displayName,
          address: edit.address,
          bluetoothName: edit.bluetoothName,
          capabilities: edit.capabilities,
          wheelCircumferenceMm: edit.wheelCircumferenceMm,
        );
      case UpdateBleDevice():
        _bleDeviceRepository.updateDevice(
          deviceId: edit.deviceId,
          displayName: edit.displayName,
          capabilities: edit.capabilities,
          enabled: edit.enabled,
          wheelCircumferenceMm: edit.wheelCircumferenceMm,
        );
      case ForgetBleDevice():
        _bleDeviceRepository.removeDevice(edit.deviceId);
      case SetBleDeviceEnabled():
        _bleDeviceRepository.setDeviceEnabled(edit.deviceId, edit.enabled);
    }
  }
}
