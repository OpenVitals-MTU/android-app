import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/period/period_load_query.dart';
import '../../core/period/period_selection.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/insights/daily_goals.dart';
import '../../domain/model/refresh_mode.dart';
import '../../domain/preferences/sleep_range_mode.dart';
import '../../domain/usecase/load_sleep_period_use_case.dart';

part 'sleep_notifier.freezed.dart';

/// The Riverpod port of the Kotlin `SleepUiState`, trimmed to the fields the
/// period detail UI consumes: the selection the scaffold drives, the resolved
/// sleep-window + week preference snapshot, the loaded [SleepPeriodLoadResult]
/// payload, and loading/error flags. The heavier `SleepDisplayState` the Kotlin
/// view-model precomputes is derived on demand by the screen from [result].
@freezed
abstract class SleepState with _$SleepState {
  const SleepState._();

  const factory SleepState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(SleepRangeMode.evening18h) SleepRangeMode sleepRangeMode,
    @Default(WeekPeriodMode.mondayToSunday) WeekPeriodMode weekPeriodMode,
    @Default(true) bool isLoading,
    /// The sleep-hours goal, moved by the goal card's steppers.
    @Default(8.0) double dailyGoalHours,
    ScreenError? error,
    SleepPeriodLoadResult? result,
  }) = _SleepState;
}

/// The Riverpod port of the Kotlin `SleepViewModel`.
///
/// A manual [Notifier] (no codegen), matching the activity template: the owning
/// [MetricDetailScaffold] drives every load through [load] (once on first frame,
/// then on each selection change) and pull-to-refresh through [refresh]. A
/// monotonic [_generation] guard drops stale results.
class SleepNotifier extends Notifier<SleepState> {
  int _generation = 0;

  @override
  SleepState build() {
    final prefs = ref.read(preferencesRepositoryProvider);
    return SleepState(
      selectedDate: LocalDate.now(),
      sleepRangeMode: prefs.sleepRangeMode,
      weekPeriodMode: prefs.weekPeriodMode,
      dailyGoalHours: prefs.dailyGoalFor(MetricDailyGoalKey.sleepHours),
    );
  }

  /// Kotlin `SleepViewModel.increaseDailyGoal` / `decreaseDailyGoal`: step the
  /// target by a quarter hour, clamped to 1–14 h, and persist it.
  void _nudgeDailyGoal(double delta) {
    const key = MetricDailyGoalKey.sleepHours;
    final next = key.normalize(state.dailyGoalHours + delta);
    if (next == state.dailyGoalHours) return;
    ref.read(preferencesRepositoryProvider).setDailyGoalFor(key, next);
    state = state.copyWith(dailyGoalHours: next);
  }

  void increaseDailyGoal() =>
      _nudgeDailyGoal(MetricDailyGoalKey.sleepHours.step);

  void decreaseDailyGoal() =>
      _nudgeDailyGoal(-MetricDailyGoalKey.sleepHours.step);

  /// Loads the sleep period for [selection] (the scaffold's current period).
  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final useCase = ref.read(loadSleepPeriodUseCaseProvider);
    final sleepRangeMode = prefs.sleepRangeMode;
    final weekPeriodMode = prefs.weekPeriodMode;

    state = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      sleepRangeMode: sleepRangeMode,
      weekPeriodMode: weekPeriodMode,
      isLoading: true,
      error: null,
    );

    final query = PeriodLoadQuery(
      range: selection.selectedRange,
      anchorDate: selection.selectedDate,
      weekPeriodMode: weekPeriodMode,
    );

    try {
      final result =
          await useCase(query, sleepRangeMode, refreshMode: refreshMode);
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(isLoading: false, result: result, error: null);
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error:
            throwableToScreenError(error, fallback: 'Unable to load sleep.'),
      );
    }
  }

  /// Force-reloads the current selection (pull-to-refresh).
  Future<void> refresh() => load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );
}

/// The sleep detail state provider. A manually-declared [NotifierProvider] (no
/// codegen), matching the dashboard/activity templates.
final sleepNotifierProvider =
    NotifierProvider<SleepNotifier, SleepState>(SleepNotifier.new);
