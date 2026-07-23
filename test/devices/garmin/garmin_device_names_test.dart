import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/domain/model/ble_sensor_models.dart';
import 'package:openvitals/devices/garmin/garmin_device_names.dart';

BleDiscoveredDevice _discovered({
  String? name,
  bool advertisesGarminService = false,
}) =>
    BleDiscoveredDevice(
      address: 'E0:48:24:D5:F7:10',
      name: name,
      rssi: -60,
      suggestedCapabilities: const {},
      advertisesGarminService: advertisesGarminService,
    );

void main() {
  group('isGarminSyncDeviceName', () {
    test('matches the accented names the watches actually advertise', () {
      // The exact string a vívoactive 5 puts in its advertisement — the device
      // in the screenshots this feature was built from.
      expect(isGarminSyncDeviceName('vívoactive 5'), isTrue);
      expect(isGarminSyncDeviceName('fēnix 7X Pro'), isTrue);
    });

    test('matches the unaccented spellings some firmware uses', () {
      expect(isGarminSyncDeviceName('vivoactive 5'), isTrue);
      expect(isGarminSyncDeviceName('fenix 6S Pro Solar'), isTrue);
    });

    test('matches by family, so an unreleased model still onboards', () {
      // The point of family matching over Gadgetbridge's exact-match table.
      expect(isGarminSyncDeviceName('vívoactive 9'), isTrue);
      expect(isGarminSyncDeviceName('Forerunner 1055'), isTrue);
    });

    test('strips the Garmin prefix some models advertise with', () {
      expect(isGarminSyncDeviceName('Garmin Forerunner 265S'), isTrue);
    });

    test('does NOT match HRM chest straps', () {
      // These expose the standard Heart Rate service and belong to the live
      // sensor path — classifying one as a watch would silently break heart
      // rate recording for anyone using one.
      expect(isGarminSyncDeviceName('HRM 200'), isFalse);
      expect(isGarminSyncDeviceName('HRMPro+:123456'), isFalse);
      expect(isGarminSyncDeviceName('HRM600:998877'), isFalse);
    });

    test('does not match other vendors, blanks or null', () {
      expect(isGarminSyncDeviceName('Wahoo TICKR'), isFalse);
      expect(isGarminSyncDeviceName('Polar H10'), isFalse);
      expect(isGarminSyncDeviceName('LE_WH-1000XM4'), isFalse);
      expect(isGarminSyncDeviceName('   '), isFalse);
      expect(isGarminSyncDeviceName(null), isFalse);
    });
  });

  group('isGarminSyncDevice', () {
    test('the advertised Garmin member service settles it even with no name',
        () {
      expect(isGarminSyncDevice(_discovered(advertisesGarminService: true)),
          isTrue);
    });

    test('falls back to the name when the service was not advertised', () {
      expect(isGarminSyncDevice(_discovered(name: 'vívoactive 5')), isTrue);
    });

    test('a nameless, serviceless advertisement is not a watch', () {
      expect(isGarminSyncDevice(_discovered()), isFalse);
    });
  });
}
