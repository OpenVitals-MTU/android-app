import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/devices/garmin/garmin_ble_transport.dart';
import 'package:openvitals/devices/garmin/garmin_session.dart';
import 'package:openvitals/devices/garmin/garmin_settings_link.dart';

/// Regression for "stop waiting on a link that has gone" (7b7858978).
///
/// Tapping an alarm hung on "Reading from the watch": the STATE request got no
/// reply, the link dropped ten seconds later, and the pending request sat out
/// its full thirty-second timeout waiting for an answer that could no longer
/// come. A dropped or closed link must resolve everything in flight at once.
void main() {
  GarminSettingsLink link() => GarminSettingsLink.forTest(
        GarminBleTransport(address: 'AA:BB:CC:DD:EE:FF'),
        GarminSession(
          // The watch never answers — every request would wait out its timeout.
          send: (_) async {},
          bluetoothName: 'Pixel 6 Pro',
          manufacturer: 'Google',
          model: 'raven',
          syncFiles: false,
        ),
      );

  test('closing the link resolves an in-flight screen read at once', () async {
    final l = link();
    // 65600 is the alarm screen id from the original hang.
    final pending = l.screen(65600);
    await pumpEventQueue();

    await l.close();

    // Well under GarminSettingsService.replyTimeout (30s): on regression this
    // throws TimeoutException instead of letting the test wait the hang out.
    final screen = await pending.timeout(const Duration(seconds: 2));
    expect(screen, isNull);
  });

  test('a request on an already-closed link answers null immediately', () async {
    final l = link();
    await l.close();

    expect(l.isOpen, isFalse);
    final screen = await l.screen(65600).timeout(const Duration(seconds: 2));
    expect(screen, isNull);
  });
}
