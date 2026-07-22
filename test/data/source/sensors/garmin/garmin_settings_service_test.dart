import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/source/sensors/garmin/garmin_protobuf.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_settings_service.dart';

Uint8List _b(List<int> xs) => Uint8List.fromList(xs);

void main() {
  _changeTests();

  _treeTests();

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

void _treeTests() {
  /// Builds the reply shape the watch actually sends, from the captured bytes:
  /// Smart{42: SettingsService{2: definitionResponse{2: ScreenDefinition}}}.
  Uint8List definitionReply(List<Uint8List> entries, {int screenId = 36352}) {
    final def = ProtobufWriter()..varint(1, screenId);
    for (final e in entries) {
      def.nested(5, e);
    }
    final response = (ProtobufWriter()..nested(2, def.toBytes())).toBytes();
    final service = (ProtobufWriter()..nested(2, response)).toBytes();
    return (ProtobufWriter()..nested(42, service)).toBytes();
  }

  Uint8List entry({
    required int id,
    String? title,
    int? targetType,
    int? subscreen,
  }) {
    final w = ProtobufWriter()..varint(1, id);
    if (title != null) {
      w.nested(3, (ProtobufWriter()..string(2, title)).toBytes());
    }
    if (targetType != null) {
      final t = ProtobufWriter()..varint(1, targetType);
      if (subscreen != null) t.varint(2, subscreen);
      w.nested(9, t.toBytes());
    }
    return w.toBytes();
  }

  group('walking the tree', () {
    test('finds the screens an entry leads to', () {
      // Shapes taken from a real vívoactive 5 root: Clocks → 204, and Garmin
      // Pay whose target type 6 opens something ON the watch.
      final reply = definitionReply([
        entry(id: 4, title: 'Clocks', targetType: 0, subscreen: 204),
        entry(id: 6, title: 'Glances', targetType: 0, subscreen: 920),
      ]);
      final found = GarminSettingsService.subscreens(reply);
      expect([for (final s in found) s.screenId], [204, 920]);
      expect(found.first.title, 'Clocks');
    });

    test('follows a subscreen-WITH-OPTIONS, which is what an alarm is', () {
      // A populated alarm slot is target type 9, not 0. Following only 0 meant
      // the walk reached the Alarms list and stopped at it.
      final reply = definitionReply([
        entry(id: 0, title: '7:00 am', targetType: 9, subscreen: 64),
      ]);
      expect(
        [for (final s in GarminSettingsService.subscreens(reply)) s.screenId],
        [64],
      );
    });

    test('an empty alarm slot points at screen zero and is skipped', () {
      // The list reserves a row per slot; unused ones target nothing. Asking
      // the watch for screen zero requests something that does not exist.
      final reply = definitionReply([
        entry(id: 1, targetType: 0, subscreen: 0),
        entry(id: 2, targetType: 0, subscreen: 0),
      ]);
      expect(GarminSettingsService.subscreens(reply), isEmpty);
    });

    test('does not try to walk into what it cannot open', () {
      // Type 6 opens an activity on the watch and 7 is hidden; treating either
      // as a screen would request an id that means something else entirely.
      final reply = definitionReply([
        entry(id: 2, title: 'Garmin Pay', targetType: 6, subscreen: 2),
        entry(id: 3, title: 'Hidden', targetType: 7, subscreen: 9),
        entry(id: 1, title: 'Finish Setup'), // no target at all
      ]);
      expect(GarminSettingsService.subscreens(reply), isEmpty);
    });

    test('a reply carrying a STATE is not a definition', () {
      final state = (ProtobufWriter()..emptyMessage(4)).toBytes();
      final reply = (ProtobufWriter()
            ..nested(42, state))
          .toBytes();
      expect(
        GarminSettingsService.carries(
            reply, GarminSettingsService.definitionResponseField),
        isFalse,
      );
      expect(
        GarminSettingsService.carries(
            reply, GarminSettingsService.stateResponseField),
        isTrue,
      );
      expect(GarminSettingsService.subscreens(reply), isEmpty);
    });
  });
}

void _changeTests() {
  int? valueOf(Uint8List req, int valueField, int inner) {
    final settings = protobufField(readProtobuf(req), 42)!.bytes!;
    final change = protobufField(readProtobuf(settings), 5)!.bytes!;
    final value = protobufField(readProtobuf(change), valueField)?.bytes;
    if (value == null) return null;
    return protobufField(readProtobuf(value), inner)?.varint;
  }

  List<int?> targetOf(Uint8List req) {
    final settings = protobufField(readProtobuf(req), 42)!.bytes!;
    final change = protobufField(readProtobuf(settings), 5)!.bytes!;
    final f = readProtobuf(change);
    return [
      protobufField(f, 1)?.varint,
      protobufField(f, 2)?.varint,
    ];
  }

  group('ChangeRequest — the only write in the stack', () {
    test('names the screen and entry it changes', () {
      final req = GarminSettingsService.changeSwitch(
          screenId: 64, entryId: 3, value: true);
      expect(targetOf(req), [64, 3]);
    });

    test('each value kind lands in its OWN field', () {
      // One generic setter could put a time in a switch's field, and the watch
      // would apply whatever it read. Separate fields make that impossible.
      expect(
        valueOf(GarminSettingsService.changeSwitch(
                screenId: 1, entryId: 1, value: true),
            3, 1),
        1,
      );
      expect(
        valueOf(GarminSettingsService.changeOption(
                screenId: 1, entryId: 1, index: 2),
            4, 1),
        2,
      );
      expect(
        valueOf(
            GarminSettingsService.changeTime(
                screenId: 1,
                entryId: 1,
                sinceMidnight: const Duration(hours: 7)),
            6,
            1),
        25200, // 07:00 as seconds since midnight
      );
      expect(
        valueOf(GarminSettingsService.changeNumber(
                screenId: 1, entryId: 1, value: 42),
            8, 1),
        42,
      );
    });

    test('a switch off is a present false, not an absent field', () {
      // Omitting it would read as "no change", and the switch would stay on.
      expect(
        valueOf(GarminSettingsService.changeSwitch(
                screenId: 1, entryId: 1, value: false),
            3, 1),
        0,
      );
    });

    test('a time outside one day is refused, not wrapped', () {
      expect(
        () => GarminSettingsService.changeTime(
            screenId: 1, entryId: 1, sinceMidnight: const Duration(hours: 25)),
        throwsArgumentError,
      );
      expect(
        () => GarminSettingsService.changeTime(
            screenId: 1,
            entryId: 1,
            sinceMidnight: const Duration(seconds: -1)),
        throwsArgumentError,
      );
    });

    test('SUCCESS is ZERO here — the opposite of the find service', () {
      Uint8List reply(int? status) {
        final w = ProtobufWriter();
        if (status != null) w.varint(1, status);
        final service = (ProtobufWriter()..nested(6, w.toBytes())).toBytes();
        return (ProtobufWriter()..nested(42, service)).toBytes();
      }

      expect(GarminSettingsService.changeSucceeded(reply(0)), isTrue);
      expect(GarminSettingsService.changeSucceeded(reply(1)), isFalse);
      // A response with no status at all is acceptance, as the find service
      // taught us — but here zero means success, not "unset".
      expect(GarminSettingsService.changeSucceeded(reply(null)), isTrue);
      // No change response at all is not an answer.
      expect(GarminSettingsService.changeSucceeded(null), isNull);
    });
  });
}
