package tech.mmarca.openvitals.data.repository

import android.util.Log
import androidx.health.connect.client.feature.ExperimentalMindfulnessSessionApi
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import tech.mmarca.openvitals.data.model.DashboardData
import tech.mmarca.openvitals.data.model.HealthConnectAvailability
import tech.mmarca.openvitals.core.preferences.SleepRangeMode
import tech.mmarca.openvitals.data.model.dailySleepSummary
import tech.mmarca.openvitals.data.model.sleepRangeWindowFor
import tech.mmarca.openvitals.healthconnect.HealthConnectManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

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
    private val readActiveCaloriesPermission = HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
    private val readHydrationPermission = HealthPermission.getReadPermission(HydrationRecord::class)
    private val readNutritionPermission = HealthPermission.getReadPermission(NutritionRecord::class)
    private val readBloodPressurePermission = HealthPermission.getReadPermission(BloodPressureRecord::class)
    private val readSpO2Permission = HealthPermission.getReadPermission(OxygenSaturationRecord::class)
    private val readVo2MaxPermission = HealthPermission.getReadPermission(Vo2MaxRecord::class)
    private val readRespiratoryRatePermission = HealthPermission.getReadPermission(RespiratoryRateRecord::class)
    private val readBodyTemperaturePermission = HealthPermission.getReadPermission(BodyTemperatureRecord::class)
    private val readFloorsPermission = HealthPermission.getReadPermission(FloorsClimbedRecord::class)
    private val readElevationPermission = HealthPermission.getReadPermission(ElevationGainedRecord::class)
    private val readMindfulnessPermission = HealthPermission.getReadPermission(MindfulnessSessionRecord::class)
    private val readHrvPermission = HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    private val readHeightPermission = HealthPermission.getReadPermission(HeightRecord::class)
    private val readLeanMassPermission = HealthPermission.getReadPermission(LeanBodyMassRecord::class)
    private val readBmrPermission = HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
    private val readBoneMassPermission = HealthPermission.getReadPermission(BoneMassRecord::class)
    private val readMenstruationPeriodPermission = HealthPermission.getReadPermission(MenstruationPeriodRecord::class)
    private val readOvulationTestPermission = HealthPermission.getReadPermission(OvulationTestRecord::class)
    private val readBasalBodyTemperaturePermission = HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class)

    // ─── Availability + permissions ───────────────────────────────────────────

    fun availability(): HealthConnectAvailability = hc.availability()

    fun permissionContract() = hc.permissionContract()

    val phase1Permissions get() = hc.phase1Permissions
    val phase2Permissions get() = hc.phase2Permissions
    val phase3Permissions get() = hc.phase3Permissions
    val phase4Permissions get() = hc.phase4Permissions
    val corePermissions get() = hc.corePermissions
    val routePermissions get() = hc.routePermissions
    val heartPermissions get() = hc.heartPermissions
    val bodyPermissions get() = hc.bodyPermissions
    val activityExtrasPermissions get() = hc.activityExtrasPermissions
    val nutritionHydrationPermissions get() = hc.nutritionHydrationPermissions
    val mindfulnessPermissions get() = hc.mindfulnessPermissions
    val additionalDataAccessPermissions get() = hc.additionalDataAccessPermissions
    val vitalsPermissions get() = hc.vitalsPermissions
    val cyclePermissions get() = hc.cyclePermissions
    val manualOnlyPermissions get() = hc.manualOnlyPermissions
    val onboardingPermissions get() = hc.requestableAllPermissions
    val allPermissions get() = hc.allPermissions
    val managedPermissions get() = hc.managedPermissions
    fun grantModeFor(permission: String) = hc.grantModeFor(permission)

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

    suspend fun loadDashboard(date: LocalDate = LocalDate.now()): DashboardData =
        loadDashboard(date, SleepRangeMode.EVENING_18H)

    suspend fun loadDashboard(
        date: LocalDate,
        sleepRangeMode: SleepRangeMode,
    ): DashboardData = coroutineScope {
        val granted = grantedPermissionsIfAvailable()
        Log.d(TAG, "loadDashboard date=$date granted=${granted.sorted()}")

        fun <T> readIfGranted(permission: String, name: String, block: suspend () -> T) =
            if (permission in granted) async { dashboardMetric(name, block) } else null

        val zone = ZoneId.systemDefault()
        val dayStart = date.atStartOfDay(zone).toInstant()
        val dayEnd = date.plusDays(1).atStartOfDay(zone).toInstant()

        val steps = readIfGranted(readStepsPermission, "steps") { hc.readSteps(date) }
        val distance = readIfGranted(readDistancePermission, "distance") { hc.readDistanceMeters(date) }
        val workout = readIfGranted(readExercisePermission, "latest workout") { hc.readLatestWorkout(date) }
        val sleep = readIfGranted(readSleepPermission, "sleep") { readDashboardSleep(date, sleepRangeMode) }
        val calories = readIfGranted(readCaloriesPermission, "calories") { hc.readCaloriesKcal(date) }
        val activeCalories = if (
            readActiveCaloriesPermission in granted &&
            readStepsPermission in granted &&
            readDistancePermission in granted
        ) {
            async {
                dashboardMetric("active calories") {
                    hc.readDailySteps(date, date, includeActiveCalories = true)
                        .firstOrNull()
                        ?.activeCaloriesKcal
                }
            }
        } else {
            null
        }
        val caloriesIn = readIfGranted(readNutritionPermission, "calories in") { hc.readCaloriesInKcal(date) }
        val macros = readIfGranted(readNutritionPermission, "macros") {
            hc.readDailyMacros(date, date).firstOrNull()
        }
        val hydration = readIfGranted(readHydrationPermission, "hydration") { hc.readHydrationLiters(date) }
        val weight = readIfGranted(readWeightPermission, "weight") { hc.readLatestWeight(date) }
        val height = readIfGranted(readHeightPermission, "height") { hc.readLatestHeight() }
        val bodyFat = readIfGranted(readBodyFatPermission, "body fat") { hc.readLatestBodyFat() }
        val leanMass = readIfGranted(readLeanMassPermission, "lean mass") { hc.readLatestLeanBodyMass() }
        val bmr = readIfGranted(readBmrPermission, "BMR") { hc.readLatestBMR() }
        val boneMass = readIfGranted(readBoneMassPermission, "bone mass") { hc.readLatestBoneMass() }
        val heartRate = readIfGranted(readHeartRatePermission, "heart rate") { hc.readAvgHeartRate(date) }
        val restingHR = readIfGranted(readRestingHRPermission, "resting heart rate") { hc.readRestingHeartRate(date) }
        val hrv = readIfGranted(readHrvPermission, "HRV") { hc.readHrvRmssd(date) }
        val bloodPressure = readIfGranted(readBloodPressurePermission, "blood pressure") {
            hc.readLatestBloodPressure(date)
        }
        val spO2 = readIfGranted(readSpO2Permission, "SpO2") { hc.readLatestSpO2(date) }
        val vo2Max = readIfGranted(readVo2MaxPermission, "VO2 max") { hc.readLatestVo2Max(date) }
        val respiratoryRate = readIfGranted(readRespiratoryRatePermission, "respiratory rate") {
            hc.readRespiratoryRateEntries(dayStart, dayEnd)
                .map { it.breathsPerMinute }
                .average()
                .takeUnless { it.isNaN() }
        }
        val bodyTemperature = readIfGranted(readBodyTemperaturePermission, "body temperature") {
            hc.readBodyTemperatureEntries(dayStart, dayEnd)
                .maxByOrNull { it.time }
                ?.temperatureCelsius
        }
        val floors = readIfGranted(readFloorsPermission, "floors") { hc.readFloorsClimbed(date) }
        val elevation = readIfGranted(readElevationPermission, "elevation") { hc.readElevationGained(date) }
        val mindfulnessMinutes = readIfGranted(readMindfulnessPermission, "mindfulness") {
            hc.readMindfulnessMinutes(date)
        }
        val menstruationPeriods = readIfGranted(readMenstruationPeriodPermission, "menstruation periods") {
            hc.readMenstruationPeriods(dayStart, dayEnd)
        }
        val ovulationTests = readIfGranted(readOvulationTestPermission, "ovulation tests") {
            hc.readOvulationTests(dayStart, dayEnd)
        }
        val basalBodyTemperature = readIfGranted(readBasalBodyTemperaturePermission, "basal body temperature") {
            hc.readBasalBodyTemperatureEntries(dayStart, dayEnd)
                .maxByOrNull { it.time }
                ?.temperatureCelsius
        }

        val missingPerms = onboardingPermissions.filterNot { it in granted }.toSet()
        val latestBloodPressure = bloodPressure?.await()
        val latestWeight = weight?.await()?.weightKg
        val latestHeight = height?.await()
        val dailyMacros = macros?.await()

        DashboardData(
            date = date,
            steps = steps?.await() ?: 0L,
            distanceMeters = distance?.await() ?: 0.0,
            caloriesKcal = calories?.await() ?: 0.0,
            activeCaloriesKcal = activeCalories?.await(),
            caloriesInKcal = caloriesIn?.await(),
            proteinGrams = dailyMacros?.proteinGrams,
            carbsGrams = dailyMacros?.carbsGrams,
            fatGrams = dailyMacros?.fatGrams,
            hydrationLiters = hydration?.await() ?: 0.0,
            workout = workout?.await(),
            sleep = sleep?.await(),
            weightKg = latestWeight ?: 0.0,
            heightCm = latestHeight,
            bmi = latestWeight?.let { weightKg ->
                latestHeight
                    ?.takeIf { it > 0.0 }
                    ?.let { heightCm -> weightKg / ((heightCm / 100.0) * (heightCm / 100.0)) }
            },
            bodyFatPercent = bodyFat?.await() ?: 0.0,
            leanMassKg = leanMass?.await(),
            bmrKcal = bmr?.await(),
            boneMassKg = boneMass?.await(),
            avgHeartRateBpm = heartRate?.await() ?: 0,
            restingHeartRateBpm = restingHR?.await() ?: 0,
            hrvRmssdMs = hrv?.await(),
            latestSystolicMmHg = latestBloodPressure?.systolicMmHg,
            latestDiastolicMmHg = latestBloodPressure?.diastolicMmHg,
            latestSpO2Percent = spO2?.await()?.percent,
            latestVo2Max = vo2Max?.await()?.vo2MaxMlPerKgPerMin,
            avgRespiratoryRate = respiratoryRate?.await(),
            latestBodyTemperatureCelsius = bodyTemperature?.await(),
            floorsClimbed = floors?.await(),
            elevationGainedMeters = elevation?.await(),
            mindfulnessMinutes = mindfulnessMinutes?.await(),
            menstruationPeriodDays = menstruationPeriods?.await()?.sumOf { period ->
                val startDate = period.startTime.atZone(zone).toLocalDate()
                val endDate = period.endTime.minusMillis(1).atZone(zone).toLocalDate()
                (Duration.between(startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()).toDays())
                    .toInt()
                    .coerceAtLeast(1)
            },
            ovulationTestCount = ovulationTests?.await()?.size,
            latestBasalBodyTemperatureCelsius = basalBodyTemperature?.await(),
            missingPermissions = missingPerms,
        )
    }

    private suspend fun readDashboardSleep(
        date: LocalDate,
        sleepRangeMode: SleepRangeMode,
    ) = with(sleepRangeWindowFor(date, sleepRangeMode, ZoneId.systemDefault())) {
        dailySleepSummary(
            sessions = hc.readSleepSessions(start.minus(Duration.ofDays(1)), end),
            selectedDate = date,
            sleepRangeMode = sleepRangeMode,
        )
    }

    private suspend fun <T> dashboardMetric(name: String, block: suspend () -> T): T? =
        runCatching { block() }
            .onFailure { Log.w(TAG, "Skipping dashboard metric $name after Health Connect failure", it) }
            .getOrNull()
}
