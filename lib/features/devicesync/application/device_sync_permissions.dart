/// Runtime Bluetooth permission gate for phone-to-phone sync.
///
/// Sync needs SCAN + CONNECT (like the BLE sensor stack) plus ADVERTISE for the
/// host's discoverable request (API 31+). On pre-31 `permission_handler` reports
/// them granted automatically, so this returns true without prompting.
library;

import 'package:permission_handler/permission_handler.dart';

/// Requests the permissions needed to sync. Returns true only if all are granted.
Future<bool> ensureSyncBluetoothPermissions() async {
  final statuses = await [
    Permission.bluetoothScan,
    Permission.bluetoothConnect,
    Permission.bluetoothAdvertise,
  ].request();
  return statuses.values.every((status) => status.isGranted);
}
