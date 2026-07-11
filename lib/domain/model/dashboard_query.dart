import 'package:freezed_annotation/freezed_annotation.dart';

import '../../core/time/local_date.dart';
import '../preferences/activity_week_mode.dart';
import '../preferences/sleep_range_mode.dart';
import 'refresh_mode.dart';

part 'dashboard_query.freezed.dart';

@freezed
abstract class DashboardQuery with _$DashboardQuery {
  const factory DashboardQuery.build({
    required LocalDate date,
    required SleepRangeMode sleepRangeMode,
    required ActivityWeekMode activityWeekMode,
    required Set<DashboardMetric> visibleMetrics,
    required RefreshMode refreshMode,
    required bool includeHistoricalBaselines,
    required bool includeWeeklyTrainingSignals,
  }) = _DashboardQuery;

  factory DashboardQuery({
    LocalDate? date,
    SleepRangeMode sleepRangeMode = SleepRangeMode.evening18h,
    ActivityWeekMode activityWeekMode = ActivityWeekMode.mondayToSunday,
    Set<DashboardMetric>? visibleMetrics,
    RefreshMode refreshMode = RefreshMode.normal,
    bool includeHistoricalBaselines = true,
    bool includeWeeklyTrainingSignals = true,
  }) =>
      DashboardQuery.build(
        date: date ?? LocalDate.now(),
        sleepRangeMode: sleepRangeMode,
        activityWeekMode: activityWeekMode,
        visibleMetrics: visibleMetrics ?? DashboardMetric.values.toSet(),
        refreshMode: refreshMode,
        includeHistoricalBaselines: includeHistoricalBaselines,
        includeWeeklyTrainingSignals: includeWeeklyTrainingSignals,
      );
}

enum DashboardMetric {
  steps('STEPS'),
  distance('DISTANCE'),
  caloriesOut('CALORIES_OUT'),
  activeCalories('ACTIVE_CALORIES'),
  floors('FLOORS'),
  elevation('ELEVATION'),
  wheelchairPushes('WHEELCHAIR_PUSHES'),
  workout('WORKOUT'),
  sleep('SLEEP'),
  hydration('HYDRATION'),
  caloriesIn('CALORIES_IN'),
  protein('PROTEIN'),
  carbs('CARBS'),
  fat('FAT'),
  caffeine('CAFFEINE'),
  weight('WEIGHT'),
  height('HEIGHT'),
  bmi('BMI'),
  ffmi('FFMI'),
  bodyFat('BODY_FAT'),
  leanMass('LEAN_MASS'),
  bmr('BMR'),
  boneMass('BONE_MASS'),
  bodyWaterMass('BODY_WATER_MASS'),
  avgHeartRate('AVG_HEART_RATE'),
  bodyEnergy('BODY_ENERGY'),
  restingHeartRate('RESTING_HEART_RATE'),
  hrv('HRV'),
  bloodPressure('BLOOD_PRESSURE'),
  spo2('SPO2'),
  vo2Max('VO2_MAX'),
  respiratoryRate('RESPIRATORY_RATE'),
  bodyTemperature('BODY_TEMPERATURE'),
  bloodGlucose('BLOOD_GLUCOSE'),
  skinTemperature('SKIN_TEMPERATURE'),
  weeklyCardioLoad('WEEKLY_CARDIO_LOAD'),
  intensityMinutes('INTENSITY_MINUTES'),
  mindfulness('MINDFULNESS'),
  cycle('CYCLE');

  const DashboardMetric(this.storageName);

  /// Original Kotlin `.name` used for persistence round-trips.
  final String storageName;

  static DashboardMetric? fromStorage(String value) {
    for (final entry in values) {
      if (entry.storageName == value) return entry;
    }
    return null;
  }
}
