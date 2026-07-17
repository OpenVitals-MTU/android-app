import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/body_energy_timeline_cache_store.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/data/repository/contract/body_repository.dart';
import 'package:openvitals/data/repository/contract/body_energy_repository.dart';
import 'package:openvitals/data/repository/contract/health_repository.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/data/repository/contract/sleep_repository.dart';
import 'package:openvitals/data/repository/contract/vitals_repository.dart';
import 'package:openvitals/data/repository/impl/body_energy_repository_impl.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/model/vitals_models.dart';

/// Counts the baseline vs day reads so the tests can tell a recompute from a
/// cache hit and a baseline reuse from a baseline recompute.
class _FakeHeart implements HeartRepository {
  int dayGraphCalls = 0;
  int dailyRestingCalls = 0;

  @override
  Future<Result<List<HeartRateSample>>> loadRawHeartRateSamplesForDayGraph(
      LocalDate date) async {
    dayGraphCalls++;
    return Ok([
      HeartRateSample(
        time: DateTime(date.year, date.month, date.day, 9),
        beatsPerMinute: 70,
        source: 'test',
      ),
    ]);
  }

  @override
  Future<Result<List<HrvSample>>> loadHrvSamples(
          DateTime start, DateTime end) async =>
      const Ok([]);
  @override
  Future<Result<int?>> loadRestingHeartRate(LocalDate date) async =>
      const Ok(55);
  @override
  Future<Result<List<DailyRestingHR>>> loadDailyRestingHR(
      LocalDate start, LocalDate end) async {
    dailyRestingCalls++;
    return Ok([DailyRestingHR(date: end, bpm: 54)]);
  }

  @override
  Future<Result<List<DailyHrv>>> loadDailyHRV(
          LocalDate start, LocalDate end) async =>
      const Ok([]);
  @override
  Future<Result<List<HeartRateSample>>> loadHeartRateSamplesInstant(
          DateTime start, DateTime end) async =>
      const Ok([]);

  @override
  dynamic noSuchMethod(Invocation i) =>
      throw UnimplementedError('${i.memberName}');
}

/// Empty stubs for the sleep / activity / vitals / body collaborators — the
/// timeline algorithm tolerates no data (the tests exercise caching, not the
/// algorithm).
class _Empty
    implements
        SleepRepository,
        ActivityRepository,
        VitalsRepository,
        BodyRepository {
  @override
  Future<Result<List<SleepData>>> loadSleepSessions(
          LocalDate a, LocalDate b) async =>
      const Ok([]);
  @override
  Future<Result<List<ExerciseData>>> loadWorkouts(
          LocalDate a, LocalDate b) async =>
      const Ok([]);
  @override
  Future<Result<List<RespiratoryRateEntry>>> loadRespiratoryRate(
          LocalDate a, LocalDate b) async =>
      const Ok([]);
  @override
  Future<Result<List<ActivityProgressPoint>>> loadActivityProgress({
    LocalDate? date,
  }) async =>
      const Ok([]);
  @override
  Future<Result<double?>> loadLatestBMR() async => const Ok(null);
  @override
  dynamic noSuchMethod(Invocation i) =>
      throw UnimplementedError('${i.memberName}');
}

class _FakeHealth implements HealthRepository {
  @override
  HealthConnectAvailability availability() =>
      HealthConnectAvailability.available;
  @override
  Future<Result<Set<String>>> grantedPermissions() async =>
      const Ok({'read-heart-rate'});
  @override
  dynamic noSuchMethod(Invocation i) =>
      throw UnimplementedError('${i.memberName}');
}

void main() {
  late _FakeHeart heart;
  late PreferencesRepository prefs;
  late BodyEnergyTimelineCacheStore cache;
  late DateTime clock;

  final today = LocalDate.fromDateTime(DateTime(2026, 6, 1, 8));

  Future<void> setUpRepo() async {
    SharedPreferences.setMockInitialValues(const {});
    final sp = await SharedPreferences.getInstance();
    prefs = PreferencesRepository(sp);
    cache = BodyEnergyTimelineCacheStore(sp);
    heart = _FakeHeart();
    clock = DateTime(2026, 6, 1, 8);
  }

  BodyEnergyRepositoryImpl repo() {
    final empty = _Empty();
    return BodyEnergyRepositoryImpl(
      heartRepository: heart,
      sleepRepository: empty,
      activityRepository: empty,
      vitalsRepository: empty,
      bodyRepository: empty,
      healthRepository: _FakeHealth(),
      preferencesRepository: prefs,
      cacheStore: cache,
      now: () => clock,
    );
  }

  final query = BodyEnergyTimelineQuery(
    period: DatePeriod(today, today),
    range: TimeRange.day,
  );

  test('a fresh cached timeline is served without recomputing', () async {
    await setUpRepo();
    final r = repo();
    (await r.loadTimeline(query)).orThrow();
    expect(heart.dayGraphCalls, 1);

    // Same instant → within the 15-minute freshness window → cache hit.
    (await r.loadTimeline(query)).orThrow();
    expect(heart.dayGraphCalls, 1, reason: 'timeline should be served cached');
  });

  test('a stale timeline recomputes but reuses the fresh baseline', () async {
    await setUpRepo();
    final r = repo();
    (await r.loadTimeline(query)).orThrow();
    expect(heart.dayGraphCalls, 1);
    expect(heart.dailyRestingCalls, 1);

    // 20 minutes later: today's timeline is stale (>=15 min) so it recomputes,
    // but the baseline is still fresh (<24 h) and must be reused.
    clock = clock.add(const Duration(minutes: 20));
    (await r.loadTimeline(query)).orThrow();
    expect(heart.dayGraphCalls, 2, reason: 'stale timeline recomputes');
    expect(heart.dailyRestingCalls, 1, reason: 'baseline reused, not recomputed');
  });

  test('a forced refresh recomputes even within the freshness window',
      () async {
    await setUpRepo();
    final r = repo();
    (await r.loadTimeline(query)).orThrow();
    expect(heart.dayGraphCalls, 1);

    (await r.loadTimeline(BodyEnergyTimelineQuery(
      period: DatePeriod(today, today),
      range: TimeRange.day,
      refreshMode: RefreshMode.force,
    )))
        .orThrow();
    expect(heart.dayGraphCalls, 2, reason: 'force bypasses the cache');
  });
}
