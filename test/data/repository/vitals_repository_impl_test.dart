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
}
