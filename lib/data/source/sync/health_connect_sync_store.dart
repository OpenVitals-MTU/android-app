/// The Health Connect implementation of [SyncRecordStore], bridging the sync
/// protocol to real record reads, dedup, and writes.
///
/// Reads become [SyncItem]s keyed by content fingerprint ([syncFingerprint]);
/// dedup happens in the session, which seeds its baseline from these same keys
/// (see [SyncRecordStore.readItems]); writes reconstruct
/// typed [ImportRecord]s (carrying the fingerprint as their clientRecordId) and
/// insert them via [HealthDataSource.insertImportedRecords]. Because both phones
/// compute the same fingerprint and write it as the clientRecordId, re-syncs
/// converge and Health Connect upserts rather than duplicating.
library;

import 'package:flutter/foundation.dart';

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
  Future<void> writeItems(List<SyncItem> items) async {
    // Group by record type and insert each group separately. A Health Connect
    // batch insert is atomic, so mixing types means one unsupported/rejected type
    // sinks the whole batch; isolating types keeps the rest landing (the earlier
    // "imported N but nothing persisted" failure). A per-type failure is logged
    // and swallowed so the sync continues with the types that do write.
    final byType = <String, List<ImportRecord>>{};
    for (final item in items) {
      (byType[item.recordType] ??= <ImportRecord>[]).add(
        decodeImportRecord(
          recordType: item.recordType,
          clientRecordId: item.key,
          payload: item.payload,
        ),
      );
    }
    for (final entry in byType.entries) {
      try {
        await _dataSource.insertImportedRecords(entry.value);
        debugPrint('[devicesync] wrote ${entry.value.length} ${entry.key} records');
      } catch (e) {
        debugPrint('[devicesync] WRITE FAILED for ${entry.value.length} ${entry.key}: $e');
      }
    }
  }
}
