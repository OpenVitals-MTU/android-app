import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter_blue_plus/flutter_blue_plus.dart';

import '../../domain/model/ble_sensor_models.dart';
import 'aggregators/ble_aggregators.dart';
import 'ble_uuids.dart';
import 'parsers/ble_parsers.dart';

/// Callbacks the coordinator supplies to a [BleGattConnection]. Port of the
/// Kotlin `BleConnectionListener`.
abstract interface class BleConnectionListener {
  void onConnectionStatusChanged(BleConnectionStatus status);
  void onMetricsUpdated();
  void onBatteryLevelChanged(String deviceId, int batteryPercent);
}

/// Port of the Kotlin `BleGattConnection` over `flutter_blue_plus`.
///
/// The Android `BluetoothGattCallback` state machine is replaced by
/// subscriptions to `flutter_blue_plus` streams: [BluetoothDevice.connectionState]
/// drives connect/reconnect, and each subscribed characteristic's
/// [BluetoothCharacteristic.onValueReceived] feeds raw bytes into the parsers →
/// aggregators. Aggregator outputs are read by the coordinator when it collects
/// metrics.
///
/// Runtime verification is deferred (device-dependent); this is a compile-clean,
/// structurally faithful port.
class BleGattConnection {
  BleGattConnection({
    required this.deviceId,
    required this.displayName,
    required this.address,
    required this.capabilities,
    required int? wheelCircumferenceMm,
    required this.listener,
  })  : _device = BluetoothDevice.fromId(address),
        cyclingSpeedAggregator = BleCyclingSpeedAggregator(
          wheelCircumferenceMeters: (wheelCircumferenceMm ?? 2100) / 1000.0,
        );

  final String deviceId;
  final String displayName;
  final String address;
  final Set<BleSensorCapability> capabilities;
  final BleConnectionListener listener;

  final BluetoothDevice _device;

  final heartRateAggregator = BleHeartRateAggregator();
  final powerAggregator = BlePowerAggregator();
  final cyclingCadenceAggregator = BleCyclingCadenceAggregator();
  final BleCyclingSpeedAggregator cyclingSpeedAggregator;
  final runningAggregator = BleRunningSpeedCadenceAggregator();

  BleConnectionStatus connectionStatus = BleConnectionStatus.disconnected;
  bool heartRateNoSignal = false;
  int? batteryPercent;

  bool _closed = false;
  StreamSubscription<BluetoothConnectionState>? _connectionSub;
  final List<StreamSubscription<List<int>>> _valueSubs = [];
  final Set<String> _subscribedCharacteristics = {};

  Future<void> connect() async {
    if (_closed) return;
    if (!await FlutterBluePlus.isSupported) {
      _updateStatus(BleConnectionStatus.disconnected);
      return;
    }
    _updateStatus(BleConnectionStatus.connecting);
    _connectionSub ??=
        _device.connectionState.listen(_onConnectionStateChanged);
    try {
      await _device.connect(license: License.nonprofit);
    } catch (error) {
      debugPrint('BleGattConnection connect failed: $error');
    }
  }

  void disconnect() {
    _closed = true;
    _subscribedCharacteristics.clear();
    _cancelValueSubs();
    resetAggregators();
    unawaited(_connectionSub?.cancel());
    _connectionSub = null;
    unawaited(_safeDisconnect());
    _updateStatus(BleConnectionStatus.disconnected);
  }

  void resetAggregators() {
    heartRateAggregator.reset();
    powerAggregator.reset();
    cyclingCadenceAggregator.reset();
    cyclingSpeedAggregator.reset();
    runningAggregator.reset();
    heartRateNoSignal = false;
  }

  Future<void> _safeDisconnect() async {
    try {
      await _device.disconnect();
    } catch (_) {
      // Ignore — the device may already be gone.
    }
  }

  void _updateStatus(BleConnectionStatus status) {
    connectionStatus = status;
    listener.onConnectionStatusChanged(status);
  }

  void _cancelValueSubs() {
    for (final sub in _valueSubs) {
      unawaited(sub.cancel());
    }
    _valueSubs.clear();
  }

  Future<void> _onConnectionStateChanged(BluetoothConnectionState state) async {
    switch (state) {
      case BluetoothConnectionState.connected:
        _updateStatus(BleConnectionStatus.connected);
        await _onConnected();
      case BluetoothConnectionState.disconnected:
        _subscribedCharacteristics.clear();
        _cancelValueSubs();
        resetAggregators();
        if (!_closed) {
          _updateStatus(BleConnectionStatus.reconnecting);
          try {
            await _device.connect(license: License.nonprofit);
          } catch (error) {
            debugPrint('BleGattConnection reconnect failed: $error');
          }
        } else {
          _updateStatus(BleConnectionStatus.disconnected);
        }
    }
  }

  Future<void> _onConnected() async {
    List<BluetoothService> services;
    try {
      services = await _device.discoverServices();
    } catch (error) {
      debugPrint('BleGattConnection discoverServices failed: $error');
      return;
    }
    final toNotify = <BluetoothCharacteristic>[];
    for (final service in services) {
      for (final characteristic in service.characteristics) {
        final charCapabilities = BleUuids.capabilitiesForCharacteristic(
          characteristic.uuid.str128,
        );
        if (charCapabilities.any(capabilities.contains)) {
          toNotify.add(characteristic);
        }
      }
    }
    await _readBatteryLevel(services);
    for (final characteristic in toNotify) {
      await _enableNotifications(characteristic);
    }
  }

  Future<void> _readBatteryLevel(List<BluetoothService> services) async {
    BluetoothCharacteristic? characteristic;
    for (final service in services) {
      if (service.uuid.str128 != BleUuids.batteryService) continue;
      for (final candidate in service.characteristics) {
        if (candidate.uuid.str128 == BleUuids.batteryLevel) {
          characteristic = candidate;
          break;
        }
      }
    }
    if (characteristic == null) return;
    try {
      final value = await characteristic.read();
      _updateBatteryLevel(value);
    } catch (error) {
      debugPrint('BleGattConnection battery read failed: $error');
    }
  }

  Future<void> _enableNotifications(
    BluetoothCharacteristic characteristic,
  ) async {
    if (!_subscribedCharacteristics.add(characteristic.uuid.str128)) return;
    _valueSubs.add(
      characteristic.onValueReceived.listen(
        (value) => _handleCharacteristicChanged(characteristic, value),
      ),
    );
    try {
      await characteristic.setNotifyValue(true);
    } catch (error) {
      debugPrint('BleGattConnection setNotifyValue failed: $error');
    }
  }

  void _updateBatteryLevel(List<int> value) {
    if (value.isEmpty) return;
    final percent = (value[0] & 0xFF).clamp(0, 100);
    batteryPercent = percent;
    listener.onBatteryLevelChanged(deviceId, percent);
  }

  void _handleCharacteristicChanged(
    BluetoothCharacteristic characteristic,
    List<int> value,
  ) {
    if (value.isEmpty) return;
    final now = DateTime.now();
    final sensorName = _device.platformName.isEmpty ? null : _device.platformName;
    final uuid = characteristic.uuid.str128;

    if (capabilities.contains(BleSensorCapability.heartRate) &&
        uuid == BleUuids.heartRate.measurementUuid) {
      final heartRate = BleHeartRateParser.parseBytes(value);
      if (heartRate != null) {
        heartRateNoSignal = false;
        heartRateAggregator.add(now, heartRate);
        listener.onMetricsUpdated();
      } else if (BleHeartRateParser.isZeroSignal(value)) {
        heartRateNoSignal = true;
        listener.onMetricsUpdated();
      }
      return;
    }

    if (capabilities.contains(BleSensorCapability.cyclingPower) &&
        uuid == BleUuids.cyclingPower.measurementUuid) {
      final data = BleCyclingPowerParser.parsePayload(value);
      if (data != null) {
        powerAggregator.add(now, data);
        final crank = data.crank;
        if (crank != null) cyclingCadenceAggregator.add(now, crank);
        listener.onMetricsUpdated();
      }
      return;
    }

    if ((capabilities.contains(BleSensorCapability.cyclingCadence) ||
            capabilities.contains(BleSensorCapability.cyclingSpeedDistance)) &&
        uuid == BleUuids.cyclingSpeedCadence.measurementUuid) {
      final parsed = BleCyclingSpeedCadenceParser.parsePayload(value);
      if (parsed != null) {
        final (wheel, crank) = parsed;
        if (wheel != null) cyclingSpeedAggregator.add(now, wheel);
        if (crank != null) cyclingCadenceAggregator.add(now, crank);
        listener.onMetricsUpdated();
      }
      return;
    }

    if (capabilities.contains(BleSensorCapability.runningSpeedCadence) &&
        uuid == BleUuids.runningSpeedCadence.measurementUuid) {
      final data = BleRunningSpeedCadenceParser.parsePayload(value, sensorName);
      if (data != null) {
        runningAggregator.add(now, data);
        listener.onMetricsUpdated();
      }
      return;
    }
  }
}
