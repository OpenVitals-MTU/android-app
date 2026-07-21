import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/data/sync/calories_history_sync_service.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/model/vitals_change_batch.dart';

/// A source backed by a mutable "truth" of per-day calories burned, plus a queue
/// of change batches to hand back from getVitalsChanges.
class _FakeSource extends HealthDataSource {
  final Map<int, double> kcalByDay = {};
  final List<VitalsChangeBatch> changeBatches = [];
  String tokenToReturn = 'token-1';

  @override
  HealthConnectAvailability get cachedAvailability =>
      HealthConnectAvailability.available;

  @override
  Future<Set<String>> grantedPermissions() async =>
      {HcPermissions.readTotalCalories};

  @override
  Future<List<DailyNutrition>> readDailyNutrition(
    LocalDate startDate,
    LocalDate endDate, {
    bool includeHydration = true,
    bool includeEstimatedCalories = false,
  }) async =>
      [
        for (final e in kcalByDay.entries)
          if (e.key >= startDate.epochDay && e.key <= endDate.epochDay)
            DailyNutrition(
              date: LocalDate.fromEpochDay(e.key),
              hydrationLiters: 0.0,
              caloriesBurnedKcal: e.value,
            ),
      ];

  @override
  Future<String> getVitalsChangesToken(String recordType) async => tokenToReturn;

  @override
  Future<VitalsChangeBatch> getVitalsChanges(String token) async =>
      changeBatches.removeAt(0);
}

void main() {
  late OpenVitalsDatabase db;
  late VitalsDailyCacheDao dao;
  late _FakeSource source;

  final now = DateTime.utc(2026, 7, 20, 12);
  final today = LocalDate.fromDateTime(now);
  const metric = caloriesBurnedCacheMetric;

  CaloriesHistorySyncService service() =>
      CaloriesHistorySyncService(dao, source, clock: () => now);

  setUp(() {
    db = OpenVitalsDatabase(NativeDatabase.memory());
    dao = db.vitalsDailyCacheDao;
    source = _FakeSource();
  });

  tearDown(() => db.close());

  test('full sync (no cursor) stores each day total and a token', () async {
    source.kcalByDay[today.epochDay] = 2400;

    await service().syncAll();

    final rows =
        await dao.aggregatesBetween(metric, today.epochDay - 1, today.epochDay + 1);
    // The day value is a kcal SUM stored with sampleCount 1.
    expect(rows.single.valueSum, 2400);
    expect(rows.single.sampleCount, 1);
    expect((await dao.cursor(metric))!.changesToken, 'token-1');
  });

  test('a zero-burn day is not stored', () async {
    source.kcalByDay[today.epochDay] = 0;

    await service().syncAll();

    final rows =
        await dao.aggregatesBetween(metric, today.epochDay - 1, today.epochDay + 1);
    expect(rows, isEmpty);
  });

  test('incremental sync recomputes only the changed day and advances the token',
      () async {
    await dao.writeFullSync(metric, 'tok', 0);
    source.kcalByDay[today.epochDay] = 3100;
    source.changeBatches.add(VitalsChangeBatch(
      upsertedDays: [today],
      hasDeletions: false,
      nextToken: 'tok2',
      tokenExpired: false,
      hasMore: false,
    ));

    await service().syncAll();

    final rows = await dao.aggregatesBetween(metric, today.epochDay, today.epochDay);
    expect(rows.single.valueSum, 3100);
    expect((await dao.cursor(metric))!.changesToken, 'tok2');
  });

  test('a day that drops to zero is deleted on incremental sync', () async {
    await dao.writeFullSync(metric, 'tok', 0);
    await dao.upsertDay(
        metric: metric, epochDay: today.epochDay, valueSum: 2000, sampleCount: 1);
    // Source now reports nothing for today (kcalByDay empty).
    source.changeBatches.add(VitalsChangeBatch(
      upsertedDays: [today],
      hasDeletions: false,
      nextToken: 'tok2',
      tokenExpired: false,
      hasMore: false,
    ));

    await service().syncAll();

    final rows = await dao.aggregatesBetween(metric, today.epochDay, today.epochDay);
    expect(rows, isEmpty);
  });

  test('a deletion triggers a full rebuild from the current truth', () async {
    await dao.writeFullSync(metric, 'tok', 0);
    await dao.upsertDay(
        metric: metric, epochDay: today.epochDay, valueSum: 9999, sampleCount: 1);
    source.kcalByDay[today.epochDay] = 1500;
    source.changeBatches.add(VitalsChangeBatch(
      upsertedDays: const [],
      hasDeletions: true,
      nextToken: 'tokX',
      tokenExpired: false,
      hasMore: false,
    ));

    await service().syncAll();

    final rows = await dao.aggregatesBetween(metric, today.epochDay, today.epochDay);
    expect(rows.single.valueSum, 1500, reason: 'rebuilt from source, stale gone');
  });

  test('two buckets on the same day are summed, not a duplicate-key crash',
      () async {
    // Health Connect can hand back two 24h buckets for one local date across a
    // DST boundary; the full sync must fold them, not abort on the primary key.
    final dup = _DupSource()..dupDay = today.epochDay;

    await CaloriesHistorySyncService(dao, dup, clock: () => now).syncAll();

    final rows = await dao.aggregatesBetween(metric, today.epochDay, today.epochDay);
    expect(rows.single.valueSum, 1000 + 1500);
    expect((await dao.cursor(metric))!.changesToken, 'token-1');
  });

  test('no total-calories permission skips the sync entirely', () async {
    source.kcalByDay[today.epochDay] = 2400;
    final noPerm = _NoPermSource()..kcalByDay[today.epochDay] = 2400;

    await CaloriesHistorySyncService(dao, noPerm, clock: () => now).syncAll();

    expect(await dao.cursor(metric), isNull);
  });
}

class _NoPermSource extends _FakeSource {
  @override
  Future<Set<String>> grantedPermissions() async => const {};
}

/// Returns two nutrition entries for [dupDay], as Health Connect does across a
/// DST transition.
class _DupSource extends _FakeSource {
  late int dupDay;

  @override
  Future<List<DailyNutrition>> readDailyNutrition(
    LocalDate startDate,
    LocalDate endDate, {
    bool includeHydration = true,
    bool includeEstimatedCalories = false,
  }) async {
    final date = LocalDate.fromEpochDay(dupDay);
    if (dupDay < startDate.epochDay || dupDay > endDate.epochDay) return const [];
    return [
      DailyNutrition(date: date, hydrationLiters: 0, caloriesBurnedKcal: 1000),
      DailyNutrition(date: date, hydrationLiters: 0, caloriesBurnedKcal: 1500),
    ];
  }
}
