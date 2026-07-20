import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/repository/impl/activity_repository_impl.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';

/// Records whether the Day-only intraday aggregate read fires. That read is the
/// only one the Day range issues that the other ranges do not, and an unbounded
/// stall in it was hanging the Day view; callers that never render the intraday
/// chart (the calories overview) must be able to skip it.
class _ProgressCaptureDataSource extends HealthDataSource {
  _ProgressCaptureDataSource(this._granted);

  final Set<String> _granted;
  bool readRawActivityProgressCalled = false;

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
  }) async =>
      const [];

  @override
  Future<List<ActivityProgressPoint>> readRawActivityProgress(
    LocalDate date,
  ) async {
    readRawActivityProgressCalled = true;
    return const [];
  }
}

_ProgressCaptureDataSource _source(Set<String> granted) =>
    _ProgressCaptureDataSource(granted)
      ..cachedAvailability = HealthConnectAvailability.available
      ..featureFlags =
          HealthConnectFeatureFlags(healthDataHistoryAvailable: true);

void main() {
  final today = LocalDate.now();
  PeriodLoadQuery dayQuery() =>
      PeriodLoadQuery(range: TimeRange.day, anchorDate: today);

  test('Day range reads the intraday progress by default', () async {
    final ds = _source({HcPermissions.readSteps});
    await ActivityRepositoryImpl(ds).loadActivityPeriod(
      dayQuery(),
      includeSteps: true,
      includeNutrition: false,
    );
    expect(ds.readRawActivityProgressCalled, isTrue);
  });

  test('includeActivityProgress:false skips the intraday read on Day', () async {
    final ds = _source({HcPermissions.readSteps});
    await ActivityRepositoryImpl(ds).loadActivityPeriod(
      dayQuery(),
      includeSteps: true,
      includeNutrition: false,
      includeActivityProgress: false,
    );
    expect(ds.readRawActivityProgressCalled, isFalse);
  });
}
