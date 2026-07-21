import '../../../core/period/period_load_query.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/exercise_session_metrics.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/activity_period_data.dart';

/// Port of the Kotlin `ActivityRepository` contract.
///
/// Kotlin overloads (`activityWritePermissions` / `hasActivityWritePermission`)
/// are disambiguated with distinct names since Dart has no method overloading.
///
/// Fallible operations return [Result]; the synchronous permission-set
/// probes ([activityWritePermissions] and friends) read cached state and
/// cannot fail, so they stay bare.
abstract interface class ActivityRepository {
  Future<Result<ActivityPeriodData>> loadActivityPeriod(
    PeriodLoadQuery query, {
    required bool includeSteps,
    required bool includeNutrition,
    bool includeWheelchairPushes = false,
    // The intraday cumulative series (Day range only). It costs an extra Health
    // Connect `aggregateGroupByDuration` call, so a caller that never renders the
    // intraday chart (e.g. the calories overview) opts out — one fewer read, and
    // one fewer place a stalled aggregate can hang the Day view.
    bool includeActivityProgress = true,
    // The previous/baseline comparison windows (two extra period reads each for
    // steps and nutrition). Only the movement-metric screen renders a
    // period-over-period comparison; overviews that show the current period
    // alone (e.g. calories) opt out, turning six window aggregates into two.
    bool includeComparisonWindows = true,
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<Result<ActivitiesPeriodData>> loadActivitiesPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  });

  Future<Result<List<DailySteps>>> loadDailySteps(LocalDate start, LocalDate end);

  Future<Result<List<ActivityProgressPoint>>> loadActivityProgress({
    LocalDate? date,
  });

  Future<Result<List<ExerciseData>>> loadWorkouts(LocalDate start, LocalDate end);

  /// [loadWorkouts] plus the per-session route metrics (total distance / average
  /// speed) that only a Health Connect aggregate over each session's window can
  /// produce. Costs one aggregate per session, so it is reserved for the window
  /// that actually renders those metrics.
  Future<Result<List<ExerciseData>>> loadWorkoutsWithMetrics(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<ExerciseData?>> loadWorkout(String id);

  /// The steps / distance / calories / elevation totals a session record does not
  /// carry, aggregated over its own window. Only the metrics the user has granted
  /// a read permission for come back; the rest stay null.
  Future<Result<ExerciseSessionMetrics>> loadWorkoutMetrics(
    DateTime start,
    DateTime end,
  );

  Future<Result<List<SpeedSample>>> loadSpeedSamples(DateTime start, DateTime end);

  Future<Result<List<ActivityCadenceSample>>> loadActivityCadenceSamples(
    DateTime start,
    DateTime end,
  );

  Future<Result<List<PlannedExerciseData>>> loadPlannedWorkouts(
    LocalDate start,
    LocalDate end,
  );

  Future<Result<List<PlannedExerciseData>>> loadPlannedWorkoutOptions(
    LocalDate date,
    int exerciseType,
  );

  Future<Result<List<PlannedExerciseData>>> loadExistingPlannedWorkouts({
    LocalDate? anchorDate,
  });

  Future<Result<String>> writePlannedWorkout(PlannedExerciseWriteRequest request);

  Future<Result<List<DailyNutrition>>> loadDailyNutrition(
    LocalDate start,
    LocalDate end,
  );

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

  Future<Result<bool>> hasActivityWritePermission();

  Future<Result<bool>> hasActivityWritePermissionFor({
    required bool includeRoute,
    required bool includeDistance,
    required bool includeElevation,
    required bool includeActiveCalories,
    required bool includeTotalCalories,
    bool includeSteps = false,
  });

  Future<Result<bool>> hasActivityWritePermissionForRequest(
    ActivityWriteRequest request,
  );

  Future<Result<String>> writeActivityEntry(ActivityWriteRequest request);

  /// Writes several activities in ONE Health Connect call, returning a record id
  /// per request, in order.
  ///
  /// Health Connect charges its rate limit per API CALL, not per record, so a bulk
  /// import that writes one activity at a time spends a unit of quota per file and
  /// exhausts the daily allowance after a couple of thousand.
  ///
  /// The call is atomic: if Health Connect rejects one record, NOTHING in the batch
  /// is written. A caller that cares which file was at fault must retry the batch
  /// as single writes.
  Future<Result<List<String>>> writeActivityEntries(
    List<ActivityWriteRequest> requests,
  );

  Future<Result<void>> updateActivityEntry(String id, ActivityWriteRequest request);

  Future<Result<void>> deleteActivityEntry(String id);
}

/// Port of the Kotlin `ActivityMarkerRepository` (a SharedPreferences-backed
/// per-activity marker store; not Health Connect).
///
/// Deliberately not [Result]-typed: every operation is a synchronous access to
/// SharedPreferences' in-memory cache (the persist behind [setMarkersForActivity]
/// is fire-and-forget, as in Kotlin), so there is no failure to type — the same
/// rule that keeps the other contracts' cached-state probes bare.
abstract interface class ActivityMarkerRepository {
  List<ActivityRecordingMarker> markersForActivity(String activityId);

  void setMarkersForActivity(
    String activityId,
    List<ActivityRecordingMarker> markers,
  );

  void deleteMarkersForActivity(String activityId);
}
