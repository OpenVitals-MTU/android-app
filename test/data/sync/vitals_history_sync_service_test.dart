import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';

import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/data/local/open_vitals_database.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/data/sync/vitals_history_sync_service.dart';
import 'package:openvitals/domain/health/health_permissions.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/vitals_change_batch.dart';
import 'package:openvitals/domain/model/vitals_models.dart';

/// A source backed by a mutable "truth" of respiratory daily points, plus a
/// queue of change batches to hand back from getVitalsChanges.
class _FakeSource extends HealthDataSource {
  final Map<int, DailyVitalPoint> respByDay = {};
  final List<VitalsChangeBatch> changeBatches = [];
  String tokenToReturn = 'token-1';

  /// Records the order of token-registration vs history-read so a test can
  /// assert the token is taken BEFORE the (slow) read.
  final List<String> callOrder = [];

  /// Permissions the fake reports as granted; defaults to all vitals reads.
  Set<String> granted = {
    HcPermissions.readRespiratoryRate,
    HcPermissions.readSpO2,
    HcPermissions.readBodyTemperature,
    HcPermissions.readVo2Max,
    HcPermissions.readBloodGlucose,
    HcPermissions.readSkinTemperature,
    HcPermissions.readBloodPressure,
  };

  @override
  HealthConnectAvailability get cachedAvailability =>
      HealthConnectAvailability.available;

  @override
  bool isSkinTemperatureAvailable() => true;

  @override
  Future<Set<String>> grantedPermissions() async => granted;

  @override
  Future<List<DailyVitalPoint>> readDailyRespiratoryRate(
    LocalDate start,
    LocalDate end,
  ) async {
    callOrder.add('read');
    return [
      for (final e in respByDay.entries)
        if (e.key >= start.epochDay && e.key <= end.epochDay) e.value,
    ];
  }

  @override
  Future<String> getVitalsChangesToken(String recordType) async {
    callOrder.add('getToken');
    return tokenToReturn;
  }

  @override
  Future<VitalsChangeBatch> getVitalsChanges(String token) async =>
      changeBatches.removeAt(0);
}

void main() {
  late OpenVitalsDatabase db;
  late VitalsDailyCacheDao dao;
  late _FakeSource source;

  final now = DateTime.utc(2026, 7, 16, 12);
  final today = LocalDate.fromDateTime(now);

  VitalsHistorySyncService service() =>
      VitalsHistorySyncService(dao, source, clock: () => now);

  setUp(() {
    db = OpenVitalsDatabase(NativeDatabase.memory());
    dao = db.vitalsDailyCacheDao;
    source = _FakeSource();
  });

  tearDown(() => db.close());

  test('full sync (no cursor) buckets the range and stores a token', () async {
    source.respByDay[today.epochDay] =
        DailyVitalPoint(date: today, value: 12, count: 3);

    await service().syncAll();

    final rows = await dao.aggregatesBetween(
        'respiratoryRate', today.epochDay - 1, today.epochDay + 1);
    // Sum reconstructs as mean*count: 12 * 3.
    expect(rows.single.valueSum, 36);
    expect(rows.single.sampleCount, 3);
    expect((await dao.cursor('respiratoryRate'))!.changesToken, 'token-1');
  });

  test('full sync registers the changes token BEFORE the history read',
      () async {
    // A write that lands during the (slow) read is in neither the snapshot nor
    // the token's delta unless the token is taken first — so it must precede the
    // read, or such writes are silently lost until the next full rebuild.
    // Isolate to one metric so the order is unambiguous.
    source.granted = {HcPermissions.readRespiratoryRate};
    source.respByDay[today.epochDay] =
        DailyVitalPoint(date: today, value: 12, count: 3);

    await service().syncAll();

    expect(source.callOrder, ['getToken', 'read']);
  });

  test('incremental sync recomputes only the changed day and advances the token',
      () async {
    await dao.writeFullSync('respiratoryRate', 'tok', 0);
    // A new reading lands today; the Changes API reports that day changed.
    source.respByDay[today.epochDay] =
        DailyVitalPoint(date: today, value: 20, count: 1);
    source.changeBatches.add(VitalsChangeBatch(
      upsertedDays: [today],
      hasDeletions: false,
      nextToken: 'tok2',
      tokenExpired: false,
      hasMore: false,
    ));

    await service().syncAll();

    final rows =
        await dao.aggregatesBetween('respiratoryRate', today.epochDay, today.epochDay);
    expect(rows.single.valueSum, 20);
    expect((await dao.cursor('respiratoryRate'))!.changesToken, 'tok2');
  });

  test('a deletion triggers a full rebuild from the current truth', () async {
    await dao.writeFullSync('respiratoryRate', 'tok', 0);
    // A stale cached day that no longer matches the source.
    await dao.upsertDay(
        metric: 'respiratoryRate',
        epochDay: today.epochDay,
        valueSum: 99,
        sampleCount: 9);
    source.respByDay[today.epochDay] =
        DailyVitalPoint(date: today, value: 15, count: 1);
    source.changeBatches.add(VitalsChangeBatch(
      upsertedDays: const [],
      hasDeletions: true,
      nextToken: 'tokX',
      tokenExpired: false,
      hasMore: false,
    ));

    await service().syncAll();

    final rows =
        await dao.aggregatesBetween('respiratoryRate', today.epochDay, today.epochDay);
    expect(rows.single.valueSum, 15, reason: 'rebuilt from source, stale gone');
  });

  test('an expired token triggers a full rebuild', () async {
    await dao.writeFullSync('respiratoryRate', 'stale', 0);
    source.respByDay[today.epochDay] =
        DailyVitalPoint(date: today, value: 11, count: 1);
    source.changeBatches.add(VitalsChangeBatch(
      upsertedDays: const [],
      hasDeletions: false,
      nextToken: 'ignored',
      tokenExpired: true,
      hasMore: false,
    ));
    source.tokenToReturn = 'fresh-after-rebuild';

    await service().syncAll();

    expect(
        (await dao.aggregatesBetween(
                'respiratoryRate', today.epochDay, today.epochDay))
            .single
            .valueSum,
        11);
    expect((await dao.cursor('respiratoryRate'))!.changesToken,
        'fresh-after-rebuild');
  });

  test('a day that lost all its records is deleted from the cache', () async {
    await dao.writeFullSync('respiratoryRate', 'tok', 0);
    await dao.upsertDay(
        metric: 'respiratoryRate',
        epochDay: today.epochDay,
        valueSum: 30,
        sampleCount: 2);
    // Source now has nothing for that day; a change points at it.
    source.changeBatches.add(VitalsChangeBatch(
      upsertedDays: [today],
      hasDeletions: false,
      nextToken: 'tok2',
      tokenExpired: false,
      hasMore: false,
    ));

    await service().syncAll();

    expect(
        await dao.aggregatesBetween(
            'respiratoryRate', today.epochDay, today.epochDay),
        isEmpty);
  });
}
