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
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.PlannedExerciseSessionRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest
import tech.mmarca.openvitals.domain.query.ActivitiesPeriodData
import tech.mmarca.openvitals.domain.query.ActivityPeriodData
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

@Singleton
class ActivityRepositoryImpl @Inject constructor(
    private val hc: HealthConnectManager,
    private val preferencesRepository: PreferencesRepository? = null,
    private val markerRepository: ActivityMarkerRepository? = null,
) : ActivityRepository {

    companion object {
        private const val TAG = "ActivityRepositoryImpl"
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
    private val readHeartRatePermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val readPlannedExercisePermission = HealthPermission.getReadPermission(PlannedExerciseSessionRecord::class)
    private val writePlannedExercisePermission = HealthPermission.getWritePermission(PlannedExerciseSessionRecord::class)
    private val writeExercisePermission = HealthPermission.getWritePermission(ExerciseSessionRecord::class)
    private val writeDistancePermission = HealthPermission.getWritePermission(DistanceRecord::class)
    private val writeElevationPermission = HealthPermission.getWritePermission(ElevationGainedRecord::class)
    private val writeActiveCaloriesPermission = HealthPermission.getWritePermission(ActiveCaloriesBurnedRecord::class)
    private val writeTotalCaloriesPermission = HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class)
    private val writeStepsPermission = HealthPermission.getWritePermission(StepsRecord::class)
    private val writeHeartRatePermission = HealthPermission.getWritePermission(HeartRateRecord::class)
    private val writePowerPermission = HealthPermission.getWritePermission(PowerRecord::class)
    private val writeSpeedPermission = HealthPermission.getWritePermission(SpeedRecord::class)
    private val writeStepsCadencePermission = HealthPermission.getWritePermission(StepsCadenceRecord::class)
    private val writeCyclingCadencePermission = HealthPermission.getWritePermission(CyclingPedalingCadenceRecord::class)
    private val writeExerciseRoutePermission = HealthPermission.PERMISSION_WRITE_EXERCISE_ROUTE

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

    @Suppress("UNUSED_PARAMETER")
    override suspend fun loadActivityPeriod(
        query: PeriodLoadQuery,
        includeSteps: Boolean,
        includeNutrition: Boolean,
        includeWheelchairPushes: Boolean,
        refreshMode: RefreshMode,
    ): ActivityPeriodData {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        return coroutineScope {
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
                if (includeNutrition) {
                    loadDailyNutrition(windows.current.start, windows.current.end, granted)
                } else {
                    emptyList()
                }
            }
            val previousNutrition = async {
                if (includeNutrition) {
                    loadDailyNutrition(windows.previous.start, windows.previous.end, granted)
                } else {
                    emptyList()
                }
            }
            val baselineNutrition = async {
                if (includeNutrition) {
                    loadDailyNutrition(windows.baseline.start, windows.baseline.end, granted)
                } else {
                    emptyList()
                }
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
    }

    @Suppress("UNUSED_PARAMETER")
    override suspend fun loadActivitiesPeriod(
        query: PeriodLoadQuery,
        refreshMode: RefreshMode,
    ): ActivitiesPeriodData {
        val windows = query.windows
        val granted = grantedPermissionsIfAvailable()
        return coroutineScope {
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
    }

    override suspend fun loadDailySteps(start: LocalDate, end: LocalDate): List<DailySteps> {
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

    override suspend fun loadActivityProgress(date: LocalDate): List<ActivityProgressPoint> {
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
        return hc.readRawActivityProgress(
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

    override suspend fun loadWorkouts(start: LocalDate, end: LocalDate): List<ExerciseData> {
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

    override suspend fun loadWorkout(id: String): ExerciseData? {
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
            includeHeartRate = readHeartRatePermission in granted,
        )
    }

    override suspend fun loadSpeedSamples(start: Instant, end: Instant): List<SpeedSample> {
        val granted = grantedPermissionsIfAvailable()
        return loadSpeedSamples(start, end, granted)
    }

    private suspend fun loadSpeedSamples(
        start: Instant,
        end: Instant,
        granted: Set<String>,
    ): List<SpeedSample> {
        if (readSpeedPermission !in granted) {
            Log.w(TAG, "Skipping loadSpeedSamples missingCount=1")
            return emptyList()
        }
        return hc.readSpeedSamples(start, end)
    }

    override suspend fun loadActivityCadenceSamples(start: Instant, end: Instant): List<ActivityCadenceSample> {
        val granted = grantedPermissionsIfAvailable()
        return loadActivityCadenceSamples(start, end, granted)
    }

    private suspend fun loadActivityCadenceSamples(
        start: Instant,
        end: Instant,
        granted: Set<String>,
    ): List<ActivityCadenceSample> {
        if (readStepsCadencePermission !in granted && readCyclingCadencePermission !in granted) {
            Log.w(TAG, "Skipping loadActivityCadenceSamples missingCount=1")
            return emptyList()
        }
        return hc.readActivityCadenceSamples(start, end).filter { sample ->
            when (sample.kind) {
                ActivityCadenceKind.CYCLING -> readCyclingCadencePermission in granted
                ActivityCadenceKind.STEPS -> readStepsCadencePermission in granted
            }
        }
    }

    private fun BleRecordingSampleBuffer.writePermissions(): Set<String> = buildSet {
        if (heartRateSamples.isNotEmpty()) add(writeHeartRatePermission)
        if (powerSamples.isNotEmpty()) add(writePowerPermission)
        if (speedSamples.isNotEmpty()) add(writeSpeedPermission)
        if (cyclingCadenceSamples.isNotEmpty()) add(writeCyclingCadencePermission)
        if (stepsCadenceSamples.isNotEmpty()) add(writeStepsCadencePermission)
    }

    override suspend fun loadPlannedWorkouts(start: LocalDate, end: LocalDate): List<PlannedExerciseData> {
        val granted = grantedPermissionsIfAvailable()
        return loadPlannedWorkouts(start, end, granted)
    }

    override suspend fun loadPlannedWorkoutOptions(date: LocalDate, exerciseType: Int): List<PlannedExerciseData> =
        loadPlannedWorkouts(date, date)
            .filter { plan -> plan.exerciseType == exerciseType && plan.completedExerciseSessionId == null }

    override suspend fun loadExistingPlannedWorkouts(anchorDate: LocalDate): List<PlannedExerciseData> {
        val granted = grantedPermissionsIfAvailable()
        if (!hc.isPlannedExerciseAvailable() || readPlannedExercisePermission !in granted) {
            Log.w(TAG, "Skipping loadExistingPlannedWorkouts missingCount=1")
            throw SecurityException("Missing Health Connect planned exercise read permission.")
        }
        return loadPlannedWorkouts(
            start = anchorDate.minusYears(1),
            end = anchorDate.plusYears(1),
            granted = granted,
        ).filter { plan -> plan.completedExerciseSessionId == null }
    }

    override suspend fun writePlannedWorkout(request: PlannedExerciseWriteRequest): String {
        val granted = grantedPermissionsIfAvailable()
        if (!hc.isPlannedExerciseAvailable() || writePlannedExercisePermission !in granted) {
            Log.w(TAG, "Skipping writePlannedWorkout missingCount=1")
            throw SecurityException("Missing Health Connect planned exercise write permission.")
        }
        return hc.writePlannedExerciseSession(request)
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

    override suspend fun loadDailyNutrition(start: LocalDate, end: LocalDate): List<DailyNutrition> {
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

    override fun activityWritePermissions(): Set<String> =
        activityWritePermissions(
            includeRoute = true,
            includeDistance = true,
            includeElevation = true,
            includeActiveCalories = true,
            includeTotalCalories = true,
            includeSteps = false,
        )

    override fun activityWritePermissions(
        includeRoute: Boolean,
        includeDistance: Boolean,
        includeElevation: Boolean,
        includeActiveCalories: Boolean,
        includeTotalCalories: Boolean,
        includeSteps: Boolean,
    ): Set<String> = buildSet {
        add(writeExercisePermission)
        if (includeRoute) add(writeExerciseRoutePermission)
        if (includeDistance) add(writeDistancePermission)
        if (includeElevation) add(writeElevationPermission)
        if (includeActiveCalories) add(writeActiveCaloriesPermission)
        if (includeTotalCalories) add(writeTotalCaloriesPermission)
        if (includeSteps) add(writeStepsPermission)
    }

    override fun activityWritePermissions(request: ActivityWriteRequest): Set<String> =
        activityWritePermissions(
            includeRoute = request.routePoints.isNotEmpty(),
            includeDistance = request.distanceMeters != null,
            includeElevation = request.elevationGainedMeters != null,
            includeActiveCalories = request.activeCaloriesKcal != null,
            includeTotalCalories = request.totalCaloriesKcal != null,
            includeSteps = request.stepsCount != null,
        ) + if (request.plannedExerciseSessionId != null && hc.isPlannedExerciseAvailable()) {
            setOf(readPlannedExercisePermission)
        } else {
            emptySet()
        } + request.bleSamples.writePermissions()

    override fun plannedWorkoutWritePermissions(): Set<String> =
        if (hc.isPlannedExerciseAvailable()) {
            setOf(readPlannedExercisePermission, writePlannedExercisePermission)
        } else {
            emptySet()
        }

    override suspend fun hasActivityWritePermission(): Boolean =
        hasActivityWritePermission(
            includeRoute = true,
            includeDistance = true,
            includeElevation = true,
            includeActiveCalories = true,
            includeTotalCalories = true,
            includeSteps = false,
        )

    override suspend fun hasActivityWritePermission(
        includeRoute: Boolean,
        includeDistance: Boolean,
        includeElevation: Boolean,
        includeActiveCalories: Boolean,
        includeTotalCalories: Boolean,
        includeSteps: Boolean,
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

    override suspend fun hasActivityWritePermission(request: ActivityWriteRequest): Boolean =
        activityWritePermissions(request).all { permission -> permission in grantedPermissionsIfAvailable() }

    override suspend fun writeActivityEntry(request: ActivityWriteRequest): String {
        val missingPermissions = activityWritePermissions(request) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping writeActivityEntry missingCount=${missingPermissions.size}")
            throw SecurityException("Missing Health Connect activity write permission.")
        }
        return hc.writeActivityEntry(request)
    }

    override suspend fun updateActivityEntry(id: String, request: ActivityWriteRequest) {
        val missingPermissions = activityWritePermissions(request) - grantedPermissionsIfAvailable()
        if (missingPermissions.isNotEmpty()) {
            Log.w(TAG, "Skipping updateActivityEntry missingCount=${missingPermissions.size}")
            throw SecurityException("Missing Health Connect activity write permission.")
        }
        hc.updateActivityEntry(id, request)
    }

    override suspend fun deleteActivityEntry(id: String) {
        val granted = grantedPermissionsIfAvailable()
        if (writeExercisePermission !in granted) {
            Log.w(TAG, "Skipping deleteActivityEntry missingCount=1")
            throw SecurityException("Missing Health Connect activity write permission.")
        }
        hc.deleteActivityEntry(id)
        markerRepository?.deleteMarkersForActivity(id)
    }
}
