import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';
import 'package:openvitals/features/imports/fit/fit_wellness_import.dart';
import 'package:openvitals/features/manualentry/activity/routeimport/fit_route_parser.dart';

/// Sleep import from Garmin FIT (file type 49). The bytes are hand-built so no
/// real health data is committed; the layout mirrors what a vívoactive writes —
/// an `event`/74 start/stop pair for the bounds and `sleep_level` transitions
/// for the stages. See docs/reference/garmin-fit-files.md.
void main() {
  final start = DateTime.utc(2024, 1, 1, 23, 0, 0);
  final stop = DateTime.utc(2024, 1, 2, 6, 0, 0);
  // (transition, sleep_level enum: 0 unmeasurable,1 awake,2 light,3 deep,4 rem)
  final levels = <(DateTime, int)>[
    (DateTime.utc(2024, 1, 1, 23, 10), 2), // light
    (DateTime.utc(2024, 1, 1, 23, 40), 3), // deep
    (DateTime.utc(2024, 1, 2, 0, 30), 4), // rem
    (DateTime.utc(2024, 1, 2, 0, 45), 1), // awake
    (DateTime.utc(2024, 1, 2, 1, 0), 2), // light (runs to session end)
  ];

  group('parseSleepSession', () {
    test('reads the session bounds and a contiguous stage timeline', () {
      final session = FitRouteParser.parseSleepSession(
        _fitSleepBytes(start: start, stop: stop, levels: levels),
      )!;

      expect(session.start, start);
      expect(session.end, stop);
      expect(session.stages.length, 5);

      // Each stage runs from its transition to the next; the last to session end.
      expect(session.stages.first.level, FitSleepLevel.light);
      expect(session.stages.first.start, DateTime.utc(2024, 1, 1, 23, 10));
      expect(session.stages[1].level, FitSleepLevel.deep);
      expect(session.stages[1].start, DateTime.utc(2024, 1, 1, 23, 40));
      expect(session.stages[2].level, FitSleepLevel.rem);
      expect(session.stages.last.level, FitSleepLevel.light);
      expect(session.stages.last.end, stop);

      // Contiguous: every stage ends where the next begins.
      for (var i = 0; i + 1 < session.stages.length; i++) {
        expect(session.stages[i].end, session.stages[i + 1].start);
      }
    });

    test('returns null when the file carries no sleep timeline', () {
      final session = FitRouteParser.parseSleepSession(
        _fitSleepBytes(start: start, stop: stop, levels: const []),
      );
      expect(session, isNull);
    });
  });

  group('fitSleepImportRecords', () {
    test('maps to one SleepSessionRecord with a deterministic id', () {
      final session = FitRouteParser.parseSleepSession(
        _fitSleepBytes(start: start, stop: stop, levels: levels),
      )!;

      final records = fitSleepImportRecords(session);
      expect(records, hasLength(1));
      final record = records.single as SleepSessionImportRecord;

      expect(record.targetType, 'SleepSessionRecord');
      expect(
        record.clientRecordId,
        'garmin_fit_sleep_${start.millisecondsSinceEpoch}',
      );
      expect(record.startTime, start);
      expect(record.endTime, stop);
      expect(record.stages.map((s) => s.stage), [
        SleepStageType.light,
        SleepStageType.deep,
        SleepStageType.rem,
        SleepStageType.awake,
        SleepStageType.light,
      ]);
    });

    test('drops unmeasurable spans, which have no Health Connect stage', () {
      final withUnmeasurable = <(DateTime, int)>[
        (DateTime.utc(2024, 1, 1, 23, 10), 0), // unmeasurable
        (DateTime.utc(2024, 1, 1, 23, 30), 2), // light
      ];
      final session = FitRouteParser.parseSleepSession(
        _fitSleepBytes(start: start, stop: stop, levels: withUnmeasurable),
      )!;

      final record = fitSleepImportRecords(session).single
          as SleepSessionImportRecord;
      expect(record.stages.map((s) => s.stage), [SleepStageType.light]);
    });
  });
}

// ── Minimal FIT writer (little-endian), enough for a sleep file ──────────────

int _fitTimestamp(DateTime time) =>
    time.millisecondsSinceEpoch ~/ 1000 - 631065600;

class _W {
  final BytesBuilder _b = BytesBuilder();
  void u8(int v) => _b.addByte(v & 0xFF);
  void bytes(List<int> v) => _b.add(v);
  void u16(int v) {
    u8(v);
    u8(v >> 8);
  }

  void u32(int v) {
    u8(v);
    u8(v >> 8);
    u8(v >> 16);
    u8(v >> 24);
  }

  /// A definition record: local type, global message number, (num,size,base)×.
  void def(int local, int global, List<List<int>> fields) {
    u8(0x40 | local);
    u8(0);
    u8(0); // little-endian
    u16(global);
    u8(fields.length);
    for (final f in fields) {
      u8(f[0]);
      u8(f[1]);
      u8(f[2]);
    }
  }

  Uint8List toBytes() => _b.toBytes();
}

/// Wraps a data section in the 14-byte FIT header + trailing CRC (unchecked by
/// the decoder, which reads the declared data size).
Uint8List _wrap(Uint8List data) {
  final w = _W()
    ..u8(14)
    ..u8(16)
    ..u16(0)
    ..u32(data.length)
    ..bytes(utf8.encode('.FIT'))
    ..u16(0)
    ..bytes(data)
    ..u16(0);
  return w.toBytes();
}

Uint8List _fitSleepBytes({
  required DateTime start,
  required DateTime stop,
  required List<(DateTime, int)> levels,
}) {
  const tsField = [253, 4, 0x86]; // timestamp, uint32
  const enumField1 = [0, 1, 0x00]; // field 0, enum/uint8
  final data = _W();

  // file_id (type = 49, sleep)
  data.def(3, 0, [
    [0, 1, 0x00]
  ]);
  data
    ..u8(3)
    ..u8(fitFileTypeSleep);

  // event (21): timestamp, event, event_type — the sleep start/stop pair.
  data.def(1, 21, [
    tsField,
    [0, 1, 0x00],
    [1, 1, 0x00],
  ]);
  data
    ..u8(1)
    ..u32(_fitTimestamp(start))
    ..u8(74) // event = sleep
    ..u8(0); // event_type = start
  data
    ..u8(1)
    ..u32(_fitTimestamp(stop))
    ..u8(74)
    ..u8(1); // event_type = stop

  // sleep_level (275): timestamp, sleep_level.
  data.def(2, 275, [tsField, enumField1]);
  for (final (at, level) in levels) {
    data
      ..u8(2)
      ..u32(_fitTimestamp(at))
      ..u8(level);
  }

  return _wrap(data.toBytes());
}
