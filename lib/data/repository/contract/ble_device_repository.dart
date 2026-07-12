import '../../../domain/model/ble_sensor_models.dart';

/// Port of the Kotlin `BleDeviceRepository` (a SharedPreferences-backed sensor
/// registry; not Health Connect). Kotlin exposes a `StateFlow`; here the
/// reactive surface is a [Stream] plus a synchronous [devices] snapshot.
///
/// Deliberately not `Result`-typed: every operation is a synchronous access to
/// SharedPreferences' in-memory cache (the persist behind the mutators is
/// fire-and-forget, as in Kotlin), so there is no failure to type — the same
/// rule that keeps the other contracts' cached-state probes bare. The one
/// throw, [updateDevice] on an unknown id, is a programming-error guard, not
/// an operational failure.
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
