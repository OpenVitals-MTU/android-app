import 'package:freezed_annotation/freezed_annotation.dart';

import '../model/activity_models.dart';
import '../model/nutrition_models.dart';

part 'activity_period_data.freezed.dart';

@freezed
abstract class ActivityPeriodData with _$ActivityPeriodData {
  const factory ActivityPeriodData({
    @Default(<DailySteps>[]) List<DailySteps> dailySteps,
    @Default(<DailySteps>[]) List<DailySteps> previousDailySteps,
    @Default(<DailySteps>[]) List<DailySteps> baselineDailySteps,
    @Default(<DailyNutrition>[]) List<DailyNutrition> nutrition,
    @Default(<DailyNutrition>[]) List<DailyNutrition> previousNutrition,
    @Default(<DailyNutrition>[]) List<DailyNutrition> baselineNutrition,
    @Default(<ActivityProgressPoint>[])
    List<ActivityProgressPoint> activityProgress,
  }) = _ActivityPeriodData;
}

@freezed
abstract class ActivitiesPeriodData with _$ActivitiesPeriodData {
  const factory ActivitiesPeriodData({
    @Default(<ExerciseData>[]) List<ExerciseData> workouts,
    @Default(<ExerciseData>[]) List<ExerciseData> previousWorkouts,
    @Default(<ExerciseData>[]) List<ExerciseData> baselineWorkouts,
    @Default(<PlannedExerciseData>[])
    List<PlannedExerciseData> plannedWorkouts,
  }) = _ActivitiesPeriodData;
}
