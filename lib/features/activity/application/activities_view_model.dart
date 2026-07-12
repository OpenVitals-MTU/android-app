import 'dart:math' as math;

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../../core/period/period_load_query.dart';
import '../../../core/period/period_selection.dart';
import '../../../core/period/time_range.dart';
import '../../../core/presentation/screen_error.dart';
import '../../../core/time/local_date.dart';
import '../../../di/providers.dart';
import '../../../domain/insights/daily_goals.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/usecase/load_activities_use_case.dart';
import '../../../domain/insights/activity_metrics.dart';
import '../presentation/exercise_labels.dart';

// The overview day and the cached load result are the use case's shape; the
// screens read them straight off the state, so they stay visible from here.
export '../../../domain/usecase/load_activities_use_case.dart'
    show ActivitiesLoadResult, ActivityOverviewDay;

part 'activities_view_model.freezed.dart';

/// The workout daily-goal key (Kotlin `MetricDailyGoalKey.WORKOUT_MINUTES`).
const MetricDailyGoalKey activitiesGoalKey = MetricDailyGoalKey.workoutMinutes;

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
/// aggregate. Unlike Kotlin (which tracks a dedicated `ActivityWeekMode`), the
/// Flutter scaffold drives the period through the shared week-period preference.
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
class ActivitiesViewModel extends Notifier<ActivitiesState> {
  int _generation = 0;
  ActivitiesLoadResult? _latestResult;

  @override
  ActivitiesState build() => ActivitiesState(
        selectedDate: LocalDate.now(),
        dailyGoalMinutes: ref
            .read(preferencesRepositoryProvider)
            .dailyGoalFor(activitiesGoalKey),
      );

  Future<void> load(PeriodSelection selection) async {
    final generation = ++_generation;
    final prefs = ref.read(preferencesRepositoryProvider);
    final loadActivities = ref.read(loadActivitiesUseCaseProvider);

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
      // Which repositories the overview needs, and how the per-day cardio-load
      // is composed out of them, is domain knowledge and lives in the use case.
      final result = await loadActivities(query);
      if (!ref.mounted || generation != _generation) return;

      _latestResult = result;
      state = _stateWithResult(
        state.copyWith(isLoading: false, error: null),
        result,
        state.selectedActivityType,
      );
    } catch (error) {
      if (!ref.mounted || generation != _generation) return;
      state = state.copyWith(
        isLoading: false,
        error:
            throwableToScreenError(error, fallback: 'Unable to load workouts.'),
      );
    }
  }

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
    state = state.copyWith(dailyGoalMinutes: next);
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
    try {
      await ref.read(deleteActivityEntryUseCaseProvider)(entryId);
      await load(PeriodSelection(state.selectedRange, state.selectedDate));
    } catch (error) {
      _latestResult = previousResult;
      state = previousState.copyWith(
        error: throwableToScreenError(error,
            fallback: 'Unable to delete workout.'),
      );
    }
  }

  /// The filter chips, ordered by the label the user actually reads — which is a
  /// presentation concern (it is localizable), so it is cut here and not in the
  /// use case.
  List<int> _availableActivityTypes(ActivitiesLoadResult result) =>
      result.activityTypes().toList()
        ..sort((a, b) => exerciseTypeLabel(a).compareTo(exerciseTypeLabel(b)));

  ActivitiesState _stateWithResult(
    ActivitiesState base,
    ActivitiesLoadResult result,
    int? type,
  ) {
    final filtered = result.filteredBy(type);
    return base.copyWith(
      selectedActivityType: type,
      availableActivityTypes: _availableActivityTypes(result),
      workouts: filtered.workouts,
      plannedWorkouts: filtered.plannedWorkouts,
      previousWorkouts: filtered.previousWorkouts,
      baselineWorkouts: filtered.baselineWorkouts,
      activityTypeAggregates: activityTypeAggregatesOf(filtered.workouts),
      overviewDays: filtered.overviewDays,
      crossDailyRestingHR: filtered.crossDailyRestingHR,
    );
  }
}

final activitiesProvider =
    NotifierProvider<ActivitiesViewModel, ActivitiesState>(
  ActivitiesViewModel.new,
);

// ── Pure aggregation helpers (Kotlin `ActivitiesViewModel` / `ActivityTypeAggregates`).

/// The workout minutes per day (Kotlin `workoutDailyGoalValues`).
List<DailyGoalValue> workoutDailyGoalValues(List<ExerciseData> workouts) {
  final byDate = <LocalDate, double>{};
  for (final w in workouts) {
    final date = instantToLocalDate(w.startTime);
    byDate[date] =
        (byDate[date] ?? 0.0) + math.max(0, w.durationMs).toDouble() / 60000.0;
  }
  return [
    for (final entry in byDate.entries)
      DailyGoalValue(date: entry.key, value: entry.value),
  ];
}

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
