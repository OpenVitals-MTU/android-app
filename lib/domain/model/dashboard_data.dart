import 'dart:math' as math;

import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';
import '../insights/body_energy_timeline.dart';
import '../insights/cardio_load.dart';
import '../insights/intensity_minutes.dart';
import '../insights/sleep_score.dart';
import 'activity_models.dart';
import 'dashboard_query.dart';
import 'nutrition_models.dart';
import 'sleep_models.dart';

part 'dashboard_data.freezed.dart';

@freezed
abstract class DashboardData with _$DashboardData {
  const factory DashboardData({
    required LocalDate date,
    @Default(0) int steps,
    @Default(0.0) double distanceMeters,
    @Default(0.0) double caloriesKcal,
    double? activeCaloriesKcal,
    @Default(0.0) double hydrationLiters,
    ExerciseData? workout,
    @Default(<ExerciseData>[]) List<ExerciseData> workouts,
    SleepData? sleep,
    @Default(SleepScoreEstimate.noData) SleepScoreEstimate sleepScore,
    double? weightKg,
    DateTime? weightTime,
    double? heightCm,
    DateTime? heightTime,
    double? bmi,
    double? ffmi,
    @Default(0) int avgHeartRateBpm,
    @Default(0) int heartRateSampleCount,
    DateTime? heartRateSampleStartTime,
    DateTime? heartRateSampleEndTime,
    @Default(0) int restingHeartRateBpm,
    int? restingHeartRateBaselineBpm,
    double? hrvRmssdMs,
    double? hrvBaselineRmssdMs,
    @Default(0) int hrvSampleCount,
    DateTime? hrvSampleStartTime,
    DateTime? hrvSampleEndTime,
    @Default(0.0) double bodyFatPercent,
    double? leanMassKg,
    double? bmrKcal,
    double? boneMassKg,
    double? bodyWaterMassKg,
    double? caloriesInKcal,
    double? proteinGrams,
    double? carbsGrams,
    double? fatGrams,
    double? caffeineGrams,

    /// Pharmacologically active caffeine at load time, in mg. Populated only
    /// when the query date is today — for a past day (or a hidden metric,
    /// missing permission, or failed read) it stays null and the tile falls
    /// back to [caffeineGrams], the day's consumed intake.
    double? activeCaffeineMg,
    int? latestSystolicMmHg,
    int? latestDiastolicMmHg,
    double? latestSpO2Percent,
    double? latestVo2Max,
    double? avgRespiratoryRate,
    double? latestBodyTemperatureCelsius,
    double? latestBloodGlucoseMillimolesPerLiter,
    double? latestSkinTemperatureDeltaCelsius,
    DashboardWeeklyCardioLoad? weeklyCardioLoad,
    DashboardWeeklyIntensityMinutes? weeklyIntensityMinutes,
    int? floorsClimbed,
    double? elevationGainedMeters,
    int? wheelchairPushes,
    int? mindfulnessMinutes,
    int? menstruationPeriodDays,
    int? ovulationTestCount,
    double? latestBasalBodyTemperatureCelsius,
    BodyEnergyTimeline? bodyEnergyTimeline,
    @Default(<String>{}) Set<String> missingPermissions,
    @Default(<DashboardMetric>{}) Set<DashboardMetric> loadedMetrics,
    /// The metrics the installed provider can serve — i.e. every permission the
    /// metric reads is one the device defines. Metrics outside this set get no
    /// tile; the rest always get one, empty or not.
    @Default(<DashboardMetric>{}) Set<DashboardMetric> supportedMetrics,
    @Default(<DashboardMetric, String>{})
    Map<DashboardMetric, String> metricSourcePackages,
    // Derived in Kotlin from caloriesKcal; construct with an explicit value when
    // caloriesKcal > 0. See the port notes.
    @Default(CaloriesBurnedSource.noData) CaloriesBurnedSource caloriesKcalSource,
  }) = _DashboardData;
}

extension DashboardDataMergeLoaded on DashboardData {
  DashboardData mergeLoaded(DashboardData other) {
    bool has(DashboardMetric metric) => other.loadedMetrics.contains(metric);

    final loadsWeight = has(DashboardMetric.weight) ||
        has(DashboardMetric.bmi) ||
        has(DashboardMetric.ffmi);
    final loadsHeight = has(DashboardMetric.height) ||
        has(DashboardMetric.bmi) ||
        has(DashboardMetric.ffmi);

    return copyWith(
      steps: has(DashboardMetric.steps) ? other.steps : steps,
      distanceMeters:
          has(DashboardMetric.distance) ? other.distanceMeters : distanceMeters,
      caloriesKcal:
          has(DashboardMetric.caloriesOut) ? other.caloriesKcal : caloriesKcal,
      caloriesKcalSource: has(DashboardMetric.caloriesOut)
          ? other.caloriesKcalSource
          : caloriesKcalSource,
      activeCaloriesKcal: has(DashboardMetric.activeCalories)
          ? other.activeCaloriesKcal
          : activeCaloriesKcal,
      hydrationLiters: has(DashboardMetric.hydration)
          ? other.hydrationLiters
          : hydrationLiters,
      workout: has(DashboardMetric.workout) ? other.workout : workout,
      workouts: has(DashboardMetric.workout) ? other.workouts : workouts,
      sleep: has(DashboardMetric.sleep) ? other.sleep : sleep,
      sleepScore: has(DashboardMetric.sleep) ? other.sleepScore : sleepScore,
      weightKg: loadsWeight ? other.weightKg : weightKg,
      weightTime: loadsWeight ? other.weightTime : weightTime,
      heightCm: loadsHeight ? other.heightCm : heightCm,
      heightTime: loadsHeight ? other.heightTime : heightTime,
      bmi: has(DashboardMetric.bmi) ? other.bmi : bmi,
      ffmi: has(DashboardMetric.ffmi) ? other.ffmi : ffmi,
      avgHeartRateBpm: has(DashboardMetric.avgHeartRate)
          ? other.avgHeartRateBpm
          : avgHeartRateBpm,
      heartRateSampleCount: has(DashboardMetric.avgHeartRate)
          ? other.heartRateSampleCount
          : heartRateSampleCount,
      heartRateSampleStartTime: has(DashboardMetric.avgHeartRate)
          ? other.heartRateSampleStartTime
          : heartRateSampleStartTime,
      heartRateSampleEndTime: has(DashboardMetric.avgHeartRate)
          ? other.heartRateSampleEndTime
          : heartRateSampleEndTime,
      restingHeartRateBpm: has(DashboardMetric.restingHeartRate)
          ? other.restingHeartRateBpm
          : restingHeartRateBpm,
      restingHeartRateBaselineBpm: has(DashboardMetric.restingHeartRate)
          ? other.restingHeartRateBaselineBpm
          : restingHeartRateBaselineBpm,
      hrvRmssdMs: has(DashboardMetric.hrv) ? other.hrvRmssdMs : hrvRmssdMs,
      hrvBaselineRmssdMs: has(DashboardMetric.hrv)
          ? other.hrvBaselineRmssdMs
          : hrvBaselineRmssdMs,
      hrvSampleCount:
          has(DashboardMetric.hrv) ? other.hrvSampleCount : hrvSampleCount,
      hrvSampleStartTime: has(DashboardMetric.hrv)
          ? other.hrvSampleStartTime
          : hrvSampleStartTime,
      hrvSampleEndTime:
          has(DashboardMetric.hrv) ? other.hrvSampleEndTime : hrvSampleEndTime,
      bodyFatPercent:
          (has(DashboardMetric.bodyFat) || has(DashboardMetric.ffmi))
              ? other.bodyFatPercent
              : bodyFatPercent,
      leanMassKg:
          has(DashboardMetric.leanMass) ? other.leanMassKg : leanMassKg,
      bmrKcal: has(DashboardMetric.bmr) ? other.bmrKcal : bmrKcal,
      boneMassKg:
          has(DashboardMetric.boneMass) ? other.boneMassKg : boneMassKg,
      bodyWaterMassKg: has(DashboardMetric.bodyWaterMass)
          ? other.bodyWaterMassKg
          : bodyWaterMassKg,
      caloriesInKcal: has(DashboardMetric.caloriesIn)
          ? other.caloriesInKcal
          : caloriesInKcal,
      proteinGrams:
          has(DashboardMetric.protein) ? other.proteinGrams : proteinGrams,
      carbsGrams: has(DashboardMetric.carbs) ? other.carbsGrams : carbsGrams,
      fatGrams: has(DashboardMetric.fat) ? other.fatGrams : fatGrams,
      caffeineGrams:
          has(DashboardMetric.caffeine) ? other.caffeineGrams : caffeineGrams,
      activeCaffeineMg: has(DashboardMetric.caffeine)
          ? other.activeCaffeineMg
          : activeCaffeineMg,
      latestSystolicMmHg: has(DashboardMetric.bloodPressure)
          ? other.latestSystolicMmHg
          : latestSystolicMmHg,
      latestDiastolicMmHg: has(DashboardMetric.bloodPressure)
          ? other.latestDiastolicMmHg
          : latestDiastolicMmHg,
      latestSpO2Percent: has(DashboardMetric.spo2)
          ? other.latestSpO2Percent
          : latestSpO2Percent,
      latestVo2Max:
          has(DashboardMetric.vo2Max) ? other.latestVo2Max : latestVo2Max,
      avgRespiratoryRate: has(DashboardMetric.respiratoryRate)
          ? other.avgRespiratoryRate
          : avgRespiratoryRate,
      latestBodyTemperatureCelsius: has(DashboardMetric.bodyTemperature)
          ? other.latestBodyTemperatureCelsius
          : latestBodyTemperatureCelsius,
      latestBloodGlucoseMillimolesPerLiter: has(DashboardMetric.bloodGlucose)
          ? other.latestBloodGlucoseMillimolesPerLiter
          : latestBloodGlucoseMillimolesPerLiter,
      latestSkinTemperatureDeltaCelsius: has(DashboardMetric.skinTemperature)
          ? other.latestSkinTemperatureDeltaCelsius
          : latestSkinTemperatureDeltaCelsius,
      weeklyCardioLoad: has(DashboardMetric.weeklyCardioLoad)
          ? other.weeklyCardioLoad
          : weeklyCardioLoad,
      weeklyIntensityMinutes: has(DashboardMetric.intensityMinutes)
          ? other.weeklyIntensityMinutes
          : weeklyIntensityMinutes,
      floorsClimbed:
          has(DashboardMetric.floors) ? other.floorsClimbed : floorsClimbed,
      elevationGainedMeters: has(DashboardMetric.elevation)
          ? other.elevationGainedMeters
          : elevationGainedMeters,
      wheelchairPushes: has(DashboardMetric.wheelchairPushes)
          ? other.wheelchairPushes
          : wheelchairPushes,
      mindfulnessMinutes: has(DashboardMetric.mindfulness)
          ? other.mindfulnessMinutes
          : mindfulnessMinutes,
      menstruationPeriodDays: has(DashboardMetric.cycle)
          ? other.menstruationPeriodDays
          : menstruationPeriodDays,
      ovulationTestCount: has(DashboardMetric.cycle)
          ? other.ovulationTestCount
          : ovulationTestCount,
      latestBasalBodyTemperatureCelsius: has(DashboardMetric.cycle)
          ? other.latestBasalBodyTemperatureCelsius
          : latestBasalBodyTemperatureCelsius,
      bodyEnergyTimeline: other.bodyEnergyTimeline ?? bodyEnergyTimeline,
      missingPermissions: {...missingPermissions, ...other.missingPermissions},
      loadedMetrics: {...loadedMetrics, ...other.loadedMetrics},
      // Device support is a property of the provider, not of a load pass; both
      // passes report the same set. Prefer the newer one when it has an answer.
      supportedMetrics:
          other.supportedMetrics.isEmpty ? supportedMetrics : other.supportedMetrics,
      metricSourcePackages: {
        ...metricSourcePackages,
        ...other.metricSourcePackages,
      },
    );
  }
}

@freezed
abstract class DashboardWeeklyCardioLoad with _$DashboardWeeklyCardioLoad {
  const DashboardWeeklyCardioLoad._();

  const factory DashboardWeeklyCardioLoad({
    required int currentScore,
    required int targetScore,
    required int todayScore,
    required CardioLoadConfidence confidence,
    required DashboardWeeklyCardioLoadTargetSource targetSource,
  }) = _DashboardWeeklyCardioLoad;

  double get progressFraction => targetScore > 0
      ? (currentScore / targetScore).clamp(0.0, 1.0).toDouble()
      : 0.0;

  int get progressPercent => (progressFraction * 100).round();

  int get todayProgressPercent => targetScore > 0
      ? math.max((todayScore * 100.0 / targetScore).round(), 0)
      : 0;
}

enum DashboardWeeklyCardioLoadTargetSource {
  recentHistory('RECENT_HISTORY'),
  currentPace('CURRENT_PACE');

  const DashboardWeeklyCardioLoadTargetSource(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static DashboardWeeklyCardioLoadTargetSource? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}

@freezed
abstract class DashboardWeeklyIntensityMinutes
    with _$DashboardWeeklyIntensityMinutes {
  const DashboardWeeklyIntensityMinutes._();

  const factory DashboardWeeklyIntensityMinutes({
    required int moderateMinutes,
    required int vigorousMinutes,
    required int moderateEquivalentMinutes,
    @Default(defaultWeeklyIntensityMinutesTarget) int targetMinutes,
    required int todayModerateEquivalentMinutes,
    required int daysElapsed,
    required IntensityMinutesConfidence confidence,
  }) = _DashboardWeeklyIntensityMinutes;

  double get progressFraction => targetMinutes > 0
      ? (moderateEquivalentMinutes / targetMinutes).clamp(0.0, 1.0).toDouble()
      : 0.0;

  int get progressPercent => (progressFraction * 100).round();

  int get expectedByNowMinutes => targetMinutes > 0
      ? math.max((targetMinutes * daysElapsed.clamp(1, 7) / 7.0).round(), 1)
      : 0;

  bool get isOnPace => moderateEquivalentMinutes >= expectedByNowMinutes;
}
