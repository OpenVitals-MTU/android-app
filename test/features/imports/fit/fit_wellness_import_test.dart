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

  group('HRV (type 68)', () {
    final hrvTime = DateTime.utc(2024, 1, 2, 6, 0, 0);

    test('reads last_night_average as an RMSSD in ms', () {
      final wellness = FitRouteParser.parseWellness(
        _fitHrvBytes(time: hrvTime, rmssdMillis: 42.5),
      );
      expect(wellness.sleep, isNull);
      expect(wellness.hrv, isNotNull);
      expect(wellness.hrv!.time, hrvTime);
      // 42.5 ms -> raw round(42.5*128)=5440 -> 5440/128 = 42.5.
      expect(wellness.hrv!.rmssdMillis, closeTo(42.5, 0.01));
    });

    test('maps to one HeartRateVariabilityRmssd record', () {
      final reading = FitRouteParser.parseWellness(
        _fitHrvBytes(time: hrvTime, rmssdMillis: 42.5),
      ).hrv!;
      final record = fitHrvImportRecords(reading).single
          as HeartRateVariabilityRmssdImportRecord;
      expect(record.targetType, 'HeartRateVariabilityRmssdRecord');
      expect(
        record.clientRecordId,
        'garmin_fit_hrv_${hrvTime.millisecondsSinceEpoch}',
      );
      expect(record.rmssdMillis, closeTo(42.5, 0.01));
    });

    test('the invalid uint16 sentinel is not read as a reading', () {
      final wellness = FitRouteParser.parseWellness(
        _fitHrvBytes(time: hrvTime, rawOverride: 0xFFFF),
      );
      expect(wellness.hrv, isNull);
    });
  });

  group('monitoring (type 32) summary', () {
    final t = DateTime.utc(2024, 1, 18, 13, 42, 0);

    test('reads resting HR and BMR, maps to two records', () {
      final wellness = FitRouteParser.parseWellness(
        _fitMonitoringBytes(time: t, restingHrBpm: 65, bmrKcalPerDay: 2265),
      );
      final m = wellness.monitoring!;
      expect(m.restingHeartRateBpm, 65);
      expect(m.bmrKcalPerDay, 2265);

      final records = fitMonitoringImportRecords(m);
      expect(records, hasLength(2));
      final rhr = records[0] as RestingHeartRateImportRecord;
      final bmr = records[1] as BasalMetabolicRateImportRecord;
      expect(rhr.beatsPerMinute, 65);
      expect(rhr.clientRecordId,
          'garmin_fit_resting_hr_${t.millisecondsSinceEpoch}');
      expect(bmr.kilocaloriesPerDay, 2265);
    });

    test('a file with only resting HR maps to one record', () {
      final wellness = FitRouteParser.parseWellness(
        _fitMonitoringBytes(time: t, restingHrBpm: 58),
      );
      final records = fitMonitoringImportRecords(wellness.monitoring!);
      expect(records, hasLength(1));
      expect((records.single as RestingHeartRateImportRecord).beatsPerMinute,
          58);
    });
  });

  group('monitoring (type 32) high-frequency series', () {
    test('HR packs hourly, respiration averages hourly, steps span the file',
        () {
      final bytes = _fitMonitoringSeriesBytes(
        hr: [
          (DateTime.utc(2024, 1, 18, 9, 10), 70),
          (DateTime.utc(2024, 1, 18, 9, 40), 72),
          (DateTime.utc(2024, 1, 18, 10, 10), 68),
          (DateTime.utc(2024, 1, 18, 10, 40), 74),
        ],
        respiration: [
          (DateTime.utc(2024, 1, 18, 9, 15), 13.0),
          (DateTime.utc(2024, 1, 18, 9, 45), 15.0),
          (DateTime.utc(2024, 1, 18, 10, 15), 14.0),
        ],
        stepsCumulative: [
          (DateTime.utc(2024, 1, 18, 9, 0), 0),
          (DateTime.utc(2024, 1, 18, 10, 0), 500),
          (DateTime.utc(2024, 1, 18, 11, 0), 1200),
        ],
      );
      final m = FitRouteParser.parseWellness(bytes).monitoring!;
      final records = fitMonitoringImportRecords(m);

      final hr = records.whereType<HeartRateImportRecord>().toList()
        ..sort((a, b) => a.startTime.compareTo(b.startTime));
      expect(hr, hasLength(2)); // one per hour (09:xx, 10:xx)
      // One hourly-average sample each, not the raw per-minute samples.
      expect(hr.expand((r) => r.samples).length, 2);
      expect(hr[0].samples.single.beatsPerMinute, 71); // avg(70,72)
      expect(hr[1].samples.single.beatsPerMinute, 71); // avg(68,74)

      final resp = records.whereType<RespiratoryRateImportRecord>().toList()
        ..sort((a, b) => a.time.compareTo(b.time));
      expect(resp, hasLength(2));
      expect(resp.first.rate, closeTo(14.0, 0.001)); // avg(13,15)

      final steps = records.whereType<StepsImportRecord>().single;
      expect(steps.count, 1200); // max - min
      expect(steps.startTime, DateTime.utc(2024, 1, 18, 9, 0));
      expect(steps.endTime, DateTime.utc(2024, 1, 18, 11, 0));
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

Uint8List _fitHrvBytes({
  required DateTime time,
  double? rmssdMillis,
  int? rawOverride,
}) {
  final raw = rawOverride ?? (rmssdMillis! * 128).round();
  final data = _W()..def(3, 0, [
    [0, 1, 0x00]
  ]);
  data
    ..u8(3)
    ..u8(68); // file_id type 68 (HRV)

  // hrv_status_summary (370): timestamp, last_night_average (field 1, uint16).
  data.def(1, 370, [
    [253, 4, 0x86],
    [1, 2, 0x84], // uint16
  ]);
  data
    ..u8(1)
    ..u32(_fitTimestamp(time))
    ..u16(raw);

  return _wrap(data.toBytes());
}

Uint8List _fitMonitoringBytes({
  required DateTime time,
  int? restingHrBpm,
  int? bmrKcalPerDay,
}) {
  final data = _W()..def(3, 0, [
    [0, 1, 0x00]
  ]);
  data
    ..u8(3)
    ..u8(32); // file_id type 32 (monitoring_b)

  if (restingHrBpm != null) {
    // monitoring_hr_data (211): timestamp, resting_heart_rate (field 0, uint8).
    data.def(1, 211, [
      [253, 4, 0x86],
      [0, 1, 0x02], // uint8
    ]);
    data
      ..u8(1)
      ..u32(_fitTimestamp(time))
      ..u8(restingHrBpm);
  }
  if (bmrKcalPerDay != null) {
    // monitoring_info (103): timestamp, resting_metabolic_rate (field 5, uint16).
    data.def(2, 103, [
      [253, 4, 0x86],
      [5, 2, 0x84], // uint16
    ]);
    data
      ..u8(2)
      ..u32(_fitTimestamp(time))
      ..u16(bmrKcalPerDay);
  }

  return _wrap(data.toBytes());
}

Uint8List _fitMonitoringSeriesBytes({
  List<(DateTime, int)> hr = const [],
  List<(DateTime, double)> respiration = const [],
  List<(DateTime, int)> stepsCumulative = const [],
}) {
  final data = _W()..def(3, 0, [
    [0, 1, 0x00]
  ]);
  data
    ..u8(3)
    ..u8(32); // file_id type 32

  // monitoring HR (local 1, global 55): timestamp + heart_rate (uint8).
  data.def(1, 55, [
    [253, 4, 0x86],
    [27, 1, 0x02],
  ]);
  for (final (t, bpm) in hr) {
    data
      ..u8(1)
      ..u32(_fitTimestamp(t))
      ..u8(bpm);
  }
  // monitoring steps (local 2, global 55): timestamp + cumulative steps (uint32).
  data.def(2, 55, [
    [253, 4, 0x86],
    [3, 4, 0x86],
  ]);
  for (final (t, s) in stepsCumulative) {
    data
      ..u8(2)
      ..u32(_fitTimestamp(t))
      ..u32(s);
  }
  // respiration_rate (local 3, global 297): timestamp + rate (sint16, ×100).
  data.def(3, 297, [
    [253, 4, 0x86],
    [0, 2, 0x83],
  ]);
  for (final (t, r) in respiration) {
    data
      ..u8(3)
      ..u32(_fitTimestamp(t))
      ..u16((r * 100).round());
  }

  return _wrap(data.toBytes());
}
