import '../../../domain/model/ble_sensor_models.dart';

/// Port of the Kotlin `BleDeviceRepository` (a SharedPreferences-backed sensor
/// registry; not Health Connect). Kotlin exposes a `StateFlow`; here the
/// reactive surface is a [Stream] plus a synchronous [devices] snapshot.
abstract interface class BleDeviceRepository {
  Stream<List<BleSensorDevice>> get devicesStream;

  List<BleSensorDevice> get devices;

  List<BleSensorDevice> get enabledDevices;

  void refresh();

  Map<BleSensorCapability, BleSensorDevice> resolveCapabilityAssignments();

  Map<BleSensorCapability, BleSensorDevice> capabilityConflicts(
    Set<BleSensorCapability> capabilities, {
    String? excludingDeviceId,
  });

  BleSensorDevice addDevice({
    required String displayName,
    required String address,
    required String? bluetoothName,
    required Set<BleSensorCapability> capabilities,
    int? wheelCircumferenceMm,
  });

  BleSensorDevice updateDevice({
    required String deviceId,
    String? displayName,
    Set<BleSensorCapability>? capabilities,
    bool? enabled,
    int? wheelCircumferenceMm,
  });

  void removeDevice(String deviceId);

  void setDeviceEnabled(String deviceId, bool enabled);

  void updateBatteryLevel(String deviceId, int batteryPercent);
}
