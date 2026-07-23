import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/devices/core/ble/ble_uuids.dart';
import 'package:openvitals/devices/garmin/garmin_scan_classifier.dart';

void main() {
  group('GarminScanClassifier', () {
    const classifier = GarminScanClassifier();

    test('claims an advertisement carrying the member service', () {
      expect(
        classifier.advertisesSyncService([BleUuids.garminMemberService]),
        isTrue,
      );
    });

    test('claims it alongside unrelated advertised services', () {
      expect(
        classifier.advertisesSyncService([
          '0000180d-0000-1000-8000-00805f9b34fb', // heart rate
          BleUuids.garminMemberService,
        ]),
        isTrue,
      );
    });

    test('does not claim a live sensor advertisement', () {
      expect(
        classifier.advertisesSyncService([
          '0000180d-0000-1000-8000-00805f9b34fb', // heart rate
          '00001816-0000-1000-8000-00805f9b34fb', // cycling speed/cadence
        ]),
        isFalse,
      );
    });

    test('does not claim an empty advertisement', () {
      expect(classifier.advertisesSyncService(const []), isFalse);
    });

    // Regression guard: the classifier keys on the ADVERTISED member service
    // (0xFE1F), never the GFDI transport UUID, which is GATT-only and never
    // advertised — filtering on it would hide every watch from discovery.
    test('does not key on the connect-only GFDI service', () {
      expect(
        classifier.advertisesSyncService(
          const ['6a4e2401-667b-11e3-949a-0800200c9a66'], // gfdiServiceV1
        ),
        isFalse,
      );
    });
  });
}
