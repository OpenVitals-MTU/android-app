import '../../../core/result/result.dart';
import '../../../domain/model/apple_health_import_records.dart';

/// The bulk write surface every importer shares.
///
/// Ported from the Kotlin `AppleHealthImportRepository` and renamed, because
/// nothing about it is Apple-specific: [ImportRecord] is already the common
/// currency of the Apple Health import, phone-to-phone device sync, the Garmin
/// FIT wellness import, the route/FIT bulk import and the CSV importer. The
/// Apple name only reflected which caller landed first, and a CSV importer
/// importing `apple_health_import_repository.dart` is how a misnomer ossifies.
///
/// It is also the only one of those write paths that is a repository at all.
/// The others call `HealthDataSource.insertImportedRecords` directly, which
/// throws instead of returning a [Result] and announces nothing to
/// `DataChangeSink` — so nothing refreshes after they write. New importers go
/// through here.
///
/// Fallible operations return [Result]; the synchronous availability probe
/// ([isMindfulnessAvailable]) reads cached state and cannot fail, so it stays
/// bare.
///
/// The Kotlin version took AndroidX `Record` / `KClass<out Record>` types; the
/// Dart importer produces pure-Dart [ImportRecord]s and identifies a record
/// class by its [ImportRecord.targetType] string (e.g. `HeartRateRecord`), so
/// the signatures are adapted accordingly.
///
/// On Android these delegate to `HealthConnectNativeDataSource`, which bulk-inserts
/// every record type through the native plugin and resolves clientRecordId-based
/// duplicate matching via `filterExistingClientIds` (the former `health`-package
/// impl could do neither).
abstract interface class ImportWriteRepository {
  bool isMindfulnessAvailable();

  Future<Result<void>> insertImportedRecords(List<ImportRecord> records);

  Future<Result<Set<String>>> findMatchingImportedClientRecordIds(
    String recordType,
    DateTime start,
    DateTime end,
    Set<String> wantedIds,
  );
}
