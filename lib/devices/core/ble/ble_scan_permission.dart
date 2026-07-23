import 'package:permission_handler/permission_handler.dart';

/// Requests the runtime permissions a BLE scan needs and reports whether they
/// were all granted.
///
/// Mirrors the Kotlin `BleDevicesViewModel.requiredBluetoothPermissions`: on
/// Android 12+ (API 31) `BLUETOOTH_SCAN` + `BLUETOOTH_CONNECT` are runtime
/// grants that must be requested before scanning (flutter_blue_plus does not
/// request them). On older Android these are not runtime permissions and
/// `permission_handler` reports them granted automatically (Kotlin's required
/// array is empty there), so this returns true without a prompt.
Future<bool> ensureBleScanPermissions() async {
  final statuses = await [
    Permission.bluetoothScan,
    Permission.bluetoothConnect,
  ].request();
  return statuses.values.every((status) => status.isGranted);
}
