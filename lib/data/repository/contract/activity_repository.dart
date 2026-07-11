import '../../../core/period/period_load_query.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/activity_period_data.dart';

/// Port of the Kotlin `ActivityRepository` contract.
///
/// Kotlin overloads (`activityWritePermissions` / `hasActivityWritePermission`)
/// are disambiguated with distinct names since Dart has no method overloading.
abstract interface class ActivityRepository {
  Future<ActivityPeriodData> loadActivityPeriod(
    PeriodLoadQuery query, {
    required bool includeSteps,
    required bool includeNutrition,
    bool includeWheelchairPushes = false,
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<ActivitiesPeriodData> loadActivitiesPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<List<DailySteps>> loadDailySteps(LocalDate start, LocalDate end);

  Future<List<ActivityProgressPoint>> loadActivityProgress({LocalDate? date});

  Future<List<ExerciseData>> loadWorkouts(LocalDate start, LocalDate end);

  /// [loadWorkouts] plus the per-session route metrics (total distance / average
  /// speed) that only a Health Connect aggregate over each session's window can
  /// produce. Costs one aggregate per session, so it is reserved for the window
  /// that actually renders those metrics.
  Future<List<ExerciseData>> loadWorkoutsWithMetrics(
    LocalDate start,
    LocalDate end,
  );

  Future<ExerciseData?> loadWorkout(String id);

  Future<List<SpeedSample>> loadSpeedSamples(DateTime start, DateTime end);

  Future<List<ActivityCadenceSample>> loadActivityCadenceSamples(
    DateTime start,
    DateTime end,
  );

  Future<List<PlannedExerciseData>> loadPlannedWorkouts(
    LocalDate start,
    LocalDate end,
  );

  Future<List<PlannedExerciseData>> loadPlannedWorkoutOptions(
    LocalDate date,
    int exerciseType,
  );

  Future<List<PlannedExerciseData>> loadExistingPlannedWorkouts({
    LocalDate? anchorDate,
  });

  Future<String> writePlannedWorkout(PlannedExerciseWriteRequest request);

  Future<List<DailyNutrition>> loadDailyNutrition(LocalDate start, LocalDate end);

  Set<String> activityWritePermissions();

  Set<String> activityWritePermissionsFor({
    required bool includeRoute,
    required bool includeDistance,
    required bool includeElevation,
    required bool includeActiveCalories,
    required bool includeTotalCalories,
    bool includeSteps = false,
  });

  Set<String> activityWritePermissionsForRequest(ActivityWriteRequest request);

  Set<String> plannedWorkoutWritePermissions();

  Future<bool> hasActivityWritePermission();

  Future<bool> hasActivityWritePermissionFor({
    required bool includeRoute,
    required bool includeDistance,
    required bool includeElevation,
    required bool includeActiveCalories,
    required bool includeTotalCalories,
    bool includeSteps = false,
  });

  Future<bool> hasActivityWritePermissionForRequest(
    ActivityWriteRequest request,
  );

  Future<String> writeActivityEntry(ActivityWriteRequest request);

  Future<void> updateActivityEntry(String id, ActivityWriteRequest request);

  Future<void> deleteActivityEntry(String id);
}

/// Port of the Kotlin `ActivityMarkerRepository` (a SharedPreferences-backed
/// per-activity marker store; not Health Connect).
abstract interface class ActivityMarkerRepository {
  List<ActivityRecordingMarker> markersForActivity(String activityId);

  void setMarkersForActivity(
    String activityId,
    List<ActivityRecordingMarker> markers,
  );

  void deleteMarkersForActivity(String activityId);
}
