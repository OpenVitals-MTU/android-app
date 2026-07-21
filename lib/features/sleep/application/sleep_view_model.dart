import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_load_query.dart';
import '../../../core/period/period_selection.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/period_metric_loader.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/insights/daily_goals.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/preferences/sleep_window.dart';
import '../../../domain/usecase/load_sleep_period_use_case.dart';
import 'sleep_display.dart';

part 'sleep_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `SleepUiState`, trimmed to the fields the
/// period detail UI consumes: the selection the scaffold drives, the resolved
/// sleep-window + week preference snapshot, the loaded [SleepPeriodLoadResult]
/// payload, the precomputed [SleepDisplay], and loading/error flags.
@freezed
abstract class SleepState with _$SleepState {
  const SleepState._();

  const factory SleepState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(SleepWindow.defaultWindow) SleepWindow sleepWindow,
    @Default(WeekPeriodMode.mondayToSunday) WeekPeriodMode weekPeriodMode,
    @Default(true) bool isLoading,
    /// The sleep-hours goal, moved by the goal card's steppers.
    @Default(8.0) double dailyGoalHours,
    ScreenError? error,
    SleepPeriodLoadResult? result,
    SleepDisplay? display,
  }) = _SleepState;
}

/// The Riverpod port of the Kotlin `SleepViewModel`.
///
/// A manual [Notifier] (no codegen), matching the activity template: the owning
/// [MetricDetailScaffold] drives every load through [load] (once on first frame,
/// then on each selection change) and pull-to-refresh through [refresh]. A
/// monotonic [_generation] guard drops stale results.
///
/// The Kotlin `SleepDisplayState` is built here, at load time — the screen
/// renders [SleepState.display] and derives nothing.
class SleepViewModel extends Notifier<SleepState>
    with PeriodMetricLoader<SleepState, SleepPeriodLoadResult> {
  @override
  SleepState build() {
    final prefs = ref.read(preferencesRepositoryProvider);
    return SleepState(
      selectedDate: LocalDate.now(),
      sleepWindow: prefs.sleepWindow,
      weekPeriodMode: prefs.weekPeriodMode,
      dailyGoalHours: prefs.dailyGoalFor(MetricDailyGoalKey.sleepHours),
    );
  }

  /// Kotlin `SleepViewModel.increaseDailyGoal` / `decreaseDailyGoal`: step the
  /// target by a quarter hour, clamped to 1–14 h, and persist it.
  ///
  /// The goal is an input to the display — goal progress and the sleep-target
  /// reading are read off it — so moving it rebuilds the display over the
  /// period already loaded, without going back to the repository.
  void _nudgeDailyGoal(double delta) {
    const key = MetricDailyGoalKey.sleepHours;
    final next = key.normalize(state.dailyGoalHours + delta);
    if (next == state.dailyGoalHours) return;
    ref.read(preferencesRepositoryProvider).setDailyGoalFor(key, next);
    final result = state.result;
    state = state.copyWith(
      dailyGoalHours: next,
      display: result == null ? null : _displayFor(result, next),
    );
  }

  void increaseDailyGoal() =>
      _nudgeDailyGoal(MetricDailyGoalKey.sleepHours.step);

  void decreaseDailyGoal() =>
      _nudgeDailyGoal(-MetricDailyGoalKey.sleepHours.step);

  SleepDisplay _displayFor(SleepPeriodLoadResult result, double goalHours) =>
      buildSleepDisplay(
        result: result,
        selectedRange: state.selectedRange,
        selectedDate: state.selectedDate,
        sleepWindow: state.sleepWindow,
        weekPeriodMode: state.weekPeriodMode,
        dailyGoalHours: goalHours,
      );

  /// Loads the sleep period for [selection] (the scaffold's current period).
  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runLoad(selection, refreshMode: refreshMode);

  @override
  String get loadErrorFallback => 'Unable to load sleep.';

  @override
  PeriodSelection selectionOf(SleepState state) =>
      PeriodSelection(state.selectedRange, state.selectedDate);

  @override
  SleepState onLoadStart(
    SleepState state,
    PeriodSelection selection, {
    required bool navigated,
  }) {
    final prefs = ref.read(preferencesRepositoryProvider);
    final next = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      sleepWindow: prefs.sleepWindow,
      weekPeriodMode: prefs.weekPeriodMode,
      isLoading: true,
      error: null,
    );
    return navigated ? next.copyWith(result: null, display: null) : next;
  }

  @override
  Future<Result<SleepPeriodLoadResult>> fetch(
    PeriodLoadQuery query,
    RefreshMode refreshMode,
  ) =>
      ref.read(loadSleepPeriodUseCaseProvider)(
        query,
        refreshMode: refreshMode,
      );

  @override
  SleepState onLoadSuccess(
    SleepState state,
    SleepPeriodLoadResult value,
    PeriodLoadQuery query,
  ) =>
      state.copyWith(
        isLoading: false,
        result: value,
        display: _displayFor(value, state.dailyGoalHours),
        error: null,
      );

  @override
  SleepState onLoadError(SleepState state, ScreenError error) =>
      state.copyWith(isLoading: false, error: error);

  /// Force-reloads the current selection (pull-to-refresh).
  Future<void> refresh() => load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );
}

/// The sleep detail state provider. A manually-declared [NotifierProvider] (no
/// codegen), matching the dashboard/activity templates.
final sleepProvider =
    NotifierProvider<SleepViewModel, SleepState>(SleepViewModel.new);
