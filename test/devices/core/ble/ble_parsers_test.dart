import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/devices/core/ble/parsers/ble_parsers.dart';

/// Byte-exact port of the Kotlin `BleParsersTest`.
void main() {
  test('parseHeartRate uint8', () {
    expect(BleHeartRateParser.parseBytes([0x00, 0x4A]), 74);
  });

  test('parseHeartRate uint16', () {
    expect(BleHeartRateParser.parseBytes([0x01, 0x2C, 0x01]), 300);
  });

  test('parseCyclingPower basic', () {
    final payload = [
      0x20,
      0x00,
      0x64, 0x00,
      0x05, 0x00,
      0x10, 0x00,
    ];
    final parsed = BleCyclingPowerParser.parsePayload(payload);
    expect(parsed, isNotNull);
    expect(parsed?.powerWatts, 100);
    expect(parsed?.crank?.crankRevolutionsCount, 5);
  });

  test('parseCyclingSpeedCadence wheel and crank', () {
    final payload = [
      0x03,
      0x10, 0x00, 0x00, 0x00,
      0x20, 0x00,
      0x05, 0x00,
      0x30, 0x00,
    ];
    final parsed = BleCyclingSpeedCadenceParser.parsePayload(payload);
    expect(parsed, isNotNull);
    expect(parsed?.$1?.wheelRevolutionsCount, 16);
    expect(parsed?.$2?.crankRevolutionsCount, 5);
  });

  test('parseRunningSpeedCadence', () {
    final payload = [
      0x00,
      0x00, 0x02,
      0x50,
    ];
    final parsed =
        BleRunningSpeedCadenceParser.parsePayload(payload, 'Stryd');
    expect(parsed, isNotNull);
    expect(parsed?.speedMetersPerSecond, 2.0);
    expect(parsed?.cadenceRpm, 80);
  });

  test('parseRunningSpeedCadence tickrX adjusts cadence', () {
    final payload = [
      0x00,
      0x00, 0x02,
      0x64,
    ];
    final parsed =
        BleRunningSpeedCadenceParser.parsePayload(payload, 'TICKR X 1234');
    expect(parsed?.cadenceRpm, 50);
  });

  test('parseHeartRate zero-signal payload', () {
    expect(BleHeartRateParser.isZeroSignal([0x00, 0x00]), isTrue);
    expect(BleHeartRateParser.parseBytes([0x00, 0x00]), isNull);
  });

  test('parseHeartRate single byte', () {
    expect(BleHeartRateParser.parseBytes([0x4A]), 74);
  });

  test('parseHeartRate empty payload returns null', () {
    expect(BleHeartRateParser.parseBytes([]), isNull);
  });
}
