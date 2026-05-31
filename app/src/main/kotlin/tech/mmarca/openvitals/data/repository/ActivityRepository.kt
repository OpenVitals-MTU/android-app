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
import tech.mmarca.openvitals.data.model.ActivityWriteRequest
import tech.mmarca.openvitals.data.model.ActivityProgressPoint
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import tech.mmarca.openvitals.healthconnect.HealthConnectQueryCache
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class ActivityRepository @Inject constructor(
    private val hc: HealthConnectManager,
    private val queryCache: HealthConnectQueryCache = HealthConnectQueryCache(),
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
    private val writeExercisePermission = HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    private val writeDistancePermission = HealthPermission.getWritePermission(DistanceRecord::class)
    private val writeElevationPermission = HealthPermission.getWritePermission(ElevationGainedRecord::class)
    private val writeActiveCaloriesPermission = HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class)
    private val writeTotalCaloriesPermission = HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
    private val writeExerciseRoutePermission = HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadActivityPeriod(query: PeriodLoadQuery, includeSteps: Boolean, includeNutrition: Boolean): ActivityPeriodData = coroutineScope {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        val dailySteps = async {
            if (includeSteps) loadDailySteps(windows.current.start, windows.current.end, granted) else emptyList()
        }
        val previousDailySteps = async {
            if (includeSteps) loadDailySteps(windows.previous.start, windows.previous.end, granted) else emptyList()
        }
        val baselineDailySteps = async {
            if (includeSteps) loadDailySteps(windows.baseline.start, windows.baseline.end, granted) else emptyList()
        }
        val nutrition = async {
            if (includeNutrition) loadDailyNutrition(windows.current.start, windows.current.end, granted) else emptyList()
        }
        val previousNutrition = async {
            if (includeNutrition) loadDailyNutrition(windows.previous.start, windows.previous.end, granted) else emptyList()
        }
        val baselineNutrition = async {
            if (includeNutrition) loadDailyNutrition(windows.baseline.start, windows.baseline.end, granted) else emptyList()
        }
        val activityProgress = async {
            if (query.range == TimeRange.DAY) loadActivityProgress(windows.current.start, granted) else emptyList()
        }
        ActivityPeriodData(
            dailySteps = dailySteps.await(),
            previousDailySteps = previousDailySteps.await(),
            baselineDailySteps = baselineDailySteps.await(),
            nutrition = nutrition.await(),
            previousNutrition = previousNutrition.await(),
            baselineNutrition = baselineNutrition.await(),
            activityProgress = activityProgress.await(),
        )
    }

    suspend fun loadActivitiesPeriod(query: PeriodLoadQuery): ActivitiesPeriodData = coroutineScope {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        val workouts = async { loadWorkouts(windows.current.start, windows.current.end, granted) }
        val previousWorkouts = async { loadWorkouts(windows.previous.start, windows.previous.end, granted) }
        val baselineWorkouts = async { loadWorkouts(windows.baseline.start, windows.baseline.end, granted) }
        ActivitiesPeriodData(
            workouts = workouts.await(),
            previousWorkouts = previousWorkouts.await(),
            baselineWorkouts = baselineWorkouts.await(),
        )
    }

    suspend fun loadDailySteps(start: LocalDate, end: LocalDate): List<DailySteps> {
        val granted = grantedPermissionsIfAvailable()
        return loadDailySteps(start, end, granted)
    }

    private suspend fun loadDailySteps(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<DailySteps> {
        val missing = listOf(readStepsPermission, readDistancePermission).filterNot { it in granted }
        if (missing.isNotEmpty()) {
            Log.w(TAG, "Skipping loadDailySteps missingCount=${missing.size}")
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
        return loadActivityProgress(date, granted)
    }

    private suspend fun loadActivityProgress(
        date: LocalDate,
        granted: Set<String>,
    ): List<ActivityProgressPoint> {
        if (readStepsPermission !in granted) {
            Log.w(TAG, "Skipping loadActivityProgress missingCount=1")
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
        return loadWorkouts(start, end, granted)
    }

    private suspend fun loadWorkouts(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<ExerciseData> {
        if (readExercisePermission !in granted) {
            Log.w(TAG, "Skipping loadWorkouts missingCount=1")
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
            Log.w(TAG, "Skipping loadWorkout missingCount=1")
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
        return loadDailyNutrition(start, end, granted)
    }

    private suspend fun loadDailyNutrition(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<DailyNutrition> {
        if (readCaloriesPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyNutrition missingCount=1")
            return emptyList()
        }
        return hc.readDailyNutrition(start, end, includeHydration = false)
    }

    fun activityWritePermissions(): Set<String> =
        activityWritePermissions(
            includeRoute = true,
            includeDistance = true,
            includeElevation = true,
            includeActiveCalories = true,
            includeTotalCalories = true,
        )

    fun activityWritePermissions(
        includeRoute: Boolean,
        includeDistance: Boolean,
        includeElevation: Boolean,
        includeActiveCalories: Boolean,
        includeTotalCalories: Boolean,
    ): Set<String> = buildSet {
        add(writeExercisePermission)
        if (includeRoute) add(writeExerciseRoutePermission)
        if (includeDistance) add(writeDistancePermission)
        if (includeElevation) add(writeElevationPermission)
        if (includeActiveCalories) add(writeActiveCaloriesPermission)
        if (includeTotalCalories) add(writeTotalCaloriesPermission)
    }

    fun activityWritePermissions(request: ActivityWriteRequest): Set<String> =
        activityWritePermissions(
            includeRoute = request.routePoints.isNotEmpty(),
            includeDistance = request.distanceMeters != null,
            includeElevation = request.elevationGainedMeters != null,
            includeActiveCalories = request.activeCaloriesKcal != null,
            includeTotalCalories = request.totalCaloriesKcal != null,
        )

    suspend fun hasActivityWritePermission(): Boolean =
        hasActivityWritePermission(
            includeRoute = true,
            includeDistance = true,
            includeElevation = true,
            includeActiveCalories = true,
            includeTotalCalories = true,
        )

    suspend fun hasActivityWritePermission(
        includeRoute: Boolean,
        includeDistance: Boolean,
        includeElevation: Boolean,
        includeActiveCalories: Boolean,
        includeTotalCalories: Boolean,
    ): Boolean {
        val required = activityWritePermissions(
            includeRoute = includeRoute,
            includeDistance = includeDistance,
            includeElevation = includeElevation,
            includeActiveCalories = includeActiveCalories,
            includeTotalCalories = includeTotalCalories,
        )
        return required.all { permission -> permission in grantedPermissionsIfAvailable() }
    }

    suspend fun writeActivityEntry(request: ActivityWriteRequest): String {
        val missingPermissions = activityWritePermissions(request) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping writeActivityEntry missingCount=${missingPermissions.size}")
            throw SecurityException("Missing Health Connect activity write permission.")
        }
        return hc.writeActivityEntry(request).also {
            queryCache.invalidateOperations("dashboard")
        }
    }

    suspend fun updateActivityEntry(id: String, request: ActivityWriteRequest) {
        val missingPermissions = activityWritePermissions(request) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping updateActivityEntry missingCount=${missingPermissions.size}")
            throw SecurityException("Missing Health Connect activity write permission.")
        }
        hc.updateActivityEntry(id, request)
        queryCache.invalidateOperations("dashboard")
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
