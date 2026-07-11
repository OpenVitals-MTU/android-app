import '../../../core/period/period_load_query.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/cycle_models.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/cycle_period_data.dart';
import '../../../health/health_data_source.dart';
import '../../../health/health_permissions.dart';
import '../contract/cycle_repository.dart';
import 'repository_time.dart';

/// Port of the Kotlin `CycleRepositoryImpl`.
class CycleRepositoryImpl implements CycleRepository {
  CycleRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  Future<Set<String>> _grantedIfAvailable() async =>
      _dataSource.cachedAvailability == HealthConnectAvailability.available
          ? _dataSource.grantedPermissions()
          : <String>{};

  @override
  Set<String> get phase4Permissions =>
      _dataSource.permissionService.phase4Permissions;

  @override
  Future<Set<String>> missingPermissions() async {
    final granted = await _grantedIfAvailable();
    return phase4Permissions.difference(granted);
  }

  @override
  Future<CyclePeriodData> loadCyclePeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final current = query.windows.current;
    final data = await loadCycleData(current.start, current.end);
    final missing = await missingPermissions();
    return CyclePeriodData(data: data, missingPermissions: missing);
  }

  @override
  Future<CycleData> loadCycleData(LocalDate start, LocalDate end) async {
    final granted = await _grantedIfAvailable();
    final s = localDayStart(start);
    final e = localDayEnd(end);

    Future<List<T>> read<T>(
      String permission,
      Future<List<T>> Function(DateTime, DateTime) reader,
    ) async {
      if (!granted.contains(permission)) return <T>[];
      return reader(s, e);
    }

    return CycleData(
      menstruationFlows: await read(
          HcPermissions.readMenstruationFlow, _dataSource.readMenstruationFlowEntries),
      menstruationPeriods: await read(
          HcPermissions.readMenstruationPeriod, _dataSource.readMenstruationPeriods),
      ovulationTests:
          await read(HcPermissions.readOvulationTest, _dataSource.readOvulationTests),
      cervicalMucus: await read(
          HcPermissions.readCervicalMucus, _dataSource.readCervicalMucusEntries),
      basalBodyTemperature: await read(HcPermissions.readBasalBodyTemperature,
          _dataSource.readBasalBodyTemperatureEntries),
      intermenstrualBleeding: await read(HcPermissions.readIntermenstrualBleeding,
          _dataSource.readIntermenstrualBleedingEntries),
      sexualActivity: await read(
          HcPermissions.readSexualActivity, _dataSource.readSexualActivityEntries),
    );
  }
}
