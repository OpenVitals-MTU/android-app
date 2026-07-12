import '../../../core/result/result.dart';
import '../../../domain/model/apple_health_import_records.dart';
import '../../source/health/health_data_source.dart';
import '../contract/apple_health_import_repository.dart';
import 'run_catching.dart';

/// Port of the Kotlin `AppleHealthImportRepository`, delegating to the
/// [HealthDataSource] imported-records write surface.
///
/// Public methods convert exceptions to failures via [runCatching] at the
/// boundary.
class AppleHealthImportRepositoryImpl implements AppleHealthImportRepository {
  AppleHealthImportRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  @override
  bool isMindfulnessAvailable() => _dataSource.isMindfulnessSessionAvailable();

  @override
  Future<Result<void>> insertImportedRecords(List<ImportRecord> records) =>
      runCatching(() => _dataSource.insertImportedRecords(records));

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
