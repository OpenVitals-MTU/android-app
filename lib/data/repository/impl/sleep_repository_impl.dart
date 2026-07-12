import '../../../core/period/period_load_query.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/model/sleep_models.dart';
import '../../../domain/model/sleep_session_merging.dart';
import '../../../domain/preferences/sleep_range_mode.dart';
import '../../../domain/query/sleep_period_data.dart';
import '../../source/health/health_data_source.dart';
import '../../../domain/health/health_permissions.dart';
import '../contract/sleep_repository.dart';
import 'health_connect_gating.dart';
import 'run_catching.dart';

/// Port of the Kotlin `SleepRepositoryImpl`.
///
/// Public methods convert exceptions to failures via [runCatching] at the
/// boundary; nothing here composes another public method, so the bodies keep
/// their original throwing flow unchanged.
class SleepRepositoryImpl implements SleepRepository {
  SleepRepositoryImpl(this._dataSource);

  final HealthDataSource _dataSource;

  @override
  Future<Result<SleepPeriodData>> loadSleepPeriod(
    PeriodLoadQuery query,
    SleepRangeMode sleepRangeMode, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readSleep)) {
          return const SleepPeriodData();
        }
        final windows = query.windows;
        final current =
            await _dataSource.readSleepData(windows.current.start, windows.current.end, sleepRangeMode);
        final previous = await _dataSource.readSleepData(
            windows.previous.start, windows.previous.end, sleepRangeMode);
        final baseline = await _dataSource.readSleepData(
            windows.baseline.start, windows.baseline.end, sleepRangeMode);
        return SleepPeriodData(
          sessions: current.sessions,
          previousSessions: previous.sessions,
          baselineSessions: baseline.sessions,
          dailyDurations: current.dailyAggregateDurations,
          previousDailyDurations: previous.dailyAggregateDurations,
          baselineDailyDurations: baseline.dailyAggregateDurations,
        );
      });

  @override
  Future<Result<List<SleepData>>> loadSleepSessions(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readSleep)) return const [];
        // Widen by a day either side, merge, then keep sessions whose end date
        // falls within [start, end] (matches the Kotlin filtering).
        final widenedStart = DateTime(start.year, start.month, start.day)
            .subtract(const Duration(days: 1));
        final widenedEnd = DateTime(end.year, end.month, end.day)
            .add(const Duration(days: 2));
        final sessions =
            await _dataSource.readSleepSessions(widenedStart, widenedEnd);
        final merged = mergeSleepSessions(sessions);
        return merged.where((session) {
          final endDate = LocalDate.fromDateTime(session.endTime.toLocal());
          return !endDate.isBefore(start) && !endDate.isAfter(end);
        }).toList();
      });

  @override
  Future<Result<SleepData?>> loadSleepSession(String id) =>
      runCatching(() => _dataSource.readSleepSession(id));
}
