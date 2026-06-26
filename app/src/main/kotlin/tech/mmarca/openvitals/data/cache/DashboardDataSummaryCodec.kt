package tech.mmarca.openvitals.data.cache

import java.time.LocalDate
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import tech.mmarca.openvitals.domain.insights.CardioLoadConfidence
import tech.mmarca.openvitals.domain.insights.IntensityMinutesConfidence
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.domain.insights.SleepScoreEstimate
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoadTargetSource
import tech.mmarca.openvitals.domain.model.DashboardWeeklyIntensityMinutes

object DashboardDataSummaryCodec {
    const val Surface = "dashboard"
    const val SchemaVersion = 2

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(data: DashboardData): String =
        json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("date", data.date.toString())
                put("steps", data.steps)
                put("distanceMeters", data.distanceMeters)
                put("caloriesKcal", data.caloriesKcal)
                putNullable("activeCaloriesKcal", data.activeCaloriesKcal)
                put("hydrationLiters", data.hydrationLiters)
                putNullable("workout", data.workout?.toJson())
                put("workouts", data.workouts.toJsonArray { it.toJson() })
                putNullable("sleep", data.sleep?.toJson())
                put("sleepScore", data.sleepScore.toJson())
                putNullable("weightKg", data.weightKg)
                putNullable("weightTime", data.weightTime?.toString())
                putNullable("heightCm", data.heightCm)
                putNullable("heightTime", data.heightTime?.toString())
                putNullable("bmi", data.bmi)
                putNullable("ffmi", data.ffmi)
                put("avgHeartRateBpm", data.avgHeartRateBpm)
                put("heartRateSampleCount", data.heartRateSampleCount)
                putNullable("heartRateSampleStartTime", data.heartRateSampleStartTime?.toString())
                putNullable("heartRateSampleEndTime", data.heartRateSampleEndTime?.toString())
                put("restingHeartRateBpm", data.restingHeartRateBpm)
                putNullable("restingHeartRateBaselineBpm", data.restingHeartRateBaselineBpm)
                putNullable("hrvRmssdMs", data.hrvRmssdMs)
                putNullable("hrvBaselineRmssdMs", data.hrvBaselineRmssdMs)
                put("hrvSampleCount", data.hrvSampleCount)
                putNullable("hrvSampleStartTime", data.hrvSampleStartTime?.toString())
                putNullable("hrvSampleEndTime", data.hrvSampleEndTime?.toString())
                put("bodyFatPercent", data.bodyFatPercent)
                putNullable("leanMassKg", data.leanMassKg)
                putNullable("bmrKcal", data.bmrKcal)
                putNullable("boneMassKg", data.boneMassKg)
                putNullable("bodyWaterMassKg", data.bodyWaterMassKg)
                putNullable("caloriesInKcal", data.caloriesInKcal)
                putNullable("proteinGrams", data.proteinGrams)
                putNullable("carbsGrams", data.carbsGrams)
                putNullable("fatGrams", data.fatGrams)
                putNullable("latestSystolicMmHg", data.latestSystolicMmHg)
                putNullable("latestDiastolicMmHg", data.latestDiastolicMmHg)
                putNullable("latestSpO2Percent", data.latestSpO2Percent)
                putNullable("latestVo2Max", data.latestVo2Max)
                putNullable("avgRespiratoryRate", data.avgRespiratoryRate)
                putNullable("latestBodyTemperatureCelsius", data.latestBodyTemperatureCelsius)
                putNullable("latestBloodGlucoseMillimolesPerLiter", data.latestBloodGlucoseMillimolesPerLiter)
                putNullable("latestSkinTemperatureDeltaCelsius", data.latestSkinTemperatureDeltaCelsius)
                putNullable("weeklyCardioLoad", data.weeklyCardioLoad?.toJson())
                putNullable("weeklyIntensityMinutes", data.weeklyIntensityMinutes?.toJson())
                putNullable("floorsClimbed", data.floorsClimbed)
                putNullable("elevationGainedMeters", data.elevationGainedMeters)
                putNullable("wheelchairPushes", data.wheelchairPushes)
                putNullable("mindfulnessMinutes", data.mindfulnessMinutes)
                putNullable("menstruationPeriodDays", data.menstruationPeriodDays)
                putNullable("ovulationTestCount", data.ovulationTestCount)
                putNullable("latestBasalBodyTemperatureCelsius", data.latestBasalBodyTemperatureCelsius)
                put("missingPermissions", data.missingPermissions.sorted().toJsonArray { JsonPrimitive(it) })
                put("loadedMetrics", data.loadedMetrics.map { it.name }.sorted().toJsonArray { JsonPrimitive(it) })
                put("caloriesKcalSource", data.caloriesKcalSource.name)
            }
        )

    fun decode(payloadJson: String): DashboardData {
        val root = json.parseToJsonElement(payloadJson).jsonObject
        return DashboardData(
            date = LocalDate.parse(root.string("date")),
            steps = root.long("steps"),
            distanceMeters = root.double("distanceMeters"),
            caloriesKcal = root.double("caloriesKcal"),
            activeCaloriesKcal = root.doubleOrNull("activeCaloriesKcal"),
            hydrationLiters = root.double("hydrationLiters"),
            workout = root.objOrNull("workout")?.toExerciseData(),
            workouts = root.array("workouts").map { it.jsonObject.toExerciseData() },
            sleep = root.objOrNull("sleep")?.toSleepData(),
            sleepScore = root.obj("sleepScore").toSleepScoreEstimate(),
            weightKg = root.doubleOrNull("weightKg"),
            weightTime = root.instantOrNull("weightTime"),
            heightCm = root.doubleOrNull("heightCm"),
            heightTime = root.instantOrNull("heightTime"),
            bmi = root.doubleOrNull("bmi"),
            ffmi = root.doubleOrNull("ffmi"),
            avgHeartRateBpm = root.long("avgHeartRateBpm"),
            heartRateSampleCount = root.int("heartRateSampleCount"),
            heartRateSampleStartTime = root.instantOrNull("heartRateSampleStartTime"),
            heartRateSampleEndTime = root.instantOrNull("heartRateSampleEndTime"),
            restingHeartRateBpm = root.long("restingHeartRateBpm"),
            restingHeartRateBaselineBpm = root.longOrNull("restingHeartRateBaselineBpm"),
            hrvRmssdMs = root.doubleOrNull("hrvRmssdMs"),
            hrvBaselineRmssdMs = root.doubleOrNull("hrvBaselineRmssdMs"),
            hrvSampleCount = root.int("hrvSampleCount"),
            hrvSampleStartTime = root.instantOrNull("hrvSampleStartTime"),
            hrvSampleEndTime = root.instantOrNull("hrvSampleEndTime"),
            bodyFatPercent = root.double("bodyFatPercent"),
            leanMassKg = root.doubleOrNull("leanMassKg"),
            bmrKcal = root.doubleOrNull("bmrKcal"),
            boneMassKg = root.doubleOrNull("boneMassKg"),
            bodyWaterMassKg = root.doubleOrNull("bodyWaterMassKg"),
            caloriesInKcal = root.doubleOrNull("caloriesInKcal"),
            proteinGrams = root.doubleOrNull("proteinGrams"),
            carbsGrams = root.doubleOrNull("carbsGrams"),
            fatGrams = root.doubleOrNull("fatGrams"),
            latestSystolicMmHg = root.intOrNull("latestSystolicMmHg"),
            latestDiastolicMmHg = root.intOrNull("latestDiastolicMmHg"),
            latestSpO2Percent = root.doubleOrNull("latestSpO2Percent"),
            latestVo2Max = root.doubleOrNull("latestVo2Max"),
            avgRespiratoryRate = root.doubleOrNull("avgRespiratoryRate"),
            latestBodyTemperatureCelsius = root.doubleOrNull("latestBodyTemperatureCelsius"),
            latestBloodGlucoseMillimolesPerLiter = root.doubleOrNull("latestBloodGlucoseMillimolesPerLiter"),
            latestSkinTemperatureDeltaCelsius = root.doubleOrNull("latestSkinTemperatureDeltaCelsius"),
            weeklyCardioLoad = root.objOrNull("weeklyCardioLoad")?.toWeeklyCardioLoad(),
            weeklyIntensityMinutes = root.objOrNull("weeklyIntensityMinutes")?.toWeeklyIntensityMinutes(),
            floorsClimbed = root.intOrNull("floorsClimbed"),
            elevationGainedMeters = root.doubleOrNull("elevationGainedMeters"),
            wheelchairPushes = root.longOrNull("wheelchairPushes"),
            mindfulnessMinutes = root.intOrNull("mindfulnessMinutes"),
            menstruationPeriodDays = root.intOrNull("menstruationPeriodDays"),
            ovulationTestCount = root.intOrNull("ovulationTestCount"),
            latestBasalBodyTemperatureCelsius = root.doubleOrNull("latestBasalBodyTemperatureCelsius"),
            missingPermissions = root.array("missingPermissions").map { it.jsonPrimitive.content }.toSet(),
            loadedMetrics = root.array("loadedMetrics").map { DashboardMetric.valueOf(it.jsonPrimitive.content) }.toSet(),
            caloriesKcalSource = CaloriesBurnedSource.valueOf(root.string("caloriesKcalSource")),
        )
    }
}

private fun SleepScoreEstimate.toJson(): JsonObject =
    buildJsonObject {
        put("score", score)
        put("confidence", confidence.name)
        put("durationPoints", durationPoints)
        put("efficiencyPoints", efficiencyPoints)
        put("continuityPoints", continuityPoints)
        put("regularityPoints", regularityPoints)
        put("sleepDurationMinutes", sleepDurationMinutes)
        put("timeInBedMinutes", timeInBedMinutes)
        put("sleepEfficiencyPercent", sleepEfficiencyPercent)
        put("wakeAfterSleepOnsetMinutes", wakeAfterSleepOnsetMinutes)
        putNullable("regularityDifferenceMinutes", regularityDifferenceMinutes)
        put("regularityBaselineNights", regularityBaselineNights)
        put("sleepStageCount", sleepStageCount)
        put("usesSleepStages", usesSleepStages)
        put("usesExplicitAwakeStages", usesExplicitAwakeStages)
    }

private fun JsonObject.toSleepScoreEstimate(): SleepScoreEstimate =
    SleepScoreEstimate(
        score = int("score"),
        confidence = SleepScoreConfidence.valueOf(string("confidence")),
        durationPoints = double("durationPoints"),
        efficiencyPoints = double("efficiencyPoints"),
        continuityPoints = double("continuityPoints"),
        regularityPoints = double("regularityPoints"),
        sleepDurationMinutes = double("sleepDurationMinutes"),
        timeInBedMinutes = double("timeInBedMinutes"),
        sleepEfficiencyPercent = double("sleepEfficiencyPercent"),
        wakeAfterSleepOnsetMinutes = double("wakeAfterSleepOnsetMinutes"),
        regularityDifferenceMinutes = doubleOrNull("regularityDifferenceMinutes"),
        regularityBaselineNights = int("regularityBaselineNights"),
        sleepStageCount = int("sleepStageCount"),
        usesSleepStages = boolean("usesSleepStages"),
        usesExplicitAwakeStages = boolean("usesExplicitAwakeStages"),
    )

private fun DashboardWeeklyCardioLoad.toJson(): JsonObject =
    buildJsonObject {
        put("currentScore", currentScore)
        put("targetScore", targetScore)
        put("todayScore", todayScore)
        put("confidence", confidence.name)
        put("targetSource", targetSource.name)
    }

private fun JsonObject.toWeeklyCardioLoad(): DashboardWeeklyCardioLoad =
    DashboardWeeklyCardioLoad(
        currentScore = int("currentScore"),
        targetScore = int("targetScore"),
        todayScore = int("todayScore"),
        confidence = CardioLoadConfidence.valueOf(string("confidence")),
        targetSource = DashboardWeeklyCardioLoadTargetSource.valueOf(string("targetSource")),
    )

private fun DashboardWeeklyIntensityMinutes.toJson(): JsonObject =
    buildJsonObject {
        put("moderateMinutes", moderateMinutes)
        put("vigorousMinutes", vigorousMinutes)
        put("moderateEquivalentMinutes", moderateEquivalentMinutes)
        put("targetMinutes", targetMinutes)
        put("todayModerateEquivalentMinutes", todayModerateEquivalentMinutes)
        put("daysElapsed", daysElapsed)
        put("confidence", confidence.name)
    }

private fun JsonObject.toWeeklyIntensityMinutes(): DashboardWeeklyIntensityMinutes =
    DashboardWeeklyIntensityMinutes(
        moderateMinutes = int("moderateMinutes"),
        vigorousMinutes = int("vigorousMinutes"),
        moderateEquivalentMinutes = int("moderateEquivalentMinutes"),
        targetMinutes = int("targetMinutes"),
        todayModerateEquivalentMinutes = int("todayModerateEquivalentMinutes"),
        daysElapsed = int("daysElapsed"),
        confidence = IntensityMinutesConfidence.valueOf(string("confidence")),
    )
