import '../../../core/period/period_load_query.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/cycle_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/cycle_period_data.dart';
import '../../source/health/health_data_source.dart';
import '../../source/health/health_permissions.dart';
import '../contract/cycle_repository.dart';
import 'repository_time.dart';
import 'health_connect_gating.dart';
import 'run_catching.dart';

/// Port of the Kotlin `CycleRepositoryImpl`.
///
/// Public methods convert exceptions to failures via [runCatching] at the
/// boundary; the private `_raw` bodies keep the original throwing flow so
/// internal composition stays plain awaits.
class CycleRepositoryImpl implements CycleRepository {
  CycleRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  @override
  Set<String> get phase4Permissions =>
      _dataSource.permissionService.phase4Permissions;

  @override
  Future<Result<Set<String>>> missingPermissions() =>
      runCatching(_missingPermissionsRaw);

  Future<Set<String>> _missingPermissionsRaw() async {
    final granted = await _dataSource.grantedIfAvailable();
    return phase4Permissions.difference(granted);
  }

  @override
  Future<Result<CyclePeriodData>> loadCyclePeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runCatching(() async {
        final current = query.windows.current;
        final data = await _loadCycleDataRaw(current.start, current.end);
        final missing = await _missingPermissionsRaw();
        return CyclePeriodData(data: data, missingPermissions: missing);
      });

  @override
  Future<Result<CycleData>> loadCycleData(LocalDate start, LocalDate end) =>
      runCatching(() => _loadCycleDataRaw(start, end));

  Future<CycleData> _loadCycleDataRaw(LocalDate start, LocalDate end) async {
    final granted = await _dataSource.grantedIfAvailable();
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
