package tech.mmarca.openvitals.domain.query

import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseData

data class ActivityPeriodData(
    val dailySteps: List<DailySteps> = emptyList(),
    val previousDailySteps: List<DailySteps> = emptyList(),
    val baselineDailySteps: List<DailySteps> = emptyList(),
    val nutrition: List<DailyNutrition> = emptyList(),
    val previousNutrition: List<DailyNutrition> = emptyList(),
    val baselineNutrition: List<DailyNutrition> = emptyList(),
    val activityProgress: List<ActivityProgressPoint> = emptyList(),
)

data class ActivitiesPeriodData(
    val workouts: List<ExerciseData> = emptyList(),
    val previousWorkouts: List<ExerciseData> = emptyList(),
    val baselineWorkouts: List<ExerciseData> = emptyList(),
    val plannedWorkouts: List<PlannedExerciseData> = emptyList(),
)
