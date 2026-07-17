import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/model/heart_rate_sample_reduction.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/heart_period_data.dart';
import '../../source/health/health_data_source.dart';
import '../../../domain/health/health_permissions.dart';
import '../contract/heart_repository.dart';
import 'repository_time.dart';
import 'health_connect_gating.dart';
import 'run_catching.dart';
import '../../../core/stats/stats.dart';

/// Port of the Kotlin `HeartRepositoryImpl`. Thin, permission-aware facade over
/// [HealthDataSource]; reads degrade to empty/null when the backing permission
/// is not granted (matching the Kotlin `readXPermission !in granted` guards).
///
/// Public methods convert exceptions to failures via [runCatching] at the
/// boundary; the private gated reads keep the original throwing flow so
/// internal composition stays plain awaits.
class HeartRepositoryImpl implements HeartRepository {
  HeartRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  @override
  Future<Result<HeartPeriodData>> loadHeartPeriod(
    PeriodLoadQuery query,
    HeartPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runCatching(
          () => _loadHeartPeriodRaw(query, metric).timeout(healthReadBudget));

  Future<HeartPeriodData> _loadHeartPeriodRaw(
    PeriodLoadQuery query,
    HeartPeriodMetric metric,
  ) async {
    final granted = await _dataSource.grantedIfAvailable();
    final isDay = query.range == TimeRange.day;
    final windows = query.windows;
    final selected = query.selectedDate;

    // The windows within a metric are independent reads, so they run
    // concurrently (start the futures, then `Future.wait`) instead of one
    // `await` after the next — the same within-repo parallelism the vitals half
    // gets, so a non-day load is one round-trip deep rather than three.
    switch (metric) {
      case HeartPeriodMetric.all:
        if (isDay) {
          final daySamples = _daySamples(selected, granted);
          final dayResting = _dayRestingSamples(selected, granted);
          final dayHrv = _dayHrvSamples(selected, granted);
          await Future.wait([daySamples, dayResting, dayHrv]);
          return HeartPeriodData(
            daySamples: await daySamples,
            dayRestingSamples: await dayResting,
            dayRestingBpm: _averageBpm(await dayResting),
            dayHrvSamples: await dayHrv,
            dayHrvMs: _averageRmssd(await dayHrv),
          );
        }
        final summaries = _dailySummaries(windows.current, granted);
        final resting = _dailyRestingHR(windows.current, granted);
        final hrv = _dailyHrv(windows.current, granted);
        await Future.wait([summaries, resting, hrv]);
        return HeartPeriodData(
          dailySummaries: await summaries,
          dailyRestingHR: await resting,
          dailyHrv: await hrv,
        );

      case HeartPeriodMetric.averageHeartRate:
        if (isDay) {
          final current = _daySamples(selected, granted);
          final previous = _daySamples(windows.previous.start, granted);
          final baseline = _dailySummaries(windows.baseline, granted);
          await Future.wait([current, previous, baseline]);
          return HeartPeriodData(
            daySamples: await current,
            previousDaySamples: await previous,
            baselineDailySummaries: await baseline,
          );
        }
        final current = _dailySummaries(windows.current, granted);
        final previous = _dailySummaries(windows.previous, granted);
        final baseline = _dailySummaries(windows.baseline, granted);
        await Future.wait([current, previous, baseline]);
        return HeartPeriodData(
          dailySummaries: await current,
          previousDailySummaries: await previous,
          baselineDailySummaries: await baseline,
        );

      case HeartPeriodMetric.restingHeartRate:
        if (isDay) {
          final current = _dayRestingSamples(selected, granted);
          final previous = _restingBpm(windows.previous.start, granted);
          final baseline = _dailyRestingHR(windows.baseline, granted);
          await Future.wait([current, previous, baseline]);
          return HeartPeriodData(
            dayRestingSamples: await current,
            previousDayRestingBpm: await previous,
            baselineDailyRestingHR: await baseline,
          );
        }
        final current = _dailyRestingHR(windows.current, granted);
        final previous = _dailyRestingHR(windows.previous, granted);
        final baseline = _dailyRestingHR(windows.baseline, granted);
        await Future.wait([current, previous, baseline]);
        return HeartPeriodData(
          dailyRestingHR: await current,
          previousDailyRestingHR: await previous,
          baselineDailyRestingHR: await baseline,
        );

      case HeartPeriodMetric.hrv:
        if (isDay) {
          final dayHrvFuture = _dayHrvSamples(selected, granted);
          final baseline = _dailyHrv(windows.baseline, granted);
          await Future.wait([dayHrvFuture, baseline]);
          final dayHrv = await dayHrvFuture;
          return HeartPeriodData(
            dayHrvSamples: dayHrv,
            dayHrvMs: _averageRmssd(dayHrv),
            baselineDailyHrv: await baseline,
          );
        }
        final current = _dailyHrv(windows.current, granted);
        final previous = _dailyHrv(windows.previous, granted);
        final baseline = _dailyHrv(windows.baseline, granted);
        await Future.wait([current, previous, baseline]);
        return HeartPeriodData(
          dailyHrv: await current,
          previousDailyHrv: await previous,
          baselineDailyHrv: await baseline,
        );
    }
  }

  // ── Contract reads ────────────────────────────────────────────────────────

  @override
  Future<Result<List<HeartRateSample>>> loadHeartRateSamplesForDay(
    LocalDate date,
  ) =>
      runCatching(() async =>
          _daySamples(date, await _dataSource.grantedIfAvailable()));

  @override
  Future<Result<List<HeartRateSample>>> loadRawHeartRateSamplesForDayGraph(
    LocalDate date,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readHeartRate)) return const [];
        return _dataSource.readRawHeartRateSamples(
          localDayStart(date),
          localDayEnd(date),
        );
      });

  @override
  Future<Result<List<HeartRateSample>>> loadHeartRateSamples(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readHeartRate)) return const [];
        final samples = await _dataSource.readHeartRateSamples(
          localDayStart(start),
          localDayEnd(end),
        );
        // Reduce per day, not across the whole range: the chart draws one series
        // per day, so a range-wide reduction would thin early days away to
        // nothing.
        final byDay = <LocalDate, List<HeartRateSample>>{};
        for (final sample in samples) {
          byDay
              .putIfAbsent(instantToLocalDate(sample.time), () => [])
              .add(sample);
        }
        return [
          for (final daySamples in byDay.values) ...daySamples.reducedForChart(),
        ];
      });

  @override
  Future<Result<List<HeartRateSample>>> loadHeartRateSamplesInstant(
    DateTime start,
    DateTime end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readHeartRate)) return const [];
        if (!end.isAfter(start)) return const [];

        // Just the window. How Health Connect stores heart rate -- that a series
        // record hides its samples behind its own boundary, and that aggregation
        // is the way out when it does -- is settled natively, in
        // HealthConnectSeries.kt, where the Health Connect SDK actually is. This
        // used to guess at it from Dart, across a Pigeon channel, and got it
        // wrong: an activity whose beats sat inside a longer record read as
        // "Not available".
        final samples = await _dataSource.readRawHeartRateSamples(start, end);
        return samples.reducedForChart();
      });

  @override
  Future<Result<List<HeartRateSummary>>> loadDailyHeartRateSummaries(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async => _dailySummaries(
          DatePeriod(start, end), await _dataSource.grantedIfAvailable()));

  @override
  Future<Result<int?>> loadRestingHeartRate(LocalDate date) =>
      runCatching(() async =>
          _restingBpm(date, await _dataSource.grantedIfAvailable()));

  @override
  Future<Result<List<DailyRestingHR>>> loadDailyRestingHR(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async => _dailyRestingHR(
          DatePeriod(start, end), await _dataSource.grantedIfAvailable()));

  @override
  Future<Result<double?>> loadHrvRmssd(LocalDate date) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readHrv)) return null;
        return _dataSource.readHrvRmssd(date);
      });

  @override
  Future<Result<List<HrvSample>>> loadHrvSamples(DateTime start, DateTime end) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readHrv)) return const [];
        return _dataSource.readHrvSamples(start, end);
      });

  @override
  Future<Result<List<DailyHrv>>> loadDailyHRV(LocalDate start, LocalDate end) =>
      runCatching(() async => _dailyHrv(
          DatePeriod(start, end), await _dataSource.grantedIfAvailable()));

  // ── Private gated reads ─────────────────────────────────────────────────

  Future<List<HeartRateSample>> _daySamples(
    LocalDate date,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readHeartRate)) return const [];
    return _dataSource.readRawHeartRateSamples(
      localDayStart(date),
      localDayEnd(date),
    );
  }

  Future<List<HeartRateSummary>> _dailySummaries(
    DatePeriod period,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readHeartRate)) return const [];
    return _dataSource.readDailyHeartRateSummaries(period.start, period.end);
  }

  Future<List<RestingHeartRateSample>> _dayRestingSamples(
    LocalDate date,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readRestingHeartRate)) return const [];
    return _dataSource.readRestingHeartRateSamples(
      localDayStart(date),
      localDayEnd(date),
    );
  }

  Future<int?> _restingBpm(LocalDate date, Set<String> granted) async {
    if (!granted.contains(HcPermissions.readRestingHeartRate)) return null;
    return _dataSource.readRestingHeartRate(date);
  }

  Future<List<DailyRestingHR>> _dailyRestingHR(
    DatePeriod period,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readRestingHeartRate)) return const [];
    return _dataSource.readDailyRestingHR(period.start, period.end);
  }

  Future<List<HrvSample>> _dayHrvSamples(
    LocalDate date,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readHrv)) return const [];
    return _dataSource.readHrvSamples(localDayStart(date), localDayEnd(date));
  }

  Future<List<DailyHrv>> _dailyHrv(
    DatePeriod period,
    Set<String> granted,
  ) async {
    if (!granted.contains(HcPermissions.readHrv)) return const [];
    return _dataSource.readDailyHRV(period.start, period.end);
  }

  /// Null, never zero: "no resting-HR reading today" is not "0 bpm", and four
  /// consumers on the heart screens branch on exactly that null.
  int? _averageBpm(List<RestingHeartRateSample> samples) =>
      average(samples.map((s) => s.beatsPerMinute))?.round();

  double? _averageRmssd(List<HrvSample> samples) =>
      average(samples.map((s) => s.rmssdMs));
}
