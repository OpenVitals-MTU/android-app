import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';

void main() {
  late OpenVitalsDatabase db;
  late VitalsDailyCacheDao dao;

  setUp(() {
    db = OpenVitalsDatabase(NativeDatabase.memory());
    dao = db.vitalsDailyCacheDao;
  });

  tearDown(() => db.close());

  test('upsert then read back a day range, ordered by day', () async {
    await dao.upsertDay(
        metric: 'respiratoryRate', epochDay: 100, valueSum: 36, sampleCount: 3);
    await dao.upsertDay(
        metric: 'respiratoryRate', epochDay: 98, valueSum: 20, sampleCount: 1);
    // A different metric on the same day must not bleed in.
    await dao.upsertDay(
        metric: 'spo2', epochDay: 100, valueSum: 96, sampleCount: 1);

    final rows = await dao.aggregatesBetween('respiratoryRate', 90, 110);
    expect(rows.map((r) => r.epochDay), [98, 100]);
    // Mean reconstructs from sum/count: day 100 = 36/3 = 12.
    expect(rows.last.valueSum / rows.last.sampleCount, 12);
  });

  test('upsert overwrites the same (metric, day)', () async {
    await dao.upsertDay(
        metric: 'spo2', epochDay: 5, valueSum: 90, sampleCount: 1);
    await dao.upsertDay(
        metric: 'spo2', epochDay: 5, valueSum: 190, sampleCount: 2);

    final rows = await dao.aggregatesBetween('spo2', 5, 5);
    expect(rows.single.valueSum, 190);
    expect(rows.single.sampleCount, 2);
  });

  test('replaceMetric atomically swaps every day for that metric only',
      () async {
    await dao.upsertDay(
        metric: 'respiratoryRate', epochDay: 1, valueSum: 12, sampleCount: 1);
    await dao.upsertDay(
        metric: 'spo2', epochDay: 1, valueSum: 96, sampleCount: 1);

    await dao.replaceMetric('respiratoryRate', [
      VitalsDailyAggregatesCompanion.insert(
          metric: 'respiratoryRate',
          epochDay: 2,
          valueSum: 14,
          sampleCount: 1),
    ]);

    expect(await dao.aggregatesBetween('respiratoryRate', 0, 10),
        hasLength(1)); // old day 1 gone, new day 2 present
    expect((await dao.aggregatesBetween('respiratoryRate', 0, 10)).single.epochDay,
        2);
    // The other metric is untouched.
    expect(await dao.aggregatesBetween('spo2', 0, 10), hasLength(1));
  });

  test('blood pressure carries a secondary (diastolic) sum', () async {
    await dao.upsertDay(
        metric: 'bloodPressure',
        epochDay: 3,
        valueSum: 240,
        sampleCount: 2,
        secondarySum: 160);
    final row = (await dao.aggregatesBetween('bloodPressure', 3, 3)).single;
    expect(row.valueSum / row.sampleCount, 120); // systolic mean
    expect(row.secondarySum! / row.sampleCount, 80); // diastolic mean
  });

  test('deleteDay removes only that day', () async {
    await dao.upsertDay(
        metric: 'spo2', epochDay: 1, valueSum: 96, sampleCount: 1);
    await dao.upsertDay(
        metric: 'spo2', epochDay: 2, valueSum: 97, sampleCount: 1);
    await dao.deleteDay('spo2', 1);
    expect((await dao.aggregatesBetween('spo2', 0, 10)).single.epochDay, 2);
  });

  group('sync cursor', () {
    test('writeFullSync sets token and stamp; writeToken preserves the stamp',
        () async {
      await dao.writeFullSync('respiratoryRate', 'tokenA', 111);
      var cursor = await dao.cursor('respiratoryRate');
      expect(cursor!.changesToken, 'tokenA');
      expect(cursor.lastFullSyncMillis, 111);

      await dao.writeToken('respiratoryRate', 'tokenB');
      cursor = await dao.cursor('respiratoryRate');
      expect(cursor!.changesToken, 'tokenB');
      expect(cursor.lastFullSyncMillis, 111,
          reason: 'an incremental token advance keeps the full-sync stamp');
    });

    test('writeToken inserts a row when none exists yet', () async {
      await dao.writeToken('spo2', 'fresh');
      expect((await dao.cursor('spo2'))!.changesToken, 'fresh');
    });

    test('cursor is null for an unsynced metric', () async {
      expect(await dao.cursor('vo2Max'), isNull);
    });
  });
}
