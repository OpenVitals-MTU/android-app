import '../../core/period/period_load_query.dart';
import '../../core/result/result.dart';
import '../../data/repository/contract/heart_repository.dart';
import '../../data/repository/contract/sleep_repository.dart';
import '../model/heart_models.dart';
import '../model/refresh_mode.dart';
import '../model/sleep_models.dart';
import '../preferences/sleep_window.dart';

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

  Future<Result<SleepPeriodLoadResult>> call(
    PeriodLoadQuery query,
    SleepWindow sleepWindow, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final loaded = await _sleepRepository.loadSleepPeriod(
      query,
      sleepWindow,
      refreshMode: refreshMode,
    );
    return loaded.flatMap((periodData) async {
      // The cross-metric HRV is insight, not sleep: a failed heart read keeps
      // the overlay empty rather than sinking the sleep period that did load.
      final crossDailyHrv = (await _heartRepository?.loadDailyHRV(
            query.windows.current.start,
            query.windows.current.end,
          ))
              ?.getOrNull() ??
          const <DailyHrv>[];
      return Ok(SleepPeriodLoadResult(
        sessions: periodData.sessions,
        previousSessions: periodData.previousSessions,
        baselineSessions: periodData.baselineSessions,
        dailyDurations: periodData.dailyDurations,
        previousDailyDurations: periodData.previousDailyDurations,
        baselineDailyDurations: periodData.baselineDailyDurations,
        crossDailyHrv: crossDailyHrv,
      ));
    });
  }
}
