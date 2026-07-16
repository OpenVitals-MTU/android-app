import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/model/vitals_models.dart';
import '../../../domain/query/vitals_period_data.dart';
import '../../source/health/health_data_source.dart';
import '../../../domain/health/health_permissions.dart';
import '../contract/vitals_repository.dart';
import '../contract/repository_exceptions.dart';
import 'repository_time.dart';
import 'health_connect_gating.dart';
import 'run_catching.dart';

/// Port of the Kotlin `VitalsRepositoryImpl`.
///
/// Public methods convert exceptions to failures via [runCatching] at the
/// boundary; the private permission-gated series reads keep the original
/// throwing flow so [loadVitalsPeriod] composes them as plain awaits.
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
  Future<Result<Set<String>>> missingPermissions() =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        return phase3Permissions.difference(granted);
      });

  @override
  Future<Result<VitalsPeriodData>> loadVitalsPeriod(
    PeriodLoadQuery query,
    VitalsPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runCatching(
          () => _loadVitalsPeriodRaw(query, metric).timeout(healthReadBudget));

  Future<VitalsPeriodData> _loadVitalsPeriodRaw(
    PeriodLoadQuery query,
    VitalsPeriodMetric metric,
  ) async {
    final granted = await _dataSource.grantedIfAvailable();
    final missing = phase3Permissions.difference(granted);
    final w = query.windows;

    // Each series read is independent (shares only the read-only [granted] set
    // and pure window math), so a metric's windows run concurrently rather than
    // one `await` after another. For Year this turns the ALL case's seven
    // full-window reads from seven serial round-trips into one parallel wave —
    // the fix for the "Syncing with Health Connect…" hang.
    switch (metric) {
      case VitalsPeriodMetric.all:
        // Kotlin's ALL loads only the current window (no previous/baseline).
        final c = w.current;
        if (query.range == TimeRange.day) {
          // Day: the raw samples ARE the chart, so read them directly.
          final bloodPressure = _bloodPressure(c.start, c.end, granted);
          final spO2 = _spO2(c.start, c.end, granted);
          final respiratoryRate = _respiratoryRate(c.start, c.end, granted);
          final bodyTemperature = _bodyTemperature(c.start, c.end, granted);
          final vo2Max = _vo2Max(c.start, c.end, granted);
          final bloodGlucose = _bloodGlucose(c.start, c.end, granted);
          final skinTemperature = _skinTemperature(c.start, c.end, granted);
          await Future.wait([
            bloodPressure,
            spO2,
            respiratoryRate,
            bodyTemperature,
            vo2Max,
            bloodGlucose,
            skinTemperature,
          ]);
          return VitalsPeriodData(
            missingVitalsPermissions: missing,
            bloodPressure: await bloodPressure,
            spO2: await spO2,
            respiratoryRate: await respiratoryRate,
            bodyTemperature: await bodyTemperature,
            vo2Max: await vo2Max,
            bloodGlucose: await bloodGlucose,
            skinTemperature: await skinTemperature,
          );
        }
        // Week/month/year: the chart plots one point per day, so read native
        // daily aggregates (bucketed on the Kotlin side) plus each metric's true
        // latest reading — a year of raw records never crosses the channel. The
        // daily point carries its reading count, so the cards' period averages
        // stay count-weighted (no data-quality loss). The display synthesizes
        // its chart points from these; see heart_vitals_overview_display.dart.
        final bpDaily = _bloodPressureDaily(c.start, c.end, granted);
        final spo2Daily = _spO2Daily(c.start, c.end, granted);
        final respDaily = _respiratoryRateDaily(c.start, c.end, granted);
        final bodyDaily = _bodyTemperatureDaily(c.start, c.end, granted);
        final vo2Daily = _vo2MaxDaily(c.start, c.end, granted);
        final glucoseDaily = _bloodGlucoseDaily(c.start, c.end, granted);
        final skinDaily = _skinTemperatureDaily(c.start, c.end, granted);
        final bpLatest = _latestBloodPressure(c.start, c.end, granted);
        final spo2Latest = _latestSpO2(c.start, c.end, granted);
        final respLatest = _latestRespiratoryRate(c.start, c.end, granted);
        final bodyLatest = _latestBodyTemperature(c.start, c.end, granted);
        final vo2Latest = _latestVo2Max(c.start, c.end, granted);
        final glucoseLatest = _latestBloodGlucose(c.start, c.end, granted);
        final skinLatest = _latestSkinTemperature(c.start, c.end, granted);
        await Future.wait([
          bpDaily,
          spo2Daily,
          respDaily,
          bodyDaily,
          vo2Daily,
          glucoseDaily,
          skinDaily,
          bpLatest,
          spo2Latest,
          respLatest,
          bodyLatest,
          vo2Latest,
          glucoseLatest,
          skinLatest,
        ]);
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          bloodPressureDaily: await bpDaily,
          spO2Daily: await spo2Daily,
          respiratoryRateDaily: await respDaily,
          bodyTemperatureDaily: await bodyDaily,
          vo2MaxDaily: await vo2Daily,
          bloodGlucoseDaily: await glucoseDaily,
          skinTemperatureDaily: await skinDaily,
          latestBloodPressure: await bpLatest,
          latestSpO2: await spo2Latest,
          latestRespiratoryRate: await respLatest,
          latestBodyTemperature: await bodyLatest,
          latestVo2Max: await vo2Latest,
          latestBloodGlucose: await glucoseLatest,
          latestSkinTemperature: await skinLatest,
        );
      case VitalsPeriodMetric.bloodPressure:
        final current = _bloodPressure(w.current.start, w.current.end, granted);
        final previous =
            _bloodPressure(w.previous.start, w.previous.end, granted);
        final baseline =
            _bloodPressure(w.baseline.start, w.baseline.end, granted);
        await Future.wait([current, previous, baseline]);
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          bloodPressure: await current,
          previousBloodPressure: await previous,
          baselineBloodPressure: await baseline,
        );
      case VitalsPeriodMetric.spo2:
        final current = _spO2(w.current.start, w.current.end, granted);
        final previous = _spO2(w.previous.start, w.previous.end, granted);
        final baseline = _spO2(w.baseline.start, w.baseline.end, granted);
        await Future.wait([current, previous, baseline]);
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          spO2: await current,
          previousSpO2: await previous,
          baselineSpO2: await baseline,
        );
      case VitalsPeriodMetric.vo2Max:
        final current = _vo2Max(w.current.start, w.current.end, granted);
        final previous = _vo2Max(w.previous.start, w.previous.end, granted);
        final baseline = _vo2Max(w.baseline.start, w.baseline.end, granted);
        await Future.wait([current, previous, baseline]);
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          vo2Max: await current,
          previousVo2Max: await previous,
          baselineVo2Max: await baseline,
        );
      case VitalsPeriodMetric.respiratoryRate:
        final current =
            _respiratoryRate(w.current.start, w.current.end, granted);
        final previous =
            _respiratoryRate(w.previous.start, w.previous.end, granted);
        final baseline =
            _respiratoryRate(w.baseline.start, w.baseline.end, granted);
        await Future.wait([current, previous, baseline]);
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          respiratoryRate: await current,
          previousRespiratoryRate: await previous,
          baselineRespiratoryRate: await baseline,
        );
      case VitalsPeriodMetric.bodyTemperature:
        final current =
            _bodyTemperature(w.current.start, w.current.end, granted);
        final previous =
            _bodyTemperature(w.previous.start, w.previous.end, granted);
        final baseline =
            _bodyTemperature(w.baseline.start, w.baseline.end, granted);
        await Future.wait([current, previous, baseline]);
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          bodyTemperature: await current,
          previousBodyTemperature: await previous,
          baselineBodyTemperature: await baseline,
        );
      case VitalsPeriodMetric.bloodGlucose:
        final current = _bloodGlucose(w.current.start, w.current.end, granted);
        final previous =
            _bloodGlucose(w.previous.start, w.previous.end, granted);
        final baseline =
            _bloodGlucose(w.baseline.start, w.baseline.end, granted);
        await Future.wait([current, previous, baseline]);
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          bloodGlucose: await current,
          previousBloodGlucose: await previous,
          baselineBloodGlucose: await baseline,
        );
      case VitalsPeriodMetric.skinTemperature:
        final current =
            _skinTemperature(w.current.start, w.current.end, granted);
        final previous =
            _skinTemperature(w.previous.start, w.previous.end, granted);
        final baseline =
            _skinTemperature(w.baseline.start, w.baseline.end, granted);
        await Future.wait([current, previous, baseline]);
        return VitalsPeriodData(
          missingVitalsPermissions: missing,
          skinTemperature: await current,
          previousSkinTemperature: await previous,
          baselineSkinTemperature: await baseline,
        );
    }
  }

  @override
  Future<Result<List<BloodPressureEntry>>> loadBloodPressure(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async =>
          _bloodPressure(start, end, await _dataSource.grantedIfAvailable()));

  @override
  Future<Result<List<SpO2Entry>>> loadSpO2(LocalDate start, LocalDate end) =>
      runCatching(() async =>
          _spO2(start, end, await _dataSource.grantedIfAvailable()));

  @override
  Future<Result<List<RespiratoryRateEntry>>> loadRespiratoryRate(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async =>
          _respiratoryRate(start, end, await _dataSource.grantedIfAvailable()));

  @override
  Future<Result<List<BodyTempEntry>>> loadBodyTemperature(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async =>
          _bodyTemperature(start, end, await _dataSource.grantedIfAvailable()));

  @override
  Future<Result<List<Vo2MaxEntry>>> loadVo2Max(LocalDate start, LocalDate end) =>
      runCatching(() async =>
          _vo2Max(start, end, await _dataSource.grantedIfAvailable()));

  @override
  Future<Result<List<BloodGlucoseEntry>>> loadBloodGlucose(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async =>
          _bloodGlucose(start, end, await _dataSource.grantedIfAvailable()));

  @override
  Future<Result<List<SkinTemperatureEntry>>> loadSkinTemperature(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async =>
          _skinTemperature(start, end, await _dataSource.grantedIfAvailable()));

  @override
  Future<Result<bool>> hasVitalsWritePermission(VitalsMeasurementType type) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        return granted.containsAll(vitalsWritePermissions(type));
      });

  @override
  Future<Result<String>> writeVitalsMeasurementEntry(
    VitalsMeasurementWriteRequest request,
  ) =>
      runCatching(() async {
        await _requireWrite(request.type);
        return _dataSource.writeVitalsMeasurementEntry(request);
      });

  @override
  Future<Result<VitalsMeasurementEntry?>> loadVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  ) =>
      runCatching(() => _dataSource.readVitalsMeasurementEntry(type, id));

  @override
  Future<Result<void>> updateVitalsMeasurementEntry(
    String id,
    VitalsMeasurementWriteRequest request,
  ) =>
      runCatching(() async {
        await _requireWrite(request.type);
        return _dataSource.updateVitalsMeasurementEntry(id, request);
      });

  @override
  Future<Result<void>> deleteVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  ) =>
      runCatching(() async {
        await _requireWrite(type);
        return _dataSource.deleteVitalsMeasurementEntry(type, id);
      });

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

  // ── Daily aggregates + window-latest (non-day overview) ────────────────────
  // The daily readers take LocalDate windows (they bucket by local date on the
  // native side); the latest readers return the newest reading in the window.

  Future<List<DailyBloodPressurePoint>> _bloodPressureDaily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readBloodPressure)) return const [];
    return _dataSource.readDailyBloodPressure(start, end);
  }

  Future<List<DailyVitalPoint>> _spO2Daily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readSpO2)) return const [];
    return _dataSource.readDailySpO2(start, end);
  }

  Future<List<DailyVitalPoint>> _respiratoryRateDaily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readRespiratoryRate)) return const [];
    return _dataSource.readDailyRespiratoryRate(start, end);
  }

  Future<List<DailyVitalPoint>> _bodyTemperatureDaily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readBodyTemperature)) return const [];
    return _dataSource.readDailyBodyTemperature(start, end);
  }

  Future<List<DailyVitalPoint>> _vo2MaxDaily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readVo2Max)) return const [];
    return _dataSource.readDailyVo2Max(start, end);
  }

  Future<List<DailyVitalPoint>> _bloodGlucoseDaily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readBloodGlucose)) return const [];
    return _dataSource.readDailyBloodGlucose(start, end);
  }

  Future<List<DailyVitalPoint>> _skinTemperatureDaily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!_dataSource.isSkinTemperatureAvailable()) return const [];
    if (!granted.contains(HcPermissions.readSkinTemperature)) return const [];
    return _dataSource.readDailySkinTemperature(start, end);
  }

  Future<BloodPressureEntry?> _latestBloodPressure(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readBloodPressure)) return null;
    return _dataSource.readLatestBloodPressureInWindow(start, end);
  }

  Future<SpO2Entry?> _latestSpO2(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readSpO2)) return null;
    return _dataSource.readLatestSpO2InWindow(start, end);
  }

  Future<RespiratoryRateEntry?> _latestRespiratoryRate(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readRespiratoryRate)) return null;
    return _dataSource.readLatestRespiratoryRateInWindow(start, end);
  }

  Future<BodyTempEntry?> _latestBodyTemperature(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readBodyTemperature)) return null;
    return _dataSource.readLatestBodyTemperatureInWindow(start, end);
  }

  Future<Vo2MaxEntry?> _latestVo2Max(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readVo2Max)) return null;
    return _dataSource.readLatestVo2MaxInWindow(start, end);
  }

  Future<BloodGlucoseEntry?> _latestBloodGlucose(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readBloodGlucose)) return null;
    return _dataSource.readLatestBloodGlucoseInWindow(start, end);
  }

  Future<SkinTemperatureEntry?> _latestSkinTemperature(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!_dataSource.isSkinTemperatureAvailable()) return null;
    if (!granted.contains(HcPermissions.readSkinTemperature)) return null;
    return _dataSource.readLatestSkinTemperatureInWindow(start, end);
  }
}
