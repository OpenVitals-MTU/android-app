import '../core/ble/ble_uuids.dart';
import '../core/ble/device_scan_classifier.dart';

/// Classifies a scanned advertisement as a Garmin file-sync watch by its member
/// service UUID (`0xFE1F`, `BleUuids.garminMemberService`).
///
/// The member service is what a Garmin watch puts in its ADVERTISEMENT (the GFDI
/// transport service is GATT-only, invisible until connected), so it is the one
/// Garmin UUID the shared scanner already carries in its scan filter — this
/// classifier reuses it rather than duplicating the constant.
class GarminScanClassifier implements DeviceScanClassifier {
  const GarminScanClassifier();

  @override
  bool advertisesSyncService(Iterable<String> advertisedServiceUuids) =>
      advertisedServiceUuids.any((uuid) => uuid == BleUuids.garminMemberService);
}
