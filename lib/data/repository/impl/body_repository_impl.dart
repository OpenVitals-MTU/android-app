import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/body_models.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/body_period_data.dart';
import '../../../health/health_data_source.dart';
import '../../../health/health_permissions.dart';
import '../contract/body_repository.dart';
import 'repository_exceptions.dart';

/// Port of the Kotlin `BodyRepositoryImpl`.
class BodyRepositoryImpl implements BodyRepository {
  BodyRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  Future<Set<String>> _grantedIfAvailable() async =>
      _dataSource.cachedAvailability == HealthConnectAvailability.available
          ? _dataSource.grantedPermissions()
          : <String>{};

  @override
  Set<String> bodyWritePermissions(BodyMeasurementType type) => switch (type) {
        BodyMeasurementType.weight => {HcPermissions.writeWeight},
        BodyMeasurementType.height => {HcPermissions.writeHeight},
        BodyMeasurementType.bodyFat => {HcPermissions.writeBodyFat},
      };

  @override
  Future<BodyPeriodData> loadBodyPeriod(
    PeriodLoadQuery query,
    BodyPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final granted = await _grantedIfAvailable();
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
  Future<List<WeightEntry>> loadWeightEntries(LocalDate start, LocalDate end) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readWeight)) return const [];
    return _dataSource.readWeightEntries(start, end);
  }

  @override
  Future<double?> loadLatestHeight() async => _latestHeightCm(await _grantedIfAvailable());

  @override
  Future<List<HeightEntry>> loadHeightEntries(LocalDate start, LocalDate end) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readHeight)) return const [];
    return _dataSource.readHeightEntries(start, end);
  }

  @override
  Future<List<BodyFatEntry>> loadBodyFatEntries(LocalDate start, LocalDate end) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readBodyFat)) return const [];
    return _dataSource.readBodyFatEntries(start, end);
  }

  @override
  Future<double?> loadLatestLeanBodyMass() async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readLeanMass)) return null;
    return _dataSource.readLatestLeanBodyMass();
  }

  @override
  Future<List<LeanBodyMassEntry>> loadLeanBodyMassEntries(
    LocalDate start,
    LocalDate end,
  ) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readLeanMass)) return const [];
    return _dataSource.readLeanBodyMassEntries(start, end);
  }

  @override
  Future<double?> loadLatestBMR() async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readBmr)) return null;
    return _dataSource.readLatestBMR();
  }

  @override
  Future<List<BmrEntry>> loadBmrEntries(LocalDate start, LocalDate end) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readBmr)) return const [];
    return _dataSource.readBmrEntries(start, end);
  }

  @override
  Future<double?> loadLatestBoneMass() async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readBoneMass)) return null;
    return _dataSource.readLatestBoneMass();
  }

  @override
  Future<List<BoneMassEntry>> loadBoneMassEntries(LocalDate start, LocalDate end) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readBoneMass)) return const [];
    return _dataSource.readBoneMassEntries(start, end);
  }

  @override
  Future<double?> loadLatestBodyWaterMass() async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readBodyWaterMass)) return null;
    return _dataSource.readLatestBodyWaterMass();
  }

  @override
  Future<List<BodyWaterMassEntry>> loadBodyWaterMassEntries(
    LocalDate start,
    LocalDate end,
  ) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readBodyWaterMass)) return const [];
    return _dataSource.readBodyWaterMassEntries(start, end);
  }

  @override
  Future<bool> hasBodyWritePermission(BodyMeasurementType type) async {
    final granted = await _grantedIfAvailable();
    return granted.containsAll(bodyWritePermissions(type));
  }

  @override
  Future<String> writeBodyMeasurementEntry(
    BodyMeasurementWriteRequest request,
  ) async {
    await _requireWrite(request.type);
    return _dataSource.writeBodyMeasurementEntry(request);
  }

  @override
  Future<BodyMeasurementEntry?> loadBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  ) =>
      _dataSource.readBodyMeasurementEntry(type, id);

  @override
  Future<void> updateBodyMeasurementEntry(
    String id,
    BodyMeasurementWriteRequest request,
  ) async {
    await _requireWrite(request.type);
    return _dataSource.updateBodyMeasurementEntry(id, request);
  }

  @override
  Future<void> deleteBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  ) async {
    await _requireWrite(type);
    return _dataSource.deleteBodyMeasurementEntry(type, id);
  }

  Future<void> _requireWrite(BodyMeasurementType type) async {
    final granted = await _grantedIfAvailable();
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
