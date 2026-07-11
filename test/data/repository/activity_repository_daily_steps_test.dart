import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/impl/activity_repository_impl.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/health/health_data_source.dart';
import 'package:openvitals/health/health_permissions.dart';

/// Records the arguments `loadDailySteps` forwards, so the tests can assert the
/// effective-start clamp and the floors flag without a real provider.
class _CaptureDataSource extends HealthDataSource {
  _CaptureDataSource(this._granted);

  final Set<String> _granted;
  LocalDate? capturedStart;
  bool? capturedFloors;

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
    capturedStart = startDate;
    capturedFloors = includeFloors;
    return const [];
  }
}

_CaptureDataSource _source(
  Set<String> granted, {
  bool historyAvailable = true,
}) {
  final ds = _CaptureDataSource(granted)
    ..cachedAvailability = HealthConnectAvailability.available
    ..featureFlags =
        HealthConnectFeatureFlags(healthDataHistoryAvailable: historyAvailable);
  return ds;
}

void main() {
  final legacyStart = LocalDate(2009, 1, 1);
  final end = LocalDate(2026, 7, 10);

  test('clamps the scan to the last 30 days when history perm is ungranted',
      () async {
    final ds = _source({HcPermissions.readSteps});
    await ActivityRepositoryImpl(ds).loadDailySteps(legacyStart, end);

    // Health Connect only surfaces the last 30 days without the history
    // permission, so the scan starts at end - 29 days, not 2009.
    expect(ds.capturedStart, end.minusDays(29));
  });

  test('scans from the full start when the history perm is granted', () async {
    final ds = _source({
      HcPermissions.readSteps,
      HealthPermissionService.readHealthDataHistoryPermission,
    });
    await ActivityRepositoryImpl(ds).loadDailySteps(legacyStart, end);

    expect(ds.capturedStart, legacyStart);
  });

  test('scans from the full start when history access is not gated', () async {
    // Device/provider does not require the history permission at all → no clamp.
    final ds = _source({HcPermissions.readSteps}, historyAvailable: false);
    await ActivityRepositoryImpl(ds).loadDailySteps(legacyStart, end);

    expect(ds.capturedStart, legacyStart);
  });

  test('requests floors when the floors permission is granted', () async {
    final ds = _source({HcPermissions.readSteps, HcPermissions.readFloors});
    await ActivityRepositoryImpl(ds).loadDailySteps(legacyStart, end);

    expect(ds.capturedFloors, isTrue);
  });

  test('omits floors when the floors permission is ungranted', () async {
    final ds = _source({HcPermissions.readSteps});
    await ActivityRepositoryImpl(ds).loadDailySteps(legacyStart, end);

    expect(ds.capturedFloors, isFalse);
  });

  test('returns empty without the steps permission', () async {
    final ds = _source(const <String>{});
    final result =
        await ActivityRepositoryImpl(ds).loadDailySteps(legacyStart, end);

    expect(result, isEmpty);
    expect(ds.capturedStart, isNull, reason: 'never queries the data source');
  });
}
