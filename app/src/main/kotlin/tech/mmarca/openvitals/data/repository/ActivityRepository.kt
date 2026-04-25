package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import tech.mmarca.openvitals.data.model.ActivityProgressPoint
import tech.mmarca.openvitals.data.model.DailyNutrition
import tech.mmarca.openvitals.data.model.DailySteps
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import java.time.LocalDate
import java.time.ZoneId

class ActivityRepository(private val hc: HealthConnectManager) {

    companion object {
        private const val TAG = "ActivityRepository"
    }

    private val readStepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val readDistancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    private val readExercisePermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val readCaloriesPermission = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    private val readHydrationPermission = HealthPermission.getReadPermission(HydrationRecord::class)
    private val readFloorsPermission = HealthPermission.getReadPermission(FloorsClimbedRecord::class)
    private val readActiveCaloriesPermission = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val readElevationPermission = HealthPermission.getReadPermission(ElevationGainedRecord::class)

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) hc.grantedPermissions() else emptySet()

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

    suspend fun loadDailyNutrition(start: LocalDate, end: LocalDate): List<DailyNutrition> {
        val granted = grantedPermissionsIfAvailable()
        if (readHydrationPermission !in granted && readCaloriesPermission !in granted) {
            Log.w(TAG, "Skipping loadDailyNutrition start=$start end=$end missing both optional permissions")
            return emptyList()
        }
        return hc.readDailyNutrition(start, end)
    }
}
