import 'package:drift/drift.dart' show Value;
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/data/local/open_vitals_database.dart';

void main() {
  late OpenVitalsDatabase db;
  late BodyEnergyTimelineDao dao;

  setUp(() {
    db = OpenVitalsDatabase(NativeDatabase.memory());
    dao = db.bodyEnergyTimelineDao;
    addTearDown(db.close);
  });

  BodyEnergyDaysCompanion summary(
    int epochDay, {
    String signature = 'v5|sig|0',
    int startScore = 50,
    int endScore = 30,
  }) =>
      BodyEnergyDaysCompanion.insert(
        // A lone INTEGER PRIMARY KEY is a rowid alias, so drift makes it
        // optional on insert; the chain always supplies it.
        epochDay: Value(epochDay),
        signature: signature,
        startScore: startScore,
        endScore: endScore,
        charged: 20,
        drained: 40,
        confidence: 'HIGH',
        confidenceReason: 'reason',
        generatedAtMillis: 1_700_000_000_000,
        algorithmVersion: 5,
        bucketMinutes: 5,
        heartRateSampleCount: 100,
        hrvSampleCount: 2,
        sleepSessionCount: 1,
        workoutCount: 0,
        respiratorySampleCount: 0,
        hasRestingHeartRate: true,
        hasBaselineRestingHeartRate: true,
        hasObservedMaxHeartRate: true,
        hasHrvBaseline: false,
        hasRespiratoryBaseline: false,
        previousEndScore: const Value(30),
        carryOverFloorApplied: false,
        seedSource: 'CARRIED_OVER',
        calibrationMode: 'AUTOMATIC',
      );

  BodyEnergyBucketsCompanion bucket(int epochDay, int timeMillis, int score) =>
      BodyEnergyBucketsCompanion.insert(
        epochDay: epochDay,
        timeMillis: timeMillis,
        score: score,
        delta: -0.5,
        state: 'REST',
        confidence: 'HIGH',
        charge: 0.0,
        intensityDrain: 0.1,
        activityEnergyDrain: 0.2,
        basalDrain: 0.11,
        stressDrain: 0.0,
        recoveryDebtDrain: 0.0,
        primaryInfluence: 'STEADY',
      );

  test('stores a day with its buckets and reads both back', () async {
    await dao.upsertDay(summary(20000), [
      bucket(20000, 300, 49),
      bucket(20000, 100, 50),
      bucket(20000, 200, 50),
    ]);

    final day = await dao.day(20000);
    expect(day, isNotNull);
    expect(day!.endScore, 30);
    expect(day.seedSource, 'CARRIED_OVER');
    expect(day.previousEndScore, 30);

    final buckets = await dao.bucketsForDay(20000);
    expect(buckets.map((b) => b.timeMillis), [100, 200, 300]);
    expect(buckets.first.score, 50);
  });

  test('re-storing a day replaces its buckets rather than merging them',
      () async {
    await dao.upsertDay(summary(20000), [
      bucket(20000, 100, 50),
      bucket(20000, 200, 49),
      bucket(20000, 300, 48),
    ]);
    await dao.upsertDay(summary(20000, endScore: 12), [
      bucket(20000, 100, 40),
    ]);

    final buckets = await dao.bucketsForDay(20000);
    expect(buckets, hasLength(1),
        reason: 'the stale 200/300 buckets must not survive a recompute');
    expect(buckets.single.score, 40);
    expect((await dao.day(20000))!.endScore, 12);
    expect(await dao.countDays(), 1);
  });

  test('daysBetween is inclusive, ordered, and excludes days out of range',
      () async {
    for (final epochDay in [19998, 19999, 20000, 20001]) {
      await dao.upsertDay(summary(epochDay, endScore: epochDay - 19990), []);
    }

    final days = await dao.daysBetween(19999, 20000);
    expect(days.map((d) => d.epochDay), [19999, 20000]);
    expect(days.map((d) => d.endScore), [9, 10]);
  });

  test('bucketsBetweenDays spans days in primary-key order', () async {
    await dao.upsertDay(summary(20001), [bucket(20001, 10, 20)]);
    await dao.upsertDay(summary(20000), [
      bucket(20000, 20, 31),
      bucket(20000, 10, 30),
    ]);

    final buckets = await dao.bucketsBetweenDays(20000, 20001);
    expect(
      buckets.map((b) => '${b.epochDay}:${b.timeMillis}'),
      ['20000:10', '20000:20', '20001:10'],
    );
  });

  test('deleteDays clears both tables in range and leaves neighbours intact',
      () async {
    for (final epochDay in [19999, 20000, 20001, 20002]) {
      await dao.upsertDay(summary(epochDay), [bucket(epochDay, 10, 50)]);
    }

    await dao.deleteDays(20000, 20001);

    expect((await dao.daysBetween(0, 30000)).map((d) => d.epochDay),
        [19999, 20002]);
    expect((await dao.bucketsBetweenDays(0, 30000)).map((b) => b.epochDay),
        [19999, 20002]);
  });

  test('deleteDays is a no-op when the range is empty', () async {
    // The forward ripple passes (date + 1, today); recomputing today makes
    // end < start, and that must not wipe the chain.
    await dao.upsertDay(summary(20000), [bucket(20000, 10, 50)]);

    await dao.deleteDays(20001, 20000);

    expect(await dao.countDays(), 1);
    expect(await dao.bucketsForDay(20000), hasLength(1));
  });

  test('purgeBucketsBefore drops buckets but keeps the day summaries',
      () async {
    await dao.upsertDay(summary(19000), [bucket(19000, 10, 50)]);
    await dao.upsertDay(summary(20000), [bucket(20000, 10, 50)]);

    await dao.purgeBucketsBefore(20000);

    expect(await dao.bucketsForDay(19000), isEmpty);
    expect(await dao.bucketsForDay(20000), hasLength(1));
    expect(await dao.countDays(), 2,
        reason: 'retention drops buckets only; the chain must stay walkable');
    expect((await dao.day(19000))!.endScore, 30);
  });

  test('purgeAll empties both tables and the chain cursor', () async {
    await dao.upsertDay(summary(20000), [bucket(20000, 10, 50)]);
    await dao.writeChainCursor(globalSignature: 'v5|old', lastPassMillis: 1);

    await dao.purgeAll();

    expect(await dao.countDays(), 0);
    expect(await dao.bucketsBetweenDays(0, 30000), isEmpty);
    expect(await dao.chainCursor(), isNull);
  });

  test('latestDay returns the newest stored day', () async {
    expect(await dao.latestDay(), isNull);
    await dao.upsertDay(summary(20000), []);
    await dao.upsertDay(summary(19999), []);

    expect((await dao.latestDay())!.epochDay, 20000);
  });

  test('writeChainCursor leaves omitted fields untouched', () async {
    await dao.writeChainCursor(globalSignature: 'v5|sig', lastPassMillis: 111);
    // Recording a completed pass must not clear the stored signature, or the
    // next run would purge the whole chain as if calibration had changed.
    await dao.writeChainCursor(lastPassMillis: 222);

    final cursor = await dao.chainCursor();
    expect(cursor!.changesToken, 'v5|sig');
    expect(cursor.lastFullSyncMillis, 222);
  });
}
