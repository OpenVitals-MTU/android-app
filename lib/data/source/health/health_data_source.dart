import '../../../core/time/local_date.dart';
import '../../../domain/model/activity_models.dart';
import '../../../domain/model/exercise_session_metrics.dart';
import '../../../domain/model/body_models.dart';
import '../../../domain/model/cycle_models.dart';
import '../../../domain/model/health_connect_availability.dart';
import '../../../domain/model/health_connect_feature_status.dart';
import '../../../domain/model/heart_models.dart';
import '../../../domain/model/mindfulness_models.dart';
import '../../../domain/model/nutrition_models.dart';
import '../../../domain/model/sleep_models.dart';
import '../../../domain/model/vitals_models.dart';
import '../../../domain/preferences/sleep_range_mode.dart';
import '../../../domain/model/apple_health_import_records.dart';
import 'health_permissions.dart';

/// The `HealthConnectManager` analogue: a single facade over the platform
/// health store that the repositories and [DashboardDataLoader] depend on.
///
/// This BASE class defines the full read/write surface with safe, side-effect
/// free defaults (empty lists / `null` / `0`). On Android,
/// `HealthConnectNativeDataSource` overrides these with real calls through the
/// native Health Connect plugin; on other platforms `UnsupportedHealthDataSource`
/// keeps the base defaults. Keeping the surface on a plain base class (rather
/// than an `abstract interface class`) lets unit tests subclass it and override
/// just the handful of methods a given test drives, without a device — every
/// other method degrades to the documented empty result, which matches the
/// Kotlin readers' "missing permission ⇒ emptyList()" behaviour.
class HealthDataSource {
  HealthDataSource({this.appPackageName});

  /// The app's own package/bundle id, used for OpenVitals-ownership tagging on
  /// reads (Kotlin `isOpenVitalsRecord`). When null, records are never treated
  /// as OpenVitals-owned.
  final String? appPackageName;

  HealthConnectFeatureFlags _featureFlags = const HealthConnectFeatureFlags();

  Set<String> _unsupportedPermissions = const <String>{};

  /// The last availability resolved by [availability]. The Kotlin
  /// `HealthConnectManager.availability()` is synchronous; the `health`
  /// package's SDK-status check is async, so callers that need it synchronously
  /// (e.g. `HealthRepository.availability()`) read this cached value, refreshed
  /// once at startup by [availability].
  HealthConnectAvailability cachedAvailability =
      HealthConnectAvailability.notSupported;

  /// The resolved permission taxonomy (feature-gated). Refreshed by
  /// [resolveFeatureFlags]; the base default assumes no optional features.
  HealthPermissionService get permissionService =>
      HealthPermissionService(_featureFlags, _unsupportedPermissions);

  set featureFlags(HealthConnectFeatureFlags flags) => _featureFlags = flags;

  /// The permissions the installed provider does not recognize (resolved by
  /// [resolveSupportedPermissions]); subtracted from every permission set.
  set unsupportedPermissions(Set<String> permissions) =>
      _unsupportedPermissions = permissions;

  // ── Availability / permissions ────────────────────────────────────────────

  Future<HealthConnectAvailability> availability() async =>
      HealthConnectAvailability.notSupported;

  /// Resolves optional-feature availability from the platform, caches it into
  /// [permissionService], and returns it.
  Future<HealthConnectFeatureFlags> resolveFeatureFlags() async => _featureFlags;

  /// Tri-state availability of a Health Connect feature (e.g.
  /// `"MINDFULNESS_SESSION"`, `"SKIN_TEMPERATURE"`, `"PLANNED_EXERCISE"`) via the
  /// native `getFeatureStatus`. The base can't reach a provider, so it reports
  /// [FeatureStatus.unavailable]; `HealthConnectNativeDataSource` overrides it.
  Future<FeatureStatus> getFeatureStatus(String feature) async =>
      FeatureStatus.unavailable;

  /// Resolves which of the app's permissions the installed Health Connect
  /// provider actually recognizes, caching the unsupported remainder into
  /// [permissionService] so device-undefined permissions (the app's
  /// connect-client is newer than the provider) drop out of every set. Base is a
  /// no-op; `HealthConnectNativeDataSource` resolves it via the plugin.
  Future<void> resolveSupportedPermissions() async {}

  bool isSkinTemperatureAvailable() => _featureFlags.skinTemperatureAvailable;

  bool isMindfulnessSessionAvailable() => _featureFlags.mindfulnessAvailable;

  bool isPlannedExerciseAvailable() => _featureFlags.plannedExerciseAvailable;

  /// Requests OS authorization for [permissions]. Returns success.
  Future<bool> requestPermissions(Set<String> permissions) async => false;

  /// The subset of [permissionService.managedPermissions] currently granted.
  Future<Set<String>> grantedPermissions() async => const <String>{};

  Future<bool> hasPermission(String permission) async =>
      (await grantedPermissions()).contains(permission);

  /// Opens the Health Connect page for this app so the user can manually grant
  /// permissions the runtime dialog reports as non-requestable. Returns whether
  /// a page was launched.
  Future<bool> openHealthConnectSettings() async => false;

  // ── Activity ──────────────────────────────────────────────────────────────

  Future<int> readSteps(LocalDate date) async => 0;

  Future<double> readDistanceMeters(LocalDate date) async => 0.0;

  Future<List<DailySteps>> readDailySteps(
    LocalDate startDate,
    LocalDate endDate, {
    bool includeActiveCalories = false,
    bool includeFloors = false,
    bool includeWheelchairPushes = false,
    bool includeElevation = false,
  }) async =>
      const <DailySteps>[];

  Future<int> readFloorsClimbed(LocalDate date) async => 0;

  /// Null when the device records no elevation at all, which the metric screens
  /// distinguish from a day that climbed nothing.
  Future<double?> readElevationGained(LocalDate date) async => null;

  Future<int?> readWheelchairPushes(LocalDate date) async => null;

  Future<List<ExerciseData>> readExerciseSessions(
    DateTime start,
    DateTime end,
  ) async =>
      const <ExerciseData>[];

  /// Like [readExerciseSessions], but each session also carries the route
  /// metrics that only an aggregate over the session's own window can produce —
  /// total distance ([includeDistance]) and average speed ([includeSpeed]).
  ///
  /// The flags are the caller's granted read-distance / read-speed permissions:
  /// an ungranted metric comes back null, never an error.
  Future<List<ExerciseData>> readExerciseSessionsWithMetrics(
    DateTime start,
    DateTime end, {
    bool includeDistance = false,
    bool includeSpeed = false,
  }) async =>
      const <ExerciseData>[];

  Future<ExerciseData?> readExerciseSession(String id) async => null;

  /// The day's cumulative metrics, hour by hour. Overridden by the native
  /// source; empty here (and on unsupported platforms), which the intraday chart
  /// renders as "no updates yet".
  Future<List<ActivityProgressPoint>> readRawActivityProgress(
    LocalDate date,
  ) async =>
      const <ActivityProgressPoint>[];

  /// The sibling-record totals for one exercise session's window (steps,
  /// distance, calories, elevation...). A session record carries none of them; see
  /// [ExerciseSessionMetrics]. [metrics] is what the caller holds a read
  /// permission for -- anything omitted comes back null.
  Future<ExerciseSessionMetrics> readExerciseSessionMetrics(
    DateTime start,
    DateTime end,
    Set<ExerciseSessionMetric> metrics,
  ) async =>
      ExerciseSessionMetrics.none;

  Future<List<SpeedSample>> readSpeedSamples(DateTime start, DateTime end) async =>
      const <SpeedSample>[];

  Future<List<ActivityCadenceSample>> readActivityCadenceSamples(
    DateTime start,
    DateTime end,
  ) async =>
      const <ActivityCadenceSample>[];

  Future<List<PlannedExerciseData>> readPlannedExerciseSessions(
    DateTime start,
    DateTime end,
  ) async =>
      const <PlannedExerciseData>[];

  Future<CaloriesBurnedValue?> readCaloriesBurned(
    LocalDate date, {
    bool includeEstimatedCalories = false,
  }) async =>
      null;

  Future<String> writeActivityEntry(ActivityWriteRequest request) async =>
      throw UnsupportedError('writeActivityEntry not supported by base source');

  Future<void> updateActivityEntry(String id, ActivityWriteRequest request) async {}

  Future<void> deleteActivityEntry(String id) async {}

  Future<String> writePlannedExerciseSession(
    PlannedExerciseWriteRequest request,
  ) async =>
      throw UnsupportedError('Planned exercise sessions are unsupported');

  // ── Nutrition / hydration ─────────────────────────────────────────────────

  Future<double?> readCaloriesInKcal(LocalDate date) async => null;

  Future<List<DailyMacros>> readDailyMacros(
    LocalDate startDate,
    LocalDate endDate,
  ) async =>
      const <DailyMacros>[];

  Future<List<NutritionEntry>> readNutritionEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <NutritionEntry>[];

  Future<List<DailyNutrition>> readDailyNutrition(
    LocalDate startDate,
    LocalDate endDate, {
    bool includeHydration = true,
    bool includeEstimatedCalories = false,
  }) async =>
      const <DailyNutrition>[];

  Future<double?> readHydrationLiters(LocalDate date) async => null;

  Future<List<DailyHydration>> readDailyHydration(
    LocalDate startDate,
    LocalDate endDate,
  ) async =>
      const <DailyHydration>[];

  Future<List<HydrationEntry>> readHydrationEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <HydrationEntry>[];

  Future<HydrationEntry?> readHydrationEntry(String id) async => null;

  Future<String> writeHydrationEntry(HydrationWriteRequest request) async =>
      throw UnsupportedError('writeHydrationEntry not supported by base source');

  Future<void> updateHydrationEntry(
    String id,
    HydrationWriteRequest request,
  ) async {}

  /// Deletes the hydration record; returns its clientRecordId (for paired
  /// nutrition cleanup), or null.
  Future<String?> deleteHydrationEntry(String id) async => null;

  Future<void> deleteHydrationNutritionEntry(
    String hydrationClientRecordId,
  ) async {}

  Future<String> writeNutritionEntry(NutritionWriteRequest request) async =>
      throw UnsupportedError('writeNutritionEntry not supported by base source');

  Future<String> writeCarbsEntry(NutritionWriteRequest request) =>
      writeNutritionEntry(request);

  Future<String?> deleteNutritionEntry(String id) async => null;

  // ── Body ──────────────────────────────────────────────────────────────────

  Future<WeightEntry?> readLatestWeight() async => null;

  Future<List<WeightEntry>> readWeightEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <WeightEntry>[];

  Future<double?> readLatestHeight() async => null;

  Future<HeightEntry?> readLatestHeightEntry() async => null;

  Future<List<HeightEntry>> readHeightEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <HeightEntry>[];

  Future<double?> readLatestBodyFat() async => null;

  Future<List<BodyFatEntry>> readBodyFatEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <BodyFatEntry>[];

  Future<double?> readLatestLeanBodyMass() async => null;

  Future<List<LeanBodyMassEntry>> readLeanBodyMassEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <LeanBodyMassEntry>[];

  Future<double?> readLatestBMR() async => null;

  Future<List<BmrEntry>> readBmrEntries(LocalDate start, LocalDate end) async =>
      const <BmrEntry>[];

  // Overridden by HealthConnectNativeDataSource on Android; base stays empty.
  Future<double?> readLatestBoneMass() async => null;

  Future<List<BoneMassEntry>> readBoneMassEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <BoneMassEntry>[];

  Future<double?> readLatestBodyWaterMass() async => null;

  Future<List<BodyWaterMassEntry>> readBodyWaterMassEntries(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <BodyWaterMassEntry>[];

  Future<String> writeBodyMeasurementEntry(
    BodyMeasurementWriteRequest request,
  ) async =>
      throw UnsupportedError('writeBodyMeasurementEntry not supported by base');

  Future<BodyMeasurementEntry?> readBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  ) async =>
      null;

  Future<void> updateBodyMeasurementEntry(
    String id,
    BodyMeasurementWriteRequest request,
  ) async {}

  Future<void> deleteBodyMeasurementEntry(
    BodyMeasurementType type,
    String id,
  ) async {}

  // ── Heart ─────────────────────────────────────────────────────────────────

  Future<int?> readAvgHeartRate(LocalDate date) async => null;

  Future<List<HeartRateSample>> readHeartRateSamples(
    DateTime start,
    DateTime end,
  ) async =>
      const <HeartRateSample>[];

  Future<List<HeartRateSample>> readRawHeartRateSamples(
    DateTime start,
    DateTime end,
  ) async =>
      const <HeartRateSample>[];

  /// Heart rate averaged into [bucket]-sized slices of `[start, end)`.
  ///
  /// The important difference from [readRawHeartRateSamples]: Health Connect
  /// filters series records by the RECORD's own boundary, so a raw read cannot
  /// see inside a `HeartRateRecord` that began outside the window — a device that
  /// groups an hour of beats into one record can hide every sample of a workout
  /// that started mid-record. Aggregation slices by TIME instead, so it cannot
  /// miss them.
  Future<List<HeartRateSample>> readAggregatedHeartRateSamples(
    DateTime start,
    DateTime end,
    Duration bucket,
  ) async =>
      const <HeartRateSample>[];

  Future<List<HeartRateSummary>> readDailyHeartRateSummaries(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <HeartRateSummary>[];

  Future<int?> readRestingHeartRate(LocalDate date) async => null;

  Future<List<RestingHeartRateSample>> readRestingHeartRateSamples(
    DateTime start,
    DateTime end,
  ) async =>
      const <RestingHeartRateSample>[];

  Future<List<DailyRestingHR>> readDailyRestingHR(
    LocalDate start,
    LocalDate end,
  ) async =>
      const <DailyRestingHR>[];

  Future<double?> readHrvRmssd(LocalDate date) async => null;

  Future<List<HrvSample>> readHrvSamples(DateTime start, DateTime end) async =>
      const <HrvSample>[];

  Future<List<DailyHrv>> readDailyHRV(LocalDate start, LocalDate end) async =>
      const <DailyHrv>[];

  // ── Vitals ────────────────────────────────────────────────────────────────

  Future<List<BloodPressureEntry>> readBloodPressureEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <BloodPressureEntry>[];

  Future<BloodPressureEntry?> readLatestBloodPressure(LocalDate date) async =>
      null;

  Future<List<SpO2Entry>> readSpO2Entries(DateTime start, DateTime end) async =>
      const <SpO2Entry>[];

  Future<SpO2Entry?> readLatestSpO2(LocalDate date) async => null;

  Future<List<RespiratoryRateEntry>> readRespiratoryRateEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <RespiratoryRateEntry>[];

  Future<List<BodyTempEntry>> readBodyTemperatureEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <BodyTempEntry>[];

  // Overridden by HealthConnectNativeDataSource on Android; base stays empty.
  Future<List<Vo2MaxEntry>> readVo2MaxEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <Vo2MaxEntry>[];

  Future<Vo2MaxEntry?> readLatestVo2Max(LocalDate date) async => null;

  Future<List<BloodGlucoseEntry>> readBloodGlucoseEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <BloodGlucoseEntry>[];

  Future<List<SkinTemperatureEntry>> readSkinTemperatureEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <SkinTemperatureEntry>[];

  Future<String> writeVitalsMeasurementEntry(
    VitalsMeasurementWriteRequest request,
  ) async =>
      throw UnsupportedError('writeVitalsMeasurementEntry not supported by base');

  Future<VitalsMeasurementEntry?> readVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  ) async =>
      null;

  Future<void> updateVitalsMeasurementEntry(
    String id,
    VitalsMeasurementWriteRequest request,
  ) async {}

  Future<void> deleteVitalsMeasurementEntry(
    VitalsMeasurementType type,
    String id,
  ) async {}

  // ── Sleep ─────────────────────────────────────────────────────────────────

  Future<SleepReadData> readSleepData(
    LocalDate startDate,
    LocalDate endDate,
    SleepRangeMode sleepRangeMode,
  ) async =>
      const SleepReadData();

  Future<List<SleepData>> readSleepSessions(DateTime start, DateTime end) async =>
      const <SleepData>[];

  Future<SleepData?> readSleepSession(String id) async => null;

  // ── Mindfulness ─────────────────────────────────────────────────────────

  Future<List<MindfulnessSession>> readMindfulnessSessions(
    DateTime start,
    DateTime end,
  ) async =>
      const <MindfulnessSession>[];

  Future<int> readMindfulnessMinutes(LocalDate date) async => 0;

  Future<MindfulnessSession?> readMindfulnessSession(String id) async => null;

  Future<String> writeMindfulnessSessionEntry(
    MindfulnessSessionWriteRequest request,
  ) async =>
      throw UnsupportedError('Mindfulness sessions unsupported on this platform');

  Future<void> updateMindfulnessSessionEntry(
    String id,
    MindfulnessSessionWriteRequest request,
  ) async {}

  Future<void> deleteMindfulnessSessionEntry(String id) async {}

  // ── Cycle ─────────────────────────────────────────────────────────────────

  Future<List<MenstruationFlowEntry>> readMenstruationFlowEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <MenstruationFlowEntry>[];

  Future<List<MenstruationPeriodEntry>> readMenstruationPeriods(
    DateTime start,
    DateTime end,
  ) async =>
      const <MenstruationPeriodEntry>[];

  Future<List<OvulationTestEntry>> readOvulationTests(
    DateTime start,
    DateTime end,
  ) async =>
      const <OvulationTestEntry>[];

  Future<List<CervicalMucusEntry>> readCervicalMucusEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <CervicalMucusEntry>[];

  Future<List<BasalBodyTemperatureEntry>> readBasalBodyTemperatureEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <BasalBodyTemperatureEntry>[];

  Future<List<IntermenstrualBleedingEntry>> readIntermenstrualBleedingEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <IntermenstrualBleedingEntry>[];

  Future<List<SexualActivityEntry>> readSexualActivityEntries(
    DateTime start,
    DateTime end,
  ) async =>
      const <SexualActivityEntry>[];

  // ── Apple Health import (Phase 6c) ────────────────────────────────────────

  /// Bulk-inserts records converted from an Apple Health export, tagged with a
  /// deterministic `apple_health_`-prefixed clientRecordId (Kotlin
  /// `HealthConnectManager.insertImportedRecords`). The base is a no-op so unit
  /// tests can drive a fake repository; `HealthConnectNativeDataSource` writes
  /// every record type through the native Health Connect plugin.
  Future<void> insertImportedRecords(List<ImportRecord> records) async {}

  /// The subset of [wantedIds] already present for [recordType] (an
  /// [ImportRecord.targetType]) within [start]..[end]. Base returns empty;
  /// `HealthConnectNativeDataSource` resolves it via the plugin's
  /// clientRecordId lookup (`filterExistingClientIds`).
  Future<Set<String>> findMatchingImportedClientRecordIds(
    String recordType,
    DateTime start,
    DateTime end,
    Set<String> wantedIds,
  ) async =>
      const <String>{};
}
