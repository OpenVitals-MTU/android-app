import '../../core/period/period_load_query.dart';
import '../../data/repository/contract/heart_repository.dart';
import '../../data/repository/contract/sleep_repository.dart';
import '../model/heart_models.dart';
import '../model/refresh_mode.dart';
import '../model/sleep_models.dart';
import '../preferences/sleep_range_mode.dart';

/// Combined sleep + cross-metric HRV result. Port of the Kotlin
/// `SleepPeriodLoadResult`.
class SleepPeriodLoadResult {
  const SleepPeriodLoadResult({
    this.sessions = const [],
    this.previousSessions = const [],
    this.baselineSessions = const [],
    this.dailyDurations = const [],
    this.previousDailyDurations = const [],
    this.baselineDailyDurations = const [],
    this.crossDailyHrv = const [],
  });

  final List<SleepData> sessions;
  final List<SleepData> previousSessions;
  final List<SleepData> baselineSessions;
  final List<DailySleepDuration> dailyDurations;
  final List<DailySleepDuration> previousDailyDurations;
  final List<DailySleepDuration> baselineDailyDurations;
  final List<DailyHrv> crossDailyHrv;
}

/// Port of the Kotlin `LoadSleepPeriodUseCase`.
class LoadSleepPeriodUseCase {
  const LoadSleepPeriodUseCase(this._sleepRepository, this._heartRepository);

  final SleepRepository _sleepRepository;
  final HeartRepository? _heartRepository;

  Future<SleepPeriodLoadResult> call(
    PeriodLoadQuery query,
    SleepRangeMode sleepRangeMode, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final periodData = await _sleepRepository.loadSleepPeriod(
      query,
      sleepRangeMode,
      refreshMode: refreshMode,
    );
    final crossDailyHrv = await _heartRepository?.loadDailyHRV(
          query.windows.current.start,
          query.windows.current.end,
        ) ??
        const <DailyHrv>[];
    return SleepPeriodLoadResult(
      sessions: periodData.sessions,
      previousSessions: periodData.previousSessions,
      baselineSessions: periodData.baselineSessions,
      dailyDurations: periodData.dailyDurations,
      previousDailyDurations: periodData.previousDailyDurations,
      baselineDailyDurations: periodData.baselineDailyDurations,
      crossDailyHrv: crossDailyHrv,
    );
  }
}
