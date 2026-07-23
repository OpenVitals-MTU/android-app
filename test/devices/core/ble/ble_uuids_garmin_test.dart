import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/devices/core/ble/ble_uuids.dart';
import 'package:openvitals/devices/garmin/garmin_uuids.dart';
import 'package:openvitals/devices/garmin/garmin_device_names.dart';

void main() {
  group('the Garmin scan filter', () {
    test('filters on the ADVERTISED member service, not GFDI', () {
      // Regression, found on a real vívoactive 5 (2026-07-22). Its
      // advertisement is:
      //   mServiceUuids=[0000fe1f-0000-1000-8000-00805f9b34fb]
      //   mManufacturerSpecificData={135=[...]}   // 0x0087 = Garmin
      //   mDeviceName=vívoactive 5
      // and carries NO GFDI UUID — GFDI is a GATT service that appears only
      // after connecting. Filtering the scan on it matched nothing, so the
      // watch was invisible unless the user toggled "Show all devices".
      expect(BleUuids.scanServiceUuids, contains(BleUuids.garminMemberService));
      expect(
        BleUuids.scanServiceUuids,
        isNot(contains(GarminUuids.gfdiServiceV1)),
        reason: 'a scan filter on a connect-only GATT service hides every '
            'Garmin watch from discovery',
      );
    });

    test('the member service grants no sensor capabilities', () {
      // A watch streams nothing live. If this ever returned a capability, the
      // watch would enter capability assignment and the recording coordinator
      // would connect to it and wait for notifications it never sends.
      expect(
        BleUuids.capabilitiesForService(BleUuids.garminMemberService),
        isEmpty,
      );
    });

    test('the standard sensor services are still in the filter', () {
      // The Garmin entry must be an addition, not a replacement.
      expect(BleUuids.scanServiceUuids, containsAll(<String>[
        BleUuids.heartRate.serviceUuid,
        BleUuids.cyclingSpeedCadence.serviceUuid,
        BleUuids.cyclingPower.serviceUuid,
        BleUuids.runningSpeedCadence.serviceUuid,
      ]));
    });
  });

  group('classifying the scan result', () {
    test('the member service surfaces a device but does not name its kind', () {
      // 0xFE1F gets the device into the scan list; the NAME classifies it. A
      // nameless advert has no family to match, so it is not a Garmin watch.
      expect(isGarminSyncDeviceName(null), isFalse);
    });

    test('a watch found via "Show all devices" is caught by its name', () {
      // That path applies no service filter, so the name is the only evidence —
      // and it is what classifies the kind.
      expect(isGarminWatchName('vívoactive 5'), isTrue);
      expect(isGarminSyncDeviceName('vívoactive 5'), isTrue);
    });
  });
}
