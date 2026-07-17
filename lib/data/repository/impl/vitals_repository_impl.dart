import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/model/vitals_models.dart';
import '../../../domain/query/vitals_period_data.dart';
import '../../local/open_vitals_database.dart';
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
  VitalsRepositoryImpl(this._dataSource, {VitalsDailyCacheDao? cacheDao})
      // A private field backing a public named parameter (`cacheDao:`, used by
      // data_providers and tests) cannot be an initializing formal — that would
      // rename the parameter `_cacheDao`.
      // ignore: prefer_initializing_formals
      : _cacheDao = cacheDao;

  final HealthDataSource _dataSource;

  /// The local daily-aggregate cache. When a metric has been synced (a cursor
  /// exists), the non-day overview reads its daily points from here instead of
  /// re-reading a year of raw records from Health Connect. Null in tests that
  /// exercise the live path.
  final VitalsDailyCacheDao? _cacheDao;

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
        //
        // Each daily read gets its OWN budget: a metric with no HC aggregate and
        // a densely-sampled year (respiratory rate can be 40s+) degrades to empty
        // and is flagged in [timedOut], instead of sinking the whole overview.
        final timedOut = <VitalsPeriodMetric>{};
        Future<List<T>> budgeted<T>(
          VitalsPeriodMetric metric,
          Future<List<T>> read,
        ) =>
            read.timeout(vitalsMetricBudget, onTimeout: () {
              timedOut.add(metric);
              return <T>[];
            });
        final bpDaily = budgeted(VitalsPeriodMetric.bloodPressure,
            _bloodPressureDaily(c.start, c.end, granted));
        final spo2Daily =
            budgeted(VitalsPeriodMetric.spo2, _spO2Daily(c.start, c.end, granted));
        final respDaily = budgeted(VitalsPeriodMetric.respiratoryRate,
            _respiratoryRateDaily(c.start, c.end, granted));
        final bodyDaily = budgeted(VitalsPeriodMetric.bodyTemperature,
            _bodyTemperatureDaily(c.start, c.end, granted));
        final vo2Daily = budgeted(
            VitalsPeriodMetric.vo2Max, _vo2MaxDaily(c.start, c.end, granted));
        final glucoseDaily = budgeted(VitalsPeriodMetric.bloodGlucose,
            _bloodGlucoseDaily(c.start, c.end, granted));
        final skinDaily = budgeted(VitalsPeriodMetric.skinTemperature,
            _skinTemperatureDaily(c.start, c.end, granted));
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
          timedOutMetrics: timedOut,
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
        final id = await _dataSource.writeVitalsMeasurementEntry(request);
        await _patchCachedDays(
            request.type, {LocalDate.fromDateTime(request.time)});
        return id;
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
        // Capture the pre-edit day before the update: an edit can move a reading
        // across midnight, leaving the old day's cached mean stale otherwise.
        final oldDay = await _dayOfEntry(request.type, id);
        await _dataSource.updateVitalsMeasurementEntry(id, request);
        await _patchCachedDays(request.type, {
          LocalDate.fromDateTime(request.time),
          ?oldDay,
        });
      });

  @override
  Future<Result<void>> deleteVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  ) =>
      runCatching(() async {
        await _requireWrite(type);
        // Delete carries no timestamp, so capture the day before removing it.
        final day = await _dayOfEntry(type, id);
        await _dataSource.deleteVitalsMeasurementEntry(type, id);
        await _patchCachedDays(type, {?day});
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

  // ── Daily-cache write-through ──────────────────────────────────────────────
  // After the app's own write/update/delete, refresh only the affected day(s) in
  // the daily-aggregate cache so long-range charts reflect the edit immediately,
  // instead of waiting for the next Changes-API drain when the overview reopens.
  // This is layered on top of that drain, which stays the source of truth: it
  // never touches the changes token, so the next incremental sync recomputes the
  // same day identically (or full-rebuilds on a deletion, as it already does).

  static VitalsPeriodMetric _cacheMetricFor(VitalsMeasurementType type) =>
      switch (type) {
        VitalsMeasurementType.bloodPressure => VitalsPeriodMetric.bloodPressure,
        VitalsMeasurementType.spo2 => VitalsPeriodMetric.spo2,
        VitalsMeasurementType.respiratoryRate =>
          VitalsPeriodMetric.respiratoryRate,
        VitalsMeasurementType.bodyTemperature =>
          VitalsPeriodMetric.bodyTemperature,
      };

  /// The local day an existing entry sits on, or null when there is no cache to
  /// patch (skips the extra read on the live/test path) or the entry is gone.
  Future<LocalDate?> _dayOfEntry(VitalsMeasurementType type, String id) async {
    if (_cacheDao == null) return null;
    final entry = await _dataSource.readVitalsMeasurementEntry(type, id);
    return entry == null ? null : LocalDate.fromDateTime(entry.time);
  }

  /// Best-effort: a failure here must never fail the write (a surfaced failure
  /// could drive a retry and duplicate the record); the drain reconciles later.
  Future<void> _patchCachedDays(
    VitalsMeasurementType type,
    Set<LocalDate> days,
  ) async {
    final dao = _cacheDao;
    if (dao == null || days.isEmpty) return;
    try {
      final metric = _cacheMetricFor(type);
      // Only patch a metric that has already been synced once — mirrors
      // [_cachedDaily]; otherwise we'd seed partial rows the reader would trust.
      if (await dao.cursor(metric.name) == null) return;
      for (final day in days) {
        await _recomputeCachedDay(dao, metric, type, day);
      }
    } catch (_) {
      // Swallowed: the write already succeeded and the Changes-API drain will
      // reconcile this day on the next overview open.
    }
  }

  /// Re-read one day's aggregate from Health Connect and upsert it (or drop the
  /// row when the day is now empty) — the single-day form of
  /// [VitalsHistorySyncService]'s recompute, using the same sum mapping so cached
  /// and drained rows are identical.
  Future<void> _recomputeCachedDay(
    VitalsDailyCacheDao dao,
    VitalsPeriodMetric metric,
    VitalsMeasurementType type,
    LocalDate day,
  ) async {
    final epochDay = day.epochDay;
    switch (type) {
      case VitalsMeasurementType.bloodPressure:
        final points = await _dataSource.readDailyBloodPressure(day, day);
        if (points.isEmpty) {
          await dao.deleteDay(metric.name, epochDay);
          return;
        }
        final p = points.first;
        await dao.upsertDay(
          metric: metric.name,
          epochDay: epochDay,
          valueSum: p.systolic * p.count,
          secondarySum: p.diastolic * p.count,
          sampleCount: p.count,
        );
      case VitalsMeasurementType.spo2:
        await _upsertSingleDay(
            dao, metric, epochDay, await _dataSource.readDailySpO2(day, day));
      case VitalsMeasurementType.respiratoryRate:
        await _upsertSingleDay(dao, metric, epochDay,
            await _dataSource.readDailyRespiratoryRate(day, day));
      case VitalsMeasurementType.bodyTemperature:
        await _upsertSingleDay(dao, metric, epochDay,
            await _dataSource.readDailyBodyTemperature(day, day));
    }
  }

  Future<void> _upsertSingleDay(
    VitalsDailyCacheDao dao,
    VitalsPeriodMetric metric,
    int epochDay,
    List<DailyVitalPoint> points,
  ) async {
    if (points.isEmpty) {
      await dao.deleteDay(metric.name, epochDay);
      return;
    }
    final p = points.first;
    await dao.upsertDay(
      metric: metric.name,
      epochDay: epochDay,
      valueSum: p.value * p.count,
      sampleCount: p.count,
    );
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

  /// Cached daily points for a single-value metric, or null when the metric has
  /// not been synced yet (no cursor) — the caller then reads live.
  Future<List<DailyVitalPoint>?> _cachedDaily(
    VitalsPeriodMetric metric,
    LocalDate start,
    LocalDate end,
  ) async {
    final dao = _cacheDao;
    if (dao == null) return null;
    if (await dao.cursor(metric.name) == null) return null;
    final rows =
        await dao.aggregatesBetween(metric.name, start.epochDay, end.epochDay);
    return [
      for (final r in rows)
        DailyVitalPoint(
          date: LocalDate.fromEpochDay(r.epochDay),
          value: r.valueSum / r.sampleCount,
          count: r.sampleCount,
        ),
    ];
  }

  Future<List<DailyBloodPressurePoint>> _bloodPressureDaily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readBloodPressure)) return const [];
    final dao = _cacheDao;
    if (dao != null &&
        await dao.cursor(VitalsPeriodMetric.bloodPressure.name) != null) {
      final rows = await dao.aggregatesBetween(
          VitalsPeriodMetric.bloodPressure.name, start.epochDay, end.epochDay);
      return [
        for (final r in rows)
          DailyBloodPressurePoint(
            date: LocalDate.fromEpochDay(r.epochDay),
            systolic: r.valueSum / r.sampleCount,
            diastolic: (r.secondarySum ?? 0) / r.sampleCount,
            count: r.sampleCount,
          ),
      ];
    }
    return _dataSource.readDailyBloodPressure(start, end);
  }

  Future<List<DailyVitalPoint>> _spO2Daily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readSpO2)) return const [];
    return await _cachedDaily(VitalsPeriodMetric.spo2, start, end) ??
        _dataSource.readDailySpO2(start, end);
  }

  Future<List<DailyVitalPoint>> _respiratoryRateDaily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readRespiratoryRate)) return const [];
    return await _cachedDaily(VitalsPeriodMetric.respiratoryRate, start, end) ??
        _dataSource.readDailyRespiratoryRate(start, end);
  }

  Future<List<DailyVitalPoint>> _bodyTemperatureDaily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readBodyTemperature)) return const [];
    return await _cachedDaily(VitalsPeriodMetric.bodyTemperature, start, end) ??
        _dataSource.readDailyBodyTemperature(start, end);
  }

  Future<List<DailyVitalPoint>> _vo2MaxDaily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readVo2Max)) return const [];
    return await _cachedDaily(VitalsPeriodMetric.vo2Max, start, end) ??
        _dataSource.readDailyVo2Max(start, end);
  }

  Future<List<DailyVitalPoint>> _bloodGlucoseDaily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readBloodGlucose)) return const [];
    return await _cachedDaily(VitalsPeriodMetric.bloodGlucose, start, end) ??
        _dataSource.readDailyBloodGlucose(start, end);
  }

  Future<List<DailyVitalPoint>> _skinTemperatureDaily(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) async {
    if (!_dataSource.isSkinTemperatureAvailable()) return const [];
    if (!granted.contains(HcPermissions.readSkinTemperature)) return const [];
    return await _cachedDaily(VitalsPeriodMetric.skinTemperature, start, end) ??
        _dataSource.readDailySkinTemperature(start, end);
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
