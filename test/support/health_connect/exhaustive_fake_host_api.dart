import 'package:flutter/services.dart';
import 'package:health_connect_native/health_connect_native.dart';

/// Every one of the 95 methods on [HealthConnectHostApi], and by default every one
/// of them THROWS.
///
/// `implements`, not `extends`. The existing `FakeHostApi`
/// (health_connect_native_data_source_test.dart) *extends*, and overrides 25 of 95 —
/// the other 70 fall through to the real Pigeon implementation, hit a channel with
/// no handler, and throw. The data source's `_catch` then swallows it and returns
/// the documented empty fallback. So a test that exercised any of those 70 got an
/// empty list, passed, and proved nothing. Mindfulness (0/6) and Cycle (0/7) have
/// no coverage at all and nothing says so.
///
/// `implements` makes the compiler demand all 95, and every one not deliberately
/// answered throws by NAME. An unimplemented read is then a loud failure instead of
/// a quiet empty list — which is the difference between a test suite and a
/// decoration.
// The two Pigeon-generated fields are part of the public interface, so implementing
// the class means implementing them. Their names are Pigeon's, not ours.
// ignore_for_file: non_constant_identifier_names
abstract class ExhaustiveFakeHostApi implements HealthConnectHostApi {
  ExhaustiveFakeHostApi();

  @override
  BinaryMessenger? get pigeonVar_binaryMessenger => null;

  @override
  String get pigeonVar_messageChannelSuffix => '';

  /// Every method a test reached for that the fake does not answer.
  ///
  /// Throwing is NOT enough, and finding that out was the whole point of building
  /// this. `HealthConnectNativeDataSource._catch` wraps every read and degrades a
  /// failure to the documented empty result -- so the refusal below is caught,
  /// logged, and turned into an empty list. The test then passes, against no data,
  /// having proved nothing. Exactly the failure mode this suite exists to end.
  ///
  /// So the refusals are RECORDED, and `bootContainer` fails the test if any were
  /// hit. The throw makes the read degrade like a real failure would; the record is
  /// what makes it impossible to ignore.
  final Set<String> refused = {};

  /// The default answer: refuse, by name, and remember it.
  Never unimplemented(String method) {
    refused.add(method);
    throw UnimplementedError(
      'FakeHealthConnect does not answer $method.\n'
      'It was not needed when the fake was written. Implement it from the fixture '
      '-- do NOT make it return an empty list, which is how a test passes while '
      'proving nothing.',
    );
  }

  @override
  Future<int> getSdkStatus() => unimplemented('getSdkStatus');

  @override
  Future<HealthConnectAvailabilityDetail> availabilityDetail() => unimplemented('availabilityDetail');

  @override
  Future<void> setSyncEnabled(bool enabled) => unimplemented('setSyncEnabled');

  @override
  Future<bool> getSyncEnabled() => unimplemented('getSyncEnabled');

  @override
  Future<List<String>> getGrantedPermissions(List<String> permissions) => unimplemented('getGrantedPermissions');

  @override
  Future<List<String>> filterSupportedPermissions(List<String> permissions) => unimplemented('filterSupportedPermissions');

  @override
  Future<bool> requestPermissions(List<String> permissions) => unimplemented('requestPermissions');

  @override
  Future<bool> openHealthConnectSettings() => unimplemented('openHealthConnectSettings');

  @override
  Future<FeatureStatusMsg> getFeatureStatus(String feature) => unimplemented('getFeatureStatus');

  @override
  Future<Map<String, double?>> aggregate(List<String> aggregateMetrics, int startEpochMs, int endEpochMs) => unimplemented('aggregate');

  @override
  Future<List<String>> aggregateGroupByPeriodJson(List<String> aggregateMetrics, int startEpochMs, int endEpochMs, String bucketType) => unimplemented('aggregateGroupByPeriodJson');

  @override
  Future<List<String>> aggregateGroupByDurationJson(List<String> aggregateMetrics, int startEpochMs, int endEpochMs, int bucketMinutes) => unimplemented('aggregateGroupByDurationJson');

  @override
  Future<List<String>> filterExistingClientIds(String recordType, List<String> clientRecordIds) => unimplemented('filterExistingClientIds');

  @override
  Future<List<WeightEntryMsg>> readWeightEntries(int startEpochMs, int endEpochMs) => unimplemented('readWeightEntries');

  @override
  Future<WeightEntryMsg?> readLatestWeight() => unimplemented('readLatestWeight');

  @override
  Future<List<HeightEntryMsg>> readHeightEntries(int startEpochMs, int endEpochMs) => unimplemented('readHeightEntries');

  @override
  Future<HeightEntryMsg?> readLatestHeightEntry() => unimplemented('readLatestHeightEntry');

  @override
  Future<List<BodyFatEntryMsg>> readBodyFatEntries(int startEpochMs, int endEpochMs) => unimplemented('readBodyFatEntries');

  @override
  Future<BodyFatEntryMsg?> readLatestBodyFat() => unimplemented('readLatestBodyFat');

  @override
  Future<List<BodyMassEntryMsg>> readLeanBodyMassEntries(int startEpochMs, int endEpochMs) => unimplemented('readLeanBodyMassEntries');

  @override
  Future<BodyMassEntryMsg?> readLatestLeanBodyMass() => unimplemented('readLatestLeanBodyMass');

  @override
  Future<List<BmrEntryMsg>> readBmrEntries(int startEpochMs, int endEpochMs) => unimplemented('readBmrEntries');

  @override
  Future<BmrEntryMsg?> readLatestBmr() => unimplemented('readLatestBmr');

  @override
  Future<List<BodyMassEntryMsg>> readBoneMassEntries(int startEpochMs, int endEpochMs) => unimplemented('readBoneMassEntries');

  @override
  Future<BodyMassEntryMsg?> readLatestBoneMass() => unimplemented('readLatestBoneMass');

  @override
  Future<List<BodyMassEntryMsg>> readBodyWaterMassEntries(int startEpochMs, int endEpochMs) => unimplemented('readBodyWaterMassEntries');

  @override
  Future<BodyMassEntryMsg?> readLatestBodyWaterMass() => unimplemented('readLatestBodyWaterMass');

  @override
  Future<String> writeBodyMeasurementEntry(BodyMeasurementWriteRequestMsg request) => unimplemented('writeBodyMeasurementEntry');

  @override
  Future<BodyMeasurementEntryMsg?> readBodyMeasurementEntry(BodyMeasurementTypeMsg type, String id) => unimplemented('readBodyMeasurementEntry');

  @override
  Future<void> updateBodyMeasurementEntry(String id, BodyMeasurementWriteRequestMsg request) => unimplemented('updateBodyMeasurementEntry');

  @override
  Future<void> deleteBodyMeasurementEntry(BodyMeasurementTypeMsg type, String id) => unimplemented('deleteBodyMeasurementEntry');

  @override
  Future<double?> readHydrationLiters(int startEpochMs, int endEpochMs) => unimplemented('readHydrationLiters');

  @override
  Future<List<DailyHydrationMsg>> readDailyHydration(int startEpochMs, int endEpochMs) => unimplemented('readDailyHydration');

  @override
  Future<List<HydrationEntryMsg>> readHydrationEntries(int startEpochMs, int endEpochMs) => unimplemented('readHydrationEntries');

  @override
  Future<HydrationEntryMsg?> readHydrationEntry(String id) => unimplemented('readHydrationEntry');

  @override
  Future<String> writeHydrationEntry(HydrationWriteRequestMsg request) => unimplemented('writeHydrationEntry');

  @override
  Future<void> updateHydrationEntry(String id, HydrationWriteRequestMsg request) => unimplemented('updateHydrationEntry');

  @override
  Future<String?> deleteHydrationEntry(String id) => unimplemented('deleteHydrationEntry');

  @override
  Future<List<MindfulnessSessionMsg>> readMindfulnessSessions(int startEpochMs, int endEpochMs) => unimplemented('readMindfulnessSessions');

  @override
  Future<MindfulnessSessionMsg?> readMindfulnessSession(String id) => unimplemented('readMindfulnessSession');

  @override
  Future<int> readMindfulnessMinutes(int startEpochMs, int endEpochMs) => unimplemented('readMindfulnessMinutes');

  @override
  Future<String> writeMindfulnessSessionEntry(MindfulnessSessionWriteRequestMsg request) => unimplemented('writeMindfulnessSessionEntry');

  @override
  Future<void> updateMindfulnessSessionEntry(String id, MindfulnessSessionWriteRequestMsg request) => unimplemented('updateMindfulnessSessionEntry');

  @override
  Future<void> deleteMindfulnessSessionEntry(String id) => unimplemented('deleteMindfulnessSessionEntry');

  @override
  Future<List<BloodPressureEntryMsg>> readBloodPressureEntries(int startEpochMs, int endEpochMs) => unimplemented('readBloodPressureEntries');

  @override
  Future<BloodPressureEntryMsg?> readLatestBloodPressure(int startEpochMs, int endEpochMs) => unimplemented('readLatestBloodPressure');

  @override
  Future<List<SpO2EntryMsg>> readSpO2Entries(int startEpochMs, int endEpochMs) => unimplemented('readSpO2Entries');

  @override
  Future<SpO2EntryMsg?> readLatestSpO2(int startEpochMs, int endEpochMs) => unimplemented('readLatestSpO2');

  @override
  Future<List<RespiratoryRateEntryMsg>> readRespiratoryRateEntries(int startEpochMs, int endEpochMs) => unimplemented('readRespiratoryRateEntries');

  @override
  Future<List<BodyTempEntryMsg>> readBodyTemperatureEntries(int startEpochMs, int endEpochMs) => unimplemented('readBodyTemperatureEntries');

  @override
  Future<List<Vo2MaxEntryMsg>> readVo2MaxEntries(int startEpochMs, int endEpochMs) => unimplemented('readVo2MaxEntries');

  @override
  Future<Vo2MaxEntryMsg?> readLatestVo2Max(int startEpochMs, int endEpochMs) => unimplemented('readLatestVo2Max');

  @override
  Future<List<BloodGlucoseEntryMsg>> readBloodGlucoseEntries(int startEpochMs, int endEpochMs) => unimplemented('readBloodGlucoseEntries');

  @override
  Future<List<SkinTemperatureEntryMsg>> readSkinTemperatureEntries(int startEpochMs, int endEpochMs) => unimplemented('readSkinTemperatureEntries');

  @override
  Future<List<DailyBloodPressurePointMsg>> readDailyBloodPressure(int startEpochMs, int endEpochMs) => unimplemented('readDailyBloodPressure');

  @override
  Future<List<DailyVitalPointMsg>> readDailySpO2(int startEpochMs, int endEpochMs) => unimplemented('readDailySpO2');

  @override
  Future<List<DailyVitalPointMsg>> readDailyRespiratoryRate(int startEpochMs, int endEpochMs) => unimplemented('readDailyRespiratoryRate');

  @override
  Future<List<DailyVitalPointMsg>> readDailyBodyTemperature(int startEpochMs, int endEpochMs) => unimplemented('readDailyBodyTemperature');

  @override
  Future<List<DailyVitalPointMsg>> readDailyVo2Max(int startEpochMs, int endEpochMs) => unimplemented('readDailyVo2Max');

  @override
  Future<List<DailyVitalPointMsg>> readDailyBloodGlucose(int startEpochMs, int endEpochMs) => unimplemented('readDailyBloodGlucose');

  @override
  Future<List<DailyVitalPointMsg>> readDailySkinTemperature(int startEpochMs, int endEpochMs) => unimplemented('readDailySkinTemperature');

  @override
  Future<RespiratoryRateEntryMsg?> readLatestRespiratoryRate(int startEpochMs, int endEpochMs) => unimplemented('readLatestRespiratoryRate');

  @override
  Future<BodyTempEntryMsg?> readLatestBodyTemperature(int startEpochMs, int endEpochMs) => unimplemented('readLatestBodyTemperature');

  @override
  Future<BloodGlucoseEntryMsg?> readLatestBloodGlucose(int startEpochMs, int endEpochMs) => unimplemented('readLatestBloodGlucose');

  @override
  Future<SkinTemperatureEntryMsg?> readLatestSkinTemperature(int startEpochMs, int endEpochMs) => unimplemented('readLatestSkinTemperature');

  @override
  Future<String> getVitalsChangesToken(String recordType) => unimplemented('getVitalsChangesToken');

  @override
  Future<VitalsChangesMsg> getVitalsChanges(String token) => unimplemented('getVitalsChanges');

  @override
  Future<String> writeVitalsMeasurementEntry(VitalsMeasurementWriteRequestMsg request) => unimplemented('writeVitalsMeasurementEntry');

  @override
  Future<VitalsMeasurementEntryMsg?> readVitalsMeasurementEntry(VitalsMeasurementTypeMsg type, String id) => unimplemented('readVitalsMeasurementEntry');

  @override
  Future<void> updateVitalsMeasurementEntry(String id, VitalsMeasurementWriteRequestMsg request) => unimplemented('updateVitalsMeasurementEntry');

  @override
  Future<void> deleteVitalsMeasurementEntry(VitalsMeasurementTypeMsg type, String id) => unimplemented('deleteVitalsMeasurementEntry');

  @override
  Future<List<MenstruationFlowEntryMsg>> readMenstruationFlowEntries(int startEpochMs, int endEpochMs) => unimplemented('readMenstruationFlowEntries');

  @override
  Future<List<MenstruationPeriodEntryMsg>> readMenstruationPeriods(int startEpochMs, int endEpochMs) => unimplemented('readMenstruationPeriods');

  @override
  Future<List<OvulationTestEntryMsg>> readOvulationTests(int startEpochMs, int endEpochMs) => unimplemented('readOvulationTests');

  @override
  Future<List<CervicalMucusEntryMsg>> readCervicalMucusEntries(int startEpochMs, int endEpochMs) => unimplemented('readCervicalMucusEntries');

  @override
  Future<List<BasalBodyTemperatureEntryMsg>> readBasalBodyTemperatureEntries(int startEpochMs, int endEpochMs) => unimplemented('readBasalBodyTemperatureEntries');

  @override
  Future<List<IntermenstrualBleedingEntryMsg>> readIntermenstrualBleedingEntries(int startEpochMs, int endEpochMs) => unimplemented('readIntermenstrualBleedingEntries');

  @override
  Future<List<SexualActivityEntryMsg>> readSexualActivityEntries(int startEpochMs, int endEpochMs) => unimplemented('readSexualActivityEntries');

  @override
  Future<int?> readAvgHeartRate(int startEpochMs, int endEpochMs) => unimplemented('readAvgHeartRate');

  @override
  Future<List<HeartRateSampleMsg>> readRawHeartRateSamples(int startEpochMs, int endEpochMs) => unimplemented('readRawHeartRateSamples');

  @override
  Future<List<HeartRateAggBucketMsg>> readHeartRateAggregatedBuckets(int startEpochMs, int endEpochMs, int bucketMs) => unimplemented('readHeartRateAggregatedBuckets');

  @override
  Future<List<HeartRateSummaryMsg>> readDailyHeartRateSummaries(int startEpochMs, int endEpochMs) => unimplemented('readDailyHeartRateSummaries');

  @override
  Future<int?> readRestingHeartRate(int startEpochMs, int endEpochMs) => unimplemented('readRestingHeartRate');

  @override
  Future<List<RestingHeartRateSampleMsg>> readRestingHeartRateSamples(int startEpochMs, int endEpochMs) => unimplemented('readRestingHeartRateSamples');

  @override
  Future<List<DailyRestingHRMsg>> readDailyRestingHR(int startEpochMs, int endEpochMs) => unimplemented('readDailyRestingHR');

  @override
  Future<List<HrvSampleMsg>> readHrvSamples(int startEpochMs, int endEpochMs) => unimplemented('readHrvSamples');

  @override
  Future<List<DailyHrvMsg>> readDailyHRV(int startEpochMs, int endEpochMs) => unimplemented('readDailyHRV');

  @override
  Future<double?> readCaloriesInKcal(int startEpochMs, int endEpochMs) => unimplemented('readCaloriesInKcal');

  @override
  Future<List<DailyNutritionMsg>> readDailyNutrition(int startEpochMs, int endEpochMs, bool includeHydration, bool includeCalories, bool includeEstimatedCalories) => unimplemented('readDailyNutrition');

  @override
  Future<List<DailyMacrosMsg>> readDailyMacros(int startEpochMs, int endEpochMs) => unimplemented('readDailyMacros');

  @override
  Future<List<NutritionEntryMsg>> readNutritionEntries(int startEpochMs, int endEpochMs) => unimplemented('readNutritionEntries');

  @override
  Future<String> writeNutritionEntry(NutritionWriteRequestMsg request) => unimplemented('writeNutritionEntry');

  @override
  Future<String?> deleteNutritionEntry(String id) => unimplemented('deleteNutritionEntry');

  @override
  Future<void> deleteHydrationNutritionEntry(String hydrationClientRecordId) => unimplemented('deleteHydrationNutritionEntry');

  @override
  Future<List<SleepDataMsg>> readSleepSessionsRaw(int startEpochMs, int endEpochMs) => unimplemented('readSleepSessionsRaw');

  @override
  Future<SleepDataMsg?> readSleepSessionById(String id) => unimplemented('readSleepSessionById');

  @override
  Future<List<ExerciseDataMsg>> readExerciseSessions(int startEpochMs, int endEpochMs) => unimplemented('readExerciseSessions');

  @override
  Future<List<ExerciseDataMsg>> readExerciseSessionsWithMetrics(int startEpochMs, int endEpochMs, bool includeDistance, bool includeSpeed) => unimplemented('readExerciseSessionsWithMetrics');

  @override
  Future<ExerciseDataMsg?> readExerciseSessionById(String id) => unimplemented('readExerciseSessionById');

  @override
  Future<ExerciseSessionMetricsMsg> readExerciseSessionMetrics(int startEpochMs, int endEpochMs, List<String> metrics) => unimplemented('readExerciseSessionMetrics');

  @override
  Future<List<SpeedSampleMsg>> readSpeedSamples(int startEpochMs, int endEpochMs) => unimplemented('readSpeedSamples');

  @override
  Future<List<ActivityCadenceSampleMsg>> readActivityCadenceSamples(int startEpochMs, int endEpochMs) => unimplemented('readActivityCadenceSamples');

  @override
  Future<List<PlannedExerciseSessionMsg>> readPlannedExerciseSessions(int startEpochMs, int endEpochMs) => unimplemented('readPlannedExerciseSessions');

  @override
  Future<String> writePlannedExerciseSession(PlannedExerciseWriteRequestMsg request) => unimplemented('writePlannedExerciseSession');

  @override
  Future<String> writeActivityEntry(ActivityWriteRequestMsg request) => unimplemented('writeActivityEntry');

  @override
  Future<List<String>> writeActivityEntries(List<ActivityWriteRequestMsg> requests) => unimplemented('writeActivityEntries');

  @override
  Future<void> updateActivityEntry(String id, ActivityWriteRequestMsg request) => unimplemented('updateActivityEntry');

  @override
  Future<void> deleteActivityEntry(String id) => unimplemented('deleteActivityEntry');

  @override
  Future<List<String>> insertImportedRecords(List<ImportRecordMsg> records) => unimplemented('insertImportedRecords');
}
