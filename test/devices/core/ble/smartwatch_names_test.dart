import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/devices/core/ble/smartwatch_names.dart';

void main() {
  group('isSmartwatchName', () {
    test('matches the Galaxy Watch (the test rig)', () {
      expect(isSmartwatchName('Galaxy Watch8 (89FZ)'), isTrue);
      expect(isSmartwatchName('Galaxy Watch4 Classic'), isTrue);
    });

    test('matches other wrist smartwatch families', () {
      expect(isSmartwatchName('Pixel Watch 2'), isTrue);
      expect(isSmartwatchName('TicWatch Pro 5'), isTrue);
      expect(isSmartwatchName('Amazfit GTR'), isTrue);
      expect(isSmartwatchName('My Wear OS device'), isTrue);
    });

    test('does not match live sensors', () {
      expect(isSmartwatchName('TICKR'), isFalse);
      expect(isSmartwatchName('Polar H10'), isFalse);
      expect(isSmartwatchName('Wahoo CADENCE'), isFalse);
      expect(isSmartwatchName('KICKR BIKE'), isFalse);
    });

    test('is null- and blank-safe', () {
      expect(isSmartwatchName(null), isFalse);
      expect(isSmartwatchName(''), isFalse);
      expect(isSmartwatchName('   '), isFalse);
    });
  });
}
