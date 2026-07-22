import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/source/sensors/garmin/garmin_protobuf.dart';
import 'package:openvitals/data/source/sensors/garmin/garmin_settings_screen.dart';

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

    test('without a state, a switch is not rendered as one', () {
      // Its value lives only in the state; drawing a toggle without knowing
      // which way it points would show every alarm as OFF.
      final screen = parseGarminSettingsScreen(alarmScreen())!;
      expect(screen.entries.first.kind, GarminEntryKind.inert);
      expect(screen.entries.first.switchedOn, isNull);
    });
  });

  group('rows a phone cannot act on', () {
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

  test('a reply that is not a definition yields no screen', () {
    expect(parseGarminSettingsScreen(null), isNull);
    expect(parseGarminSettingsScreen(Uint8List.fromList([0xFF, 0xFF])), isNull);
    // A STATE reply is not a definition.
    expect(parseGarminSettingsScreen(_stateReply(screenId: 64)), isNull);
  });
}
