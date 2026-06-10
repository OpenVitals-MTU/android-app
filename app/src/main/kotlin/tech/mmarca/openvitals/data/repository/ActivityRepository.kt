package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
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
    private val preferencesRepository: PreferencesRepository? = null,
) {

    companion object {
        private const val TAG = "ActivityRepository"
    }

    private val readStepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val readDistancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    private val readHealthDataHistoryPermission = HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY
    private val readExercisePermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val readCaloriesPermission = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    private val readFloorsPermission = HealthPermission.getReadPermission(FloorsClimbedRecord::class)
    private val readActiveCaloriesPermission = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val readBmrPermission = HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
    private val readElevationPermission = HealthPermission.getReadPermission(ElevationGainedRecord::class)
    private val readWheelchairPushesPermission = HealthPermission.getReadPermission(WheelchairPushesRecord::class)
    private val readSpeedPermission = HealthPermission.getReadPermission(SpeedRecord::class)
    private val readPowerPermission = HealthPermission.getReadPermission(PowerRecord::class)
    private val readStepsCadencePermission = HealthPermission.getReadPermission(StepsCadenceRecord::class)
    private val readCyclingCadencePermission = HealthPermission.getReadPermission(CyclingPedalingCadenceRecord::class)
    private val readPlannedExercisePermission = HealthPermission.getReadPermission(PlannedExerciseSessionRecord::class)
    private val writeExercisePermission = HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    private val writeDistancePermission = HealthPermission.getWritePermission(DistanceRecord::class)
    private val writeElevationPermission = HealthPermission.getWritePermission(ElevationGainedRecord::class)
    private val writeActiveCaloriesPermission = HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class)
    private val writeTotalCaloriesPermission = HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
    private val writeStepsPermission = HealthPermission.getWritePermission(StepsRecord::class)
    private val writeExerciseRoutePermission = HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    suspend fun loadActivityPeriod(
        query: PeriodLoadQuery,
        includeSteps: Boolean,
        includeNutrition: Boolean,
        includeWheelchairPushes: Boolean = false,
    ): ActivityPeriodData = coroutineScope {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        val dailySteps = async {
            if (includeSteps || includeWheelchairPushes) {
                loadDailySteps(
                    start = windows.current.start,
                    end = windows.current.end,
                    granted = granted,
                    includeSteps = includeSteps,
                    includeWheelchairPushes = includeWheelchairPushes,
                )
            } else {
                emptyList()
            }
        }
        val previousDailySteps = async {
            if (includeSteps || includeWheelchairPushes) {
                loadDailySteps(
                    start = windows.previous.start,
                    end = windows.previous.end,
                    granted = granted,
                    includeSteps = includeSteps,
                    includeWheelchairPushes = includeWheelchairPushes,
                )
            } else {
                emptyList()
            }
        }
        val baselineDailySteps = async {
            if (includeSteps || includeWheelchairPushes) {
                loadDailySteps(
                    start = windows.baseline.start,
                    end = windows.baseline.end,
                    granted = granted,
                    includeSteps = includeSteps,
                    includeWheelchairPushes = includeWheelchairPushes,
                )
            } else {
                emptyList()
            }
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
            if (query.range == TimeRange.DAY && (includeSteps || includeWheelchairPushes)) {
                loadActivityProgress(
                    date = windows.current.start,
                    granted = granted,
                    includeSteps = includeSteps,
                    includeWheelchairPushes = includeWheelchairPushes,
                )
            } else {
                emptyList()
            }
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
        val plannedWorkouts = async { loadPlannedWorkouts(windows.current.start, windows.current.end, granted) }
        ActivitiesPeriodData(
            workouts = workouts.await(),
            previousWorkouts = previousWorkouts.await(),
            baselineWorkouts = baselineWorkouts.await(),
            plannedWorkouts = plannedWorkouts.await(),
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
        includeSteps: Boolean = true,
        includeWheelchairPushes: Boolean = false,
    ): List<DailySteps> {
        val missingRequired = buildSet {
            if (includeSteps && readStepsPermission !in granted) add(readStepsPermission)
            if (includeWheelchairPushes && readWheelchairPushesPermission !in granted) add(readWheelchairPushesPermission)
        }
        if (missingRequired.isNotEmpty()) {
            Log.w(TAG, "Skipping loadDailySteps missingCount=${missingRequired.size}")
            return emptyList()
        }
        val effectiveStart = activityHistoryStart(start, end, granted)
        return hc.readDailySteps(
            startDate = effectiveStart,
            endDate = end,
            includeSteps = includeSteps,
            includeDistance = readDistancePermission in granted,
            includeWheelchairPushes = includeWheelchairPushes && readWheelchairPushesPermission in granted,
            includeFloors = readFloorsPermission in granted,
            includeActiveCalories = readActiveCaloriesPermission in granted,
            includeElevation = readElevationPermission in granted,
        )
    }

    private fun activityHistoryStart(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): LocalDate {
        val historyPermissionRequired = readHealthDataHistoryPermission in hc.additionalDataAccessPermissions
        return if (historyPermissionRequired && readHealthDataHistoryPermission !in granted) {
            maxOf(start, end.minusDays(29))
        } else {
            start
        }
    }

    suspend fun loadActivityProgress(date: LocalDate = LocalDate.now()): List<ActivityProgressPoint> {
        val granted = grantedPermissionsIfAvailable()
        return loadActivityProgress(date, granted)
    }

    private suspend fun loadActivityProgress(
        date: LocalDate,
        granted: Set<String>,
        includeSteps: Boolean = true,
        includeWheelchairPushes: Boolean = false,
    ): List<ActivityProgressPoint> {
        val missingRequired = buildSet {
            if (includeSteps && readStepsPermission !in granted) add(readStepsPermission)
            if (includeWheelchairPushes && readWheelchairPushesPermission !in granted) add(readWheelchairPushesPermission)
        }
        if (missingRequired.isNotEmpty()) {
            Log.w(TAG, "Skipping loadActivityProgress missingCount=${missingRequired.size}")
            return emptyList()
        }
        return hc.readActivityProgress(
            date = date,
            includeSteps = includeSteps,
            includeDistance = readDistancePermission in granted,
            includeCalories = readCaloriesPermission in granted,
            includeActiveCalories = readActiveCaloriesPermission in granted,
            includeCaloriesEstimate = canEstimateTotalCalories(granted),
            includeWheelchairPushes = includeWheelchairPushes && readWheelchairPushesPermission in granted,
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
            includeTotalCaloriesEstimate = canEstimateTotalCalories(granted),
            includeWheelchairPushes = readWheelchairPushesPermission in granted,
            includeFloors = readFloorsPermission in granted,
            includeElevation = readElevationPermission in granted,
            includeSpeed = readSpeedPermission in granted,
            includePower = readPowerPermission in granted,
            includeStepsCadence = readStepsCadencePermission in granted,
            includeCyclingCadence = readCyclingCadencePermission in granted,
        )
    }

    suspend fun loadPlannedWorkouts(start: LocalDate, end: LocalDate): List<PlannedExerciseData> {
        val granted = grantedPermissionsIfAvailable()
        return loadPlannedWorkouts(start, end, granted)
    }

    private suspend fun loadPlannedWorkouts(
        start: LocalDate,
        end: LocalDate,
        granted: Set<String>,
    ): List<PlannedExerciseData> {
        if (!hc.isPlannedExerciseAvailable() || readPlannedExercisePermission !in granted) {
            Log.w(TAG, "Skipping loadPlannedWorkouts missingCount=1")
            return emptyList()
        }
        val zone = ZoneId.systemDefault()
        val startInstant = start.atStartOfDay(zone).toInstant()
        val endInstant = end.plusDays(1).atStartOfDay(zone).toInstant()
        return hc.readPlannedExerciseSessions(startInstant, endInstant)
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
        return hc.readDailyNutrition(
            startDate = start,
            endDate = end,
            includeHydration = false,
            includeEstimatedCalories = canEstimateTotalCalories(granted),
        )
    }

    private fun canEstimateTotalCalories(granted: Set<String>): Boolean =
        preferencesRepository?.showOpenVitalsCalculatedCalories == true &&
            readActiveCaloriesPermission in granted &&
            readBmrPermission in granted

    fun activityWritePermissions(): Set<String> =
        activityWritePermissions(
            includeRoute = true,
            includeDistance = true,
            includeElevation = true,
            includeActiveCalories = true,
            includeTotalCalories = true,
            includeSteps = false,
        )

    fun activityWritePermissions(
        includeRoute: Boolean,
        includeDistance: Boolean,
        includeElevation: Boolean,
        includeActiveCalories: Boolean,
        includeTotalCalories: Boolean,
        includeSteps: Boolean = false,
    ): Set<String> = buildSet {
        add(writeExercisePermission)
        if (includeRoute) add(writeExerciseRoutePermission)
        if (includeDistance) add(writeDistancePermission)
        if (includeElevation) add(writeElevationPermission)
        if (includeActiveCalories) add(writeActiveCaloriesPermission)
        if (includeTotalCalories) add(writeTotalCaloriesPermission)
        if (includeSteps) add(writeStepsPermission)
    }

    fun activityWritePermissions(request: ActivityWriteRequest): Set<String> =
        activityWritePermissions(
            includeRoute = request.routePoints.isNotEmpty(),
            includeDistance = request.distanceMeters != null,
            includeElevation = request.elevationGainedMeters != null,
            includeActiveCalories = request.activeCaloriesKcal != null,
            includeTotalCalories = request.totalCaloriesKcal != null,
            includeSteps = request.stepsCount != null,
        )

    suspend fun hasActivityWritePermission(): Boolean =
        hasActivityWritePermission(
            includeRoute = true,
            includeDistance = true,
            includeElevation = true,
            includeActiveCalories = true,
            includeTotalCalories = true,
            includeSteps = false,
        )

    suspend fun hasActivityWritePermission(
        includeRoute: Boolean,
        includeDistance: Boolean,
        includeElevation: Boolean,
        includeActiveCalories: Boolean,
        includeTotalCalories: Boolean,
        includeSteps: Boolean = false,
    ): Boolean {
        val required = activityWritePermissions(
            includeRoute = includeRoute,
            includeDistance = includeDistance,
            includeElevation = includeElevation,
            includeActiveCalories = includeActiveCalories,
            includeTotalCalories = includeTotalCalories,
            includeSteps = includeSteps,
        )
        return required.all { permission -> permission in grantedPermissionsIfAvailable() }
    }

    suspend fun hasActivityWritePermission(request: ActivityWriteRequest): Boolean =
        activityWritePermissions(request).all { permission -> permission in grantedPermissionsIfAvailable() }

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

    suspend fun deleteActivityEntry(id: String) {
        val granted = grantedPermissionsIfAvailable()
        if (writeExercisePermission !in granted) {
            Log.w(TAG, "Skipping deleteActivityEntry missingCount=1")
            throw SecurityException("Missing Health Connect activity write permission.")
        }
        hc.deleteActivityEntry(id)
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
    val plannedWorkouts: List<PlannedExerciseData> = emptyList(),
)
