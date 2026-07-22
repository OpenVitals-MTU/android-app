import '../../../domain/model/ble_sensor_models.dart';
import '../../source/sensors/garmin/garmin_capabilities.dart';

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
  void markSynced(String deviceId, DateTime at);

  /// Which of a watch's files a previous sync already pulled, keyed by
  /// [GarminDirectoryEntry.dedupKey].
  ///
  /// Purely a BANDWIDTH optimisation, and the secondary one at that: the primary
  /// mechanism is the archive flag the sync sets on the watch, which stops it
  /// re-offering a file at all. This set covers the cases where that fails — a
  /// re-pair, a sync that died before archiving — and Health Connect's
  /// `clientRecordId` makes a re-import idempotent regardless, so a stale or
  /// empty set costs airtime and never correctness.
  Set<String> syncedFileKeys(String deviceId);

  /// Adds to that set. Bounded, oldest-dropped-first: a watch worn for years
  /// would otherwise grow an unbounded list in SharedPreferences.
  void recordSyncedFileKeys(String deviceId, Iterable<String> keys);

  /// What the watch declared it can do, from the last handshake.
  ///
  /// Persisted per device because it is the only thing that says whether a
  /// watch supports finding, alarms or its own settings tree — and the UI has
  /// to decide that before a sync has run, not during one.
  Set<GarminCapability> capabilities(String deviceId);

  void recordCapabilities(String deviceId, Set<GarminCapability> capabilities);

  /// Drops a watch's recorded keys, so a re-pair starts clean.
  void clearSyncedFileKeys(String deviceId);
}
