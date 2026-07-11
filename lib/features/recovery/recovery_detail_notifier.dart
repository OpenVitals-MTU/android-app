import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/insights/sleep_score.dart';
import '../../domain/model/sleep_models.dart';

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

/// Port of the Kotlin `RecoveryUiState` (the sleep-score slice the two detail
/// screens read; the stress slice lives in `RecoveryNotifier`).
class RecoveryDetailState {
  RecoveryDetailState({
    this.isLoading = true,
    LocalDate? selectedDate,
    this.days = const <RecoveryDay>[],
    this.error,
  }) : selectedDate = selectedDate ?? LocalDate.now();

  final bool isLoading;
  final LocalDate selectedDate;
  final List<RecoveryDay> days;
  final ScreenError? error;

  /// Kotlin `RecoveryUiState.today`.
  RecoveryDay get today {
    for (final day in days) {
      if (day.date == selectedDate) return day;
    }
    return RecoveryDay(date: selectedDate);
  }
}

/// Port of the Kotlin `RecoveryViewModel`'s sleep-score load: the last
/// [recoveryLookbackDays] days of sleep sessions turned into [RecoveryDay]s
/// via [calculateSleepScoresByDate]. Kept separate from [RecoveryNotifier],
/// which is stress-only in the Flutter port.
class RecoveryDetailNotifier extends Notifier<RecoveryDetailState> {
  int _generation = 0;

  @override
  RecoveryDetailState build() {
    Future.microtask(() {
      if (ref.mounted) load();
    });
    return RecoveryDetailState();
  }

  Future<void> load([LocalDate? date]) async {
    final today = date ?? LocalDate.now();
    final start = today.minusDays(recoveryLookbackDays - 1);
    final generation = ++_generation;
    final repository = ref.read(sleepRepositoryProvider);
    state = RecoveryDetailState(
      isLoading: true,
      selectedDate: today,
      days: state.days,
    );

    try {
      final sessions = await repository.loadSleepSessions(start, today);
      if (!ref.mounted || generation != _generation) return;
      state = RecoveryDetailState(
        isLoading: false,
        selectedDate: today,
        days: _toRecoveryDays(sessions, start, today),
      );
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = RecoveryDetailState(
        isLoading: false,
        selectedDate: today,
        days: state.days,
        error: throwableToScreenError(
          error,
          fallback: 'Unable to load sleep data.',
        ),
      );
    }
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

/// Shared by the sleep-score and sleep-efficiency detail screens (the Kotlin
/// screens share one `RecoveryViewModel` instance the same way).
final recoveryDetailNotifierProvider =
    NotifierProvider.autoDispose<RecoveryDetailNotifier, RecoveryDetailState>(
  RecoveryDetailNotifier.new,
);
