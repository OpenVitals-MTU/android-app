import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../../di/providers.dart';
import '../../../domain/model/ble_sensor_models.dart';

/// One paired BLE sensor as the dashboard sees it: the persisted registry entry
/// merged with the live connection/battery reading. Port of the Kotlin
/// `DashboardSensorDeviceStatus`.
class DashboardSensorDeviceStatus {
  const DashboardSensorDeviceStatus({
    required this.id,
    required this.displayName,
    required this.enabled,
    required this.connectionStatus,
    required this.batteryPercent,
  });

  final String id;
  final String displayName;
  final bool enabled;
  final BleConnectionStatus connectionStatus;
  final int? batteryPercent;
}

/// The dashboard's roll-up of every paired BLE sensor (Kotlin
/// `DashboardSensorStatus`). The derived getters drive the sensor status card
/// and the top-bar action's visibility.
class DashboardSensorStatus {
  const DashboardSensorStatus({
    this.devices = const <DashboardSensorDeviceStatus>[],
  });

  final List<DashboardSensorDeviceStatus> devices;

  bool get hasDevices => devices.isNotEmpty;

  int get enabledCount => devices.where((d) => d.enabled).length;

  int get connectedCount => devices
      .where((d) => d.connectionStatus == BleConnectionStatus.connected)
      .length;

  /// The lowest reported battery across the paired sensors, or null when none
  /// of them has reported one yet.
  int? get lowestBatteryPercent {
    int? lowest;
    for (final device in devices) {
      final percent = device.batteryPercent;
      if (percent == null) continue;
      if (lowest == null || percent < lowest) lowest = percent;
    }
    return lowest;
  }
}

/// Merges the paired-device registry with the live recording metrics, mirroring
/// the Kotlin `List<BleSensorDevice>.toDashboardSensorStatus(...)`.
///
/// A live status is looked up by device id first, then by MAC address (the
/// coordinator keys some statuses by address for devices it has not matched back
/// to a registry id yet). The live reading wins for both connection state and
/// battery; the persisted battery is only a fallback.
DashboardSensorStatus toDashboardSensorStatus(
  List<BleSensorDevice> devices,
  List<BleDeviceConnectionStatus> connectionStatuses,
) {
  final byId = <String, BleDeviceConnectionStatus>{
    for (final status in connectionStatuses) status.deviceId: status,
  };
  final byAddress = <String, BleDeviceConnectionStatus>{
    for (final status in connectionStatuses) status.address: status,
  };
  return DashboardSensorStatus(
    devices: [
      for (final device in devices)
        () {
          final live = byId[device.id] ?? byAddress[device.address];
          return DashboardSensorDeviceStatus(
            id: device.id,
            displayName: device.displayName,
            enabled: device.enabled,
            connectionStatus: live?.status ?? BleConnectionStatus.disconnected,
            batteryPercent: live?.batteryPercent ?? device.batteryPercent,
          );
        }(),
    ],
  );
}

/// The dashboard's live sensor roll-up (Kotlin `observeSensorStatus`, which
/// combines the device registry flow with the coordinator's metrics flow).
final dashboardSensorStatusProvider = Provider<DashboardSensorStatus>((ref) {
  final devices = ref.watch(bleDevicesProvider).value ?? const <BleSensorDevice>[];
  final metrics = ref.watch(bleMetricsProvider).value;
  return toDashboardSensorStatus(
    devices,
    metrics?.deviceStatuses ?? const <BleDeviceConnectionStatus>[],
  );
});
