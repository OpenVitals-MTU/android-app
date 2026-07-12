import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/body_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/body_period_data.dart';
import '../../source/health/health_data_source.dart';
import '../../source/health/health_permissions.dart';
import '../contract/body_repository.dart';
import '../contract/repository_exceptions.dart';
import 'health_connect_gating.dart';
import 'run_catching.dart';

/// Port of the Kotlin `BodyRepositoryImpl`.
///
/// Public methods convert exceptions to failures via [runCatching] at the
/// boundary; the private `_raw` / `_latestX` bodies keep the original throwing
/// flow so internal composition stays plain awaits.
class BodyRepositoryImpl implements BodyRepository {
  BodyRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  @override
  Set<String> bodyWritePermissions(BodyMeasurementType type) => switch (type) {
        BodyMeasurementType.weight => {HcPermissions.writeWeight},
        BodyMeasurementType.height => {HcPermissions.writeHeight},
        BodyMeasurementType.bodyFat => {HcPermissions.writeBodyFat},
      };

  @override
  Future<Result<BodyPeriodData>> loadBodyPeriod(
    PeriodLoadQuery query,
    BodyPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runCatching(() => _loadBodyPeriodRaw(query, metric));

  Future<BodyPeriodData> _loadBodyPeriodRaw(
    PeriodLoadQuery query,
    BodyPeriodMetric metric,
  ) async {
    final granted = await _dataSource.grantedIfAvailable();
    final w = query.windows;

    Future<List<T>> read<T>(
      String permission,
      DatePeriod period,
      Future<List<T>> Function(LocalDate, LocalDate) reader,
    ) async {
      if (!granted.contains(permission)) return <T>[];
      return reader(period.start, period.end);
    }

    switch (metric) {
      case BodyPeriodMetric.all:
        return BodyPeriodData(
          weightEntries:
              await read(HcPermissions.readWeight, w.current, _dataSource.readWeightEntries),
          previousWeightEntries:
              await read(HcPermissions.readWeight, w.previous, _dataSource.readWeightEntries),
          baselineWeightEntries:
              await read(HcPermissions.readWeight, w.baseline, _dataSource.readWeightEntries),
          latestWeightKg: await _latestWeightKg(granted),
          heightCm: await _latestHeightCm(granted),
          heightEntries:
              await read(HcPermissions.readHeight, w.current, _dataSource.readHeightEntries),
          previousHeightEntries:
              await read(HcPermissions.readHeight, w.previous, _dataSource.readHeightEntries),
          baselineHeightEntries:
              await read(HcPermissions.readHeight, w.baseline, _dataSource.readHeightEntries),
          bodyFatEntries:
              await read(HcPermissions.readBodyFat, w.current, _dataSource.readBodyFatEntries),
          previousBodyFatEntries:
              await read(HcPermissions.readBodyFat, w.previous, _dataSource.readBodyFatEntries),
          baselineBodyFatEntries:
              await read(HcPermissions.readBodyFat, w.baseline, _dataSource.readBodyFatEntries),
          latestBodyFatPercent: await _latestBodyFat(granted),
          leanMassEntries: await read(
              HcPermissions.readLeanMass, w.current, _dataSource.readLeanBodyMassEntries),
          previousLeanMassEntries: await read(
              HcPermissions.readLeanMass, w.previous, _dataSource.readLeanBodyMassEntries),
          baselineLeanMassEntries: await read(
              HcPermissions.readLeanMass, w.baseline, _dataSource.readLeanBodyMassEntries),
          bmrEntries:
              await read(HcPermissions.readBmr, w.current, _dataSource.readBmrEntries),
          previousBmrEntries:
              await read(HcPermissions.readBmr, w.previous, _dataSource.readBmrEntries),
          baselineBmrEntries:
              await read(HcPermissions.readBmr, w.baseline, _dataSource.readBmrEntries),
          boneMassEntries: await read(
              HcPermissions.readBoneMass, w.current, _dataSource.readBoneMassEntries),
          previousBoneMassEntries: await read(
              HcPermissions.readBoneMass, w.previous, _dataSource.readBoneMassEntries),
          baselineBoneMassEntries: await read(
              HcPermissions.readBoneMass, w.baseline, _dataSource.readBoneMassEntries),
          bodyWaterMassEntries: await read(HcPermissions.readBodyWaterMass, w.current,
              _dataSource.readBodyWaterMassEntries),
          previousBodyWaterMassEntries: await read(HcPermissions.readBodyWaterMass,
              w.previous, _dataSource.readBodyWaterMassEntries),
          baselineBodyWaterMassEntries: await read(HcPermissions.readBodyWaterMass,
              w.baseline, _dataSource.readBodyWaterMassEntries),
        );
      case BodyPeriodMetric.weight:
        return BodyPeriodData(
          weightEntries:
              await read(HcPermissions.readWeight, w.current, _dataSource.readWeightEntries),
          previousWeightEntries:
              await read(HcPermissions.readWeight, w.previous, _dataSource.readWeightEntries),
          baselineWeightEntries:
              await read(HcPermissions.readWeight, w.baseline, _dataSource.readWeightEntries),
        );
      case BodyPeriodMetric.height:
        return BodyPeriodData(
          heightEntries:
              await read(HcPermissions.readHeight, w.current, _dataSource.readHeightEntries),
          previousHeightEntries:
              await read(HcPermissions.readHeight, w.previous, _dataSource.readHeightEntries),
          baselineHeightEntries:
              await read(HcPermissions.readHeight, w.baseline, _dataSource.readHeightEntries),
        );
      case BodyPeriodMetric.bmi:
        return BodyPeriodData(
          weightEntries:
              await read(HcPermissions.readWeight, w.current, _dataSource.readWeightEntries),
          previousWeightEntries:
              await read(HcPermissions.readWeight, w.previous, _dataSource.readWeightEntries),
          baselineWeightEntries:
              await read(HcPermissions.readWeight, w.baseline, _dataSource.readWeightEntries),
          latestWeightKg: await _latestWeightKg(granted),
          heightCm: await _latestHeightCm(granted),
        );
      case BodyPeriodMetric.bodyFat:
        return BodyPeriodData(
          bodyFatEntries:
              await read(HcPermissions.readBodyFat, w.current, _dataSource.readBodyFatEntries),
          previousBodyFatEntries:
              await read(HcPermissions.readBodyFat, w.previous, _dataSource.readBodyFatEntries),
          baselineBodyFatEntries:
              await read(HcPermissions.readBodyFat, w.baseline, _dataSource.readBodyFatEntries),
        );
      case BodyPeriodMetric.leanMass:
        return BodyPeriodData(
          leanMassEntries: await read(
              HcPermissions.readLeanMass, w.current, _dataSource.readLeanBodyMassEntries),
          previousLeanMassEntries: await read(
              HcPermissions.readLeanMass, w.previous, _dataSource.readLeanBodyMassEntries),
          baselineLeanMassEntries: await read(
              HcPermissions.readLeanMass, w.baseline, _dataSource.readLeanBodyMassEntries),
        );
      case BodyPeriodMetric.bmr:
        return BodyPeriodData(
          bmrEntries:
              await read(HcPermissions.readBmr, w.current, _dataSource.readBmrEntries),
          previousBmrEntries:
              await read(HcPermissions.readBmr, w.previous, _dataSource.readBmrEntries),
          baselineBmrEntries:
              await read(HcPermissions.readBmr, w.baseline, _dataSource.readBmrEntries),
        );
      case BodyPeriodMetric.boneMass:
        return BodyPeriodData(
          boneMassEntries: await read(
              HcPermissions.readBoneMass, w.current, _dataSource.readBoneMassEntries),
          previousBoneMassEntries: await read(
              HcPermissions.readBoneMass, w.previous, _dataSource.readBoneMassEntries),
          baselineBoneMassEntries: await read(
              HcPermissions.readBoneMass, w.baseline, _dataSource.readBoneMassEntries),
        );
      case BodyPeriodMetric.bodyWaterMass:
        return BodyPeriodData(
          bodyWaterMassEntries: await read(HcPermissions.readBodyWaterMass, w.current,
              _dataSource.readBodyWaterMassEntries),
          previousBodyWaterMassEntries: await read(HcPermissions.readBodyWaterMass,
              w.previous, _dataSource.readBodyWaterMassEntries),
          baselineBodyWaterMassEntries: await read(HcPermissions.readBodyWaterMass,
              w.baseline, _dataSource.readBodyWaterMassEntries),
        );
    }
  }

  @override
  Future<Result<List<WeightEntry>>> loadWeightEntries(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readWeight)) return const [];
        return _dataSource.readWeightEntries(start, end);
      });

  @override
  Future<Result<double?>> loadLatestHeight() => runCatching(
      () async => _latestHeightCm(await _dataSource.grantedIfAvailable()));

  @override
  Future<Result<List<HeightEntry>>> loadHeightEntries(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readHeight)) return const [];
        return _dataSource.readHeightEntries(start, end);
      });

  @override
  Future<Result<List<BodyFatEntry>>> loadBodyFatEntries(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readBodyFat)) return const [];
        return _dataSource.readBodyFatEntries(start, end);
      });

  @override
  Future<Result<double?>> loadLatestLeanBodyMass() =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readLeanMass)) return null;
        return _dataSource.readLatestLeanBodyMass();
      });

  @override
  Future<Result<List<LeanBodyMassEntry>>> loadLeanBodyMassEntries(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readLeanMass)) return const [];
        return _dataSource.readLeanBodyMassEntries(start, end);
      });

  @override
  Future<Result<double?>> loadLatestBMR() =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readBmr)) return null;
        return _dataSource.readLatestBMR();
      });

  @override
  Future<Result<List<BmrEntry>>> loadBmrEntries(LocalDate start, LocalDate end) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readBmr)) return const [];
        return _dataSource.readBmrEntries(start, end);
      });

  @override
  Future<Result<double?>> loadLatestBoneMass() =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readBoneMass)) return null;
        return _dataSource.readLatestBoneMass();
      });

  @override
  Future<Result<List<BoneMassEntry>>> loadBoneMassEntries(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readBoneMass)) return const [];
        return _dataSource.readBoneMassEntries(start, end);
      });

  @override
  Future<Result<double?>> loadLatestBodyWaterMass() =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readBodyWaterMass)) return null;
        return _dataSource.readLatestBodyWaterMass();
      });

  @override
  Future<Result<List<BodyWaterMassEntry>>> loadBodyWaterMassEntries(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readBodyWaterMass)) return const [];
        return _dataSource.readBodyWaterMassEntries(start, end);
      });

  @override
  Future<Result<bool>> hasBodyWritePermission(BodyMeasurementType type) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        return granted.containsAll(bodyWritePermissions(type));
      });

  @override
  Future<Result<String>> writeBodyMeasurementEntry(
    BodyMeasurementWriteRequest request,
  ) =>
      runCatching(() async {
        await _requireWrite(request.type);
        return _dataSource.writeBodyMeasurementEntry(request);
      });

  @override
  Future<Result<BodyMeasurementEntry?>> loadBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  ) =>
      runCatching(() => _dataSource.readBodyMeasurementEntry(type, id));

  @override
  Future<Result<void>> updateBodyMeasurementEntry(
    String id,
    BodyMeasurementWriteRequest request,
  ) =>
      runCatching(() async {
        await _requireWrite(request.type);
        return _dataSource.updateBodyMeasurementEntry(id, request);
      });

  @override
  Future<Result<void>> deleteBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  ) =>
      runCatching(() async {
        await _requireWrite(type);
        return _dataSource.deleteBodyMeasurementEntry(type, id);
      });

  Future<void> _requireWrite(BodyMeasurementType type) async {
    final granted = await _dataSource.grantedIfAvailable();
    final missing = bodyWritePermissions(type).difference(granted);
    if (missing.isNotEmpty) {
      throw const MissingHealthPermissionException(
        'Missing Health Connect body write permission.',
      );
    }
  }

  Future<double?> _latestWeightKg(Set<String> granted) async {
    if (!granted.contains(HcPermissions.readWeight)) return null;
    return (await _dataSource.readLatestWeight())?.weightKg;
  }

  Future<double?> _latestHeightCm(Set<String> granted) async {
    if (!granted.contains(HcPermissions.readHeight)) return null;
    return _dataSource.readLatestHeight();
  }

  Future<double?> _latestBodyFat(Set<String> granted) async {
    if (!granted.contains(HcPermissions.readBodyFat)) return null;
    return _dataSource.readLatestBodyFat();
  }
}
