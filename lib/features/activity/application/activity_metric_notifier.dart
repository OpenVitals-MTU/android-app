import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_load_query.dart';
import '../../../core/period/period_selection.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/activity_period_data.dart';
import '../presentation/activity_metric.dart';
import '../presentation/activity_metric_display.dart';

part 'activity_metric_notifier.freezed.dart';

/// The Riverpod port of the Kotlin `ActivityUiState`, trimmed to the fields the
/// period detail UI actually consumes: the selection the scaffold drives, the
/// loaded [ActivityPeriodData] payload, and loading/error flags.
@freezed
abstract class ActivityMetricState with _$ActivityMetricState {
  const ActivityMetricState._();

  const factory ActivityMetricState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    ActivityPeriodData? data,
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
class ActivityMetricNotifier extends Notifier<ActivityMetricState> {
  ActivityMetricNotifier(this.metric);

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
    state = state.copyWith(dailyGoal: next);
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

    try {
      final data = await loadActivityMetricPeriod(
        query,
        includeSteps: metric.usesDailySteps,
        includeNutrition: metric.usesNutrition,
        includeWheelchairPushes: metric.usesWheelchairPushes,
        refreshMode: refreshMode,
      );
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(isLoading: false, data: data, error: null);
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error: throwableToScreenError(error, fallback: 'Unable to load data.'),
      );
    }
  }

  /// Force-reloads the current selection (pull-to-refresh).
  Future<void> refresh() => load(
        PeriodSelection(state.selectedRange, state.selectedDate),
        refreshMode: RefreshMode.force,
      );
}

/// One [NotifierProvider] per movement metric, built eagerly so each metric's
/// state survives across screen rebuilds. Callers resolve the provider for a
/// metric via [activityMetricProvider].
final Map<ActivityMetric,
        NotifierProvider<ActivityMetricNotifier, ActivityMetricState>>
    _activityMetricProviders = {
  for (final metric in ActivityMetric.values)
    metric: NotifierProvider<ActivityMetricNotifier, ActivityMetricState>(
      () => ActivityMetricNotifier(metric),
    ),
};

/// The state provider for [metric]'s period detail screen.
NotifierProvider<ActivityMetricNotifier, ActivityMetricState>
    activityMetricProvider(ActivityMetric metric) =>
        _activityMetricProviders[metric]!;
