import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_calculations.dart';
import '../../../core/period/period_load_query.dart';
import '../../../core/period/period_selection.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/activity_period_data.dart';
import '../presentation/activity_metric.dart';
import 'activity_metric_display.dart';

part 'activity_metric_view_model.freezed.dart';

/// The Riverpod port of the Kotlin `ActivityUiState`, trimmed to the fields the
/// period detail UI actually consumes: the selection the scaffold drives, the
/// loaded [ActivityPeriodData] payload, its precomputed [ActivityMetricDisplay],
/// and loading/error flags.
@freezed
abstract class ActivityMetricState with _$ActivityMetricState {
  const ActivityMetricState._();

  const factory ActivityMetricState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    ActivityPeriodData? data,
    ActivityMetricDisplay? display,
    /// The metric's persisted daily goal, moved by the goal card's steppers.
    @Default(0.0) double dailyGoal,
  }) = _ActivityMetricState;
}

/// The Riverpod port of the Kotlin `ActivityViewModel`, shared across the six
/// movement metrics (keyed by [metric]).
///
/// A manual [Notifier] (no codegen), matching the dashboard template. Unlike the
/// dashboard notifier it does not self-trigger its first load: the owning
/// [MetricDetailScaffold] drives every load through [load] (once on first frame,
/// then on each selection change) and pull-to-refresh through [refresh]. A
/// monotonic [_generation] guard drops stale results.
///
/// The display model is built here, at load time — the screen renders
/// [ActivityMetricState.display] and derives nothing.
class ActivityMetricViewModel extends Notifier<ActivityMetricState> {
  ActivityMetricViewModel(this.metric);

  final ActivityMetric metric;
  int _generation = 0;

  @override
  ActivityMetricState build() => ActivityMetricState(
        selectedDate: LocalDate.now(),
        dailyGoal: ref
            .read(preferencesRepositoryProvider)
            .dailyGoalFor(activityMetricGoalKey(metric)),
      );

  /// Kotlin `ActivityViewModel.increaseDailyGoal` / `decreaseDailyGoal`: step the
  /// goal by its metric's step, clamped to the metric's own bounds, and persist.
  void _nudgeDailyGoal(double delta) {
    final key = activityMetricGoalKey(metric);
    final next = key.normalize(state.dailyGoal + delta);
    if (next == state.dailyGoal) return;
    ref.read(preferencesRepositoryProvider).setDailyGoalFor(key, next);
    // The goal feeds the display's goal progress, so a moved goal re-derives it:
    // the screen recomputes nothing on rebuild.
    final data = state.data;
    state = state.copyWith(
      dailyGoal: next,
      display: data == null
          ? null
          : _display(data, state.selectedRange, state.selectedDate, next),
    );
  }

  void increaseDailyGoal() =>
      _nudgeDailyGoal(activityMetricGoalKey(metric).step);

  void decreaseDailyGoal() =>
      _nudgeDailyGoal(-activityMetricGoalKey(metric).step);

  /// Loads the metric data for [selection] (the scaffold's current period).
  Future<void> load(
    PeriodSelection selection, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final loadActivityMetricPeriod =
        ref.read(loadActivityMetricPeriodUseCaseProvider);

    state = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      isLoading: true,
      error: null,
    );

    final query = PeriodLoadQuery(
      range: selection.selectedRange,
      anchorDate: selection.selectedDate,
      weekPeriodMode: prefs.weekPeriodMode,
    );

    final result = await loadActivityMetricPeriod(
      query,
      includeSteps: metric.usesDailySteps,
      includeNutrition: metric.usesNutrition,
      includeWheelchairPushes: metric.usesWheelchairPushes,
      refreshMode: refreshMode,
    );
    if (!ref.mounted || generation != _generation) return;
    switch (result) {
      case Ok(:final value):
        state = state.copyWith(
          isLoading: false,
          data: value,
          display: _display(
            value,
            selection.selectedRange,
            selection.selectedDate,
            state.dailyGoal,
          ),
          error: null,
        );
      case Err(:final failure):
        state = state.copyWith(
          isLoading: false,
          error: failure.toScreenError(fallback: 'Unable to load data.'),
        );
    }
  }

  /// Force-reloads the current selection (pull-to-refresh).
  Future<void> refresh() => load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );

  /// The derivation, cut against the period the scaffold is showing — the same
  /// window it hands its content builder.
  ActivityMetricDisplay _display(
    ActivityPeriodData data,
    TimeRange range,
    LocalDate anchorDate,
    double dailyGoal,
  ) =>
      buildActivityMetricDisplay(
        metric: metric,
        data: data,
        range: range,
        period: displayPeriodFor(
          range,
          anchorDate,
          weekPeriodMode:
              ref.read(preferencesRepositoryProvider).weekPeriodMode,
        ),
        dailyGoal: dailyGoal,
      );
}

/// One [NotifierProvider] per movement metric, built eagerly so each metric's
/// state survives across screen rebuilds. Callers resolve the provider for a
/// metric via [activityMetricProvider].
final Map<ActivityMetric,
        NotifierProvider<ActivityMetricViewModel, ActivityMetricState>>
    _activityMetricProviders = {
  for (final metric in ActivityMetric.values)
    metric: NotifierProvider<ActivityMetricViewModel, ActivityMetricState>(
      () => ActivityMetricViewModel(metric),
    ),
};

/// The state provider for [metric]'s period detail screen.
NotifierProvider<ActivityMetricViewModel, ActivityMetricState>
    activityMetricProvider(ActivityMetric metric) =>
        _activityMetricProviders[metric]!;
