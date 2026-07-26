import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';
import 'package:openvitals/devices/garmin/wellness/fit_wellness_import.dart';
import 'package:openvitals/devices/garmin/wellness/garmin_fit_wellness.dart';

/// Sleep import from Garmin FIT (file type 49). The bytes are hand-built so no
/// real health data is committed; the layout mirrors what a vívoactive writes —
/// an `event`/74 start/stop pair for the bounds and `sleep_level` transitions
/// for the stages. See docs/reference/garmin-fit-files.md.
void main() {
  _incrementalSyncRegression();

  final start = DateTime.utc(2024, 1, 1, 23, 0, 0);
  final stop = DateTime.utc(2024, 1, 2, 6, 0, 0);
  // (transition, sleep_level enum: 0 unmeasurable,1 awake,2 light,3 deep,4 rem).
  // Each timestamp is the UPPER BOUND of the stage it names: the stage runs from
  // the previous transition (the session start for the first) up to here. The
  // last transition is the session stop, as a real Garmin file writes it.
  final levels = <(DateTime, int)>[
    (DateTime.utc(2024, 1, 1, 23, 10), 2), // light: 23:00 -> 23:10
    (DateTime.utc(2024, 1, 1, 23, 40), 3), // deep:  23:10 -> 23:40
    (DateTime.utc(2024, 1, 2, 0, 30), 4), // rem:   23:40 -> 00:30
    (DateTime.utc(2024, 1, 2, 0, 45), 1), // awake: 00:30 -> 00:45
    (DateTime.utc(2024, 1, 2, 6, 0), 2), // light:  00:45 -> 06:00 (to stop)
  ];

  group('parseGarminSleepSession', () {
    test('reads the session bounds and a contiguous stage timeline', () {
      final session = parseGarminSleepSession(
        _fitSleepBytes(start: start, stop: stop, levels: levels),
      )!;

      expect(session.start, start);
      expect(session.end, stop);
      expect(session.stages.length, 5);

      // Each stage ends at its transition; the first begins at the session start.
      expect(session.stages.first.level, FitSleepLevel.light);
      expect(session.stages.first.start, start);
      expect(session.stages.first.end, DateTime.utc(2024, 1, 1, 23, 10));
      expect(session.stages[1].level, FitSleepLevel.deep);
      expect(session.stages[1].end, DateTime.utc(2024, 1, 1, 23, 40));
      expect(session.stages[2].level, FitSleepLevel.rem);
      expect(session.stages.last.level, FitSleepLevel.light);
      expect(session.stages.last.end, stop);

      // Contiguous: every stage ends where the next begins.
      for (var i = 0; i + 1 < session.stages.length; i++) {
        expect(session.stages[i].end, session.stages[i + 1].start);
      }
    });

    test('returns null when the file carries no sleep timeline', () {
      final session = parseGarminSleepSession(
        _fitSleepBytes(start: start, stop: stop, levels: const []),
      );
      expect(session, isNull);
    });
  });

  group('fitSleepImportRecords', () {
    test('maps to one SleepSessionRecord with a deterministic id', () {
      final session = parseGarminSleepSession(
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
      final session = parseGarminSleepSession(
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
      final wellness = parseGarminWellness(
        _fitHrvBytes(time: hrvTime, rmssdMillis: 42.5),
      );
      expect(wellness.sleep, isNull);
      expect(wellness.hrv, isNotNull);
      expect(wellness.hrv!.time, hrvTime);
      // 42.5 ms -> raw round(42.5*128)=5440 -> 5440/128 = 42.5.
      expect(wellness.hrv!.rmssdMillis, closeTo(42.5, 0.01));
    });

    test('maps to one HeartRateVariabilityRmssd record', () {
      final reading = parseGarminWellness(
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
      final wellness = parseGarminWellness(
        _fitHrvBytes(time: hrvTime, rawOverride: 0xFFFF),
      );
      expect(wellness.hrv, isNull);
    });
  });

  group('monitoring (type 32) summary', () {
    final t = DateTime.utc(2024, 1, 18, 13, 42, 0);

    test('reads resting HR and BMR, maps to two records', () {
      final wellness = parseGarminWellness(
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
      final wellness = parseGarminWellness(
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
      final m = parseGarminWellness(bytes).monitoring!;
      final records = fitMonitoringImportRecords(m);

      final hr = records.whereType<HeartRateImportRecord>().toList();
      expect(hr, hasLength(2)); // one per hour (09:xx, 10:xx)
      expect(hr.expand((r) => r.samples).length, 4);

      final resp = records.whereType<RespiratoryRateImportRecord>().toList()
        ..sort((a, b) => a.time.compareTo(b.time));
      expect(resp, hasLength(2));
      expect(resp.first.rate, closeTo(14.0, 0.001)); // avg(13,15)

      // The counters are no longer part of this call — they are accumulated
      // across a whole sync and mapped once (see the intraday group below).
      expect(records.whereType<StepsImportRecord>(), isEmpty);
    });

    test('a day of counters becomes intraday records, not one flat total', () {
      // One record per day said how far you walked and never when, so Health
      // Connect drew the day as a straight ramp from midnight to now. The watch
      // samples the counters about once a minute; the shape is there to read.
      final import = _counterImport(
        stepsCumulative: [
          (DateTime(2024, 1, 18, 9), 0),
          (DateTime(2024, 1, 18, 10), 500),
          (DateTime(2024, 1, 18, 11), 1200),
        ],
      );
      final steps = _steps(import);

      // 09:00 read zero, so nothing happened before it worth recording.
      expect(steps, hasLength(2));
      expect(steps[0].startTime.toLocal(), DateTime(2024, 1, 18, 9));
      expect(steps[0].endTime.toLocal(), DateTime(2024, 1, 18, 10));
      expect(steps[0].count, 500);
      expect(steps[1].count, 700);
      // ...and they still add up to the day the wrist reported.
      expect(_stepsTotal(import), 1200);
    });

    test('what came before the first reading is not lost', () {
      // A watch synced at noon reports a counter already in the thousands. Those
      // steps have no snapshot to be differenced against, so they are recorded
      // against the stretch from midnight — the only claim the data supports.
      final import = _counterImport(
        stepsCumulative: [(DateTime(2024, 1, 18, 12), 8000)],
      );
      final steps = _steps(import).single;

      expect(steps.startTime.toLocal(), DateTime(2024, 1, 18));
      expect(steps.endTime.toLocal(), DateTime(2024, 1, 18, 12));
      expect(steps.count, 8000);
    });

    test('standing still writes nothing', () {
      final import = _counterImport(
        stepsCumulative: [
          (DateTime(2024, 1, 18, 9), 500),
          (DateTime(2024, 1, 18, 10), 500),
          (DateTime(2024, 1, 18, 11), 500),
        ],
      );

      // One record for the 500 before 09:00, and nothing for the two hours that
      // followed: a night of empty entries would bury the day.
      expect(_steps(import), hasLength(1));
      expect(_stepsTotal(import), 500);
    });

    test('the next sync carries on from the watermark, not from midnight', () {
      // The seam this exists for: each file holds only the minutes since the
      // last sync, so the steps between one sync's last reading and the next
      // sync's first are in NEITHER file's own differences.
      final first = _counterImport(
        stepsCumulative: [
          (DateTime(2024, 1, 18, 9), 500),
          (DateTime(2024, 1, 18, 10), 900),
        ],
      );
      final second = _counterImport(
        stepsCumulative: [
          (DateTime(2024, 1, 18, 11), 1500),
          (DateTime(2024, 1, 18, 12), 1700),
        ],
        previous: first.watermarks,
      );

      // 900 -> 1500 across the seam, then 1500 -> 1700.
      expect(_steps(second).first.startTime.toLocal(), DateTime(2024, 1, 18, 10));
      expect(_steps(second).first.count, 600);
      // Every step the wrist counted, and each one only once.
      expect(_stepsTotal(first) + _stepsTotal(second), 1700);
    });

    test('re-importing a file already behind the watermark writes nothing', () {
      // The bug this pins: 540 steps on the wrist became 1403 in Health Connect
      // over thirteen syncs of one day. A watch re-offers a file whose archive
      // flag did not stick, and a sync re-reads the file it was halfway through.
      final cumulative = [
        (DateTime(2024, 1, 18, 9), 200),
        (DateTime(2024, 1, 18, 10), 540),
      ];
      final first = _counterImport(stepsCumulative: cumulative);
      final again = _counterImport(
        stepsCumulative: cumulative,
        previous: first.watermarks,
      );

      expect(_stepsTotal(first), 540);
      expect(again.records, isEmpty);
    });

    test('a counter reset is not a walk backwards', () {
      // The counters restart from zero at a wear-session boundary.
      final import = _counterImport(
        stepsCumulative: [
          (DateTime(2024, 1, 18, 9), 900),
          (DateTime(2024, 1, 18, 10), 0),
          (DateTime(2024, 1, 18, 11), 300),
        ],
      );

      // The 900 stands, the reset adds nothing, and the climb back to 300 is
      // not counted again from zero.
      expect(_stepsTotal(import), 900);
    });

    test('activity-type counters are summed, never subtracted', () {
      // A walking counter at 540 beside a generic one still at 0 is not a
      // 540-step change. Taking max - min across all points made it one, which
      // is how a file with no new steps still wrote a full day's worth.
      expect(
        _stepsTotal(_counterImport(
          stepsCumulative: [
            (DateTime(2024, 1, 18, 9), 540),
            (DateTime(2024, 1, 18, 10), 0),
          ],
        )),
        540,
      );
    });

    test('a total moved between activity types is not counted twice', () {
      // The bug this pins: 24,724 steps on the wrist reached Health Connect as
      // 49,448 — exactly twice — for 25 Jul 2026. The watch does not only
      // accumulate per bucket, it MOVES a total from one to another and zeroes
      // the one it left (that day's generic bucket ended on 709 m of distance
      // and 6181 s of active time with no steps at all).
      expect(
        _stepsTotal(_counterImport(
          typedStepsCumulative: [
            (DateTime(2024, 1, 18, 9), 0, 24724),
            (DateTime(2024, 1, 18, 9), 6, 0),
            // The gaining bucket is written FIRST on purpose: a sum taken point
            // by point rather than instant by instant would see 24,724 in both.
            (DateTime(2024, 1, 18, 10), 6, 24724),
            (DateTime(2024, 1, 18, 10), 0, 0),
          ],
        )),
        24724,
      );
    });

    test('types still add up when they hold different totals', () {
      // The other half of the same rule: walking and running are genuinely
      // separate counters, and the day is their sum — the real 25 Jul file read
      // generic 0 + walking 24,724 + running 119.
      expect(
        _stepsTotal(_counterImport(
          typedStepsCumulative: [
            (DateTime(2024, 1, 18, 10), 0, 0),
            (DateTime(2024, 1, 18, 10), 6, 24724),
            (DateTime(2024, 1, 18, 10), 1, 119),
          ],
        )),
        24843,
      );
    });

    test('a counter naming no activity is not a bucket of its own', () {
      // An untyped counter beside typed ones is the same day's total under a
      // name of its own, so adding it to them counts those steps twice.
      expect(
        _stepsTotal(_counterImport(
          stepsCumulative: [(DateTime(2024, 1, 18, 9), 24724)],
          typedStepsCumulative: [(DateTime(2024, 1, 18, 9), 6, 24724)],
        )),
        24724,
      );
    });

    test('an untyped counter still counts when it is all the file has', () {
      // ...but dropping it outright would report zero steps for a file that
      // names no activity type anywhere, which is all the counter it has.
      expect(
        _stepsTotal(_counterImport(
          stepsCumulative: [
            (DateTime(2024, 1, 18, 9), 500),
            (DateTime(2024, 1, 18, 10), 1200),
          ],
        )),
        1200,
      );
    });
  });
}

/// The counters a monitoring file carried, mapped as one sync would map them:
/// accumulated across the run's files, then differenced against [previous].
FitCounterImport _counterImport({
  List<(DateTime, int)> stepsCumulative = const [],
  List<(DateTime, int, int)> typedStepsCumulative = const [],
  List<(DateTime, int)> caloriesCumulative = const [],
  Map<String, FitCounterWatermark> previous = const {},
}) {
  final monitoring = parseGarminWellness(
    _fitMonitoringSeriesBytes(
      stepsCumulative: stepsCumulative,
      typedStepsCumulative: typedStepsCumulative,
      caloriesCumulative: caloriesCumulative,
    ),
  ).monitoring!;
  return fitMonitoringCounterRecords(
    fitMonitoringCounters(monitoring),
    previous: previous,
  );
}

List<StepsImportRecord> _steps(FitCounterImport import) =>
    import.records.whereType<StepsImportRecord>().toList()
      ..sort((a, b) => a.startTime.compareTo(b.startTime));

int _stepsTotal(FitCounterImport import) =>
    _steps(import).fold(0, (sum, record) => sum + record.count);

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
  List<(DateTime, int, int)> typedStepsCumulative = const [],
  List<(DateTime, int)> caloriesCumulative = const [],
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
  // monitoring steps carrying their activity_type (local 4, global 55), as a
  // real watch writes them: one message per active type at each timestamp.
  // Written AFTER the untyped ones above so those keep no type — the decoder
  // carries the last declared type forward, so a message following a typed one
  // inherits it rather than counting as unknown.
  data.def(4, 55, [
    [253, 4, 0x86],
    [5, 1, 0x00],
    [3, 4, 0x86],
  ]);
  for (final (t, activityType, s) in typedStepsCumulative) {
    data
      ..u8(4)
      ..u32(_fitTimestamp(t))
      ..u8(activityType)
      ..u32(s);
  }
  // monitoring active calories (local 5, global 55): timestamp + cumulative
  // active_calories (field 19, uint16). Untested until the counter records
  // started being written per interval — the id derivation is shared with
  // steps, so a bug in it reaches calories too.
  data.def(5, 55, [
    [253, 4, 0x86],
    [19, 2, 0x84],
  ]);
  for (final (t, kcal) in caloriesCumulative) {
    data
      ..u8(5)
      ..u32(_fitTimestamp(t))
      ..u16(kcal);
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

/// Regression: successive syncs within one hour must not overwrite each other.
///
/// The hourly clientRecordId assumed one file per day. A watch sync delivers a
/// fresh file every few minutes, so several land in the same hour — and sharing
/// a key made each one REPLACE the last, collapsing an hour of heart rate to
/// whichever sliver synced most recently.
void _incrementalSyncRegression() {
  FitMonitoringSummary monitoringWith({
    required List<(DateTime, int)> hr,
    List<(DateTime, double)> respiration = const [],
  }) =>
      FitMonitoringSummary(
        heartRateSamples: hr,
        respiration: respiration,
        stepPoints: const [],
        distancePoints: const [],
        caloriePoints: const [],
      );

  group('incremental files in the same hour', () {
    test('two HR chunks produce two distinct records', () {
      // Two consecutive sync windows, both inside the 10:00 hour.
      final first = fitMonitoringImportRecords(monitoringWith(hr: [
        (DateTime.utc(2026, 7, 22, 10, 5), 70),
        (DateTime.utc(2026, 7, 22, 10, 6), 71),
      ])).whereType<HeartRateImportRecord>().single;
      final second = fitMonitoringImportRecords(monitoringWith(hr: [
        (DateTime.utc(2026, 7, 22, 10, 40), 74),
        (DateTime.utc(2026, 7, 22, 10, 41), 75),
      ])).whereType<HeartRateImportRecord>().single;

      expect(
        first.clientRecordId,
        isNot(second.clientRecordId),
        reason: 'a shared id makes Health Connect upsert one over the other',
      );
    });

    test('re-importing the same chunk stays idempotent', () {
      final samples = [
        (DateTime.utc(2026, 7, 22, 10, 5), 70),
        (DateTime.utc(2026, 7, 22, 10, 6), 71),
      ];
      final a = fitMonitoringImportRecords(monitoringWith(hr: samples))
          .whereType<HeartRateImportRecord>()
          .single;
      final b = fitMonitoringImportRecords(monitoringWith(hr: samples))
          .whereType<HeartRateImportRecord>()
          .single;

      // Same data must keep the same id, so a repeat sync overwrites itself
      // rather than duplicating.
      expect(a.clientRecordId, b.clientRecordId);
    });

    test('a whole-day file still yields one record per hour', () {
      final records = fitMonitoringImportRecords(monitoringWith(hr: [
        for (var h = 0; h < 24; h++)
          (DateTime.utc(2026, 7, 22, h, 30), 60 + h),
      ])).whereType<HeartRateImportRecord>();

      expect(records, hasLength(24));
      expect(records.map((r) => r.clientRecordId).toSet(), hasLength(24));
    });

    test('respiration is keyed and timed on its first reading', () {
      final record = fitMonitoringImportRecords(monitoringWith(
        hr: const [],
        respiration: [
          (DateTime.utc(2026, 7, 22, 10, 5), 14.0),
          (DateTime.utc(2026, 7, 22, 10, 6), 16.0),
        ],
      )).whereType<RespiratoryRateImportRecord>().single;

      // Not the top of the hour, which every file in that hour would claim.
      expect(record.time, DateTime.utc(2026, 7, 22, 10, 5));
      expect(record.rate, 15.0);
    });
  });

  group('counter record identity', () {
    // The regression that made a real device report 49,695 steps for a 24,844
    // step day, with 2,506 minutes of coverage on a 1,440 minute day: the id
    // used to be derived from a walking CURSOR rather than from the clock, so
    // a re-sync re-partitioned the day and every record after the first landed
    // beside the previous run's instead of replacing it.

    /// What Health Connect actually stores: a later record with the same
    /// clientRecordId replaces the earlier one.
    Map<String, StepsImportRecord> upserted(List<FitCounterImport> imports) {
      final byId = <String, StepsImportRecord>{};
      for (final import in imports) {
        for (final record in _steps(import)) {
          byId[record.clientRecordId] = record;
        }
      }
      return byId;
    }

    test('a re-sync from a lost watermark replaces rather than accumulates',
        () {
      final cumulative = [
        for (var minute = 0; minute < 180; minute += 5)
          (DateTime(2024, 1, 18, 9).add(Duration(minutes: minute)), minute * 10),
      ];
      final first = _counterImport(stepsCumulative: cumulative);
      // The watermark is gone — a reinstall, or prefs cleared — so the whole
      // day is walked again, and this run sees a DIFFERENT set of instants.
      final relearned = _counterImport(
        stepsCumulative: [
          for (var minute = 0; minute < 180; minute += 7)
            (DateTime(2024, 1, 18, 9).add(Duration(minutes: minute)),
                minute * 10),
        ],
      );

      final stored = upserted([first, relearned]);
      final total = stored.values.fold(0, (sum, r) => sum + r.count);
      expect(total, lessThanOrEqualTo(1790),
          reason: 'the day must not be counted twice');

      // And no two stored records may cover the same minute.
      final spans = stored.values.toList()
        ..sort((a, b) => a.startTime.compareTo(b.startTime));
      for (var i = 1; i < spans.length; i++) {
        expect(
          spans[i].startTime.isBefore(spans[i - 1].endTime),
          isFalse,
          reason: 'records overlap: ${spans[i - 1].clientRecordId} '
              'and ${spans[i].clientRecordId}',
        );
      }
    });

    test('the same minutes always produce the same id', () {
      final cumulative = [
        (DateTime(2024, 1, 18, 9), 0),
        (DateTime(2024, 1, 18, 9, 20), 400),
        (DateTime(2024, 1, 18, 10), 900),
      ];

      expect(
        _steps(_counterImport(stepsCumulative: cumulative))
            .map((r) => r.clientRecordId),
        _steps(_counterImport(stepsCumulative: cumulative))
            .map((r) => r.clientRecordId),
      );
    });

    test("the day's first record keeps the legacy day-keyed id", () {
      // Before the counters became intraday, one record per day was written as
      // `garmin_fit_steps_<yyyy-mm-dd>`. Those are still in Health Connect
      // holding a whole day's total each, and no cursor-derived id could ever
      // collide with them — which is why the device showed the day twice.
      // Reusing the id makes the first bucket overwrite the stale record.
      final import = _counterImport(
        stepsCumulative: [
          (DateTime(2024, 1, 18, 6), 300),
          (DateTime(2024, 1, 18, 10), 900),
        ],
      );

      expect(
        _steps(import).first.clientRecordId,
        'garmin_fit_steps_2024-01-18',
      );
    });

    test('the legacy day key is handed out once, not re-handed each sync', () {
      // It is a one-shot: the record it supersedes is superseded the first time.
      // Recomputing "the first bucket" every sync would move the id to a later
      // bucket each run, and each move would overwrite the previous holder's
      // minutes with a different bucket's -- silently losing them.
      final first = _counterImport(
        stepsCumulative: [
          (DateTime(2024, 1, 18, 6), 300),
          (DateTime(2024, 1, 18, 10), 900),
        ],
      );
      expect(_steps(first).first.clientRecordId, 'garmin_fit_steps_2024-01-18');
      expect(first.watermarks['2024-01-18']!.legacyRetired, isTrue);

      final second = _counterImport(
        stepsCumulative: [
          (DateTime(2024, 1, 18, 14), 1500),
          (DateTime(2024, 1, 18, 18), 2100),
        ],
        previous: first.watermarks,
      );

      expect(
        _steps(second).map((r) => r.clientRecordId),
        isNot(contains('garmin_fit_steps_2024-01-18')),
        reason: 'the second sync must not steal the id from the first bucket',
      );
      expect(second.watermarks['2024-01-18']!.legacyRetired, isTrue);
    });

    test('a day whose first sync emitted nothing still retires the legacy id',
        () {
      // Everything the sync saw fell inside the first grid bucket, so that
      // bucket was still filling and nothing was emitted. Keying on "the bucket
      // at midnight" gave such a day no second chance: the next sync starts at
      // the watermark and has no midnight bucket either, so the whole-day
      // record survived beside every intraday record written afterwards.
      final first = _counterImport(
        stepsCumulative: [(DateTime(2024, 1, 18, 0, 5), 300)],
      );
      expect(_steps(first), isEmpty);
      expect(first.watermarks['2024-01-18']!.legacyRetired, isFalse);

      final second = _counterImport(
        stepsCumulative: [
          (DateTime(2024, 1, 18, 6), 300),
          (DateTime(2024, 1, 18, 10), 900),
        ],
        previous: first.watermarks,
      );

      expect(_steps(second).first.clientRecordId, 'garmin_fit_steps_2024-01-18');
      expect(second.watermarks['2024-01-18']!.legacyRetired, isTrue);
    });

    test('calories ride the same grid as steps', () {
      // The calorie counter path had no coverage at all, and it shares the id
      // derivation, so a bug in one reached the other unseen.
      final import = _counterImport(
        stepsCumulative: [
          (DateTime(2024, 1, 18, 9), 0),
          (DateTime(2024, 1, 18, 9, 20), 400),
        ],
        caloriesCumulative: [
          (DateTime(2024, 1, 18, 9), 0),
          (DateTime(2024, 1, 18, 9, 20), 80),
        ],
      );

      final calories =
          import.records.whereType<ActiveCaloriesBurnedImportRecord>().toList();
      expect(calories, isNotEmpty);
      expect(
        calories.map((r) => r.clientRecordId.replaceFirst('active_cal', 'steps')),
        containsAll(_steps(import).map((r) => r.clientRecordId)),
        reason: 'both counters must share one grid, or their records drift '
            'apart across re-syncs',
      );
      expect(calories.fold<double>(0, (s, r) => s + r.kilocalories), 80.0);
    });
  });
}
