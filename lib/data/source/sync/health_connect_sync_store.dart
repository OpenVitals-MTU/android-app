/// The Health Connect implementation of [SyncRecordStore], bridging the sync
/// protocol to real record reads, dedup, and writes.
///
/// Reads become [SyncItem]s keyed by content fingerprint ([syncFingerprint]);
/// dedup runs through Health Connect's clientRecordId lookup
/// ([HealthDataSource.findMatchingImportedClientRecordIds]); writes reconstruct
/// typed [ImportRecord]s (carrying the fingerprint as their clientRecordId) and
/// insert them via [HealthDataSource.insertImportedRecords]. Because both phones
/// compute the same fingerprint and write it as the clientRecordId, re-syncs
/// converge and Health Connect upserts rather than duplicating.
library;

import '../../../domain/model/apple_health_import_records.dart';
import '../health/health_data_source.dart';
import 'import_record_sync_codec.dart';
import 'sync_messages.dart';
import 'sync_session.dart';

class HealthConnectSyncStore implements SyncRecordStore {
  HealthConnectSyncStore({
    required HealthDataSource dataSource,
    required this.windowStart,
    required this.windowEnd,
  }) : _dataSource = dataSource; // ignore: prefer_initializing_formals

  final HealthDataSource _dataSource;

  /// The inclusive sync window the user chose ("how far back").
  final DateTime windowStart;
  final DateTime windowEnd;

  @override
  Future<List<SyncItem>> readItems(Set<String> types) async {
    final items = <SyncItem>[];
    for (final type in types) {
      final records =
          await _dataSource.readImportRecords(type, windowStart, windowEnd);
      for (final record in records) {
        final key = syncFingerprint(record);
        items.add(
          SyncItem(
            key: key,
            recordType: record.targetType,
            payload: encodeImportRecordPayload(record),
          ),
        );
      }
    }
    return items;
  }

  @override
  Future<Set<String>> existingKeys(List<SyncItem> incoming) async {
    // Group by type so each clientRecordId lookup is one bounded query.
    final byType = <String, Set<String>>{};
    for (final item in incoming) {
      (byType[item.recordType] ??= <String>{}).add(item.key);
    }
    final present = <String>{};
    for (final entry in byType.entries) {
      final matches = await _dataSource.findMatchingImportedClientRecordIds(
        entry.key,
        windowStart,
        windowEnd,
        entry.value,
      );
      present.addAll(matches);
    }
    return present;
  }

  @override
  Future<void> writeItems(List<SyncItem> items) async {
    final records = <ImportRecord>[
      for (final item in items)
        decodeImportRecord(
          recordType: item.recordType,
          clientRecordId: item.key,
          payload: item.payload,
        ),
    ];
    if (records.isNotEmpty) {
      await _dataSource.insertImportedRecords(records);
    }
  }
}
