import 'dart:async';
import 'dart:math' as math;

import 'package:fake_async/fake_async.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
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
          .loadVitalsPeriod(_yearQuery(), VitalsPeriodMetric.all);

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
            .loadVitalsPeriod(_yearQuery(), VitalsPeriodMetric.all)
            .then((r) => result = r);

        async.flushMicrotasks();
        expect(result, isNull, reason: 'still loading before the budget elapses');

        async.elapse(const Duration(seconds: 31));
        expect(result, isA<Err<VitalsPeriodData>>(),
            reason: 'the read budget must surface a retryable failure');
      });
    });
  });
}
