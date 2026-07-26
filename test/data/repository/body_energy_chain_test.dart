import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'package:openvitals/core/period/time_range.dart';
import 'package:openvitals/core/result/result.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';
import 'package:openvitals/data/prefs/preferences_repository.dart';
import 'package:openvitals/data/repository/body_energy_baseline_cache_store.dart';
import 'package:openvitals/data/repository/body_energy_timeline_store.dart';
import 'package:openvitals/data/repository/contract/activity_repository.dart';
import 'package:openvitals/data/repository/contract/body_energy_repository.dart';
import 'package:openvitals/data/repository/contract/body_repository.dart';
import 'package:openvitals/data/repository/contract/health_repository.dart';
import 'package:openvitals/data/repository/contract/heart_repository.dart';
import 'package:openvitals/data/repository/contract/sleep_repository.dart';
import 'package:openvitals/data/repository/contract/vitals_repository.dart';
import 'package:openvitals/data/repository/impl/body_energy_repository_impl.dart';
import 'package:openvitals/domain/insights/body_energy_calibration_fit.dart';
import 'package:openvitals/domain/insights/body_energy_timeline.dart';
import 'package:openvitals/domain/preferences/body_energy_calibration.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/heart_models.dart';
import 'package:openvitals/domain/model/refresh_mode.dart';
import 'package:openvitals/domain/model/sleep_models.dart';
import 'package:openvitals/domain/model/vitals_models.dart';

/// A heart repository that reports a steady waking heart rate all day, so
/// every day drains a predictable amount and the chain is observable.
///
/// [dayGraphCalls] is what tells a stored-chain read (no Health Connect work)
/// apart from a recompute.
class _FakeHeart implements HeartRepository {
  int dayGraphCalls = 0;
  final List<LocalDate> daysRead = [];

  @override
  Future<Result<List<HeartRateSample>>> loadRawHeartRateSamplesForDayGraph(
      LocalDate date) async {
    dayGraphCalls++;
    daysRead.add(date);
    return Ok([
      for (var hour = 7; hour < 22; hour++)
        HeartRateSample(
          time: DateTime(date.year, date.month, date.day, hour),
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
          LocalDate start, LocalDate end) async =>
      Ok([DailyRestingHR(date: end, bpm: 54)]);
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

/// A heart repository reporting a much higher waking heart rate, so the same
/// day drains further and ends on a different score — with real points, unlike
/// [_SilentHeart].
class _ElevatedHeart extends _FakeHeart {
  @override
  Future<Result<List<HeartRateSample>>> loadRawHeartRateSamplesForDayGraph(
      LocalDate date) async {
    dayGraphCalls++;
    daysRead.add(date);
    return Ok([
      for (var hour = 7; hour < 22; hour++)
        HeartRateSample(
          time: DateTime(date.year, date.month, date.day, hour),
          beatsPerMinute: 115,
          source: 'test',
        ),
    ]);
  }
}

/// A heart repository with no data at all — drives the `empty()` path.
class _SilentHeart extends _FakeHeart {
  @override
  Future<Result<List<HeartRateSample>>> loadRawHeartRateSamplesForDayGraph(
      LocalDate date) async {
    dayGraphCalls++;
    daysRead.add(date);
    return const Ok([]);
  }
}

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
  TestWidgetsFlutterBinding.ensureInitialized();

  // "Now" is late on 1 June, so 1 June is today and everything before it is a
  // completed past day.
  final now = DateTime(2026, 6, 1, 22);
  final today = LocalDate.fromDateTime(now);

  late _FakeHeart heart;
  late PreferencesRepository prefs;
  late SharedPreferences sp;
  late OpenVitalsDatabase db;
  late BodyEnergyTimelineStore timelines;
  late BodyEnergyBaselineCacheStore baselines;

  Future<void> setUpChain({_FakeHeart? heartOverride}) async {
    SharedPreferences.setMockInitialValues(const {});
    sp = await SharedPreferences.getInstance();
    prefs = PreferencesRepository(sp);
    baselines = BodyEnergyBaselineCacheStore(sp);
    db = OpenVitalsDatabase(NativeDatabase.memory());
    addTearDown(db.close);
    timelines = BodyEnergyTimelineStore(db.bodyEnergyTimelineDao);
    heart = heartOverride ?? _FakeHeart();
  }

  BodyEnergyRepositoryImpl repo({bool withStore = true}) {
    final empty = _Empty();
    return BodyEnergyRepositoryImpl(
      heartRepository: heart,
      sleepRepository: empty,
      activityRepository: empty,
      vitalsRepository: empty,
      bodyRepository: empty,
      healthRepository: _FakeHealth(),
      preferencesRepository: prefs,
      baselineCacheStore: baselines,
      timelineStore: withStore ? timelines : null,
      now: () => now,
    );
  }

  Future<BodyEnergyTimeline> load(
    BodyEnergyRepositoryImpl r,
    LocalDate date, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final result = (await r.loadTimeline(BodyEnergyTimelineQuery(
      period: DatePeriod(date, date),
      range: TimeRange.day,
      refreshMode: refreshMode,
    )))
        .orThrow();
    return result.days.single;
  }

  /// Stores a day directly, as the warm service or an earlier open would have.
  /// Signature is taken from a real computation of that day so it validates.
  Future<int> seedStoredDay(
    BodyEnergyRepositoryImpl r,
    LocalDate date,
  ) async {
    final day = await load(r, date);
    return day.currentScore;
  }

  group('continuity across midnight', () {
    test('a day opens where the stored previous day closed', () async {
      await setUpChain();
      final r = repo();

      final yesterdayEnd = await seedStoredDay(r, today.minusDays(1));
      expect(yesterdayEnd, lessThan(50),
          reason: 'a waking day with no sleep should drain');

      final day = await load(r, today);

      expect(day.startScore, bodyEnergySeedScore(yesterdayEnd),
          reason: 'the day must carry over, not reset to 50');
      expect(day.startScore, isNot(bodyEnergyNeutralStartScore));
      expect(day.inputSummary.previousEndScore, yesterdayEnd);
      expect(day.inputSummary.seedSource, BodyEnergySeedSource.carriedOver);
    });

    test('the warm path costs no Health Connect read', () async {
      await setUpChain();
      final r = repo();
      await seedStoredDay(r, today.minusDays(1));
      final callsAfterSeed = heart.dayGraphCalls;

      await load(r, today);

      expect(heart.dayGraphCalls, callsAfterSeed + 1,
          reason: 'only today is read; the seed comes from SQLite');
    });

    test('a cold chain starts neutral, because there is nothing to carry',
        () async {
      await setUpChain();

      final day = await load(repo(), today);

      expect(day.startScore, bodyEnergyNeutralStartScore);
      expect(day.inputSummary.seedSource, BodyEnergySeedSource.neutral);
      expect(day.inputSummary.previousEndScore, isNull);
    });

    test('a multi-day query threads each day into the next', () async {
      await setUpChain();
      final r = repo();

      final result = (await r.loadTimeline(BodyEnergyTimelineQuery(
        period: DatePeriod(today.minusDays(3), today),
        range: TimeRange.day,
      )))
          .orThrow();

      for (var i = 1; i < result.days.length; i++) {
        // Through the floor: these fixture days never sleep, so the chain
        // bottoms out and the carry-over floor is what stops it staying there.
        expect(
          result.days[i].startScore,
          bodyEnergySeedScore(result.days[i - 1].currentScore),
          reason: 'day $i must open where day ${i - 1} closed',
        );
        expect(result.days[i].inputSummary.previousEndScore,
            result.days[i - 1].currentScore);
      }
    });
  });

  group('the gap fill', () {
    test('a one-day gap is closed and the filled day is persisted', () async {
      await setUpChain();
      final r = repo();
      await seedStoredDay(r, today.minusDays(2));

      final day = await load(r, today);

      expect(day.inputSummary.seedSource, BodyEnergySeedSource.carriedOver);
      expect(heart.daysRead, contains(today.minusDays(1)),
          reason: 'the missing day must be computed to close the chain');

      final stored = await timelines.storedDaysBetween(
        today.minusDays(1),
        today.minusDays(1),
      );
      expect(stored, hasLength(1),
          reason: 'a filled gap day must be stored so the next open is warm');
      expect(day.inputSummary.previousEndScore, stored.single.endScore);
      expect(day.startScore, bodyEnergySeedScore(stored.single.endScore));
    });

    test('a gap wider than the foreground bound is reported, not walked',
        () async {
      await setUpChain();
      final r = repo();
      await seedStoredDay(r, today.minusDays(5));
      final callsAfterSeed = heart.dayGraphCalls;

      final day = await load(r, today);

      expect(day.inputSummary.seedSource, BodyEnergySeedSource.chainGap);
      expect(day.startScore, bodyEnergyNeutralStartScore);
      expect(heart.dayGraphCalls, callsAfterSeed + 1,
          reason: 'a 4-day gap must not trigger four background-sized reads');
    });
  });

  group('the per-day signature', () {
    test('a stored day under a foreign signature is not used as an anchor',
        () async {
      await setUpChain();
      final r = repo();
      final yesterday = today.minusDays(1);
      await seedStoredDay(r, yesterday);

      // Rewrite yesterday's row with a signature from a different world. The
      // regression this guards: validating a predecessor against the REQUESTED
      // day's signature instead of its own, which the body profile's
      // date-dependent signature made silently wrong across a birthday.
      final stored = (await timelines.storedDaysBetween(yesterday, yesterday))
          .single;
      final cached = await timelines.load(yesterday, stored.signature);
      await timelines
          .save(cached!.copyWith(signature: 'v5|not-this-calibration|0'));

      final day = await load(r, today);

      expect(day.inputSummary.seedSource, BodyEnergySeedSource.neutral,
          reason: 'a row from another calibration must not seed the chain');
    });

    test('a gain the watch learner nudged still seeds the next day', () async {
      // The learner moves the gains by a fraction of a percent per observation,
      // and they used to be part of the day signature. So one watch reading
      // invalidated all fourteen stored days at once, the next load found no
      // valid predecessor, and the day opened on the neutral 50 with yesterday
      // sitting at 0. Seen on device 2026-07-26: gains sleep/activity/basal all
      // 1.00, stress 1.04 — a single fit — and today started at 50.
      await setUpChain();
      final r = repo();
      final yesterday = today.minusDays(1);
      final yesterdayEnd = await seedStoredDay(r, yesterday);

      prefs.setBodyEnergyCalibration(
        prefs.bodyEnergyCalibrationListenable.value
            .copyWith(stressDrainGain: 1.04),
      );

      final day = await load(repo(), today);

      expect(day.inputSummary.seedSource, BodyEnergySeedSource.carriedOver);
      expect(day.startScore, bodyEnergySeedScore(yesterdayEnd));
    });

    test('but editing the heart zones does break the chain', () async {
      // The other half of the split. Zones change what a bucket MEANS, and only
      // ever because someone edited a setting, so a reset there is honest.
      await setUpChain();
      final r = repo();
      await seedStoredDay(r, today.minusDays(1));

      prefs.setBodyEnergyCalibration(
        prefs.bodyEnergyCalibrationListenable.value.copyWith(
          useManualZones: true,
          manualZoneThresholdsBpm: const HeartZoneThresholds(
            zone1LowerBpm: 95,
            zone2LowerBpm: 115,
            zone3LowerBpm: 135,
            zone4LowerBpm: 155,
            zone5LowerBpm: 175,
          ),
        ),
      );

      final day = await load(repo(), today);

      expect(day.inputSummary.seedSource, BodyEnergySeedSource.neutral);
    });
  });

  group('the forward ripple', () {
    test('recomputing a past day drops the days that followed it', () async {
      await setUpChain();
      final r = repo();
      for (var back = 3; back >= 1; back--) {
        await seedStoredDay(r, today.minusDays(back));
      }
      expect(
        (await timelines.storedDaysBetween(today.minusDays(3), today)).length,
        3,
      );

      // Force a recompute of the oldest day with a heart rate that changes its
      // end score, so the days after it are now built on a dead seed.
      heart.daysRead.clear();
      final target = today.minusDays(3);
      final before =
          (await timelines.storedDaysBetween(target, target)).single.endScore;
      // Swap in a harder day: a much higher heart rate drains further, so the
      // end score moves. Deliberately NOT a data-less day — that is the case
      // the empty-recompute guard protects, so it would keep the stored day and
      // the score would not move at all.
      final harder = _ElevatedHeart();
      final r2 = BodyEnergyRepositoryImpl(
        heartRepository: harder,
        sleepRepository: _Empty(),
        activityRepository: _Empty(),
        vitalsRepository: _Empty(),
        bodyRepository: _Empty(),
        healthRepository: _FakeHealth(),
        preferencesRepository: prefs,
        baselineCacheStore: baselines,
        timelineStore: timelines,
        now: () => now,
      );
      final recomputed =
          await load(r2, target, refreshMode: RefreshMode.force);
      expect(recomputed.currentScore, isNot(before),
          reason: 'the test needs the end score to actually move');

      final remaining =
          await timelines.storedDaysBetween(today.minusDays(2), today);
      expect(remaining, isEmpty,
          reason: 'days built on the old seed must be invalidated');
    });

    test('a recompute landing on the same score keeps the chain intact',
        () async {
      await setUpChain();
      final r = repo();
      for (var back = 3; back >= 1; back--) {
        await seedStoredDay(r, today.minusDays(back));
      }

      // Same inputs, so the same end score — nothing downstream changed.
      await load(r, today.minusDays(3), refreshMode: RefreshMode.force);

      final remaining =
          await timelines.storedDaysBetween(today.minusDays(2), today);
      expect(remaining, hasLength(2),
          reason: 'a no-op recompute must not wipe the stored chain');
    });
  });

  group('a day with no data', () {
    test('passes the seed through instead of resetting to 50', () async {
      await setUpChain();
      // Yesterday has data and drains; today has none at all.
      final r = repo();
      final yesterdayEnd = await seedStoredDay(r, today.minusDays(1));

      final silent = _SilentHeart();
      final r2 = BodyEnergyRepositoryImpl(
        heartRepository: silent,
        sleepRepository: _Empty(),
        activityRepository: _Empty(),
        vitalsRepository: _Empty(),
        bodyRepository: _Empty(),
        healthRepository: _FakeHealth(),
        preferencesRepository: prefs,
        baselineCacheStore: baselines,
        timelineStore: timelines,
        now: () => now,
      );
      final day = await load(r2, today);

      expect(day.confidence, BodyEnergyConfidence.noData);
      expect(day.startScore, yesterdayEnd);
      expect(day.currentScore, yesterdayEnd,
          reason: 'a day we know nothing about must not reset the chain');
    });
  });

  group('without a drift store (the alarm isolate)', () {
    test('falls back to the prefs seed mirror for the previous day', () async {
      await setUpChain();
      // The foreground writes the mirror as it computes past days.
      await seedStoredDay(repo(), today.minusDays(1));
      final mirrored = prefs.bodyEnergyChainSeedMirror;
      expect(mirrored, isNotNull,
          reason: 'computing a past day must mirror its end score');

      final day = await load(repo(withStore: false), today);

      expect(day.inputSummary.seedSource, BodyEnergySeedSource.carriedOver);
      expect(
        '${today.minusDays(1).epochDay}|${day.inputSummary.previousEndScore}',
        mirrored,
      );
    });

    test('ignores a mirror that is not for the immediately previous day',
        () async {
      await setUpChain();
      prefs.bodyEnergyChainSeedMirror =
          '${today.minusDays(3).epochDay}|40';

      final day = await load(repo(withStore: false), today);

      expect(day.inputSummary.seedSource, BodyEnergySeedSource.neutral);
      expect(day.startScore, bodyEnergyNeutralStartScore);
    });

    test('the mirror only moves forward, so a backfill cannot rewind it',
        () async {
      await setUpChain();
      final r = repo();
      await seedStoredDay(r, today.minusDays(1));
      final afterYesterday = prefs.bodyEnergyChainSeedMirror;

      await seedStoredDay(r, today.minusDays(4));

      expect(prefs.bodyEnergyChainSeedMirror, afterYesterday,
          reason: 'an older backfilled day must not overwrite the mirror');
    });
  });

  group('settled days are served, not recomputed', () {
    // The bucket table was write-only before this: retention kept 120 days but
    // every past day older than 24h was treated as stale, so nothing ever read
    // a bucket back. These pin the three tiers.

    /// Ages the stored copy of [date] by rewriting its generatedAt.
    Future<void> ageStoredDay(LocalDate date, Duration by) async {
      final stored = (await timelines.storedDaysBetween(date, date)).single;
      final cached = await timelines.load(date, stored.signature);
      await timelines.save(
        cached!.copyWith(generatedAt: now.subtract(by)),
      );
    }

    test('a settled day is served from storage with no Health Connect read',
        () async {
      await setUpChain();
      final r = repo();
      final settled = today.minusDays(bodyEnergyChainSettlingDays + 3);
      await seedStoredDay(r, settled);
      await ageStoredDay(settled, const Duration(days: 30));
      final callsBefore = heart.dayGraphCalls;

      final day = await load(r, settled);

      expect(heart.dayGraphCalls, callsBefore,
          reason: 'a settled day must cost no Health Connect read at all');
      expect(day.points, isNotEmpty);
    });

    test('a day still inside the settling window recomputes once it ages',
        () async {
      await setUpChain();
      final r = repo();
      final recent = today.minusDays(2);
      await seedStoredDay(r, recent);
      await ageStoredDay(recent, const Duration(hours: 25));
      final callsBefore = heart.dayGraphCalls;

      await load(r, recent);

      expect(heart.dayGraphCalls, callsBefore + 1,
          reason: 'late watch data can still land on a recent day');
    });

    test('today still follows the 15-minute rule', () async {
      await setUpChain();
      final r = repo();
      await load(r, today);
      final callsBefore = heart.dayGraphCalls;

      await load(r, today);
      expect(heart.dayGraphCalls, callsBefore,
          reason: 'within the window today is served cached');
    });

    test('a forced refresh still recomputes a settled day', () async {
      await setUpChain();
      final r = repo();
      final settled = today.minusDays(bodyEnergyChainSettlingDays + 3);
      await seedStoredDay(r, settled);
      await ageStoredDay(settled, const Duration(days: 30));
      final callsBefore = heart.dayGraphCalls;

      await load(r, settled, refreshMode: RefreshMode.force);

      expect(heart.dayGraphCalls, callsBefore + 1,
          reason: 'pull-to-refresh must always reach Health Connect');
    });

    test('a signature change still rebuilds a settled day', () async {
      // "Never stale" must not be read as "never updated": a calibration edit
      // has to reach even a day the settling window would otherwise freeze.
      await setUpChain();
      final r = repo();
      final settled = today.minusDays(bodyEnergyChainSettlingDays + 3);
      await seedStoredDay(r, settled);
      final stored = (await timelines.storedDaysBetween(settled, settled)).single;
      final cached = await timelines.load(settled, stored.signature);
      await timelines.save(cached!.copyWith(signature: 'v6|other-calibration|0'));
      final callsBefore = heart.dayGraphCalls;

      await load(r, settled);

      expect(heart.dayGraphCalls, callsBefore + 1);
    });

    test('a day whose buckets retention purged is recomputed, not served blank',
        () async {
      await setUpChain();
      final r = repo();
      final ancient = today.minusDays(bodyEnergyChainSettlingDays + 5);
      await seedStoredDay(r, ancient);
      // Retention keeps the summary and drops the buckets; serving that would
      // put a real headline score above an empty chart.
      await db.bodyEnergyTimelineDao
          .purgeBucketsBefore(ancient.epochDay + 1);
      final callsBefore = heart.dayGraphCalls;

      final day = await load(r, ancient);

      expect(heart.dayGraphCalls, callsBefore + 1);
      expect(day.points, isNotEmpty);
    });
  });

  group('an empty recompute never destroys a stored day', () {
    // Without the history grant Health Connect serves only ~30 days, so an old
    // day can recompute to nothing purely because its data is out of reach --
    // and save() deletes that day's buckets before writing.
    BodyEnergyRepositoryImpl silentRepo() => BodyEnergyRepositoryImpl(
          heartRepository: _SilentHeart(),
          sleepRepository: _Empty(),
          activityRepository: _Empty(),
          vitalsRepository: _Empty(),
          bodyRepository: _Empty(),
          healthRepository: _FakeHealth(),
          preferencesRepository: prefs,
          baselineCacheStore: baselines,
          timelineStore: timelines,
          now: () => now,
        );

    test('the stored timeline survives and is returned', () async {
      await setUpChain();
      final target = today.minusDays(2);
      await seedStoredDay(repo(), target);
      final storedBefore =
          (await timelines.storedDaysBetween(target, target)).single;
      final bucketsBefore =
          await db.bodyEnergyTimelineDao.countBucketsForDay(target.epochDay);
      expect(bucketsBefore, greaterThan(0));

      final day = await load(silentRepo(), target, refreshMode: RefreshMode.force);

      expect(day.points, isNotEmpty,
          reason: 'the caller must get the day we still have, not the blank');
      expect(day.currentScore, storedBefore.endScore);
      expect(
        await db.bodyEnergyTimelineDao.countBucketsForDay(target.epochDay),
        bucketsBefore,
      );
    });

    test('the days after it are not rippled away', () async {
      await setUpChain();
      final r = repo();
      for (var back = 3; back >= 1; back--) {
        await seedStoredDay(r, today.minusDays(back));
      }

      await load(silentRepo(), today.minusDays(3),
          refreshMode: RefreshMode.force);

      expect(
        await timelines.storedDaysBetween(today.minusDays(2), today.minusDays(1)),
        hasLength(2),
        reason: 'nothing changed upstream, so nothing downstream is invalid',
      );
    });

    test('a genuinely data-less day with nothing stored is still recorded',
        () async {
      // The guard protects existing buckets; it must not stop a first, honest
      // "we know nothing about this day" from being written.
      await setUpChain();
      final target = today.minusDays(2);

      await load(silentRepo(), target);

      final stored = await timelines.storedDaysBetween(target, target);
      expect(stored, hasLength(1));
      expect(stored.single.endScore, bodyEnergyNeutralStartScore);
    });
  });

  group('the algorithm-change gain reset', () {
    // It used to live in the chain sync service, which is only kicked by the
    // Body Energy screen and a watch sync — so the dashboard, the widgets and
    // the diagnostics all reached the model without it and the reset silently
    // never ran. On a real device that left sleepChargeGain at 0.80,
    // suppressing exactly the charge the new algorithm had just added.
    test('runs on any load, not only when the chain sync happens to fire',
        () async {
      await setUpChain();
      prefs.setBodyEnergyCalibration(
        const BodyEnergyCalibration(
          sleepChargeGain: 0.8,
          activityDrainGain: 1.1,
          basalDrainGain: 0.72,
          feelCheckCount: 2,
          watchObservationCount: 39,
        ),
      );

      // A plain load — nothing else touched.
      await load(repo(), today);

      final after = prefs.bodyEnergyCalibrationListenable.value;
      expect(after.sleepChargeGain, 1.0);
      expect(after.activityDrainGain, 1.0);
      expect(after.basalDrainGain, 1.0);
      expect(prefs.bodyEnergyGainsAlgorithmVersion,
          bodyEnergyTimelineAlgorithmVersion);
    });

    test('rewinds the watch fit watermark so the gains can actually relearn',
        () async {
      // The reset without this is a trap: it tells the model to relearn from
      // 1.0 while the watermark still says every stored watch sample has been
      // consumed, so the only evidence left is whatever syncs afterwards.
      // Measured on device after the v10 bump -- 5472 samples in drift, ~100
      // pairable hours, and a watch observation count of 2.
      await setUpChain();
      prefs.bodyEnergyWatchFitWatermarkMillis =
          today.atStartOfDayUtc().millisecondsSinceEpoch;

      await load(repo(), today);

      expect(prefs.bodyEnergyWatchFitWatermarkMillis, 0);
    });

    test('rewinds the watermark on an install already at this algorithm version',
        () async {
      // The rewind used to hang off the algorithm-version reset, which returns
      // early when the version already matches -- so on every install that had
      // seen the current version, which is all of them, it was dead code. The
      // fit bug did not change the algorithm, so nothing ever triggered it.
      // Measured after the fix shipped: 6030 stored samples, ~100 pairable
      // hours, watch observation count still 2, every gain still exactly 1.00.
      await setUpChain();
      prefs.bodyEnergyGainsAlgorithmVersion = bodyEnergyTimelineAlgorithmVersion;
      prefs.bodyEnergyWatchFitEpoch = 0;
      prefs.bodyEnergyWatchFitWatermarkMillis =
          today.atStartOfDayUtc().millisecondsSinceEpoch;

      await load(repo(), today);

      expect(prefs.bodyEnergyWatchFitWatermarkMillis, 0);
      expect(prefs.bodyEnergyWatchFitEpoch, bodyEnergyWatchFitEpoch);
    });

    test('and does not rewind it again once that epoch is recorded', () async {
      // Otherwise every load re-reads a week of watch samples and refits the
      // gains from them, compounding the same evidence without end.
      await setUpChain();
      prefs.bodyEnergyGainsAlgorithmVersion = bodyEnergyTimelineAlgorithmVersion;
      prefs.bodyEnergyWatchFitEpoch = bodyEnergyWatchFitEpoch;
      final watermark = today.atStartOfDayUtc().millisecondsSinceEpoch;
      prefs.bodyEnergyWatchFitWatermarkMillis = watermark;

      await load(repo(), today);

      expect(prefs.bodyEnergyWatchFitWatermarkMillis, watermark);
    });

    test('rewinds the watermark even when there were no personal gains to reset',
        () async {
      // A model still sitting at 1.0 is the one with the most to relearn, and
      // the early return for "nothing to reset" used to skip it.
      await setUpChain();
      prefs.bodyEnergyWatchFitWatermarkMillis =
          today.atStartOfDayUtc().millisecondsSinceEpoch;
      prefs.setBodyEnergyCalibration(const BodyEnergyCalibration());

      await load(repo(), today);

      expect(prefs.bodyEnergyWatchFitWatermarkMillis, 0);
    });

    test('leaves the manual heart zones and profile alone', () async {
      await setUpChain();
      prefs.setBodyEnergyCalibration(
        const BodyEnergyCalibration(
          sleepChargeGain: 0.8,
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

      await load(repo(), today);

      final after = prefs.bodyEnergyCalibrationListenable.value;
      expect(after.sleepChargeGain, 1.0);
      expect(after.useManualZones, isTrue);
      expect(after.manualZoneThresholdsBpm?.zone4LowerBpm, 155);
    });

    test('does not undo a gain learned after it ran', () async {
      await setUpChain();
      final r = repo();
      await load(r, today);

      prefs.setBodyEnergyCalibration(
        const BodyEnergyCalibration(sleepChargeGain: 1.4),
      );
      await load(r, today.minusDays(1));

      expect(prefs.bodyEnergyCalibrationListenable.value.sleepChargeGain, 1.4);
    });
  });
}
