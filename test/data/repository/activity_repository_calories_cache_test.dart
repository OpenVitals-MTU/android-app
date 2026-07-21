import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/period/period_load_query.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';
import 'package:openvitals/data/repository/impl/activity_repository_impl.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';

/// Grants everything and returns no steps; `readDailyNutrition` throws so the
/// test proves a cache hit never touches the live calories read.
class _NutritionThrowsSource extends HealthDataSource {
  bool nutritionCalled = false;

  @override
  Future<Set<String>> grantedPermissions() async => {
        HcPermissions.readSteps,
        HcPermissions.readNutrition,
        HcPermissions.readTotalCalories,
        HcPermissions.readActiveCalories,
      };

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
  Future<List<DailyNutrition>> readDailyNutrition(
    LocalDate startDate,
    LocalDate endDate, {
    bool includeHydration = true,
    bool includeEstimatedCalories = false,
  }) async {
    nutritionCalled = true;
    throw StateError('live nutrition read must not run on a cache hit');
  }
}

void main() {
  late OpenVitalsDatabase db;
  late VitalsDailyCacheDao dao;

  setUp(() {
    db = OpenVitalsDatabase(NativeDatabase.memory());
    dao = db.vitalsDailyCacheDao;
  });
  tearDown(() => db.close());

  PeriodLoadQuery yearQuery() {
    final today = LocalDate.now();
    return PeriodLoadQuery(range: TimeRange.year, anchorDate: today, today: today);
  }

  test('a synced, in-window calories day is served from the cache', () async {
    final day = LocalDate.now().minusDays(3); // recent, inside the year window
    await dao.writeFullSync(caloriesBurnedCacheMetric, 'tok', 0);
    await dao.upsertDay(
        metric: caloriesBurnedCacheMetric,
        epochDay: day.epochDay,
        valueSum: 2500,
        sampleCount: 1);

    final ds = _NutritionThrowsSource()
      ..cachedAvailability = HealthConnectAvailability.available;
    final repo = ActivityRepositoryImpl(ds, caloriesCacheDao: dao);

    final result = await repo.loadActivityPeriod(
      yearQuery(),
      includeSteps: true,
      includeNutrition: true,
      includeComparisonWindows: false,
    );

    expect(ds.nutritionCalled, isFalse, reason: 'cache hit, no live read');
    final data = result.getOrNull()!;
    expect(
      data.nutrition.firstWhere((n) => n.date == day).caloriesBurnedKcal,
      2500,
    );
  });

  test('without a cursor the cache is skipped and the live read runs', () async {
    // No writeFullSync ⇒ no cursor ⇒ cache miss ⇒ live read (which throws here).
    final ds = _NutritionThrowsSource()
      ..cachedAvailability = HealthConnectAvailability.available;
    final repo = ActivityRepositoryImpl(ds, caloriesCacheDao: dao);

    final result = await repo.loadActivityPeriod(
      yearQuery(),
      includeSteps: true,
      includeNutrition: true,
      includeComparisonWindows: false,
    );

    // The live read threw → the load fails → confirms the cache was bypassed.
    expect(ds.nutritionCalled, isTrue);
    expect(result.getOrNull(), isNull);
  });
}
