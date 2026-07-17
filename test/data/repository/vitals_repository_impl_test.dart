import 'dart:async';
import 'dart:math' as math;

import 'package:fake_async/fake_async.dart';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';
import 'package:openvitals/data/repository/contract/vitals_repository.dart';
import 'package:openvitals/data/repository/impl/vitals_repository_impl.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/vitals_models.dart';
import 'package:openvitals/domain/query/vitals_period_data.dart';

/// A data source whose seven vitals reads all park on a shared [gate], so a test
/// can observe how many are in flight at once. [maxConcurrent] is the high-water
/// mark: with the repository fanning the ALL case out via `Future.wait` it
/// reaches seven; a serial `await`-after-`await` would never exceed one.
class _GatedVitalsSource extends HealthDataSource {
  _GatedVitalsSource(this.gate);

  /// Reads complete only when this resolves; leave it uncompleted to model a
  /// stuck native call.
  final Completer<void> gate;

  int _inFlight = 0;
  int maxConcurrent = 0;

  @override
  HealthConnectAvailability get cachedAvailability =>
      HealthConnectAvailability.available;

  @override
  bool isSkinTemperatureAvailable() => true;

  @override
  Future<Set<String>> grantedPermissions() async => {
        HcPermissions.readBloodPressure,
        HcPermissions.readSpO2,
        HcPermissions.readRespiratoryRate,
        HcPermissions.readBodyTemperature,
        HcPermissions.readVo2Max,
        HcPermissions.readBloodGlucose,
        HcPermissions.readSkinTemperature,
      };

  Future<List<T>> _gated<T>() async {
    _inFlight++;
    maxConcurrent = math.max(maxConcurrent, _inFlight);
    await gate.future;
    _inFlight--;
    return const [];
  }

  @override
  Future<List<BloodPressureEntry>> readBloodPressureEntries(
          DateTime start, DateTime end) =>
      _gated();

  @override
  Future<List<SpO2Entry>> readSpO2Entries(DateTime start, DateTime end) =>
      _gated();

  @override
  Future<List<RespiratoryRateEntry>> readRespiratoryRateEntries(
          DateTime start, DateTime end) =>
      _gated();

  @override
  Future<List<BodyTempEntry>> readBodyTemperatureEntries(
          DateTime start, DateTime end) =>
      _gated();

  @override
  Future<List<Vo2MaxEntry>> readVo2MaxEntries(DateTime start, DateTime end) =>
      _gated();

  @override
  Future<List<BloodGlucoseEntry>> readBloodGlucoseEntries(
          DateTime start, DateTime end) =>
      _gated();

  @override
  Future<List<SkinTemperatureEntry>> readSkinTemperatureEntries(
          DateTime start, DateTime end) =>
      _gated();
}

// The day view reads the seven raw vitals series (the fake gates those); the
// non-day view reads native daily aggregates instead, which this fake does not
// stub. Both go through the same Future.wait + timeout, so the day path is what
// exercises the parallelism and the read budget here.
PeriodLoadQuery _dayQuery() => PeriodLoadQuery(
      range: TimeRange.day,
      anchorDate: const LocalDate(2026, 7, 16),
    );

/// A source whose respiratory-rate DAILY read never returns (a year of dense
/// data), while every other metric answers empty from the base defaults. Models
/// the real "one metric is too large to read raw" case on the non-day overview.
class _SlowRespiratorySource extends HealthDataSource {
  _SlowRespiratorySource(this.gate);

  final Completer<void> gate;

  @override
  HealthConnectAvailability get cachedAvailability =>
      HealthConnectAvailability.available;

  @override
  bool isSkinTemperatureAvailable() => true;

  @override
  Future<Set<String>> grantedPermissions() async => {
        HcPermissions.readBloodPressure,
        HcPermissions.readSpO2,
        HcPermissions.readRespiratoryRate,
        HcPermissions.readBodyTemperature,
        HcPermissions.readVo2Max,
        HcPermissions.readBloodGlucose,
        HcPermissions.readSkinTemperature,
      };

  @override
  Future<List<DailyVitalPoint>> readDailyRespiratoryRate(
    LocalDate start,
    LocalDate end,
  ) async {
    await gate.future;
    return const [];
  }
}

PeriodLoadQuery _yearQuery() => PeriodLoadQuery(
      range: TimeRange.year,
      anchorDate: const LocalDate(2026, 7, 16),
    );

/// A source for the write-through tests: it accepts writes/updates/deletes and
/// serves programmable single-day daily aggregates keyed by [LocalDate.epochDay],
/// so a test can say what Health Connect returns for a given day after the write.
class _WriteThroughSource extends HealthDataSource {
  /// Returned by [readVitalsMeasurementEntry] — the entry the repository looks up
  /// to find the day of an update/delete.
  VitalsMeasurementEntry? entry;

  final Map<int, List<DailyVitalPoint>> singleByDay = {};
  final Map<int, List<DailyBloodPressurePoint>> bpByDay = {};

  /// When set, every daily read throws — models the recompute read failing.
  bool dailyThrows = false;

  int writes = 0;
  int updates = 0;
  int deletes = 0;

  @override
  HealthConnectAvailability get cachedAvailability =>
      HealthConnectAvailability.available;

  @override
  bool isSkinTemperatureAvailable() => true;

  @override
  Future<Set<String>> grantedPermissions() async => {
        HcPermissions.readBloodPressure,
        HcPermissions.readSpO2,
        HcPermissions.readRespiratoryRate,
        HcPermissions.readBodyTemperature,
        HcPermissions.writeBloodPressure,
        HcPermissions.writeSpO2,
        HcPermissions.writeRespiratoryRate,
        HcPermissions.writeBodyTemperature,
      };

  @override
  Future<String> writeVitalsMeasurementEntry(
    VitalsMeasurementWriteRequest request,
  ) async {
    writes++;
    return 'new-id';
  }

  @override
  Future<void> updateVitalsMeasurementEntry(
    String id,
    VitalsMeasurementWriteRequest request,
  ) async {
    updates++;
  }

  @override
  Future<void> deleteVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  ) async {
    deletes++;
  }

  @override
  Future<VitalsMeasurementEntry?> readVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  ) async =>
      entry;

  List<DailyVitalPoint> _single(LocalDate start) {
    if (dailyThrows) throw StateError('daily read failed');
    return singleByDay[start.epochDay] ?? const [];
  }

  @override
  Future<List<DailyVitalPoint>> readDailySpO2(LocalDate s, LocalDate e) async =>
      _single(s);

  @override
  Future<List<DailyVitalPoint>> readDailyRespiratoryRate(
          LocalDate s, LocalDate e) async =>
      _single(s);

  @override
  Future<List<DailyVitalPoint>> readDailyBodyTemperature(
          LocalDate s, LocalDate e) async =>
      _single(s);

  @override
  Future<List<DailyBloodPressurePoint>> readDailyBloodPressure(
      LocalDate s, LocalDate e) async {
    if (dailyThrows) throw StateError('daily read failed');
    return bpByDay[s.epochDay] ?? const [];
  }
}

void main() {
  group('VitalsRepositoryImpl.loadVitalsPeriod (ALL)', () {
    test('fans the seven vitals reads out concurrently, not serially', () async {
      final gate = Completer<void>();
      final source = _GatedVitalsSource(gate);

      final future = VitalsRepositoryImpl(source)
          .loadVitalsPeriod(_dayQuery(), VitalsPeriodMetric.all);

      // Let the load reach the point where every read is parked on the gate.
      await pumpEventQueue();
      expect(source.maxConcurrent, 7,
          reason: 'ALL must dispatch all seven reads before awaiting any');

      gate.complete();
      final result = await future;
      expect(result, isA<Ok<VitalsPeriodData>>());
    });

    test('a stuck read times out into a failure instead of hanging forever', () {
      fakeAsync((async) {
        // A gate that never completes models a native read that never returns —
        // the real "stuck on Syncing with Health Connect" case.
        final source = _GatedVitalsSource(Completer<void>());

        Result<VitalsPeriodData>? result;
        VitalsRepositoryImpl(source)
            .loadVitalsPeriod(_dayQuery(), VitalsPeriodMetric.all)
            .then((r) => result = r);

        async.flushMicrotasks();
        expect(result, isNull, reason: 'still loading before the budget elapses');

        async.elapse(const Duration(seconds: 31));
        expect(result, isA<Err<VitalsPeriodData>>(),
            reason: 'the read budget must surface a retryable failure');
      });
    });

    test('a metric too large to read degrades to empty and is flagged, not fatal',
        () {
      fakeAsync((async) {
        // Respiratory hangs; the other metrics answer empty immediately.
        final source = _SlowRespiratorySource(Completer<void>());

        Result<VitalsPeriodData>? result;
        VitalsRepositoryImpl(source)
            .loadVitalsPeriod(_yearQuery(), VitalsPeriodMetric.all)
            .then((r) => result = r);

        async.flushMicrotasks();
        // Past the per-metric budget (6s) but well under the whole-load budget.
        async.elapse(const Duration(seconds: 7));
        async.flushMicrotasks();

        expect(result, isA<Ok<VitalsPeriodData>>(),
            reason: 'one slow metric must not fail the whole overview');
        final data = (result! as Ok<VitalsPeriodData>).value;
        expect(data.respiratoryRateDaily, isEmpty);
        expect(data.timedOutMetrics, {VitalsPeriodMetric.respiratoryRate},
            reason: 'the timed-out metric is flagged so its card can say so');
      });
    });

    test('a synced metric reads daily points from the cache, not live',
        () async {
      final db = OpenVitalsDatabase(NativeDatabase.memory());
      final dao = db.vitalsDailyCacheDao;
      addTearDown(db.close);
      const anchor = LocalDate(2026, 7, 16);
      // Respiratory has been synced (cursor present) with one cached day inside
      // the year window.
      await dao.writeFullSync('respiratoryRate', 'tok', 0);
      await dao.upsertDay(
          metric: 'respiratoryRate',
          epochDay: anchor.epochDay,
          valueSum: 36,
          sampleCount: 3);

      // The live respiratory read would hang; reaching it would fail the test.
      final source = _SlowRespiratorySource(Completer<void>());
      final result = await VitalsRepositoryImpl(source, cacheDao: dao)
          .loadVitalsPeriod(_yearQuery(), VitalsPeriodMetric.all);

      expect(result, isA<Ok<VitalsPeriodData>>());
      final data = (result as Ok<VitalsPeriodData>).value;
      expect(data.timedOutMetrics, isEmpty,
          reason: 'the cache serves instantly, so nothing times out');
      expect(data.respiratoryRateDaily, hasLength(1));
      expect(data.respiratoryRateDaily.single.value, 12); // 36 / 3
    });
  });

  group('VitalsRepositoryImpl daily-cache write-through', () {
    const anchor = LocalDate(2026, 7, 16);

    VitalsMeasurementWriteRequest req(
      VitalsMeasurementType type,
      LocalDate day, {
      double value = 18,
      double? secondary,
    }) =>
        VitalsMeasurementWriteRequest(
          type: type,
          time: DateTime(day.year, day.month, day.day, 8),
          value: value,
          secondaryValue: secondary,
        );

    VitalsMeasurementEntry entryOn(VitalsMeasurementType type, LocalDate day) =>
        VitalsMeasurementEntry(
          id: 'e1',
          type: type,
          time: DateTime(day.year, day.month, day.day, 8),
          value: 18,
          source: 'tech.mmarca.openvitals',
          isOpenVitalsEntry: true,
        );

    (OpenVitalsDatabase, VitalsDailyCacheDao) buildDb() {
      final db = OpenVitalsDatabase(NativeDatabase.memory());
      addTearDown(db.close);
      return (db, db.vitalsDailyCacheDao);
    }

    test('a write refreshes the affected day in the cache', () async {
      final (_, dao) = buildDb();
      await dao.writeFullSync('respiratoryRate', 'tok', 0);
      final source = _WriteThroughSource()
        ..singleByDay[anchor.epochDay] = [
          DailyVitalPoint(date: anchor, value: 12, count: 3),
        ];

      final result = await VitalsRepositoryImpl(source, cacheDao: dao)
          .writeVitalsMeasurementEntry(
              req(VitalsMeasurementType.respiratoryRate, anchor));

      expect(result, isA<Ok<String>>());
      final rows = await dao.aggregatesBetween(
          'respiratoryRate', anchor.epochDay, anchor.epochDay);
      expect(rows, hasLength(1));
      expect(rows.single.valueSum, 36); // 12 mean × 3 readings
      expect(rows.single.sampleCount, 3);
    });

    test('a delete that empties the day removes its cached row', () async {
      final (_, dao) = buildDb();
      await dao.writeFullSync('respiratoryRate', 'tok', 0);
      await dao.upsertDay(
          metric: 'respiratoryRate',
          epochDay: anchor.epochDay,
          valueSum: 36,
          sampleCount: 3);
      final source = _WriteThroughSource()
        ..entry = entryOn(VitalsMeasurementType.respiratoryRate, anchor)
        ..singleByDay[anchor.epochDay] = const []; // empty once removed

      final result = await VitalsRepositoryImpl(source, cacheDao: dao)
          .deleteVitalsMeasurementEntry(
              VitalsMeasurementType.respiratoryRate, 'e1');

      expect(result, isA<Ok<void>>());
      expect(source.deletes, 1);
      final rows = await dao.aggregatesBetween(
          'respiratoryRate', anchor.epochDay, anchor.epochDay);
      expect(rows, isEmpty);
    });

    test('a delete leaving other readings recomputes the day', () async {
      final (_, dao) = buildDb();
      await dao.writeFullSync('respiratoryRate', 'tok', 0);
      await dao.upsertDay(
          metric: 'respiratoryRate',
          epochDay: anchor.epochDay,
          valueSum: 100,
          sampleCount: 5);
      final source = _WriteThroughSource()
        ..entry = entryOn(VitalsMeasurementType.respiratoryRate, anchor)
        ..singleByDay[anchor.epochDay] = [
          DailyVitalPoint(date: anchor, value: 10, count: 1),
        ];

      await VitalsRepositoryImpl(source, cacheDao: dao)
          .deleteVitalsMeasurementEntry(
              VitalsMeasurementType.respiratoryRate, 'e1');

      final rows = await dao.aggregatesBetween(
          'respiratoryRate', anchor.epochDay, anchor.epochDay);
      expect(rows.single.valueSum, 10);
      expect(rows.single.sampleCount, 1);
    });

    test('an edit across midnight recomputes both the old and new day',
        () async {
      final (_, dao) = buildDb();
      await dao.writeFullSync('respiratoryRate', 'tok', 0);
      final oldDay = anchor.plusDays(-1);
      // A stale cached mean on the old day the reading is moving away from.
      await dao.upsertDay(
          metric: 'respiratoryRate',
          epochDay: oldDay.epochDay,
          valueSum: 999,
          sampleCount: 1);
      final source = _WriteThroughSource()
        ..entry = entryOn(VitalsMeasurementType.respiratoryRate, oldDay)
        ..singleByDay[oldDay.epochDay] = const [] // old day now empty
        ..singleByDay[anchor.epochDay] = [
          DailyVitalPoint(date: anchor, value: 12, count: 2),
        ];

      final result = await VitalsRepositoryImpl(source, cacheDao: dao)
          .updateVitalsMeasurementEntry(
              'e1', req(VitalsMeasurementType.respiratoryRate, anchor));

      expect(result, isA<Ok<void>>());
      final oldRows = await dao.aggregatesBetween(
          'respiratoryRate', oldDay.epochDay, oldDay.epochDay);
      expect(oldRows, isEmpty, reason: 'the vacated old day is recomputed away');
      final newRows = await dao.aggregatesBetween(
          'respiratoryRate', anchor.epochDay, anchor.epochDay);
      expect(newRows.single.valueSum, 24); // 12 × 2
    });

    test('a blood-pressure write carries diastolic into secondarySum', () async {
      final (_, dao) = buildDb();
      await dao.writeFullSync('bloodPressure', 'tok', 0);
      final source = _WriteThroughSource()
        ..bpByDay[anchor.epochDay] = [
          DailyBloodPressurePoint(
              date: anchor, systolic: 120, diastolic: 80, count: 2),
        ];

      await VitalsRepositoryImpl(source, cacheDao: dao)
          .writeVitalsMeasurementEntry(req(
              VitalsMeasurementType.bloodPressure, anchor,
              value: 120, secondary: 80));

      final rows = await dao.aggregatesBetween(
          'bloodPressure', anchor.epochDay, anchor.epochDay);
      expect(rows.single.valueSum, 240); // 120 × 2
      expect(rows.single.secondarySum, 160); // 80 × 2
      expect(rows.single.sampleCount, 2);
    });

    test('a write is not cached until the metric has had its first sync',
        () async {
      final (_, dao) = buildDb();
      // No writeFullSync ⇒ no cursor.
      final source = _WriteThroughSource()
        ..singleByDay[anchor.epochDay] = [
          DailyVitalPoint(date: anchor, value: 12, count: 3),
        ];

      final result = await VitalsRepositoryImpl(source, cacheDao: dao)
          .writeVitalsMeasurementEntry(
              req(VitalsMeasurementType.respiratoryRate, anchor));

      expect(result, isA<Ok<String>>());
      final rows = await dao.aggregatesBetween(
          'respiratoryRate', anchor.epochDay, anchor.epochDay);
      expect(rows, isEmpty, reason: 'partial rows would be trusted by the reader');
    });

    test('with no cache wired, a write still succeeds', () async {
      final source = _WriteThroughSource();
      final result = await VitalsRepositoryImpl(source)
          .writeVitalsMeasurementEntry(
              req(VitalsMeasurementType.respiratoryRate, anchor));
      expect(result, isA<Ok<String>>());
      expect(source.writes, 1);
    });

    test('a cache-patch failure never fails the write', () async {
      final (_, dao) = buildDb();
      await dao.writeFullSync('respiratoryRate', 'tok', 0);
      final source = _WriteThroughSource()..dailyThrows = true;

      final result = await VitalsRepositoryImpl(source, cacheDao: dao)
          .writeVitalsMeasurementEntry(
              req(VitalsMeasurementType.respiratoryRate, anchor));

      expect(result, isA<Ok<String>>(),
          reason: 'the write succeeded; the drain will reconcile the cache');
      final rows = await dao.aggregatesBetween(
          'respiratoryRate', anchor.epochDay, anchor.epochDay);
      expect(rows, isEmpty);
    });
  });
}
