import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/source/sensors/garmin/garmin_protobuf.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_settings_service.dart';

Uint8List _b(List<int> xs) => Uint8List.fromList(xs);

void main() {
  test('init carries the locale that translates the whole tree', () {
    final bytes = GarminSettingsService.init(language: 'en_US', region: 'us');
    // Smart.settings_service = 42 → key (42<<3)|2 = 0x02 0x02... varint-encoded.
    final settings = protobufField(readProtobuf(bytes), 42)!.bytes!;
    final init = protobufField(readProtobuf(settings), 8)!.bytes!;
    final fields = readProtobuf(init);
    expect(String.fromCharCodes(protobufField(fields, 1)!.bytes!), 'en_US');
    expect(String.fromCharCodes(protobufField(fields, 2)!.bytes!), 'us');
  });

  test('a definition request names the screen and the language', () {
    final bytes = GarminSettingsService.screenDefinition(36352);
    final settings = protobufField(readProtobuf(bytes), 42)!.bytes!;
    final request = protobufField(readProtobuf(settings), 1)!.bytes!;
    final fields = readProtobuf(request);
    expect(protobufField(fields, 1)!.varint, 36352);
    expect(String.fromCharCodes(protobufField(fields, 3)!.bytes!), 'en_US');
  });

  test('a state request carries only the screen id', () {
    final settings = protobufField(
      readProtobuf(GarminSettingsService.screenState(36352)),
      42,
    )!.bytes!;
    final request = protobufField(readProtobuf(settings), 3)!.bytes!;
    expect(readProtobuf(request).single.varint, 36352);
  });

  test('recognises a definition reply, and does not mistake a state for one', () {
    Uint8List reply(int field) {
      final service = (ProtobufWriter()..emptyMessage(field)).toBytes();
      return (ProtobufWriter()..nested(42, service)).toBytes();
    }

    expect(GarminSettingsService.hasDefinition(reply(2)), isTrue);
    expect(GarminSettingsService.hasState(reply(2)), isFalse);
    expect(GarminSettingsService.hasState(reply(4)), isTrue);
    expect(GarminSettingsService.hasDefinition(reply(4)), isFalse);
  });

  test('a reply for another service is not a settings reply', () {
    // The watch narrates on services this app does not speak; mistaking one for
    // a settings screen would render a menu out of unrelated bytes.
    final other = (ProtobufWriter()..emptyMessage(12)).toBytes();
    expect(GarminSettingsService.unwrap(other), isNull);
    expect(GarminSettingsService.hasDefinition(other), isFalse);
    expect(GarminSettingsService.hasDefinition(null), isFalse);
    expect(GarminSettingsService.hasDefinition(_b([0xFF, 0xFF])), isFalse);
  });
}
