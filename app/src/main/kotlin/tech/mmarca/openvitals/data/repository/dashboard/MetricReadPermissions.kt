package tech.mmarca.openvitals.data.repository.dashboard

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
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
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import tech.mmarca.openvitals.domain.model.DashboardMetric

/**
 * The raw read permissions each metric needs, unfiltered. Callers intersect
 * with `HealthConnectManager.managedPermissions`.
 */
object MetricReadPermissions {

    val readStepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
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
    private val readBloodGlucosePermission = HealthPermission.getReadPermission(BloodGlucoseRecord::class)
    private val readSkinTemperaturePermission = HealthPermission.getReadPermission(SkinTemperatureRecord::class)
    private val readFloorsPermission = HealthPermission.getReadPermission(FloorsClimbedRecord::class)
    private val readElevationPermission = HealthPermission.getReadPermission(ElevationGainedRecord::class)
    private val readWheelchairPushesPermission = HealthPermission.getReadPermission(WheelchairPushesRecord::class)
    private val readMindfulnessPermission = HealthPermission.getReadPermission(MindfulnessSessionRecord::class)
    private val readHrvPermission = HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class)
    private val readHeightPermission = HealthPermission.getReadPermission(HeightRecord::class)
    private val readLeanMassPermission = HealthPermission.getReadPermission(LeanBodyMassRecord::class)
    private val readBmrPermission = HealthPermission.getReadPermission(BasalMetabolicRateRecord::class)
    private val readBoneMassPermission = HealthPermission.getReadPermission(BoneMassRecord::class)
    private val readBodyWaterMassPermission = HealthPermission.getReadPermission(BodyWaterMassRecord::class)
    private val readMenstruationPeriodPermission = HealthPermission.getReadPermission(MenstruationPeriodRecord::class)
    private val readOvulationTestPermission = HealthPermission.getReadPermission(OvulationTestRecord::class)
    private val readBasalBodyTemperaturePermission = HealthPermission.getReadPermission(BasalBodyTemperatureRecord::class)

    fun forMetric(
        metric: DashboardMetric,
        showOpenVitalsCalculatedCalories: Boolean,
    ): Set<String> =
        when (metric) {
            DashboardMetric.STEPS -> setOf(readStepsPermission)
            DashboardMetric.DISTANCE -> setOf(readDistancePermission)
            DashboardMetric.CALORIES_OUT -> if (showOpenVitalsCalculatedCalories) {
                setOf(readCaloriesPermission, readActiveCaloriesPermission, readBmrPermission)
            } else {
                setOf(readCaloriesPermission)
            }
            DashboardMetric.ACTIVE_CALORIES -> setOf(
                readActiveCaloriesPermission,
                readStepsPermission,
                readDistancePermission,
            )
            DashboardMetric.FLOORS -> setOf(readFloorsPermission)
            DashboardMetric.ELEVATION -> setOf(readElevationPermission)
            DashboardMetric.WHEELCHAIR_PUSHES -> setOf(readWheelchairPushesPermission)
            DashboardMetric.WORKOUT -> setOf(readExercisePermission)
            DashboardMetric.SLEEP -> setOf(readSleepPermission)
            DashboardMetric.HYDRATION -> setOf(readHydrationPermission)
            DashboardMetric.CALORIES_IN,
            DashboardMetric.PROTEIN,
            DashboardMetric.CARBS,
            DashboardMetric.FAT,
            DashboardMetric.CAFFEINE,
            -> setOf(readNutritionPermission)
            DashboardMetric.WEIGHT -> setOf(readWeightPermission)
            DashboardMetric.HEIGHT -> setOf(readHeightPermission)
            DashboardMetric.BMI -> setOf(readWeightPermission, readHeightPermission)
            DashboardMetric.FFMI -> setOf(readWeightPermission, readHeightPermission, readBodyFatPermission)
            DashboardMetric.BODY_FAT -> setOf(readBodyFatPermission)
            DashboardMetric.LEAN_MASS -> setOf(readLeanMassPermission)
            DashboardMetric.BMR -> setOf(readBmrPermission)
            DashboardMetric.BONE_MASS -> setOf(readBoneMassPermission)
            DashboardMetric.BODY_WATER_MASS -> setOf(readBodyWaterMassPermission)
            DashboardMetric.AVG_HEART_RATE -> setOf(readHeartRatePermission)
            DashboardMetric.RESTING_HEART_RATE -> setOf(readRestingHRPermission)
            DashboardMetric.HRV -> setOf(readHrvPermission)
            DashboardMetric.BLOOD_PRESSURE -> setOf(readBloodPressurePermission)
            DashboardMetric.SPO2 -> setOf(readSpO2Permission)
            DashboardMetric.VO2_MAX -> setOf(readVo2MaxPermission)
            DashboardMetric.RESPIRATORY_RATE -> setOf(readRespiratoryRatePermission)
            DashboardMetric.BODY_TEMPERATURE -> setOf(readBodyTemperaturePermission)
            DashboardMetric.BLOOD_GLUCOSE -> setOf(readBloodGlucosePermission)
            // Raw even where the feature flag is off; `managedPermissions` subtracts.
            DashboardMetric.SKIN_TEMPERATURE -> setOf(readSkinTemperaturePermission)
            DashboardMetric.WEEKLY_CARDIO_LOAD -> setOf(readStepsPermission)
            DashboardMetric.INTENSITY_MINUTES -> setOf(
                readHeartRatePermission,
                readRestingHRPermission,
                readExercisePermission,
                readActiveCaloriesPermission,
                readStepsPermission,
                readDistancePermission,
            )
            DashboardMetric.MINDFULNESS -> setOf(readMindfulnessPermission)
            DashboardMetric.CYCLE -> setOf(
                readMenstruationPeriodPermission,
                readOvulationTestPermission,
                readBasalBodyTemperaturePermission,
            )
        }
}
