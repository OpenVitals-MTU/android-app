import 'dart:typed_data';

import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/devices/garmin/wellness/fit_wellness_import.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';
import 'package:openvitals/devices/garmin/wellness/garmin_fit_wellness.dart';

/// Garmin device epoch: seconds between the Unix and Garmin epochs.
const int _garminEpochOffset = 631065600;

int _fitTimestamp(DateTime t) =>
    t.toUtc().millisecondsSinceEpoch ~/ 1000 - _garminEpochOffset;

/// Minimal FIT writer, as in `fit_stress_body_energy_test.dart`.
class _W {
  final List<int> _b = [];
  void u8(int v) => _b.add(v & 0xFF);
  void u16(int v) => _b.addAll([v & 0xFF, (v >> 8) & 0xFF]);
  void u32(int v) => _b.addAll([
        v & 0xFF,
        (v >> 8) & 0xFF,
        (v >> 16) & 0xFF,
        (v >> 24) & 0xFF,
      ]);
  void bytes(List<int> v) => _b.addAll(v);
  Uint8List toBytes() => Uint8List.fromList(_b);
}

/// Wraps a data section in the FIT header/CRC envelope.
Uint8List _wrap(Uint8List data) => (_W()
      ..u8(14)
      ..u8(16)
      ..u16(0)
      ..u32(data.length)
      ..bytes([0x2E, 0x46, 0x49, 0x54])
      ..u16(0)
      ..bytes(data)
      ..u16(0))
    .toBytes();

/// `file_id` carrying just the type, so the file classifies.
void _fileId(_W d, int fileType) {
  d
    ..u8(0x40)
    ..u8(0)
    ..u8(0)
    ..u16(0)
    ..u8(1)
    ..bytes([0, 1, 0])
    ..u8(0)
    ..u8(fileType);
}

/// A metrics file (type 44): VO2 max, recovery time, readiness and load, each
/// in its own message, exactly as the watch splits them.
Uint8List _metricsFile({
  required DateTime at,
  int? vo2MaxTenths,
  int? recoveryMinutes,
  int? readiness,
  int? loadAcute,
  int? loadChronic,
}) {
  final d = _W();
  _fileId(d, 44);
  if (vo2MaxTenths != null) {
    d
      ..u8(0x41)
      ..u8(0)
      ..u8(0)
      ..u16(229)
      ..u8(2)
      ..bytes([253, 4, 134]) // timestamp, uint32
      ..bytes([2, 2, 132]) // vo2_max, uint16 scale 10
      ..u8(0x01)
      ..u32(_fitTimestamp(at))
      ..u16(vo2MaxTenths);
  }
  if (recoveryMinutes != null) {
    d
      ..u8(0x42)
      ..u8(0)
      ..u8(0)
      ..u16(140)
      ..u8(2)
      ..bytes([253, 4, 134])
      ..bytes([9, 2, 132]) // recovery_time, uint16 minutes
      ..u8(0x02)
      ..u32(_fitTimestamp(at))
      ..u16(recoveryMinutes);
  }
  if (readiness != null) {
    d
      ..u8(0x43)
      ..u8(0)
      ..u8(0)
      ..u16(369)
      ..u8(2)
      ..bytes([253, 4, 134])
      ..bytes([0, 1, 2]) // training_readiness, uint8
      ..u8(0x03)
      ..u32(_fitTimestamp(at))
      ..u8(readiness);
  }
  if (loadAcute != null || loadChronic != null) {
    d
      ..u8(0x44)
      ..u8(0)
      ..u8(0)
      ..u16(378)
      ..u8(3)
      ..bytes([253, 4, 134])
      ..bytes([3, 2, 132])
      ..bytes([4, 2, 132])
      ..u8(0x04)
      ..u32(_fitTimestamp(at))
      ..u16(loadAcute ?? 0xFFFF)
      ..u16(loadChronic ?? 0xFFFF);
  }
  return _wrap(d.toBytes());
}

/// A sleep file (type 49): the event/74 bounds, one stage transition so a
/// session forms, plus the watch's own sleep_stats and any naps.
Uint8List _sleepFile({
  required DateTime start,
  required DateTime end,
  int? score,
  int? awakenings,
  List<(DateTime, DateTime)> naps = const [],
}) {
  final d = _W();
  _fileId(d, 49);
  // event (21): field 0 event, 1 event_type, 253 timestamp.
  d
    ..u8(0x41)
    ..u8(0)
    ..u8(0)
    ..u16(21)
    ..u8(3)
    ..bytes([253, 4, 134])
    ..bytes([0, 1, 0])
    ..bytes([1, 1, 0])
    ..u8(0x01)
    ..u32(_fitTimestamp(start))
    ..u8(74)
    ..u8(0) // start
    ..u8(0x01)
    ..u32(_fitTimestamp(end))
    ..u8(74)
    ..u8(1); // stop
  // sleep_level (275): one transition to light so a stage exists. Its timestamp
  // is the stage's UPPER BOUND (its end), so it sits at the session stop and the
  // light stage spans the night — a transition at the start would name a
  // zero-length span with nothing before it.
  d
    ..u8(0x42)
    ..u8(0)
    ..u8(0)
    ..u16(275)
    ..u8(2)
    ..bytes([253, 4, 134])
    ..bytes([0, 1, 0])
    ..u8(0x02)
    ..u32(_fitTimestamp(end))
    ..u8(2); // light
  if (score != null || awakenings != null) {
    d
      ..u8(0x43)
      ..u8(0)
      ..u8(0)
      ..u16(346)
      ..u8(2)
      ..bytes([6, 1, 2]) // overall_sleep_score, uint8
      ..bytes([11, 1, 2]) // awakenings_count, uint8
      ..u8(0x03)
      ..u8(score ?? 0xFF)
      ..u8(awakenings ?? 0xFF);
  }
  if (naps.isNotEmpty) {
    d
      ..u8(0x44)
      ..u8(0)
      ..u8(0)
      ..u16(412)
      ..u8(2)
      ..bytes([0, 4, 134]) // start_timestamp
      ..bytes([2, 4, 134]); // end_timestamp
    for (final (napStart, napEnd) in naps) {
      d
        ..u8(0x04)
        ..u32(_fitTimestamp(napStart))
        ..u32(_fitTimestamp(napEnd));
    }
  }
  return _wrap(d.toBytes());
}

/// A monitoring file (type 32) carrying the intensity-minute totals.
///
/// [alt] writes them into 33/34 instead of 37/38 — both name the same quantity
/// and which pair a device populates varies.
Uint8List _intensityFile(
  List<(DateTime, int moderate, int vigorous)> samples, {
  bool alt = false,
}) {
  final d = _W();
  _fileId(d, 32);
  d
    ..u8(0x41)
    ..u8(0)
    ..u8(0)
    ..u16(55)
    ..u8(3)
    ..bytes([253, 4, 134]) // timestamp, uint32
    ..bytes([alt ? 33 : 37, 2, 132]) // moderate, uint16 minutes
    ..bytes([alt ? 34 : 38, 2, 132]); // vigorous, uint16 minutes
  for (final (at, moderate, vigorous) in samples) {
    d
      ..u8(0x01)
      ..u32(_fitTimestamp(at))
      ..u16(moderate)
      ..u16(vigorous);
  }
  return _wrap(d.toBytes());
}

/// A metrics file as a vívoactive 5 actually writes it: daily_sleep (384) and
/// sleep_demand (410), with none of the training-load messages other Garmins
/// put in this file type.
Uint8List _dailySleepFile({
  required DateTime endTime,
  int? score,
  int? awakeSeconds,
  int? pressure,
  int? normalMinutes,
  int? demandMinutes,
  DateTime? demandAt,
}) {
  final d = _W();
  _fileId(d, 44);
  d
    ..u8(0x41)
    ..u8(0)
    ..u8(0)
    ..u16(384)
    ..u8(4)
    ..bytes([2, 1, 2]) // sleep_score, uint8
    ..bytes([3, 2, 132]) // awake_duration, uint16
    ..bytes([11, 4, 134]) // sleep_end_time, uint32
    ..bytes([22, 2, 131]) // sleep_pressure, sint16
    ..u8(0x01)
    ..u8(score ?? 0xFF)
    ..u16(awakeSeconds ?? 0xFFFF)
    ..u32(_fitTimestamp(endTime))
    ..u16(pressure ?? 0x7FFF);
  if (normalMinutes != null || demandMinutes != null) {
    d
      ..u8(0x42)
      ..u8(0)
      ..u8(0)
      ..u16(410)
      ..u8(3)
      ..bytes([253, 4, 134])
      ..bytes([0, 2, 132])
      ..bytes([1, 2, 132])
      ..u8(0x02)
      ..u32(_fitTimestamp(demandAt ?? endTime))
      ..u16(normalMinutes ?? 0xFFFF)
      ..u16(demandMinutes ?? 0xFFFF);
  }
  return _wrap(d.toBytes());
}

/// A Health Snapshot file (type 70). Each message packs a whole recording into
/// one record: field 0 the seconds between samples, field 1 an ARRAY.
Uint8List _hsaFile({
  required DateTime at,
  required int globalMessage,
  required int intervalSeconds,
  required List<int> samples,
  int elementSize = 1,
  int baseType = 2, // uint8
}) {
  final d = _W();
  _fileId(d, 70);
  d
    ..u8(0x41)
    ..u8(0)
    ..u8(0)
    ..u16(globalMessage)
    ..u8(3)
    ..bytes([253, 4, 134]) // timestamp, uint32
    ..bytes([0, 2, 132]) // processing_interval, uint16
    ..bytes([1, samples.length * elementSize, baseType]) // the array
    ..u8(0x01)
    ..u32(_fitTimestamp(at))
    ..u16(intervalSeconds);
  for (final s in samples) {
    if (elementSize == 1) {
      d.u8(s);
    } else {
      d.u16(s);
    }
  }
  return _wrap(d.toBytes());
}

void main() {
  final at = DateTime.utc(2026, 7, 22, 6, 30);

  group('health snapshot', () {
    test('unpacks an array field into one sample per interval', () {
      // The capability the parser lacked: every other field is a scalar, and
      // reading only the first element would have silently dropped a whole
      // two-minute recording down to its opening reading.
      final snapshot = parseGarminWellness(_hsaFile(
        at: at,
        globalMessage: 305, // hsa_spo2_data
        intervalSeconds: 5,
        samples: [96, 97, 97, 98],
      )).healthSnapshot!;

      expect(snapshot.spo2, [
        (at, 96),
        (at.add(const Duration(seconds: 5)), 97),
        (at.add(const Duration(seconds: 10)), 97),
        (at.add(const Duration(seconds: 15)), 98),
      ]);
    });

    test('SpO2 reaches Health Connect, stress does not', () {
      final spo2 = parseGarminWellness(_hsaFile(
        at: at,
        globalMessage: 305,
        intervalSeconds: 60,
        samples: [95, 96],
      )).healthSnapshot!;
      final records = fitHealthSnapshotImportRecords(spo2);
      expect(records, hasLength(2));
      expect(records.first, isA<OxygenSaturationImportRecord>());
      // Namespaced apart from the all-day series: a deliberate spot measurement
      // must not overwrite the passive one taken at the same instant.
      expect(records.first.clientRecordId, startsWith('garmin_fit_hsa_spo2_'));

      final stress = parseGarminWellness(_hsaFile(
        at: at,
        globalMessage: 306, // hsa_stress_data
        intervalSeconds: 60,
        samples: [30, 35],
      )).healthSnapshot!;
      expect(stress.stress, hasLength(2));
      // No Health Connect type for stress, so nothing to map.
      expect(fitHealthSnapshotImportRecords(stress), isEmpty);
    });

    test('respiration is scaled by 100', () {
      final snapshot = parseGarminWellness(_hsaFile(
        at: at,
        globalMessage: 307, // hsa_respiration_data
        intervalSeconds: 30,
        samples: [1450, 1520],
        elementSize: 2,
        baseType: 131, // sint16
      )).healthSnapshot!;

      expect(snapshot.respiration.map((r) => r.$2), [14.5, 15.2]);
    });

    test('a zero interval drops the record rather than stacking samples', () {
      // Every sample would otherwise land on the same instant and the
      // (metric, time) key would collapse the recording to one reading.
      final wellness = parseGarminWellness(_hsaFile(
        at: at,
        globalMessage: 305,
        intervalSeconds: 0,
        samples: [96, 97, 98],
      ));
      expect(wellness.healthSnapshot, isNull);
    });

    test('out-of-range readings are dropped', () {
      final snapshot = parseGarminWellness(_hsaFile(
        at: at,
        globalMessage: 305,
        intervalSeconds: 10,
        samples: [96, 0, 97],
      )).healthSnapshot!;
      // 0% blood oxygen is a sentinel, not a reading.
      expect(snapshot.spo2.map((s) => s.$2), [96, 97]);
      // The dropped sample must not shift the ones after it.
      expect(snapshot.spo2.last.$1, at.add(const Duration(seconds: 20)));
    });
  });

  group('daily sleep, from the metrics file', () {
    final endTime = DateTime.utc(2026, 7, 22, 7, 20);

    test('reads awake duration as SECONDS, not the profile\'s minutes', () {
      // The number from a real night: 1020 inside an 8.7-hour window. Read as
      // minutes it would be 17 HOURS awake — longer than the night itself.
      final daily = parseGarminWellness(_dailySleepFile(
        endTime: endTime,
        score: 71,
        awakeSeconds: 1020,
      )).dailySleep!;

      expect(daily.awakeDuration, const Duration(minutes: 17));
      expect(daily.score, 71);
      expect(daily.endTime, endTime);
    });

    test('sleep pressure passes through raw, including negatives', () {
      final daily = parseGarminWellness(
        _dailySleepFile(endTime: endTime, pressure: -33),
      ).dailySleep!;
      // Undocumented scale — inventing units would be worse than passing it on.
      expect(daily.pressure, -33);
    });

    test('reads Sleep Coach need against the usual need', () {
      final demand = parseGarminWellness(_dailySleepFile(
        endTime: endTime,
        normalMinutes: 470,
        demandMinutes: 520,
      )).sleepDemand!;

      expect(demand.normal, const Duration(hours: 7, minutes: 50));
      expect(demand.demand, const Duration(hours: 8, minutes: 40));
    });

    test('a metrics file of only sleep data is not empty', () {
      // It was: this watch puts no training-load messages in the metrics file,
      // so a parser that looked only for those saw nothing and the whole file
      // was discarded as unusable.
      final wellness =
          parseGarminWellness(_dailySleepFile(endTime: endTime, score: 71));

      expect(wellness.isEmpty, isFalse);
      expect(wellness.metrics, isNull); // no VO2 max / load in this file
      expect(wellness.dailySleep, isNotNull);
    });

    test('invalid sentinels do not become readings', () {
      final wellness = parseGarminWellness(
        _dailySleepFile(endTime: endTime),
      );
      expect(wellness.dailySleep, isNull);
      expect(wellness.sleepDemand, isNull);
    });
  });

  group('intensity minutes', () {
    test('reads the running daily totals', () {
      final m = parseGarminWellness(_intensityFile([
        (at, 12, 4),
        (at.add(const Duration(minutes: 15)), 19, 4),
      ])).monitoring!;

      // Cumulative totals, not per-message increments — the mapper's problem,
      // not the parser's, so they are kept exactly as the watch wrote them.
      expect(m.moderateMinutes, [(at, 12), (at.add(const Duration(minutes: 15)), 19)]);
      expect(m.vigorousMinutes, [(at, 4), (at.add(const Duration(minutes: 15)), 4)]);
    });

    test('reads the alternate field pair too', () {
      final m = parseGarminWellness(
        _intensityFile([(at, 7, 2)], alt: true),
      ).monitoring!;
      expect(m.moderateMinutes, [(at, 7)]);
      expect(m.vigorousMinutes, [(at, 2)]);
    });

    test('zero is a real total and is kept', () {
      // The vívoactive 5 writes 0 all day until minutes are earned; dropping
      // those would make "no data yet" indistinguishable from "not tracked".
      final m =
          parseGarminWellness(_intensityFile([(at, 0, 0)])).monitoring!;
      expect(m.moderateMinutes, [(at, 0)]);
      expect(m.vigorousMinutes, [(at, 0)]);
    });

    test('the uint16 invalid sentinel is not a total', () {
      final m = parseGarminWellness(
        _intensityFile([(at, 0xFFFF, 0xFFFF)]),
      ).monitoring;
      expect(m?.moderateMinutes ?? const [], isEmpty);
      expect(m?.vigorousMinutes ?? const [], isEmpty);
    });
  });

  group('metrics file', () {
    test('reads VO2 max, recovery, readiness and load from one file', () {
      final metrics = parseGarminWellness(_metricsFile(
        at: at,
        vo2MaxTenths: 425,
        recoveryMinutes: 1320,
        readiness: 68,
        loadAcute: 412,
        loadChronic: 380,
      )).metrics!;

      expect(metrics.vo2Max, 42.5); // uint16 scale 10
      expect(metrics.recoveryTimeMinutes, 1320);
      expect(metrics.trainingReadiness, 68);
      expect(metrics.trainingLoadAcute, 412);
      expect(metrics.trainingLoadChronic, 380);
      expect(metrics.time, at);
    });

    test('a file carrying only training load still yields metrics', () {
      // The case that used to read as a failed import: the watch re-offers
      // metrics files constantly and most carry a subset.
      final wellness =
          parseGarminWellness(_metricsFile(at: at, loadAcute: 300));

      expect(wellness.metrics, isNotNull);
      expect(wellness.metrics!.trainingLoadAcute, 300);
      expect(wellness.metrics!.vo2Max, isNull);
      expect(wellness.isEmpty, isFalse);
    });

    test('only VO2 max reaches Health Connect', () {
      final wellness = parseGarminWellness(_metricsFile(
        at: at,
        vo2MaxTenths: 501,
        recoveryMinutes: 60,
        readiness: 80,
      ));
      final records = fitMetricsImportRecords(wellness.metrics!);

      // Recovery time and readiness have no Health Connect type; sending them
      // anywhere near the import path would mean inventing one.
      expect(records, hasLength(1));
      final record = records.single as Vo2MaxImportRecord;
      expect(record.vo2MillilitersPerMinuteKilogram, closeTo(50.1, 0.001));
      expect(record.clientRecordId,
          'garmin_fit_vo2max_${at.millisecondsSinceEpoch}');
    });

    test('a metrics file with no VO2 max maps to nothing', () {
      final wellness =
          parseGarminWellness(_metricsFile(at: at, readiness: 70));
      expect(fitMetricsImportRecords(wellness.metrics!), isEmpty);
    });
  });

  group('sleep extras', () {
    final start = DateTime.utc(2026, 7, 22, 0, 10);
    final end = DateTime.utc(2026, 7, 22, 7, 20);

    test("carries the watch's own score alongside the derived stages", () {
      final sleep = parseGarminWellness(
        _sleepFile(start: start, end: end, score: 74, awakenings: 3),
      ).sleep!;

      // Both survive on purpose: the score is Garmin's verdict, the stages are
      // ours, and a disagreement between them is the thing worth seeing.
      expect(sleep.overallScore, 74);
      expect(sleep.awakeningsCount, 3);
      expect(sleep.stages, isNotEmpty);
    });

    test('a night without sleep_stats still parses', () {
      final sleep =
          parseGarminWellness(_sleepFile(start: start, end: end))
              .sleep!;
      expect(sleep.overallScore, isNull);
      expect(sleep.awakeningsCount, isNull);
      expect(sleep.stages, isNotEmpty);
    });

    test('naps become their own stage-less sleep sessions', () {
      final napStart = DateTime.utc(2026, 7, 22, 14, 0);
      final napEnd = DateTime.utc(2026, 7, 22, 14, 35);
      final wellness = parseGarminWellness(_sleepFile(
        start: start,
        end: end,
        naps: [(napStart, napEnd)],
      ));

      expect(wellness.naps, hasLength(1));
      final records = fitNapImportRecords(wellness.naps);
      final nap = records.single as SleepSessionImportRecord;
      expect(nap.startTime, napStart);
      expect(nap.endTime, napEnd);
      // No stages: the nap message carries none, and inventing one would put a
      // fabricated stage next to the measured ones from the night.
      expect(nap.stages, isEmpty);
      expect(nap.clientRecordId,
          'garmin_fit_nap_${napStart.millisecondsSinceEpoch}');
    });

    test('a nap that ends before it starts is dropped', () {
      final wellness = parseGarminWellness(_sleepFile(
        start: start,
        end: end,
        naps: [(DateTime.utc(2026, 7, 22, 15), DateTime.utc(2026, 7, 22, 14))],
      ));
      expect(wellness.naps, isEmpty);
    });
  });
}
