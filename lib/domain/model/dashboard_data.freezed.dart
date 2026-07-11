// GENERATED CODE - DO NOT MODIFY BY HAND
// coverage:ignore-file
// ignore_for_file: type=lint
// ignore_for_file: unused_element, deprecated_member_use, deprecated_member_use_from_same_package, use_function_type_syntax_for_parameters, unnecessary_const, avoid_init_to_null, invalid_override_different_default_values_named, prefer_expression_function_bodies, annotate_overrides, invalid_annotation_target, unnecessary_question_mark

part of 'dashboard_data.dart';

// **************************************************************************
// FreezedGenerator
// **************************************************************************

// dart format off
T _$identity<T>(T value) => value;
/// @nodoc
mixin _$DashboardData {

 LocalDate get date; int get steps; double get distanceMeters; double get caloriesKcal; double? get activeCaloriesKcal; double get hydrationLiters; ExerciseData? get workout; List<ExerciseData> get workouts; SleepData? get sleep; SleepScoreEstimate get sleepScore; double? get weightKg; DateTime? get weightTime; double? get heightCm; DateTime? get heightTime; double? get bmi; double? get ffmi; int get avgHeartRateBpm; int get heartRateSampleCount; DateTime? get heartRateSampleStartTime; DateTime? get heartRateSampleEndTime; int get restingHeartRateBpm; int? get restingHeartRateBaselineBpm; double? get hrvRmssdMs; double? get hrvBaselineRmssdMs; int get hrvSampleCount; DateTime? get hrvSampleStartTime; DateTime? get hrvSampleEndTime; double get bodyFatPercent; double? get leanMassKg; double? get bmrKcal; double? get boneMassKg; double? get bodyWaterMassKg; double? get caloriesInKcal; double? get proteinGrams; double? get carbsGrams; double? get fatGrams; double? get caffeineGrams; int? get latestSystolicMmHg; int? get latestDiastolicMmHg; double? get latestSpO2Percent; double? get latestVo2Max; double? get avgRespiratoryRate; double? get latestBodyTemperatureCelsius; double? get latestBloodGlucoseMillimolesPerLiter; double? get latestSkinTemperatureDeltaCelsius; DashboardWeeklyCardioLoad? get weeklyCardioLoad; DashboardWeeklyIntensityMinutes? get weeklyIntensityMinutes; int? get floorsClimbed; double? get elevationGainedMeters; int? get wheelchairPushes; int? get mindfulnessMinutes; int? get menstruationPeriodDays; int? get ovulationTestCount; double? get latestBasalBodyTemperatureCelsius; BodyEnergyTimeline? get bodyEnergyTimeline; Set<String> get missingPermissions; Set<DashboardMetric> get loadedMetrics;/// The metrics the installed provider can serve — i.e. every permission the
/// metric reads is one the device defines. Metrics outside this set get no
/// tile; the rest always get one, empty or not.
 Set<DashboardMetric> get supportedMetrics; Map<DashboardMetric, String> get metricSourcePackages; CaloriesBurnedSource get caloriesKcalSource;
/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DashboardDataCopyWith<DashboardData> get copyWith => _$DashboardDataCopyWithImpl<DashboardData>(this as DashboardData, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DashboardData&&(identical(other.date, date) || other.date == date)&&(identical(other.steps, steps) || other.steps == steps)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters)&&(identical(other.caloriesKcal, caloriesKcal) || other.caloriesKcal == caloriesKcal)&&(identical(other.activeCaloriesKcal, activeCaloriesKcal) || other.activeCaloriesKcal == activeCaloriesKcal)&&(identical(other.hydrationLiters, hydrationLiters) || other.hydrationLiters == hydrationLiters)&&(identical(other.workout, workout) || other.workout == workout)&&const DeepCollectionEquality().equals(other.workouts, workouts)&&(identical(other.sleep, sleep) || other.sleep == sleep)&&(identical(other.sleepScore, sleepScore) || other.sleepScore == sleepScore)&&(identical(other.weightKg, weightKg) || other.weightKg == weightKg)&&(identical(other.weightTime, weightTime) || other.weightTime == weightTime)&&(identical(other.heightCm, heightCm) || other.heightCm == heightCm)&&(identical(other.heightTime, heightTime) || other.heightTime == heightTime)&&(identical(other.bmi, bmi) || other.bmi == bmi)&&(identical(other.ffmi, ffmi) || other.ffmi == ffmi)&&(identical(other.avgHeartRateBpm, avgHeartRateBpm) || other.avgHeartRateBpm == avgHeartRateBpm)&&(identical(other.heartRateSampleCount, heartRateSampleCount) || other.heartRateSampleCount == heartRateSampleCount)&&(identical(other.heartRateSampleStartTime, heartRateSampleStartTime) || other.heartRateSampleStartTime == heartRateSampleStartTime)&&(identical(other.heartRateSampleEndTime, heartRateSampleEndTime) || other.heartRateSampleEndTime == heartRateSampleEndTime)&&(identical(other.restingHeartRateBpm, restingHeartRateBpm) || other.restingHeartRateBpm == restingHeartRateBpm)&&(identical(other.restingHeartRateBaselineBpm, restingHeartRateBaselineBpm) || other.restingHeartRateBaselineBpm == restingHeartRateBaselineBpm)&&(identical(other.hrvRmssdMs, hrvRmssdMs) || other.hrvRmssdMs == hrvRmssdMs)&&(identical(other.hrvBaselineRmssdMs, hrvBaselineRmssdMs) || other.hrvBaselineRmssdMs == hrvBaselineRmssdMs)&&(identical(other.hrvSampleCount, hrvSampleCount) || other.hrvSampleCount == hrvSampleCount)&&(identical(other.hrvSampleStartTime, hrvSampleStartTime) || other.hrvSampleStartTime == hrvSampleStartTime)&&(identical(other.hrvSampleEndTime, hrvSampleEndTime) || other.hrvSampleEndTime == hrvSampleEndTime)&&(identical(other.bodyFatPercent, bodyFatPercent) || other.bodyFatPercent == bodyFatPercent)&&(identical(other.leanMassKg, leanMassKg) || other.leanMassKg == leanMassKg)&&(identical(other.bmrKcal, bmrKcal) || other.bmrKcal == bmrKcal)&&(identical(other.boneMassKg, boneMassKg) || other.boneMassKg == boneMassKg)&&(identical(other.bodyWaterMassKg, bodyWaterMassKg) || other.bodyWaterMassKg == bodyWaterMassKg)&&(identical(other.caloriesInKcal, caloriesInKcal) || other.caloriesInKcal == caloriesInKcal)&&(identical(other.proteinGrams, proteinGrams) || other.proteinGrams == proteinGrams)&&(identical(other.carbsGrams, carbsGrams) || other.carbsGrams == carbsGrams)&&(identical(other.fatGrams, fatGrams) || other.fatGrams == fatGrams)&&(identical(other.caffeineGrams, caffeineGrams) || other.caffeineGrams == caffeineGrams)&&(identical(other.latestSystolicMmHg, latestSystolicMmHg) || other.latestSystolicMmHg == latestSystolicMmHg)&&(identical(other.latestDiastolicMmHg, latestDiastolicMmHg) || other.latestDiastolicMmHg == latestDiastolicMmHg)&&(identical(other.latestSpO2Percent, latestSpO2Percent) || other.latestSpO2Percent == latestSpO2Percent)&&(identical(other.latestVo2Max, latestVo2Max) || other.latestVo2Max == latestVo2Max)&&(identical(other.avgRespiratoryRate, avgRespiratoryRate) || other.avgRespiratoryRate == avgRespiratoryRate)&&(identical(other.latestBodyTemperatureCelsius, latestBodyTemperatureCelsius) || other.latestBodyTemperatureCelsius == latestBodyTemperatureCelsius)&&(identical(other.latestBloodGlucoseMillimolesPerLiter, latestBloodGlucoseMillimolesPerLiter) || other.latestBloodGlucoseMillimolesPerLiter == latestBloodGlucoseMillimolesPerLiter)&&(identical(other.latestSkinTemperatureDeltaCelsius, latestSkinTemperatureDeltaCelsius) || other.latestSkinTemperatureDeltaCelsius == latestSkinTemperatureDeltaCelsius)&&(identical(other.weeklyCardioLoad, weeklyCardioLoad) || other.weeklyCardioLoad == weeklyCardioLoad)&&(identical(other.weeklyIntensityMinutes, weeklyIntensityMinutes) || other.weeklyIntensityMinutes == weeklyIntensityMinutes)&&(identical(other.floorsClimbed, floorsClimbed) || other.floorsClimbed == floorsClimbed)&&(identical(other.elevationGainedMeters, elevationGainedMeters) || other.elevationGainedMeters == elevationGainedMeters)&&(identical(other.wheelchairPushes, wheelchairPushes) || other.wheelchairPushes == wheelchairPushes)&&(identical(other.mindfulnessMinutes, mindfulnessMinutes) || other.mindfulnessMinutes == mindfulnessMinutes)&&(identical(other.menstruationPeriodDays, menstruationPeriodDays) || other.menstruationPeriodDays == menstruationPeriodDays)&&(identical(other.ovulationTestCount, ovulationTestCount) || other.ovulationTestCount == ovulationTestCount)&&(identical(other.latestBasalBodyTemperatureCelsius, latestBasalBodyTemperatureCelsius) || other.latestBasalBodyTemperatureCelsius == latestBasalBodyTemperatureCelsius)&&(identical(other.bodyEnergyTimeline, bodyEnergyTimeline) || other.bodyEnergyTimeline == bodyEnergyTimeline)&&const DeepCollectionEquality().equals(other.missingPermissions, missingPermissions)&&const DeepCollectionEquality().equals(other.loadedMetrics, loadedMetrics)&&const DeepCollectionEquality().equals(other.supportedMetrics, supportedMetrics)&&const DeepCollectionEquality().equals(other.metricSourcePackages, metricSourcePackages)&&(identical(other.caloriesKcalSource, caloriesKcalSource) || other.caloriesKcalSource == caloriesKcalSource));
}


@override
int get hashCode => Object.hashAll([runtimeType,date,steps,distanceMeters,caloriesKcal,activeCaloriesKcal,hydrationLiters,workout,const DeepCollectionEquality().hash(workouts),sleep,sleepScore,weightKg,weightTime,heightCm,heightTime,bmi,ffmi,avgHeartRateBpm,heartRateSampleCount,heartRateSampleStartTime,heartRateSampleEndTime,restingHeartRateBpm,restingHeartRateBaselineBpm,hrvRmssdMs,hrvBaselineRmssdMs,hrvSampleCount,hrvSampleStartTime,hrvSampleEndTime,bodyFatPercent,leanMassKg,bmrKcal,boneMassKg,bodyWaterMassKg,caloriesInKcal,proteinGrams,carbsGrams,fatGrams,caffeineGrams,latestSystolicMmHg,latestDiastolicMmHg,latestSpO2Percent,latestVo2Max,avgRespiratoryRate,latestBodyTemperatureCelsius,latestBloodGlucoseMillimolesPerLiter,latestSkinTemperatureDeltaCelsius,weeklyCardioLoad,weeklyIntensityMinutes,floorsClimbed,elevationGainedMeters,wheelchairPushes,mindfulnessMinutes,menstruationPeriodDays,ovulationTestCount,latestBasalBodyTemperatureCelsius,bodyEnergyTimeline,const DeepCollectionEquality().hash(missingPermissions),const DeepCollectionEquality().hash(loadedMetrics),const DeepCollectionEquality().hash(supportedMetrics),const DeepCollectionEquality().hash(metricSourcePackages),caloriesKcalSource]);

@override
String toString() {
  return 'DashboardData(date: $date, steps: $steps, distanceMeters: $distanceMeters, caloriesKcal: $caloriesKcal, activeCaloriesKcal: $activeCaloriesKcal, hydrationLiters: $hydrationLiters, workout: $workout, workouts: $workouts, sleep: $sleep, sleepScore: $sleepScore, weightKg: $weightKg, weightTime: $weightTime, heightCm: $heightCm, heightTime: $heightTime, bmi: $bmi, ffmi: $ffmi, avgHeartRateBpm: $avgHeartRateBpm, heartRateSampleCount: $heartRateSampleCount, heartRateSampleStartTime: $heartRateSampleStartTime, heartRateSampleEndTime: $heartRateSampleEndTime, restingHeartRateBpm: $restingHeartRateBpm, restingHeartRateBaselineBpm: $restingHeartRateBaselineBpm, hrvRmssdMs: $hrvRmssdMs, hrvBaselineRmssdMs: $hrvBaselineRmssdMs, hrvSampleCount: $hrvSampleCount, hrvSampleStartTime: $hrvSampleStartTime, hrvSampleEndTime: $hrvSampleEndTime, bodyFatPercent: $bodyFatPercent, leanMassKg: $leanMassKg, bmrKcal: $bmrKcal, boneMassKg: $boneMassKg, bodyWaterMassKg: $bodyWaterMassKg, caloriesInKcal: $caloriesInKcal, proteinGrams: $proteinGrams, carbsGrams: $carbsGrams, fatGrams: $fatGrams, caffeineGrams: $caffeineGrams, latestSystolicMmHg: $latestSystolicMmHg, latestDiastolicMmHg: $latestDiastolicMmHg, latestSpO2Percent: $latestSpO2Percent, latestVo2Max: $latestVo2Max, avgRespiratoryRate: $avgRespiratoryRate, latestBodyTemperatureCelsius: $latestBodyTemperatureCelsius, latestBloodGlucoseMillimolesPerLiter: $latestBloodGlucoseMillimolesPerLiter, latestSkinTemperatureDeltaCelsius: $latestSkinTemperatureDeltaCelsius, weeklyCardioLoad: $weeklyCardioLoad, weeklyIntensityMinutes: $weeklyIntensityMinutes, floorsClimbed: $floorsClimbed, elevationGainedMeters: $elevationGainedMeters, wheelchairPushes: $wheelchairPushes, mindfulnessMinutes: $mindfulnessMinutes, menstruationPeriodDays: $menstruationPeriodDays, ovulationTestCount: $ovulationTestCount, latestBasalBodyTemperatureCelsius: $latestBasalBodyTemperatureCelsius, bodyEnergyTimeline: $bodyEnergyTimeline, missingPermissions: $missingPermissions, loadedMetrics: $loadedMetrics, supportedMetrics: $supportedMetrics, metricSourcePackages: $metricSourcePackages, caloriesKcalSource: $caloriesKcalSource)';
}


}

/// @nodoc
abstract mixin class $DashboardDataCopyWith<$Res>  {
  factory $DashboardDataCopyWith(DashboardData value, $Res Function(DashboardData) _then) = _$DashboardDataCopyWithImpl;
@useResult
$Res call({
 LocalDate date, int steps, double distanceMeters, double caloriesKcal, double? activeCaloriesKcal, double hydrationLiters, ExerciseData? workout, List<ExerciseData> workouts, SleepData? sleep, SleepScoreEstimate sleepScore, double? weightKg, DateTime? weightTime, double? heightCm, DateTime? heightTime, double? bmi, double? ffmi, int avgHeartRateBpm, int heartRateSampleCount, DateTime? heartRateSampleStartTime, DateTime? heartRateSampleEndTime, int restingHeartRateBpm, int? restingHeartRateBaselineBpm, double? hrvRmssdMs, double? hrvBaselineRmssdMs, int hrvSampleCount, DateTime? hrvSampleStartTime, DateTime? hrvSampleEndTime, double bodyFatPercent, double? leanMassKg, double? bmrKcal, double? boneMassKg, double? bodyWaterMassKg, double? caloriesInKcal, double? proteinGrams, double? carbsGrams, double? fatGrams, double? caffeineGrams, int? latestSystolicMmHg, int? latestDiastolicMmHg, double? latestSpO2Percent, double? latestVo2Max, double? avgRespiratoryRate, double? latestBodyTemperatureCelsius, double? latestBloodGlucoseMillimolesPerLiter, double? latestSkinTemperatureDeltaCelsius, DashboardWeeklyCardioLoad? weeklyCardioLoad, DashboardWeeklyIntensityMinutes? weeklyIntensityMinutes, int? floorsClimbed, double? elevationGainedMeters, int? wheelchairPushes, int? mindfulnessMinutes, int? menstruationPeriodDays, int? ovulationTestCount, double? latestBasalBodyTemperatureCelsius, BodyEnergyTimeline? bodyEnergyTimeline, Set<String> missingPermissions, Set<DashboardMetric> loadedMetrics, Set<DashboardMetric> supportedMetrics, Map<DashboardMetric, String> metricSourcePackages, CaloriesBurnedSource caloriesKcalSource
});


$ExerciseDataCopyWith<$Res>? get workout;$SleepDataCopyWith<$Res>? get sleep;$SleepScoreEstimateCopyWith<$Res> get sleepScore;$DashboardWeeklyCardioLoadCopyWith<$Res>? get weeklyCardioLoad;$DashboardWeeklyIntensityMinutesCopyWith<$Res>? get weeklyIntensityMinutes;$BodyEnergyTimelineCopyWith<$Res>? get bodyEnergyTimeline;

}
/// @nodoc
class _$DashboardDataCopyWithImpl<$Res>
    implements $DashboardDataCopyWith<$Res> {
  _$DashboardDataCopyWithImpl(this._self, this._then);

  final DashboardData _self;
  final $Res Function(DashboardData) _then;

/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? date = null,Object? steps = null,Object? distanceMeters = null,Object? caloriesKcal = null,Object? activeCaloriesKcal = freezed,Object? hydrationLiters = null,Object? workout = freezed,Object? workouts = null,Object? sleep = freezed,Object? sleepScore = null,Object? weightKg = freezed,Object? weightTime = freezed,Object? heightCm = freezed,Object? heightTime = freezed,Object? bmi = freezed,Object? ffmi = freezed,Object? avgHeartRateBpm = null,Object? heartRateSampleCount = null,Object? heartRateSampleStartTime = freezed,Object? heartRateSampleEndTime = freezed,Object? restingHeartRateBpm = null,Object? restingHeartRateBaselineBpm = freezed,Object? hrvRmssdMs = freezed,Object? hrvBaselineRmssdMs = freezed,Object? hrvSampleCount = null,Object? hrvSampleStartTime = freezed,Object? hrvSampleEndTime = freezed,Object? bodyFatPercent = null,Object? leanMassKg = freezed,Object? bmrKcal = freezed,Object? boneMassKg = freezed,Object? bodyWaterMassKg = freezed,Object? caloriesInKcal = freezed,Object? proteinGrams = freezed,Object? carbsGrams = freezed,Object? fatGrams = freezed,Object? caffeineGrams = freezed,Object? latestSystolicMmHg = freezed,Object? latestDiastolicMmHg = freezed,Object? latestSpO2Percent = freezed,Object? latestVo2Max = freezed,Object? avgRespiratoryRate = freezed,Object? latestBodyTemperatureCelsius = freezed,Object? latestBloodGlucoseMillimolesPerLiter = freezed,Object? latestSkinTemperatureDeltaCelsius = freezed,Object? weeklyCardioLoad = freezed,Object? weeklyIntensityMinutes = freezed,Object? floorsClimbed = freezed,Object? elevationGainedMeters = freezed,Object? wheelchairPushes = freezed,Object? mindfulnessMinutes = freezed,Object? menstruationPeriodDays = freezed,Object? ovulationTestCount = freezed,Object? latestBasalBodyTemperatureCelsius = freezed,Object? bodyEnergyTimeline = freezed,Object? missingPermissions = null,Object? loadedMetrics = null,Object? supportedMetrics = null,Object? metricSourcePackages = null,Object? caloriesKcalSource = null,}) {
  return _then(_self.copyWith(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,steps: null == steps ? _self.steps : steps // ignore: cast_nullable_to_non_nullable
as int,distanceMeters: null == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double,caloriesKcal: null == caloriesKcal ? _self.caloriesKcal : caloriesKcal // ignore: cast_nullable_to_non_nullable
as double,activeCaloriesKcal: freezed == activeCaloriesKcal ? _self.activeCaloriesKcal : activeCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,hydrationLiters: null == hydrationLiters ? _self.hydrationLiters : hydrationLiters // ignore: cast_nullable_to_non_nullable
as double,workout: freezed == workout ? _self.workout : workout // ignore: cast_nullable_to_non_nullable
as ExerciseData?,workouts: null == workouts ? _self.workouts : workouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,sleep: freezed == sleep ? _self.sleep : sleep // ignore: cast_nullable_to_non_nullable
as SleepData?,sleepScore: null == sleepScore ? _self.sleepScore : sleepScore // ignore: cast_nullable_to_non_nullable
as SleepScoreEstimate,weightKg: freezed == weightKg ? _self.weightKg : weightKg // ignore: cast_nullable_to_non_nullable
as double?,weightTime: freezed == weightTime ? _self.weightTime : weightTime // ignore: cast_nullable_to_non_nullable
as DateTime?,heightCm: freezed == heightCm ? _self.heightCm : heightCm // ignore: cast_nullable_to_non_nullable
as double?,heightTime: freezed == heightTime ? _self.heightTime : heightTime // ignore: cast_nullable_to_non_nullable
as DateTime?,bmi: freezed == bmi ? _self.bmi : bmi // ignore: cast_nullable_to_non_nullable
as double?,ffmi: freezed == ffmi ? _self.ffmi : ffmi // ignore: cast_nullable_to_non_nullable
as double?,avgHeartRateBpm: null == avgHeartRateBpm ? _self.avgHeartRateBpm : avgHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int,heartRateSampleCount: null == heartRateSampleCount ? _self.heartRateSampleCount : heartRateSampleCount // ignore: cast_nullable_to_non_nullable
as int,heartRateSampleStartTime: freezed == heartRateSampleStartTime ? _self.heartRateSampleStartTime : heartRateSampleStartTime // ignore: cast_nullable_to_non_nullable
as DateTime?,heartRateSampleEndTime: freezed == heartRateSampleEndTime ? _self.heartRateSampleEndTime : heartRateSampleEndTime // ignore: cast_nullable_to_non_nullable
as DateTime?,restingHeartRateBpm: null == restingHeartRateBpm ? _self.restingHeartRateBpm : restingHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int,restingHeartRateBaselineBpm: freezed == restingHeartRateBaselineBpm ? _self.restingHeartRateBaselineBpm : restingHeartRateBaselineBpm // ignore: cast_nullable_to_non_nullable
as int?,hrvRmssdMs: freezed == hrvRmssdMs ? _self.hrvRmssdMs : hrvRmssdMs // ignore: cast_nullable_to_non_nullable
as double?,hrvBaselineRmssdMs: freezed == hrvBaselineRmssdMs ? _self.hrvBaselineRmssdMs : hrvBaselineRmssdMs // ignore: cast_nullable_to_non_nullable
as double?,hrvSampleCount: null == hrvSampleCount ? _self.hrvSampleCount : hrvSampleCount // ignore: cast_nullable_to_non_nullable
as int,hrvSampleStartTime: freezed == hrvSampleStartTime ? _self.hrvSampleStartTime : hrvSampleStartTime // ignore: cast_nullable_to_non_nullable
as DateTime?,hrvSampleEndTime: freezed == hrvSampleEndTime ? _self.hrvSampleEndTime : hrvSampleEndTime // ignore: cast_nullable_to_non_nullable
as DateTime?,bodyFatPercent: null == bodyFatPercent ? _self.bodyFatPercent : bodyFatPercent // ignore: cast_nullable_to_non_nullable
as double,leanMassKg: freezed == leanMassKg ? _self.leanMassKg : leanMassKg // ignore: cast_nullable_to_non_nullable
as double?,bmrKcal: freezed == bmrKcal ? _self.bmrKcal : bmrKcal // ignore: cast_nullable_to_non_nullable
as double?,boneMassKg: freezed == boneMassKg ? _self.boneMassKg : boneMassKg // ignore: cast_nullable_to_non_nullable
as double?,bodyWaterMassKg: freezed == bodyWaterMassKg ? _self.bodyWaterMassKg : bodyWaterMassKg // ignore: cast_nullable_to_non_nullable
as double?,caloriesInKcal: freezed == caloriesInKcal ? _self.caloriesInKcal : caloriesInKcal // ignore: cast_nullable_to_non_nullable
as double?,proteinGrams: freezed == proteinGrams ? _self.proteinGrams : proteinGrams // ignore: cast_nullable_to_non_nullable
as double?,carbsGrams: freezed == carbsGrams ? _self.carbsGrams : carbsGrams // ignore: cast_nullable_to_non_nullable
as double?,fatGrams: freezed == fatGrams ? _self.fatGrams : fatGrams // ignore: cast_nullable_to_non_nullable
as double?,caffeineGrams: freezed == caffeineGrams ? _self.caffeineGrams : caffeineGrams // ignore: cast_nullable_to_non_nullable
as double?,latestSystolicMmHg: freezed == latestSystolicMmHg ? _self.latestSystolicMmHg : latestSystolicMmHg // ignore: cast_nullable_to_non_nullable
as int?,latestDiastolicMmHg: freezed == latestDiastolicMmHg ? _self.latestDiastolicMmHg : latestDiastolicMmHg // ignore: cast_nullable_to_non_nullable
as int?,latestSpO2Percent: freezed == latestSpO2Percent ? _self.latestSpO2Percent : latestSpO2Percent // ignore: cast_nullable_to_non_nullable
as double?,latestVo2Max: freezed == latestVo2Max ? _self.latestVo2Max : latestVo2Max // ignore: cast_nullable_to_non_nullable
as double?,avgRespiratoryRate: freezed == avgRespiratoryRate ? _self.avgRespiratoryRate : avgRespiratoryRate // ignore: cast_nullable_to_non_nullable
as double?,latestBodyTemperatureCelsius: freezed == latestBodyTemperatureCelsius ? _self.latestBodyTemperatureCelsius : latestBodyTemperatureCelsius // ignore: cast_nullable_to_non_nullable
as double?,latestBloodGlucoseMillimolesPerLiter: freezed == latestBloodGlucoseMillimolesPerLiter ? _self.latestBloodGlucoseMillimolesPerLiter : latestBloodGlucoseMillimolesPerLiter // ignore: cast_nullable_to_non_nullable
as double?,latestSkinTemperatureDeltaCelsius: freezed == latestSkinTemperatureDeltaCelsius ? _self.latestSkinTemperatureDeltaCelsius : latestSkinTemperatureDeltaCelsius // ignore: cast_nullable_to_non_nullable
as double?,weeklyCardioLoad: freezed == weeklyCardioLoad ? _self.weeklyCardioLoad : weeklyCardioLoad // ignore: cast_nullable_to_non_nullable
as DashboardWeeklyCardioLoad?,weeklyIntensityMinutes: freezed == weeklyIntensityMinutes ? _self.weeklyIntensityMinutes : weeklyIntensityMinutes // ignore: cast_nullable_to_non_nullable
as DashboardWeeklyIntensityMinutes?,floorsClimbed: freezed == floorsClimbed ? _self.floorsClimbed : floorsClimbed // ignore: cast_nullable_to_non_nullable
as int?,elevationGainedMeters: freezed == elevationGainedMeters ? _self.elevationGainedMeters : elevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double?,wheelchairPushes: freezed == wheelchairPushes ? _self.wheelchairPushes : wheelchairPushes // ignore: cast_nullable_to_non_nullable
as int?,mindfulnessMinutes: freezed == mindfulnessMinutes ? _self.mindfulnessMinutes : mindfulnessMinutes // ignore: cast_nullable_to_non_nullable
as int?,menstruationPeriodDays: freezed == menstruationPeriodDays ? _self.menstruationPeriodDays : menstruationPeriodDays // ignore: cast_nullable_to_non_nullable
as int?,ovulationTestCount: freezed == ovulationTestCount ? _self.ovulationTestCount : ovulationTestCount // ignore: cast_nullable_to_non_nullable
as int?,latestBasalBodyTemperatureCelsius: freezed == latestBasalBodyTemperatureCelsius ? _self.latestBasalBodyTemperatureCelsius : latestBasalBodyTemperatureCelsius // ignore: cast_nullable_to_non_nullable
as double?,bodyEnergyTimeline: freezed == bodyEnergyTimeline ? _self.bodyEnergyTimeline : bodyEnergyTimeline // ignore: cast_nullable_to_non_nullable
as BodyEnergyTimeline?,missingPermissions: null == missingPermissions ? _self.missingPermissions : missingPermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,loadedMetrics: null == loadedMetrics ? _self.loadedMetrics : loadedMetrics // ignore: cast_nullable_to_non_nullable
as Set<DashboardMetric>,supportedMetrics: null == supportedMetrics ? _self.supportedMetrics : supportedMetrics // ignore: cast_nullable_to_non_nullable
as Set<DashboardMetric>,metricSourcePackages: null == metricSourcePackages ? _self.metricSourcePackages : metricSourcePackages // ignore: cast_nullable_to_non_nullable
as Map<DashboardMetric, String>,caloriesKcalSource: null == caloriesKcalSource ? _self.caloriesKcalSource : caloriesKcalSource // ignore: cast_nullable_to_non_nullable
as CaloriesBurnedSource,
  ));
}
/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ExerciseDataCopyWith<$Res>? get workout {
    if (_self.workout == null) {
    return null;
  }

  return $ExerciseDataCopyWith<$Res>(_self.workout!, (value) {
    return _then(_self.copyWith(workout: value));
  });
}/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDataCopyWith<$Res>? get sleep {
    if (_self.sleep == null) {
    return null;
  }

  return $SleepDataCopyWith<$Res>(_self.sleep!, (value) {
    return _then(_self.copyWith(sleep: value));
  });
}/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepScoreEstimateCopyWith<$Res> get sleepScore {
  
  return $SleepScoreEstimateCopyWith<$Res>(_self.sleepScore, (value) {
    return _then(_self.copyWith(sleepScore: value));
  });
}/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DashboardWeeklyCardioLoadCopyWith<$Res>? get weeklyCardioLoad {
    if (_self.weeklyCardioLoad == null) {
    return null;
  }

  return $DashboardWeeklyCardioLoadCopyWith<$Res>(_self.weeklyCardioLoad!, (value) {
    return _then(_self.copyWith(weeklyCardioLoad: value));
  });
}/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DashboardWeeklyIntensityMinutesCopyWith<$Res>? get weeklyIntensityMinutes {
    if (_self.weeklyIntensityMinutes == null) {
    return null;
  }

  return $DashboardWeeklyIntensityMinutesCopyWith<$Res>(_self.weeklyIntensityMinutes!, (value) {
    return _then(_self.copyWith(weeklyIntensityMinutes: value));
  });
}/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BodyEnergyTimelineCopyWith<$Res>? get bodyEnergyTimeline {
    if (_self.bodyEnergyTimeline == null) {
    return null;
  }

  return $BodyEnergyTimelineCopyWith<$Res>(_self.bodyEnergyTimeline!, (value) {
    return _then(_self.copyWith(bodyEnergyTimeline: value));
  });
}
}


/// Adds pattern-matching-related methods to [DashboardData].
extension DashboardDataPatterns on DashboardData {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DashboardData value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DashboardData() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DashboardData value)  $default,){
final _that = this;
switch (_that) {
case _DashboardData():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DashboardData value)?  $default,){
final _that = this;
switch (_that) {
case _DashboardData() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( LocalDate date,  int steps,  double distanceMeters,  double caloriesKcal,  double? activeCaloriesKcal,  double hydrationLiters,  ExerciseData? workout,  List<ExerciseData> workouts,  SleepData? sleep,  SleepScoreEstimate sleepScore,  double? weightKg,  DateTime? weightTime,  double? heightCm,  DateTime? heightTime,  double? bmi,  double? ffmi,  int avgHeartRateBpm,  int heartRateSampleCount,  DateTime? heartRateSampleStartTime,  DateTime? heartRateSampleEndTime,  int restingHeartRateBpm,  int? restingHeartRateBaselineBpm,  double? hrvRmssdMs,  double? hrvBaselineRmssdMs,  int hrvSampleCount,  DateTime? hrvSampleStartTime,  DateTime? hrvSampleEndTime,  double bodyFatPercent,  double? leanMassKg,  double? bmrKcal,  double? boneMassKg,  double? bodyWaterMassKg,  double? caloriesInKcal,  double? proteinGrams,  double? carbsGrams,  double? fatGrams,  double? caffeineGrams,  int? latestSystolicMmHg,  int? latestDiastolicMmHg,  double? latestSpO2Percent,  double? latestVo2Max,  double? avgRespiratoryRate,  double? latestBodyTemperatureCelsius,  double? latestBloodGlucoseMillimolesPerLiter,  double? latestSkinTemperatureDeltaCelsius,  DashboardWeeklyCardioLoad? weeklyCardioLoad,  DashboardWeeklyIntensityMinutes? weeklyIntensityMinutes,  int? floorsClimbed,  double? elevationGainedMeters,  int? wheelchairPushes,  int? mindfulnessMinutes,  int? menstruationPeriodDays,  int? ovulationTestCount,  double? latestBasalBodyTemperatureCelsius,  BodyEnergyTimeline? bodyEnergyTimeline,  Set<String> missingPermissions,  Set<DashboardMetric> loadedMetrics,  Set<DashboardMetric> supportedMetrics,  Map<DashboardMetric, String> metricSourcePackages,  CaloriesBurnedSource caloriesKcalSource)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DashboardData() when $default != null:
return $default(_that.date,_that.steps,_that.distanceMeters,_that.caloriesKcal,_that.activeCaloriesKcal,_that.hydrationLiters,_that.workout,_that.workouts,_that.sleep,_that.sleepScore,_that.weightKg,_that.weightTime,_that.heightCm,_that.heightTime,_that.bmi,_that.ffmi,_that.avgHeartRateBpm,_that.heartRateSampleCount,_that.heartRateSampleStartTime,_that.heartRateSampleEndTime,_that.restingHeartRateBpm,_that.restingHeartRateBaselineBpm,_that.hrvRmssdMs,_that.hrvBaselineRmssdMs,_that.hrvSampleCount,_that.hrvSampleStartTime,_that.hrvSampleEndTime,_that.bodyFatPercent,_that.leanMassKg,_that.bmrKcal,_that.boneMassKg,_that.bodyWaterMassKg,_that.caloriesInKcal,_that.proteinGrams,_that.carbsGrams,_that.fatGrams,_that.caffeineGrams,_that.latestSystolicMmHg,_that.latestDiastolicMmHg,_that.latestSpO2Percent,_that.latestVo2Max,_that.avgRespiratoryRate,_that.latestBodyTemperatureCelsius,_that.latestBloodGlucoseMillimolesPerLiter,_that.latestSkinTemperatureDeltaCelsius,_that.weeklyCardioLoad,_that.weeklyIntensityMinutes,_that.floorsClimbed,_that.elevationGainedMeters,_that.wheelchairPushes,_that.mindfulnessMinutes,_that.menstruationPeriodDays,_that.ovulationTestCount,_that.latestBasalBodyTemperatureCelsius,_that.bodyEnergyTimeline,_that.missingPermissions,_that.loadedMetrics,_that.supportedMetrics,_that.metricSourcePackages,_that.caloriesKcalSource);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( LocalDate date,  int steps,  double distanceMeters,  double caloriesKcal,  double? activeCaloriesKcal,  double hydrationLiters,  ExerciseData? workout,  List<ExerciseData> workouts,  SleepData? sleep,  SleepScoreEstimate sleepScore,  double? weightKg,  DateTime? weightTime,  double? heightCm,  DateTime? heightTime,  double? bmi,  double? ffmi,  int avgHeartRateBpm,  int heartRateSampleCount,  DateTime? heartRateSampleStartTime,  DateTime? heartRateSampleEndTime,  int restingHeartRateBpm,  int? restingHeartRateBaselineBpm,  double? hrvRmssdMs,  double? hrvBaselineRmssdMs,  int hrvSampleCount,  DateTime? hrvSampleStartTime,  DateTime? hrvSampleEndTime,  double bodyFatPercent,  double? leanMassKg,  double? bmrKcal,  double? boneMassKg,  double? bodyWaterMassKg,  double? caloriesInKcal,  double? proteinGrams,  double? carbsGrams,  double? fatGrams,  double? caffeineGrams,  int? latestSystolicMmHg,  int? latestDiastolicMmHg,  double? latestSpO2Percent,  double? latestVo2Max,  double? avgRespiratoryRate,  double? latestBodyTemperatureCelsius,  double? latestBloodGlucoseMillimolesPerLiter,  double? latestSkinTemperatureDeltaCelsius,  DashboardWeeklyCardioLoad? weeklyCardioLoad,  DashboardWeeklyIntensityMinutes? weeklyIntensityMinutes,  int? floorsClimbed,  double? elevationGainedMeters,  int? wheelchairPushes,  int? mindfulnessMinutes,  int? menstruationPeriodDays,  int? ovulationTestCount,  double? latestBasalBodyTemperatureCelsius,  BodyEnergyTimeline? bodyEnergyTimeline,  Set<String> missingPermissions,  Set<DashboardMetric> loadedMetrics,  Set<DashboardMetric> supportedMetrics,  Map<DashboardMetric, String> metricSourcePackages,  CaloriesBurnedSource caloriesKcalSource)  $default,) {final _that = this;
switch (_that) {
case _DashboardData():
return $default(_that.date,_that.steps,_that.distanceMeters,_that.caloriesKcal,_that.activeCaloriesKcal,_that.hydrationLiters,_that.workout,_that.workouts,_that.sleep,_that.sleepScore,_that.weightKg,_that.weightTime,_that.heightCm,_that.heightTime,_that.bmi,_that.ffmi,_that.avgHeartRateBpm,_that.heartRateSampleCount,_that.heartRateSampleStartTime,_that.heartRateSampleEndTime,_that.restingHeartRateBpm,_that.restingHeartRateBaselineBpm,_that.hrvRmssdMs,_that.hrvBaselineRmssdMs,_that.hrvSampleCount,_that.hrvSampleStartTime,_that.hrvSampleEndTime,_that.bodyFatPercent,_that.leanMassKg,_that.bmrKcal,_that.boneMassKg,_that.bodyWaterMassKg,_that.caloriesInKcal,_that.proteinGrams,_that.carbsGrams,_that.fatGrams,_that.caffeineGrams,_that.latestSystolicMmHg,_that.latestDiastolicMmHg,_that.latestSpO2Percent,_that.latestVo2Max,_that.avgRespiratoryRate,_that.latestBodyTemperatureCelsius,_that.latestBloodGlucoseMillimolesPerLiter,_that.latestSkinTemperatureDeltaCelsius,_that.weeklyCardioLoad,_that.weeklyIntensityMinutes,_that.floorsClimbed,_that.elevationGainedMeters,_that.wheelchairPushes,_that.mindfulnessMinutes,_that.menstruationPeriodDays,_that.ovulationTestCount,_that.latestBasalBodyTemperatureCelsius,_that.bodyEnergyTimeline,_that.missingPermissions,_that.loadedMetrics,_that.supportedMetrics,_that.metricSourcePackages,_that.caloriesKcalSource);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( LocalDate date,  int steps,  double distanceMeters,  double caloriesKcal,  double? activeCaloriesKcal,  double hydrationLiters,  ExerciseData? workout,  List<ExerciseData> workouts,  SleepData? sleep,  SleepScoreEstimate sleepScore,  double? weightKg,  DateTime? weightTime,  double? heightCm,  DateTime? heightTime,  double? bmi,  double? ffmi,  int avgHeartRateBpm,  int heartRateSampleCount,  DateTime? heartRateSampleStartTime,  DateTime? heartRateSampleEndTime,  int restingHeartRateBpm,  int? restingHeartRateBaselineBpm,  double? hrvRmssdMs,  double? hrvBaselineRmssdMs,  int hrvSampleCount,  DateTime? hrvSampleStartTime,  DateTime? hrvSampleEndTime,  double bodyFatPercent,  double? leanMassKg,  double? bmrKcal,  double? boneMassKg,  double? bodyWaterMassKg,  double? caloriesInKcal,  double? proteinGrams,  double? carbsGrams,  double? fatGrams,  double? caffeineGrams,  int? latestSystolicMmHg,  int? latestDiastolicMmHg,  double? latestSpO2Percent,  double? latestVo2Max,  double? avgRespiratoryRate,  double? latestBodyTemperatureCelsius,  double? latestBloodGlucoseMillimolesPerLiter,  double? latestSkinTemperatureDeltaCelsius,  DashboardWeeklyCardioLoad? weeklyCardioLoad,  DashboardWeeklyIntensityMinutes? weeklyIntensityMinutes,  int? floorsClimbed,  double? elevationGainedMeters,  int? wheelchairPushes,  int? mindfulnessMinutes,  int? menstruationPeriodDays,  int? ovulationTestCount,  double? latestBasalBodyTemperatureCelsius,  BodyEnergyTimeline? bodyEnergyTimeline,  Set<String> missingPermissions,  Set<DashboardMetric> loadedMetrics,  Set<DashboardMetric> supportedMetrics,  Map<DashboardMetric, String> metricSourcePackages,  CaloriesBurnedSource caloriesKcalSource)?  $default,) {final _that = this;
switch (_that) {
case _DashboardData() when $default != null:
return $default(_that.date,_that.steps,_that.distanceMeters,_that.caloriesKcal,_that.activeCaloriesKcal,_that.hydrationLiters,_that.workout,_that.workouts,_that.sleep,_that.sleepScore,_that.weightKg,_that.weightTime,_that.heightCm,_that.heightTime,_that.bmi,_that.ffmi,_that.avgHeartRateBpm,_that.heartRateSampleCount,_that.heartRateSampleStartTime,_that.heartRateSampleEndTime,_that.restingHeartRateBpm,_that.restingHeartRateBaselineBpm,_that.hrvRmssdMs,_that.hrvBaselineRmssdMs,_that.hrvSampleCount,_that.hrvSampleStartTime,_that.hrvSampleEndTime,_that.bodyFatPercent,_that.leanMassKg,_that.bmrKcal,_that.boneMassKg,_that.bodyWaterMassKg,_that.caloriesInKcal,_that.proteinGrams,_that.carbsGrams,_that.fatGrams,_that.caffeineGrams,_that.latestSystolicMmHg,_that.latestDiastolicMmHg,_that.latestSpO2Percent,_that.latestVo2Max,_that.avgRespiratoryRate,_that.latestBodyTemperatureCelsius,_that.latestBloodGlucoseMillimolesPerLiter,_that.latestSkinTemperatureDeltaCelsius,_that.weeklyCardioLoad,_that.weeklyIntensityMinutes,_that.floorsClimbed,_that.elevationGainedMeters,_that.wheelchairPushes,_that.mindfulnessMinutes,_that.menstruationPeriodDays,_that.ovulationTestCount,_that.latestBasalBodyTemperatureCelsius,_that.bodyEnergyTimeline,_that.missingPermissions,_that.loadedMetrics,_that.supportedMetrics,_that.metricSourcePackages,_that.caloriesKcalSource);case _:
  return null;

}
}

}

/// @nodoc


class _DashboardData implements DashboardData {
  const _DashboardData({required this.date, this.steps = 0, this.distanceMeters = 0.0, this.caloriesKcal = 0.0, this.activeCaloriesKcal, this.hydrationLiters = 0.0, this.workout, final  List<ExerciseData> workouts = const <ExerciseData>[], this.sleep, this.sleepScore = SleepScoreEstimate.noData, this.weightKg, this.weightTime, this.heightCm, this.heightTime, this.bmi, this.ffmi, this.avgHeartRateBpm = 0, this.heartRateSampleCount = 0, this.heartRateSampleStartTime, this.heartRateSampleEndTime, this.restingHeartRateBpm = 0, this.restingHeartRateBaselineBpm, this.hrvRmssdMs, this.hrvBaselineRmssdMs, this.hrvSampleCount = 0, this.hrvSampleStartTime, this.hrvSampleEndTime, this.bodyFatPercent = 0.0, this.leanMassKg, this.bmrKcal, this.boneMassKg, this.bodyWaterMassKg, this.caloriesInKcal, this.proteinGrams, this.carbsGrams, this.fatGrams, this.caffeineGrams, this.latestSystolicMmHg, this.latestDiastolicMmHg, this.latestSpO2Percent, this.latestVo2Max, this.avgRespiratoryRate, this.latestBodyTemperatureCelsius, this.latestBloodGlucoseMillimolesPerLiter, this.latestSkinTemperatureDeltaCelsius, this.weeklyCardioLoad, this.weeklyIntensityMinutes, this.floorsClimbed, this.elevationGainedMeters, this.wheelchairPushes, this.mindfulnessMinutes, this.menstruationPeriodDays, this.ovulationTestCount, this.latestBasalBodyTemperatureCelsius, this.bodyEnergyTimeline, final  Set<String> missingPermissions = const <String>{}, final  Set<DashboardMetric> loadedMetrics = const <DashboardMetric>{}, final  Set<DashboardMetric> supportedMetrics = const <DashboardMetric>{}, final  Map<DashboardMetric, String> metricSourcePackages = const <DashboardMetric, String>{}, this.caloriesKcalSource = CaloriesBurnedSource.noData}): _workouts = workouts,_missingPermissions = missingPermissions,_loadedMetrics = loadedMetrics,_supportedMetrics = supportedMetrics,_metricSourcePackages = metricSourcePackages;
  

@override final  LocalDate date;
@override@JsonKey() final  int steps;
@override@JsonKey() final  double distanceMeters;
@override@JsonKey() final  double caloriesKcal;
@override final  double? activeCaloriesKcal;
@override@JsonKey() final  double hydrationLiters;
@override final  ExerciseData? workout;
 final  List<ExerciseData> _workouts;
@override@JsonKey() List<ExerciseData> get workouts {
  if (_workouts is EqualUnmodifiableListView) return _workouts;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableListView(_workouts);
}

@override final  SleepData? sleep;
@override@JsonKey() final  SleepScoreEstimate sleepScore;
@override final  double? weightKg;
@override final  DateTime? weightTime;
@override final  double? heightCm;
@override final  DateTime? heightTime;
@override final  double? bmi;
@override final  double? ffmi;
@override@JsonKey() final  int avgHeartRateBpm;
@override@JsonKey() final  int heartRateSampleCount;
@override final  DateTime? heartRateSampleStartTime;
@override final  DateTime? heartRateSampleEndTime;
@override@JsonKey() final  int restingHeartRateBpm;
@override final  int? restingHeartRateBaselineBpm;
@override final  double? hrvRmssdMs;
@override final  double? hrvBaselineRmssdMs;
@override@JsonKey() final  int hrvSampleCount;
@override final  DateTime? hrvSampleStartTime;
@override final  DateTime? hrvSampleEndTime;
@override@JsonKey() final  double bodyFatPercent;
@override final  double? leanMassKg;
@override final  double? bmrKcal;
@override final  double? boneMassKg;
@override final  double? bodyWaterMassKg;
@override final  double? caloriesInKcal;
@override final  double? proteinGrams;
@override final  double? carbsGrams;
@override final  double? fatGrams;
@override final  double? caffeineGrams;
@override final  int? latestSystolicMmHg;
@override final  int? latestDiastolicMmHg;
@override final  double? latestSpO2Percent;
@override final  double? latestVo2Max;
@override final  double? avgRespiratoryRate;
@override final  double? latestBodyTemperatureCelsius;
@override final  double? latestBloodGlucoseMillimolesPerLiter;
@override final  double? latestSkinTemperatureDeltaCelsius;
@override final  DashboardWeeklyCardioLoad? weeklyCardioLoad;
@override final  DashboardWeeklyIntensityMinutes? weeklyIntensityMinutes;
@override final  int? floorsClimbed;
@override final  double? elevationGainedMeters;
@override final  int? wheelchairPushes;
@override final  int? mindfulnessMinutes;
@override final  int? menstruationPeriodDays;
@override final  int? ovulationTestCount;
@override final  double? latestBasalBodyTemperatureCelsius;
@override final  BodyEnergyTimeline? bodyEnergyTimeline;
 final  Set<String> _missingPermissions;
@override@JsonKey() Set<String> get missingPermissions {
  if (_missingPermissions is EqualUnmodifiableSetView) return _missingPermissions;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_missingPermissions);
}

 final  Set<DashboardMetric> _loadedMetrics;
@override@JsonKey() Set<DashboardMetric> get loadedMetrics {
  if (_loadedMetrics is EqualUnmodifiableSetView) return _loadedMetrics;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_loadedMetrics);
}

/// The metrics the installed provider can serve — i.e. every permission the
/// metric reads is one the device defines. Metrics outside this set get no
/// tile; the rest always get one, empty or not.
 final  Set<DashboardMetric> _supportedMetrics;
/// The metrics the installed provider can serve — i.e. every permission the
/// metric reads is one the device defines. Metrics outside this set get no
/// tile; the rest always get one, empty or not.
@override@JsonKey() Set<DashboardMetric> get supportedMetrics {
  if (_supportedMetrics is EqualUnmodifiableSetView) return _supportedMetrics;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableSetView(_supportedMetrics);
}

 final  Map<DashboardMetric, String> _metricSourcePackages;
@override@JsonKey() Map<DashboardMetric, String> get metricSourcePackages {
  if (_metricSourcePackages is EqualUnmodifiableMapView) return _metricSourcePackages;
  // ignore: implicit_dynamic_type
  return EqualUnmodifiableMapView(_metricSourcePackages);
}

@override@JsonKey() final  CaloriesBurnedSource caloriesKcalSource;

/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DashboardDataCopyWith<_DashboardData> get copyWith => __$DashboardDataCopyWithImpl<_DashboardData>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DashboardData&&(identical(other.date, date) || other.date == date)&&(identical(other.steps, steps) || other.steps == steps)&&(identical(other.distanceMeters, distanceMeters) || other.distanceMeters == distanceMeters)&&(identical(other.caloriesKcal, caloriesKcal) || other.caloriesKcal == caloriesKcal)&&(identical(other.activeCaloriesKcal, activeCaloriesKcal) || other.activeCaloriesKcal == activeCaloriesKcal)&&(identical(other.hydrationLiters, hydrationLiters) || other.hydrationLiters == hydrationLiters)&&(identical(other.workout, workout) || other.workout == workout)&&const DeepCollectionEquality().equals(other._workouts, _workouts)&&(identical(other.sleep, sleep) || other.sleep == sleep)&&(identical(other.sleepScore, sleepScore) || other.sleepScore == sleepScore)&&(identical(other.weightKg, weightKg) || other.weightKg == weightKg)&&(identical(other.weightTime, weightTime) || other.weightTime == weightTime)&&(identical(other.heightCm, heightCm) || other.heightCm == heightCm)&&(identical(other.heightTime, heightTime) || other.heightTime == heightTime)&&(identical(other.bmi, bmi) || other.bmi == bmi)&&(identical(other.ffmi, ffmi) || other.ffmi == ffmi)&&(identical(other.avgHeartRateBpm, avgHeartRateBpm) || other.avgHeartRateBpm == avgHeartRateBpm)&&(identical(other.heartRateSampleCount, heartRateSampleCount) || other.heartRateSampleCount == heartRateSampleCount)&&(identical(other.heartRateSampleStartTime, heartRateSampleStartTime) || other.heartRateSampleStartTime == heartRateSampleStartTime)&&(identical(other.heartRateSampleEndTime, heartRateSampleEndTime) || other.heartRateSampleEndTime == heartRateSampleEndTime)&&(identical(other.restingHeartRateBpm, restingHeartRateBpm) || other.restingHeartRateBpm == restingHeartRateBpm)&&(identical(other.restingHeartRateBaselineBpm, restingHeartRateBaselineBpm) || other.restingHeartRateBaselineBpm == restingHeartRateBaselineBpm)&&(identical(other.hrvRmssdMs, hrvRmssdMs) || other.hrvRmssdMs == hrvRmssdMs)&&(identical(other.hrvBaselineRmssdMs, hrvBaselineRmssdMs) || other.hrvBaselineRmssdMs == hrvBaselineRmssdMs)&&(identical(other.hrvSampleCount, hrvSampleCount) || other.hrvSampleCount == hrvSampleCount)&&(identical(other.hrvSampleStartTime, hrvSampleStartTime) || other.hrvSampleStartTime == hrvSampleStartTime)&&(identical(other.hrvSampleEndTime, hrvSampleEndTime) || other.hrvSampleEndTime == hrvSampleEndTime)&&(identical(other.bodyFatPercent, bodyFatPercent) || other.bodyFatPercent == bodyFatPercent)&&(identical(other.leanMassKg, leanMassKg) || other.leanMassKg == leanMassKg)&&(identical(other.bmrKcal, bmrKcal) || other.bmrKcal == bmrKcal)&&(identical(other.boneMassKg, boneMassKg) || other.boneMassKg == boneMassKg)&&(identical(other.bodyWaterMassKg, bodyWaterMassKg) || other.bodyWaterMassKg == bodyWaterMassKg)&&(identical(other.caloriesInKcal, caloriesInKcal) || other.caloriesInKcal == caloriesInKcal)&&(identical(other.proteinGrams, proteinGrams) || other.proteinGrams == proteinGrams)&&(identical(other.carbsGrams, carbsGrams) || other.carbsGrams == carbsGrams)&&(identical(other.fatGrams, fatGrams) || other.fatGrams == fatGrams)&&(identical(other.caffeineGrams, caffeineGrams) || other.caffeineGrams == caffeineGrams)&&(identical(other.latestSystolicMmHg, latestSystolicMmHg) || other.latestSystolicMmHg == latestSystolicMmHg)&&(identical(other.latestDiastolicMmHg, latestDiastolicMmHg) || other.latestDiastolicMmHg == latestDiastolicMmHg)&&(identical(other.latestSpO2Percent, latestSpO2Percent) || other.latestSpO2Percent == latestSpO2Percent)&&(identical(other.latestVo2Max, latestVo2Max) || other.latestVo2Max == latestVo2Max)&&(identical(other.avgRespiratoryRate, avgRespiratoryRate) || other.avgRespiratoryRate == avgRespiratoryRate)&&(identical(other.latestBodyTemperatureCelsius, latestBodyTemperatureCelsius) || other.latestBodyTemperatureCelsius == latestBodyTemperatureCelsius)&&(identical(other.latestBloodGlucoseMillimolesPerLiter, latestBloodGlucoseMillimolesPerLiter) || other.latestBloodGlucoseMillimolesPerLiter == latestBloodGlucoseMillimolesPerLiter)&&(identical(other.latestSkinTemperatureDeltaCelsius, latestSkinTemperatureDeltaCelsius) || other.latestSkinTemperatureDeltaCelsius == latestSkinTemperatureDeltaCelsius)&&(identical(other.weeklyCardioLoad, weeklyCardioLoad) || other.weeklyCardioLoad == weeklyCardioLoad)&&(identical(other.weeklyIntensityMinutes, weeklyIntensityMinutes) || other.weeklyIntensityMinutes == weeklyIntensityMinutes)&&(identical(other.floorsClimbed, floorsClimbed) || other.floorsClimbed == floorsClimbed)&&(identical(other.elevationGainedMeters, elevationGainedMeters) || other.elevationGainedMeters == elevationGainedMeters)&&(identical(other.wheelchairPushes, wheelchairPushes) || other.wheelchairPushes == wheelchairPushes)&&(identical(other.mindfulnessMinutes, mindfulnessMinutes) || other.mindfulnessMinutes == mindfulnessMinutes)&&(identical(other.menstruationPeriodDays, menstruationPeriodDays) || other.menstruationPeriodDays == menstruationPeriodDays)&&(identical(other.ovulationTestCount, ovulationTestCount) || other.ovulationTestCount == ovulationTestCount)&&(identical(other.latestBasalBodyTemperatureCelsius, latestBasalBodyTemperatureCelsius) || other.latestBasalBodyTemperatureCelsius == latestBasalBodyTemperatureCelsius)&&(identical(other.bodyEnergyTimeline, bodyEnergyTimeline) || other.bodyEnergyTimeline == bodyEnergyTimeline)&&const DeepCollectionEquality().equals(other._missingPermissions, _missingPermissions)&&const DeepCollectionEquality().equals(other._loadedMetrics, _loadedMetrics)&&const DeepCollectionEquality().equals(other._supportedMetrics, _supportedMetrics)&&const DeepCollectionEquality().equals(other._metricSourcePackages, _metricSourcePackages)&&(identical(other.caloriesKcalSource, caloriesKcalSource) || other.caloriesKcalSource == caloriesKcalSource));
}


@override
int get hashCode => Object.hashAll([runtimeType,date,steps,distanceMeters,caloriesKcal,activeCaloriesKcal,hydrationLiters,workout,const DeepCollectionEquality().hash(_workouts),sleep,sleepScore,weightKg,weightTime,heightCm,heightTime,bmi,ffmi,avgHeartRateBpm,heartRateSampleCount,heartRateSampleStartTime,heartRateSampleEndTime,restingHeartRateBpm,restingHeartRateBaselineBpm,hrvRmssdMs,hrvBaselineRmssdMs,hrvSampleCount,hrvSampleStartTime,hrvSampleEndTime,bodyFatPercent,leanMassKg,bmrKcal,boneMassKg,bodyWaterMassKg,caloriesInKcal,proteinGrams,carbsGrams,fatGrams,caffeineGrams,latestSystolicMmHg,latestDiastolicMmHg,latestSpO2Percent,latestVo2Max,avgRespiratoryRate,latestBodyTemperatureCelsius,latestBloodGlucoseMillimolesPerLiter,latestSkinTemperatureDeltaCelsius,weeklyCardioLoad,weeklyIntensityMinutes,floorsClimbed,elevationGainedMeters,wheelchairPushes,mindfulnessMinutes,menstruationPeriodDays,ovulationTestCount,latestBasalBodyTemperatureCelsius,bodyEnergyTimeline,const DeepCollectionEquality().hash(_missingPermissions),const DeepCollectionEquality().hash(_loadedMetrics),const DeepCollectionEquality().hash(_supportedMetrics),const DeepCollectionEquality().hash(_metricSourcePackages),caloriesKcalSource]);

@override
String toString() {
  return 'DashboardData(date: $date, steps: $steps, distanceMeters: $distanceMeters, caloriesKcal: $caloriesKcal, activeCaloriesKcal: $activeCaloriesKcal, hydrationLiters: $hydrationLiters, workout: $workout, workouts: $workouts, sleep: $sleep, sleepScore: $sleepScore, weightKg: $weightKg, weightTime: $weightTime, heightCm: $heightCm, heightTime: $heightTime, bmi: $bmi, ffmi: $ffmi, avgHeartRateBpm: $avgHeartRateBpm, heartRateSampleCount: $heartRateSampleCount, heartRateSampleStartTime: $heartRateSampleStartTime, heartRateSampleEndTime: $heartRateSampleEndTime, restingHeartRateBpm: $restingHeartRateBpm, restingHeartRateBaselineBpm: $restingHeartRateBaselineBpm, hrvRmssdMs: $hrvRmssdMs, hrvBaselineRmssdMs: $hrvBaselineRmssdMs, hrvSampleCount: $hrvSampleCount, hrvSampleStartTime: $hrvSampleStartTime, hrvSampleEndTime: $hrvSampleEndTime, bodyFatPercent: $bodyFatPercent, leanMassKg: $leanMassKg, bmrKcal: $bmrKcal, boneMassKg: $boneMassKg, bodyWaterMassKg: $bodyWaterMassKg, caloriesInKcal: $caloriesInKcal, proteinGrams: $proteinGrams, carbsGrams: $carbsGrams, fatGrams: $fatGrams, caffeineGrams: $caffeineGrams, latestSystolicMmHg: $latestSystolicMmHg, latestDiastolicMmHg: $latestDiastolicMmHg, latestSpO2Percent: $latestSpO2Percent, latestVo2Max: $latestVo2Max, avgRespiratoryRate: $avgRespiratoryRate, latestBodyTemperatureCelsius: $latestBodyTemperatureCelsius, latestBloodGlucoseMillimolesPerLiter: $latestBloodGlucoseMillimolesPerLiter, latestSkinTemperatureDeltaCelsius: $latestSkinTemperatureDeltaCelsius, weeklyCardioLoad: $weeklyCardioLoad, weeklyIntensityMinutes: $weeklyIntensityMinutes, floorsClimbed: $floorsClimbed, elevationGainedMeters: $elevationGainedMeters, wheelchairPushes: $wheelchairPushes, mindfulnessMinutes: $mindfulnessMinutes, menstruationPeriodDays: $menstruationPeriodDays, ovulationTestCount: $ovulationTestCount, latestBasalBodyTemperatureCelsius: $latestBasalBodyTemperatureCelsius, bodyEnergyTimeline: $bodyEnergyTimeline, missingPermissions: $missingPermissions, loadedMetrics: $loadedMetrics, supportedMetrics: $supportedMetrics, metricSourcePackages: $metricSourcePackages, caloriesKcalSource: $caloriesKcalSource)';
}


}

/// @nodoc
abstract mixin class _$DashboardDataCopyWith<$Res> implements $DashboardDataCopyWith<$Res> {
  factory _$DashboardDataCopyWith(_DashboardData value, $Res Function(_DashboardData) _then) = __$DashboardDataCopyWithImpl;
@override @useResult
$Res call({
 LocalDate date, int steps, double distanceMeters, double caloriesKcal, double? activeCaloriesKcal, double hydrationLiters, ExerciseData? workout, List<ExerciseData> workouts, SleepData? sleep, SleepScoreEstimate sleepScore, double? weightKg, DateTime? weightTime, double? heightCm, DateTime? heightTime, double? bmi, double? ffmi, int avgHeartRateBpm, int heartRateSampleCount, DateTime? heartRateSampleStartTime, DateTime? heartRateSampleEndTime, int restingHeartRateBpm, int? restingHeartRateBaselineBpm, double? hrvRmssdMs, double? hrvBaselineRmssdMs, int hrvSampleCount, DateTime? hrvSampleStartTime, DateTime? hrvSampleEndTime, double bodyFatPercent, double? leanMassKg, double? bmrKcal, double? boneMassKg, double? bodyWaterMassKg, double? caloriesInKcal, double? proteinGrams, double? carbsGrams, double? fatGrams, double? caffeineGrams, int? latestSystolicMmHg, int? latestDiastolicMmHg, double? latestSpO2Percent, double? latestVo2Max, double? avgRespiratoryRate, double? latestBodyTemperatureCelsius, double? latestBloodGlucoseMillimolesPerLiter, double? latestSkinTemperatureDeltaCelsius, DashboardWeeklyCardioLoad? weeklyCardioLoad, DashboardWeeklyIntensityMinutes? weeklyIntensityMinutes, int? floorsClimbed, double? elevationGainedMeters, int? wheelchairPushes, int? mindfulnessMinutes, int? menstruationPeriodDays, int? ovulationTestCount, double? latestBasalBodyTemperatureCelsius, BodyEnergyTimeline? bodyEnergyTimeline, Set<String> missingPermissions, Set<DashboardMetric> loadedMetrics, Set<DashboardMetric> supportedMetrics, Map<DashboardMetric, String> metricSourcePackages, CaloriesBurnedSource caloriesKcalSource
});


@override $ExerciseDataCopyWith<$Res>? get workout;@override $SleepDataCopyWith<$Res>? get sleep;@override $SleepScoreEstimateCopyWith<$Res> get sleepScore;@override $DashboardWeeklyCardioLoadCopyWith<$Res>? get weeklyCardioLoad;@override $DashboardWeeklyIntensityMinutesCopyWith<$Res>? get weeklyIntensityMinutes;@override $BodyEnergyTimelineCopyWith<$Res>? get bodyEnergyTimeline;

}
/// @nodoc
class __$DashboardDataCopyWithImpl<$Res>
    implements _$DashboardDataCopyWith<$Res> {
  __$DashboardDataCopyWithImpl(this._self, this._then);

  final _DashboardData _self;
  final $Res Function(_DashboardData) _then;

/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? date = null,Object? steps = null,Object? distanceMeters = null,Object? caloriesKcal = null,Object? activeCaloriesKcal = freezed,Object? hydrationLiters = null,Object? workout = freezed,Object? workouts = null,Object? sleep = freezed,Object? sleepScore = null,Object? weightKg = freezed,Object? weightTime = freezed,Object? heightCm = freezed,Object? heightTime = freezed,Object? bmi = freezed,Object? ffmi = freezed,Object? avgHeartRateBpm = null,Object? heartRateSampleCount = null,Object? heartRateSampleStartTime = freezed,Object? heartRateSampleEndTime = freezed,Object? restingHeartRateBpm = null,Object? restingHeartRateBaselineBpm = freezed,Object? hrvRmssdMs = freezed,Object? hrvBaselineRmssdMs = freezed,Object? hrvSampleCount = null,Object? hrvSampleStartTime = freezed,Object? hrvSampleEndTime = freezed,Object? bodyFatPercent = null,Object? leanMassKg = freezed,Object? bmrKcal = freezed,Object? boneMassKg = freezed,Object? bodyWaterMassKg = freezed,Object? caloriesInKcal = freezed,Object? proteinGrams = freezed,Object? carbsGrams = freezed,Object? fatGrams = freezed,Object? caffeineGrams = freezed,Object? latestSystolicMmHg = freezed,Object? latestDiastolicMmHg = freezed,Object? latestSpO2Percent = freezed,Object? latestVo2Max = freezed,Object? avgRespiratoryRate = freezed,Object? latestBodyTemperatureCelsius = freezed,Object? latestBloodGlucoseMillimolesPerLiter = freezed,Object? latestSkinTemperatureDeltaCelsius = freezed,Object? weeklyCardioLoad = freezed,Object? weeklyIntensityMinutes = freezed,Object? floorsClimbed = freezed,Object? elevationGainedMeters = freezed,Object? wheelchairPushes = freezed,Object? mindfulnessMinutes = freezed,Object? menstruationPeriodDays = freezed,Object? ovulationTestCount = freezed,Object? latestBasalBodyTemperatureCelsius = freezed,Object? bodyEnergyTimeline = freezed,Object? missingPermissions = null,Object? loadedMetrics = null,Object? supportedMetrics = null,Object? metricSourcePackages = null,Object? caloriesKcalSource = null,}) {
  return _then(_DashboardData(
date: null == date ? _self.date : date // ignore: cast_nullable_to_non_nullable
as LocalDate,steps: null == steps ? _self.steps : steps // ignore: cast_nullable_to_non_nullable
as int,distanceMeters: null == distanceMeters ? _self.distanceMeters : distanceMeters // ignore: cast_nullable_to_non_nullable
as double,caloriesKcal: null == caloriesKcal ? _self.caloriesKcal : caloriesKcal // ignore: cast_nullable_to_non_nullable
as double,activeCaloriesKcal: freezed == activeCaloriesKcal ? _self.activeCaloriesKcal : activeCaloriesKcal // ignore: cast_nullable_to_non_nullable
as double?,hydrationLiters: null == hydrationLiters ? _self.hydrationLiters : hydrationLiters // ignore: cast_nullable_to_non_nullable
as double,workout: freezed == workout ? _self.workout : workout // ignore: cast_nullable_to_non_nullable
as ExerciseData?,workouts: null == workouts ? _self._workouts : workouts // ignore: cast_nullable_to_non_nullable
as List<ExerciseData>,sleep: freezed == sleep ? _self.sleep : sleep // ignore: cast_nullable_to_non_nullable
as SleepData?,sleepScore: null == sleepScore ? _self.sleepScore : sleepScore // ignore: cast_nullable_to_non_nullable
as SleepScoreEstimate,weightKg: freezed == weightKg ? _self.weightKg : weightKg // ignore: cast_nullable_to_non_nullable
as double?,weightTime: freezed == weightTime ? _self.weightTime : weightTime // ignore: cast_nullable_to_non_nullable
as DateTime?,heightCm: freezed == heightCm ? _self.heightCm : heightCm // ignore: cast_nullable_to_non_nullable
as double?,heightTime: freezed == heightTime ? _self.heightTime : heightTime // ignore: cast_nullable_to_non_nullable
as DateTime?,bmi: freezed == bmi ? _self.bmi : bmi // ignore: cast_nullable_to_non_nullable
as double?,ffmi: freezed == ffmi ? _self.ffmi : ffmi // ignore: cast_nullable_to_non_nullable
as double?,avgHeartRateBpm: null == avgHeartRateBpm ? _self.avgHeartRateBpm : avgHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int,heartRateSampleCount: null == heartRateSampleCount ? _self.heartRateSampleCount : heartRateSampleCount // ignore: cast_nullable_to_non_nullable
as int,heartRateSampleStartTime: freezed == heartRateSampleStartTime ? _self.heartRateSampleStartTime : heartRateSampleStartTime // ignore: cast_nullable_to_non_nullable
as DateTime?,heartRateSampleEndTime: freezed == heartRateSampleEndTime ? _self.heartRateSampleEndTime : heartRateSampleEndTime // ignore: cast_nullable_to_non_nullable
as DateTime?,restingHeartRateBpm: null == restingHeartRateBpm ? _self.restingHeartRateBpm : restingHeartRateBpm // ignore: cast_nullable_to_non_nullable
as int,restingHeartRateBaselineBpm: freezed == restingHeartRateBaselineBpm ? _self.restingHeartRateBaselineBpm : restingHeartRateBaselineBpm // ignore: cast_nullable_to_non_nullable
as int?,hrvRmssdMs: freezed == hrvRmssdMs ? _self.hrvRmssdMs : hrvRmssdMs // ignore: cast_nullable_to_non_nullable
as double?,hrvBaselineRmssdMs: freezed == hrvBaselineRmssdMs ? _self.hrvBaselineRmssdMs : hrvBaselineRmssdMs // ignore: cast_nullable_to_non_nullable
as double?,hrvSampleCount: null == hrvSampleCount ? _self.hrvSampleCount : hrvSampleCount // ignore: cast_nullable_to_non_nullable
as int,hrvSampleStartTime: freezed == hrvSampleStartTime ? _self.hrvSampleStartTime : hrvSampleStartTime // ignore: cast_nullable_to_non_nullable
as DateTime?,hrvSampleEndTime: freezed == hrvSampleEndTime ? _self.hrvSampleEndTime : hrvSampleEndTime // ignore: cast_nullable_to_non_nullable
as DateTime?,bodyFatPercent: null == bodyFatPercent ? _self.bodyFatPercent : bodyFatPercent // ignore: cast_nullable_to_non_nullable
as double,leanMassKg: freezed == leanMassKg ? _self.leanMassKg : leanMassKg // ignore: cast_nullable_to_non_nullable
as double?,bmrKcal: freezed == bmrKcal ? _self.bmrKcal : bmrKcal // ignore: cast_nullable_to_non_nullable
as double?,boneMassKg: freezed == boneMassKg ? _self.boneMassKg : boneMassKg // ignore: cast_nullable_to_non_nullable
as double?,bodyWaterMassKg: freezed == bodyWaterMassKg ? _self.bodyWaterMassKg : bodyWaterMassKg // ignore: cast_nullable_to_non_nullable
as double?,caloriesInKcal: freezed == caloriesInKcal ? _self.caloriesInKcal : caloriesInKcal // ignore: cast_nullable_to_non_nullable
as double?,proteinGrams: freezed == proteinGrams ? _self.proteinGrams : proteinGrams // ignore: cast_nullable_to_non_nullable
as double?,carbsGrams: freezed == carbsGrams ? _self.carbsGrams : carbsGrams // ignore: cast_nullable_to_non_nullable
as double?,fatGrams: freezed == fatGrams ? _self.fatGrams : fatGrams // ignore: cast_nullable_to_non_nullable
as double?,caffeineGrams: freezed == caffeineGrams ? _self.caffeineGrams : caffeineGrams // ignore: cast_nullable_to_non_nullable
as double?,latestSystolicMmHg: freezed == latestSystolicMmHg ? _self.latestSystolicMmHg : latestSystolicMmHg // ignore: cast_nullable_to_non_nullable
as int?,latestDiastolicMmHg: freezed == latestDiastolicMmHg ? _self.latestDiastolicMmHg : latestDiastolicMmHg // ignore: cast_nullable_to_non_nullable
as int?,latestSpO2Percent: freezed == latestSpO2Percent ? _self.latestSpO2Percent : latestSpO2Percent // ignore: cast_nullable_to_non_nullable
as double?,latestVo2Max: freezed == latestVo2Max ? _self.latestVo2Max : latestVo2Max // ignore: cast_nullable_to_non_nullable
as double?,avgRespiratoryRate: freezed == avgRespiratoryRate ? _self.avgRespiratoryRate : avgRespiratoryRate // ignore: cast_nullable_to_non_nullable
as double?,latestBodyTemperatureCelsius: freezed == latestBodyTemperatureCelsius ? _self.latestBodyTemperatureCelsius : latestBodyTemperatureCelsius // ignore: cast_nullable_to_non_nullable
as double?,latestBloodGlucoseMillimolesPerLiter: freezed == latestBloodGlucoseMillimolesPerLiter ? _self.latestBloodGlucoseMillimolesPerLiter : latestBloodGlucoseMillimolesPerLiter // ignore: cast_nullable_to_non_nullable
as double?,latestSkinTemperatureDeltaCelsius: freezed == latestSkinTemperatureDeltaCelsius ? _self.latestSkinTemperatureDeltaCelsius : latestSkinTemperatureDeltaCelsius // ignore: cast_nullable_to_non_nullable
as double?,weeklyCardioLoad: freezed == weeklyCardioLoad ? _self.weeklyCardioLoad : weeklyCardioLoad // ignore: cast_nullable_to_non_nullable
as DashboardWeeklyCardioLoad?,weeklyIntensityMinutes: freezed == weeklyIntensityMinutes ? _self.weeklyIntensityMinutes : weeklyIntensityMinutes // ignore: cast_nullable_to_non_nullable
as DashboardWeeklyIntensityMinutes?,floorsClimbed: freezed == floorsClimbed ? _self.floorsClimbed : floorsClimbed // ignore: cast_nullable_to_non_nullable
as int?,elevationGainedMeters: freezed == elevationGainedMeters ? _self.elevationGainedMeters : elevationGainedMeters // ignore: cast_nullable_to_non_nullable
as double?,wheelchairPushes: freezed == wheelchairPushes ? _self.wheelchairPushes : wheelchairPushes // ignore: cast_nullable_to_non_nullable
as int?,mindfulnessMinutes: freezed == mindfulnessMinutes ? _self.mindfulnessMinutes : mindfulnessMinutes // ignore: cast_nullable_to_non_nullable
as int?,menstruationPeriodDays: freezed == menstruationPeriodDays ? _self.menstruationPeriodDays : menstruationPeriodDays // ignore: cast_nullable_to_non_nullable
as int?,ovulationTestCount: freezed == ovulationTestCount ? _self.ovulationTestCount : ovulationTestCount // ignore: cast_nullable_to_non_nullable
as int?,latestBasalBodyTemperatureCelsius: freezed == latestBasalBodyTemperatureCelsius ? _self.latestBasalBodyTemperatureCelsius : latestBasalBodyTemperatureCelsius // ignore: cast_nullable_to_non_nullable
as double?,bodyEnergyTimeline: freezed == bodyEnergyTimeline ? _self.bodyEnergyTimeline : bodyEnergyTimeline // ignore: cast_nullable_to_non_nullable
as BodyEnergyTimeline?,missingPermissions: null == missingPermissions ? _self._missingPermissions : missingPermissions // ignore: cast_nullable_to_non_nullable
as Set<String>,loadedMetrics: null == loadedMetrics ? _self._loadedMetrics : loadedMetrics // ignore: cast_nullable_to_non_nullable
as Set<DashboardMetric>,supportedMetrics: null == supportedMetrics ? _self._supportedMetrics : supportedMetrics // ignore: cast_nullable_to_non_nullable
as Set<DashboardMetric>,metricSourcePackages: null == metricSourcePackages ? _self._metricSourcePackages : metricSourcePackages // ignore: cast_nullable_to_non_nullable
as Map<DashboardMetric, String>,caloriesKcalSource: null == caloriesKcalSource ? _self.caloriesKcalSource : caloriesKcalSource // ignore: cast_nullable_to_non_nullable
as CaloriesBurnedSource,
  ));
}

/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$ExerciseDataCopyWith<$Res>? get workout {
    if (_self.workout == null) {
    return null;
  }

  return $ExerciseDataCopyWith<$Res>(_self.workout!, (value) {
    return _then(_self.copyWith(workout: value));
  });
}/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepDataCopyWith<$Res>? get sleep {
    if (_self.sleep == null) {
    return null;
  }

  return $SleepDataCopyWith<$Res>(_self.sleep!, (value) {
    return _then(_self.copyWith(sleep: value));
  });
}/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$SleepScoreEstimateCopyWith<$Res> get sleepScore {
  
  return $SleepScoreEstimateCopyWith<$Res>(_self.sleepScore, (value) {
    return _then(_self.copyWith(sleepScore: value));
  });
}/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DashboardWeeklyCardioLoadCopyWith<$Res>? get weeklyCardioLoad {
    if (_self.weeklyCardioLoad == null) {
    return null;
  }

  return $DashboardWeeklyCardioLoadCopyWith<$Res>(_self.weeklyCardioLoad!, (value) {
    return _then(_self.copyWith(weeklyCardioLoad: value));
  });
}/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$DashboardWeeklyIntensityMinutesCopyWith<$Res>? get weeklyIntensityMinutes {
    if (_self.weeklyIntensityMinutes == null) {
    return null;
  }

  return $DashboardWeeklyIntensityMinutesCopyWith<$Res>(_self.weeklyIntensityMinutes!, (value) {
    return _then(_self.copyWith(weeklyIntensityMinutes: value));
  });
}/// Create a copy of DashboardData
/// with the given fields replaced by the non-null parameter values.
@override
@pragma('vm:prefer-inline')
$BodyEnergyTimelineCopyWith<$Res>? get bodyEnergyTimeline {
    if (_self.bodyEnergyTimeline == null) {
    return null;
  }

  return $BodyEnergyTimelineCopyWith<$Res>(_self.bodyEnergyTimeline!, (value) {
    return _then(_self.copyWith(bodyEnergyTimeline: value));
  });
}
}

/// @nodoc
mixin _$DashboardWeeklyCardioLoad {

 int get currentScore; int get targetScore; int get todayScore; CardioLoadConfidence get confidence; DashboardWeeklyCardioLoadTargetSource get targetSource;
/// Create a copy of DashboardWeeklyCardioLoad
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DashboardWeeklyCardioLoadCopyWith<DashboardWeeklyCardioLoad> get copyWith => _$DashboardWeeklyCardioLoadCopyWithImpl<DashboardWeeklyCardioLoad>(this as DashboardWeeklyCardioLoad, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DashboardWeeklyCardioLoad&&(identical(other.currentScore, currentScore) || other.currentScore == currentScore)&&(identical(other.targetScore, targetScore) || other.targetScore == targetScore)&&(identical(other.todayScore, todayScore) || other.todayScore == todayScore)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.targetSource, targetSource) || other.targetSource == targetSource));
}


@override
int get hashCode => Object.hash(runtimeType,currentScore,targetScore,todayScore,confidence,targetSource);

@override
String toString() {
  return 'DashboardWeeklyCardioLoad(currentScore: $currentScore, targetScore: $targetScore, todayScore: $todayScore, confidence: $confidence, targetSource: $targetSource)';
}


}

/// @nodoc
abstract mixin class $DashboardWeeklyCardioLoadCopyWith<$Res>  {
  factory $DashboardWeeklyCardioLoadCopyWith(DashboardWeeklyCardioLoad value, $Res Function(DashboardWeeklyCardioLoad) _then) = _$DashboardWeeklyCardioLoadCopyWithImpl;
@useResult
$Res call({
 int currentScore, int targetScore, int todayScore, CardioLoadConfidence confidence, DashboardWeeklyCardioLoadTargetSource targetSource
});




}
/// @nodoc
class _$DashboardWeeklyCardioLoadCopyWithImpl<$Res>
    implements $DashboardWeeklyCardioLoadCopyWith<$Res> {
  _$DashboardWeeklyCardioLoadCopyWithImpl(this._self, this._then);

  final DashboardWeeklyCardioLoad _self;
  final $Res Function(DashboardWeeklyCardioLoad) _then;

/// Create a copy of DashboardWeeklyCardioLoad
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? currentScore = null,Object? targetScore = null,Object? todayScore = null,Object? confidence = null,Object? targetSource = null,}) {
  return _then(_self.copyWith(
currentScore: null == currentScore ? _self.currentScore : currentScore // ignore: cast_nullable_to_non_nullable
as int,targetScore: null == targetScore ? _self.targetScore : targetScore // ignore: cast_nullable_to_non_nullable
as int,todayScore: null == todayScore ? _self.todayScore : todayScore // ignore: cast_nullable_to_non_nullable
as int,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as CardioLoadConfidence,targetSource: null == targetSource ? _self.targetSource : targetSource // ignore: cast_nullable_to_non_nullable
as DashboardWeeklyCardioLoadTargetSource,
  ));
}

}


/// Adds pattern-matching-related methods to [DashboardWeeklyCardioLoad].
extension DashboardWeeklyCardioLoadPatterns on DashboardWeeklyCardioLoad {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DashboardWeeklyCardioLoad value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DashboardWeeklyCardioLoad() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DashboardWeeklyCardioLoad value)  $default,){
final _that = this;
switch (_that) {
case _DashboardWeeklyCardioLoad():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DashboardWeeklyCardioLoad value)?  $default,){
final _that = this;
switch (_that) {
case _DashboardWeeklyCardioLoad() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int currentScore,  int targetScore,  int todayScore,  CardioLoadConfidence confidence,  DashboardWeeklyCardioLoadTargetSource targetSource)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DashboardWeeklyCardioLoad() when $default != null:
return $default(_that.currentScore,_that.targetScore,_that.todayScore,_that.confidence,_that.targetSource);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int currentScore,  int targetScore,  int todayScore,  CardioLoadConfidence confidence,  DashboardWeeklyCardioLoadTargetSource targetSource)  $default,) {final _that = this;
switch (_that) {
case _DashboardWeeklyCardioLoad():
return $default(_that.currentScore,_that.targetScore,_that.todayScore,_that.confidence,_that.targetSource);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int currentScore,  int targetScore,  int todayScore,  CardioLoadConfidence confidence,  DashboardWeeklyCardioLoadTargetSource targetSource)?  $default,) {final _that = this;
switch (_that) {
case _DashboardWeeklyCardioLoad() when $default != null:
return $default(_that.currentScore,_that.targetScore,_that.todayScore,_that.confidence,_that.targetSource);case _:
  return null;

}
}

}

/// @nodoc


class _DashboardWeeklyCardioLoad extends DashboardWeeklyCardioLoad {
  const _DashboardWeeklyCardioLoad({required this.currentScore, required this.targetScore, required this.todayScore, required this.confidence, required this.targetSource}): super._();
  

@override final  int currentScore;
@override final  int targetScore;
@override final  int todayScore;
@override final  CardioLoadConfidence confidence;
@override final  DashboardWeeklyCardioLoadTargetSource targetSource;

/// Create a copy of DashboardWeeklyCardioLoad
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DashboardWeeklyCardioLoadCopyWith<_DashboardWeeklyCardioLoad> get copyWith => __$DashboardWeeklyCardioLoadCopyWithImpl<_DashboardWeeklyCardioLoad>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DashboardWeeklyCardioLoad&&(identical(other.currentScore, currentScore) || other.currentScore == currentScore)&&(identical(other.targetScore, targetScore) || other.targetScore == targetScore)&&(identical(other.todayScore, todayScore) || other.todayScore == todayScore)&&(identical(other.confidence, confidence) || other.confidence == confidence)&&(identical(other.targetSource, targetSource) || other.targetSource == targetSource));
}


@override
int get hashCode => Object.hash(runtimeType,currentScore,targetScore,todayScore,confidence,targetSource);

@override
String toString() {
  return 'DashboardWeeklyCardioLoad(currentScore: $currentScore, targetScore: $targetScore, todayScore: $todayScore, confidence: $confidence, targetSource: $targetSource)';
}


}

/// @nodoc
abstract mixin class _$DashboardWeeklyCardioLoadCopyWith<$Res> implements $DashboardWeeklyCardioLoadCopyWith<$Res> {
  factory _$DashboardWeeklyCardioLoadCopyWith(_DashboardWeeklyCardioLoad value, $Res Function(_DashboardWeeklyCardioLoad) _then) = __$DashboardWeeklyCardioLoadCopyWithImpl;
@override @useResult
$Res call({
 int currentScore, int targetScore, int todayScore, CardioLoadConfidence confidence, DashboardWeeklyCardioLoadTargetSource targetSource
});




}
/// @nodoc
class __$DashboardWeeklyCardioLoadCopyWithImpl<$Res>
    implements _$DashboardWeeklyCardioLoadCopyWith<$Res> {
  __$DashboardWeeklyCardioLoadCopyWithImpl(this._self, this._then);

  final _DashboardWeeklyCardioLoad _self;
  final $Res Function(_DashboardWeeklyCardioLoad) _then;

/// Create a copy of DashboardWeeklyCardioLoad
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? currentScore = null,Object? targetScore = null,Object? todayScore = null,Object? confidence = null,Object? targetSource = null,}) {
  return _then(_DashboardWeeklyCardioLoad(
currentScore: null == currentScore ? _self.currentScore : currentScore // ignore: cast_nullable_to_non_nullable
as int,targetScore: null == targetScore ? _self.targetScore : targetScore // ignore: cast_nullable_to_non_nullable
as int,todayScore: null == todayScore ? _self.todayScore : todayScore // ignore: cast_nullable_to_non_nullable
as int,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as CardioLoadConfidence,targetSource: null == targetSource ? _self.targetSource : targetSource // ignore: cast_nullable_to_non_nullable
as DashboardWeeklyCardioLoadTargetSource,
  ));
}


}

/// @nodoc
mixin _$DashboardWeeklyIntensityMinutes {

 int get moderateMinutes; int get vigorousMinutes; int get moderateEquivalentMinutes; int get targetMinutes; int get todayModerateEquivalentMinutes; int get daysElapsed; IntensityMinutesConfidence get confidence;
/// Create a copy of DashboardWeeklyIntensityMinutes
/// with the given fields replaced by the non-null parameter values.
@JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
$DashboardWeeklyIntensityMinutesCopyWith<DashboardWeeklyIntensityMinutes> get copyWith => _$DashboardWeeklyIntensityMinutesCopyWithImpl<DashboardWeeklyIntensityMinutes>(this as DashboardWeeklyIntensityMinutes, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is DashboardWeeklyIntensityMinutes&&(identical(other.moderateMinutes, moderateMinutes) || other.moderateMinutes == moderateMinutes)&&(identical(other.vigorousMinutes, vigorousMinutes) || other.vigorousMinutes == vigorousMinutes)&&(identical(other.moderateEquivalentMinutes, moderateEquivalentMinutes) || other.moderateEquivalentMinutes == moderateEquivalentMinutes)&&(identical(other.targetMinutes, targetMinutes) || other.targetMinutes == targetMinutes)&&(identical(other.todayModerateEquivalentMinutes, todayModerateEquivalentMinutes) || other.todayModerateEquivalentMinutes == todayModerateEquivalentMinutes)&&(identical(other.daysElapsed, daysElapsed) || other.daysElapsed == daysElapsed)&&(identical(other.confidence, confidence) || other.confidence == confidence));
}


@override
int get hashCode => Object.hash(runtimeType,moderateMinutes,vigorousMinutes,moderateEquivalentMinutes,targetMinutes,todayModerateEquivalentMinutes,daysElapsed,confidence);

@override
String toString() {
  return 'DashboardWeeklyIntensityMinutes(moderateMinutes: $moderateMinutes, vigorousMinutes: $vigorousMinutes, moderateEquivalentMinutes: $moderateEquivalentMinutes, targetMinutes: $targetMinutes, todayModerateEquivalentMinutes: $todayModerateEquivalentMinutes, daysElapsed: $daysElapsed, confidence: $confidence)';
}


}

/// @nodoc
abstract mixin class $DashboardWeeklyIntensityMinutesCopyWith<$Res>  {
  factory $DashboardWeeklyIntensityMinutesCopyWith(DashboardWeeklyIntensityMinutes value, $Res Function(DashboardWeeklyIntensityMinutes) _then) = _$DashboardWeeklyIntensityMinutesCopyWithImpl;
@useResult
$Res call({
 int moderateMinutes, int vigorousMinutes, int moderateEquivalentMinutes, int targetMinutes, int todayModerateEquivalentMinutes, int daysElapsed, IntensityMinutesConfidence confidence
});




}
/// @nodoc
class _$DashboardWeeklyIntensityMinutesCopyWithImpl<$Res>
    implements $DashboardWeeklyIntensityMinutesCopyWith<$Res> {
  _$DashboardWeeklyIntensityMinutesCopyWithImpl(this._self, this._then);

  final DashboardWeeklyIntensityMinutes _self;
  final $Res Function(DashboardWeeklyIntensityMinutes) _then;

/// Create a copy of DashboardWeeklyIntensityMinutes
/// with the given fields replaced by the non-null parameter values.
@pragma('vm:prefer-inline') @override $Res call({Object? moderateMinutes = null,Object? vigorousMinutes = null,Object? moderateEquivalentMinutes = null,Object? targetMinutes = null,Object? todayModerateEquivalentMinutes = null,Object? daysElapsed = null,Object? confidence = null,}) {
  return _then(_self.copyWith(
moderateMinutes: null == moderateMinutes ? _self.moderateMinutes : moderateMinutes // ignore: cast_nullable_to_non_nullable
as int,vigorousMinutes: null == vigorousMinutes ? _self.vigorousMinutes : vigorousMinutes // ignore: cast_nullable_to_non_nullable
as int,moderateEquivalentMinutes: null == moderateEquivalentMinutes ? _self.moderateEquivalentMinutes : moderateEquivalentMinutes // ignore: cast_nullable_to_non_nullable
as int,targetMinutes: null == targetMinutes ? _self.targetMinutes : targetMinutes // ignore: cast_nullable_to_non_nullable
as int,todayModerateEquivalentMinutes: null == todayModerateEquivalentMinutes ? _self.todayModerateEquivalentMinutes : todayModerateEquivalentMinutes // ignore: cast_nullable_to_non_nullable
as int,daysElapsed: null == daysElapsed ? _self.daysElapsed : daysElapsed // ignore: cast_nullable_to_non_nullable
as int,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as IntensityMinutesConfidence,
  ));
}

}


/// Adds pattern-matching-related methods to [DashboardWeeklyIntensityMinutes].
extension DashboardWeeklyIntensityMinutesPatterns on DashboardWeeklyIntensityMinutes {
/// A variant of `map` that fallback to returning `orElse`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeMap<TResult extends Object?>(TResult Function( _DashboardWeeklyIntensityMinutes value)?  $default,{required TResult orElse(),}){
final _that = this;
switch (_that) {
case _DashboardWeeklyIntensityMinutes() when $default != null:
return $default(_that);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// Callbacks receives the raw object, upcasted.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case final Subclass2 value:
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult map<TResult extends Object?>(TResult Function( _DashboardWeeklyIntensityMinutes value)  $default,){
final _that = this;
switch (_that) {
case _DashboardWeeklyIntensityMinutes():
return $default(_that);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `map` that fallback to returning `null`.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case final Subclass value:
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? mapOrNull<TResult extends Object?>(TResult? Function( _DashboardWeeklyIntensityMinutes value)?  $default,){
final _that = this;
switch (_that) {
case _DashboardWeeklyIntensityMinutes() when $default != null:
return $default(_that);case _:
  return null;

}
}
/// A variant of `when` that fallback to an `orElse` callback.
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return orElse();
/// }
/// ```

@optionalTypeArgs TResult maybeWhen<TResult extends Object?>(TResult Function( int moderateMinutes,  int vigorousMinutes,  int moderateEquivalentMinutes,  int targetMinutes,  int todayModerateEquivalentMinutes,  int daysElapsed,  IntensityMinutesConfidence confidence)?  $default,{required TResult orElse(),}) {final _that = this;
switch (_that) {
case _DashboardWeeklyIntensityMinutes() when $default != null:
return $default(_that.moderateMinutes,_that.vigorousMinutes,_that.moderateEquivalentMinutes,_that.targetMinutes,_that.todayModerateEquivalentMinutes,_that.daysElapsed,_that.confidence);case _:
  return orElse();

}
}
/// A `switch`-like method, using callbacks.
///
/// As opposed to `map`, this offers destructuring.
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case Subclass2(:final field2):
///     return ...;
/// }
/// ```

@optionalTypeArgs TResult when<TResult extends Object?>(TResult Function( int moderateMinutes,  int vigorousMinutes,  int moderateEquivalentMinutes,  int targetMinutes,  int todayModerateEquivalentMinutes,  int daysElapsed,  IntensityMinutesConfidence confidence)  $default,) {final _that = this;
switch (_that) {
case _DashboardWeeklyIntensityMinutes():
return $default(_that.moderateMinutes,_that.vigorousMinutes,_that.moderateEquivalentMinutes,_that.targetMinutes,_that.todayModerateEquivalentMinutes,_that.daysElapsed,_that.confidence);case _:
  throw StateError('Unexpected subclass');

}
}
/// A variant of `when` that fallback to returning `null`
///
/// It is equivalent to doing:
/// ```dart
/// switch (sealedClass) {
///   case Subclass(:final field):
///     return ...;
///   case _:
///     return null;
/// }
/// ```

@optionalTypeArgs TResult? whenOrNull<TResult extends Object?>(TResult? Function( int moderateMinutes,  int vigorousMinutes,  int moderateEquivalentMinutes,  int targetMinutes,  int todayModerateEquivalentMinutes,  int daysElapsed,  IntensityMinutesConfidence confidence)?  $default,) {final _that = this;
switch (_that) {
case _DashboardWeeklyIntensityMinutes() when $default != null:
return $default(_that.moderateMinutes,_that.vigorousMinutes,_that.moderateEquivalentMinutes,_that.targetMinutes,_that.todayModerateEquivalentMinutes,_that.daysElapsed,_that.confidence);case _:
  return null;

}
}

}

/// @nodoc


class _DashboardWeeklyIntensityMinutes extends DashboardWeeklyIntensityMinutes {
  const _DashboardWeeklyIntensityMinutes({required this.moderateMinutes, required this.vigorousMinutes, required this.moderateEquivalentMinutes, this.targetMinutes = defaultWeeklyIntensityMinutesTarget, required this.todayModerateEquivalentMinutes, required this.daysElapsed, required this.confidence}): super._();
  

@override final  int moderateMinutes;
@override final  int vigorousMinutes;
@override final  int moderateEquivalentMinutes;
@override@JsonKey() final  int targetMinutes;
@override final  int todayModerateEquivalentMinutes;
@override final  int daysElapsed;
@override final  IntensityMinutesConfidence confidence;

/// Create a copy of DashboardWeeklyIntensityMinutes
/// with the given fields replaced by the non-null parameter values.
@override @JsonKey(includeFromJson: false, includeToJson: false)
@pragma('vm:prefer-inline')
_$DashboardWeeklyIntensityMinutesCopyWith<_DashboardWeeklyIntensityMinutes> get copyWith => __$DashboardWeeklyIntensityMinutesCopyWithImpl<_DashboardWeeklyIntensityMinutes>(this, _$identity);



@override
bool operator ==(Object other) {
  return identical(this, other) || (other.runtimeType == runtimeType&&other is _DashboardWeeklyIntensityMinutes&&(identical(other.moderateMinutes, moderateMinutes) || other.moderateMinutes == moderateMinutes)&&(identical(other.vigorousMinutes, vigorousMinutes) || other.vigorousMinutes == vigorousMinutes)&&(identical(other.moderateEquivalentMinutes, moderateEquivalentMinutes) || other.moderateEquivalentMinutes == moderateEquivalentMinutes)&&(identical(other.targetMinutes, targetMinutes) || other.targetMinutes == targetMinutes)&&(identical(other.todayModerateEquivalentMinutes, todayModerateEquivalentMinutes) || other.todayModerateEquivalentMinutes == todayModerateEquivalentMinutes)&&(identical(other.daysElapsed, daysElapsed) || other.daysElapsed == daysElapsed)&&(identical(other.confidence, confidence) || other.confidence == confidence));
}


@override
int get hashCode => Object.hash(runtimeType,moderateMinutes,vigorousMinutes,moderateEquivalentMinutes,targetMinutes,todayModerateEquivalentMinutes,daysElapsed,confidence);

@override
String toString() {
  return 'DashboardWeeklyIntensityMinutes(moderateMinutes: $moderateMinutes, vigorousMinutes: $vigorousMinutes, moderateEquivalentMinutes: $moderateEquivalentMinutes, targetMinutes: $targetMinutes, todayModerateEquivalentMinutes: $todayModerateEquivalentMinutes, daysElapsed: $daysElapsed, confidence: $confidence)';
}


}

/// @nodoc
abstract mixin class _$DashboardWeeklyIntensityMinutesCopyWith<$Res> implements $DashboardWeeklyIntensityMinutesCopyWith<$Res> {
  factory _$DashboardWeeklyIntensityMinutesCopyWith(_DashboardWeeklyIntensityMinutes value, $Res Function(_DashboardWeeklyIntensityMinutes) _then) = __$DashboardWeeklyIntensityMinutesCopyWithImpl;
@override @useResult
$Res call({
 int moderateMinutes, int vigorousMinutes, int moderateEquivalentMinutes, int targetMinutes, int todayModerateEquivalentMinutes, int daysElapsed, IntensityMinutesConfidence confidence
});




}
/// @nodoc
class __$DashboardWeeklyIntensityMinutesCopyWithImpl<$Res>
    implements _$DashboardWeeklyIntensityMinutesCopyWith<$Res> {
  __$DashboardWeeklyIntensityMinutesCopyWithImpl(this._self, this._then);

  final _DashboardWeeklyIntensityMinutes _self;
  final $Res Function(_DashboardWeeklyIntensityMinutes) _then;

/// Create a copy of DashboardWeeklyIntensityMinutes
/// with the given fields replaced by the non-null parameter values.
@override @pragma('vm:prefer-inline') $Res call({Object? moderateMinutes = null,Object? vigorousMinutes = null,Object? moderateEquivalentMinutes = null,Object? targetMinutes = null,Object? todayModerateEquivalentMinutes = null,Object? daysElapsed = null,Object? confidence = null,}) {
  return _then(_DashboardWeeklyIntensityMinutes(
moderateMinutes: null == moderateMinutes ? _self.moderateMinutes : moderateMinutes // ignore: cast_nullable_to_non_nullable
as int,vigorousMinutes: null == vigorousMinutes ? _self.vigorousMinutes : vigorousMinutes // ignore: cast_nullable_to_non_nullable
as int,moderateEquivalentMinutes: null == moderateEquivalentMinutes ? _self.moderateEquivalentMinutes : moderateEquivalentMinutes // ignore: cast_nullable_to_non_nullable
as int,targetMinutes: null == targetMinutes ? _self.targetMinutes : targetMinutes // ignore: cast_nullable_to_non_nullable
as int,todayModerateEquivalentMinutes: null == todayModerateEquivalentMinutes ? _self.todayModerateEquivalentMinutes : todayModerateEquivalentMinutes // ignore: cast_nullable_to_non_nullable
as int,daysElapsed: null == daysElapsed ? _self.daysElapsed : daysElapsed // ignore: cast_nullable_to_non_nullable
as int,confidence: null == confidence ? _self.confidence : confidence // ignore: cast_nullable_to_non_nullable
as IntensityMinutesConfidence,
  ));
}


}

// dart format on
