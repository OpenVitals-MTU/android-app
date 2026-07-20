import 'package:flutter_test/flutter_test.dart';
import 'package:openvitals/data/source/health/health_data_source.dart';
import 'package:openvitals/data/source/sync/health_connect_sync_store.dart';
import 'package:openvitals/data/source/sync/import_record_sync_codec.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';

/// A HealthDataSource that models Health Connect's import surface in memory:
/// records live keyed by clientRecordId; reads return them, writes upsert, and
/// the clientRecordId lookup powers dedup.
class FakeHealthDataSource extends HealthDataSource {
  final Map<String, ImportRecord> _byClientId = {};

  void seed(ImportRecord record) {
    // Emulate Health Connect assigning the sync fingerprint as clientRecordId,
    // as the write path does.
    _byClientId[syncFingerprint(record)] = record;
  }

  int get count => _byClientId.length;

  @override
  Future<List<ImportRecord>> readImportRecords(
    String recordType,
    DateTime start,
    DateTime end,
  ) async =>
      _byClientId.values.where((r) => r.targetType == recordType).toList();

  @override
  Future<Set<String>> findMatchingImportedClientRecordIds(
    String recordType,
    DateTime start,
    DateTime end,
    Set<String> wantedIds,
  ) async =>
      wantedIds.where(_byClientId.containsKey).toSet();

  @override
  Future<void> insertImportedRecords(List<ImportRecord> records) async {
    for (final r in records) {
      _byClientId[r.clientRecordId] = r;
    }
  }
}

WeightImportRecord weight(int day, double kg) => WeightImportRecord(
      clientRecordId: 'ignored',
      time: DateTime.utc(2026, 1, day),
      zoneOffset: null,
      kilograms: kg,
    );

void main() {
  late FakeHealthDataSource ds;
  late HealthConnectSyncStore store;

  setUp(() {
    ds = FakeHealthDataSource();
    store = HealthConnectSyncStore(
      dataSource: ds,
      windowStart: DateTime.utc(2025, 1, 1),
      windowEnd: DateTime.utc(2027, 1, 1),
    );
  });

  test('readItems keys each record by its content fingerprint', () async {
    ds.seed(weight(1, 70));
    ds.seed(weight(2, 71));

    final items = await store.readItems({'WeightRecord'});
    expect(items, hasLength(2));
    for (final item in items) {
      expect(item.key, startsWith('sync_'));
      expect(item.recordType, 'WeightRecord');
    }
  });

  test('existingKeys reports only fingerprints Health Connect already holds',
      () async {
    ds.seed(weight(1, 70));
    final items = await store.readItems({'WeightRecord'});
    final present = await store.existingKeys(items);
    expect(present, {items.single.key});

    // An item the store has never seen is absent.
    final fresh = await store.readItems({'WeightRecord'});
    final other = HealthConnectSyncStore(
      dataSource: FakeHealthDataSource(),
      windowStart: store.windowStart,
      windowEnd: store.windowEnd,
    );
    expect(await other.existingKeys(fresh), isEmpty);
  });

  test('writeItems reconstructs typed records under the fingerprint id',
      () async {
    // Build items on a source phone, then write them into a fresh target.
    final source = FakeHealthDataSource()..seed(weight(3, 80));
    final sourceStore = HealthConnectSyncStore(
      dataSource: source,
      windowStart: store.windowStart,
      windowEnd: store.windowEnd,
    );
    final items = await sourceStore.readItems({'WeightRecord'});

    await store.writeItems(items);
    expect(ds.count, 1);

    // The written record is typed and re-fingerprints to the same key (so a
    // re-sync would dedup it).
    final written = await store.readItems({'WeightRecord'});
    expect(written.single.key, items.single.key);
    expect(await store.existingKeys(items), {items.single.key});
  });

  test('a full round trip through the store is idempotent', () async {
    final source = FakeHealthDataSource()
      ..seed(weight(4, 60))
      ..seed(weight(5, 61));
    final sourceStore = HealthConnectSyncStore(
      dataSource: source,
      windowStart: store.windowStart,
      windowEnd: store.windowEnd,
    );

    final items = await sourceStore.readItems({'WeightRecord'});
    // First write imports both.
    expect(await store.existingKeys(items), isEmpty);
    await store.writeItems(items);
    expect(ds.count, 2);

    // Second pass: all already present, nothing new to write.
    final present = await store.existingKeys(items);
    expect(present, items.map((i) => i.key).toSet());
  });
}
