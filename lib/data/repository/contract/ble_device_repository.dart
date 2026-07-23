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

  /// Only [BleDeviceKind.sensor] devices take part: a watch streams nothing
  /// live, so it can neither own a capability nor conflict over one.
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
    BleDeviceKind kind,
  });

  BleSensorDevice updateDevice({
    required String deviceId,
    String? displayName,
    Set<BleSensorCapability>? capabilities,
    bool? enabled,
    int? wheelCircumferenceMm,
    BleDeviceKind? kind,
  });

  void removeDevice(String deviceId);

  void setDeviceEnabled(String deviceId, bool enabled);

  void updateBatteryLevel(String deviceId, int batteryPercent);

  /// Stamps a watch's last successful FIT-file sync. No-op for an unknown id —
  /// a sync can outlive the user forgetting the device it ran against, and that
  /// race must not throw the way [updateDevice] does.
  ///
  /// The last-sync instant is the one piece of watch state that stays here: it
  /// is a generic property of any synced device and lives on the device model.
  /// A watch's Garmin-specific state — the GFDI capability bitmap and which
  /// files a sync already pulled — lives in `GarminDeviceStateStore` instead, so
  /// this registry carries no Garmin knowledge.
  void markSynced(String deviceId, DateTime at);
}
