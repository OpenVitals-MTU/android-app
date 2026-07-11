import 'dart:convert';

import 'package:health_connect_native/health_connect_native.dart';

import '../../core/time/local_date.dart';
import '../../domain/model/activity_backfill.dart';
import '../../domain/model/activity_models.dart';
import '../../domain/model/exercise_session_metrics.dart';
import '../../domain/model/activity_session_deduplication.dart';
import '../../domain/model/body_models.dart';
import '../../domain/model/cycle_models.dart';
import '../../domain/model/health_connect_availability.dart';
import '../../domain/model/health_connect_feature_status.dart';
import '../../domain/model/heart_models.dart';
import '../../domain/model/heart_rate_aggregated_samples.dart';
import '../../domain/model/mindfulness_models.dart';
import '../../domain/model/nutrition_models.dart';
import '../../domain/model/sleep_models.dart';
import '../../domain/model/sleep_session_merging.dart';
import '../../domain/model/vitals_models.dart';
import '../../domain/preferences/sleep_range_mode.dart';
import '../../features/imports/applehealth/apple_health_import_records.dart';
import '../health_data_source.dart';
import '../health_permissions.dart';
import 'health_record_json.dart';
import 'import_record_mapper.dart';

/// Real [HealthDataSource] over the native AndroidX Health Connect plugin
/// ([HealthConnectHostApi]).
///
/// Records cross the bridge as JSON strings (canonical schema in
/// `packages/health_connect_native`); this source `jsonDecode`s them and maps
/// through [HealthRecordJson]. Daily activity totals are read via the Health
/// Connect aggregation API; individual entries via `readRecordsJson`.
///
/// Every read is defensive (per-metric degrade to empty/null), mirroring the
/// Kotlin readers which swallow Health Connect failures per metric. Reads the
/// old `health`-package impl left unimplemented (base empty defaults) stay that
/// way here for parity; the two genuine gains the native bridge unlocks —
/// `clientRecordId` round-tripping and full Apple-Health import coverage — are
/// implemented (see [insertImportedRecords] / [findMatchingImportedClientRecordIds]).
class HealthConnectNativeDataSource extends HealthDataSource {
  HealthConnectNativeDataSource({
    HealthConnectHostApi? hostApi,
    super.appPackageName,
  }) : _api = hostApi ?? HealthConnectHostApi();

  final HealthConnectHostApi _api;
  // ── Time helpers (device-local day boundaries, as in the Kotlin readers) ──
  DateTime _dayStart(LocalDate date) => DateTime(date.year, date.month, date.day);
  DateTime _dayEnd(LocalDate date) => _dayStart(date.plusDays(1));

  /// Maps a Pigeon epoch-millis field back to a local [DateTime].
  DateTime _fromMs(int epochMs) => DateTime.fromMillisecondsSinceEpoch(epochMs);

  Future<T> _catch<T>(Future<T> Function() block, T fallback) async {
    try {
      return await block();
    } catch (_) {
      return fallback;
    }
  }

  Future<Map<String, double?>> _aggregate(
    List<String> metrics,
    DateTime start,
    DateTime end,
  ) =>
      _catch(
        () => _api.aggregate(
          metrics,
          start.millisecondsSinceEpoch,
          end.millisecondsSinceEpoch,
        ),
        <String, double?>{for (final m in metrics) m: null},
      );

  // ── Availability / permissions ────────────────────────────────────────────

  @override
  Future<HealthConnectAvailability> availability() async {
    // HealthConnectClient SDK status: SDK_UNAVAILABLE=1,
    // SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED=2, SDK_AVAILABLE=3.
    final status = await _catch(() => _api.getSdkStatus(), 1);
    final resolved = switch (status) {
      3 => HealthConnectAvailability.available,
      2 => HealthConnectAvailability.needsProviderUpdate,
      _ => HealthConnectAvailability.notSupported,
    };
    cachedAvailability = resolved;
    return resolved;
  }

  @override
  Future<FeatureStatus> getFeatureStatus(String feature) => _catch(
        () async => _mapFeatureStatus(await _api.getFeatureStatus(feature)),
        FeatureStatus.unknown,
      );

  static FeatureStatus _mapFeatureStatus(FeatureStatusMsg status) =>
      switch (status) {
        FeatureStatusMsg.available => FeatureStatus.available,
        FeatureStatusMsg.unavailable => FeatureStatus.unavailable,
        FeatureStatusMsg.unknown => FeatureStatus.unknown,
      };

  @override
  Future<void> resolveSupportedPermissions() async {
    // Reset first so [permissionService] yields the full (unfiltered) universe
    // of permissions the app might request under the current feature flags.
    unsupportedPermissions = const <String>{};
    final universe = permissionService.managedPermissions.toList();
    final supported = await _catch(
      () => _api.filterSupportedPermissions(universe),
      universe,
    );
    unsupportedPermissions = universe.toSet().difference(supported.toSet());
  }

  @override
  Future<HealthConnectFeatureFlags> resolveFeatureFlags() async {
    Future<bool> available(String name) async =>
        (await getFeatureStatus(name)).isAvailable;
    // Resolve the feature checks concurrently rather than sequentially so
    // onboarding startup does one round-trip's worth of latency, not five.
    final results = await Future.wait(<Future<bool>>[
      available('SKIN_TEMPERATURE'),
      available('MINDFULNESS_SESSION'),
      available('PLANNED_EXERCISE'),
      available('READ_HEALTH_DATA_HISTORY'),
      available('READ_HEALTH_DATA_IN_BACKGROUND'),
    ]);
    final flags = HealthConnectFeatureFlags(
      skinTemperatureAvailable: results[0],
      mindfulnessAvailable: results[1],
      plannedExerciseAvailable: results[2],
      healthDataHistoryAvailable: results[3],
      backgroundReadAvailable: results[4],
    );
    featureFlags = flags;
    return flags;
  }

  @override
  Future<bool> requestPermissions(Set<String> permissions) async {
    if (permissions.isEmpty) return false;
    return _catch(
      () => _api.requestPermissions(permissions.toList()),
      false,
    );
  }

  @override
  Future<bool> openHealthConnectSettings() =>
      _catch(() => _api.openHealthConnectSettings(), false);

  @override
  Future<Set<String>> grantedPermissions() async {
    final managed = permissionService.managedPermissions.toList();
    if (managed.isEmpty) return const <String>{};
    final granted = await _catch(
      () => _api.getGrantedPermissions(managed),
      const <String>[],
    );
    return granted.toSet();
  }

  // ── Activity ──────────────────────────────────────────────────────────────

  @override
  Future<int> readSteps(LocalDate date) async {
    final agg = await _aggregate(
      const ['Steps.count'],
      _dayStart(date),
      _dayEnd(date),
    );
    return (agg['Steps.count'] ?? 0).round();
  }

  @override
  Future<double> readDistanceMeters(LocalDate date) async {
    final agg = await _aggregate(
      const ['Distance.distance'],
      _dayStart(date),
      _dayEnd(date),
    );
    return agg['Distance.distance'] ?? 0.0;
  }

  @override
  Future<int> readFloorsClimbed(LocalDate date) async {
    final agg = await _aggregate(
      const ['FloorsClimbed.floors'],
      _dayStart(date),
      _dayEnd(date),
    );
    return (agg['FloorsClimbed.floors'] ?? 0).round();
  }

  @override
  Future<List<DailySteps>> readDailySteps(
    LocalDate startDate,
    LocalDate endDate, {
    bool includeActiveCalories = false,
    bool includeFloors = false,
    bool includeWheelchairPushes = false,
    bool includeElevation = false,
  }) async {
    final metrics = <String>[
      'Steps.count',
      'Distance.distance',
      if (includeActiveCalories) 'ActiveCaloriesBurned.energy',
      if (includeFloors) 'FloorsClimbed.floors',
      if (includeWheelchairPushes) 'WheelchairPushes.count',
      if (includeElevation) 'ElevationGained.elevation',
    ];
    // Kotlin `readDailyStepsChunk` slices with `aggregateGroupByDuration` over
    // an instant range, not `aggregateGroupByPeriod` over a local one. The
    // period variant resolves its window against each record's stored zone
    // offset, so records written under a different offset drop out of the day
    // and the total comes in under the plain `aggregate` the dashboard uses.
    const dayMinutes = 24 * 60;
    final byDate = <LocalDate, Map<String, double?>>{};
    // Health Connect's aggregateGroupByDuration rejects windows that slice into
    // too many buckets, so — like Kotlin `dailyStepDateChunks` — split the range
    // into <=366-day chunks. Without this the achievements scan (which runs from
    // the legacy 2009 start) issues one multi-thousand-bucket query that fails
    // and returns nothing, so no badges ever count.
    for (final chunk in _dailyStepDateChunks(startDate, endDate)) {
      final buckets = await _catch(
        () => _api.aggregateGroupByDurationJson(
          metrics,
          _dayStart(chunk.$1).millisecondsSinceEpoch,
          _dayEnd(chunk.$2).millisecondsSinceEpoch,
          dayMinutes,
        ),
        const <String>[],
      );
      for (final bucket in buckets) {
        final map = jsonDecode(bucket) as Map<String, dynamic>;
        final startMs = (map['startEpochMs'] as num).toInt();
        final date = LocalDate.fromDateTime(
          DateTime.fromMillisecondsSinceEpoch(startMs),
        );
        final values = (map['values'] as Map).cast<String, dynamic>();
        byDate[date] = {
          for (final entry in values.entries)
            entry.key: (entry.value as num?)?.toDouble(),
        };
      }
    }
    final result = <DailySteps>[];
    var date = startDate;
    while (!date.isAfter(endDate)) {
      final values = byDate[date];
      result.add(
        DailySteps(
          date: date,
          steps: (values?['Steps.count'] ?? 0).round(),
          distanceMeters: values?['Distance.distance'] ?? 0.0,
          activeCaloriesKcal: includeActiveCalories
              ? (values?['ActiveCaloriesBurned.energy'])
              : null,
          floorsClimbed: includeFloors
              ? (values?['FloorsClimbed.floors'] ?? 0).round()
              : null,
          wheelchairPushes: includeWheelchairPushes
              ? (values?['WheelchairPushes.count'] ?? 0).round()
              : null,
          elevationGainedMeters: includeElevation
              ? (values?['ElevationGained.elevation'] ?? 0.0)
              : null,
        ),
      );
      date = date.plusDays(1);
    }
    return result;
  }

  /// Port of Kotlin `dailyStepDateChunks` — split `[start, end]` into inclusive
  /// windows of at most 366 days so no single aggregate query exceeds Health
  /// Connect's bucket limit.
  static const int _dailyStepsMaxQueryDays = 366;

  List<(LocalDate, LocalDate)> _dailyStepDateChunks(
    LocalDate start,
    LocalDate end,
  ) {
    if (end.isBefore(start)) return const [];
    final chunks = <(LocalDate, LocalDate)>[];
    var chunkStart = start;
    while (!chunkStart.isAfter(end)) {
      final tentativeEnd = chunkStart.plusDays(_dailyStepsMaxQueryDays - 1);
      final chunkEnd = tentativeEnd.isAfter(end) ? end : tentativeEnd;
      chunks.add((chunkStart, chunkEnd));
      chunkStart = chunkEnd.plusDays(1);
    }
    return chunks;
  }

  @override
  Future<double?> readElevationGained(LocalDate date) async {
    final agg = await _aggregate(
      const ['ElevationGained.elevation'],
      _dayStart(date),
      _dayEnd(date),
    );
    // Null, not zero: "this device records no elevation" and "you climbed
    // nothing today" are different, and the metric screens key on which.
    return agg['ElevationGained.elevation'];
  }

  @override
  Future<int?> readWheelchairPushes(LocalDate date) async {
    final agg = await _aggregate(
      const ['WheelchairPushes.count'],
      _dayStart(date),
      _dayEnd(date),
    );
    return agg['WheelchairPushes.count']?.round();
  }

  /// Kotlin `ActivityHealthReader.readRawActivityProgress`: the day's metrics
  /// aggregated into hourly buckets and accumulated, so each point is the
  /// running total at that hour — what the intraday chart plots.
  ///
  /// Health Connect omits empty buckets, so the returned points are sparse; the
  /// chart draws straight lines between them, which is what a cumulative series
  /// means anyway.
  @override
  Future<List<ActivityProgressPoint>> readRawActivityProgress(
    LocalDate date,
  ) async {
    const metrics = <String>[
      'Steps.count',
      'Distance.distance',
      'TotalCaloriesBurned.energy',
      'ActiveCaloriesBurned.energy',
      'WheelchairPushes.count',
      'FloorsClimbed.floors',
      'ElevationGained.elevation',
    ];
    // Today's chart stops at "now" rather than running on to midnight.
    final isToday = date == LocalDate.now();
    final end = isToday ? DateTime.now() : _dayEnd(date);
    final buckets = await _catch(
      () => _api.aggregateGroupByDurationJson(
        metrics,
        _dayStart(date).millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
        60,
      ),
      const <String>[],
    );

    var steps = 0.0;
    var distance = 0.0;
    var calories = 0.0;
    var activeCalories = 0.0;
    var wheelchairPushes = 0.0;
    var floors = 0.0;
    var elevation = 0.0;

    // A metric the device never reports stays null on every point, so the
    // display mapper drops it rather than charting a flat zero line.
    var hasDistance = false;
    var hasCalories = false;
    var hasActiveCalories = false;
    var hasWheelchairPushes = false;
    var hasFloors = false;
    var hasElevation = false;

    final points = <ActivityProgressPoint>[];
    for (final bucket in buckets) {
      final map = jsonDecode(bucket) as Map<String, dynamic>;
      final endMs = (map['endEpochMs'] as num).toInt();
      final values = (map['values'] as Map).cast<String, dynamic>();
      double? valueOf(String key) => (values[key] as num?)?.toDouble();

      steps += valueOf('Steps.count') ?? 0.0;
      if (valueOf('Distance.distance') case final value?) {
        distance += value;
        hasDistance = true;
      }
      if (valueOf('TotalCaloriesBurned.energy') case final value?) {
        calories += value;
        hasCalories = true;
      }
      if (valueOf('ActiveCaloriesBurned.energy') case final value?) {
        activeCalories += value;
        hasActiveCalories = true;
      }
      if (valueOf('WheelchairPushes.count') case final value?) {
        wheelchairPushes += value;
        hasWheelchairPushes = true;
      }
      if (valueOf('FloorsClimbed.floors') case final value?) {
        floors += value;
        hasFloors = true;
      }
      if (valueOf('ElevationGained.elevation') case final value?) {
        elevation += value;
        hasElevation = true;
      }

      points.add(
        ActivityProgressPoint(
          time: _fromMs(endMs),
          totalSteps: steps.round(),
          totalDistanceMeters: hasDistance ? distance : null,
          totalCaloriesBurnedKcal: hasCalories ? calories : null,
          totalActiveCaloriesKcal: hasActiveCalories ? activeCalories : null,
          totalWheelchairPushes:
              hasWheelchairPushes ? wheelchairPushes.round() : null,
          totalFloorsClimbed: hasFloors ? floors.round() : null,
          totalElevationGainedMeters: hasElevation ? elevation : null,
        ),
      );
    }
    return points;
  }

  // ── Activity / Exercise (Phase 8) — typed via native ActivityHealthReader ───

  ExerciseRouteStatus _routeStatus(ExerciseRouteStatusMsg s) => switch (s) {
        ExerciseRouteStatusMsg.data => ExerciseRouteStatus.data,
        ExerciseRouteStatusMsg.consentRequired =>
          ExerciseRouteStatus.consentRequired,
        ExerciseRouteStatusMsg.noData => ExerciseRouteStatus.noData,
      };

  ExerciseData _exerciseData(ExerciseDataMsg m) => ExerciseData(
        id: m.id,
        title: m.title,
        exerciseType: m.exerciseType,
        startTime: _fromMs(m.startEpochMs),
        endTime: _fromMs(m.endEpochMs),
        durationMs: m.endEpochMs - m.startEpochMs,
        source: m.source,
        // Null unless the session came from `readExerciseSessionsWithMetrics`
        // with the matching read permission granted.
        totalDistanceMeters: m.totalDistanceMeters,
        averageSpeedMetersPerSecond: m.averageSpeedMetersPerSecond,
        notes: m.notes,
        clientRecordId: m.clientRecordId,
        plannedExerciseSessionId: m.plannedExerciseSessionId,
        device: m.device == null
            ? null
            : ExerciseDeviceData(
                type: m.device!.type,
                manufacturer: m.device!.manufacturer,
                model: m.device!.model,
              ),
        segments: [
          for (final s in m.segments)
            ExerciseSegmentData(
              startTime: _fromMs(s.startEpochMs),
              endTime: _fromMs(s.endEpochMs),
              segmentType: s.segmentType,
              repetitions: s.repetitions,
              setIndex: s.setIndex,
            ),
        ],
        laps: [
          for (final l in m.laps)
            ExerciseLapData(
              startTime: _fromMs(l.startEpochMs),
              endTime: _fromMs(l.endEpochMs),
              lengthMeters: l.lengthMeters,
            ),
        ],
        route: ExerciseRouteData(
          status: _routeStatus(m.route.status),
          points: [
            for (final p in m.route.points)
              ExerciseRoutePoint(
                time: _fromMs(p.timeEpochMs),
                latitude: p.latitude,
                longitude: p.longitude,
                altitudeMeters: p.altitudeMeters,
                horizontalAccuracyMeters: p.horizontalAccuracyMeters,
                verticalAccuracyMeters: p.verticalAccuracyMeters,
              ),
          ],
        ),
        isOpenVitalsEntry: m.isOpenVitalsEntry,
      );

  ActivityWriteRequestMsg _activityWriteMsg(ActivityWriteRequest request) =>
      ActivityWriteRequestMsg(
        exerciseType: request.exerciseType,
        startEpochMs: request.startTime.millisecondsSinceEpoch,
        endEpochMs: request.endTime.millisecondsSinceEpoch,
        title: request.title,
        notes: request.notes,
        plannedExerciseSessionId: request.plannedExerciseSessionId,
        segments: [
          for (final s in request.exerciseSegments)
            ExerciseSegmentMsg(
              startEpochMs: s.startTime.millisecondsSinceEpoch,
              endEpochMs: s.endTime.millisecondsSinceEpoch,
              segmentType: s.segmentType,
              repetitions: s.repetitions,
              setIndex: s.setIndex,
            ),
        ],
        laps: [
          for (final l in request.laps)
            ExerciseLapMsg(
              startEpochMs: l.startTime.millisecondsSinceEpoch,
              endEpochMs: l.endTime.millisecondsSinceEpoch,
              lengthMeters: l.lengthMeters,
            ),
        ],
        routePoints: [
          for (final p in request.routePoints)
            ExerciseRoutePointMsg(
              timeEpochMs: p.time.millisecondsSinceEpoch,
              latitude: p.latitude,
              longitude: p.longitude,
              altitudeMeters: p.altitudeMeters,
              horizontalAccuracyMeters: p.horizontalAccuracyMeters,
              verticalAccuracyMeters: p.verticalAccuracyMeters,
            ),
        ],
        pauseIntervals: [
          for (final pause in request.pauseIntervals)
            ActivityPauseIntervalMsg(
              startEpochMs: pause.startTime.millisecondsSinceEpoch,
              endEpochMs: pause.endTime.millisecondsSinceEpoch,
            ),
        ],
        stepsCount: request.stepsCount,
        distanceMeters: request.distanceMeters,
        elevationGainedMeters: request.elevationGainedMeters,
        activeCaloriesKcal: request.activeCaloriesKcal,
        totalCaloriesKcal: request.totalCaloriesKcal,
        bleSamples: request.bleSamples.isEmpty()
            ? null
            : ActivityBleSamplesMsg(
                heartRateSamples: [
                  for (final s in request.bleSamples.heartRateSamples)
                    BleHeartRateSampleMsg(
                      timeEpochMs: s.time.millisecondsSinceEpoch,
                      beatsPerMinute: s.beatsPerMinute,
                    ),
                ],
                powerSamples: [
                  for (final s in request.bleSamples.powerSamples)
                    BlePowerSampleMsg(
                      timeEpochMs: s.time.millisecondsSinceEpoch,
                      watts: s.watts,
                    ),
                ],
                cyclingCadenceSamples: [
                  for (final s in request.bleSamples.cyclingCadenceSamples)
                    BleCyclingCadenceSampleMsg(
                      timeEpochMs: s.time.millisecondsSinceEpoch,
                      rpm: s.rpm,
                    ),
                ],
                speedSamples: [
                  for (final s in request.bleSamples.speedSamples)
                    BleSpeedSampleMsg(
                      timeEpochMs: s.time.millisecondsSinceEpoch,
                      metersPerSecond: s.metersPerSecond,
                      isRunning: s.isRunning,
                    ),
                ],
                stepsCadenceSamples: [
                  for (final s in request.bleSamples.stepsCadenceSamples)
                    BleStepsCadenceSampleMsg(
                      timeEpochMs: s.time.millisecondsSinceEpoch,
                      stepsPerMinute: s.stepsPerMinute,
                    ),
                ],
              ),
      );

  @override
  Future<List<ExerciseData>> readExerciseSessions(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readExerciseSessions(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <ExerciseDataMsg>[],
    );
    return deduplicateExerciseSessions([for (final m in msgs) _exerciseData(m)]);
  }

  @override
  Future<List<ExerciseData>> readExerciseSessionsWithMetrics(
    DateTime start,
    DateTime end, {
    bool includeDistance = false,
    bool includeSpeed = false,
  }) async {
    final msgs = await _catch(
      () => _api.readExerciseSessionsWithMetrics(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
        includeDistance,
        includeSpeed,
      ),
      const <ExerciseDataMsg>[],
    );
    // Route backfill mirrors Kotlin's `toExerciseData(backfillRouteMetrics = true)`
    // on this path: a session whose provider recorded a route but no
    // DistanceRecord still gets a distance (and hence a pace) from the route
    // geometry. Backfill first, dedup after — as Kotlin does.
    return deduplicateExerciseSessions([
      for (final m in msgs) _exerciseData(m).withRouteBackfilledMetrics(),
    ]);
  }

  @override
  Future<ExerciseData?> readExerciseSession(String id) async {
    final m = await _catch(() => _api.readExerciseSessionById(id), null);
    // The list read backfills route-derived distance/elevation; a session opened
    // by id must not silently miss them just because it took the other door.
    return m == null ? null : _exerciseData(m).withRouteBackfilledMetrics();
  }

  @override
  Future<ExerciseSessionMetrics> readExerciseSessionMetrics(
    DateTime start,
    DateTime end,
    Set<ExerciseSessionMetric> metrics,
  ) async {
    if (metrics.isEmpty) return ExerciseSessionMetrics.none;
    final m = await _catch(
      () => _api.readExerciseSessionMetrics(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
        [for (final metric in metrics) metric.wireName],
      ),
      null,
    );
    if (m == null) return ExerciseSessionMetrics.none;
    return ExerciseSessionMetrics(
      totalDistanceMeters: m.totalDistanceMeters,
      averageSpeedMetersPerSecond: m.averageSpeedMetersPerSecond,
      steps: m.steps,
      totalCaloriesKcal: m.totalCaloriesKcal,
      activeCaloriesKcal: m.activeCaloriesKcal,
      elevationGainedMeters: m.elevationGainedMeters,
      floorsClimbed: m.floorsClimbed,
      wheelchairPushes: m.wheelchairPushes,
    );
  }

  @override
  Future<CaloriesBurnedValue?> readCaloriesBurned(
    LocalDate date, {
    bool includeEstimatedCalories = false,
  }) async {
    final agg = await _aggregate(
      const ['TotalCaloriesBurned.energy'],
      _dayStart(date),
      _dayEnd(date),
    );
    final kcal = agg['TotalCaloriesBurned.energy'];
    if (kcal == null || kcal <= 0) return null;
    return CaloriesBurnedValue(
      kcal: kcal,
      source: CaloriesBurnedSource.recordedTotal,
    );
  }

  @override
  Future<List<SpeedSample>> readSpeedSamples(DateTime start, DateTime end) async {
    final msgs = await _catch(
      () => _api.readSpeedSamples(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <SpeedSampleMsg>[],
    );
    return [
      for (final m in msgs)
        SpeedSample(
          time: _fromMs(m.timeEpochMs),
          metersPerSecond: m.metersPerSecond,
          source: m.source,
        ),
    ]..sort((a, b) => a.time.compareTo(b.time));
  }

  @override
  Future<List<ActivityCadenceSample>> readActivityCadenceSamples(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readActivityCadenceSamples(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <ActivityCadenceSampleMsg>[],
    );
    return [
      for (final m in msgs)
        ActivityCadenceSample(
          time: _fromMs(m.timeEpochMs),
          rate: m.rate,
          // Cycling is revolutions per minute; steps cadence is steps per
          // minute. Same number, different meaning.
          kind: m.isCycling
              ? ActivityCadenceKind.cycling
              : ActivityCadenceKind.steps,
          source: m.source,
        ),
    ];
  }

  @override
  Future<List<PlannedExerciseData>> readPlannedExerciseSessions(
    DateTime start,
    DateTime end,
  ) async {
    if (!isPlannedExerciseAvailable()) return const <PlannedExerciseData>[];
    final msgs = await _catch(
      () => _api.readPlannedExerciseSessions(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <PlannedExerciseSessionMsg>[],
    );
    return [
      for (final m in msgs)
        PlannedExerciseData(
          id: m.id,
          title: m.title,
          exerciseType: m.exerciseType,
          startTime: _fromMs(m.startEpochMs),
          endTime: _fromMs(m.endEpochMs),
          hasExplicitTime: m.hasExplicitTime,
          completedExerciseSessionId: m.completedExerciseSessionId,
          notes: m.notes,
          blockCount: m.blocks.length,
          source: m.source,
          blocks: [for (final block in m.blocks) _plannedBlock(block)],
        ),
    ];
  }

  @override
  Future<String> writePlannedExerciseSession(
    PlannedExerciseWriteRequest request,
  ) async {
    if (!isPlannedExerciseAvailable()) {
      throw UnsupportedError(
        'Planned exercise sessions are unavailable on this Health Connect '
        'provider.',
      );
    }
    return _api.writePlannedExerciseSession(
      PlannedExerciseWriteRequestMsg(
        id: request.id,
        exerciseType: request.exerciseType,
        startEpochMs: request.startTime.millisecondsSinceEpoch,
        endEpochMs: request.endTime.millisecondsSinceEpoch,
        title: request.title,
        notes: request.notes,
        blocks: [for (final block in request.blocks) _plannedBlockMsg(block)],
      ),
    );
  }

  PlannedExerciseBlockData _plannedBlock(PlannedExerciseBlockMsg block) =>
      PlannedExerciseBlockData(
        repetitions: block.repetitions,
        description: block.description,
        steps: [for (final step in block.steps) _plannedStep(step)],
      );

  PlannedExerciseStepData _plannedStep(PlannedExerciseStepMsg step) =>
      PlannedExerciseStepData(
        exerciseType: step.exerciseType,
        exercisePhase: step.exercisePhase,
        description: step.description,
        completion: switch (step.completionKind) {
          PlannedExerciseCompletionKindMsg.repetitions =>
            PlannedExerciseCompletionRepetitions(step.completionRepetitions ?? 0),
          PlannedExerciseCompletionKindMsg.durationSeconds =>
            PlannedExerciseCompletionDurationSeconds(step.completionSeconds ?? 0),
          PlannedExerciseCompletionKindMsg.manual =>
            const PlannedExerciseCompletionManual(),
          PlannedExerciseCompletionKindMsg.unknown =>
            const PlannedExerciseCompletionUnknown(),
        },
      );

  PlannedExerciseBlockMsg _plannedBlockMsg(PlannedExerciseBlockData block) =>
      PlannedExerciseBlockMsg(
        repetitions: block.repetitions,
        description: block.description,
        steps: [for (final step in block.steps) _plannedStepMsg(step)],
      );

  PlannedExerciseStepMsg _plannedStepMsg(PlannedExerciseStepData step) {
    final completion = step.completion;
    return PlannedExerciseStepMsg(
      exerciseType: step.exerciseType,
      exercisePhase: step.exercisePhase,
      description: step.description,
      completionKind: switch (completion) {
        PlannedExerciseCompletionRepetitions() =>
          PlannedExerciseCompletionKindMsg.repetitions,
        PlannedExerciseCompletionDurationSeconds() =>
          PlannedExerciseCompletionKindMsg.durationSeconds,
        PlannedExerciseCompletionManual() =>
          PlannedExerciseCompletionKindMsg.manual,
        PlannedExerciseCompletionUnknown() =>
          PlannedExerciseCompletionKindMsg.unknown,
      },
      completionRepetitions: completion is PlannedExerciseCompletionRepetitions
          ? completion.repetitions
          : null,
      completionSeconds: completion is PlannedExerciseCompletionDurationSeconds
          ? completion.seconds
          : null,
    );
  }

  // ── Nutrition / hydration ─────────────────────────────────────────────────

  // ── Nutrition (Phase 6) — typed via native NutritionHealthReader ────────────

  /// Nutrient maps cross the bridge keyed by [NutritionNutrient.storageName];
  /// unknown keys are dropped.
  Map<NutritionNutrient, double> _nutrientMap(Map<String, double> raw) {
    final out = <NutritionNutrient, double>{};
    for (final e in raw.entries) {
      final n = NutritionNutrient.fromStorage(e.key);
      if (n != null) out[n] = e.value;
    }
    return out;
  }

  Map<String, double> _nutrientMsg(Map<NutritionNutrient, double> values) => {
        for (final e in values.entries) e.key.storageName: e.value,
      };

  CaloriesBurnedSource _caloriesSource(CaloriesBurnedSourceMsg m) =>
      switch (m) {
        CaloriesBurnedSourceMsg.noData => CaloriesBurnedSource.noData,
        CaloriesBurnedSourceMsg.recordedTotal =>
          CaloriesBurnedSource.recordedTotal,
        CaloriesBurnedSourceMsg.estimatedActiveAndBmr =>
          CaloriesBurnedSource.estimatedActiveAndBmr,
      };

  @override
  Future<double?> readCaloriesInKcal(LocalDate date) => _catch(
        () => _api.readCaloriesInKcal(
          _dayStart(date).millisecondsSinceEpoch,
          _dayEnd(date).millisecondsSinceEpoch,
        ),
        null,
      );

  @override
  Future<List<NutritionEntry>> readNutritionEntries(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readNutritionEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <NutritionEntryMsg>[],
    );
    return [
      for (final m in msgs)
        () {
          final nutrients = _nutrientMap(m.nutrientValues);
          return NutritionEntry(
            time: _fromMs(m.startEpochMs),
            endTime: _fromMs(m.endEpochMs),
            mealType: m.mealType,
            name: m.name,
            energyKcal: nutrients[NutritionNutrient.energy],
            proteinGrams: nutrients[NutritionNutrient.protein],
            carbsGrams: nutrients[NutritionNutrient.totalCarbohydrate],
            fatGrams: nutrients[NutritionNutrient.totalFat],
            fiberGrams: nutrients[NutritionNutrient.dietaryFiber],
            sugarGrams: nutrients[NutritionNutrient.sugar],
            source: m.source,
            nutrientValues: nutrients,
            id: m.id,
            clientRecordId: m.clientRecordId,
            isOpenVitalsEntry: m.isOpenVitalsEntry,
          );
        }(),
    ];
  }

  @override
  Future<List<DailyMacros>> readDailyMacros(
    LocalDate startDate,
    LocalDate endDate,
  ) async {
    final msgs = await _catch(
      () => _api.readDailyMacros(
        _dayStart(startDate).millisecondsSinceEpoch,
        _dayEnd(endDate).millisecondsSinceEpoch,
      ),
      const <DailyMacrosMsg>[],
    );
    return [
      for (final m in msgs)
        DailyMacros(
          date: LocalDate.fromDateTime(_fromMs(m.dateEpochMs)),
          nutrientValues: _nutrientMap(m.nutrientValues),
        ),
    ];
  }

  @override
  Future<List<DailyNutrition>> readDailyNutrition(
    LocalDate startDate,
    LocalDate endDate, {
    bool includeHydration = true,
    bool includeEstimatedCalories = false,
  }) async {
    final msgs = await _catch(
      () => _api.readDailyNutrition(
        _dayStart(startDate).millisecondsSinceEpoch,
        _dayEnd(endDate).millisecondsSinceEpoch,
        includeHydration,
        true, // includeCalories (always on, matching the reference default)
        includeEstimatedCalories,
      ),
      const <DailyNutritionMsg>[],
    );
    return [
      for (final m in msgs)
        DailyNutrition(
          date: LocalDate.fromDateTime(_fromMs(m.dateEpochMs)),
          hydrationLiters: m.hydrationLiters,
          caloriesBurnedKcal: m.caloriesBurnedKcal,
          caloriesBurnedSource: _caloriesSource(m.caloriesBurnedSource),
        ),
    ];
  }

  // ── Hydration (Phase 2) — typed via native HydrationHealthReader ────────────

  HydrationEntry _hydrationEntry(HydrationEntryMsg m) => HydrationEntry(
        startTime: _fromMs(m.startEpochMs),
        endTime: _fromMs(m.endEpochMs),
        liters: m.liters,
        source: m.source,
        id: m.id,
        clientRecordId: m.clientRecordId,
        isOpenVitalsEntry: m.isOpenVitalsEntry,
      );

  @override
  Future<double?> readHydrationLiters(LocalDate date) => _catch(
        () => _api.readHydrationLiters(
          _dayStart(date).millisecondsSinceEpoch,
          _dayEnd(date).millisecondsSinceEpoch,
        ),
        null,
      );

  @override
  Future<List<DailyHydration>> readDailyHydration(
    LocalDate startDate,
    LocalDate endDate,
  ) async {
    final msgs = await _catch(
      () => _api.readDailyHydration(
        _dayStart(startDate).millisecondsSinceEpoch,
        _dayEnd(endDate).millisecondsSinceEpoch,
      ),
      const <DailyHydrationMsg>[],
    );
    // Native returns raw per-day aggregate buckets; fill the full range here so
    // days without hydration data still appear as 0 L (matches the reference).
    final byDay = <int, double>{
      for (final m in msgs)
        LocalDate.fromDateTime(_fromMs(m.dateEpochMs)).epochDay: m.liters,
    };
    final out = <DailyHydration>[];
    for (var date = startDate;
        date.compareTo(endDate) <= 0;
        date = date.plusDays(1)) {
      out.add(DailyHydration(date: date, liters: byDay[date.epochDay] ?? 0.0));
    }
    return out;
  }

  @override
  Future<List<HydrationEntry>> readHydrationEntries(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readHydrationEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <HydrationEntryMsg>[],
    );
    return [for (final m in msgs) _hydrationEntry(m)];
  }

  @override
  Future<HydrationEntry?> readHydrationEntry(String id) async {
    final m = await _catch(() => _api.readHydrationEntry(id), null);
    return m == null ? null : _hydrationEntry(m);
  }

  // ── Body (Phase 1) — typed via native BodyHealthReader ──────────────────────

  BodyMeasurementTypeMsg _bodyTypeMsg(BodyMeasurementType type) => switch (type) {
        BodyMeasurementType.weight => BodyMeasurementTypeMsg.weight,
        BodyMeasurementType.height => BodyMeasurementTypeMsg.height,
        BodyMeasurementType.bodyFat => BodyMeasurementTypeMsg.bodyFat,
      };

  BodyMeasurementType _bodyType(BodyMeasurementTypeMsg type) => switch (type) {
        BodyMeasurementTypeMsg.weight => BodyMeasurementType.weight,
        BodyMeasurementTypeMsg.height => BodyMeasurementType.height,
        BodyMeasurementTypeMsg.bodyFat => BodyMeasurementType.bodyFat,
      };

  WeightEntry _weightEntry(WeightEntryMsg m) => WeightEntry(
        time: _fromMs(m.timeEpochMs),
        weightKg: m.weightKg,
        source: m.source,
        id: m.id,
        isOpenVitalsEntry: m.isOpenVitalsEntry,
      );

  HeightEntry _heightEntry(HeightEntryMsg m) => HeightEntry(
        time: _fromMs(m.timeEpochMs),
        heightCm: m.heightCm,
        source: m.source,
        id: m.id,
        isOpenVitalsEntry: m.isOpenVitalsEntry,
      );

  BodyFatEntry _bodyFatEntry(BodyFatEntryMsg m) => BodyFatEntry(
        time: _fromMs(m.timeEpochMs),
        percent: m.percent,
        source: m.source,
        id: m.id,
        isOpenVitalsEntry: m.isOpenVitalsEntry,
      );

  @override
  Future<List<WeightEntry>> readWeightEntries(
    LocalDate start,
    LocalDate end,
  ) async {
    final msgs = await _catch(
      () => _api.readWeightEntries(
        _dayStart(start).millisecondsSinceEpoch,
        _dayEnd(end).millisecondsSinceEpoch,
      ),
      const <WeightEntryMsg>[],
    );
    return [for (final m in msgs) _weightEntry(m)];
  }

  @override
  Future<WeightEntry?> readLatestWeight() async {
    final m = await _catch(() => _api.readLatestWeight(), null);
    return m == null ? null : _weightEntry(m);
  }

  @override
  Future<List<HeightEntry>> readHeightEntries(
    LocalDate start,
    LocalDate end,
  ) async {
    final msgs = await _catch(
      () => _api.readHeightEntries(
        _dayStart(start).millisecondsSinceEpoch,
        _dayEnd(end).millisecondsSinceEpoch,
      ),
      const <HeightEntryMsg>[],
    );
    return [for (final m in msgs) _heightEntry(m)];
  }

  @override
  Future<HeightEntry?> readLatestHeightEntry() async {
    final m = await _catch(() => _api.readLatestHeightEntry(), null);
    return m == null ? null : _heightEntry(m);
  }

  @override
  Future<double?> readLatestHeight() async =>
      (await readLatestHeightEntry())?.heightCm;

  @override
  Future<List<BodyFatEntry>> readBodyFatEntries(
    LocalDate start,
    LocalDate end,
  ) async {
    final msgs = await _catch(
      () => _api.readBodyFatEntries(
        _dayStart(start).millisecondsSinceEpoch,
        _dayEnd(end).millisecondsSinceEpoch,
      ),
      const <BodyFatEntryMsg>[],
    );
    return [for (final m in msgs) _bodyFatEntry(m)];
  }

  @override
  Future<double?> readLatestBodyFat() async {
    final m = await _catch(() => _api.readLatestBodyFat(), null);
    return m?.percent;
  }

  @override
  Future<List<LeanBodyMassEntry>> readLeanBodyMassEntries(
    LocalDate start,
    LocalDate end,
  ) async {
    final msgs = await _catch(
      () => _api.readLeanBodyMassEntries(
        _dayStart(start).millisecondsSinceEpoch,
        _dayEnd(end).millisecondsSinceEpoch,
      ),
      const <BodyMassEntryMsg>[],
    );
    return [
      for (final m in msgs)
        LeanBodyMassEntry(
            time: _fromMs(m.timeEpochMs), massKg: m.massKg, source: m.source),
    ];
  }

  @override
  Future<double?> readLatestLeanBodyMass() async {
    final m = await _catch(() => _api.readLatestLeanBodyMass(), null);
    return m?.massKg;
  }

  @override
  Future<List<BmrEntry>> readBmrEntries(LocalDate start, LocalDate end) async {
    final msgs = await _catch(
      () => _api.readBmrEntries(
        _dayStart(start).millisecondsSinceEpoch,
        _dayEnd(end).millisecondsSinceEpoch,
      ),
      const <BmrEntryMsg>[],
    );
    return [
      for (final m in msgs)
        BmrEntry(
            time: _fromMs(m.timeEpochMs),
            kcalPerDay: m.kcalPerDay,
            source: m.source),
    ];
  }

  @override
  Future<double?> readLatestBMR() async {
    final m = await _catch(() => _api.readLatestBmr(), null);
    return m?.kcalPerDay;
  }

  @override
  Future<List<BoneMassEntry>> readBoneMassEntries(
    LocalDate start,
    LocalDate end,
  ) async {
    final msgs = await _catch(
      () => _api.readBoneMassEntries(
        _dayStart(start).millisecondsSinceEpoch,
        _dayEnd(end).millisecondsSinceEpoch,
      ),
      const <BodyMassEntryMsg>[],
    );
    return [
      for (final m in msgs)
        BoneMassEntry(
            time: _fromMs(m.timeEpochMs), massKg: m.massKg, source: m.source),
    ];
  }

  @override
  Future<double?> readLatestBoneMass() async {
    final m = await _catch(() => _api.readLatestBoneMass(), null);
    return m?.massKg;
  }

  @override
  Future<List<BodyWaterMassEntry>> readBodyWaterMassEntries(
    LocalDate start,
    LocalDate end,
  ) async {
    final msgs = await _catch(
      () => _api.readBodyWaterMassEntries(
        _dayStart(start).millisecondsSinceEpoch,
        _dayEnd(end).millisecondsSinceEpoch,
      ),
      const <BodyMassEntryMsg>[],
    );
    return [
      for (final m in msgs)
        BodyWaterMassEntry(
            time: _fromMs(m.timeEpochMs), massKg: m.massKg, source: m.source),
    ];
  }

  @override
  Future<double?> readLatestBodyWaterMass() async {
    final m = await _catch(() => _api.readLatestBodyWaterMass(), null);
    return m?.massKg;
  }

  @override
  Future<BodyMeasurementEntry?> readBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  ) async {
    final m = await _catch(
      () => _api.readBodyMeasurementEntry(_bodyTypeMsg(type), id),
      null,
    );
    return m == null
        ? null
        : BodyMeasurementEntry(
            id: m.id,
            type: _bodyType(m.type),
            time: _fromMs(m.timeEpochMs),
            value: m.value,
            source: m.source,
            isOpenVitalsEntry: m.isOpenVitalsEntry,
          );
  }

  // ── Heart ─────────────────────────────────────────────────────────────────

  // ── Heart (Phase 5) — typed via native HeartHealthReader ────────────────────

  HeartRateSample _heartRateSample(HeartRateSampleMsg m) => HeartRateSample(
        time: _fromMs(m.timeEpochMs),
        beatsPerMinute: m.beatsPerMinute,
        source: m.source,
      );

  @override
  Future<List<HeartRateSample>> readHeartRateSamples(
    DateTime start,
    DateTime end,
  ) async {
    // Adaptive: high-frequency days would blow past page limits, so aggregate
    // them into chart buckets; short (workout) ranges keep raw samples.
    if (shouldUseAggregatedHeartRateSamples(end.difference(start))) {
      final buckets = await _catch(
        () => _api.readHeartRateAggregatedBuckets(
          start.millisecondsSinceEpoch,
          end.millisecondsSinceEpoch,
          heartRateChartBucketDuration.inMilliseconds,
        ),
        const <HeartRateAggBucketMsg>[],
      );
      return [
        for (final b in buckets)
          heartRateSampleFromAggregateBucket(
            startTime: _fromMs(b.startEpochMs),
            avgBpm: b.avgBpm,
          ),
      ];
    }
    return readRawHeartRateSamples(start, end);
  }

  @override
  Future<List<HeartRateSample>> readRawHeartRateSamples(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readRawHeartRateSamples(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <HeartRateSampleMsg>[],
    );
    return [for (final m in msgs) _heartRateSample(m)];
  }

  @override
  Future<int?> readAvgHeartRate(LocalDate date) => _catch(
        () => _api.readAvgHeartRate(
          _dayStart(date).millisecondsSinceEpoch,
          _dayEnd(date).millisecondsSinceEpoch,
        ),
        null,
      );

  @override
  Future<List<HeartRateSummary>> readDailyHeartRateSummaries(
    LocalDate start,
    LocalDate end,
  ) async {
    final msgs = await _catch(
      () => _api.readDailyHeartRateSummaries(
        _dayStart(start).millisecondsSinceEpoch,
        _dayEnd(end).millisecondsSinceEpoch,
      ),
      const <HeartRateSummaryMsg>[],
    );
    return [
      for (final m in msgs)
        HeartRateSummary(
          date: LocalDate.fromDateTime(_fromMs(m.dateEpochMs)),
          avgBpm: m.avgBpm,
          minBpm: m.minBpm,
          maxBpm: m.maxBpm,
        ),
    ];
  }

  @override
  Future<List<RestingHeartRateSample>> readRestingHeartRateSamples(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readRestingHeartRateSamples(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <RestingHeartRateSampleMsg>[],
    );
    return [
      for (final m in msgs)
        RestingHeartRateSample(
          time: _fromMs(m.timeEpochMs),
          beatsPerMinute: m.beatsPerMinute,
          source: m.source,
        ),
    ];
  }

  @override
  Future<int?> readRestingHeartRate(LocalDate date) => _catch(
        () => _api.readRestingHeartRate(
          _dayStart(date).millisecondsSinceEpoch,
          _dayEnd(date).millisecondsSinceEpoch,
        ),
        null,
      );

  @override
  Future<List<DailyRestingHR>> readDailyRestingHR(
    LocalDate start,
    LocalDate end,
  ) async {
    final msgs = await _catch(
      () => _api.readDailyRestingHR(
        _dayStart(start).millisecondsSinceEpoch,
        _dayEnd(end).millisecondsSinceEpoch,
      ),
      const <DailyRestingHRMsg>[],
    );
    return [
      for (final m in msgs)
        DailyRestingHR(
          date: LocalDate.fromDateTime(_fromMs(m.dateEpochMs)),
          bpm: m.bpm,
        ),
    ];
  }

  @override
  Future<List<HrvSample>> readHrvSamples(DateTime start, DateTime end) async {
    final msgs = await _catch(
      () => _api.readHrvSamples(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <HrvSampleMsg>[],
    );
    return [
      for (final m in msgs)
        HrvSample(time: _fromMs(m.timeEpochMs), rmssdMs: m.rmssdMs, source: m.source),
    ];
  }

  @override
  Future<double?> readHrvRmssd(LocalDate date) async {
    final samples = await readHrvSamples(_dayStart(date), _dayEnd(date));
    if (samples.isEmpty) return null;
    final sum = samples.fold<double>(0, (a, s) => a + s.rmssdMs);
    return sum / samples.length;
  }

  @override
  Future<List<DailyHrv>> readDailyHRV(LocalDate start, LocalDate end) async {
    final msgs = await _catch(
      () => _api.readDailyHRV(
        _dayStart(start).millisecondsSinceEpoch,
        _dayEnd(end).millisecondsSinceEpoch,
      ),
      const <DailyHrvMsg>[],
    );
    return [
      for (final m in msgs)
        DailyHrv(
          date: LocalDate.fromDateTime(_fromMs(m.dateEpochMs)),
          rmssdMs: m.rmssdMs,
        ),
    ];
  }

  // ── Vitals (Phase 3) — typed via native VitalsHealthReader ──────────────────

  VitalsMeasurementTypeMsg _vitalsTypeMsg(VitalsMeasurementType type) =>
      switch (type) {
        VitalsMeasurementType.bloodPressure =>
          VitalsMeasurementTypeMsg.bloodPressure,
        VitalsMeasurementType.spo2 => VitalsMeasurementTypeMsg.spo2,
        VitalsMeasurementType.respiratoryRate =>
          VitalsMeasurementTypeMsg.respiratoryRate,
        VitalsMeasurementType.bodyTemperature =>
          VitalsMeasurementTypeMsg.bodyTemperature,
      };

  VitalsMeasurementType _vitalsType(VitalsMeasurementTypeMsg type) =>
      switch (type) {
        VitalsMeasurementTypeMsg.bloodPressure =>
          VitalsMeasurementType.bloodPressure,
        VitalsMeasurementTypeMsg.spo2 => VitalsMeasurementType.spo2,
        VitalsMeasurementTypeMsg.respiratoryRate =>
          VitalsMeasurementType.respiratoryRate,
        VitalsMeasurementTypeMsg.bodyTemperature =>
          VitalsMeasurementType.bodyTemperature,
      };

  BloodPressureEntry _bloodPressureEntry(BloodPressureEntryMsg m) =>
      BloodPressureEntry(
        time: _fromMs(m.timeEpochMs),
        systolicMmHg: m.systolicMmHg,
        diastolicMmHg: m.diastolicMmHg,
        source: m.source,
        id: m.id,
        isOpenVitalsEntry: m.isOpenVitalsEntry,
      );

  SpO2Entry _spO2Entry(SpO2EntryMsg m) => SpO2Entry(
        time: _fromMs(m.timeEpochMs),
        percent: m.percent,
        source: m.source,
        id: m.id,
        isOpenVitalsEntry: m.isOpenVitalsEntry,
      );

  Vo2MaxEntry _vo2MaxEntry(Vo2MaxEntryMsg m) => Vo2MaxEntry(
        time: _fromMs(m.timeEpochMs),
        vo2MaxMlPerKgPerMin: m.vo2MaxMlPerKgPerMin,
        source: m.source,
      );

  @override
  Future<List<BloodPressureEntry>> readBloodPressureEntries(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readBloodPressureEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <BloodPressureEntryMsg>[],
    );
    return [for (final m in msgs) _bloodPressureEntry(m)]
      ..sort((a, b) => a.time.compareTo(b.time));
  }

  @override
  Future<BloodPressureEntry?> readLatestBloodPressure(LocalDate date) async {
    final m = await _catch(
      () => _api.readLatestBloodPressure(
        _dayStart(date).millisecondsSinceEpoch,
        _dayEnd(date).millisecondsSinceEpoch,
      ),
      null,
    );
    return m == null ? null : _bloodPressureEntry(m);
  }

  @override
  Future<List<SpO2Entry>> readSpO2Entries(DateTime start, DateTime end) async {
    final msgs = await _catch(
      () => _api.readSpO2Entries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <SpO2EntryMsg>[],
    );
    return [for (final m in msgs) _spO2Entry(m)]
      ..sort((a, b) => a.time.compareTo(b.time));
  }

  @override
  Future<SpO2Entry?> readLatestSpO2(LocalDate date) async {
    final m = await _catch(
      () => _api.readLatestSpO2(
        _dayStart(date).millisecondsSinceEpoch,
        _dayEnd(date).millisecondsSinceEpoch,
      ),
      null,
    );
    return m == null ? null : _spO2Entry(m);
  }

  @override
  Future<List<RespiratoryRateEntry>> readRespiratoryRateEntries(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readRespiratoryRateEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <RespiratoryRateEntryMsg>[],
    );
    return [
      for (final m in msgs)
        RespiratoryRateEntry(
          time: _fromMs(m.timeEpochMs),
          breathsPerMinute: m.breathsPerMinute,
          source: m.source,
          id: m.id,
          isOpenVitalsEntry: m.isOpenVitalsEntry,
        ),
    ]..sort((a, b) => a.time.compareTo(b.time));
  }

  @override
  Future<List<BodyTempEntry>> readBodyTemperatureEntries(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readBodyTemperatureEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <BodyTempEntryMsg>[],
    );
    return [
      for (final m in msgs)
        BodyTempEntry(
          time: _fromMs(m.timeEpochMs),
          temperatureCelsius: m.temperatureCelsius,
          source: m.source,
          id: m.id,
          isOpenVitalsEntry: m.isOpenVitalsEntry,
        ),
    ]..sort((a, b) => a.time.compareTo(b.time));
  }

  @override
  Future<List<Vo2MaxEntry>> readVo2MaxEntries(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readVo2MaxEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <Vo2MaxEntryMsg>[],
    );
    return [for (final m in msgs) _vo2MaxEntry(m)]
      ..sort((a, b) => a.time.compareTo(b.time));
  }

  @override
  Future<Vo2MaxEntry?> readLatestVo2Max(LocalDate date) async {
    final m = await _catch(
      () => _api.readLatestVo2Max(
        _dayStart(date).millisecondsSinceEpoch,
        _dayEnd(date).millisecondsSinceEpoch,
      ),
      null,
    );
    return m == null ? null : _vo2MaxEntry(m);
  }

  @override
  Future<List<BloodGlucoseEntry>> readBloodGlucoseEntries(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readBloodGlucoseEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <BloodGlucoseEntryMsg>[],
    );
    return [
      for (final m in msgs)
        BloodGlucoseEntry(
          time: _fromMs(m.timeEpochMs),
          millimolesPerLiter: m.millimolesPerLiter,
          specimenSource: m.specimenSource,
          mealType: m.mealType,
          relationToMeal: m.relationToMeal,
          source: m.source,
        ),
    ]..sort((a, b) => a.time.compareTo(b.time));
  }

  @override
  Future<List<SkinTemperatureEntry>> readSkinTemperatureEntries(
    DateTime start,
    DateTime end,
  ) async {
    if (!isSkinTemperatureAvailable()) return const <SkinTemperatureEntry>[];
    final msgs = await _catch(
      () => _api.readSkinTemperatureEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <SkinTemperatureEntryMsg>[],
    );
    return [
      for (final m in msgs)
        SkinTemperatureEntry(
          startTime: _fromMs(m.startEpochMs),
          endTime: _fromMs(m.endEpochMs),
          baselineCelsius: m.baselineCelsius,
          averageDeltaCelsius: m.averageDeltaCelsius,
          minDeltaCelsius: m.minDeltaCelsius,
          maxDeltaCelsius: m.maxDeltaCelsius,
          measurementLocation: m.measurementLocation,
          source: m.source,
        ),
    ]..sort((a, b) => a.time.compareTo(b.time));
  }

  // ── Sleep (Phase 7) — typed via native SleepHealthReader; merge in Dart ─────

  SleepData _sleepData(SleepDataMsg m) {
    final stages = [
      for (final s in m.stages)
        SleepStage(
          startTime: _fromMs(s.startEpochMs),
          endTime: _fromMs(s.endEpochMs),
          stageType: s.stageType,
        ),
    ];
    final spanMs = m.endEpochMs - m.startEpochMs;
    return SleepData(
      id: m.id,
      startTime: _fromMs(m.startEpochMs),
      endTime: _fromMs(m.endEpochMs),
      durationMs: sleepDurationMsFromStages(stages, spanMs),
      source: m.source,
      title: m.title,
      notes: m.notes,
      clientRecordId: m.clientRecordId,
      device: m.device == null
          ? null
          : SleepDeviceData(
              type: m.device!.type,
              manufacturer: m.device!.manufacturer,
              model: m.device!.model,
            ),
      stages: stages,
    );
  }

  @override
  Future<List<SleepData>> readSleepSessions(DateTime start, DateTime end) async {
    final msgs = await _catch(
      () => _api.readSleepSessionsRaw(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <SleepDataMsg>[],
    );
    final sessions = [for (final m in msgs) _sleepData(m)]
      ..sort((a, b) => a.startTime.compareTo(b.startTime));
    return mergeSleepSessions(sessions);
  }

  @override
  Future<SleepData?> readSleepSession(String id) async {
    final m = await _catch(() => _api.readSleepSessionById(id), null);
    return m == null ? null : _sleepData(m);
  }

  @override
  Future<SleepReadData> readSleepData(
    LocalDate startDate,
    LocalDate endDate,
    SleepRangeMode sleepRangeMode,
  ) async {
    // Widen by a day on each side so sessions crossing midnight are captured.
    final sessions = await readSleepSessions(
      _dayStart(startDate.minusDays(1)),
      _dayEnd(endDate),
    );
    final durationByDate = <LocalDate, int>{};
    for (final session in sessions) {
      final date = LocalDate.fromDateTime(session.startTime.toLocal());
      durationByDate[date] = (durationByDate[date] ?? 0) + session.durationMs;
    }
    final dates = durationByDate.keys.toList()..sort();
    return SleepReadData(
      sessions: sessions,
      dailyAggregateDurations: [
        for (final date in dates)
          DailySleepDuration(date: date, durationMs: durationByDate[date]!),
      ],
    );
  }

  // ── Cycle (Phase 4) — typed via native CycleHealthReader (read-only) ────────

  @override
  Future<List<MenstruationFlowEntry>> readMenstruationFlowEntries(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readMenstruationFlowEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <MenstruationFlowEntryMsg>[],
    );
    return [
      for (final m in msgs)
        MenstruationFlowEntry(
          time: _fromMs(m.timeEpochMs),
          flow: m.flow,
          source: m.source,
        ),
    ];
  }

  @override
  Future<List<MenstruationPeriodEntry>> readMenstruationPeriods(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readMenstruationPeriods(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <MenstruationPeriodEntryMsg>[],
    );
    return [
      for (final m in msgs)
        MenstruationPeriodEntry(
          startTime: _fromMs(m.startEpochMs),
          endTime: _fromMs(m.endEpochMs),
          source: m.source,
        ),
    ];
  }

  @override
  Future<List<OvulationTestEntry>> readOvulationTests(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readOvulationTests(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <OvulationTestEntryMsg>[],
    );
    return [
      for (final m in msgs)
        OvulationTestEntry(
          time: _fromMs(m.timeEpochMs),
          result: m.result,
          source: m.source,
        ),
    ];
  }

  @override
  Future<List<CervicalMucusEntry>> readCervicalMucusEntries(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readCervicalMucusEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <CervicalMucusEntryMsg>[],
    );
    return [
      for (final m in msgs)
        CervicalMucusEntry(
          time: _fromMs(m.timeEpochMs),
          appearance: m.appearance,
          sensation: m.sensation,
          source: m.source,
        ),
    ];
  }

  @override
  Future<List<BasalBodyTemperatureEntry>> readBasalBodyTemperatureEntries(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readBasalBodyTemperatureEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <BasalBodyTemperatureEntryMsg>[],
    );
    return [
      for (final m in msgs)
        BasalBodyTemperatureEntry(
          time: _fromMs(m.timeEpochMs),
          temperatureCelsius: m.temperatureCelsius,
          measurementLocation: m.measurementLocation,
          source: m.source,
        ),
    ];
  }

  @override
  Future<List<IntermenstrualBleedingEntry>> readIntermenstrualBleedingEntries(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readIntermenstrualBleedingEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <IntermenstrualBleedingEntryMsg>[],
    );
    return [
      for (final m in msgs)
        IntermenstrualBleedingEntry(
          time: _fromMs(m.timeEpochMs),
          source: m.source,
        ),
    ];
  }

  @override
  Future<List<SexualActivityEntry>> readSexualActivityEntries(
    DateTime start,
    DateTime end,
  ) async {
    final msgs = await _catch(
      () => _api.readSexualActivityEntries(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <SexualActivityEntryMsg>[],
    );
    return [
      for (final m in msgs)
        SexualActivityEntry(
          time: _fromMs(m.timeEpochMs),
          protectionUsed: m.protectionUsed,
          source: m.source,
        ),
    ];
  }

  // ── Writes ────────────────────────────────────────────────────────────────

  @override
  Future<String> writeHydrationEntry(HydrationWriteRequest request) =>
      _api.writeHydrationEntry(
        HydrationWriteRequestMsg(
          timeEpochMs: request.time.millisecondsSinceEpoch,
          volumeLiters: request.volumeLiters,
          drinkId: request.drinkId,
        ),
      );

  @override
  Future<void> updateHydrationEntry(
    String id,
    HydrationWriteRequest request,
  ) =>
      _api.updateHydrationEntry(
        id,
        HydrationWriteRequestMsg(
          timeEpochMs: request.time.millisecondsSinceEpoch,
          volumeLiters: request.volumeLiters,
          drinkId: request.drinkId,
        ),
      );

  @override
  Future<String?> deleteHydrationEntry(String id) =>
      // Returns the deleted record's clientRecordId (for paired-nutrition
      // cleanup, handled in the nutrition phase); ownership is enforced natively.
      _catch(() => _api.deleteHydrationEntry(id), null);

  // ── Mindfulness (Phase 2) — typed via native MindfulnessHealthReader ────────

  MindfulnessSession _mindfulnessSession(MindfulnessSessionMsg m) =>
      MindfulnessSession(
        id: m.id,
        title: m.title,
        startTime: _fromMs(m.startEpochMs),
        endTime: _fromMs(m.endEpochMs),
        durationMs: m.durationMs,
        source: m.source,
        isOpenVitalsEntry: m.isOpenVitalsEntry,
      );

  @override
  Future<List<MindfulnessSession>> readMindfulnessSessions(
    DateTime start,
    DateTime end,
  ) async {
    if (!isMindfulnessSessionAvailable()) return const <MindfulnessSession>[];
    final msgs = await _catch(
      () => _api.readMindfulnessSessions(
        start.millisecondsSinceEpoch,
        end.millisecondsSinceEpoch,
      ),
      const <MindfulnessSessionMsg>[],
    );
    return [for (final m in msgs) _mindfulnessSession(m)];
  }

  @override
  Future<MindfulnessSession?> readMindfulnessSession(String id) async {
    if (!isMindfulnessSessionAvailable()) return null;
    final m = await _catch(() => _api.readMindfulnessSession(id), null);
    return m == null ? null : _mindfulnessSession(m);
  }

  @override
  Future<int> readMindfulnessMinutes(LocalDate date) async {
    if (!isMindfulnessSessionAvailable()) return 0;
    return _catch(
      () => _api.readMindfulnessMinutes(
        _dayStart(date).millisecondsSinceEpoch,
        _dayEnd(date).millisecondsSinceEpoch,
      ),
      0,
    );
  }

  @override
  Future<String> writeMindfulnessSessionEntry(
    MindfulnessSessionWriteRequest request,
  ) =>
      _api.writeMindfulnessSessionEntry(
        MindfulnessSessionWriteRequestMsg(
          title: request.title,
          startEpochMs: request.startTime.millisecondsSinceEpoch,
          endEpochMs: request.endTime.millisecondsSinceEpoch,
        ),
      );

  @override
  Future<void> updateMindfulnessSessionEntry(
    String id,
    MindfulnessSessionWriteRequest request,
  ) =>
      _api.updateMindfulnessSessionEntry(
        id,
        MindfulnessSessionWriteRequestMsg(
          title: request.title,
          startEpochMs: request.startTime.millisecondsSinceEpoch,
          endEpochMs: request.endTime.millisecondsSinceEpoch,
        ),
      );

  @override
  Future<void> deleteMindfulnessSessionEntry(String id) =>
      _api.deleteMindfulnessSessionEntry(id);

  @override
  Future<String> writeBodyMeasurementEntry(
    BodyMeasurementWriteRequest request,
  ) =>
      _api.writeBodyMeasurementEntry(
        BodyMeasurementWriteRequestMsg(
          type: _bodyTypeMsg(request.type),
          timeEpochMs: request.time.millisecondsSinceEpoch,
          value: request.value,
        ),
      );

  @override
  Future<void> updateBodyMeasurementEntry(
    String id,
    BodyMeasurementWriteRequest request,
  ) =>
      _api.updateBodyMeasurementEntry(
        id,
        BodyMeasurementWriteRequestMsg(
          type: _bodyTypeMsg(request.type),
          timeEpochMs: request.time.millisecondsSinceEpoch,
          value: request.value,
        ),
      );

  @override
  Future<void> deleteBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  ) =>
      _api.deleteBodyMeasurementEntry(_bodyTypeMsg(type), id);

  VitalsMeasurementWriteRequestMsg _vitalsWriteMsg(
    VitalsMeasurementWriteRequest request,
  ) =>
      VitalsMeasurementWriteRequestMsg(
        type: _vitalsTypeMsg(request.type),
        timeEpochMs: request.time.millisecondsSinceEpoch,
        value: request.value,
        secondaryValue: request.secondaryValue,
      );

  @override
  Future<String> writeVitalsMeasurementEntry(
    VitalsMeasurementWriteRequest request,
  ) =>
      _api.writeVitalsMeasurementEntry(_vitalsWriteMsg(request));

  @override
  Future<VitalsMeasurementEntry?> readVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  ) async {
    final m = await _catch(
      () => _api.readVitalsMeasurementEntry(_vitalsTypeMsg(type), id),
      null,
    );
    return m == null
        ? null
        : VitalsMeasurementEntry(
            id: m.id,
            type: _vitalsType(m.type),
            time: _fromMs(m.timeEpochMs),
            value: m.value,
            secondaryValue: m.secondaryValue,
            source: m.source,
            isOpenVitalsEntry: m.isOpenVitalsEntry,
          );
  }

  @override
  Future<void> updateVitalsMeasurementEntry(
    String id,
    VitalsMeasurementWriteRequest request,
  ) =>
      _api.updateVitalsMeasurementEntry(id, _vitalsWriteMsg(request));

  @override
  Future<void> deleteVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  ) =>
      _api.deleteVitalsMeasurementEntry(_vitalsTypeMsg(type), id);

  @override
  Future<String> writeNutritionEntry(NutritionWriteRequest request) =>
      _api.writeNutritionEntry(
        NutritionWriteRequestMsg(
          timeEpochMs: request.time.millisecondsSinceEpoch,
          name: request.name,
          nutrientValues: _nutrientMsg(request.nutrientValues),
          associatedHydrationClientRecordId:
              request.associatedHydrationClientRecordId,
        ),
      );

  @override
  Future<String?> deleteNutritionEntry(String id) =>
      _catch(() => _api.deleteNutritionEntry(id), null);

  @override
  Future<void> deleteHydrationNutritionEntry(String hydrationClientRecordId) =>
      _api.deleteHydrationNutritionEntry(hydrationClientRecordId);

  @override
  Future<String> writeActivityEntry(ActivityWriteRequest request) =>
      _api.writeActivityEntry(_activityWriteMsg(request));

  @override
  Future<void> updateActivityEntry(String id, ActivityWriteRequest request) =>
      _api.updateActivityEntry(id, _activityWriteMsg(request));

  @override
  Future<void> deleteActivityEntry(String id) =>
      _api.deleteActivityEntry(id);

  // ── Apple Health import ────────────────────────────────────────────────────

  @override
  Future<void> insertImportedRecords(List<ImportRecord> records) async {
    if (records.isEmpty) return;
    // Let failures propagate so the import service can classify duplicates /
    // failures and retry individually (Kotlin parity).
    await _api.insertImportedRecords([
      for (final record in records) importRecordMsg(record),
    ]);
  }

  @override
  Future<Set<String>> findMatchingImportedClientRecordIds(
    String recordType,
    DateTime start,
    DateTime end,
    Set<String> wantedIds,
  ) async {
    if (wantedIds.isEmpty) return const <String>{};
    final schemaType = HealthRecordJson.schemaTypeForImport(recordType);
    if (schemaType == null) return const <String>{};
    final existing = await _catch(
      () => _api.filterExistingClientIds(schemaType, wantedIds.toList()),
      const <String>[],
    );
    return existing.toSet();
  }
}
