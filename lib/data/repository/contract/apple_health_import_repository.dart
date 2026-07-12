import '../../../domain/model/apple_health_import_records.dart';

/// Port of the Kotlin `AppleHealthImportRepository` contract.
///
/// The Kotlin version takes AndroidX `Record` / `KClass<out Record>` types; the
/// Dart importer produces pure-Dart [ImportRecord]s and identifies a record
/// class by its [ImportRecord.targetType] string (e.g. `HeartRateRecord`), so
/// the signatures are adapted accordingly.
///
/// On Android these delegate to `HealthConnectNativeDataSource`, which bulk-inserts
/// every record type through the native plugin and resolves clientRecordId-based
/// duplicate matching via `filterExistingClientIds` (the former `health`-package
/// impl could do neither).
abstract interface class AppleHealthImportRepository {
  bool isMindfulnessAvailable();

  Future<void> insertImportedRecords(List<ImportRecord> records);

  Future<Set<String>> findMatchingImportedClientRecordIds(
    String recordType,
    DateTime start,
    DateTime end,
    Set<String> wantedIds,
  );
}
