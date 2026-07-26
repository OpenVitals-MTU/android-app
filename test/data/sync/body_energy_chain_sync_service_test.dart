import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/body_energy_baseline_cache_store.dart';
import 'package:openvitals/data/repository/body_energy_timeline_store.dart';
import 'package:openvitals/data/repository/contract/body_energy_repository.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/data/sync/body_energy_chain_sync_service.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';

class _FakeSource extends HealthDataSource {
  Set<String> granted = {HcPermissions.readHeartRate};

  @override
  HealthConnectAvailability get cachedAvailability =>
      HealthConnectAvailability.available;

  @override
  Future<Set<String>> grantedPermissions() async => granted;

  @override
  dynamic noSuchMethod(Invocation i) =>
      throw UnimplementedError('${i.memberName}');
}

/// Records which days it was asked for, and persists each one so the service's
/// "already stored and fresh" skip is exercised for real.
class _RecordingRepository implements BodyEnergyRepository {
  _RecordingRepository(this._store, this._now);

  final BodyEnergyTimelineStore _store;
  final DateTime Function() _now;
  final List<LocalDate> requested = [];
  bool throwOnLoad = false;

  @override
  Future<Result<BodyEnergyTimelineResult>> loadTimeline(
    BodyEnergyTimelineQuery query,
  ) async {
    if (throwOnLoad) throw StateError('health connect exploded');
    final date = query.period.start;
    requested.add(date);

    // Chain the stored predecessor, exactly as the real repository does, so a
    // test can assert the walk really produced a connected chain.
    final previous = await _store.storedDaysBetween(
      date.minusDays(1),
      date.minusDays(1),
    );
    final seed = previous.isEmpty ? null : previous.single.endScore;
    final start = bodyEnergySeedScore(seed);
    final timeline = BodyEnergyTimeline(
      date: date,
      startScore: start,
      currentScore: (start - 7).clamp(0, 100),
      charged: 0,
      drained: 7,
      points: const [],
      confidence: BodyEnergyConfidence.high,
      confidenceReason: 'test',
      inputSummary: BodyEnergyInputSummary(previousEndScore: seed),
      generatedAt: _now(),
      signature: 'v$bodyEnergyTimelineAlgorithmVersion|test|0',
    );
    await _store.save(timeline);
    return Ok(BodyEnergyTimelineResult(query: query, days: [timeline]));
  }

  @override
  dynamic noSuchMethod(Invocation i) =>
      throw UnimplementedError('${i.memberName}');
}

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  // Mid-morning, so the tests that advance the clock by a couple of hours stay
  // on the same calendar day and the warm window does not shift under them.
  var now = DateTime(2026, 6, 1, 10);
  final today = LocalDate.fromDateTime(now);

  late OpenVitalsDatabase db;
  late BodyEnergyTimelineStore store;
  late BodyEnergyBaselineCacheStore baselines;
  late PreferencesRepository prefs;
  late SharedPreferences sp;
  late _FakeSource source;
  late _RecordingRepository repository;

  Future<BodyEnergyChainSyncService> newService({int windowDays = 5}) async {
    return BodyEnergyChainSyncService(
      repository,
      store,
      baselines,
      source,
      prefs,
      clock: () => now,
      windowDays: windowDays,
    );
  }

  setUp(() async {
    now = DateTime(2026, 6, 1, 10);
    SharedPreferences.setMockInitialValues(const {});
    sp = await SharedPreferences.getInstance();
    prefs = PreferencesRepository(sp);
    baselines = BodyEnergyBaselineCacheStore(sp);
    db = OpenVitalsDatabase(NativeDatabase.memory());
    addTearDown(db.close);
    store = BodyEnergyTimelineStore(db.bodyEnergyTimelineDao);
    source = _FakeSource();
    repository = _RecordingRepository(store, () => now);
  });

  test('a cold window is walked oldest first, and today is left alone',
      () async {
    final service = await newService();

    await service.syncAll();

    // Order is load-bearing: a day's seed must already be stored when its
    // successor is computed.
    expect(repository.requested, [
      today.minusDays(4),
      today.minusDays(3),
      today.minusDays(2),
      today.minusDays(1),
    ]);
    expect(repository.requested, isNot(contains(today)),
        reason: 'the foreground load owns today');
  });

  test('the walked days form a connected chain', () async {
    await (await newService()).syncAll();

    final days = await store.storedDaysBetween(
      today.minusDays(4),
      today.minusDays(1),
    );
    expect(days, hasLength(4));
    for (var i = 1; i < days.length; i++) {
      expect(days[i].startScore, bodyEnergySeedScore(days[i - 1].endScore));
    }
  });

  test('a second pass inside the throttle window does no work', () async {
    final service = await newService();
    await service.syncAll();
    final firstPass = repository.requested.length;

    now = now.add(const Duration(minutes: 5));
    await service.syncAll();

    expect(repository.requested, hasLength(firstPass),
        reason: 'every screen open calls syncAll; it must not re-walk');
  });

  test('past the throttle, already-stored fresh days are still skipped',
      () async {
    final service = await newService();
    await service.syncAll();
    repository.requested.clear();

    now = now.add(const Duration(hours: 2));
    await service.syncAll();

    expect(repository.requested, isEmpty,
        reason: 'stored days under 24h old are fresh and cost nothing');
  });

  test('a changed calibration purges the chain rather than ageing it out',
      () async {
    final service = await newService();
    await service.syncAll();
    expect(await db.bodyEnergyTimelineDao.countDays(), 4);

    // Rows computed under retired ZONES are wrong, not merely stale: the zones
    // decide what every bucket meant. This used to be triggered with a learned
    // gain instead, which made the purge fire on essentially every watch sync
    // and take the whole bucket history with it — see the gain case below.
    prefs.setBodyEnergyCalibration(
      const BodyEnergyCalibration(
        useManualZones: true,
        manualZoneThresholdsBpm: HeartZoneThresholds(
          zone1LowerBpm: 95,
          zone2LowerBpm: 115,
          zone3LowerBpm: 135,
          zone4LowerBpm: 155,
          zone5LowerBpm: 175,
        ),
      ),
    );
    now = now.add(const Duration(hours: 2));
    repository.requested.clear();
    await service.syncAll();

    expect(repository.requested, hasLength(4),
        reason: 'the purge must force a full rebuild');
  });

  test('without the heart-rate permission it does nothing', () async {
    source.granted = {};

    await (await newService()).syncAll();

    expect(repository.requested, isEmpty);
    expect(await db.bodyEnergyTimelineDao.countDays(), 0);
  });

  test('concurrent calls share a single run', () async {
    final service = await newService();

    await Future.wait([service.syncAll(), service.syncAll()]);

    expect(repository.requested, hasLength(4),
        reason: 'two callers must not walk the window twice');
  });

  test('a throwing repository is swallowed, not surfaced', () async {
    repository.throwOnLoad = true;
    final service = await newService();

    await expectLater(service.syncAll(), completes);
  });

  test('the legacy prefs timelines are purged on the first pass', () async {
    await sp.setString('2026-05-30|-12345', 'a retired encoded timeline');

    await (await newService()).syncAll();

    expect(sp.getKeys(), isNot(contains('2026-05-30|-12345')));
  });

  test('retention drops old buckets but keeps their day summaries', () async {
    final service = await newService();
    // Establish the global signature first: the very first pass has no stored
    // signature and so purges, which would take the fixture with it.
    await service.syncAll();

    // A day well outside the retention window, with buckets.
    final ancient = today.minusDays(bodyEnergyBucketRetentionDays + 10);
    await store.save(BodyEnergyTimeline(
      date: ancient,
      startScore: 50,
      currentScore: 40,
      charged: 0,
      drained: 10,
      points: [
        BodyEnergyTimelinePoint(
          time: DateTime.utc(ancient.year, ancient.month, ancient.day),
          score: 50,
          delta: 0.0,
          state: BodyEnergyBucketState.rest,
          confidence: BodyEnergyConfidence.high,
        ),
      ],
      confidence: BodyEnergyConfidence.high,
      confidenceReason: 'test',
      generatedAt: now,
      signature: 'v$bodyEnergyTimelineAlgorithmVersion|test|0',
    ));
    expect(await db.bodyEnergyTimelineDao.bucketsForDay(ancient.epochDay),
        hasLength(1));

    now = now.add(const Duration(hours: 2));
    await service.syncAll();

    expect(await db.bodyEnergyTimelineDao.bucketsForDay(ancient.epochDay),
        isEmpty);
    expect(await db.bodyEnergyTimelineDao.day(ancient.epochDay), isNotNull,
        reason: 'the chain must stay walkable past the bucket window');
  });

  test('a gain the watch learner nudged does not purge the stored history',
      () async {
    // The global signature gates a purgeAll() of every day AND every bucket.
    // With the learned gains folded into it, each observation the watch fit
    // absorbed wiped up to bodyEnergyBucketRetentionDays of history — deleting
    // the very buckets the weekly view is built on, on essentially every sync.
    final service = await newService();
    await service.syncAll();
    final storedBefore = await store.storedDaysBetween(
        today.minusDays(bodyEnergyBucketRetentionDays), today);
    expect(storedBefore, isNotEmpty);

    prefs.setBodyEnergyCalibration(
      prefs.bodyEnergyCalibrationListenable.value
          .copyWith(stressDrainGain: 1.04),
    );
    now = now.add(const Duration(hours: 2));
    await (await newService()).syncAll();

    expect(
      await store.storedDaysBetween(
          today.minusDays(bodyEnergyBucketRetentionDays), today),
      hasLength(greaterThanOrEqualTo(storedBefore.length)),
      reason: 'a sub-percent gain nudge must not destroy the chain',
    );
  });

  test('a later pass skips settled days and revisits only unsettled ones',
      () async {
    // The window has to reach past the settling horizon for this to bite, so
    // it is wider here than the other cases use.
    final service = await newService(windowDays: 12);
    await service.syncAll();
    expect(repository.requested, hasLength(11));
    repository.requested.clear();

    // A day later every stored day is over 24h old, which before the settling
    // window meant the whole 14-day walk ran again -- ~104 Health Connect reads
    // for days that cannot have gained anything.
    now = now.add(const Duration(hours: 25));
    final shiftedToday = LocalDate.fromDateTime(now);
    await service.syncAll();

    final settled = [
      for (var back = 8; back <= 11; back++) shiftedToday.minusDays(back),
    ];
    final unsettled = [
      for (var back = 1; back <= bodyEnergyChainSettlingDays; back++)
        shiftedToday.minusDays(back),
    ];
    for (final date in settled) {
      expect(repository.requested, isNot(contains(date)),
          reason: '$date is settled and was already stored');
    }
    expect(repository.requested, containsAll(unsettled),
        reason: 'days that can still gain late data must be revisited');
  });

  group('a forced pass', () {
    /// The state a watch sync leaves behind: the days it back-filled dropped
    /// from the chain, so there is real work for the next pass to find.
    Future<void> invalidateAsAGarminSyncWould() =>
        store.invalidateForward(today.minusDays(2), today);

    test('bypasses the throttle, so a watch sync is acted on immediately',
        () async {
      final service = await newService();
      await service.syncAll();
      await invalidateAsAGarminSyncWould();
      repository.requested.clear();

      now = now.add(const Duration(minutes: 2));
      await service.syncAll(force: true);

      expect(repository.requested, [today.minusDays(2), today.minusDays(1)],
          reason: 'oldest first, and only the days that went missing');
      expect(await db.bodyEnergyTimelineDao.countDays(), 4);
    });

    test('an unforced call inside the throttle leaves the holes alone',
        () async {
      // The counterpart: without force the same invalidation waits out the
      // 30-minute window, which is why the port passes force.
      final service = await newService();
      await service.syncAll();
      await invalidateAsAGarminSyncWould();
      repository.requested.clear();

      now = now.add(const Duration(minutes: 2));
      await service.syncAll();

      expect(repository.requested, isEmpty);
      expect(await db.bodyEnergyTimelineDao.countDays(), 2);
    });

    test('force does not override the freshness skip', () async {
      // Force is about the throttle only. A day already stored and fresh is
      // still skipped, or every sync would re-read the whole window.
      final service = await newService();
      await service.syncAll();
      repository.requested.clear();

      now = now.add(const Duration(minutes: 2));
      await service.syncAll(force: true);

      expect(repository.requested, isEmpty);
    });
  });

}
