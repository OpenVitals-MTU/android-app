import '../../../features/imports/applehealth/apple_health_import_records.dart';
import '../../../health/health_data_source.dart';
import '../contract/apple_health_import_repository.dart';

/// Port of the Kotlin `AppleHealthImportRepository`, delegating to the
/// [HealthDataSource] imported-records write surface.
class AppleHealthImportRepositoryImpl implements AppleHealthImportRepository {
  AppleHealthImportRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  @override
  bool isMindfulnessAvailable() => _dataSource.isMindfulnessSessionAvailable();

  @override
  Future<void> insertImportedRecords(List<ImportRecord> records) =>
      _dataSource.insertImportedRecords(records);

  @override
  Future<Set<String>> findMatchingImportedClientRecordIds(
    String recordType,
    DateTime start,
    DateTime end,
    Set<String> wantedIds,
  ) =>
      _dataSource.findMatchingImportedClientRecordIds(
        recordType,
        start,
        end,
        wantedIds,
      );
}
