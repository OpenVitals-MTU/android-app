import '../../../core/period/period_load_query.dart';
import '../../../core/period/time_range.dart';
import '../../../core/result/result.dart';
import '../../../core/time/local_date.dart';
import '../../prefs/preferences_repository.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/exercise_session_metrics.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/refresh_mode.dart';
import '../../../domain/query/activity_period_data.dart';
import '../../source/health/health_data_source.dart';
import '../../../domain/health/health_permissions.dart';
import '../contract/activity_repository.dart';
import '../contract/repository_exceptions.dart';
import 'repository_time.dart';
import 'health_connect_gating.dart';
import 'run_catching.dart';

/// Port of the Kotlin `ActivityRepositoryImpl`.
///
/// Public methods convert exceptions to failures via [runCatching] at the
/// boundary; the private `_raw` bodies keep the original throwing flow so
/// internal composition ([loadActivitiesPeriod], the gated writes) stays
/// plain awaits.
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
  Future<Result<ActivityPeriodData>> loadActivityPeriod(
    PeriodLoadQuery query, {
    required bool includeSteps,
    required bool includeNutrition,
    bool includeWheelchairPushes = false,
    bool includeActivityProgress = true,
    bool includeComparisonWindows = true,
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        final w = query.windows;
        final includeActiveCalories =
            granted.contains(HcPermissions.readActiveCalories);

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

        final activityProgress = includeActivityProgress &&
                query.range == TimeRange.day &&
                granted.contains(HcPermissions.readSteps)
            ? await _dataSource.readRawActivityProgress(w.current.start)
            : const <ActivityProgressPoint>[];

        // The window reads are independent (distinct date ranges, distinct
        // metrics), so fire them together rather than in series. Over a YEAR the
        // current and previous windows are each a 365-day Health Connect
        // aggregate, and `TotalCaloriesBurned` is heavy to synthesize; awaiting
        // them one by one stacked into the ~45s the calories year view took to
        // open. Concurrent reads collapse that to roughly a single window's
        // latency. A failing read still fails the whole load (via runCatching),
        // exactly as the sequential awaits did.
        //
        // The previous/baseline windows exist only for the movement-metric
        // screen's period comparison; the calories overview never reads them, so
        // it opts out ([includeComparisonWindows] = false) and skips four more
        // year-long aggregates — the difference between two concurrent reads and
        // six.
        Future<List<T>> skip<T>() => Future.value(const []);
        final (
          currentSteps,
          previousSteps,
          baselineSteps,
          currentNutrition,
          previousNutrition,
          baselineNutrition,
        ) = await (
          steps(w.current),
          includeComparisonWindows ? steps(w.previous) : skip<DailySteps>(),
          includeComparisonWindows ? steps(w.baseline) : skip<DailySteps>(),
          nutrition(w.current),
          includeComparisonWindows
              ? nutrition(w.previous)
              : skip<DailyNutrition>(),
          includeComparisonWindows
              ? nutrition(w.baseline)
              : skip<DailyNutrition>(),
        ).wait;

        return ActivityPeriodData(
          dailySteps: currentSteps,
          previousDailySteps: previousSteps,
          baselineDailySteps: baselineSteps,
          nutrition: currentNutrition,
          previousNutrition: previousNutrition,
          baselineNutrition: baselineNutrition,
          activityProgress: activityProgress,
        );
      });

  @override
  Future<Result<ActivitiesPeriodData>> loadActivitiesPeriod(
    PeriodLoadQuery query, {
    RefreshMode refreshMode = RefreshMode.normal,
  }) =>
      runCatching(() async {
        final w = query.windows;
        return ActivitiesPeriodData(
          workouts: await _loadWorkoutsRaw(w.current.start, w.current.end),
          previousWorkouts:
              await _loadWorkoutsRaw(w.previous.start, w.previous.end),
          baselineWorkouts:
              await _loadWorkoutsRaw(w.baseline.start, w.baseline.end),
          plannedWorkouts:
              await _loadPlannedWorkoutsRaw(w.current.start, w.current.end),
        );
      });

  @override
  Future<Result<List<DailySteps>>> loadDailySteps(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readSteps)) return const [];
        return _dataSource.readDailySteps(
          _activityHistoryStart(start, end, granted),
          end,
          includeActiveCalories:
              granted.contains(HcPermissions.readActiveCalories),
          includeFloors: granted.contains(HcPermissions.readFloors),
          includeElevation: granted.contains(HcPermissions.readElevation),
        );
      });

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
  Future<Result<List<ActivityProgressPoint>>> loadActivityProgress({
    LocalDate? date,
  }) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readSteps)) return const [];
        return _dataSource.readRawActivityProgress(date ?? LocalDate.now());
      });

  @override
  Future<Result<List<ExerciseData>>> loadWorkouts(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() => _loadWorkoutsRaw(start, end));

  Future<List<ExerciseData>> _loadWorkoutsRaw(
    LocalDate start,
    LocalDate end,
  ) async {
    final granted = await _dataSource.grantedIfAvailable();
    if (!granted.contains(HcPermissions.readExercise)) return const [];
    return _dataSource.readExerciseSessions(localDayStart(start), localDayEnd(end));
  }

  @override
  Future<Result<List<ExerciseData>>> loadWorkoutsWithMetrics(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readExercise)) return const [];
        // Distance / speed are gated independently: an ungranted metric is left
        // out of the aggregate and comes back null, rather than failing the read.
        return _dataSource.readExerciseSessionsWithMetrics(
          localDayStart(start),
          localDayEnd(end),
          includeDistance: granted.contains(HcPermissions.readDistance),
          includeSpeed: granted.contains(HcPermissions.readSpeed),
        );
      });

  @override
  Future<Result<ExerciseData?>> loadWorkout(String id) =>
      runCatching(() => _dataSource.readExerciseSession(id));

  /// Each metric is gated on its OWN read permission, exactly as the list read
  /// gates distance and speed: an ungranted metric is simply left out of the
  /// request and comes back null, instead of failing the whole read.
  @override
  Future<Result<ExerciseSessionMetrics>> loadWorkoutMetrics(
    DateTime start,
    DateTime end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        final wanted = <ExerciseSessionMetric>{
          if (granted.contains(HcPermissions.readDistance))
            ExerciseSessionMetric.distance,
          if (granted.contains(HcPermissions.readSpeed))
            ExerciseSessionMetric.speed,
          if (granted.contains(HcPermissions.readSteps))
            ExerciseSessionMetric.steps,
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
          if (granted.contains(HcPermissions.readPower))
            ExerciseSessionMetric.power,
        };
        if (wanted.isEmpty) return ExerciseSessionMetrics.none;
        return _dataSource.readExerciseSessionMetrics(start, end, wanted);
      });

  @override
  Future<Result<List<SpeedSample>>> loadSpeedSamples(
    DateTime start,
    DateTime end,
  ) =>
      runCatching(() async {
        // Gated like every other metric read: without the SPEED permission the
        // caller gets "no speed samples", not a failure. The activity detail
        // screen degrades to route-derived (or estimated) splits.
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readSpeed)) return const [];
        return _dataSource.readSpeedSamples(start, end);
      });

  @override
  Future<Result<List<ActivityCadenceSample>>> loadActivityCadenceSamples(
    DateTime start,
    DateTime end,
  ) =>
      runCatching(() => _dataSource.readActivityCadenceSamples(start, end));

  @override
  Future<Result<List<PlannedExerciseData>>> loadPlannedWorkouts(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() => _loadPlannedWorkoutsRaw(start, end));

  Future<List<PlannedExerciseData>> _loadPlannedWorkoutsRaw(
    LocalDate start,
    LocalDate end,
  ) async {
    if (!_dataSource.isPlannedExerciseAvailable()) return const [];
    return _dataSource.readPlannedExerciseSessions(
        localDayStart(start), localDayEnd(end));
  }

  @override
  Future<Result<List<PlannedExerciseData>>> loadPlannedWorkoutOptions(
    LocalDate date,
    int exerciseType,
  ) async =>
      const Ok([]);

  @override
  Future<Result<List<PlannedExerciseData>>> loadExistingPlannedWorkouts({
    LocalDate? anchorDate,
  }) async =>
      const Ok([]);

  @override
  Future<Result<String>> writePlannedWorkout(
    PlannedExerciseWriteRequest request,
  ) =>
      runCatching(() async {
        // TODO(health-pkg): PlannedExerciseSession writes are unsupported.
        throw const MissingHealthPermissionException(
          'Planned workouts are not supported by the health package.',
        );
      });

  @override
  Future<Result<List<DailyNutrition>>> loadDailyNutrition(
    LocalDate start,
    LocalDate end,
  ) =>
      runCatching(() async {
        final granted = await _dataSource.grantedIfAvailable();
        if (!granted.contains(HcPermissions.readNutrition)) return const [];
        return _dataSource.readDailyNutrition(start, end, includeHydration: false);
      });

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
  Set<String> activityWritePermissionsForRequest(ActivityWriteRequest request) {
    final ble = request.bleSamples;
    return {
      ...activityWritePermissionsFor(
        includeRoute: request.routePoints.isNotEmpty,
        includeDistance: request.distanceMeters != null,
        includeElevation: request.elevationGainedMeters != null,
        includeActiveCalories: request.activeCaloriesKcal != null,
        includeTotalCalories: request.totalCaloriesKcal != null,
        includeSteps: request.stepsCount != null,
      ),
      // The sensor series a recorded activity carries. These were missing, and the
      // omission was not cosmetic: the session and every one of these records go to
      // Health Connect in ONE atomic insertRecords call, so a user who granted
      // WRITE_EXERCISE but not WRITE_HEART_RATE had the whole save thrown — after a
      // permission check that had just told them everything was in order.
      //
      // One permission per series that actually has samples, mirroring the native
      // writer, which skips an empty series rather than writing an empty record.
      if (ble.heartRateSamples.isNotEmpty) HcPermissions.writeHeartRate,
      if (ble.powerSamples.isNotEmpty) HcPermissions.writePower,
      if (ble.speedSamples.isNotEmpty) HcPermissions.writeSpeed,
      if (ble.cyclingCadenceSamples.isNotEmpty) HcPermissions.writeCyclingCadence,
      if (ble.stepsCadenceSamples.isNotEmpty) HcPermissions.writeStepsCadence,
    };
  }

  @override
  Set<String> plannedWorkoutWritePermissions() =>
      _dataSource.permissionService.plannedExercisePermissions;

  @override
  Future<Result<bool>> hasActivityWritePermission() =>
      runCatching(_hasWritePermissionRaw);

  Future<bool> _hasWritePermissionRaw() async {
    final granted = await _dataSource.grantedIfAvailable();
    return granted.containsAll(activityWritePermissions());
  }

  @override
  Future<Result<bool>> hasActivityWritePermissionFor({
    required bool includeRoute,
    required bool includeDistance,
    required bool includeElevation,
    required bool includeActiveCalories,
    required bool includeTotalCalories,
    bool includeSteps = false,
  }) =>
      runCatching(() async {
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
      });

  @override
  Future<Result<bool>> hasActivityWritePermissionForRequest(
    ActivityWriteRequest request,
  ) =>
      runCatching(() => _hasWritePermissionForRequestRaw(request));

  Future<bool> _hasWritePermissionForRequestRaw(
    ActivityWriteRequest request,
  ) async {
    final granted = await _dataSource.grantedIfAvailable();
    return granted.containsAll(activityWritePermissionsForRequest(request));
  }

  @override
  Future<Result<String>> writeActivityEntry(ActivityWriteRequest request) =>
      runCatching(() async {
        if (!await _hasWritePermissionForRequestRaw(request)) {
          throw const MissingHealthPermissionException(
            'Missing Health Connect activity write permission.',
          );
        }
        return _dataSource.writeActivityEntry(request);
      });

  @override
  Future<Result<List<String>>> writeActivityEntries(
    List<ActivityWriteRequest> requests,
  ) =>
      runCatching(() async {
        if (requests.isEmpty) return const <String>[];
        // ONE permission read for the whole batch, not one per request: the
        // granted set cannot change midway through a single call, and the point of
        // batching is to stop talking to Health Connect more than we must.
        final granted = await _dataSource.grantedIfAvailable();
        final needed = <String>{
          for (final request in requests)
            ...activityWritePermissionsForRequest(request),
        };
        if (!granted.containsAll(needed)) {
          throw const MissingHealthPermissionException(
            'Missing Health Connect activity write permission.',
          );
        }
        return _dataSource.writeActivityEntries(requests);
      });

  @override
  Future<Result<void>> updateActivityEntry(
    String id,
    ActivityWriteRequest request,
  ) =>
      runCatching(() async {
        if (!await _hasWritePermissionForRequestRaw(request)) {
          throw const MissingHealthPermissionException(
            'Missing Health Connect activity write permission.',
          );
        }
        await _dataSource.updateActivityEntry(id, request);
      });

  @override
  Future<Result<void>> deleteActivityEntry(String id) =>
      runCatching(() async {
        if (!await _hasWritePermissionRaw()) {
          throw const MissingHealthPermissionException(
            'Missing Health Connect activity write permission.',
          );
        }
        await _dataSource.deleteActivityEntry(id);
        _markers?.deleteMarkersForActivity(id);
      });
}
