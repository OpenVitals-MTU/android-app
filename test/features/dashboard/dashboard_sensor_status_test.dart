import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/features/dashboard/presentation/dashboard_sensor_status.dart';

BleSensorDevice _device({
  required String id,
  String address = 'AA:BB:CC:DD:EE:FF',
  bool enabled = true,
  int? batteryPercent,
  BleDeviceKind kind = BleDeviceKind.sensor,
}) =>
    BleSensorDevice(
      id: id,
      displayName: 'Sensor $id',
      address: address,
      bluetoothName: null,
      capabilities: const <BleSensorCapability>{BleSensorCapability.heartRate},
      enabled: enabled,
      wheelCircumferenceMm: null,
      batteryPercent: batteryPercent,
      addedAt: DateTime(2026, 1, 1),
      kind: kind,
    );

BleDeviceConnectionStatus _status({
  required String deviceId,
  String address = 'AA:BB:CC:DD:EE:FF',
  BleConnectionStatus status = BleConnectionStatus.connected,
  int? batteryPercent,
}) =>
    BleDeviceConnectionStatus(
      deviceId: deviceId,
      displayName: 'Sensor $deviceId',
      address: address,
      status: status,
      capabilities: const <BleSensorCapability>{BleSensorCapability.heartRate},
      batteryPercent: batteryPercent,
    );

void main() {
  group('toDashboardSensorStatus', () {
    test('the live battery wins over the persisted one', () {
      final result = toDashboardSensorStatus(
        [_device(id: 'a', batteryPercent: 90)],
        [_status(deviceId: 'a', batteryPercent: 42)],
      );

      expect(result.devices.single.batteryPercent, 42);
      expect(result.devices.single.connectionStatus,
          BleConnectionStatus.connected);
    });

    test('the persisted battery is the fallback when no live one is reported',
        () {
      final result = toDashboardSensorStatus(
        [_device(id: 'a', batteryPercent: 90)],
        [_status(deviceId: 'a')],
      );

      expect(result.devices.single.batteryPercent, 90);
    });

    test('the lookup falls back from device id to address', () {
      final result = toDashboardSensorStatus(
        [_device(id: 'registry-id', address: 'AA:11', batteryPercent: 90)],
        // The live status was keyed by the raw MAC, not the registry id.
        [
          _status(
            deviceId: 'runtime-id',
            address: 'AA:11',
            status: BleConnectionStatus.reconnecting,
            batteryPercent: 33,
          ),
        ],
      );

      final device = result.devices.single;
      expect(device.batteryPercent, 33);
      expect(device.connectionStatus, BleConnectionStatus.reconnecting);
    });

    test('a device with no live status at all reads as disconnected', () {
      final result = toDashboardSensorStatus(
        [_device(id: 'a', address: 'AA:11', batteryPercent: 55)],
        const <BleDeviceConnectionStatus>[],
      );

      final device = result.devices.single;
      expect(device.connectionStatus, BleConnectionStatus.disconnected);
      expect(device.batteryPercent, 55);
    });
  });

  group('derived getters', () {
    test('a paired watch alone puts up no icon: it is not in the screen the '
        'icon opens', () {
      // The bug this pins: pairing a smartwatch made the top-bar battery icon
      // appear, and tapping it opened an empty Sensors & devices screen — the
      // watch is listed under Settings > Watches, not there.
      final status = toDashboardSensorStatus(
        [_device(id: 'watch', kind: BleDeviceKind.watch, batteryPercent: 80)],
        const [],
      );

      expect(status.hasDevices, isFalse);
      expect(status.lowestBatteryPercent, isNull);
    });

    test('a watch beside a sensor adds neither counts nor battery', () {
      final status = toDashboardSensorStatus(
        [
          _device(id: 'hrm', batteryPercent: 60),
          _device(id: 'watch', kind: BleDeviceKind.watch, batteryPercent: 20),
        ],
        const [],
      );

      expect(status.devices, hasLength(1));
      expect(status.lowestBatteryPercent, 60);
    });

    test('a bike computer still counts: the screen lists it', () {
      final status = toDashboardSensorStatus(
        [_device(id: 'edge', kind: BleDeviceKind.bikeComputer)],
        const [],
      );

      expect(status.hasDevices, isTrue);
    });

    test('an empty status has no devices and no battery', () {
      const status = DashboardSensorStatus();
      expect(status.hasDevices, isFalse);
      expect(status.enabledCount, 0);
      expect(status.connectedCount, 0);
      expect(status.lowestBatteryPercent, isNull);
    });

    test('counts enabled/connected devices and the lowest battery', () {
      final status = toDashboardSensorStatus(
        [
          _device(id: 'a', address: 'AA:11'),
          _device(id: 'b', address: 'AA:22'),
          _device(id: 'c', address: 'AA:33', enabled: false),
        ],
        [
          _status(deviceId: 'a', address: 'AA:11', batteryPercent: 80),
          _status(
            deviceId: 'b',
            address: 'AA:22',
            status: BleConnectionStatus.connecting,
            batteryPercent: 17,
          ),
          // 'c' is disabled and never connects: no live status, no battery.
        ],
      );

      expect(status.hasDevices, isTrue);
      expect(status.enabledCount, 2);
      expect(status.connectedCount, 1);
      expect(status.lowestBatteryPercent, 17);
    });

    test('the lowest battery ignores devices that never reported one', () {
      final status = toDashboardSensorStatus(
        [
          _device(id: 'a', address: 'AA:11'),
          _device(id: 'b', address: 'AA:22', batteryPercent: 64),
        ],
        const <BleDeviceConnectionStatus>[],
      );

      expect(status.lowestBatteryPercent, 64);
    });
  });
}
