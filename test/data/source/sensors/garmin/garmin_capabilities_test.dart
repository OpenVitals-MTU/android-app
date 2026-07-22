import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/source/sensors/garmin/garmin_capabilities.dart';

void main() {
  test('the enum order IS the wire order', () {
    // Bit positions are declaration order, so a reorder silently remaps every
    // watch's capabilities. These three are pinned against Gadgetbridge's enum.
    expect(GarminCapability.sync.bit, 3);
    expect(GarminCapability.findMyWatch.bit, 9); // byte 1, bit 1
    expect(GarminCapability.realtimeSettings.bit, 92); // byte 11, bit 4
    expect(GarminCapability.values, hasLength(120)); // 15 bytes exactly
  });

  test('decodes a flag from its byte and bit', () {
    final bits = Uint8List(15);
    bits[11] = 1 << 4; // REALTIME_SETTINGS
    expect(decodeGarminCapabilities(bits), {GarminCapability.realtimeSettings});
  });

  test('an all-ones bitmap sets everything', () {
    final bits = Uint8List.fromList(List.filled(15, 0xFF));
    expect(decodeGarminCapabilities(bits), hasLength(120));
  });

  test('an empty bitmap sets nothing', () {
    expect(decodeGarminCapabilities(Uint8List(15)), isEmpty);
  });

  test('a short buffer is not an error', () {
    // A future watch may send fewer bytes than we know flags; everything past
    // the end is absent rather than a crash mid-handshake.
    final bits = Uint8List.fromList([0xFF, 0xFF]);
    final decoded = decodeGarminCapabilities(bits);
    expect(decoded, contains(GarminCapability.sync));
    expect(decoded, isNot(contains(GarminCapability.realtimeSettings)));
    expect(decoded, hasLength(16));
  });
}
