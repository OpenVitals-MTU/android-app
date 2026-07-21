import 'package:drift/drift.dart' show Value;
import 'package:flutter/foundation.dart';

import '../../core/time/local_date.dart';
import '../../domain/health/health_permissions.dart';
import '../../domain/model/vitals_models.dart';
import '../local/open_vitals_database.dart';
import '../repository/contract/vitals_repository.dart';
import '../repository/impl/health_connect_gating.dart';
import '../source/health/health_data_source.dart';

/// Keeps the local daily-vitals cache ([VitalsDailyAggregates]) in step with
/// Health Connect so long-range charts read ~365 cached rows instead of a year
/// of raw records.
///
/// Densely-sampled metrics with no HC aggregate (respiratory rate) are read raw
/// ONCE per metric — the full sync, in the background — then kept current
/// cheaply via the Changes API: each poll recomputes only the days that changed.
/// A deletion (id-only, no date) or an expired token triggers a full rebuild of
/// that metric. Storing valueSum/sampleCount (not the mean) makes every recompute
/// exact.
class VitalsHistorySyncService {
  VitalsHistorySyncService(
    this._cacheDao,
    this._dataSource, {
    DateTime Function() clock = DateTime.now,
    int historyLookbackDays = 730,
    // Private fields backing public named parameters (`clock:`,
    // `historyLookbackDays:`) cannot be initializing formals without renaming the
    // parameters.
  })  : _clock = clock, // ignore: prefer_initializing_formals
        _historyLookbackDays = // ignore: prefer_initializing_formals
            historyLookbackDays;

  final VitalsDailyCacheDao _cacheDao;
  final HealthDataSource _dataSource;
  final DateTime Function() _clock;
  final int _historyLookbackDays;

  Future<void>? _running;

  /// Sync every permitted metric. Concurrent calls share one run.
  Future<void> syncAll() =>
      _running ??= _syncAll().whenComplete(() => _running = null);

  Future<void> _syncAll() async {
    final granted = await _dataSource.grantedIfAvailable();
    await Future.wait([
      for (final metric in _metrics())
        _syncMetric(metric, granted).catchError((Object e, StackTrace s) {
          // One metric's failure must not abort the others; it retries next run.
          // Log it, though — an unlogged swallow is the exact failure mode the
          // native _catch guard argues against (silent forever-retry).
          debugPrint('VitalsHistorySyncService: ${metric.metric.name} sync '
              'failed, will retry next run: $e\n$s');
        }),
    ]);
  }

  Future<void> _syncMetric(_MetricSync m, Set<String> granted) async {
    if (!granted.contains(m.readPermission)) return;
    if (m.requiresSkinTempFeature &&
        !_dataSource.isSkinTemperatureAvailable()) {
      return;
    }

    final cursor = await _cacheDao.cursor(m.metric.name);
    final token = cursor?.changesToken;
    if (token == null || token.isEmpty) {
      await _fullSync(m);
      return;
    }
    await _incrementalSync(m, token);
  }

  Future<void> _fullSync(_MetricSync m) async {
    final today = LocalDate.fromDateTime(_clock());
    final start = today.plusDays(-_historyLookbackDays);
    // Register the changes token BEFORE the (slow) history read, so writes that
    // land during it are caught by the next incremental sync rather than lost
    // (they would otherwise be in neither this snapshot nor the token's delta).
    // Mirrors CaloriesHistorySyncService._fullSync.
    final freshToken = await _dataSource.getVitalsChangesToken(m.recordType);
    final aggregates = await m.read(start, today);
    await _cacheDao.replaceMetric(m.metric.name, [
      for (final a in aggregates)
        VitalsDailyAggregatesCompanion.insert(
          metric: m.metric.name,
          epochDay: a.epochDay,
          valueSum: a.valueSum,
          sampleCount: a.sampleCount,
          secondarySum: Value(a.secondarySum),
        ),
    ]);
    await _cacheDao.writeFullSync(
      m.metric.name,
      freshToken.isEmpty ? null : freshToken,
      _clock().millisecondsSinceEpoch,
    );
  }

  Future<void> _incrementalSync(_MetricSync m, String token) async {
    var current = token;
    while (true) {
      final batch = await _dataSource.getVitalsChanges(current);
      // A deletion carries no date, and an expired token means we can't trust
      // the delta — either way, rebuild the metric from scratch.
      if (batch.tokenExpired || batch.hasDeletions) {
        await _fullSync(m);
        return;
      }
      for (final day in batch.upsertedDays) {
        await _recomputeDay(m, day);
      }
      current = batch.nextToken;
      await _cacheDao.writeToken(m.metric.name, current);
      if (!batch.hasMore) break;
    }
  }

  Future<void> _recomputeDay(_MetricSync m, LocalDate day) async {
    final aggregates = await m.read(day, day);
    if (aggregates.isEmpty) {
      await _cacheDao.deleteDay(m.metric.name, day.epochDay);
      return;
    }
    final a = aggregates.first;
    await _cacheDao.upsertDay(
      metric: m.metric.name,
      epochDay: a.epochDay,
      valueSum: a.valueSum,
      sampleCount: a.sampleCount,
      secondarySum: a.secondarySum,
    );
  }

  List<_MetricSync> _metrics() => [
        _MetricSync(
          VitalsPeriodMetric.respiratoryRate,
          'RespiratoryRate',
          HcPermissions.readRespiratoryRate,
          (s, e) async =>
              _single(await _dataSource.readDailyRespiratoryRate(s, e)),
        ),
        _MetricSync(
          VitalsPeriodMetric.spo2,
          'OxygenSaturation',
          HcPermissions.readSpO2,
          (s, e) async => _single(await _dataSource.readDailySpO2(s, e)),
        ),
        _MetricSync(
          VitalsPeriodMetric.bodyTemperature,
          'BodyTemperature',
          HcPermissions.readBodyTemperature,
          (s, e) async =>
              _single(await _dataSource.readDailyBodyTemperature(s, e)),
        ),
        _MetricSync(
          VitalsPeriodMetric.vo2Max,
          'Vo2Max',
          HcPermissions.readVo2Max,
          (s, e) async => _single(await _dataSource.readDailyVo2Max(s, e)),
        ),
        _MetricSync(
          VitalsPeriodMetric.bloodGlucose,
          'BloodGlucose',
          HcPermissions.readBloodGlucose,
          (s, e) async => _single(await _dataSource.readDailyBloodGlucose(s, e)),
        ),
        _MetricSync(
          VitalsPeriodMetric.skinTemperature,
          'SkinTemperature',
          HcPermissions.readSkinTemperature,
          (s, e) async =>
              _single(await _dataSource.readDailySkinTemperature(s, e)),
          requiresSkinTempFeature: true,
        ),
        _MetricSync(
          VitalsPeriodMetric.bloodPressure,
          'BloodPressure',
          HcPermissions.readBloodPressure,
          (s, e) async => [
            for (final p in await _dataSource.readDailyBloodPressure(s, e))
              _DayAgg(
                p.date.epochDay,
                p.systolic * p.count,
                p.count,
                p.diastolic * p.count,
              ),
          ],
        ),
      ];

  static List<_DayAgg> _single(List<DailyVitalPoint> points) => [
        for (final p in points) _DayAgg(p.date.epochDay, p.value * p.count, p.count),
      ];
}

/// A day's cached aggregate: sums (not means) so recomputes stay exact.
class _DayAgg {
  const _DayAgg(this.epochDay, this.valueSum, this.sampleCount,
      [this.secondarySum]);

  final int epochDay;
  final double valueSum;
  final int sampleCount;
  final double? secondarySum;
}

/// Wires a cache metric to its Health Connect record type, permission and the
/// daily read that produces its aggregates.
class _MetricSync {
  const _MetricSync(
    this.metric,
    this.recordType,
    this.readPermission,
    this.read, {
    this.requiresSkinTempFeature = false,
  });

  final VitalsPeriodMetric metric;
  final String recordType;
  final String readPermission;
  final Future<List<_DayAgg>> Function(LocalDate start, LocalDate end) read;
  final bool requiresSkinTempFeature;
}
