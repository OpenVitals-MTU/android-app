package tech.mmarca.openvitals.data.cache

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import tech.mmarca.openvitals.data.repository.ActivitiesPeriodData
import tech.mmarca.openvitals.data.repository.ActivityPeriodData
import tech.mmarca.openvitals.data.repository.BodyPeriodData
import tech.mmarca.openvitals.data.repository.CyclePeriodData
import tech.mmarca.openvitals.data.repository.HeartPeriodData
import tech.mmarca.openvitals.data.repository.HydrationPeriodData
import tech.mmarca.openvitals.data.repository.MindfulnessPeriodData
import tech.mmarca.openvitals.data.repository.NutritionPeriodData
import tech.mmarca.openvitals.data.repository.SleepPeriodData
import tech.mmarca.openvitals.data.repository.VitalsPeriodData

object ActivityPeriodDataCodec {
    const val Surface = "activity-period"
    const val SchemaVersion = 1

    fun encode(data: ActivityPeriodData): String = encodeObject {
        put("dailySteps", data.dailySteps.toJsonArray { it.toJson() })
        put("previousDailySteps", data.previousDailySteps.toJsonArray { it.toJson() })
        put("baselineDailySteps", data.baselineDailySteps.toJsonArray { it.toJson() })
        put("nutrition", data.nutrition.toJsonArray { it.toJson() })
        put("previousNutrition", data.previousNutrition.toJsonArray { it.toJson() })
        put("baselineNutrition", data.baselineNutrition.toJsonArray { it.toJson() })
        put("activityProgress", data.activityProgress.toJsonArray { it.toJson() })
    }

    fun decode(payloadJson: String): ActivityPeriodData {
        val root = decodeObject(payloadJson)
        return ActivityPeriodData(
            dailySteps = root.array("dailySteps").map { it.jsonObject.toDailySteps() },
            previousDailySteps = root.array("previousDailySteps").map { it.jsonObject.toDailySteps() },
            baselineDailySteps = root.array("baselineDailySteps").map { it.jsonObject.toDailySteps() },
            nutrition = root.array("nutrition").map { it.jsonObject.toDailyNutrition() },
            previousNutrition = root.array("previousNutrition").map { it.jsonObject.toDailyNutrition() },
            baselineNutrition = root.array("baselineNutrition").map { it.jsonObject.toDailyNutrition() },
            activityProgress = root.array("activityProgress").map { it.jsonObject.toActivityProgressPoint() },
        )
    }
}

object ActivitiesPeriodDataCodec {
    const val Surface = "activities-period"
    const val SchemaVersion = 1

    fun encode(data: ActivitiesPeriodData): String = encodeObject {
        put("workouts", data.workouts.toJsonArray { it.toJson() })
        put("previousWorkouts", data.previousWorkouts.toJsonArray { it.toJson() })
        put("baselineWorkouts", data.baselineWorkouts.toJsonArray { it.toJson() })
        put("plannedWorkouts", data.plannedWorkouts.toJsonArray { it.toJson() })
    }

    fun decode(payloadJson: String): ActivitiesPeriodData {
        val root = decodeObject(payloadJson)
        return ActivitiesPeriodData(
            workouts = root.array("workouts").map { it.jsonObject.toExerciseData() },
            previousWorkouts = root.array("previousWorkouts").map { it.jsonObject.toExerciseData() },
            baselineWorkouts = root.array("baselineWorkouts").map { it.jsonObject.toExerciseData() },
            plannedWorkouts = root.array("plannedWorkouts").map { it.jsonObject.toPlannedExerciseData() },
        )
    }
}

object SleepPeriodDataCodec {
    const val Surface = "sleep-period"
    const val SchemaVersion = 1

    fun encode(data: SleepPeriodData): String = encodeObject {
        put("sessions", data.sessions.toJsonArray { it.toJson() })
        put("previousSessions", data.previousSessions.toJsonArray { it.toJson() })
        put("baselineSessions", data.baselineSessions.toJsonArray { it.toJson() })
    }

    fun decode(payloadJson: String): SleepPeriodData {
        val root = decodeObject(payloadJson)
        return SleepPeriodData(
            sessions = root.array("sessions").map { it.jsonObject.toSleepData() },
            previousSessions = root.array("previousSessions").map { it.jsonObject.toSleepData() },
            baselineSessions = root.array("baselineSessions").map { it.jsonObject.toSleepData() },
        )
    }
}

object HeartPeriodDataCodec {
    const val Surface = "heart-period"
    const val SchemaVersion = 2

    fun encode(data: HeartPeriodData): String = encodeObject {
        // High-frequency day samples are loaded fresh and are not persisted in SQLite cache.
        put("dailySummaries", data.dailySummaries.toJsonArray { it.toJson() })
        put("previousDailySummaries", data.previousDailySummaries.toJsonArray { it.toJson() })
        put("baselineDailySummaries", data.baselineDailySummaries.toJsonArray { it.toJson() })
        putNullable("dayRestingBpm", data.dayRestingBpm)
        putNullable("previousDayRestingBpm", data.previousDayRestingBpm)
        putNullable("dayHrvMs", data.dayHrvMs)
        putNullable("previousDayHrvMs", data.previousDayHrvMs)
        put("dailyRestingHR", data.dailyRestingHR.toJsonArray { it.toJson() })
        put("previousDailyRestingHR", data.previousDailyRestingHR.toJsonArray { it.toJson() })
        put("baselineDailyRestingHR", data.baselineDailyRestingHR.toJsonArray { it.toJson() })
        put("dailyHrv", data.dailyHrv.toJsonArray { it.toJson() })
        put("previousDailyHrv", data.previousDailyHrv.toJsonArray { it.toJson() })
        put("baselineDailyHrv", data.baselineDailyHrv.toJsonArray { it.toJson() })
    }

    fun decode(payloadJson: String): HeartPeriodData {
        val root = decodeObject(payloadJson)
        return HeartPeriodData(
            daySamples = root.array("daySamples").map { it.jsonObject.toHeartRateSample() },
            previousDaySamples = root.array("previousDaySamples").map { it.jsonObject.toHeartRateSample() },
            dailySummaries = root.array("dailySummaries").map { it.jsonObject.toHeartRateSummary() },
            previousDailySummaries = root.array("previousDailySummaries").map { it.jsonObject.toHeartRateSummary() },
            baselineDailySummaries = root.array("baselineDailySummaries").map { it.jsonObject.toHeartRateSummary() },
            dayRestingBpm = root.longOrNull("dayRestingBpm"),
            previousDayRestingBpm = root.longOrNull("previousDayRestingBpm"),
            dayHrvMs = root.doubleOrNull("dayHrvMs"),
            previousDayHrvMs = root.doubleOrNull("previousDayHrvMs"),
            dailyRestingHR = root.array("dailyRestingHR").map { it.jsonObject.toDailyRestingHR() },
            previousDailyRestingHR = root.array("previousDailyRestingHR").map { it.jsonObject.toDailyRestingHR() },
            baselineDailyRestingHR = root.array("baselineDailyRestingHR").map { it.jsonObject.toDailyRestingHR() },
            dailyHrv = root.array("dailyHrv").map { it.jsonObject.toDailyHrv() },
            previousDailyHrv = root.array("previousDailyHrv").map { it.jsonObject.toDailyHrv() },
            baselineDailyHrv = root.array("baselineDailyHrv").map { it.jsonObject.toDailyHrv() },
        )
    }
}

object BodyPeriodDataCodec {
    const val Surface = "body-period"
    const val SchemaVersion = 2

    fun encode(data: BodyPeriodData): String = encodeObject {
        put("weightEntries", data.weightEntries.toJsonArray { it.toJson() })
        put("previousWeightEntries", data.previousWeightEntries.toJsonArray { it.toJson() })
        put("baselineWeightEntries", data.baselineWeightEntries.toJsonArray { it.toJson() })
        putNullable("latestWeightKg", data.latestWeightKg)
        putNullable("heightCm", data.heightCm)
        put("heightEntries", data.heightEntries.toJsonArray { it.toJson() })
        put("previousHeightEntries", data.previousHeightEntries.toJsonArray { it.toJson() })
        put("baselineHeightEntries", data.baselineHeightEntries.toJsonArray { it.toJson() })
        put("bodyFatEntries", data.bodyFatEntries.toJsonArray { it.toJson() })
        put("previousBodyFatEntries", data.previousBodyFatEntries.toJsonArray { it.toJson() })
        put("baselineBodyFatEntries", data.baselineBodyFatEntries.toJsonArray { it.toJson() })
        putNullable("latestBodyFatPercent", data.latestBodyFatPercent)
        putNullable("leanMassKg", data.leanMassKg)
        put("leanMassEntries", data.leanMassEntries.toJsonArray { it.toJson() })
        put("previousLeanMassEntries", data.previousLeanMassEntries.toJsonArray { it.toJson() })
        put("baselineLeanMassEntries", data.baselineLeanMassEntries.toJsonArray { it.toJson() })
        putNullable("bmrKcal", data.bmrKcal)
        put("bmrEntries", data.bmrEntries.toJsonArray { it.toJson() })
        put("previousBmrEntries", data.previousBmrEntries.toJsonArray { it.toJson() })
        put("baselineBmrEntries", data.baselineBmrEntries.toJsonArray { it.toJson() })
        putNullable("boneMassKg", data.boneMassKg)
        put("boneMassEntries", data.boneMassEntries.toJsonArray { it.toJson() })
        put("previousBoneMassEntries", data.previousBoneMassEntries.toJsonArray { it.toJson() })
        put("baselineBoneMassEntries", data.baselineBoneMassEntries.toJsonArray { it.toJson() })
        putNullable("bodyWaterMassKg", data.bodyWaterMassKg)
        put("bodyWaterMassEntries", data.bodyWaterMassEntries.toJsonArray { it.toJson() })
        put("previousBodyWaterMassEntries", data.previousBodyWaterMassEntries.toJsonArray { it.toJson() })
        put("baselineBodyWaterMassEntries", data.baselineBodyWaterMassEntries.toJsonArray { it.toJson() })
    }

    fun decode(payloadJson: String): BodyPeriodData {
        val root = decodeObject(payloadJson)
        return BodyPeriodData(
            weightEntries = root.array("weightEntries").map { it.jsonObject.toWeightEntry() },
            previousWeightEntries = root.array("previousWeightEntries").map { it.jsonObject.toWeightEntry() },
            baselineWeightEntries = root.array("baselineWeightEntries").map { it.jsonObject.toWeightEntry() },
            latestWeightKg = root.doubleOrNull("latestWeightKg"),
            heightCm = root.doubleOrNull("heightCm"),
            heightEntries = root.array("heightEntries").map { it.jsonObject.toHeightEntry() },
            previousHeightEntries = root.array("previousHeightEntries").map { it.jsonObject.toHeightEntry() },
            baselineHeightEntries = root.array("baselineHeightEntries").map { it.jsonObject.toHeightEntry() },
            bodyFatEntries = root.array("bodyFatEntries").map { it.jsonObject.toBodyFatEntry() },
            previousBodyFatEntries = root.array("previousBodyFatEntries").map { it.jsonObject.toBodyFatEntry() },
            baselineBodyFatEntries = root.array("baselineBodyFatEntries").map { it.jsonObject.toBodyFatEntry() },
            latestBodyFatPercent = root.doubleOrNull("latestBodyFatPercent"),
            leanMassKg = root.doubleOrNull("leanMassKg"),
            leanMassEntries = root.array("leanMassEntries").map { it.jsonObject.toLeanBodyMassEntry() },
            previousLeanMassEntries = root.array("previousLeanMassEntries").map { it.jsonObject.toLeanBodyMassEntry() },
            baselineLeanMassEntries = root.array("baselineLeanMassEntries").map { it.jsonObject.toLeanBodyMassEntry() },
            bmrKcal = root.doubleOrNull("bmrKcal"),
            bmrEntries = root.array("bmrEntries").map { it.jsonObject.toBmrEntry() },
            previousBmrEntries = root.array("previousBmrEntries").map { it.jsonObject.toBmrEntry() },
            baselineBmrEntries = root.array("baselineBmrEntries").map { it.jsonObject.toBmrEntry() },
            boneMassKg = root.doubleOrNull("boneMassKg"),
            boneMassEntries = root.array("boneMassEntries").map { it.jsonObject.toBoneMassEntry() },
            previousBoneMassEntries = root.array("previousBoneMassEntries").map { it.jsonObject.toBoneMassEntry() },
            baselineBoneMassEntries = root.array("baselineBoneMassEntries").map { it.jsonObject.toBoneMassEntry() },
            bodyWaterMassKg = root.doubleOrNull("bodyWaterMassKg"),
            bodyWaterMassEntries = root.array("bodyWaterMassEntries").map { it.jsonObject.toBodyWaterMassEntry() },
            previousBodyWaterMassEntries = root.array("previousBodyWaterMassEntries").map { it.jsonObject.toBodyWaterMassEntry() },
            baselineBodyWaterMassEntries = root.array("baselineBodyWaterMassEntries").map { it.jsonObject.toBodyWaterMassEntry() },
        )
    }
}

object VitalsPeriodDataCodec {
    const val Surface = "vitals-period"
    const val SchemaVersion = 1

    fun encode(data: VitalsPeriodData): String = encodeObject {
        put("missingVitalsPermissions", data.missingVitalsPermissions.toStringJsonArray())
        put("bloodPressure", data.bloodPressure.toJsonArray { it.toJson() })
        put("previousBloodPressure", data.previousBloodPressure.toJsonArray { it.toJson() })
        put("baselineBloodPressure", data.baselineBloodPressure.toJsonArray { it.toJson() })
        put("spO2", data.spO2.toJsonArray { it.toJson() })
        put("previousSpO2", data.previousSpO2.toJsonArray { it.toJson() })
        put("baselineSpO2", data.baselineSpO2.toJsonArray { it.toJson() })
        put("respiratoryRate", data.respiratoryRate.toJsonArray { it.toJson() })
        put("previousRespiratoryRate", data.previousRespiratoryRate.toJsonArray { it.toJson() })
        put("baselineRespiratoryRate", data.baselineRespiratoryRate.toJsonArray { it.toJson() })
        put("bodyTemperature", data.bodyTemperature.toJsonArray { it.toJson() })
        put("previousBodyTemperature", data.previousBodyTemperature.toJsonArray { it.toJson() })
        put("baselineBodyTemperature", data.baselineBodyTemperature.toJsonArray { it.toJson() })
        put("vo2Max", data.vo2Max.toJsonArray { it.toJson() })
        put("previousVo2Max", data.previousVo2Max.toJsonArray { it.toJson() })
        put("baselineVo2Max", data.baselineVo2Max.toJsonArray { it.toJson() })
        put("bloodGlucose", data.bloodGlucose.toJsonArray { it.toJson() })
        put("previousBloodGlucose", data.previousBloodGlucose.toJsonArray { it.toJson() })
        put("baselineBloodGlucose", data.baselineBloodGlucose.toJsonArray { it.toJson() })
        put("skinTemperature", data.skinTemperature.toJsonArray { it.toJson() })
        put("previousSkinTemperature", data.previousSkinTemperature.toJsonArray { it.toJson() })
        put("baselineSkinTemperature", data.baselineSkinTemperature.toJsonArray { it.toJson() })
    }

    fun decode(payloadJson: String): VitalsPeriodData {
        val root = decodeObject(payloadJson)
        return VitalsPeriodData(
            missingVitalsPermissions = root.stringSet("missingVitalsPermissions"),
            bloodPressure = root.array("bloodPressure").map { it.jsonObject.toBloodPressureEntry() },
            previousBloodPressure = root.array("previousBloodPressure").map { it.jsonObject.toBloodPressureEntry() },
            baselineBloodPressure = root.array("baselineBloodPressure").map { it.jsonObject.toBloodPressureEntry() },
            spO2 = root.array("spO2").map { it.jsonObject.toSpO2Entry() },
            previousSpO2 = root.array("previousSpO2").map { it.jsonObject.toSpO2Entry() },
            baselineSpO2 = root.array("baselineSpO2").map { it.jsonObject.toSpO2Entry() },
            respiratoryRate = root.array("respiratoryRate").map { it.jsonObject.toRespiratoryRateEntry() },
            previousRespiratoryRate = root.array("previousRespiratoryRate").map { it.jsonObject.toRespiratoryRateEntry() },
            baselineRespiratoryRate = root.array("baselineRespiratoryRate").map { it.jsonObject.toRespiratoryRateEntry() },
            bodyTemperature = root.array("bodyTemperature").map { it.jsonObject.toBodyTempEntry() },
            previousBodyTemperature = root.array("previousBodyTemperature").map { it.jsonObject.toBodyTempEntry() },
            baselineBodyTemperature = root.array("baselineBodyTemperature").map { it.jsonObject.toBodyTempEntry() },
            vo2Max = root.array("vo2Max").map { it.jsonObject.toVo2MaxEntry() },
            previousVo2Max = root.array("previousVo2Max").map { it.jsonObject.toVo2MaxEntry() },
            baselineVo2Max = root.array("baselineVo2Max").map { it.jsonObject.toVo2MaxEntry() },
            bloodGlucose = root.array("bloodGlucose").map { it.jsonObject.toBloodGlucoseEntry() },
            previousBloodGlucose = root.array("previousBloodGlucose").map { it.jsonObject.toBloodGlucoseEntry() },
            baselineBloodGlucose = root.array("baselineBloodGlucose").map { it.jsonObject.toBloodGlucoseEntry() },
            skinTemperature = root.array("skinTemperature").map { it.jsonObject.toSkinTemperatureEntry() },
            previousSkinTemperature = root.array("previousSkinTemperature").map { it.jsonObject.toSkinTemperatureEntry() },
            baselineSkinTemperature = root.array("baselineSkinTemperature").map { it.jsonObject.toSkinTemperatureEntry() },
        )
    }
}

object HydrationPeriodDataCodec {
    const val Surface = "hydration-period"
    const val SchemaVersion = 1

    fun encode(data: HydrationPeriodData): String = encodeObject {
        put("dailyHydration", data.dailyHydration.toJsonArray { it.toJson() })
        put("previousDailyHydration", data.previousDailyHydration.toJsonArray { it.toJson() })
        put("baselineDailyHydration", data.baselineDailyHydration.toJsonArray { it.toJson() })
        put("hydrationEntries", data.hydrationEntries.toJsonArray { it.toJson() })
    }

    fun decode(payloadJson: String): HydrationPeriodData {
        val root = decodeObject(payloadJson)
        return HydrationPeriodData(
            dailyHydration = root.array("dailyHydration").map { it.jsonObject.toDailyHydration() },
            previousDailyHydration = root.array("previousDailyHydration").map { it.jsonObject.toDailyHydration() },
            baselineDailyHydration = root.array("baselineDailyHydration").map { it.jsonObject.toDailyHydration() },
            hydrationEntries = root.array("hydrationEntries").map { it.jsonObject.toHydrationEntry() },
        )
    }
}

object NutritionPeriodDataCodec {
    const val Surface = "nutrition-period"
    const val SchemaVersion = 1

    fun encode(data: NutritionPeriodData): String = encodeObject {
        put("dailyMacros", data.dailyMacros.toJsonArray { it.toJson() })
        put("previousDailyMacros", data.previousDailyMacros.toJsonArray { it.toJson() })
        put("baselineDailyMacros", data.baselineDailyMacros.toJsonArray { it.toJson() })
        put("entries", data.entries.toJsonArray { it.toJson() })
    }

    fun decode(payloadJson: String): NutritionPeriodData {
        val root = decodeObject(payloadJson)
        return NutritionPeriodData(
            dailyMacros = root.array("dailyMacros").map { it.jsonObject.toDailyMacros() },
            previousDailyMacros = root.array("previousDailyMacros").map { it.jsonObject.toDailyMacros() },
            baselineDailyMacros = root.array("baselineDailyMacros").map { it.jsonObject.toDailyMacros() },
            entries = root.array("entries").map { it.jsonObject.toNutritionEntry() },
        )
    }
}

object MindfulnessPeriodDataCodec {
    const val Surface = "mindfulness-period"
    const val SchemaVersion = 1

    fun encode(data: MindfulnessPeriodData): String = encodeObject {
        put("sessions", data.sessions.toJsonArray { it.toJson() })
        put("previousSessions", data.previousSessions.toJsonArray { it.toJson() })
        put("baselineSessions", data.baselineSessions.toJsonArray { it.toJson() })
    }

    fun decode(payloadJson: String): MindfulnessPeriodData {
        val root = decodeObject(payloadJson)
        return MindfulnessPeriodData(
            sessions = root.array("sessions").map { it.jsonObject.toMindfulnessSession() },
            previousSessions = root.array("previousSessions").map { it.jsonObject.toMindfulnessSession() },
            baselineSessions = root.array("baselineSessions").map { it.jsonObject.toMindfulnessSession() },
        )
    }
}

object CyclePeriodDataCodec {
    const val Surface = "cycle-period"
    const val SchemaVersion = 1

    fun encode(data: CyclePeriodData): String = encodeObject {
        put("data", data.data.toJson())
        put("missingPermissions", data.missingPermissions.toStringJsonArray())
    }

    fun decode(payloadJson: String): CyclePeriodData {
        val root = decodeObject(payloadJson)
        return CyclePeriodData(
            data = root.obj("data").toCycleData(),
            missingPermissions = root.stringSet("missingPermissions"),
        )
    }
}
