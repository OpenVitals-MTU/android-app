import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/time/local_date.dart';
import '../../../data/prefs/preferences_repository.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/exercise_session_metrics.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/activity_period_data.dart';
import '../../../health/health_data_source.dart';
import '../../../health/health_permissions.dart';
import '../contract/activity_repository.dart';
import 'repository_exceptions.dart';
import 'repository_time.dart';
import 'health_connect_gating.dart';

/// Port of the Kotlin `ActivityRepositoryImpl`.
class ActivityRepositoryImpl implements ActivityRepository {
  ActivityRepositoryImpl(
    this._dataSource, {
    PreferencesRepository? preferencesRepository,
    ActivityMarkerRepository? markerRepository,
  })  : _preferences = preferencesRepository,
        _markers = markerRepository;

  final HealthDataSource _dataSource;
  // ignore: unused_field
  final PreferencesRepository? _preferences;
  final ActivityMarkerRepository? _markers;

  @override
  Future<ActivityPeriodData> loadActivityPeriod(
    PeriodLoadQuery query, {
    required bool includeSteps,
    required bool includeNutrition,
    bool includeWheelchairPushes = false,
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final granted = await _dataSource.grantedIfAvailable();
    final w = query.windows;
    final includeActiveCalories = granted.contains(HcPermissions.readActiveCalories);

    Future<List<DailySteps>> steps(DatePeriod period) async {
      if (!(includeSteps || includeWheelchairPushes)) return const [];
      if (!granted.contains(HcPermissions.readSteps)) return const [];
      return _dataSource.readDailySteps(
        period.start,
        period.end,
        includeActiveCalories: includeActiveCalories,
      );
    }

    Future<List<DailyNutrition>> nutrition(DatePeriod period) async {
      if (!includeNutrition) return const [];
      if (!granted.contains(HcPermissions.readNutrition)) return const [];
      return _dataSource.readDailyNutrition(
        period.start,
        period.end,
        includeHydration: false,
      );
    }

    final activityProgress = query.range == TimeRange.day &&
            granted.contains(HcPermissions.readSteps)
        ? await _dataSource.readRawActivityProgress(w.current.start)
        : const <ActivityProgressPoint>[];

    return ActivityPeriodData(
      dailySteps: await steps(w.current),
      previousDailySteps: await steps(w.previous),
      baselineDailySteps: await steps(w.baseline),
      nutrition: await nutrition(w.current),
      previousNutrition: await nutrition(w.previous),
      baselineNutrition: await nutrition(w.baseline),
      activityProgress: activityProgress,
    );
  }

  @override
  Future<ActivitiesPeriodData> loadActivitiesPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) async {
    final w = query.windows;
    return ActivitiesPeriodData(
      workouts: await loadWorkouts(w.current.start, w.current.end),
      previousWorkouts: await loadWorkouts(w.previous.start, w.previous.end),
      baselineWorkouts: await loadWorkouts(w.baseline.start, w.baseline.end),
      plannedWorkouts: await loadPlannedWorkouts(w.current.start, w.current.end),
    );
  }

  @override
  Future<List<DailySteps>> loadDailySteps(LocalDate start, LocalDate end) async {
    final granted = await _dataSource.grantedIfAvailable();
    if (!granted.contains(HcPermissions.readSteps)) return const [];
    return _dataSource.readDailySteps(
      _activityHistoryStart(start, end, granted),
      end,
      includeActiveCalories: granted.contains(HcPermissions.readActiveCalories),
      includeFloors: granted.contains(HcPermissions.readFloors),
      includeElevation: granted.contains(HcPermissions.readElevation),
    );
  }

  /// Port of Kotlin `ActivityRepository.activityHistoryStart`: when the platform
  /// gates historical reads behind READ_HEALTH_DATA_HISTORY and that permission
  /// is not granted, Health Connect only surfaces the last 30 days, so scanning
  /// back to the legacy 2009 start is pointless — clamp to `end - 29 days`.
  LocalDate _activityHistoryStart(
    LocalDate start,
    LocalDate end,
    Set<String> granted,
  ) {
    final historyRequired = _dataSource
        .permissionService.additionalDataAccessPermissions
        .contains(HealthPermissionService.readHealthDataHistoryPermission);
    if (historyRequired &&
        !granted
            .contains(HealthPermissionService.readHealthDataHistoryPermission)) {
      final clamped = end.minusDays(29);
      return clamped.isAfter(start) ? clamped : start;
    }
    return start;
  }

  @override
  Future<List<ActivityProgressPoint>> loadActivityProgress({
    LocalDate? date,
  }) async {
    final granted = await _dataSource.grantedIfAvailable();
    if (!granted.contains(HcPermissions.readSteps)) return const [];
    return _dataSource.readRawActivityProgress(date ?? LocalDate.now());
  }

  @override
  Future<List<ExerciseData>> loadWorkouts(LocalDate start, LocalDate end) async {
    final granted = await _dataSource.grantedIfAvailable();
    if (!granted.contains(HcPermissions.readExercise)) return const [];
    return _dataSource.readExerciseSessions(localDayStart(start), localDayEnd(end));
  }

  @override
  Future<List<ExerciseData>> loadWorkoutsWithMetrics(
    LocalDate start,
    LocalDate end,
  ) async {
    final granted = await _dataSource.grantedIfAvailable();
    if (!granted.contains(HcPermissions.readExercise)) return const [];
    // Distance / speed are gated independently: an ungranted metric is left out
    // of the aggregate and comes back null, rather than failing the read.
    return _dataSource.readExerciseSessionsWithMetrics(
      localDayStart(start),
      localDayEnd(end),
      includeDistance: granted.contains(HcPermissions.readDistance),
      includeSpeed: granted.contains(HcPermissions.readSpeed),
    );
  }

  @override
  Future<ExerciseData?> loadWorkout(String id) =>
      _dataSource.readExerciseSession(id);

  /// Each metric is gated on its OWN read permission, exactly as the list read
  /// gates distance and speed: an ungranted metric is simply left out of the
  /// request and comes back null, instead of failing the whole read.
  @override
  Future<ExerciseSessionMetrics> loadWorkoutMetrics(
    DateTime start,
    DateTime end,
  ) async {
    final granted = await _dataSource.grantedIfAvailable();
    final wanted = <ExerciseSessionMetric>{
      if (granted.contains(HcPermissions.readDistance))
        ExerciseSessionMetric.distance,
      if (granted.contains(HcPermissions.readSpeed)) ExerciseSessionMetric.speed,
      if (granted.contains(HcPermissions.readSteps)) ExerciseSessionMetric.steps,
      if (granted.contains(HcPermissions.readTotalCalories))
        ExerciseSessionMetric.totalCalories,
      if (granted.contains(HcPermissions.readActiveCalories))
        ExerciseSessionMetric.activeCalories,
      if (granted.contains(HcPermissions.readElevation))
        ExerciseSessionMetric.elevation,
      if (granted.contains(HcPermissions.readFloors))
        ExerciseSessionMetric.floors,
      if (granted.contains(HcPermissions.readWheelchairPushes))
        ExerciseSessionMetric.wheelchairPushes,
    };
    if (wanted.isEmpty) return ExerciseSessionMetrics.none;
    return _dataSource.readExerciseSessionMetrics(start, end, wanted);
  }

  @override
  Future<List<SpeedSample>> loadSpeedSamples(DateTime start, DateTime end) async {
    // Gated like every other metric read: without the SPEED permission the
    // caller gets "no speed samples", not an exception. The activity detail
    // screen degrades to route-derived (or estimated) splits.
    final granted = await _dataSource.grantedIfAvailable();
    if (!granted.contains(HcPermissions.readSpeed)) return const [];
    return _dataSource.readSpeedSamples(start, end);
  }

  @override
  Future<List<ActivityCadenceSample>> loadActivityCadenceSamples(
    DateTime start,
    DateTime end,
  ) =>
      _dataSource.readActivityCadenceSamples(start, end);

  @override
  Future<List<PlannedExerciseData>> loadPlannedWorkouts(
    LocalDate start,
    LocalDate end,
  ) async {
    if (!_dataSource.isPlannedExerciseAvailable()) return const [];
    return _dataSource.readPlannedExerciseSessions(
        localDayStart(start), localDayEnd(end));
  }

  @override
  Future<List<PlannedExerciseData>> loadPlannedWorkoutOptions(
    LocalDate date,
    int exerciseType,
  ) async =>
      const [];

  @override
  Future<List<PlannedExerciseData>> loadExistingPlannedWorkouts({
    LocalDate? anchorDate,
  }) async =>
      const [];

  @override
  Future<String> writePlannedWorkout(PlannedExerciseWriteRequest request) async {
    // TODO(health-pkg): PlannedExerciseSession writes are unsupported.
    throw const MissingHealthPermissionException(
      'Planned workouts are not supported by the health package.',
    );
  }

  @override
  Future<List<DailyNutrition>> loadDailyNutrition(
    LocalDate start,
    LocalDate end,
  ) async {
    final granted = await _dataSource.grantedIfAvailable();
    if (!granted.contains(HcPermissions.readNutrition)) return const [];
    return _dataSource.readDailyNutrition(start, end, includeHydration: false);
  }

  // ── Write permissions ─────────────────────────────────────────────────────

  @override
  Set<String> activityWritePermissions() => {HcPermissions.writeExercise};

  @override
  Set<String> activityWritePermissionsFor({
    required bool includeRoute,
    required bool includeDistance,
    required bool includeElevation,
    required bool includeActiveCalories,
    required bool includeTotalCalories,
    bool includeSteps = false,
  }) =>
      {
        HcPermissions.writeExercise,
        if (includeRoute) HcPermissions.writeExerciseRoute,
        if (includeDistance) HcPermissions.writeDistance,
        if (includeElevation) HcPermissions.writeElevation,
        if (includeActiveCalories) HcPermissions.writeActiveCalories,
        if (includeTotalCalories) HcPermissions.writeTotalCalories,
        if (includeSteps) HcPermissions.writeSteps,
      };

  @override
  Set<String> activityWritePermissionsForRequest(ActivityWriteRequest request) =>
      activityWritePermissionsFor(
        includeRoute: request.routePoints.isNotEmpty,
        includeDistance: request.distanceMeters != null,
        includeElevation: request.elevationGainedMeters != null,
        includeActiveCalories: request.activeCaloriesKcal != null,
        includeTotalCalories: request.totalCaloriesKcal != null,
        includeSteps: request.stepsCount != null,
      );

  @override
  Set<String> plannedWorkoutWritePermissions() =>
      _dataSource.permissionService.plannedExercisePermissions;

  @override
  Future<bool> hasActivityWritePermission() async {
    final granted = await _dataSource.grantedIfAvailable();
    return granted.containsAll(activityWritePermissions());
  }

  @override
  Future<bool> hasActivityWritePermissionFor({
    required bool includeRoute,
    required bool includeDistance,
    required bool includeElevation,
    required bool includeActiveCalories,
    required bool includeTotalCalories,
    bool includeSteps = false,
  }) async {
    final granted = await _dataSource.grantedIfAvailable();
    return granted.containsAll(
      activityWritePermissionsFor(
        includeRoute: includeRoute,
        includeDistance: includeDistance,
        includeElevation: includeElevation,
        includeActiveCalories: includeActiveCalories,
        includeTotalCalories: includeTotalCalories,
        includeSteps: includeSteps,
      ),
    );
  }

  @override
  Future<bool> hasActivityWritePermissionForRequest(
    ActivityWriteRequest request,
  ) async {
    final granted = await _dataSource.grantedIfAvailable();
    return granted.containsAll(activityWritePermissionsForRequest(request));
  }

  @override
  Future<String> writeActivityEntry(ActivityWriteRequest request) async {
    if (!await hasActivityWritePermissionForRequest(request)) {
      throw const MissingHealthPermissionException(
        'Missing Health Connect activity write permission.',
      );
    }
    return _dataSource.writeActivityEntry(request);
  }

  @override
  Future<void> updateActivityEntry(String id, ActivityWriteRequest request) async {
    if (!await hasActivityWritePermissionForRequest(request)) {
      throw const MissingHealthPermissionException(
        'Missing Health Connect activity write permission.',
      );
    }
    await _dataSource.updateActivityEntry(id, request);
  }

  @override
  Future<void> deleteActivityEntry(String id) async {
    if (!await hasActivityWritePermission()) {
      throw const MissingHealthPermissionException(
        'Missing Health Connect activity write permission.',
      );
    }
    await _dataSource.deleteActivityEntry(id);
    _markers?.deleteMarkersForActivity(id);
  }
}
