package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import tech.mmarca.openvitals.data.model.DashboardData
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate

@OptIn(ExperimentalMindfulnessSessionApi::class)
class HealthRepository(private val hc: HealthConnectManager) {

    companion object {
        private const val TAG = "HealthRepository"
    }

    private val readStepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
    private val readDistancePermission = HealthPermission.getReadPermission(DistanceRecord::class)
    private val readExercisePermission = HealthPermission.getReadPermission(ExerciseSessionRecord::class)
    private val readSleepPermission = HealthPermission.getReadPermission(SleepSessionRecord::class)
    private val readHeartRatePermission = HealthPermission.getReadPermission(HeartRateRecord::class)
    private val readRestingHRPermission = HealthPermission.getReadPermission(RestingHeartRateRecord::class)
    private val readWeightPermission = HealthPermission.getReadPermission(WeightRecord::class)
    private val readBodyFatPermission = HealthPermission.getReadPermission(BodyFatRecord::class)
    private val readCaloriesPermission = HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)
    private val readHydrationPermission = HealthPermission.getReadPermission(HydrationRecord::class)
    private val readNutritionPermission = HealthPermission.getReadPermission(NutritionRecord::class)
    private val readBloodPressurePermission = HealthPermission.getReadPermission(BloodPressureRecord::class)
    private val readSpO2Permission = HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    private val readVo2MaxPermission = HealthPermission.getReadPermission(Vo2MaxRecord::class)
    private val readFloorsPermission = HealthPermission.getReadPermission(FloorsClimbedRecord::class)
    private val readElevationPermission = HealthPermission.getReadPermission(ElevationGainedRecord::class)
    private val readMindfulnessPermission = HealthPermission.getReadPermission(MindfulnessSessionRecord::class)

    // ─── Availability + permissions ───────────────────────────────────────────

    fun availability(): HealthConnectAvailability = hc.availability()

    fun permissionContract() = hc.permissionContract()

    val phase1Permissions get() = hc.phase1Permissions
    val phase2Permissions get() = hc.phase2Permissions
    val phase3Permissions get() = hc.phase3Permissions
    val corePermissions get() = hc.corePermissions
    val heartPermissions get() = hc.heartPermissions
    val bodyPermissions get() = hc.bodyPermissions
    val activityExtrasPermissions get() = hc.activityExtrasPermissions
    val nutritionHydrationPermissions get() = hc.nutritionHydrationPermissions
    val mindfulnessPermissions get() = hc.mindfulnessPermissions
    val vitalsPermissions get() = hc.vitalsPermissions
    val onboardingPermissions get() = hc.allPermissions
    val allPermissions get() = hc.allPermissions

    fun isMindfulnessAvailable(): Boolean = hc.isMindfulnessSessionAvailable()

    suspend fun grantedPermissions(): Set<String> = hc.grantedPermissions()

    suspend fun missingPhase1(): Set<String> {
        val granted = hc.grantedPermissions()
        return hc.phase1Permissions.filterNot { it in granted }.toSet()
    }

    private suspend fun grantedPermissionsIfAvailable(): Set<String> =
        if (hc.availability() == HealthConnectAvailability.AVAILABLE) {
            hc.grantedPermissions().also { granted ->
                Log.d(TAG, "grantedPermissionsIfAvailable count=${granted.size}")
            }
        } else {
            Log.w(TAG, "Health Connect unavailable, returning empty granted permissions")
            emptySet()
        }

    // ─── Dashboard ────────────────────────────────────────────────────────────

    suspend fun loadDashboard(date: LocalDate = LocalDate.now()): DashboardData = coroutineScope {
        val granted = grantedPermissionsIfAvailable()
        Log.d(TAG, "loadDashboard date=$date granted=${granted.sorted()}")

        val steps = if (readStepsPermission in granted) async { hc.readSteps(date) } else null
        val distance = if (readDistancePermission in granted) async { hc.readDistanceMeters(date) } else null
        val workout = if (readExercisePermission in granted) async { hc.readLatestWorkout(date) } else null
        val sleep = if (readSleepPermission in granted) async { hc.readSleepSession(date) } else null
        val calories = if (readCaloriesPermission in granted) async { hc.readCaloriesKcal(date) } else null
        val caloriesIn = if (readNutritionPermission in granted) async { hc.readCaloriesInKcal(date) } else null
        val hydration = if (readHydrationPermission in granted) async { hc.readHydrationLiters(date) } else null
        val weight = if (readWeightPermission in granted) async { hc.readLatestWeight(date) } else null
        val bodyFat = if (readBodyFatPermission in granted) async { hc.readLatestBodyFat() } else null
        val heartRate = if (readHeartRatePermission in granted) async { hc.readAvgHeartRate(date) } else null
        val restingHR = if (readRestingHRPermission in granted) async { hc.readRestingHeartRate(date) } else null
        val bloodPressure = if (readBloodPressurePermission in granted) async { hc.readLatestBloodPressure(date) } else null
        val spO2 = if (readSpO2Permission in granted) async { hc.readLatestSpO2(date) } else null
        val vo2Max = if (readVo2MaxPermission in granted) async { hc.readLatestVo2Max(date) } else null
        val floors = if (readFloorsPermission in granted) async { hc.readFloorsClimbed(date) } else null
        val elevation = if (readElevationPermission in granted) async { hc.readElevationGained(date) } else null
        val mindfulnessMinutes = if (readMindfulnessPermission in granted) async { hc.readMindfulnessMinutes(date) } else null

        val missingPerms = onboardingPermissions.filterNot { it in granted }.toSet()
        val latestBloodPressure = bloodPressure?.await()

        DashboardData(
            date = date,
            steps = steps?.await() ?: 0L,
            distanceMeters = distance?.await() ?: 0.0,
            caloriesKcal = calories?.await() ?: 0.0,
            caloriesInKcal = caloriesIn?.await(),
            hydrationLiters = hydration?.await() ?: 0.0,
            workout = workout?.await(),
            sleep = sleep?.await(),
            weightKg = weight?.await()?.weightKg ?: 0.0,
            bodyFatPercent = bodyFat?.await() ?: 0.0,
            avgHeartRateBpm = heartRate?.await() ?: 0,
            restingHeartRateBpm = restingHR?.await() ?: 0,
            latestSystolicMmHg = latestBloodPressure?.systolicMmHg,
            latestDiastolicMmHg = latestBloodPressure?.diastolicMmHg,
            latestSpO2Percent = spO2?.await()?.percent,
            latestVo2Max = vo2Max?.await()?.vo2MaxMlPerKgPerMin,
            floorsClimbed = floors?.await(),
            elevationGainedMeters = elevation?.await(),
            mindfulnessMinutes = mindfulnessMinutes?.await(),
            missingPermissions = missingPerms,
        )
    }
}
