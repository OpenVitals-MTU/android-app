import '../../../core/result/result.dart';
import '../../../domain/model/apple_health_import_records.dart';
import '../../../domain/refresh/data_change_sink.dart';
import '../../../domain/refresh/data_domain.dart';
import '../../source/health/health_data_source.dart';
import '../contract/import_write_repository.dart';
import 'run_catching.dart';

/// Ported from the Kotlin `AppleHealthImportRepository` (see the contract for
/// why the name lost its Apple), delegating to the [HealthDataSource]
/// imported-records write surface.
///
/// Public methods convert exceptions to failures via [runCatching] at the
/// boundary.
class ImportWriteRepositoryImpl implements ImportWriteRepository {
  ImportWriteRepositoryImpl(
    this._dataSource, {
    DataChangeSink changes = const NoopDataChangeSink(),
    // ignore: prefer_initializing_formals
  }) : _changes = changes;

  final HealthDataSource _dataSource;

  /// Where a successful import batch is announced.
  ///
  /// Every domain, not a set derived per record: a batch spans hundreds of
  /// records of every kind, and the coordinator's debounce collapses a whole
  /// import into one signal anyway. An Apple Health import used to invalidate
  /// nothing at all.
  final DataChangeSink _changes;

  @override
  bool isMindfulnessAvailable() => _dataSource.isMindfulnessSessionAvailable();

  @override
  Future<Result<void>> insertImportedRecords(List<ImportRecord> records) =>
      runCatching(() async {
        await _dataSource.insertImportedRecords(records);
        if (records.isNotEmpty) _changes.changed(DataDomain.values.toSet());
      });

  @override
  Future<Result<Set<String>>> findMatchingImportedClientRecordIds(
    String recordType,
    DateTime start,
    DateTime end,
    Set<String> wantedIds,
  ) =>
      runCatching(
        () => _dataSource.findMatchingImportedClientRecordIds(
          recordType,
          start,
          end,
          wantedIds,
        ),
      );
}
