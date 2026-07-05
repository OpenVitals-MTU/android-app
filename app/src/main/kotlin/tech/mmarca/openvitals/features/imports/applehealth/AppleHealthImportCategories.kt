package tech.mmarca.openvitals.features.imports.applehealth

import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord

internal val AllAppleHealthImportCategories: Set<AppleHealthImportCategory> =
    AppleHealthImportCategory.entries.toSet()

internal fun ConvertedAppleRecord.importCategory(): AppleHealthImportCategory =
    when (targetType) {
        "ExerciseSessionRecord" -> AppleHealthImportCategory.WORKOUTS
        "StepsRecord",
        "DistanceRecord",
        "ActiveCaloriesBurnedRecord",
        "TotalCaloriesBurnedRecord",
        "BasalMetabolicRateRecord",
        "FloorsClimbedRecord",
        "ElevationGainedRecord",
        "WheelchairPushesRecord",
        "SpeedRecord",
        -> AppleHealthImportCategory.ACTIVITY
        "HeartRateRecord",
        "RestingHeartRateRecord",
        "HeartRateVariabilityRmssdRecord",
        -> AppleHealthImportCategory.HEART
        "SleepSessionRecord" -> AppleHealthImportCategory.SLEEP
        "WeightRecord",
        "HeightRecord",
        "BodyFatRecord",
        "LeanBodyMassRecord",
        "BoneMassRecord",
        "BodyWaterMassRecord",
        -> AppleHealthImportCategory.BODY
        "BloodPressureRecord",
        "OxygenSaturationRecord",
        "RespiratoryRateRecord",
        "BodyTemperatureRecord",
        "BloodGlucoseRecord",
        "Vo2MaxRecord",
        -> AppleHealthImportCategory.VITALS
        "NutritionRecord" -> AppleHealthImportCategory.NUTRITION
        "HydrationRecord" -> AppleHealthImportCategory.HYDRATION
        "MindfulnessSessionRecord" -> AppleHealthImportCategory.MINDFULNESS
        "MenstruationFlowRecord",
        "OvulationTestRecord",
        "CervicalMucusRecord",
        "BasalBodyTemperatureRecord",
        "IntermenstrualBleedingRecord",
        "SexualActivityRecord",
        -> AppleHealthImportCategory.CYCLE
        else -> AppleHealthImportCategory.ACTIVITY
    }

internal fun ConvertedAppleRecord.hasExerciseRoute(): Boolean =
    (record as? ExerciseSessionRecord)?.exerciseRouteResult is ExerciseRouteResult.Data

internal val AppleHealthImportCategory.reportName: String
    get() = when (this) {
        AppleHealthImportCategory.WORKOUTS -> "Workouts and routes"
        AppleHealthImportCategory.ACTIVITY -> "Activity metrics"
        AppleHealthImportCategory.HEART -> "Heart"
        AppleHealthImportCategory.SLEEP -> "Sleep"
        AppleHealthImportCategory.BODY -> "Body measurements"
        AppleHealthImportCategory.VITALS -> "Vitals"
        AppleHealthImportCategory.NUTRITION -> "Nutrition"
        AppleHealthImportCategory.HYDRATION -> "Hydration"
        AppleHealthImportCategory.MINDFULNESS -> "Mindfulness"
        AppleHealthImportCategory.CYCLE -> "Cycle tracking"
    }
