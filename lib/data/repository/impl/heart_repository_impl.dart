import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/model/heart_rate_sample_reduction.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/heart_period_data.dart';
import '../../source/health/health_data_source.dart';
import '../../source/health/health_permissions.dart';
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
      runCatching(() => _loadHeartPeriodRaw(query, metric));

  Future<HeartPeriodData> _loadHeartPeriodRaw(
    PeriodLoadQuery query,
    HeartPeriodMetric metric,
  ) async {
    final granted = await _dataSource.grantedIfAvailable();
    final isDay = query.range == TimeRange.day;
    final windows = query.windows;
    final selected = query.selectedDate;

    switch (metric) {
      case HeartPeriodMetric.all:
        if (isDay) {
          final daySamples = await _daySamples(selected, granted);
          final dayResting = await _dayRestingSamples(selected, granted);
          final dayHrv = await _dayHrvSamples(selected, granted);
          return HeartPeriodData(
            daySamples: daySamples,
            dayRestingSamples: dayResting,
            dayRestingBpm: _averageBpm(dayResting),
            dayHrvSamples: dayHrv,
            dayHrvMs: _averageRmssd(dayHrv),
          );
        }
        return HeartPeriodData(
          dailySummaries:
              await _dailySummaries(windows.current, granted),
          dailyRestingHR: await _dailyRestingHR(windows.current, granted),
          dailyHrv: await _dailyHrv(windows.current, granted),
        );

      case HeartPeriodMetric.averageHeartRate:
        if (isDay) {
          return HeartPeriodData(
            daySamples: await _daySamples(selected, granted),
            previousDaySamples:
                await _daySamples(windows.previous.start, granted),
            baselineDailySummaries:
                await _dailySummaries(windows.baseline, granted),
          );
        }
        return HeartPeriodData(
          dailySummaries: await _dailySummaries(windows.current, granted),
          previousDailySummaries:
              await _dailySummaries(windows.previous, granted),
          baselineDailySummaries:
              await _dailySummaries(windows.baseline, granted),
        );

      case HeartPeriodMetric.restingHeartRate:
        if (isDay) {
          return HeartPeriodData(
            dayRestingSamples: await _dayRestingSamples(selected, granted),
            previousDayRestingBpm:
                await _restingBpm(windows.previous.start, granted),
            baselineDailyRestingHR:
                await _dailyRestingHR(windows.baseline, granted),
          );
        }
        return HeartPeriodData(
          dailyRestingHR: await _dailyRestingHR(windows.current, granted),
          previousDailyRestingHR:
              await _dailyRestingHR(windows.previous, granted),
          baselineDailyRestingHR:
              await _dailyRestingHR(windows.baseline, granted),
        );

      case HeartPeriodMetric.hrv:
        if (isDay) {
          final dayHrv = await _dayHrvSamples(selected, granted);
          return HeartPeriodData(
            dayHrvSamples: dayHrv,
            dayHrvMs: _averageRmssd(dayHrv),
            baselineDailyHrv: await _dailyHrv(windows.baseline, granted),
          );
        }
        return HeartPeriodData(
          dailyHrv: await _dailyHrv(windows.current, granted),
          previousDailyHrv: await _dailyHrv(windows.previous, granted),
          baselineDailyHrv: await _dailyHrv(windows.baseline, granted),
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
