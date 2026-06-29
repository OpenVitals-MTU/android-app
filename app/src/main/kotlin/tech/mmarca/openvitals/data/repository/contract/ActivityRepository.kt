package tech.mmarca.openvitals.data.repository.contract

import java.time.Instant
import java.time.LocalDate
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.domain.query.ActivitiesPeriodData
import tech.mmarca.openvitals.domain.query.ActivityPeriodData
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.domain.model.SpeedSample

interface ActivityRepository {
    suspend fun loadActivityPeriod(
        query: PeriodLoadQuery,
        includeSteps: Boolean,
        includeNutrition: Boolean,
        includeWheelchairPushes: Boolean = false,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): ActivityPeriodData

    suspend fun loadActivitiesPeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode = RefreshMode.NORMAL,
    ): ActivitiesPeriodData

    suspend fun loadDailySteps(start: LocalDate, end: LocalDate): List<DailySteps>

    suspend fun loadActivityProgress(date: LocalDate = LocalDate.now()): List<ActivityProgressPoint>

    suspend fun loadWorkouts(start: LocalDate, end: LocalDate): List<ExerciseData>

    suspend fun loadWorkout(id: String): ExerciseData?

    suspend fun loadSpeedSamples(start: Instant, end: Instant): List<SpeedSample>

    suspend fun loadActivityCadenceSamples(start: Instant, end: Instant): List<ActivityCadenceSample>

    suspend fun loadPlannedWorkouts(start: LocalDate, end: LocalDate): List<PlannedExerciseData>

    suspend fun loadPlannedWorkoutOptions(date: LocalDate, exerciseType: Int): List<PlannedExerciseData>

    suspend fun loadExistingPlannedWorkouts(anchorDate: LocalDate = LocalDate.now()): List<PlannedExerciseData>

    suspend fun writePlannedWorkout(request: PlannedExerciseWriteRequest): String

    suspend fun loadDailyNutrition(start: LocalDate, end: LocalDate): List<DailyNutrition>

    fun activityWritePermissions(): Set<String>

    fun activityWritePermissions(
        includeRoute: Boolean,
        includeDistance: Boolean,
        includeElevation: Boolean,
        includeActiveCalories: Boolean,
        includeTotalCalories: Boolean,
        includeSteps: Boolean = false,
    ): Set<String>

    fun activityWritePermissions(request: ActivityWriteRequest): Set<String>

    fun plannedWorkoutWritePermissions(): Set<String>

    suspend fun hasActivityWritePermission(): Boolean

    suspend fun hasActivityWritePermission(
        includeRoute: Boolean,
        includeDistance: Boolean,
        includeElevation: Boolean,
        includeActiveCalories: Boolean,
        includeTotalCalories: Boolean,
        includeSteps: Boolean = false,
    ): Boolean

    suspend fun hasActivityWritePermission(request: ActivityWriteRequest): Boolean

    suspend fun writeActivityEntry(request: ActivityWriteRequest): String

    suspend fun updateActivityEntry(id: String, request: ActivityWriteRequest)

    suspend fun deleteActivityEntry(id: String)
}
