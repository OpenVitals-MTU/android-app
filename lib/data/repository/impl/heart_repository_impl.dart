import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/model/heart_rate_sample_reduction.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/heart_period_data.dart';
import '../../../health/health_data_source.dart';
import '../../../health/health_permissions.dart';
import '../contract/heart_repository.dart';
import 'repository_time.dart';

/// Port of the Kotlin `HeartRepositoryImpl`. Thin, permission-aware facade over
/// [HealthDataSource]; reads degrade to empty/null when the backing permission
/// is not granted (matching the Kotlin `readXPermission !in granted` guards).
class HeartRepositoryImpl implements HeartRepository {
  HeartRepositoryImpl(this._dataSource);

  /// Health Connect filters series records by their record boundary, not each nested
  /// sample. Gadgetbridge can group roughly an hour of samples into one
  /// HeartRateRecord, so a record that starts before a workout can still contain
  /// samples from the start of that workout.
  static const _heartRateSeriesLookback = Duration(hours: 1);

  final HealthDataSource _dataSource;

  Future<Set<String>> _grantedIfAvailable() async =>
      _dataSource.cachedAvailability == HealthConnectAvailability.available
          ? _dataSource.grantedPermissions()
          : <String>{};

  @override
  Future<HeartPeriodData> loadHeartPeriod(
    PeriodLoadQuery query,
    HeartPeriodMetric metric, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final granted = await _grantedIfAvailable();
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
  Future<List<HeartRateSample>> loadHeartRateSamplesForDay(LocalDate date) async =>
      _daySamples(date, await _grantedIfAvailable());

  @override
  Future<List<HeartRateSample>> loadRawHeartRateSamplesForDayGraph(
    LocalDate date,
  ) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readHeartRate)) return const [];
    return _dataSource.readRawHeartRateSamples(
      localDayStart(date),
      localDayEnd(date),
    );
  }

  @override
  Future<List<HeartRateSample>> loadHeartRateSamples(
    LocalDate start,
    LocalDate end,
  ) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readHeartRate)) return const [];
    final samples = await _dataSource.readHeartRateSamples(
      localDayStart(start),
      localDayEnd(end),
    );
    // Reduce per day, not across the whole range: the chart draws one series per
    // day, so a range-wide reduction would thin early days away to nothing.
    final byDay = <LocalDate, List<HeartRateSample>>{};
    for (final sample in samples) {
      byDay.putIfAbsent(instantToLocalDate(sample.time), () => []).add(sample);
    }
    return [
      for (final daySamples in byDay.values) ...daySamples.reducedForChart(),
    ];
  }

  @override
  Future<List<HeartRateSample>> loadHeartRateSamplesInstant(
    DateTime start,
    DateTime end,
  ) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readHeartRate)) return const [];
    if (!end.isAfter(start)) return const [];

    // Health Connect filters series records by their record boundary, not each nested
    // sample. Gadgetbridge can group roughly an hour of samples into one HeartRateRecord,
    // so a record that starts before a workout can still contain samples from the start
    // of that workout -- read back an hour, then filter to the window we actually want.
    final raw = await _dataSource.readRawHeartRateSamples(
      start.subtract(_heartRateSeriesLookback),
      end,
    );
    final windowed = raw
        .where((s) => !s.time.isBefore(start) && s.time.isBefore(end))
        .toList()
      ..sort((a, b) => a.time.compareTo(b.time));
    return windowed.reducedForChart();
  }

  @override
  Future<List<HeartRateSummary>> loadDailyHeartRateSummaries(
    LocalDate start,
    LocalDate end,
  ) async =>
      _dailySummaries(DatePeriod(start, end), await _grantedIfAvailable());

  @override
  Future<int?> loadRestingHeartRate(LocalDate date) async =>
      _restingBpm(date, await _grantedIfAvailable());

  @override
  Future<List<DailyRestingHR>> loadDailyRestingHR(
    LocalDate start,
    LocalDate end,
  ) async =>
      _dailyRestingHR(DatePeriod(start, end), await _grantedIfAvailable());

  @override
  Future<double?> loadHrvRmssd(LocalDate date) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readHrv)) return null;
    return _dataSource.readHrvRmssd(date);
  }

  @override
  Future<List<HrvSample>> loadHrvSamples(DateTime start, DateTime end) async {
    final granted = await _grantedIfAvailable();
    if (!granted.contains(HcPermissions.readHrv)) return const [];
    return _dataSource.readHrvSamples(start, end);
  }

  @override
  Future<List<DailyHrv>> loadDailyHRV(LocalDate start, LocalDate end) async =>
      _dailyHrv(DatePeriod(start, end), await _grantedIfAvailable());

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

  int? _averageBpm(List<RestingHeartRateSample> samples) {
    if (samples.isEmpty) return null;
    final sum = samples.fold<int>(0, (a, s) => a + s.beatsPerMinute);
    return (sum / samples.length).round();
  }

  double? _averageRmssd(List<HrvSample> samples) {
    if (samples.isEmpty) return null;
    final sum = samples.fold<double>(0, (a, s) => a + s.rmssdMs);
    return sum / samples.length;
  }
}
