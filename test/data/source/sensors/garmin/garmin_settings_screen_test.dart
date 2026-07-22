import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/source/sensors/garmin/garmin_protobuf.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_settings_screen.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_settings_service.dart';

/// Rebuilds the reply the watch sends, so the parser is exercised against the
/// real wire shape rather than a convenient one.
Uint8List _definitionReply({
  required int screenId,
  String? title,
  List<Uint8List> entries = const [],
}) {
  final def = ProtobufWriter()..varint(1, screenId);
  if (title != null) def.nested(4, _label(title));
  for (final e in entries) {
    def.nested(5, e);
  }
  final response = (ProtobufWriter()
        ..varint(1, 0)
        ..nested(2, def.toBytes()))
      .toBytes();
  final service = (ProtobufWriter()..nested(2, response)).toBytes();
  return (ProtobufWriter()..nested(42, service)).toBytes();
}

Uint8List _stateReply({
  required int screenId,
  List<Uint8List> states = const [],
}) {
  final state = ProtobufWriter()..varint(1, screenId);
  for (final s in states) {
    state.nested(4, s);
  }
  final response = (ProtobufWriter()
        ..varint(1, 0)
        ..nested(2, state.toBytes()))
      .toBytes();
  final service = (ProtobufWriter()..nested(4, response)).toBytes();
  return (ProtobufWriter()..nested(42, service)).toBytes();
}

Uint8List _label(String text) =>
    (ProtobufWriter()..string(2, text)).toBytes();

Uint8List _entry({
  required int id,
  String? title,
  int? targetType,
  int? subscreen,
  List<String> options = const [],
}) {
  final w = ProtobufWriter()..varint(1, id);
  if (title != null) w.nested(3, _label(title));
  if (targetType != null) {
    final t = ProtobufWriter()..varint(1, targetType);
    if (subscreen != null) t.varint(2, subscreen);
    if (options.isNotEmpty) {
      final list = ProtobufWriter();
      for (final o in options) {
        list.nested(1, (ProtobufWriter()..nested(3, _label(o))).toBytes());
      }
      t.nested(4, list.toBytes());
    }
    w.nested(9, t.toBytes());
  }
  return w.toBytes();
}

Uint8List _switchState({required int id, required bool on}) => (ProtobufWriter()
      ..varint(1, id)
      ..nested(3, (ProtobufWriter()..varint(1, on ? 1 : 0)).toBytes()))
    .toBytes();

/// A row the watch marked as removable: field 9, present and EMPTY. That mark
/// is the only thing separating an alarm's "Delete" from the untargeted rows at
/// the root of the tree.
Uint8List _removableState({required int id}) => (ProtobufWriter()
      ..varint(1, id)
      ..nested(9, Uint8List(0)))
    .toBytes();

/// A row the watch mentioned and said nothing about — how the root's own rows
/// arrive.
Uint8List _bareState({required int id}) =>
    (ProtobufWriter()..varint(1, id)).toBytes();

void main() {
  group("an alarm's own screen, as a vívoactive 5 sends it", () {
    /// Screen 64, titled "Customize", captured from a real watch: a switch with
    /// no target, a time, and two option lists the WATCH supplied.
    Uint8List alarmScreen() => _definitionReply(
          screenId: 64,
          title: 'Customize',
          entries: [
            _entry(id: 0, title: 'Status'),
            _entry(id: 1, title: 'Time', targetType: 3),
            _entry(
              id: 2,
              title: 'Repeat',
              targetType: 1,
              options: ['Once', 'Daily', 'Weekday', 'Weekend'],
            ),
            _entry(
              id: 3,
              title: 'Label',
              targetType: 1,
              options: ['None', 'Wake Up'],
            ),
          ],
        );

    test('reads each row as the control the watch declared', () {
      final screen = parseGarminSettingsScreen(
        alarmScreen(),
        stateReply: _stateReply(
          screenId: 64,
          states: [_switchState(id: 0, on: true)],
        ),
      )!;

      expect(screen.screenId, 64);
      expect(screen.title, 'Customize');
      expect([for (final e in screen.entries) e.kind], [
        GarminEntryKind.toggle, // Status — no target, value lives in the state
        GarminEntryKind.time, // target type 3
        GarminEntryKind.options, // target type 1
        GarminEntryKind.options,
      ]);
    });

    test('the options come from the WATCH, never from this app', () {
      // Repeat is Once/Daily/Weekday/Weekend on this firmware. Hard-coding that
      // list would be wrong on the next model, and wrong in every language.
      final repeat = parseGarminSettingsScreen(alarmScreen())!.entries[2];
      expect([for (final o in repeat.options) o.title],
          ['Once', 'Daily', 'Weekday', 'Weekend']);
      // Index is what a change names, so it must match the order sent.
      expect([for (final o in repeat.options) o.index], [0, 1, 2, 3]);
    });

    test('a switch takes its value from the STATE, not the definition', () {
      final on = parseGarminSettingsScreen(
        alarmScreen(),
        stateReply:
            _stateReply(screenId: 64, states: [_switchState(id: 0, on: true)]),
      )!;
      final off = parseGarminSettingsScreen(
        alarmScreen(),
        stateReply:
            _stateReply(screenId: 64, states: [_switchState(id: 0, on: false)]),
      )!;

      expect(on.entries.first.switchedOn, isTrue);
      expect(off.entries.first.switchedOn, isFalse);
    });

    test('without a state, a switch is neither a toggle NOR a button', () {
      // Its value lives only in the state, so a toggle drawn without one would
      // show every alarm as OFF. And a target-less row cannot be told apart
      // from an action button without the state either — treating it as one
      // would offer "Status" the action reserved for "Delete", which is how a
      // dropped reply becomes a deleted alarm.
      final screen = parseGarminSettingsScreen(alarmScreen())!;
      expect(screen.entries.first.kind, GarminEntryKind.inert);
      expect(screen.entries.first.switchedOn, isNull);
    });

    test('a button is the row the WATCH marked, not one we inferred', () {
      // "Delete" on a real alarm screen carries field 9 in its state. Nothing
      // else on the screen does.
      final screen = parseGarminSettingsScreen(
        _definitionReply(
          screenId: 65600,
          entries: [
            _entry(id: 0, title: 'Status'),
            _entry(id: 4, title: 'Delete'),
          ],
        ),
        stateReply: _stateReply(
          screenId: 65600,
          states: [
            _switchState(id: 0, on: true),
            _removableState(id: 4),
          ],
        ),
      )!;
      expect(screen.entries.first.kind, GarminEntryKind.toggle);
      expect(screen.entries.last.kind, GarminEntryKind.action);
    });

    test('an untargeted row the watch did NOT mark is never a button', () {
      // The root of the tree, as a vívoactive 5 sends it: Finish Setup, Help &
      // Info, Software Update and Find My Device all arrive with no target and
      // a bare state. Read as buttons, they offered a DELETE — and tapping Find
      // My Device sent the watch one. It refused, by its own choice.
      final screen = parseGarminSettingsScreen(
        _definitionReply(
          screenId: 36352,
          entries: [
            _entry(id: 1, title: 'Finish Setup'),
            _entry(id: 24, title: 'Help & Info'),
            _entry(id: 25, title: 'Find My Device'),
          ],
        ),
        stateReply: _stateReply(
          screenId: 36352,
          states: [
            _bareState(id: 1),
            _bareState(id: 24),
            _bareState(id: 25),
          ],
        ),
      )!;
      expect([for (final e in screen.entries) e.kind],
          everyElement(GarminEntryKind.inert));
    });
  });

  group('rows a phone cannot act on', () {
    test('an unused slot is blank, and blank rows are droppable', () {
      // A real alarm list came back as twenty rows with no title at all plus
      // one "Add Alarm" — drawn literally, that is twenty empty cards.
      final screen = parseGarminSettingsScreen(_definitionReply(
        screenId: 68,
        entries: [
          _entry(id: 0),
          _entry(id: 20, title: 'Add Alarm', targetType: 0, subscreen: 999),
        ],
      ))!;
      expect(screen.entries.first.isBlank, isTrue);
      expect(screen.entries.last.isBlank, isFalse);
    });

    test('an unhandled target keeps the type it declared', () {
      // "Delete" on an alarm's screen came out inert, which is
      // indistinguishable from a hidden row without the number that says what
      // control it really is.
      final screen = parseGarminSettingsScreen(_definitionReply(
        screenId: 65600,
        entries: [_entry(id: 4, title: 'Delete', targetType: 11)],
      ))!;
      expect(screen.entries.single.kind, GarminEntryKind.inert);
      expect(screen.entries.single.rawTargetType, 11);
      expect(screen.entries.single.isBlank, isFalse);
    });

    test('an empty alarm slot leads nowhere', () {
      // The Alarms list reserves a row per slot and points unused ones at
      // screen zero.
      final screen = parseGarminSettingsScreen(_definitionReply(
        screenId: 68,
        entries: [_entry(id: 1, targetType: 0, subscreen: 0)],
      ))!;
      expect(screen.entries.single.kind, GarminEntryKind.inert);
      expect(screen.entries.single.isActionable, isFalse);
    });

    test('opens-on-the-watch and hidden are inert, not guessed at', () {
      final screen = parseGarminSettingsScreen(_definitionReply(
        screenId: 36352,
        entries: [
          _entry(id: 2, title: 'Garmin Pay', targetType: 6, subscreen: 2),
          _entry(id: 1, title: 'Stopwatch', targetType: 7),
        ],
      ))!;
      expect([for (final e in screen.entries) e.kind],
          [GarminEntryKind.inert, GarminEntryKind.inert]);
    });

    test('an unknown target type is inert rather than a guessed widget', () {
      // Garmin's schema is older than the firmware. Rendering an unrecognised
      // control as the nearest familiar one would put the wrong widget in front
      // of a real setting.
      final screen = parseGarminSettingsScreen(_definitionReply(
        screenId: 1,
        entries: [_entry(id: 0, title: 'Something new', targetType: 99)],
      ))!;
      expect(screen.entries.single.kind, GarminEntryKind.inert);
      expect(screen.entries.single.title, 'Something new');
    });
  });

  group('the Clocks screen', () {
    test('a populated alarm is a subscreen, whichever target type it uses', () {
      // Type 9 is "subscreen with options" and is what a real alarm uses; type
      // 0 is the plain form. Both walk into another screen.
      final screen = parseGarminSettingsScreen(_definitionReply(
        screenId: 204,
        title: 'Clocks',
        entries: [
          _entry(id: 0, title: '7:00 am', targetType: 9, subscreen: 64),
          _entry(id: 3, title: 'Time', targetType: 0, subscreen: 738),
        ],
      ))!;

      expect([for (final e in screen.entries) e.kind],
          [GarminEntryKind.subscreen, GarminEntryKind.subscreen]);
      expect([for (final e in screen.entries) e.subscreenId], [64, 738]);
    });
  });

  group('telling one screen\'s reply from another', () {
    test('a definition names the screen it describes', () {
      expect(
        GarminSettingsService.screenIdOf(
          _definitionReply(screenId: 65600),
          GarminSettingsService.definitionResponseField,
        ),
        65600,
      );
    });

    test('a state names it too', () {
      expect(
        GarminSettingsService.screenIdOf(
          _stateReply(screenId: 68),
          GarminSettingsService.stateResponseField,
        ),
        68,
      );
    });

    test('a change response names it from a field of its own', () {
      // Captured from a vívoactive 5 answering a delete: the change response
      // nests its screen at field 3, not field 2 like the other two.
      final reply = Uint8List.fromList([
        0xd2, 0x02, 0x19, 0x32, 0x17, 0x08, 0x00, 0x1a, 0x11, //
        0x08, 0xc0, 0x80, 0x8c, 0x08, 0x10, 0x00, 0x18, 0xa8, 0x88, 0x68,
        0x22, 0x04, 0x08, 0x04, 0x4a, 0x00, 0x28, 0x01,
      ]);
      expect(
        GarminSettingsService.screenIdOf(
          reply,
          GarminSettingsService.changeResponseField,
        ),
        16973888,
      );
    });

    test('a reply about another screen is not this screen\'s answer', () {
      // The watch retransmits anything it thinks went unacknowledged, so the
      // alarm LIST's definition arrived while one alarm's screen was pending
      // and was taken as the answer — pairing one screen's rows with another's
      // values.
      final list = GarminSettingsService.screenIdOf(
        _definitionReply(screenId: 68),
        GarminSettingsService.definitionResponseField,
      );
      expect(list, isNot(65600));
    });
  });

  group('the value behind a row, as the watch reports it', () {
    /// The state a vívoactive 5 sent for an alarm's Repeat and Time rows,
    /// rebuilt field for field: a chosen option is a POSITION, and a time is
    /// seconds since midnight.
    Uint8List valueState() => _stateReply(screenId: 65600, states: [
          (ProtobufWriter()
                ..varint(1, 2)
                ..nested(
                    4,
                    (ProtobufWriter()
                          ..nested(1, _label('Once'))
                          ..nested(2, (ProtobufWriter()..varint(1, 0)).toBytes()))
                        .toBytes()))
              .toBytes(),
          (ProtobufWriter()
                ..varint(1, 1)
                ..nested(
                    4,
                    (ProtobufWriter()
                          ..nested(1, _label('11:10 am'))
                          ..nested(
                              4,
                              (ProtobufWriter()..varint(1, 40200)).toBytes()))
                        .toBytes()))
              .toBytes(),
        ]);

    Uint8List definition() => _definitionReply(screenId: 65600, entries: [
          _entry(
            id: 1,
            title: 'Time',
            targetType: 3,
          ),
          _entry(
            id: 2,
            title: 'Repeat',
            targetType: 1,
            options: ['Once', 'Daily', 'Weekday', 'Weekend'],
          ),
        ]);

    test('a chosen option is a position, not the summary text', () {
      // Matching the summary against the option titles held up until a screen
      // arrived whose summary was EMPTY — and then nothing looked selected.
      final repeat = parseGarminSettingsScreen(definition(),
              stateReply: valueState())!
          .entries
          .firstWhere((e) => e.id == 2);
      expect(repeat.selectedIndex, 0);
      expect(repeat.options[repeat.selectedIndex!].title, 'Once');
    });

    test('a time comes back as the time, not just its rendering', () {
      // 40200 seconds is 11:10 — the same instant the watch spelled out beside
      // it. The picker opens here; starting from "now" reset any alarm it was
      // used on.
      final time = parseGarminSettingsScreen(definition(),
              stateReply: valueState())!
          .entries
          .firstWhere((e) => e.id == 1);
      expect(time.time, const Duration(hours: 11, minutes: 10));
    });
  });

  test('a nameless row is hidden even when it carries a value', () {
    // After a delete the freed slots came back with a leftover summary and no
    // title at all, which drew two empty grey cards under the real alarms.
    final screen = parseGarminSettingsScreen(_definitionReply(
      screenId: 68,
      entries: [_entry(id: 2)],
    ))!;
    expect(screen.entries.single.isBlank, isTrue);
  });

  test('a reply that is not a definition yields no screen', () {
    expect(parseGarminSettingsScreen(null), isNull);
    expect(parseGarminSettingsScreen(Uint8List.fromList([0xFF, 0xFF])), isNull);
    // A STATE reply is not a definition.
    expect(parseGarminSettingsScreen(_stateReply(screenId: 64)), isNull);
  });
}
