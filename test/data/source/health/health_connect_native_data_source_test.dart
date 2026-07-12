import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:health_connect_native/health_connect_native.dart';
import 'package:openvitals/core/time/local_date.dart';
import 'package:openvitals/domain/model/activity_models.dart';
import 'package:openvitals/domain/model/body_models.dart';
import 'package:openvitals/domain/model/health_connect_availability.dart';
import 'package:openvitals/domain/model/health_connect_feature_status.dart';
import 'package:openvitals/domain/model/nutrition_models.dart';
import 'package:openvitals/domain/model/vitals_models.dart';
import 'package:openvitals/domain/model/apple_health_import_records.dart';
import 'package:openvitals/data/source/health/native/health_connect_native_data_source.dart';

const _appPackage = 'tech.mmarca.openvitals';

/// In-memory [HealthConnectHostApi] that returns canned JSON and captures
/// writes/deletes, so the data source can be exercised without a device.
class FakeHostApi extends HealthConnectHostApi {
  FakeHostApi() : super();

  int sdkStatus = 3;
  Set<String> availableFeatures = {};

  /// Features the provider is too old to even report on (→ [FeatureStatusMsg.unknown]).
  Set<String> unknownFeatures = {};
  List<String> grantedPermissionsResult = const [];
  bool requestPermissionsResult = true;

  Map<String, double?> aggregateValues = {};
  List<String> periodBuckets = const [];
  List<String> durationBuckets = const [];
  List<ActivityCadenceSampleMsg> cadenceSamples = const [];
  List<PlannedExerciseSessionMsg> plannedSessions = const [];
  PlannedExerciseWriteRequestMsg? writtenPlan;

  /// The arguments the last `aggregateGroupByDurationJson` call was made with.
  ({List<String> metrics, int startEpochMs, int endEpochMs, int bucketMinutes})?
      lastDurationQuery;

  /// Every `aggregateGroupByDurationJson` call, in order — lets tests assert the
  /// range was chunked instead of issued as one over-large query.
  final List<({int startEpochMs, int endEpochMs})> durationQueries = [];
  List<String> existingClientIds = const [];

  final List<({String type, List<String> ids})> filterQueries = [];

  @override
  Future<int> getSdkStatus() async => sdkStatus;

  @override
  Future<List<String>> getGrantedPermissions(List<String> permissions) async =>
      grantedPermissionsResult;

  /// Permissions the fake provider doesn't recognize (dropped by
  /// `filterSupportedPermissions`).
  Set<String> unsupportedPermissions = {};

  @override
  Future<List<String>> filterSupportedPermissions(
    List<String> permissions,
  ) async =>
      permissions.where((p) => !unsupportedPermissions.contains(p)).toList();

  @override
  Future<bool> requestPermissions(List<String> permissions) async =>
      requestPermissionsResult;

  @override
  Future<FeatureStatusMsg> getFeatureStatus(String feature) async {
    if (unknownFeatures.contains(feature)) return FeatureStatusMsg.unknown;
    return availableFeatures.contains(feature)
        ? FeatureStatusMsg.available
        : FeatureStatusMsg.unavailable;
  }

  @override
  Future<Map<String, double?>> aggregate(
    List<String> aggregateMetrics,
    int startEpochMs,
    int endEpochMs,
  ) async =>
      {for (final m in aggregateMetrics) m: aggregateValues[m]};

  @override
  Future<List<String>> aggregateGroupByPeriodJson(
    List<String> aggregateMetrics,
    int startEpochMs,
    int endEpochMs,
    String bucketType,
  ) async =>
      periodBuckets;

  @override
  Future<List<ActivityCadenceSampleMsg>> readActivityCadenceSamples(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      cadenceSamples;

  @override
  Future<List<PlannedExerciseSessionMsg>> readPlannedExerciseSessions(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      plannedSessions;

  @override
  Future<String> writePlannedExerciseSession(
    PlannedExerciseWriteRequestMsg request,
  ) async {
    writtenPlan = request;
    return 'plan-1';
  }

  @override
  Future<List<String>> aggregateGroupByDurationJson(
    List<String> aggregateMetrics,
    int startEpochMs,
    int endEpochMs,
    int bucketMinutes,
  ) async {
    lastDurationQuery = (
      metrics: aggregateMetrics,
      startEpochMs: startEpochMs,
      endEpochMs: endEpochMs,
      bucketMinutes: bucketMinutes,
    );
    durationQueries.add((startEpochMs: startEpochMs, endEpochMs: endEpochMs));
    return durationBuckets;
  }

  @override
  Future<List<String>> filterExistingClientIds(
    String recordType,
    List<String> clientRecordIds,
  ) async {
    filterQueries.add((type: recordType, ids: clientRecordIds));
    return existingClientIds;
  }

  // ── Body (Phase 1) typed fakes ────────────────────────────────────────────
  List<WeightEntryMsg> weightEntries = const [];
  BodyMeasurementWriteRequestMsg? bodyWriteRequest;

  @override
  Future<List<WeightEntryMsg>> readWeightEntries(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      weightEntries;

  @override
  Future<String> writeBodyMeasurementEntry(
    BodyMeasurementWriteRequestMsg request,
  ) async {
    bodyWriteRequest = request;
    return 'openvitals_body_${request.type.name}_written';
  }

  // ── Hydration (Phase 2) typed fakes ───────────────────────────────────────
  HydrationWriteRequestMsg? hydrationWriteRequest;

  @override
  Future<String> writeHydrationEntry(HydrationWriteRequestMsg request) async {
    hydrationWriteRequest = request;
    return 'openvitals_hydration_written';
  }

  // ── Sleep (Phase 7) typed fakes ───────────────────────────────────────────
  final Map<String, SleepDataMsg> sleepById = {};

  @override
  Future<SleepDataMsg?> readSleepSessionById(String id) async => sleepById[id];

  // ── Heart (Phase 5) typed fakes ───────────────────────────────────────────
  List<HeartRateSampleMsg> rawHeartRateSamples = const [];

  @override
  Future<List<HeartRateSampleMsg>> readRawHeartRateSamples(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      rawHeartRateSamples;

  // ── Vitals (Phase 3) typed fakes ──────────────────────────────────────────
  List<BloodPressureEntryMsg> bloodPressureEntries = const [];
  VitalsMeasurementWriteRequestMsg? vitalsWriteRequest;

  @override
  Future<List<BloodPressureEntryMsg>> readBloodPressureEntries(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      bloodPressureEntries;

  @override
  Future<String> writeVitalsMeasurementEntry(
    VitalsMeasurementWriteRequestMsg request,
  ) async {
    vitalsWriteRequest = request;
    return 'openvitals_vitals_${request.type.name}_written';
  }

  // ── Nutrition (Phase 6) typed fakes ───────────────────────────────────────
  NutritionWriteRequestMsg? nutritionWriteRequest;

  @override
  Future<String> writeNutritionEntry(NutritionWriteRequestMsg request) async {
    nutritionWriteRequest = request;
    return 'openvitals_nutrition_written';
  }

  // ── Apple Health import (Phase 9) typed fakes ─────────────────────────────
  List<ImportRecordMsg> importedRecords = const [];

  @override
  Future<List<String>> insertImportedRecords(
    List<ImportRecordMsg> records,
  ) async {
    importedRecords = records;
    return [for (var i = 0; i < records.length; i++) 'imported_$i'];
  }

  // ── Activity (Phase 8) typed fakes ────────────────────────────────────────
  ActivityWriteRequestMsg? activityWriteRequest;
  final List<String> deletedActivityIds = [];
  List<ExerciseDataMsg> exerciseSessions = const [];

  @override
  Future<List<ExerciseDataMsg>> readExerciseSessions(
    int startEpochMs,
    int endEpochMs,
  ) async =>
      exerciseSessions;

  /// The include-flags the last `readExerciseSessionsWithMetrics` was made with.
  ({bool includeDistance, bool includeSpeed})? lastMetricsQuery;

  @override
  Future<List<ExerciseDataMsg>> readExerciseSessionsWithMetrics(
    int startEpochMs,
    int endEpochMs,
    bool includeDistance,
    bool includeSpeed,
  ) async {
    lastMetricsQuery =
        (includeDistance: includeDistance, includeSpeed: includeSpeed);
    // Mirrors the native reader: an ungranted metric is simply left out of the
    // aggregate, so it comes back null.
    return [
      for (final m in exerciseSessions)
        ExerciseDataMsg(
          id: m.id,
          title: m.title,
          exerciseType: m.exerciseType,
          startEpochMs: m.startEpochMs,
          endEpochMs: m.endEpochMs,
          source: m.source,
          notes: m.notes,
          clientRecordId: m.clientRecordId,
          plannedExerciseSessionId: m.plannedExerciseSessionId,
          device: m.device,
          segments: m.segments,
          laps: m.laps,
          route: m.route,
          isOpenVitalsEntry: m.isOpenVitalsEntry,
          totalDistanceMeters:
              includeDistance ? m.totalDistanceMeters : null,
          averageSpeedMetersPerSecond:
              includeSpeed ? m.averageSpeedMetersPerSecond : null,
        ),
    ];
  }

  @override
  Future<String> writeActivityEntry(ActivityWriteRequestMsg request) async {
    activityWriteRequest = request;
    return 'openvitals_activity_written';
  }

  @override
  Future<void> deleteActivityEntry(String id) async {
    deletedActivityIds.add(id);
  }
}

HealthConnectNativeDataSource _source(FakeHostApi api) =>
    HealthConnectNativeDataSource(hostApi: api, appPackageName: _appPackage);

int _ms(int y, int mo, int d, [int h = 0, int mi = 0]) =>
    DateTime.utc(y, mo, d, h, mi).millisecondsSinceEpoch;

/// A minimal one-hour exercise session, with the route metrics the with-metrics
/// read backfills.
ExerciseDataMsg _exerciseMsg({
  double? totalDistanceMeters,
  double? averageSpeedMetersPerSecond,
  List<ExerciseRoutePointMsg> routePoints = const [],
  int? startZoneOffsetSeconds,
  int? endZoneOffsetSeconds,
  int? lastModifiedEpochMs,
  int? clientRecordVersion,
  int? recordingMethod,
}) =>
    ExerciseDataMsg(
      id: 'ex-1',
      title: 'Morning run',
      exerciseType: 56,
      startEpochMs: _ms(2026, 1, 2, 8),
      endEpochMs: _ms(2026, 1, 2, 9),
      source: _appPackage,
      notes: null,
      clientRecordId: null,
      plannedExerciseSessionId: null,
      device: null,
      segments: const [],
      laps: const [],
      route: ExerciseRouteMsg(
        status: routePoints.isEmpty
            ? ExerciseRouteStatusMsg.noData
            : ExerciseRouteStatusMsg.data,
        points: routePoints,
      ),
      isOpenVitalsEntry: true,
      totalDistanceMeters: totalDistanceMeters,
      averageSpeedMetersPerSecond: averageSpeedMetersPerSecond,
      startZoneOffsetSeconds: startZoneOffsetSeconds,
      endZoneOffsetSeconds: endZoneOffsetSeconds,
      lastModifiedEpochMs: lastModifiedEpochMs,
      clientRecordVersion: clientRecordVersion,
      recordingMethod: recordingMethod,
    );

void main() {
  group('availability', () {
    test('maps SDK status ints and caches the result', () async {
      final api = FakeHostApi()..sdkStatus = 3;
      final source = _source(api);
      expect(await source.availability(), HealthConnectAvailability.available);
      expect(source.cachedAvailability, HealthConnectAvailability.available);

      api.sdkStatus = 2;
      expect(await source.availability(),
          HealthConnectAvailability.needsProviderUpdate);

      api.sdkStatus = 1;
      expect(await source.availability(), HealthConnectAvailability.notSupported);
    });

    test('resolveFeatureFlags reads optional-feature availability', () async {
      final api = FakeHostApi()
        ..availableFeatures = {'SKIN_TEMPERATURE', 'MINDFULNESS_SESSION'};
      final source = _source(api);
      final flags = await source.resolveFeatureFlags();
      expect(flags.skinTemperatureAvailable, isTrue);
      expect(flags.mindfulnessAvailable, isTrue);
      expect(flags.plannedExerciseAvailable, isFalse);
      expect(source.isSkinTemperatureAvailable(), isTrue);
    });

    test('getFeatureStatus surfaces the tri-state per feature', () async {
      final api = FakeHostApi()
        ..availableFeatures = {'SKIN_TEMPERATURE'}
        ..unknownFeatures = {'PLANNED_EXERCISE'};
      final source = _source(api);
      expect(
        await source.getFeatureStatus('SKIN_TEMPERATURE'),
        FeatureStatus.available,
      );
      expect(
        await source.getFeatureStatus('MINDFULNESS_SESSION'),
        FeatureStatus.unavailable,
      );
      // Provider too old to report → UNKNOWN, and it must not gate a feature on.
      final planned = await source.getFeatureStatus('PLANNED_EXERCISE');
      expect(planned, FeatureStatus.unknown);
      expect(planned.isAvailable, isFalse);
    });

    test('UNKNOWN feature status resolves the flag to unavailable', () async {
      final api = FakeHostApi()..unknownFeatures = {'MINDFULNESS_SESSION'};
      final source = _source(api);
      final flags = await source.resolveFeatureFlags();
      expect(flags.mindfulnessAvailable, isFalse);
      expect(source.isMindfulnessSessionAvailable(), isFalse);
    });

    test(
        'resolveSupportedPermissions drops permissions the provider does not '
        'recognize from every set', () async {
      const readStepsCadence = 'android.permission.health.READ_STEPS_CADENCE';
      const writeStepsCadence = 'android.permission.health.WRITE_STEPS_CADENCE';
      const readCyclingCadence =
          'android.permission.health.READ_CYCLING_PEDALING_CADENCE';
      const writeCyclingCadence =
          'android.permission.health.WRITE_CYCLING_PEDALING_CADENCE';
      final api = FakeHostApi()
        ..unsupportedPermissions = {
          readStepsCadence,
          writeStepsCadence,
          readCyclingCadence,
          writeCyclingCadence,
        };
      final source = _source(api);
      await source.resolveSupportedPermissions();
      final svc = source.permissionService;

      // The cadence reads/writes are gone from the leaf sets…
      expect(svc.activityExtrasPermissions, isNot(contains(readStepsCadence)));
      expect(
          svc.activityExtrasPermissions, isNot(contains(readCyclingCadence)));
      expect(svc.activityWritePermissions, isNot(contains(writeStepsCadence)));
      // …and therefore from the composed onboarding/managed sets.
      expect(svc.onboardingPermissions, isNot(contains(writeCyclingCadence)));
      expect(svc.managedPermissions, isNot(contains(readStepsCadence)));
      // Supported neighbours in the same groups are untouched.
      expect(svc.activityExtrasPermissions,
          contains('android.permission.health.READ_SPEED'));
      expect(svc.activityWritePermissions,
          contains('android.permission.health.WRITE_SPEED'));
    });
  });

  group('permissions', () {
    test('grantedPermissions returns the plugin-reported subset', () async {
      final api = FakeHostApi()
        ..grantedPermissionsResult = [
          'android.permission.health.READ_STEPS',
          'android.permission.health.READ_SLEEP',
        ];
      final source = _source(api);
      final granted = await source.grantedPermissions();
      expect(granted, {
        'android.permission.health.READ_STEPS',
        'android.permission.health.READ_SLEEP',
      });
    });

    test('requestPermissions returns false for an empty set', () async {
      final source = _source(FakeHostApi()..requestPermissionsResult = true);
      expect(await source.requestPermissions(const {}), isFalse);
      expect(
        await source.requestPermissions({'android.permission.health.READ_STEPS'}),
        isTrue,
      );
    });
  });

  group('reads', () {
    test('ExerciseSession msg maps to ExerciseData with segments/laps/route',
        () async {
      final api = FakeHostApi()
        ..exerciseSessions = [
          ExerciseDataMsg(
            id: 'ex-1',
            title: 'Morning run',
            exerciseType: 56,
            startEpochMs: _ms(2026, 1, 2, 8),
            endEpochMs: _ms(2026, 1, 2, 9),
            source: _appPackage,
            notes: 'felt good',
            clientRecordId: 'openvitals_activity_1',
            plannedExerciseSessionId: null,
            device: null,
            segments: [
              ExerciseSegmentMsg(
                startEpochMs: _ms(2026, 1, 2, 8),
                endEpochMs: _ms(2026, 1, 2, 8, 30),
                segmentType: 42,
                repetitions: 12,
                setIndex: null,
              ),
            ],
            laps: [
              ExerciseLapMsg(
                startEpochMs: _ms(2026, 1, 2, 8),
                endEpochMs: _ms(2026, 1, 2, 8, 15),
                lengthMeters: 1000.0,
              ),
            ],
            route: ExerciseRouteMsg(
              status: ExerciseRouteStatusMsg.data,
              points: [
                ExerciseRoutePointMsg(
                  timeEpochMs: _ms(2026, 1, 2, 8),
                  latitude: 51.5,
                  longitude: -0.12,
                  altitudeMeters: 10.0,
                  horizontalAccuracyMeters: null,
                  verticalAccuracyMeters: null,
                ),
              ],
            ),
            isOpenVitalsEntry: true,
          ),
        ];
      final sessions = await _source(api).readExerciseSessions(
        DateTime.utc(2026, 1, 2),
        DateTime.utc(2026, 1, 3),
      );
      expect(sessions, hasLength(1));
      final session = sessions.single;
      expect(session.id, 'ex-1');
      expect(session.exerciseType, 56);
      expect(session.title, 'Morning run');
      expect(session.clientRecordId, 'openvitals_activity_1');
      expect(session.isOpenVitalsEntry, isTrue);
      expect(session.durationMs, 3600000);
      expect(session.segments, hasLength(1));
      expect(session.segments.single.segmentType, 42);
      expect(session.segments.single.repetitions, 12);
      expect(session.laps, hasLength(1));
      expect(session.laps.single.lengthMeters, 1000.0);
      expect(session.route.status, ExerciseRouteStatus.data);
      expect(session.route.points, hasLength(1));
      expect(session.route.points.single.latitude, 51.5);
      // The plain read carries no aggregate-derived route metrics.
      expect(session.totalDistanceMeters, isNull);
      expect(session.averageSpeedMetersPerSecond, isNull);
    });

    test('readExerciseSessionsWithMetrics maps aggregate distance/speed through',
        () async {
      final api = FakeHostApi()
        ..exerciseSessions = [
          _exerciseMsg(totalDistanceMeters: 5000.0, averageSpeedMetersPerSecond: 2.7),
        ];

      final sessions = await _source(api).readExerciseSessionsWithMetrics(
        DateTime.utc(2026, 1, 2),
        DateTime.utc(2026, 1, 3),
        includeDistance: true,
        includeSpeed: true,
      );

      expect(api.lastMetricsQuery,
          (includeDistance: true, includeSpeed: true));
      expect(sessions.single.totalDistanceMeters, 5000.0);
      expect(sessions.single.averageSpeedMetersPerSecond, 2.7);
    });

    test('readExerciseSessionsWithMetrics degrades to null without the perms',
        () async {
      final api = FakeHostApi()
        ..exerciseSessions = [
          _exerciseMsg(totalDistanceMeters: 5000.0, averageSpeedMetersPerSecond: 2.7),
        ];

      // Health Connect reads are permission-gated: an ungranted distance/speed
      // must degrade to null metrics, never throw or drop the session.
      final sessions = await _source(api).readExerciseSessionsWithMetrics(
        DateTime.utc(2026, 1, 2),
        DateTime.utc(2026, 1, 3),
      );

      expect(api.lastMetricsQuery,
          (includeDistance: false, includeSpeed: false));
      expect(sessions, hasLength(1));
      expect(sessions.single.id, 'ex-1');
      expect(sessions.single.totalDistanceMeters, isNull);
      expect(sessions.single.averageSpeedMetersPerSecond, isNull);
    });

    test('readExerciseSessionsWithMetrics backfills distance from the route',
        () async {
      // No DistanceRecord in the window (aggregate returns nothing), but the
      // session has a route — Kotlin's `backfillRouteMetrics = true` path
      // derives the distance from the route geometry.
      final api = FakeHostApi()
        ..exerciseSessions = [
          _exerciseMsg(
            totalDistanceMeters: 0.0,
            routePoints: [
              ExerciseRoutePointMsg(
                timeEpochMs: _ms(2026, 1, 2, 8),
                latitude: 51.5,
                longitude: -0.12,
                altitudeMeters: null,
                horizontalAccuracyMeters: null,
                verticalAccuracyMeters: null,
              ),
              ExerciseRoutePointMsg(
                timeEpochMs: _ms(2026, 1, 2, 8, 30),
                latitude: 51.51,
                longitude: -0.12,
                altitudeMeters: null,
                horizontalAccuracyMeters: null,
                verticalAccuracyMeters: null,
              ),
            ],
          ),
        ];

      final sessions = await _source(api).readExerciseSessionsWithMetrics(
        DateTime.utc(2026, 1, 2),
        DateTime.utc(2026, 1, 3),
        includeDistance: true,
      );

      // ~0.01 degrees of latitude ≈ 1.1 km.
      expect(sessions.single.totalDistanceMeters, closeTo(1112, 20));
    });

    test('HeartRate raw samples map from typed msgs (short range)', () async {
      // A 1h range is below the aggregate threshold, so the data source takes
      // the raw path; sample flattening now happens in the native reader.
      final api = FakeHostApi()
        ..rawHeartRateSamples = [
          HeartRateSampleMsg(
            timeEpochMs: _ms(2026, 1, 2, 6),
            beatsPerMinute: 60,
            source: 'com.watch',
          ),
          HeartRateSampleMsg(
            timeEpochMs: _ms(2026, 1, 2, 6, 5),
            beatsPerMinute: 80,
            source: 'com.watch',
          ),
        ];
      final samples = await _source(api).readHeartRateSamples(
        DateTime.utc(2026, 1, 2, 6),
        DateTime.utc(2026, 1, 2, 7),
      );
      expect(samples, hasLength(2));
      expect(samples.map((s) => s.beatsPerMinute), [60, 80]);
      expect(samples.every((s) => s.source == 'com.watch'), isTrue);
    });

    test('BloodPressure entries map systolic/diastolic and ownership',
        () async {
      final api = FakeHostApi()
        ..bloodPressureEntries = [
          BloodPressureEntryMsg(
            timeEpochMs: _ms(2026, 1, 2, 8),
            systolicMmHg: 120,
            diastolicMmHg: 80,
            source: _appPackage,
            id: 'bp-1',
            isOpenVitalsEntry: true,
          ),
        ];
      final entries = await _source(api).readBloodPressureEntries(
        DateTime.utc(2026, 1, 2),
        DateTime.utc(2026, 1, 3),
      );
      expect(entries.single.systolicMmHg, 120);
      expect(entries.single.diastolicMmHg, 80);
      expect(entries.single.isOpenVitalsEntry, isTrue);
    });

    test('readSteps / readDistanceMeters use the aggregate API', () async {
      final api = FakeHostApi()
        ..aggregateValues = {
          'Steps.count': 8421.0,
          'Distance.distance': 6123.4,
          'FloorsClimbed.floors': 12.0,
        };
      final source = _source(api);
      final date = LocalDate(2026, 1, 2);
      expect(await source.readSteps(date), 8421);
      expect(await source.readDistanceMeters(date), closeTo(6123.4, 1e-9));
      expect(await source.readFloorsClimbed(date), 12);
    });

    test('readDailySteps slices 24h duration buckets over an instant range',
        () async {
      final api = FakeHostApi();
      final dayStartMs = DateTime(2026, 1, 2).millisecondsSinceEpoch;
      api.durationBuckets = [
        jsonEncode({
          'startEpochMs': dayStartMs,
          'endEpochMs': DateTime(2026, 1, 3).millisecondsSinceEpoch,
          'values': {
            'Steps.count': 5000.0,
            'Distance.distance': 4000.0,
            'ActiveCaloriesBurned.energy': 220.0,
          },
        }),
      ];
      final daily = await _source(api).readDailySteps(
        LocalDate(2026, 1, 2),
        LocalDate(2026, 1, 2),
        includeActiveCalories: true,
      );
      expect(daily, hasLength(1));
      expect(daily.single.date, LocalDate(2026, 1, 2));
      expect(daily.single.steps, 5000);
      expect(daily.single.distanceMeters, 4000.0);
      expect(daily.single.activeCaloriesKcal, 220.0);

      // Kotlin `readDailyStepsChunk` uses aggregateGroupByDuration over an
      // instant range. The period variant undercounts against the plain
      // `aggregate` the dashboard tile reads, so the totals disagreed.
      final query = api.lastDurationQuery;
      expect(query, isNotNull);
      expect(query!.bucketMinutes, 24 * 60);
      expect(query.startEpochMs, dayStartMs);
      expect(query.endEpochMs, DateTime(2026, 1, 3).millisecondsSinceEpoch);
    });

    test('readDailySteps chunks a multi-year range into <=366-day queries',
        () async {
      final api = FakeHostApi();
      // A 3-year span (>2*366 days). A single aggregateGroupByDuration over this
      // slices into >1000 buckets and Health Connect rejects it — the reason the
      // achievements scan (from the 2009 legacy start) returned nothing.
      final daily = await _source(api).readDailySteps(
        LocalDate(2023, 1, 1),
        LocalDate(2025, 12, 31),
        includeFloors: true,
      );

      // Every requested day is still materialised (empty days included).
      expect(daily.length, greaterThan(2 * 366));
      // Split into ceil(1096 / 366) = 3 chunks, each spanning at most 366 days.
      expect(api.durationQueries, hasLength(3));
      const maxSpanMs = 366 * 24 * 60 * 60 * 1000;
      for (final q in api.durationQueries) {
        expect(q.endEpochMs - q.startEpochMs, lessThanOrEqualTo(maxSpanMs));
      }
      // The chunks tile the range without gaps or overlap: first starts at the
      // range start, last ends at the day after the range end.
      expect(
        api.durationQueries.first.startEpochMs,
        DateTime(2023, 1, 1).millisecondsSinceEpoch,
      );
      expect(
        api.durationQueries.last.endEpochMs,
        DateTime(2026, 1, 1).millisecondsSinceEpoch,
      );
      // Floors were requested, so the metric set carries the floors aggregate.
      expect(api.lastDurationQuery!.metrics, contains('FloorsClimbed.floors'));
    });

    test('readDailySteps maps floors when requested', () async {
      final api = FakeHostApi();
      api.durationBuckets = [
        jsonEncode({
          'startEpochMs': DateTime(2026, 1, 2).millisecondsSinceEpoch,
          'endEpochMs': DateTime(2026, 1, 3).millisecondsSinceEpoch,
          'values': {'Steps.count': 100.0, 'FloorsClimbed.floors': 12.0},
        }),
      ];
      final daily = await _source(api).readDailySteps(
        LocalDate(2026, 1, 2),
        LocalDate(2026, 1, 2),
        includeFloors: true,
      );
      expect(daily.single.floorsClimbed, 12);
      // Not requested → left null (device-records-nothing vs zero distinction).
      expect(daily.single.elevationGainedMeters, isNull);
    });

    test('single Sleep session maps stages from a typed msg', () async {
      final api = FakeHostApi()
        ..sleepById['sleep-1'] = SleepDataMsg(
          id: 'sleep-1',
          startEpochMs: _ms(2026, 1, 2, 23),
          endEpochMs: _ms(2026, 1, 3, 6),
          source: 'com.watch',
          title: 'Night',
          notes: null,
          clientRecordId: null,
          device: null,
          stages: [
            SleepStageMsg(
              startEpochMs: _ms(2026, 1, 2, 23),
              endEpochMs: _ms(2026, 1, 3, 1),
              stageType: 4,
            ),
            SleepStageMsg(
              startEpochMs: _ms(2026, 1, 3, 1),
              endEpochMs: _ms(2026, 1, 3, 6),
              stageType: 5,
            ),
          ],
        );
      final session = await _source(api).readSleepSession('sleep-1');
      expect(session, isNotNull);
      expect(session!.id, 'sleep-1');
      expect(session.stages, hasLength(2));
      expect(session.stages.first.stageType, 4);
      expect(session.stages.last.stageType, 5);
    });

    test('an Exercise session carries the record provenance across the bridge',
        () async {
      // Not cosmetic, unlike the sleep rows. `recordingMethod` is how the
      // activities screen counts manually-entered workouts, and
      // `lastModifiedTime` is the final tie-break deciding WHICH of two duplicate
      // sessions survives deduplication. Both were null for every session ever
      // read, because ExerciseDataMsg did not carry them — so the manual count
      // was always zero and the tie-break always a draw.
      final api = FakeHostApi()
        ..exerciseSessions = [
          _exerciseMsg(
            startZoneOffsetSeconds: -5 * 3600, // the writer's zone, not ours
            endZoneOffsetSeconds: -5 * 3600,
            lastModifiedEpochMs: _ms(2026, 1, 2, 10),
            clientRecordVersion: 7,
            recordingMethod: 3, // MANUAL_ENTRY
          ),
        ];

      final sessions = await _source(api).readExerciseSessions(
        DateTime.utc(2026, 1, 2),
        DateTime.utc(2026, 1, 3),
      );

      final session = sessions.single;
      expect(session.startZoneOffset, const Duration(hours: -5));
      expect(session.endZoneOffset, const Duration(hours: -5));
      expect(session.lastModifiedTime, isNotNull);
      expect(session.clientRecordVersion, 7);
      expect(session.recordingMethod, 3);
    });

    test('a Sleep session carries the record provenance the detail screen shows',
        () async {
      // Start zone, End zone, Recording, Last modified and Client version all
      // read "Not available" on the sleep detail screen, for every session ever
      // recorded. The domain model had the fields and the screen rendered them —
      // the Pigeon message simply never carried them, so they were always null.
      final api = FakeHostApi()
        ..sleepById['sleep-2'] = SleepDataMsg(
          id: 'sleep-2',
          startEpochMs: _ms(2026, 1, 2, 23),
          endEpochMs: _ms(2026, 1, 3, 6),
          source: 'nodomain.freeyourgadget.gadgetbridge',
          title: null,
          notes: null,
          clientRecordId: 'gb-sleep-1',
          device: null,
          stages: const [],
          // +02:00, i.e. the zone the WRITER recorded the night in.
          startZoneOffsetSeconds: 2 * 3600,
          endZoneOffsetSeconds: 2 * 3600,
          lastModifiedEpochMs: _ms(2026, 1, 3, 7),
          clientRecordVersion: 3,
          recordingMethod: 2,
        );

      final session = await _source(api).readSleepSession('sleep-2');

      expect(session!.startZoneOffset, const Duration(hours: 2));
      expect(session.endZoneOffset, const Duration(hours: 2));
      expect(session.lastModifiedTime, isNotNull);
      expect(session.clientRecordVersion, 3);
      expect(session.recordingMethod, 2);
    });

    test('a session with no zone offsets keeps them null, not zero', () async {
      // Null means "the writer recorded no offset"; zero means UTC. Collapsing
      // the two would print "UTC" for a record that never claimed one.
      final api = FakeHostApi()
        ..sleepById['sleep-3'] = SleepDataMsg(
          id: 'sleep-3',
          startEpochMs: _ms(2026, 1, 2, 23),
          endEpochMs: _ms(2026, 1, 3, 6),
          source: 'com.watch',
          title: null,
          notes: null,
          clientRecordId: null,
          device: null,
          stages: const [],
          startZoneOffsetSeconds: null,
          endZoneOffsetSeconds: null,
          lastModifiedEpochMs: null,
          clientRecordVersion: null,
          recordingMethod: null,
        );

      final session = await _source(api).readSleepSession('sleep-3');

      expect(session!.startZoneOffset, isNull);
      expect(session.endZoneOffset, isNull);
      expect(session.lastModifiedTime, isNull);
    });

    test('Weight entries map from typed msgs and preserve ownership', () async {
      // Unit conversion + ownership tagging now happen in the native
      // BodyHealthReader; the data source maps WeightEntryMsg -> WeightEntry.
      final api = FakeHostApi()
        ..weightEntries = [
          WeightEntryMsg(
            timeEpochMs: _ms(2026, 1, 1, 8),
            weightKg: 79.0,
            source: 'com.other',
            id: 'w-2',
            isOpenVitalsEntry: false,
          ),
          WeightEntryMsg(
            timeEpochMs: _ms(2026, 1, 2, 8),
            weightKg: 80.5,
            source: _appPackage,
            id: 'w-1',
            isOpenVitalsEntry: true,
          ),
        ];
      final entries = await _source(api).readWeightEntries(
        LocalDate(2026, 1, 1),
        LocalDate(2026, 1, 2),
      );
      expect(entries.map((e) => e.weightKg), [79.0, 80.5]);
      expect(entries.last.isOpenVitalsEntry, isTrue);
      expect(entries.first.isOpenVitalsEntry, isFalse);
      expect(entries.first.source, 'com.other');
    });
  });

  group('writes', () {
    test('writeHydrationEntry forwards a typed request', () async {
      // The record build (litres->mL, clientRecordId) now lives in the native
      // HydrationHealthReader; the data source forwards the typed request.
      final api = FakeHostApi();
      final id = await _source(api).writeHydrationEntry(
        HydrationWriteRequest(
          time: DateTime.utc(2026, 1, 2, 10),
          volumeLiters: 0.25,
        ),
      );
      expect(id, startsWith('openvitals_hydration_'));
      final req = api.hydrationWriteRequest!;
      expect(req.volumeLiters, 0.25);
      expect(req.timeEpochMs, _ms(2026, 1, 2, 10));
    });

    test('writeBodyMeasurementEntry forwards a typed request', () async {
      // The cm->m conversion + record build now live in the native
      // BodyHealthReader; the data source forwards the typed request.
      final api = FakeHostApi();
      final id = await _source(api).writeBodyMeasurementEntry(
        BodyMeasurementWriteRequest(
          type: BodyMeasurementType.height,
          time: DateTime.utc(2026, 1, 2, 8),
          value: 183.0,
        ),
      );
      expect(id, startsWith('openvitals_body_height_'));
      final req = api.bodyWriteRequest!;
      expect(req.type, BodyMeasurementTypeMsg.height);
      expect(req.value, 183.0);
      expect(req.timeEpochMs, _ms(2026, 1, 2, 8));
    });

    test('writeVitalsMeasurementEntry forwards a typed request', () async {
      // The record build (mmHg units, clientRecordId) now lives in the native
      // VitalsHealthReader; the data source forwards the typed request.
      final api = FakeHostApi();
      final id = await _source(api).writeVitalsMeasurementEntry(
        VitalsMeasurementWriteRequest(
          type: VitalsMeasurementType.bloodPressure,
          time: DateTime.utc(2026, 1, 2, 8),
          value: 118,
          secondaryValue: 76,
        ),
      );
      expect(id, isNotEmpty);
      final req = api.vitalsWriteRequest!;
      expect(req.type, VitalsMeasurementTypeMsg.bloodPressure);
      expect(req.value, 118);
      expect(req.secondaryValue, 76);
      expect(req.timeEpochMs, _ms(2026, 1, 2, 8));
    });

    test('writeNutritionEntry forwards a typed request keyed by storageName',
        () async {
      // The NutritionRecord field mapping now lives in the native reader; the
      // data source forwards nutrient values keyed by NutritionNutrient.storageName.
      final api = FakeHostApi();
      await _source(api).writeNutritionEntry(
        NutritionWriteRequest(
          time: DateTime.utc(2026, 1, 2, 12),
          name: 'Lunch',
          nutrientValues: {
            NutritionNutrient.energy: 500.0,
            NutritionNutrient.protein: 30.0,
            NutritionNutrient.totalCarbohydrate: 60.0,
            NutritionNutrient.dietaryFiber: 8.0,
            NutritionNutrient.caffeine: 0.05,
          },
        ),
      );
      final req = api.nutritionWriteRequest!;
      expect(req.name, 'Lunch');
      expect(req.timeEpochMs, _ms(2026, 1, 2, 12));
      expect(req.nutrientValues['ENERGY'], 500.0);
      expect(req.nutrientValues['PROTEIN'], 30.0);
      expect(req.nutrientValues['TOTAL_CARBOHYDRATE'], 60.0);
      expect(req.nutrientValues['DIETARY_FIBER'], 8.0);
      expect(req.nutrientValues['CAFFEINE'], 0.05);
    });

    test('writeActivityEntry forwards a typed ExerciseSession request',
        () async {
      // The ExerciseSession build now lives in the native ActivityHealthReader;
      // the data source forwards a typed request.
      final api = FakeHostApi();
      final id = await _source(api).writeActivityEntry(
        ActivityWriteRequest(
          exerciseType: 56,
          startTime: DateTime.utc(2026, 1, 2, 8),
          endTime: DateTime.utc(2026, 1, 2, 9),
          title: 'Run',
          exerciseSegments: [
            ActivityExerciseSegmentWrite(
              startTime: DateTime.utc(2026, 1, 2, 8),
              endTime: DateTime.utc(2026, 1, 2, 8, 30),
              segmentType: 42,
              repetitions: 10,
            ),
          ],
        ),
      );
      expect(id, startsWith('openvitals_activity_'));
      final req = api.activityWriteRequest!;
      expect(req.exerciseType, 56);
      expect(req.title, 'Run');
      expect(req.segments, hasLength(1));
      expect(req.segments.first.segmentType, 42);
      expect(req.segments.first.repetitions, 10);
    });

    test('deleteActivityEntry delegates by id', () async {
      final api = FakeHostApi();
      await _source(api).deleteActivityEntry('ex-9');
      expect(api.deletedActivityIds, ['ex-9']);
    });
  });

  group('apple health import', () {
    test('insertImportedRecords converts every record to canonical JSON',
        () async {
      final api = FakeHostApi();
      await _source(api).insertImportedRecords([
        WeightImportRecord(
          clientRecordId: 'apple_health_weight_1',
          time: DateTime.utc(2026, 1, 2, 8),
          zoneOffset: null,
          kilograms: 81.2,
        ),
        SleepSessionImportRecord(
          clientRecordId: 'apple_health_sleep_1',
          startTime: DateTime.utc(2026, 1, 2, 23),
          startZoneOffset: null,
          endTime: DateTime.utc(2026, 1, 3, 6),
          endZoneOffset: null,
          title: 'Night',
          stages: [
            SleepStageValue(
              startTime: DateTime.utc(2026, 1, 2, 23),
              endTime: DateTime.utc(2026, 1, 3, 6),
              stage: SleepStageType.deep,
            ),
          ],
        ),
      ]);
      expect(api.importedRecords, hasLength(2));
      final weight = api.importedRecords[0];
      expect(weight.recordType, 'Weight');
      expect(weight.doubleFields['weightKg'], 81.2);
      expect(weight.clientRecordId, 'apple_health_weight_1');
      final sleep = api.importedRecords[1];
      expect(sleep.recordType, 'Sleep');
      expect(sleep.sleepStages.single.stage, 5); // DEEP
      expect(sleep.clientRecordId, 'apple_health_sleep_1');
    });

    test('findMatchingImportedClientRecordIds maps targetType and filters',
        () async {
      final api = FakeHostApi()..existingClientIds = ['apple_health_a'];
      final matched = await _source(api).findMatchingImportedClientRecordIds(
        'SleepSessionRecord',
        DateTime.utc(2026, 1, 1),
        DateTime.utc(2026, 1, 3),
        {'apple_health_a', 'apple_health_b'},
      );
      expect(matched, {'apple_health_a'});
      // SleepSessionRecord maps to the schema record type "Sleep".
      expect(api.filterQueries.single.type, 'Sleep');
    });
  });

  group('readRawActivityProgress', () {
    /// One hourly bucket ending at [hour] o'clock on [date].
    String bucket(LocalDate date, int hour, Map<String, num?> values) {
      final end = DateTime(date.year, date.month, date.day, hour);
      return jsonEncode({
        'startEpochMs':
            end.subtract(const Duration(hours: 1)).millisecondsSinceEpoch,
        'endEpochMs': end.millisecondsSinceEpoch,
        'values': values,
      });
    }

    test('accumulates each hourly bucket into a running total', () async {
      final api = FakeHostApi();
      final date = LocalDate(2026, 7, 5);
      api.durationBuckets = [
        bucket(date, 9, {'Steps.count': 1200, 'Distance.distance': 800.0}),
        bucket(date, 10, {'Steps.count': 800, 'Distance.distance': 500.0}),
        bucket(date, 11, {'Steps.count': 2000, 'Distance.distance': 1400.0}),
      ];

      final points = await _source(api).readRawActivityProgress(date);

      expect(points, hasLength(3));
      // Cumulative, not per-bucket.
      expect([for (final p in points) p.totalSteps], [1200, 2000, 4000]);
      expect(points.last.totalDistanceMeters, 2700.0);
      expect(points.last.time, DateTime(2026, 7, 5, 11));
    });

    test('a metric the device never reports stays null, not a zero line',
        () async {
      final api = FakeHostApi();
      final date = LocalDate(2026, 7, 5);
      api.durationBuckets = [
        bucket(date, 9, {'Steps.count': 1000, 'FloorsClimbed.floors': null}),
      ];

      final point = (await _source(api).readRawActivityProgress(date)).single;

      expect(point.totalSteps, 1000);
      expect(point.totalFloorsClimbed, isNull);
      expect(point.totalElevationGainedMeters, isNull);
      expect(point.totalWheelchairPushes, isNull);
    });

    test('a metric stays non-null from the bucket it first appears in',
        () async {
      final api = FakeHostApi();
      final date = LocalDate(2026, 7, 5);
      api.durationBuckets = [
        bucket(date, 9, {'Steps.count': 1000}),
        bucket(date, 10, {'Steps.count': 500, 'FloorsClimbed.floors': 3}),
        bucket(date, 11, {'Steps.count': 500}),
      ];

      final points = await _source(api).readRawActivityProgress(date);

      expect(points[0].totalFloorsClimbed, isNull);
      expect(points[1].totalFloorsClimbed, 3);
      // The running total carries forward even though this bucket had none.
      expect(points[2].totalFloorsClimbed, 3);
    });

    test('asks for hourly buckets across the whole of a past day', () async {
      final api = FakeHostApi();
      final date = LocalDate(2026, 7, 5);
      await _source(api).readRawActivityProgress(date);

      final query = api.lastDurationQuery!;
      expect(query.bucketMinutes, 60);
      expect(
        DateTime.fromMillisecondsSinceEpoch(query.startEpochMs),
        DateTime(2026, 7, 5),
      );
      // A past day runs to midnight.
      expect(
        DateTime.fromMillisecondsSinceEpoch(query.endEpochMs),
        DateTime(2026, 7, 6),
      );
      expect(query.metrics, contains('Steps.count'));
    });

    test('today stops at now rather than running on to midnight', () async {
      final api = FakeHostApi();
      final before = DateTime.now();
      await _source(api).readRawActivityProgress(LocalDate.now());
      final after = DateTime.now();

      final end = DateTime.fromMillisecondsSinceEpoch(
        api.lastDurationQuery!.endEpochMs,
      );
      expect(end.isBefore(before.subtract(const Duration(seconds: 1))), isFalse);
      expect(end.isAfter(after.add(const Duration(seconds: 1))), isFalse);
    });

    test('no buckets means no points', () async {
      final api = FakeHostApi()..durationBuckets = const [];
      expect(
        await _source(api).readRawActivityProgress(LocalDate(2026, 7, 5)),
        isEmpty,
      );
    });
  });

  group('elevation + wheelchair aggregates', () {
    test('return the aggregated value', () async {
      final api = FakeHostApi()
        ..aggregateValues = {
          'ElevationGained.elevation': 123.5,
          'WheelchairPushes.count': 1240.0,
        };
      final source = _source(api);

      expect(await source.readElevationGained(LocalDate(2026, 7, 5)), 123.5);
      expect(await source.readWheelchairPushes(LocalDate(2026, 7, 5)), 1240);
    });

    test('stay null when the device records neither', () async {
      final api = FakeHostApi()
        ..aggregateValues = {
          'ElevationGained.elevation': null,
          'WheelchairPushes.count': null,
        };
      final source = _source(api);

      // Null, not 0: the metric screens show "no data" rather than a zero day.
      expect(await source.readElevationGained(LocalDate(2026, 7, 5)), isNull);
      expect(await source.readWheelchairPushes(LocalDate(2026, 7, 5)), isNull);
    });

    test('a wheelchair count is rounded to whole pushes', () async {
      final api = FakeHostApi()
        ..aggregateValues = {'WheelchairPushes.count': 1240.6};
      expect(await _source(api).readWheelchairPushes(LocalDate(2026, 7, 5)), 1241);
    });
  });

  group('readActivityCadenceSamples', () {
    test('maps cycling and steps samples to their own kinds', () async {
      final api = FakeHostApi()
        ..cadenceSamples = [
          ActivityCadenceSampleMsg(
            timeEpochMs: _ms(2026, 7, 5, 9),
            rate: 82.0,
            isCycling: true,
            source: 'garmin',
          ),
          ActivityCadenceSampleMsg(
            timeEpochMs: _ms(2026, 7, 5, 10),
            rate: 164.0,
            isCycling: false,
            source: 'phone',
          ),
        ];

      final samples = await _source(api).readActivityCadenceSamples(
        DateTime(2026, 7, 5),
        DateTime(2026, 7, 6),
      );

      expect(samples, hasLength(2));
      // 82 rpm on a bike; 164 steps/min running. Same shape, different unit.
      expect(samples[0].kind, ActivityCadenceKind.cycling);
      expect(samples[0].rate, 82.0);
      expect(samples[0].source, 'garmin');
      expect(samples[1].kind, ActivityCadenceKind.steps);
      expect(samples[1].rate, 164.0);
    });

    test('is empty when the device records no cadence', () async {
      expect(
        await _source(FakeHostApi()).readActivityCadenceSamples(
          DateTime(2026, 7, 5),
          DateTime(2026, 7, 6),
        ),
        isEmpty,
      );
    });
  });

  group('planned exercise sessions', () {
    PlannedExerciseStepMsg step(
      PlannedExerciseCompletionKindMsg kind, {
      int? reps,
      int? seconds,
    }) =>
        PlannedExerciseStepMsg(
          exerciseType: 79,
          exercisePhase: 2,
          description: 'set',
          completionKind: kind,
          completionRepetitions: reps,
          completionSeconds: seconds,
        );

    /// The feature is optional; a provider that lacks it must not be called.
    Future<HealthConnectNativeDataSource> enabledSource(FakeHostApi api) async {
      api.availableFeatures = {'PLANNED_EXERCISE'};
      final source = _source(api);
      await source.resolveFeatureFlags();
      return source;
    }

    test('reads sessions with their blocks, steps and completion goals',
        () async {
      final api = FakeHostApi()
        ..plannedSessions = [
          PlannedExerciseSessionMsg(
            id: 'p1',
            title: 'Push day',
            exerciseType: 79,
            startEpochMs: _ms(2026, 7, 5, 9),
            endEpochMs: _ms(2026, 7, 5, 10),
            hasExplicitTime: true,
            completedExerciseSessionId: null,
            notes: 'go easy',
            source: 'openvitals',
            blocks: [
              PlannedExerciseBlockMsg(
                repetitions: 3,
                description: 'main',
                steps: [
                  step(PlannedExerciseCompletionKindMsg.repetitions, reps: 12),
                  step(PlannedExerciseCompletionKindMsg.durationSeconds,
                      seconds: 60),
                  step(PlannedExerciseCompletionKindMsg.manual),
                  step(PlannedExerciseCompletionKindMsg.unknown),
                ],
              ),
            ],
          ),
        ];

      final plans = await (await enabledSource(api))
          .readPlannedExerciseSessions(DateTime(2026, 7, 5), DateTime(2026, 7, 6));

      final plan = plans.single;
      expect(plan.title, 'Push day');
      expect(plan.hasExplicitTime, isTrue);
      expect(plan.blockCount, 1);

      final steps = plan.blocks.single.steps;
      expect(plan.blocks.single.repetitions, 3);
      expect(
        steps[0].completion,
        const PlannedExerciseCompletionRepetitions(12),
      );
      expect(
        steps[1].completion,
        const PlannedExerciseCompletionDurationSeconds(60),
      );
      expect(steps[2].completion, isA<PlannedExerciseCompletionManual>());
      expect(steps[3].completion, isA<PlannedExerciseCompletionUnknown>());
    });

    test('a write round-trips every completion kind', () async {
      final api = FakeHostApi();
      final source = await enabledSource(api);

      final id = await source.writePlannedExerciseSession(
        PlannedExerciseWriteRequest(
          exerciseType: 79,
          startTime: DateTime.fromMillisecondsSinceEpoch(_ms(2026, 7, 5, 9)),
          endTime: DateTime.fromMillisecondsSinceEpoch(_ms(2026, 7, 5, 10)),
          title: 'Push day',
          blocks: const [
            PlannedExerciseBlockData(
              repetitions: 3,
              description: 'main',
              steps: [
                PlannedExerciseStepData(
                  exerciseType: 79,
                  exercisePhase: 2,
                  description: null,
                  completion: PlannedExerciseCompletionRepetitions(12),
                ),
                PlannedExerciseStepData(
                  exerciseType: 79,
                  exercisePhase: 3,
                  description: null,
                  completion: PlannedExerciseCompletionDurationSeconds(60),
                ),
                PlannedExerciseStepData(
                  exerciseType: 79,
                  exercisePhase: 2,
                  description: null,
                  completion: PlannedExerciseCompletionManual(),
                ),
              ],
            ),
          ],
        ),
      );

      expect(id, 'plan-1');
      final sent = api.writtenPlan!;
      expect(sent.title, 'Push day');
      final steps = sent.blocks.single.steps;
      expect(steps[0].completionKind,
          PlannedExerciseCompletionKindMsg.repetitions);
      expect(steps[0].completionRepetitions, 12);
      expect(steps[0].completionSeconds, isNull);
      expect(steps[1].completionKind,
          PlannedExerciseCompletionKindMsg.durationSeconds);
      expect(steps[1].completionSeconds, 60);
      expect(steps[1].completionRepetitions, isNull);
      expect(steps[2].completionKind, PlannedExerciseCompletionKindMsg.manual);
    });

    test('an unsupported provider reads empty and refuses to write', () async {
      final api = FakeHostApi()
        ..plannedSessions = [
          PlannedExerciseSessionMsg(
            id: 'p1',
            title: null,
            exerciseType: 79,
            startEpochMs: _ms(2026, 7, 5, 9),
            endEpochMs: _ms(2026, 7, 5, 10),
            hasExplicitTime: false,
            completedExerciseSessionId: null,
            notes: null,
            source: 'x',
            blocks: const [],
          ),
        ];
      // Feature flag left unresolved → unavailable.
      final source = _source(api);

      expect(
        await source.readPlannedExerciseSessions(
            DateTime(2026, 7, 5), DateTime(2026, 7, 6)),
        isEmpty,
      );
      await expectLater(
        source.writePlannedExerciseSession(
          PlannedExerciseWriteRequest(
            exerciseType: 79,
            startTime: DateTime(2026, 7, 5, 9),
            endTime: DateTime(2026, 7, 5, 10),
            blocks: const [],
          ),
        ),
        throwsUnsupportedError,
      );
      expect(api.writtenPlan, isNull);
    });
  });
}
