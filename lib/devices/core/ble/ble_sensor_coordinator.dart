import 'dart:async';

import 'package:collection/collection.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';

import '../../../domain/model/ble_sensor_models.dart';
import '../registry/ble_device_repository.dart';
import 'ble_gatt_connection.dart';
import 'ble_sensor_repository.dart';
import 'ble_uuids.dart';

/// Port of the Kotlin `BleSensorCoordinator` over `flutter_blue_plus`.
///
/// Maps `flutter_blue_plus` to the metrics pipeline:
///   * scan → [FlutterBluePlus.onScanResults] → [BleDiscoveredDevice] list;
///   * connect → one [BleGattConnection] per paired device address;
///   * notify → each connection feeds characteristic bytes through the parsers
///     into its aggregators;
///   * aggregate → [collectMetrics] reads each aggregator's `current()` and
///     assembles a [BleRecordingMetrics] snapshot, published on [metricsStream].
///
/// The Kotlin `StateFlow`s are replaced by broadcast streams that replay their
/// latest value on subscribe ([metricsStream], [discoveredDevicesStream]).
///
/// Runtime verification is deferred; scan/connect are device-dependent.
class BleSensorCoordinator implements BleSensorRepository {
  BleSensorCoordinator(this._deviceRepository);

  final BleDeviceRepository _deviceRepository;

  final Map<String, BleGattConnection> _connections = {};
  final Map<BleSensorCapability, BleSensorDevice> _capabilityOwners = {};
  BleRecordingSampleBuffer _sampleBuffer = const BleRecordingSampleBuffer();
  bool _recordingActive = false;

  BleRecordingMetrics _metrics = const BleRecordingMetrics();
  final StreamController<BleRecordingMetrics> _metricsController =
      StreamController<BleRecordingMetrics>.broadcast();

  List<BleDiscoveredDevice> _discoveredDevices = const [];
  final StreamController<List<BleDiscoveredDevice>> _discoveredController =
      StreamController<List<BleDiscoveredDevice>>.broadcast();

  StreamSubscription<List<ScanResult>>? _scanSub;
  final Map<String, BleDiscoveredDevice> _scanResults = {};
  Timer? _metricsTicker;

  static const Duration _capabilityDiscoveryTimeout = Duration(seconds: 8);
  static const Duration _metricsTimeoutPublishInterval = Duration(seconds: 1);

  /// Latest metrics snapshot, then live updates.
  @override
  Stream<BleRecordingMetrics> get metricsStream async* {
    yield _metrics;
    yield* _metricsController.stream;
  }

  @override
  BleRecordingMetrics get metrics => _metrics;

  /// Latest discovered-devices list, then live updates.
  @override
  Stream<List<BleDiscoveredDevice>> get discoveredDevicesStream async* {
    yield _discoveredDevices;
    yield* _discoveredController.stream;
  }

  List<BleDiscoveredDevice> get discoveredDevices => _discoveredDevices;

  @override
  BleRecordingSampleBuffer currentSampleBuffer() => _sampleBuffer;

  // ── Recording lifecycle ────────────────────────────────────────────────

  @override
  void startRecording() {
    _recordingActive = true;
    _sampleBuffer = const BleRecordingSampleBuffer();
    final desiredAssignments = _deviceRepository.resolveCapabilityAssignments();
    if (_connections.isEmpty ||
        !const MapEquality<BleSensorCapability, BleSensorDevice>()
            .equals(_capabilityOwners, desiredAssignments)) {
      refreshConnections();
    } else {
      _publishMetrics(recordSamples: true);
      _scheduleMetricsTimeoutTicker();
    }
  }

  @override
  BleRecordingSampleBuffer stopRecording() {
    _recordingActive = false;
    disconnectAll();
    final buffer = _sampleBuffer.trimmed();
    _sampleBuffer = const BleRecordingSampleBuffer();
    _setMetrics(const BleRecordingMetrics());
    return buffer;
  }

  @override
  void refreshConnections() {
    disconnectAll();
    _capabilityOwners.clear();
    _deviceRepository.resolveCapabilityAssignments().forEach((capability, device) {
      _capabilityOwners[capability] = device;
    });
    final grouped = <String, List<BleSensorCapability>>{};
    _capabilityOwners.forEach((capability, device) {
      grouped.putIfAbsent(device.address, () => []).add(capability);
    });
    grouped.forEach((address, capabilities) {
      final device = _capabilityOwners.values.firstWhere(
        (it) => it.address == address,
      );
      final connection = BleGattConnection(
        deviceId: device.id,
        displayName: device.displayName,
        address: address,
        capabilities: capabilities.toSet(),
        wheelCircumferenceMm: device.wheelCircumferenceMm,
        listener: _connectionListener,
      );
      _connections[address] = connection;
      unawaited(connection.connect());
    });
    _publishMetrics();
    _scheduleMetricsTimeoutTicker();
  }

  @override
  void disconnectAll() {
    _stopMetricsTimeoutTicker();
    for (final connection in _connections.values) {
      connection.disconnect();
    }
    _connections.clear();
    _capabilityOwners.clear();
    _publishMetrics();
  }

  // ── Scanning ───────────────────────────────────────────────────────────

  @override
  Future<void> startScan({bool showAllDevices = false}) async {
    await stopScan();
    _scanResults.clear();
    _publishScanResults();
    if (!await FlutterBluePlus.isSupported) return;
    final withServices = showAllDevices
        ? const <Guid>[]
        : BleUuids.scanServiceUuids.map(Guid.new).toList();
    _scanSub = FlutterBluePlus.onScanResults.listen((results) {
      for (final result in results) {
        _addScanResult(result);
      }
    });
    try {
      await FlutterBluePlus.startScan(
        withServices: withServices,
        androidScanMode: AndroidScanMode.lowLatency,
      );
    } catch (error) {
      debugPrint('BleSensorCoordinator startScan failed: $error');
    }
    await _addBondedDevices();
    _publishScanResults();
  }

  @override
  Future<void> stopScan() async {
    await _scanSub?.cancel();
    _scanSub = null;
    try {
      if (FlutterBluePlus.isScanningNow) {
        await FlutterBluePlus.stopScan();
      }
    } catch (error) {
      debugPrint('BleSensorCoordinator stopScan failed: $error');
    }
  }

  /// Connects to [address], enumerates its services and returns the capabilities
  /// they map to, then disconnects. Port of the Kotlin `discoverCapabilities`.
  @override
  Future<Set<BleSensorCapability>> discoverCapabilities(String address) async {
    if (!await FlutterBluePlus.isSupported) return const {};
    final device = BluetoothDevice.fromId(address);
    final discovered = <BleSensorCapability>{};
    try {
      await device.connect(
        license: License.nonprofit,
        timeout: _capabilityDiscoveryTimeout,
      );
      final services = await device.discoverServices();
      for (final service in services) {
        discovered.addAll(BleUuids.capabilitiesForService(service.uuid.str128));
      }
    } catch (error) {
      debugPrint('BleSensorCoordinator discoverCapabilities failed: $error');
    } finally {
      try {
        await device.disconnect();
      } catch (_) {
        // Ignore.
      }
    }
    return discovered;
  }

  Future<void> _addBondedDevices() async {
    List<BluetoothDevice> bonded;
    try {
      bonded = await FlutterBluePlus.bondedDevices;
    } catch (_) {
      // Not supported on this platform (e.g. iOS).
      return;
    }
    for (final device in bonded) {
      final address = device.remoteId.str.toUpperCase();
      _scanResults.putIfAbsent(
        address,
        () => BleDiscoveredDevice(
          address: address,
          name: device.platformName.isEmpty ? null : device.platformName,
          rssi: null,
          suggestedCapabilities: const {},
        ),
      );
    }
  }

  void _addScanResult(ScanResult result) {
    final address = result.device.remoteId.str.toUpperCase();
    final serviceCapabilities = <BleSensorCapability>{
      for (final uuid in result.advertisementData.serviceUuids)
        ...BleUuids.capabilitiesForService(uuid.str128),
    };
    // 0xFE1F, not GFDI: GFDI is a GATT service that only exists once connected,
    // so an advertisement never carries it.
    final advertisesGarmin = result.advertisementData.serviceUuids.any(
      (uuid) => uuid.str128 == BleUuids.garminMemberService,
    );
    final existing = _scanResults[address];
    final name = result.advertisementData.advName.isNotEmpty
        ? result.advertisementData.advName
        : existing?.name;
    _scanResults[address] = BleDiscoveredDevice(
      address: address,
      name: name,
      rssi: result.rssi,
      suggestedCapabilities: {
        ...?existing?.suggestedCapabilities,
        ...serviceCapabilities,
      },
      // Sticky across advertisements: a watch does not put every service in
      // every packet, so one sighting of GFDI settles it for this scan.
      advertisesGarminService:
          advertisesGarmin || (existing?.advertisesGarminService ?? false),
    );
    _publishScanResults();
  }

  void _publishScanResults() {
    const minRssi = -2147483648; // Int.MIN_VALUE — sorts unknown RSSI last.
    final sorted = _scanResults.values.toList()
      ..sort((a, b) {
        final rssiCompare =
            (b.rssi ?? minRssi).compareTo(a.rssi ?? minRssi);
        if (rssiCompare != 0) return rssiCompare;
        return _displayLabel(a).compareTo(_displayLabel(b));
      });
    _discoveredDevices = List.unmodifiable(sorted);
    if (!_discoveredController.isClosed) {
      _discoveredController.add(_discoveredDevices);
    }
  }

  String _displayLabel(BleDiscoveredDevice device) {
    final name = device.name;
    if (name == null) return device.address;
    return name.isEmpty ? device.address : name;
  }

  // ── Connection listener → metrics ──────────────────────────────────────

  late final BleConnectionListener _connectionListener =
      _CoordinatorConnectionListener(this);

  void _onConnectionStatusChanged() {
    _publishMetrics();
    _scheduleMetricsTimeoutTicker();
  }

  void _onMetricsUpdated() {
    _publishMetrics(recordSamples: true);
    _scheduleMetricsTimeoutTicker();
  }

  void _onBatteryLevelChanged(String deviceId, int batteryPercent) {
    _deviceRepository.updateBatteryLevel(deviceId, batteryPercent);
    _publishMetrics();
    _scheduleMetricsTimeoutTicker();
  }

  BleRecordingMetrics _collectMetrics([DateTime? now]) {
    final effectiveNow = now ?? DateTime.now();
    final statuses = <BleDeviceConnectionStatus>[];
    _connections.forEach((address, connection) {
      final device = _capabilityOwners.values
          .firstWhereOrNull((it) => it.address == address);
      statuses.add(
        BleDeviceConnectionStatus(
          deviceId: device?.id ?? '',
          displayName: device?.displayName ?? address,
          address: address,
          status: connection.connectionStatus,
          capabilities: {
            for (final entry in _capabilityOwners.entries)
              if (entry.value.address == address) entry.key,
          },
          batteryPercent: connection.batteryPercent ?? device?.batteryPercent,
        ),
      );
    });
    final hrConnection = _connectionForCapability(BleSensorCapability.heartRate);
    final cadenceConnection =
        _connectionForCapability(BleSensorCapability.cyclingCadence) ??
            _connectionForCapability(BleSensorCapability.cyclingPower);
    final powerConnection =
        _connectionForCapability(BleSensorCapability.cyclingPower);
    final speedConnection =
        _connectionForCapability(BleSensorCapability.cyclingSpeedDistance);
    final runningConnection =
        _connectionForCapability(BleSensorCapability.runningSpeedCadence);
    final running = runningConnection?.runningAggregator.current(effectiveNow);
    return BleRecordingMetrics(
      heartRateBpm: hrConnection?.heartRateAggregator.current(effectiveNow),
      heartRateNoSignal: hrConnection?.heartRateNoSignal ?? false,
      cyclingCadenceRpm:
          cadenceConnection?.cyclingCadenceAggregator.current(effectiveNow),
      powerWatts: powerConnection?.powerAggregator.current(effectiveNow),
      cyclingSpeedMetersPerSecond:
          speedConnection?.cyclingSpeedAggregator.current(effectiveNow),
      runningSpeedMetersPerSecond: running?.$1,
      runningCadenceRpm: running?.$2,
      deviceStatuses: statuses,
    );
  }

  void _appendSamples(DateTime now, BleRecordingMetrics metrics) {
    var next = _sampleBuffer;
    final heartRate = metrics.heartRateBpm;
    if (heartRate != null) next = next.withHeartRateSample(now, heartRate);
    final power = metrics.powerWatts;
    if (power != null) next = next.withPowerSample(now, power);
    final cadence = metrics.cyclingCadenceRpm;
    if (cadence != null) next = next.withCyclingCadenceSample(now, cadence);
    final cyclingSpeed = metrics.cyclingSpeedMetersPerSecond;
    if (cyclingSpeed != null) {
      next = next.withSpeedSample(now, cyclingSpeed, false);
    }
    final runningSpeed = metrics.runningSpeedMetersPerSecond;
    if (runningSpeed != null) {
      next = next.withSpeedSample(now, runningSpeed, true);
    }
    final runningCadence = metrics.runningCadenceRpm;
    if (runningCadence != null) {
      next = next.withStepsCadenceSample(now, runningCadence);
    }
    _sampleBuffer = next;
  }

  BleGattConnection? _connectionForCapability(BleSensorCapability capability) {
    final device = _capabilityOwners[capability];
    if (device == null) return null;
    return _connections[device.address];
  }

  void _publishMetrics({DateTime? now, bool recordSamples = false}) {
    final effectiveNow = now ?? DateTime.now();
    final metrics = _collectMetrics(effectiveNow);
    _setMetrics(metrics);
    if (recordSamples && _recordingActive) {
      _appendSamples(effectiveNow, metrics);
    }
  }

  void _setMetrics(BleRecordingMetrics metrics) {
    _metrics = metrics;
    if (!_metricsController.isClosed) {
      _metricsController.add(metrics);
    }
  }

  void _scheduleMetricsTimeoutTicker() {
    if (_connections.isEmpty || _metricsTicker != null) return;
    _metricsTicker = Timer(_metricsTimeoutPublishInterval, _onMetricsTick);
  }

  void _onMetricsTick() {
    _metricsTicker = null;
    if (_connections.isEmpty) return;
    // Refresh displayed metrics only; samples are recorded on BLE notifications.
    _publishMetrics();
    _scheduleMetricsTimeoutTicker();
  }

  void _stopMetricsTimeoutTicker() {
    _metricsTicker?.cancel();
    _metricsTicker = null;
  }

  void dispose() {
    _stopMetricsTimeoutTicker();
    unawaited(_scanSub?.cancel());
    for (final connection in _connections.values) {
      connection.disconnect();
    }
    _connections.clear();
    unawaited(_metricsController.close());
    unawaited(_discoveredController.close());
  }
}

/// Adapts the coordinator's private handlers to the [BleConnectionListener]
/// interface each [BleGattConnection] calls back through.
class _CoordinatorConnectionListener implements BleConnectionListener {
  _CoordinatorConnectionListener(this._coordinator);

  final BleSensorCoordinator _coordinator;

  @override
  void onConnectionStatusChanged(BleConnectionStatus status) =>
      _coordinator._onConnectionStatusChanged();

  @override
  void onMetricsUpdated() => _coordinator._onMetricsUpdated();

  @override
  void onBatteryLevelChanged(String deviceId, int batteryPercent) =>
      _coordinator._onBatteryLevelChanged(deviceId, batteryPercent);
}
