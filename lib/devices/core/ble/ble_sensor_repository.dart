import '../../../domain/model/ble_sensor_models.dart';
import '../../../domain/port/ble_capability_probe.dart';

/// The BLE sensor stack, as the app is allowed to see it.
///
/// The implementation (`BleSensorCoordinator`) is a long-lived *service*: it
/// owns live GATT connections, a scan session and a sample buffer. Flutter's
/// app-architecture guidance is that the UI layer talks to a contract, not to a
/// service — so features depend on this type, and only `lib/di/` knows which
/// class satisfies it.
///
/// Nothing here returns a `Result`: a BLE stack that cannot reach a sensor
/// reports that through the metrics and connection state it publishes (a sensor
/// simply stops appearing), not by failing a call. Scan/connect failures are
/// already swallowed into the streams — that is the Kotlin behaviour and the
/// screens are written against it.
abstract interface class BleSensorRepository implements BleCapabilityProbe {
  /// Live recording metrics (heart rate, cadence, power) from the connected
  /// sensors. Kotlin's `StateFlow<BleRecordingMetrics>`.
  Stream<BleRecordingMetrics> get metricsStream;

  /// The latest metrics, without waiting for the next emission — what a
  /// recording tick samples.
  BleRecordingMetrics get metrics;

  /// Live scan results. Kotlin's `StateFlow<List<BleDiscoveredDevice>>`.
  Stream<List<BleDiscoveredDevice>> get discoveredDevicesStream;

  /// The samples buffered since [startRecording].
  BleRecordingSampleBuffer currentSampleBuffer();

  void startRecording();

  BleRecordingSampleBuffer stopRecording();

  /// Reconnects the paired sensors that should be connected and drops the ones
  /// that should not.
  void refreshConnections();

  void disconnectAll();

  Future<void> startScan({bool showAllDevices = false});

  Future<void> stopScan();
}
