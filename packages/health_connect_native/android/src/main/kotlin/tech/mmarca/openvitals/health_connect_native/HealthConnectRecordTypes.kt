package tech.mmarca.openvitals.health_connect_native

import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.BasalBodyTemperatureRecord
import androidx.health.connect.client.records.BasalMetabolicRateRecord
import androidx.health.connect.client.records.BloodGlucoseRecord
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.records.BodyFatRecord
import androidx.health.connect.client.records.BodyTemperatureRecord
import androidx.health.connect.client.records.BodyWaterMassRecord
import androidx.health.connect.client.records.BoneMassRecord
import androidx.health.connect.client.records.CervicalMucusRecord
import androidx.health.connect.client.records.CyclingPedalingCadenceRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ElevationGainedRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.FloorsClimbedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.HeightRecord
import androidx.health.connect.client.records.HydrationRecord
import androidx.health.connect.client.records.IntermenstrualBleedingRecord
import androidx.health.connect.client.records.LeanBodyMassRecord
import androidx.health.connect.client.records.MenstruationFlowRecord
import androidx.health.connect.client.records.MenstruationPeriodRecord
import androidx.health.connect.client.records.MindfulnessSessionRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.OvulationTestRecord
import androidx.health.connect.client.records.OxygenSaturationRecord
import androidx.health.connect.client.records.PowerRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RespiratoryRateRecord
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SexualActivityRecord
import androidx.health.connect.client.records.SkinTemperatureRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.SpeedRecord
import androidx.health.connect.client.records.StepsCadenceRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.Vo2MaxRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.records.WheelchairPushesRecord
import kotlin.reflect.KClass

/**
 * Maps a canonical record-type string to its AndroidX [Record] class. Used by
 * [HealthConnectNativePlugin.filterExistingClientIds] (import dedup). Extracted
 * from the (now-removed) JSON `HealthRecordConverters`.
 */
internal fun recordClassFor(recordType: String): KClass<out Record>? = when (recordType) {
  "Steps" -> StepsRecord::class
  "Distance" -> DistanceRecord::class
  "ActiveCaloriesBurned" -> ActiveCaloriesBurnedRecord::class
  "TotalCaloriesBurned" -> TotalCaloriesBurnedRecord::class
  "FloorsClimbed" -> FloorsClimbedRecord::class
  "ElevationGained" -> ElevationGainedRecord::class
  "WheelchairPushes" -> WheelchairPushesRecord::class
  "ExerciseSession" -> ExerciseSessionRecord::class
  "Speed" -> SpeedRecord::class
  "StepsCadence" -> StepsCadenceRecord::class
  "CyclingPedalingCadence" -> CyclingPedalingCadenceRecord::class
  "Power" -> PowerRecord::class
  "Sleep" -> SleepSessionRecord::class
  "HeartRate" -> HeartRateRecord::class
  "RestingHeartRate" -> RestingHeartRateRecord::class
  "HeartRateVariabilityRmssd" -> HeartRateVariabilityRmssdRecord::class
  "Weight" -> WeightRecord::class
  "Height" -> HeightRecord::class
  "BodyFat" -> BodyFatRecord::class
  "LeanBodyMass" -> LeanBodyMassRecord::class
  "BoneMass" -> BoneMassRecord::class
  "BodyWaterMass" -> BodyWaterMassRecord::class
  "BasalMetabolicRate" -> BasalMetabolicRateRecord::class
  "Hydration" -> HydrationRecord::class
  "Nutrition" -> NutritionRecord::class
  "MindfulnessSession" -> MindfulnessSessionRecord::class
  "MenstruationFlow" -> MenstruationFlowRecord::class
  "MenstruationPeriod" -> MenstruationPeriodRecord::class
  "OvulationTest" -> OvulationTestRecord::class
  "CervicalMucus" -> CervicalMucusRecord::class
  "BasalBodyTemperature" -> BasalBodyTemperatureRecord::class
  "IntermenstrualBleeding" -> IntermenstrualBleedingRecord::class
  "SexualActivity" -> SexualActivityRecord::class
  "BloodPressure" -> BloodPressureRecord::class
  "OxygenSaturation" -> OxygenSaturationRecord::class
  "RespiratoryRate" -> RespiratoryRateRecord::class
  "BodyTemperature" -> BodyTemperatureRecord::class
  "SkinTemperature" -> SkinTemperatureRecord::class
  "BloodGlucose" -> BloodGlucoseRecord::class
  "Vo2Max" -> Vo2MaxRecord::class
  else -> null
}
