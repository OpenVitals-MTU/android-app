import 'package:drift/drift.dart' show Value;

import '../../core/time/local_date.dart';
import '../../domain/health/health_permissions.dart';
import '../local/open_vitals_database.dart';
import '../repository/impl/health_connect_gating.dart';
import '../source/health/health_data_source.dart';

/// Keeps the daily calories-burned cache current, mirroring
/// [VitalsHistorySyncService] for a single metric.
///
/// Health Connect's `TotalCaloriesBurned` day-by-day aggregate takes 13-24s over
/// a dense year (it re-derives each day's total from the underlying active +
/// basal records) — that read is the entire cost of opening the calories screen
/// on the Year range. So we do it once in the background into the generic daily
/// aggregate cache ([VitalsDailyCacheDao], keyed by [caloriesBurnedCacheMetric]),
/// then keep it current cheaply through Health Connect's Changes API. The screen
/// then reads ~365 SQLite rows instead of waiting on the aggregate.
///
/// Unlike the vitals metrics, the app never *writes* `TotalCaloriesBurned`
/// records (only wearables/other apps do), so there is no write-through path —
/// the Changes-API drain is the only invalidation signal, which is exactly what
/// it is designed for.
class CaloriesHistorySyncService {
  CaloriesHistorySyncService(
    this._cacheDao,
    this._dataSource, {
    DateTime Function() clock = DateTime.now,
    int historyLookbackDays = caloriesCacheLookbackDays,
  })  : _clock = clock,
        _historyLookbackDays = historyLookbackDays;

  final VitalsDailyCacheDao _cacheDao;
  final HealthDataSource _dataSource;
  final DateTime Function() _clock;
  final int _historyLookbackDays;

  /// The Health Connect record type whose changes drive the cache.
  static const String _recordType = 'TotalCaloriesBurned';

  /// The full sync reads history in windows of this many days. A single
  /// multi-year `TotalCaloriesBurned` aggregate is the pathological read (it can
  /// throw or take minutes); a bounded yearly window keeps each read to the
  /// ~24s a dense year costs, and persists progress chunk by chunk.
  static const int _chunkDays = 365;

  Future<void>? _running;

  /// Sync the calories-burned cache. Concurrent calls share one run.
  Future<void> syncAll() =>
      _running ??= _sync().whenComplete(() => _running = null);

  Future<void> _sync() async {
    final granted = await _dataSource.grantedIfAvailable();
    if (!granted.contains(HcPermissions.readTotalCalories)) return;
    try {
      final cursor = await _cacheDao.cursor(caloriesBurnedCacheMetric);
      final token = cursor?.changesToken;
      if (token == null || token.isEmpty) {
        await _fullSync();
      } else {
        await _incrementalSync(token);
      }
    } catch (_) {
      // Best-effort: a failed sync just leaves the screen on its live read and
      // retries on the next open. Never surfaced.
    }
  }

  Future<void> _fullSync() async {
    final today = LocalDate.fromDateTime(_clock());
    final earliest = today.plusDays(-_historyLookbackDays);
    // Register the changes token BEFORE the (slow) history read, so writes that
    // land during it are caught by the next incremental sync rather than lost.
    final freshToken = await _dataSource.getVitalsChangesToken(_recordType);
    // Read the window in bounded yearly chunks — a single multi-year
    // TotalCaloriesBurned aggregate is the read that throws or takes minutes.
    // The newest chunk clears any prior rows; older chunks upsert onto it.
    var cleared = false;
    var chunkEnd = today;
    while (!chunkEnd.isBefore(earliest)) {
      var chunkStart = chunkEnd.plusDays(-(_chunkDays - 1));
      if (chunkStart.isBefore(earliest)) chunkStart = earliest;
      final days = await _readDays(chunkStart, chunkEnd);
      if (!cleared) {
        await _cacheDao.replaceMetric(caloriesBurnedCacheMetric, [
          for (final d in days)
            VitalsDailyAggregatesCompanion.insert(
              metric: caloriesBurnedCacheMetric,
              epochDay: d.epochDay,
              valueSum: d.kcal,
              sampleCount: 1,
              secondarySum: const Value(null),
            ),
        ]);
        cleared = true;
      } else {
        for (final d in days) {
          await _cacheDao.upsertDay(
            metric: caloriesBurnedCacheMetric,
            epochDay: d.epochDay,
            valueSum: d.kcal,
            sampleCount: 1,
          );
        }
      }
      chunkEnd = chunkStart.plusDays(-1);
    }
    if (!cleared) {
      await _cacheDao.replaceMetric(caloriesBurnedCacheMetric, const []);
    }
    await _cacheDao.writeFullSync(
      caloriesBurnedCacheMetric,
      freshToken.isEmpty ? null : freshToken,
      _clock().millisecondsSinceEpoch,
    );
  }

  Future<void> _incrementalSync(String token) async {
    var current = token;
    while (true) {
      final batch = await _dataSource.getVitalsChanges(current);
      // A deletion carries no date, and an expired token means we cannot trust
      // the delta — either way, rebuild from scratch.
      if (batch.tokenExpired || batch.hasDeletions) {
        await _fullSync();
        return;
      }
      for (final day in batch.upsertedDays) {
        await _recomputeDay(day);
      }
      current = batch.nextToken;
      await _cacheDao.writeToken(caloriesBurnedCacheMetric, current);
      if (!batch.hasMore) break;
    }
  }

  Future<void> _recomputeDay(LocalDate day) async {
    final days = await _readDays(day, day);
    final kcal = days.isEmpty ? 0.0 : days.first.kcal;
    if (kcal <= 0.0) {
      await _cacheDao.deleteDay(caloriesBurnedCacheMetric, day.epochDay);
      return;
    }
    await _cacheDao.upsertDay(
      metric: caloriesBurnedCacheMetric,
      epochDay: day.epochDay,
      valueSum: kcal,
      sampleCount: 1,
    );
  }

  /// The days in `[start, end]` that recorded calories burned. Matches the
  /// screen's own read (`includeHydration: false`, recorded totals only), and
  /// keeps the cache lean by storing only days with a positive burn.
  ///
  /// Values are summed per calendar day: Health Connect's `Duration.ofDays(1)`
  /// slicing drifts across DST transitions, so it can hand back two 24h buckets
  /// that map to the same local date. Summing them (a) avoids a duplicate
  /// primary key that would abort the whole sync, and (b) matches how the live
  /// heatmap already folds same-date values (`_valuesByDate`).
  Future<List<_CalorieDay>> _readDays(LocalDate start, LocalDate end) async {
    final nutrition = await _dataSource.readDailyNutrition(
      start,
      end,
      includeHydration: false,
    );
    final byDay = <int, double>{};
    for (final n in nutrition) {
      if (n.caloriesBurnedKcal > 0.0) {
        byDay[n.date.epochDay] =
            (byDay[n.date.epochDay] ?? 0.0) + n.caloriesBurnedKcal;
      }
    }
    return [
      for (final e in byDay.entries) _CalorieDay(e.key, e.value),
    ];
  }
}

class _CalorieDay {
  const _CalorieDay(this.epochDay, this.kcal);
  final int epochDay;
  final double kcal;
}
