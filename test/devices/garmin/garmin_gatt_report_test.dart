import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/devices/garmin/garmin_transport.dart';

GarminGattReport _report(
  GarminTransportVariant variant, {
  List<GarminGattService> services = const [],
}) =>
    GarminGattReport(
      address: 'E0:48:24:D5:F7:10',
      variant: variant,
      services: services,
    );

void main() {
  group('isSupported', () {
    test('true for the transports this app can drive', () {
      expect(_report(GarminTransportVariant.v1).isSupported, isTrue);
      expect(_report(GarminTransportVariant.v2).isSupported, isTrue);
    });

    test('false when the verdict says nothing usable', () {
      // These two must stay distinct in the model even though both are "no":
      // unknown means "we enumerated and did not recognise it" (report it),
      // unreachable means "we learnt nothing" (retry is worthwhile).
      expect(_report(GarminTransportVariant.unknown).isSupported, isFalse);
      expect(_report(GarminTransportVariant.unreachable).isSupported, isFalse);
    });
  });

  group('describe', () {
    test('renders every service and characteristic under one grep-able tag', () {
      final report = _report(
        GarminTransportVariant.v1,
        services: const [
          GarminGattService(
            uuid: '6a4e2401-667b-11e3-949a-0800200c9a66',
            characteristics: {
              '6a4e4c80-667b-11e3-949a-0800200c9a66': ['writeNoRsp'],
              '6a4ecd28-667b-11e3-949a-0800200c9a66': ['notify'],
            },
          ),
        ],
      );

      final lines = report.describe().split('\n');

      // Every line carries the tag: the log is read with grep, and an untagged
      // continuation line would be invisible in the results.
      expect(lines.every((l) => l.startsWith('[GARMIN-GATT]')), isTrue);
      expect(lines.first, contains('variant=v1'));
      expect(lines.first, contains('services=1'));
      expect(
        report.describe(),
        contains('6a4e4c80-667b-11e3-949a-0800200c9a66 [writeNoRsp]'),
      );
      expect(
        report.describe(),
        contains('6a4ecd28-667b-11e3-949a-0800200c9a66 [notify]'),
      );
    });

    test('an unknown device still dumps what it found', () {
      // The case the dump exists for: the verdict alone cannot be diagnosed.
      final report = _report(
        GarminTransportVariant.unknown,
        services: const [
          GarminGattService(
            uuid: '0000180f-0000-1000-8000-00805f9b34fb',
            characteristics: {
              '00002a19-0000-1000-8000-00805f9b34fb': ['read', 'notify'],
            },
          ),
        ],
      );

      expect(report.describe(), contains('variant=unknown'));
      expect(report.describe(), contains('0000180f-0000-1000-8000-00805f9b34fb'));
      expect(report.describe(), contains('[read,notify]'));
    });
  });
}
