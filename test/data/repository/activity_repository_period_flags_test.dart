import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/impl/activity_repository_impl.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';

/// Records the flags `loadActivityPeriod` forwards to `readDailySteps`.
///
/// Regression guard: the period read used by the floors/elevation/wheelchair
/// detail screens once dropped these flags entirely, so every daily row came
/// back with null optional columns and the screens rendered 0 while their
/// intraday charts (a separate read) showed data.
class _CaptureDataSource extends HealthDataSource {
  _CaptureDataSource(this._granted);

  final Set<String> _granted;
  bool? capturedFloors;
  bool? capturedElevation;
  bool? capturedWheelchairPushes;

  @override
  Future<Set<String>> grantedPermissions() async => _granted;

  @override
  Future<List<DailySteps>> readDailySteps(
    LocalDate startDate,
    LocalDate endDate, {
    bool includeActiveCalories = false,
    bool includeFloors = false,
    bool includeWheelchairPushes = false,
    bool includeElevation = false,
  }) async {
    capturedFloors = includeFloors;
    capturedElevation = includeElevation;
    capturedWheelchairPushes = includeWheelchairPushes;
    return const [];
  }
}

_CaptureDataSource _source(Set<String> granted) =>
    _CaptureDataSource(granted)
      ..cachedAvailability = HealthConnectAvailability.available;

PeriodLoadQuery _dayQuery() {
  final today = LocalDate.now();
  return PeriodLoadQuery(range: TimeRange.day, anchorDate: today, today: today);
}

void main() {
  test('period read requests floors and elevation when granted', () async {
    final ds = _source({
      HcPermissions.readSteps,
      HcPermissions.readFloors,
      HcPermissions.readElevation,
    });

    await ActivityRepositoryImpl(ds).loadActivityPeriod(
      _dayQuery(),
      includeSteps: true,
      includeNutrition: false,
    );

    expect(ds.capturedFloors, isTrue);
    expect(ds.capturedElevation, isTrue);
  });

  test('period read omits floors and elevation when ungranted', () async {
    final ds = _source({HcPermissions.readSteps});

    await ActivityRepositoryImpl(ds).loadActivityPeriod(
      _dayQuery(),
      includeSteps: true,
      includeNutrition: false,
    );

    expect(ds.capturedFloors, isFalse);
    expect(ds.capturedElevation, isFalse);
  });

  test('period read forwards wheelchair pushes when asked and granted',
      () async {
    final ds = _source({
      HcPermissions.readSteps,
      HcPermissions.readWheelchairPushes,
    });

    await ActivityRepositoryImpl(ds).loadActivityPeriod(
      _dayQuery(),
      includeSteps: false,
      includeNutrition: false,
      includeWheelchairPushes: true,
    );

    expect(ds.capturedWheelchairPushes, isTrue);
  });

  test('period read omits wheelchair pushes when the metric never asked',
      () async {
    // The steps screen must not pay for a wheelchair aggregate it never shows,
    // even when the permission is granted.
    final ds = _source({
      HcPermissions.readSteps,
      HcPermissions.readWheelchairPushes,
    });

    await ActivityRepositoryImpl(ds).loadActivityPeriod(
      _dayQuery(),
      includeSteps: true,
      includeNutrition: false,
    );

    expect(ds.capturedWheelchairPushes, isFalse);
  });
}
