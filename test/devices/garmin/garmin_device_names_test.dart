import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/devices/garmin/garmin_device_names.dart';

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

    test('also matches Edge bike computers (they sync FIT files too)', () {
      expect(isGarminSyncDeviceName('Edge 840'), isTrue);
      expect(isGarminSyncDeviceName('Garmin Edge 1040'), isTrue);
    });
  });

  group('isGarminWatchName', () {
    test('matches the watch families, not the Edge', () {
      expect(isGarminWatchName('vívoactive 5'), isTrue);
      expect(isGarminWatchName('fēnix 7X Pro'), isTrue);
      expect(isGarminWatchName('Garmin Forerunner 265S'), isTrue);
      expect(isGarminWatchName('Edge 840'), isFalse);
    });

    test('does not match straps, other vendors, blanks or null', () {
      expect(isGarminWatchName('HRM 200'), isFalse);
      expect(isGarminWatchName('Wahoo TICKR'), isFalse);
      expect(isGarminWatchName('   '), isFalse);
      expect(isGarminWatchName(null), isFalse);
    });
  });

  group('isGarminBikeComputerName', () {
    test('matches the Edge family, including sub-models and the prefix', () {
      expect(isGarminBikeComputerName('Edge 840'), isTrue);
      expect(isGarminBikeComputerName('Edge Explore 2'), isTrue);
      expect(isGarminBikeComputerName('Edge MTB'), isTrue);
      expect(isGarminBikeComputerName('Garmin Edge 1040'), isTrue);
    });

    test('is disjoint from the watch families', () {
      expect(isGarminBikeComputerName('vívoactive 5'), isFalse);
      expect(isGarminBikeComputerName('fēnix 7X Pro'), isFalse);
      expect(isGarminBikeComputerName('Forerunner 265S'), isFalse);
    });

    test('does not match straps, other vendors, blanks or null', () {
      expect(isGarminBikeComputerName('HRM 200'), isFalse);
      expect(isGarminBikeComputerName('Wahoo TICKR'), isFalse);
      expect(isGarminBikeComputerName('   '), isFalse);
      expect(isGarminBikeComputerName(null), isFalse);
    });
  });
}
