package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import tech.mmarca.openvitals.data.model.ActivityProgressPoint
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val hc: HealthConnectManager,
) {

    companion object {
        private const val TAG = "ActivityRepository"
    }

    private val readStepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val readDistancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    private val readExercisePermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val readCaloriesPermission = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    private val readFloorsPermission = HealthPermission.getReadPermission(FloorsClimbedRecord::class)
    private val readActiveCaloriesPermission = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val readElevationPermission = HealthPermission.getReadPermission(ElevationGainedRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadActivityPeriod(query: PeriodLoadQuery, includeSteps: Boolean, includeNutrition: Boolean): ActivityPeriodData {
        val windows = query.windows
        return ActivityPeriodData(
            dailySteps = if (includeSteps) loadDailySteps(windows.current.start, windows.current.end) else emptyList(),
            previousDailySteps = if (includeSteps) loadDailySteps(windows.previous.start, windows.previous.end) else emptyList(),
            baselineDailySteps = if (includeSteps) loadDailySteps(windows.baseline.start, windows.baseline.end) else emptyList(),
            nutrition = if (includeNutrition) loadDailyNutrition(windows.current.start, windows.current.end) else emptyList(),
            previousNutrition = if (includeNutrition) loadDailyNutrition(windows.previous.start, windows.previous.end) else emptyList(),
            baselineNutrition = if (includeNutrition) loadDailyNutrition(windows.baseline.start, windows.baseline.end) else emptyList(),
            activityProgress = if (query.range == TimeRange.DAY) loadActivityProgress(windows.current.start) else emptyList(),
        )
    }

    suspend fun loadActivitiesPeriod(query: PeriodLoadQuery): ActivitiesPeriodData {
        val windows = query.windows
        return ActivitiesPeriodData(
            workouts = loadWorkouts(windows.current.start, windows.current.end),
            previousWorkouts = loadWorkouts(windows.previous.start, windows.previous.end),
            baselineWorkouts = loadWorkouts(windows.baseline.start, windows.baseline.end),
        )
    }

    suspend fun loadDailySteps(start: LocalDate, end: LocalDate): List<DailySteps> {
        val granted = grantedPermissionsIfAvailable()
        if (readStepsPermission !in granted || readDistancePermission !in granted) {
            Log.w(TAG, "Skipping loadDailySteps start=$start end=$end missing=${listOf(readStepsPermission, readDistancePermission).filterNot { it in granted }}")
            return emptyList()
        }
        return hc.readDailySteps(
            startDate = start,
            endDate = end,
            includeFloors = readFloorsPermission in granted,
            includeActiveCalories = readActiveCaloriesPermission in granted,
            includeElevation = readElevationPermission in granted,
        )
    }

    suspend fun loadActivityProgress(date: LocalDate = LocalDate.now()): List<ActivityProgressPoint> {
        val granted = grantedPermissionsIfAvailable()
        if (readStepsPermission !in granted) {
            Log.w(TAG, "Skipping loadActivityProgress date=$date missing=$readStepsPermission")
            return emptyList()
        }
        return hc.readActivityProgress(
            date = date,
            includeDistance = readDistancePermission in granted,
            includeCalories = readCaloriesPermission in granted,
            includeActiveCalories = readActiveCaloriesPermission in granted,
            includeFloors = readFloorsPermission in granted,
            includeElevation = readElevationPermission in granted,
        )
    }

    suspend fun loadWorkouts(start: LocalDate, end: LocalDate): List<ExerciseData> {
        val granted = grantedPermissionsIfAvailable()
        if (readExercisePermission !in granted) {
            Log.w(TAG, "Skipping loadWorkouts start=$start end=$end missing=$readExercisePermission")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readExerciseSessions(startInstant, endInstant)
    }

    suspend fun loadWorkout(id: String): ExerciseData? {
        val granted = grantedPermissionsIfAvailable()
        if (readExercisePermission !in granted) {
            Log.w(TAG, "Skipping loadWorkout id=$id missing=$readExercisePermission")
            return null
        }
        return hc.readExerciseSession(
            id = id,
            includeSteps = readStepsPermission in granted,
            includeDistance = readDistancePermission in granted,
            includeTotalCalories = readCaloriesPermission in granted,
            includeActiveCalories = readActiveCaloriesPermission in granted,
            includeFloors = readFloorsPermission in granted,
            includeElevation = readElevationPermission in granted,
        )
    }

    suspend fun loadDailyNutrition(start: LocalDate, end: LocalDate): List<DailyNutrition> {
        val granted = grantedPermissionsIfAvailable()
        if (readCaloriesPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyNutrition start=$start end=$end missing=$readCaloriesPermission")
            return emptyList()
        }
        return hc.readDailyNutrition(start, end, includeHydration = false)
    }
}

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
)
