import '../../../core/period/period_load_query.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/model/vitals_models.dart';
import '../../../domain/query/vitals_period_data.dart';
import '../../source/health/health_data_source.dart';
import '../../source/health/health_permissions.dart';
import '../contract/vitals_repository.dart';
import '../contract/repository_exceptions.dart';
import 'repository_time.dart';
import 'health_connect_gating.dart';

/// Port of the Kotlin `VitalsRepositoryImpl`.
class VitalsRepositoryImpl implements VitalsRepository {
  VitalsRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  @override
  Set<String> get phase3Permissions =>
      _dataSource.permissionService.phase3Permissions;

  @override
  Set<String> vitalsWritePermissions(VitalsMeasurementType type) =>
      switch (type) {
        VitalsMeasurementType.bloodPressure => {HcPermissions.writeBloodPressure},
        VitalsMeasurementType.spo2 => {HcPermissions.writeSpO2},
        VitalsMeasurementType.respiratoryRate => {HcPermissions.writeRespiratoryRate},
        VitalsMeasurementType.bodyTemperature => {HcPermissions.writeBodyTemperature},
      };

  @override
  Future<Set<String>> missingPermissions() async {
    final granted = await _dataSource.grantedIfAvailable();
    return phase3Permissions.difference(granted);
  }

  @override
  Future<VitalsPeriodData> loadVitalsPeriod(
    PeriodLoadQuery query,
    VitalsPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final granted = await _dataSource.grantedIfAvailable();
    final missing = phase3Permissions.difference(granted);
    final w = query.windows;

    switch (metric) {
      case VitalsPeriodMetric.all:
        // Kotlin's ALL loads only the current window (no previous/baseline).
        final c = w.current;
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          bloodPressure: await _bloodPressure(c.start, c.end, granted),
          spO2: await _spO2(c.start, c.end, granted),
          respiratoryRate: await _respiratoryRate(c.start, c.end, granted),
          bodyTemperature: await _bodyTemperature(c.start, c.end, granted),
          vo2Max: await _vo2Max(c.start, c.end, granted),
          bloodGlucose: await _bloodGlucose(c.start, c.end, granted),
          skinTemperature: await _skinTemperature(c.start, c.end, granted),
        );
      case VitalsPeriodMetric.bloodPressure:
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          bloodPressure: await _bloodPressure(w.current.start, w.current.end, granted),
          previousBloodPressure:
              await _bloodPressure(w.previous.start, w.previous.end, granted),
          baselineBloodPressure:
              await _bloodPressure(w.baseline.start, w.baseline.end, granted),
        );
      case VitalsPeriodMetric.spo2:
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          spO2: await _spO2(w.current.start, w.current.end, granted),
          previousSpO2: await _spO2(w.previous.start, w.previous.end, granted),
          baselineSpO2: await _spO2(w.baseline.start, w.baseline.end, granted),
        );
      case VitalsPeriodMetric.vo2Max:
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          vo2Max: await _vo2Max(w.current.start, w.current.end, granted),
          previousVo2Max: await _vo2Max(w.previous.start, w.previous.end, granted),
          baselineVo2Max: await _vo2Max(w.baseline.start, w.baseline.end, granted),
        );
      case VitalsPeriodMetric.respiratoryRate:
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          respiratoryRate: await _respiratoryRate(w.current.start, w.current.end, granted),
          previousRespiratoryRate:
              await _respiratoryRate(w.previous.start, w.previous.end, granted),
          baselineRespiratoryRate:
              await _respiratoryRate(w.baseline.start, w.baseline.end, granted),
        );
      case VitalsPeriodMetric.bodyTemperature:
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          bodyTemperature: await _bodyTemperature(w.current.start, w.current.end, granted),
          previousBodyTemperature:
              await _bodyTemperature(w.previous.start, w.previous.end, granted),
          baselineBodyTemperature:
              await _bodyTemperature(w.baseline.start, w.baseline.end, granted),
        );
      case VitalsPeriodMetric.bloodGlucose:
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          bloodGlucose: await _bloodGlucose(w.current.start, w.current.end, granted),
          previousBloodGlucose:
              await _bloodGlucose(w.previous.start, w.previous.end, granted),
          baselineBloodGlucose:
              await _bloodGlucose(w.baseline.start, w.baseline.end, granted),
        );
      case VitalsPeriodMetric.skinTemperature:
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          skinTemperature: await _skinTemperature(w.current.start, w.current.end, granted),
          previousSkinTemperature:
              await _skinTemperature(w.previous.start, w.previous.end, granted),
          baselineSkinTemperature:
              await _skinTemperature(w.baseline.start, w.baseline.end, granted),
        );
    }
  }

  @override
  Future<List<BloodPressureEntry>> loadBloodPressure(
    LocalDate start,
    LocalDate end,
  ) async =>
      _bloodPressure(start, end, await _dataSource.grantedIfAvailable());

  @override
  Future<List<SpO2Entry>> loadSpO2(LocalDate start, LocalDate end) async =>
      _spO2(start, end, await _dataSource.grantedIfAvailable());

  @override
  Future<List<RespiratoryRateEntry>> loadRespiratoryRate(
    LocalDate start,
    LocalDate end,
  ) async =>
      _respiratoryRate(start, end, await _dataSource.grantedIfAvailable());

  @override
  Future<List<BodyTempEntry>> loadBodyTemperature(
    LocalDate start,
    LocalDate end,
  ) async =>
      _bodyTemperature(start, end, await _dataSource.grantedIfAvailable());

  @override
  Future<List<Vo2MaxEntry>> loadVo2Max(LocalDate start, LocalDate end) async =>
      _vo2Max(start, end, await _dataSource.grantedIfAvailable());

  @override
  Future<List<BloodGlucoseEntry>> loadBloodGlucose(
    LocalDate start,
    LocalDate end,
  ) async =>
      _bloodGlucose(start, end, await _dataSource.grantedIfAvailable());

  @override
  Future<List<SkinTemperatureEntry>> loadSkinTemperature(
    LocalDate start,
    LocalDate end,
  ) async =>
      _skinTemperature(start, end, await _dataSource.grantedIfAvailable());

  @override
  Future<bool> hasVitalsWritePermission(VitalsMeasurementType type) async {
    final granted = await _dataSource.grantedIfAvailable();
    return granted.containsAll(vitalsWritePermissions(type));
  }

  @override
  Future<String> writeVitalsMeasurementEntry(
    VitalsMeasurementWriteRequest request,
  ) async {
    await _requireWrite(request.type);
    return _dataSource.writeVitalsMeasurementEntry(request);
  }

  @override
  Future<VitalsMeasurementEntry?> loadVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  ) =>
      _dataSource.readVitalsMeasurementEntry(type, id);

  @override
  Future<void> updateVitalsMeasurementEntry(
    String id,
    VitalsMeasurementWriteRequest request,
  ) async {
    await _requireWrite(request.type);
    return _dataSource.updateVitalsMeasurementEntry(id, request);
  }

  @override
  Future<void> deleteVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  ) async {
    await _requireWrite(type);
    return _dataSource.deleteVitalsMeasurementEntry(type, id);
  }

  Future<void> _requireWrite(VitalsMeasurementType type) async {
    final granted = await _dataSource.grantedIfAvailable();
    final missing = vitalsWritePermissions(type).difference(granted);
    if (missing.isNotEmpty) {
      throw MissingHealthPermissionException(
        'Missing Health Connect write permission for ${type.storageName}.',
      );
    }
  }

  // ── Gated series reads ────────────────────────────────────────────────────

  Future<List<BloodPressureEntry>> _bloodPressure(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readBloodPressure)) return const [];
    return _dataSource.readBloodPressureEntries(localDayStart(start), localDayEnd(end));
  }

  Future<List<SpO2Entry>> _spO2(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readSpO2)) return const [];
    return _dataSource.readSpO2Entries(localDayStart(start), localDayEnd(end));
  }

  Future<List<RespiratoryRateEntry>> _respiratoryRate(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readRespiratoryRate)) return const [];
    return _dataSource.readRespiratoryRateEntries(
        localDayStart(start), localDayEnd(end));
  }

  Future<List<BodyTempEntry>> _bodyTemperature(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readBodyTemperature)) return const [];
    return _dataSource.readBodyTemperatureEntries(
        localDayStart(start), localDayEnd(end));
  }

  Future<List<Vo2MaxEntry>> _vo2Max(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readVo2Max)) return const [];
    return _dataSource.readVo2MaxEntries(localDayStart(start), localDayEnd(end));
  }

  Future<List<BloodGlucoseEntry>> _bloodGlucose(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readBloodGlucose)) return const [];
    return _dataSource.readBloodGlucoseEntries(localDayStart(start), localDayEnd(end));
  }

  Future<List<SkinTemperatureEntry>> _skinTemperature(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!_dataSource.isSkinTemperatureAvailable()) return const [];
    if (!granted.contains(HcPermissions.readSkinTemperature)) return const [];
    return _dataSource.readSkinTemperatureEntries(
        localDayStart(start), localDayEnd(end));
  }
}
