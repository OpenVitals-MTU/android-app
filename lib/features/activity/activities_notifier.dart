import 'dart:math' as math;

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/period/period_load_query.dart';
import '../../core/period/period_selection.dart';
import '../../core/period/time_range.dart';
import '../../core/presentation/screen_error.dart';
import '../../core/time/local_date.dart';
import '../../di/providers.dart';
import '../../domain/insights/cardio_load.dart';
import '../../domain/insights/daily_goals.dart';
import '../../domain/model/activity_models.dart';
import '../../domain/model/heart_models.dart';
import '../../domain/model/nutrition_models.dart';
import '../manualentry/activity/activity_entry_types.dart';
import 'exercise_labels.dart';

part 'activities_notifier.freezed.dart';

/// The workout daily-goal key (Kotlin `MetricDailyGoalKey.WORKOUT_MINUTES`).
const MetricDailyGoalKey activitiesGoalKey = MetricDailyGoalKey.workoutMinutes;

/// One day of the activities overview aggregate — the union of steps,
/// energy-burned, HRV, cardio-load and workouts for that date. A plain (non
/// freezed) value type; ported from Kotlin `ActivityOverviewDay`.
class ActivityOverviewDay {
  const ActivityOverviewDay({
    required this.date,
    required this.steps,
    required this.distanceMeters,
    required this.activeCaloriesKcal,
    required this.energyBurnedKcal,
    required this.energyBurnedSource,
    required this.workouts,
    required this.hrvRmssdMs,
    required this.cardioLoad,
    required this.cardioLoadConfidence,
  });

  final LocalDate date;
  final int steps;
  final double distanceMeters;
  final double? activeCaloriesKcal;
  final double energyBurnedKcal;
  final CaloriesBurnedSource energyBurnedSource;
  final List<ExerciseData> workouts;
  final double? hrvRmssdMs;
  final int cardioLoad;
  final CardioLoadConfidence cardioLoadConfidence;

  ActivityOverviewDay withWorkouts(List<ExerciseData> workouts) =>
      ActivityOverviewDay(
        date: date,
        steps: steps,
        distanceMeters: distanceMeters,
        activeCaloriesKcal: activeCaloriesKcal,
        energyBurnedKcal: energyBurnedKcal,
        energyBurnedSource: energyBurnedSource,
        workouts: workouts,
        hrvRmssdMs: hrvRmssdMs,
        cardioLoad: cardioLoad,
        cardioLoadConfidence: cardioLoadConfidence,
      );
}

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

/// The unfiltered load result, cached so the activity-type filter can re-slice
/// the data without a repository round-trip (Kotlin `ActivitiesLoadResult`).
class _ActivitiesLoadResult {
  const _ActivitiesLoadResult({
    required this.workouts,
    required this.plannedWorkouts,
    required this.previousWorkouts,
    required this.baselineWorkouts,
    required this.overviewDays,
    required this.crossDailyRestingHR,
  });

  final List<ExerciseData> workouts;
  final List<PlannedExerciseData> plannedWorkouts;
  final List<ExerciseData> previousWorkouts;
  final List<ExerciseData> baselineWorkouts;
  final List<ActivityOverviewDay> overviewDays;
  final List<DailyRestingHR> crossDailyRestingHR;

  List<int> availableActivityTypes() {
    final types = <int>{
      for (final w in workouts) w.exerciseType,
      for (final p in plannedWorkouts) p.exerciseType,
    }.toList()
      ..sort((a, b) => exerciseTypeLabel(a).compareTo(exerciseTypeLabel(b)));
    return types;
  }

  _ActivitiesLoadResult filteredBy(int? type) {
    if (type == null) return this;
    return _ActivitiesLoadResult(
      workouts: [for (final w in workouts) if (w.exerciseType == type) w],
      plannedWorkouts: [
        for (final p in plannedWorkouts)
          if (p.exerciseType == type) p,
      ],
      previousWorkouts: [
        for (final w in previousWorkouts)
          if (w.exerciseType == type) w,
      ],
      baselineWorkouts: [
        for (final w in baselineWorkouts)
          if (w.exerciseType == type) w,
      ],
      overviewDays: [
        for (final day in overviewDays)
          day.withWorkouts(
            [for (final w in day.workouts) if (w.exerciseType == type) w],
          ),
      ],
      crossDailyRestingHR: crossDailyRestingHR,
    );
  }

  _ActivitiesLoadResult withoutEntry(String id) => _ActivitiesLoadResult(
        workouts: [for (final w in workouts) if (w.id != id) w],
        plannedWorkouts: plannedWorkouts,
        previousWorkouts: previousWorkouts,
        baselineWorkouts: baselineWorkouts,
        overviewDays: [
          for (final day in overviewDays)
            day.withWorkouts([for (final w in day.workouts) if (w.id != id) w]),
        ],
        crossDailyRestingHR: crossDailyRestingHR,
      );
}

/// The Riverpod port of the Kotlin `ActivitiesViewModel`. Loads the full
/// activities aggregate (workouts, planned workouts, previous/baseline windows,
/// the per-day overview with cardio-load, and the resting-HR cross series) for
/// the scaffold's current period.
class ActivitiesNotifier extends Notifier<ActivitiesState> {
  int _generation = 0;
  _ActivitiesLoadResult? _latestResult;

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
    final repo = ref.read(activityRepositoryProvider);
    final heartRepo = ref.read(heartRepositoryProvider);

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
    final windows = query.windows;
    final current = windows.current;
    final isYear = selection.selectedRange == TimeRange.year;

    try {
      final results = await (
        // Only the current window pays for the per-session distance/speed
        // aggregates — it is the one that renders the activity-type stats card.
        // Previous/baseline stay on the plain read (Kotlin `ActivitiesViewModel`
        // switched exactly this one call site).
        repo.loadWorkoutsWithMetrics(current.start, current.end),
        repo.loadPlannedWorkouts(current.start, current.end),
        repo.loadWorkouts(windows.previous.start, windows.previous.end),
        repo.loadWorkouts(windows.baseline.start, windows.baseline.end),
        repo.loadDailySteps(current.start, current.end),
        repo.loadDailyNutrition(current.start, current.end),
        heartRepo.loadDailyRestingHR(current.start, current.end),
        heartRepo.loadDailyHRV(current.start, current.end),
        isYear
            ? Future<List<HeartRateSample>>.value(const <HeartRateSample>[])
            : heartRepo.loadHeartRateSamples(current.start, current.end),
      ).wait;
      if (!ref.mounted || generation != _generation) return;

      final overviewDays = _activityOverviewDays(
        start: current.start,
        end: current.end,
        steps: results.$5,
        nutrition: results.$6,
        workouts: results.$1,
        heartRateSamples: results.$9,
        restingHeartRate: results.$7,
        hrv: results.$8,
      );

      final result = _ActivitiesLoadResult(
        workouts: results.$1,
        plannedWorkouts: results.$2,
        previousWorkouts: results.$3,
        baselineWorkouts: results.$4,
        overviewDays: overviewDays,
        crossDailyRestingHR: results.$7,
      );
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
      await ref.read(activityRepositoryProvider).deleteActivityEntry(entryId);
      await load(PeriodSelection(state.selectedRange, state.selectedDate));
    } catch (error) {
      _latestResult = previousResult;
      state = previousState.copyWith(
        error: throwableToScreenError(error,
            fallback: 'Unable to delete workout.'),
      );
    }
  }

  ActivitiesState _stateWithResult(
    ActivitiesState base,
    _ActivitiesLoadResult result,
    int? type,
  ) {
    final filtered = result.filteredBy(type);
    return base.copyWith(
      selectedActivityType: type,
      availableActivityTypes: result.availableActivityTypes(),
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

final activitiesNotifierProvider =
    NotifierProvider<ActivitiesNotifier, ActivitiesState>(
  ActivitiesNotifier.new,
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
        group.fold<int>(0, (sum, w) => sum + _movingDurationMs(w));
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
  final movingMs = _movingDurationMs(workout);
  if (movingMs <= 0) return null;
  return _finitePositive(distance / (movingMs / 1000.0));
}

/// Summed pause-segment duration of a workout, each coerced >= 0 and the total
/// capped at the workout's own duration (Kotlin `ActivityMetrics.pausedDurationMs`).
int _pausedDurationMs(ExerciseData workout) {
  final total = math.max(0, workout.durationMs);
  var paused = 0;
  for (final segment in workout.segments) {
    if (segment.segmentType == ExerciseSegmentType.pause) {
      paused += math.max(0, segment.durationMs);
    }
  }
  return math.min(paused, total);
}

/// Moving (non-paused) duration of a workout in ms
/// (Kotlin `ActivityMetrics.movingDurationMs`).
int _movingDurationMs(ExerciseData workout) =>
    math.max(0, math.max(0, workout.durationMs) - _pausedDurationMs(workout));

List<ActivityOverviewDay> _activityOverviewDays({
  required LocalDate start,
  required LocalDate end,
  required List<DailySteps> steps,
  required List<DailyNutrition> nutrition,
  required List<ExerciseData> workouts,
  required List<HeartRateSample> heartRateSamples,
  required List<DailyRestingHR> restingHeartRate,
  required List<DailyHrv> hrv,
}) {
  final stepsByDate = {for (final s in steps) s.date: s};
  final nutritionByDate = {for (final n in nutrition) n.date: n};
  final hrvByDate = {for (final h in hrv) h.date: h};
  final restingByDate = {for (final r in restingHeartRate) r.date: r.bpm};
  final samplesByDate = <LocalDate, List<HeartRateSample>>{};
  for (final sample in heartRateSamples) {
    samplesByDate
        .putIfAbsent(instantToLocalDate(sample.time), () => <HeartRateSample>[])
        .add(sample);
  }
  final baselineResting = _median(restingHeartRate.map((r) => r.bpm).toList());
  final observedMax = heartRateSamples.isEmpty
      ? null
      : heartRateSamples
          .map((s) => s.beatsPerMinute)
          .reduce((a, b) => a > b ? a : b);

  final days = <ActivityOverviewDay>[];
  var date = start;
  while (!date.isAfter(end)) {
    final daySteps = stepsByDate[date];
    final dayNutrition = nutritionByDate[date];
    final dayWorkouts = _overlapping(workouts, date);
    final estimate = calculateCardioLoad(
      daySteps,
      samplesByDate[date] ?? const <HeartRateSample>[],
      restingByDate[date],
      baselineResting,
      observedMax,
      _cardioWindows(dayWorkouts, date),
    );
    days.add(ActivityOverviewDay(
      date: date,
      steps: daySteps?.steps ?? 0,
      distanceMeters: daySteps?.distanceMeters ?? 0.0,
      activeCaloriesKcal: daySteps?.activeCaloriesKcal,
      energyBurnedKcal: dayNutrition?.caloriesBurnedKcal ?? 0.0,
      energyBurnedSource:
          dayNutrition?.caloriesBurnedSource ?? CaloriesBurnedSource.noData,
      workouts: dayWorkouts,
      hrvRmssdMs: hrvByDate[date]?.rmssdMs,
      cardioLoad: estimate.score,
      cardioLoadConfidence: estimate.confidence,
    ));
    date = date.plusDays(1);
  }
  return days;
}

List<ExerciseData> _overlapping(List<ExerciseData> workouts, LocalDate date) {
  final dayStart = DateTime(date.year, date.month, date.day);
  final dayEnd = dayStart.add(const Duration(days: 1));
  final result = [
    for (final w in workouts)
      if (w.endTime.toLocal().isAfter(dayStart) &&
          w.startTime.toLocal().isBefore(dayEnd))
        w,
  ]..sort((a, b) => b.startTime.compareTo(a.startTime));
  return result;
}

List<CardioLoadTimeWindow> _cardioWindows(
  List<ExerciseData> workouts,
  LocalDate date,
) {
  final dayStart = DateTime(date.year, date.month, date.day);
  final dayEnd = dayStart.add(const Duration(days: 1));
  final windows = <CardioLoadTimeWindow>[];
  for (final w in workouts) {
    final startLocal = w.startTime.toLocal();
    final endLocal = w.endTime.toLocal();
    final windowStart = startLocal.isBefore(dayStart) ? dayStart : startLocal;
    final windowEnd = endLocal.isAfter(dayEnd) ? dayEnd : endLocal;
    final window = CardioLoadTimeWindow(start: windowStart, end: windowEnd);
    if (window.durationMinutes > 0.0) windows.add(window);
  }
  return windows;
}

int? _median(List<int> values) {
  if (values.isEmpty) return null;
  final sorted = [...values]..sort();
  return sorted[(sorted.length - 1) ~/ 2];
}
