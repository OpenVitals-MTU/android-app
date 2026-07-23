import '../../../domain/model/ble_sensor_models.dart';

/// How far a device sync has got, reported as it runs. Device-agnostic — the
/// integration maps its own protocol phases onto these.
enum DeviceSyncPhase { handshake, listing, downloading, complete, failed }

/// A progress tick from an in-flight sync: the [phase] and, while downloading,
/// how many of [filesTotal] files are [filesDone].
class DeviceSyncProgress {
  const DeviceSyncProgress({
    required this.phase,
    this.filesTotal = 0,
    this.filesDone = 0,
  });

  final DeviceSyncPhase phase;
  final int filesTotal;
  final int filesDone;
}

/// The outcome of a whole sync-and-persist run.
sealed class DeviceSyncResult {
  const DeviceSyncResult();
}

/// The sync finished; [fileCount] files were downloaded and handed on (0 is a
/// success — the watch simply had nothing new).
class DeviceSyncSucceeded extends DeviceSyncResult {
  const DeviceSyncSucceeded(this.fileCount);

  final int fileCount;
}

/// The sync failed. [message] is already a rendered, integration-agnostic
/// string — the seam never leaks the integration's exception type.
class DeviceSyncFailed extends DeviceSyncResult {
  const DeviceSyncFailed(this.message);

  final String message;
}

/// The seam between the app's generic sync orchestration and one integration's
/// sync implementation. Owns the WHOLE operation for a device — pull, import,
/// persist, stamp — reporting progress as it goes, so a second integration
/// (WearOS, …) plugs in without the view-model naming any protocol.
///
/// A **port**, like `BleCapabilityProbe`/`WatchPairingPort`: features and the
/// generic `DeviceSyncViewModel` depend on this type, and only DI knows which
/// integration satisfies it.
abstract interface class DeviceSyncPort {
  /// Whether this integration owns the sync-and-persist for [device].
  bool canSync(BleSensorDevice device);

  /// Runs the whole pull → import → store → stamp sequence for [device],
  /// reporting progress via [onProgress]. Never throws — a failed sync comes
  /// back as [DeviceSyncFailed].
  Future<DeviceSyncResult> sync(
    BleSensorDevice device, {
    Duration listenAfter = Duration.zero,
    void Function(DeviceSyncProgress)? onProgress,
  });
}
