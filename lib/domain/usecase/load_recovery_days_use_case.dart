import '../../core/result/result.dart';
import '../../core/time/local_date.dart';
import '../../data/repository/contract/sleep_repository.dart';
import '../insights/sleep_score.dart';
import '../model/sleep_models.dart';

/// Kotlin `RecoveryLookbackDays`.
const int recoveryLookbackDays = 7;

/// One day of recovery data: its sleep sessions and the sleep score computed
/// against the preceding nights. Port of the Kotlin `RecoveryDay`.
class RecoveryDay {
  const RecoveryDay({
    required this.date,
    this.sessions = const <SleepData>[],
    this.sleepScore = SleepScoreEstimate.noData,
  });

  final LocalDate date;
  final List<SleepData> sessions;
  final SleepScoreEstimate sleepScore;

  /// Kotlin `mainSleepSession`: the session with the most stage-derived sleep.
  SleepData? get mainSleepSession {
    SleepData? best;
    int? bestDuration;
    for (final session in sessions) {
      final duration =
          sleepDurationMsFromStages(session.stages, session.durationMs);
      if (bestDuration == null || duration > bestDuration) {
        bestDuration = duration;
        best = session;
      }
    }
    return best;
  }
}

/// Loads the recovery week: the last [recoveryLookbackDays] nights, scored.
///
/// A sleep score is a comparison, not a measurement — [calculateSleepScoresByDate]
/// scores each night against the ones around it — so the read spans the whole
/// lookback window even though the screen shows a single day out of it.
///
/// The sessions are bucketed by the day they *end* on: a night that starts at
/// 23:40 belongs to the morning it delivers, which is the day the user thinks of
/// as "how I slept".
class LoadRecoveryDaysUseCase {
  const LoadRecoveryDaysUseCase(this._sleepRepository);

  final SleepRepository _sleepRepository;

  Future<Result<List<RecoveryDay>>> call(LocalDate today) async {
    final start = today.minusDays(recoveryLookbackDays - 1);
    final loaded = await _sleepRepository.loadSleepSessions(start, today);
    return loaded.map((sessions) => _toRecoveryDays(sessions, start, today));
  }
}

/// Kotlin `List<SleepData>.toRecoveryDays`.
List<RecoveryDay> _toRecoveryDays(
  List<SleepData> sessions,
  LocalDate start,
  LocalDate end,
) {
  final sessionsByDate = <LocalDate, List<SleepData>>{};
  for (final session in sessions) {
    final key = instantToLocalDate(session.endTime);
    (sessionsByDate[key] ??= <SleepData>[]).add(session);
  }
  final sleepScores = calculateSleepScoresByDate(sessions, start, end);

  final days = <RecoveryDay>[];
  var date = start;
  while (!date.isAfter(end)) {
    days.add(
      RecoveryDay(
        date: date,
        sessions: sessionsByDate[date] ?? const <SleepData>[],
        sleepScore: sleepScores[date] ?? SleepScoreEstimate.noData,
      ),
    );
    date = date.plusDays(1);
  }
  return days;
}
