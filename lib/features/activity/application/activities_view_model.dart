import 'dart:math' as math;

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_calculations.dart';
import '../../../core/period/period_load_query.dart';
import '../../../core/period/period_selection.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/period_metric_loader.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/usecase/load_activities_use_case.dart';
import '../../../domain/insights/activity_metrics.dart';
import '../presentation/exercise_labels.dart';
import 'activities_display.dart';

// The overview day and the cached load result are the use case's shape; the
// screens read them straight off the state, so they stay visible from here.
export '../../../domain/usecase/load_activities_use_case.dart'
    show ActivitiesLoadResult, ActivityOverviewDay;

part 'activities_view_model.freezed.dart';

/// Per-activity-type rollup (Kotlin `ActivityTypeAggregate`). A plain value type.
class ActivityTypeAggregate {
  const ActivityTypeAggregate({
    required this.exerciseType,
    required this.count,
    required this.totalDistanceMeters,
    required this.totalDurationMs,
    required this.totalMovingDurationMs,
    required this.averageMovingSpeedMetersPerSecond,
    required this.bestSpeedMetersPerSecond,
  });

  final int exerciseType;
  final int count;
  final double totalDistanceMeters;
  final int totalDurationMs;
  final int totalMovingDurationMs;
  final double? averageMovingSpeedMetersPerSecond;
  final double? bestSpeedMetersPerSecond;
}

/// The Riverpod port of the Kotlin `ActivitiesUiState`: the full activities
/// aggregate, plus the precomputed [ActivitiesDisplay] the screen renders.
/// Unlike Kotlin (which tracks a dedicated `ActivityWeekMode`), the Flutter
/// scaffold drives the period through the shared week-period preference.
@freezed
abstract class ActivitiesState with _$ActivitiesState {
  const ActivitiesState._();

  const factory ActivitiesState({
    required LocalDate selectedDate,
    @Default(TimeRange.week) TimeRange selectedRange,
    @Default(true) bool isLoading,
    ScreenError? error,
    @Default(30.0) double dailyGoalMinutes,
    int? selectedActivityType,
    @Default(<int>[]) List<int> availableActivityTypes,
    @Default(<ExerciseData>[]) List<ExerciseData> workouts,
    @Default(<PlannedExerciseData>[]) List<PlannedExerciseData> plannedWorkouts,
    @Default(<ExerciseData>[]) List<ExerciseData> previousWorkouts,
    @Default(<ExerciseData>[]) List<ExerciseData> baselineWorkouts,
    @Default(<ActivityTypeAggregate>[])
    List<ActivityTypeAggregate> activityTypeAggregates,
    @Default(<ActivityOverviewDay>[]) List<ActivityOverviewDay> overviewDays,
    @Default(<DailyRestingHR>[]) List<DailyRestingHR> crossDailyRestingHR,
    ActivitiesDisplay? display,
  }) = _ActivitiesState;

  /// Total recorded duration across the period's workouts.
  int get totalDurationMs =>
      workouts.fold<int>(0, (sum, w) => sum + w.durationMs);

  /// Total recorded distance (metres) across the period's workouts.
  double get totalDistanceMeters => workouts.fold<double>(
        0.0,
        (sum, w) => sum + (w.totalDistanceMeters ?? 0.0),
      );
}

/// The Riverpod port of the Kotlin `ActivitiesViewModel`. Loads the full
/// activities aggregate (workouts, planned workouts, previous/baseline windows,
/// the per-day overview with cardio-load, and the resting-HR cross series) for
/// the scaffold's current period.
///
/// The display model is built here — at load time, and again whenever the filter
/// or the daily goal moves the slice it was cut from. The screen renders
/// [ActivitiesState.display] and derives nothing.
class ActivitiesViewModel extends Notifier<ActivitiesState>
    with PeriodMetricLoader<ActivitiesState, ActivitiesLoadResult> {
  ActivitiesLoadResult? _latestResult;

  @override
  ActivitiesState build() => ActivitiesState(
        selectedDate: LocalDate.now(),
        dailyGoalMinutes: ref
            .read(preferencesRepositoryProvider)
            .dailyGoalFor(activitiesGoalKey),
      );

  Future<void> load(PeriodSelection selection) => runLoad(selection);

  @override
  String get loadErrorFallback => 'Unable to load workouts.';

  @override
  PeriodSelection selectionOf(ActivitiesState state) =>
      PeriodSelection(state.selectedRange, state.selectedDate);

  @override
  ActivitiesState onLoadStart(
    ActivitiesState state,
    PeriodSelection selection, {
    required bool navigated,
  }) {
    final next = state.copyWith(
      selectedRange: selection.selectedRange,
      selectedDate: selection.selectedDate,
      isLoading: true,
      error: null,
    );
    return navigated ? next.copyWith(display: null) : next;
  }

  @override
  Future<Result<ActivitiesLoadResult>> fetch(
    PeriodLoadQuery query,
    RefreshMode refreshMode,
  ) =>
      // This use case has no incremental refresh, so [refreshMode] is unused.
      // Which repositories the overview needs, and how the per-day cardio-load
      // is composed out of them, is domain knowledge and lives in the use case.
      ref.read(loadActivitiesUseCaseProvider)(query);

  @override
  ActivitiesState onLoadSuccess(
    ActivitiesState state,
    ActivitiesLoadResult value,
    PeriodLoadQuery query,
  ) {
    _latestResult = value;
    return _stateWithResult(
      state.copyWith(isLoading: false, error: null),
      value,
      state.selectedActivityType,
    );
  }

  @override
  ActivitiesState onLoadError(ActivitiesState state, ScreenError error) =>
      state.copyWith(isLoading: false, error: error);

  Future<void> refresh() =>
      load(PeriodSelection(state.selectedRange, state.selectedDate));

  /// Kotlin `selectActivityType`: re-slices the cached result without reloading.
  void selectActivityType(int? type) {
    final result = _latestResult;
    if (result == null) {
      state = state.copyWith(selectedActivityType: type);
      return;
    }
    state = _stateWithResult(state, result, type);
  }

  void increaseDailyGoal() => _nudgeGoal(activitiesGoalKey.step);

  void decreaseDailyGoal() => _nudgeGoal(-activitiesGoalKey.step);

  void _nudgeGoal(double delta) {
    final next = activitiesGoalKey.normalize(state.dailyGoalMinutes + delta);
    if (next == state.dailyGoalMinutes) return;
    ref.read(preferencesRepositoryProvider).setDailyGoalFor(activitiesGoalKey, next);
    // The goal is an input to the display's goal progress, so the moved goal has
    // to re-derive it — the screen no longer recomputes anything on rebuild.
    final nudged = state.copyWith(dailyGoalMinutes: next);
    final result = _latestResult;
    state = result == null
        ? nudged
        : _stateWithResult(nudged, result, nudged.selectedActivityType);
  }

  /// Kotlin `deleteActivityEntry`: optimistic removal of an OpenVitals workout,
  /// then a repository delete + reload (rolling back on failure).
  Future<void> deleteActivityEntry(String entryId) async {
    if (entryId.isEmpty) return;
    final entry =
        state.workouts.where((w) => w.id == entryId).cast<ExerciseData?>().firstOrNull;
    if (entry == null || !entry.isOpenVitalsEntry) return;

    final previousState = state;
    final previousResult = _latestResult;
    final trimmed = _latestResult?.withoutEntry(entryId);
    _latestResult = trimmed;
    if (trimmed != null) {
      state = _stateWithResult(
        state.copyWith(error: null),
        trimmed,
        state.selectedActivityType,
      );
    }
    final deleted = await ref.read(deleteActivityEntryUseCaseProvider)(entryId);
    switch (deleted) {
      case Ok():
        await load(PeriodSelection(state.selectedRange, state.selectedDate));
      case Err(:final failure):
        _latestResult = previousResult;
        state = previousState.copyWith(
          error: failure.toScreenError(fallback: 'Unable to delete workout.'),
        );
    }
  }

  /// The filter chips, ordered by the label the user actually reads — which is a
  /// presentation concern (it is localizable), so it is cut here and not in the
  /// use case.
  List<int> _availableActivityTypes(ActivitiesLoadResult result) =>
      result.activityTypes().toList()
        ..sort((a, b) => exerciseTypeLabel(a).compareTo(exerciseTypeLabel(b)));

  /// The period the scaffold is showing — the same window it hands its content
  /// builder, so the display's goal progress and confidence are cut against the
  /// dates the screen prints.
  DatePeriod _displayPeriod(ActivitiesState base) => displayPeriodFor(
        base.selectedRange,
        base.selectedDate,
        weekPeriodMode: ref.read(preferencesRepositoryProvider).weekPeriodMode,
      );

  ActivitiesState _stateWithResult(
    ActivitiesState base,
    ActivitiesLoadResult result,
    int? type,
  ) {
    final filtered = result.filteredBy(type);
    final availableActivityTypes = _availableActivityTypes(result);
    return base.copyWith(
      selectedActivityType: type,
      availableActivityTypes: availableActivityTypes,
      workouts: filtered.workouts,
      plannedWorkouts: filtered.plannedWorkouts,
      previousWorkouts: filtered.previousWorkouts,
      baselineWorkouts: filtered.baselineWorkouts,
      activityTypeAggregates: activityTypeAggregatesOf(filtered.workouts),
      overviewDays: filtered.overviewDays,
      crossDailyRestingHR: filtered.crossDailyRestingHR,
      display: buildActivitiesDisplay(
        result: filtered,
        availableActivityTypes: availableActivityTypes,
        selectedActivityType: type,
        range: base.selectedRange,
        period: _displayPeriod(base),
        dailyGoalMinutes: base.dailyGoalMinutes,
      ),
    );
  }
}

final activitiesProvider =
    NotifierProvider<ActivitiesViewModel, ActivitiesState>(
  ActivitiesViewModel.new,
);

// ── Pure aggregation helpers (Kotlin `ActivitiesViewModel` / `ActivityTypeAggregates`).

List<ActivityTypeAggregate> activityTypeAggregatesOf(
  List<ExerciseData> workouts,
) {
  final byType = <int, List<ExerciseData>>{};
  for (final w in workouts) {
    byType.putIfAbsent(w.exerciseType, () => <ExerciseData>[]).add(w);
  }
  final aggregates = <ActivityTypeAggregate>[];
  byType.forEach((type, group) {
    if (group.isEmpty) return;
    final totalDistance = group.fold<double>(
      0.0,
      (sum, w) => sum + ((w.totalDistanceMeters ?? 0) > 0
          ? w.totalDistanceMeters!
          : 0.0),
    );
    final totalDuration =
        group.fold<int>(0, (sum, w) => sum + math.max(0, w.durationMs));
    // Moving duration excludes paused segments (Kotlin `ActivityMetrics`
    // `movingDurationMs`): per workout, subtract the summed pause-segment
    // durations from its total duration, then aggregate over the group.
    final totalMovingDuration =
        group.fold<int>(0, (sum, w) => sum + movingDurationMs(w));
    final averageMovingSpeed = totalDistance > 0 && totalMovingDuration > 0
        ? _finitePositive(totalDistance / (totalMovingDuration / 1000.0))
        : null;
    // Fastest workout of the group. Per workout Kotlin takes the *max* of the
    // recorded average speed and the derived distance/moving-duration speed —
    // a provider may record only one of the two, and where both exist the
    // recorded average can be diluted by paused stretches the moving duration
    // already excludes.
    double? bestSpeed;
    for (final w in group) {
      for (final candidate in [
        _finitePositive(w.averageSpeedMetersPerSecond ?? 0),
        _workoutMovingSpeedMetersPerSecond(w),
      ]) {
        if (candidate != null && (bestSpeed == null || candidate > bestSpeed)) {
          bestSpeed = candidate;
        }
      }
    }
    aggregates.add(ActivityTypeAggregate(
      exerciseType: type,
      count: group.length,
      totalDistanceMeters: totalDistance,
      totalDurationMs: totalDuration,
      totalMovingDurationMs: totalMovingDuration,
      averageMovingSpeedMetersPerSecond: averageMovingSpeed,
      bestSpeedMetersPerSecond: bestSpeed,
    ));
  });
  aggregates.sort((a, b) {
    final byDuration = b.totalDurationMs.compareTo(a.totalDurationMs);
    if (byDuration != 0) return byDuration;
    return exerciseTypeLabel(a.exerciseType)
        .compareTo(exerciseTypeLabel(b.exerciseType));
  });
  return aggregates;
}

double? _finitePositive(double value) =>
    value.isFinite && value > 0 ? value : null;

/// A single workout's distance / moving-duration speed (Kotlin
/// `ExerciseData.averageMovingSpeedMetersPerSecond`). Null when the workout has
/// no positive distance or no moving time.
double? _workoutMovingSpeedMetersPerSecond(ExerciseData workout) {
  final distance = workout.totalDistanceMeters;
  if (distance == null || distance <= 0) return null;
  final movingMs = movingDurationMs(workout);
  if (movingMs <= 0) return null;
  return _finitePositive(distance / (movingMs / 1000.0));
}
