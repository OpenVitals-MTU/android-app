package tech.mmarca.openvitals.data.cache

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.BasalBodyTemperatureEntry
import tech.mmarca.openvitals.domain.model.BloodGlucoseEntry
import tech.mmarca.openvitals.domain.model.BloodPressureEntry
import tech.mmarca.openvitals.domain.model.BodyFatEntry
import tech.mmarca.openvitals.domain.model.BodyTempEntry
import tech.mmarca.openvitals.domain.model.BodyWaterMassEntry
import tech.mmarca.openvitals.domain.model.BmrEntry
import tech.mmarca.openvitals.domain.model.BoneMassEntry
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.CervicalMucusEntry
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.DailyHrv
import tech.mmarca.openvitals.domain.model.DailyHydration
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.DailyNutrition
import tech.mmarca.openvitals.domain.model.DailyRestingHR
import tech.mmarca.openvitals.domain.model.DailySteps
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.model.HeightEntry
import tech.mmarca.openvitals.domain.model.HydrationEntry
import tech.mmarca.openvitals.domain.model.IntermenstrualBleedingEntry
import tech.mmarca.openvitals.domain.model.LeanBodyMassEntry
import tech.mmarca.openvitals.domain.model.MenstruationFlowEntry
import tech.mmarca.openvitals.domain.model.MenstruationPeriodEntry
import tech.mmarca.openvitals.domain.model.MindfulnessSession
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.domain.model.NutritionNutrient
import tech.mmarca.openvitals.domain.model.OvulationTestEntry
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.RespiratoryRateEntry
import tech.mmarca.openvitals.domain.model.SexualActivityEntry
import tech.mmarca.openvitals.domain.model.SkinTemperatureEntry
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.model.SpO2Entry
import tech.mmarca.openvitals.domain.model.Vo2MaxEntry
import tech.mmarca.openvitals.domain.model.WeightEntry
import java.time.Instant

internal fun DailySteps.toJson(): JsonObject = buildJsonObject {
    put("date", date.toString())
    put("steps", steps)
    put("distanceMeters", distanceMeters)
    putNullable("wheelchairPushes", wheelchairPushes)
    putNullable("floorsClimbed", floorsClimbed)
    putNullable("activeCaloriesKcal", activeCaloriesKcal)
    putNullable("elevationGainedMeters", elevationGainedMeters)
}

internal fun JsonObject.toDailySteps(): DailySteps = DailySteps(
    date = localDate("date"),
    steps = long("steps"),
    distanceMeters = double("distanceMeters"),
    wheelchairPushes = longOrNull("wheelchairPushes"),
    floorsClimbed = intOrNull("floorsClimbed"),
    activeCaloriesKcal = doubleOrNull("activeCaloriesKcal"),
    elevationGainedMeters = doubleOrNull("elevationGainedMeters"),
)

internal fun DailyNutrition.toJson(): JsonObject = buildJsonObject {
    put("date", date.toString())
    put("hydrationLiters", hydrationLiters)
    put("caloriesBurnedKcal", caloriesBurnedKcal)
    put("caloriesBurnedSource", caloriesBurnedSource.name)
}

internal fun JsonObject.toDailyNutrition(): DailyNutrition = DailyNutrition(
    date = localDate("date"),
    hydrationLiters = double("hydrationLiters"),
    caloriesBurnedKcal = double("caloriesBurnedKcal"),
    caloriesBurnedSource = CaloriesBurnedSource.valueOf(string("caloriesBurnedSource")),
)

internal fun ActivityProgressPoint.toJson(): JsonObject = buildJsonObject {
    put("time", time.toString())
    put("totalSteps", totalSteps)
    putNullable("totalDistanceMeters", totalDistanceMeters)
    putNullable("totalCaloriesBurnedKcal", totalCaloriesBurnedKcal)
    putNullable("totalActiveCaloriesKcal", totalActiveCaloriesKcal)
    putNullable("totalWheelchairPushes", totalWheelchairPushes)
    putNullable("totalFloorsClimbed", totalFloorsClimbed)
    putNullable("totalElevationGainedMeters", totalElevationGainedMeters)
}

internal fun JsonObject.toActivityProgressPoint(): ActivityProgressPoint = ActivityProgressPoint(
    time = instant("time"),
    totalSteps = long("totalSteps"),
    totalDistanceMeters = doubleOrNull("totalDistanceMeters"),
    totalCaloriesBurnedKcal = doubleOrNull("totalCaloriesBurnedKcal"),
    totalActiveCaloriesKcal = doubleOrNull("totalActiveCaloriesKcal"),
    totalWheelchairPushes = longOrNull("totalWheelchairPushes"),
    totalFloorsClimbed = intOrNull("totalFloorsClimbed"),
    totalElevationGainedMeters = doubleOrNull("totalElevationGainedMeters"),
)

internal fun ExerciseData.toJson(): JsonObject = buildJsonObject {
    put("id", id)
    putNullable("title", title)
    put("exerciseType", exerciseType)
    put("startTime", startTime.toString())
    put("endTime", endTime.toString())
    put("durationMs", durationMs)
    put("source", source)
    putNullable("totalDistanceMeters", totalDistanceMeters)
    putNullable("totalCaloriesKcal", totalCaloriesKcal)
    putNullable("activeCaloriesKcal", activeCaloriesKcal)
    putNullable("steps", steps)
    putNullable("wheelchairPushes", wheelchairPushes)
    putNullable("averageSpeedMetersPerSecond", averageSpeedMetersPerSecond)
    putNullable("averagePowerWatts", averagePowerWatts)
    putNullable("averageStepsCadenceRate", averageStepsCadenceRate)
    putNullable("averageCyclingCadenceRpm", averageCyclingCadenceRpm)
    putNullable("floorsClimbed", floorsClimbed)
    putNullable("elevationGainedMeters", elevationGainedMeters)
    putNullable("notes", notes)
    putNullable("lastModifiedTime", lastModifiedTime?.toString())
    putNullable("clientRecordId", clientRecordId)
    putNullable("clientRecordVersion", clientRecordVersion)
    putNullable("recordingMethod", recordingMethod)
    putNullable("plannedExerciseSessionId", plannedExerciseSessionId)
    put("isOpenVitalsEntry", isOpenVitalsEntry)
    put("totalCaloriesSource", totalCaloriesSource.name)
}

internal fun JsonObject.toExerciseData(): ExerciseData = ExerciseData(
    id = string("id"),
    title = stringOrNull("title"),
    exerciseType = int("exerciseType"),
    startTime = instant("startTime"),
    endTime = instant("endTime"),
    durationMs = long("durationMs"),
    source = string("source"),
    totalDistanceMeters = doubleOrNull("totalDistanceMeters"),
    totalCaloriesKcal = doubleOrNull("totalCaloriesKcal"),
    activeCaloriesKcal = doubleOrNull("activeCaloriesKcal"),
    steps = longOrNull("steps"),
    wheelchairPushes = longOrNull("wheelchairPushes"),
    averageSpeedMetersPerSecond = doubleOrNull("averageSpeedMetersPerSecond"),
    averagePowerWatts = doubleOrNull("averagePowerWatts"),
    averageStepsCadenceRate = doubleOrNull("averageStepsCadenceRate"),
    averageCyclingCadenceRpm = doubleOrNull("averageCyclingCadenceRpm"),
    floorsClimbed = intOrNull("floorsClimbed"),
    elevationGainedMeters = doubleOrNull("elevationGainedMeters"),
    notes = stringOrNull("notes"),
    lastModifiedTime = instantOrNull("lastModifiedTime"),
    clientRecordId = stringOrNull("clientRecordId"),
    clientRecordVersion = longOrNull("clientRecordVersion"),
    recordingMethod = intOrNull("recordingMethod"),
    plannedExerciseSessionId = stringOrNull("plannedExerciseSessionId"),
    isOpenVitalsEntry = boolean("isOpenVitalsEntry"),
    totalCaloriesSource = CaloriesBurnedSource.valueOf(string("totalCaloriesSource")),
)

internal fun PlannedExerciseData.toJson(): JsonObject = buildJsonObject {
    put("id", id)
    putNullable("title", title)
    put("exerciseType", exerciseType)
    put("startTime", startTime.toString())
    put("endTime", endTime.toString())
    put("hasExplicitTime", hasExplicitTime)
    putNullable("completedExerciseSessionId", completedExerciseSessionId)
    putNullable("notes", notes)
    put("blockCount", blockCount)
    put("source", source)
    put("blocks", blocks.toJsonArray { it.toJson() })
}

internal fun JsonObject.toPlannedExerciseData(): PlannedExerciseData = PlannedExerciseData(
    id = string("id"),
    title = stringOrNull("title"),
    exerciseType = int("exerciseType"),
    startTime = instant("startTime"),
    endTime = instant("endTime"),
    hasExplicitTime = boolean("hasExplicitTime"),
    completedExerciseSessionId = stringOrNull("completedExerciseSessionId"),
    notes = stringOrNull("notes"),
    blockCount = int("blockCount"),
    source = string("source"),
    blocks = array("blocks").map { it.jsonObject.toPlannedExerciseBlockData() },
)

private fun PlannedExerciseBlockData.toJson(): JsonObject = buildJsonObject {
    put("repetitions", repetitions)
    putNullable("description", description)
    put("steps", steps.toJsonArray { it.toJson() })
}

private fun JsonObject.toPlannedExerciseBlockData(): PlannedExerciseBlockData = PlannedExerciseBlockData(
    repetitions = int("repetitions"),
    description = stringOrNull("description"),
    steps = array("steps").map { it.jsonObject.toPlannedExerciseStepData() },
)

private fun PlannedExerciseStepData.toJson(): JsonObject = buildJsonObject {
    put("exerciseType", exerciseType)
    put("exercisePhase", exercisePhase)
    putNullable("description", description)
    put("completion", completion.toJson())
}

private fun JsonObject.toPlannedExerciseStepData(): PlannedExerciseStepData = PlannedExerciseStepData(
    exerciseType = int("exerciseType"),
    exercisePhase = int("exercisePhase"),
    description = stringOrNull("description"),
    completion = obj("completion").toPlannedExerciseCompletion(),
)

private fun PlannedExerciseCompletion.toJson(): JsonObject = buildJsonObject {
    when (this@toJson) {
        is PlannedExerciseCompletion.Repetitions -> {
            put("type", "repetitions")
            put("repetitions", repetitions)
        }
        is PlannedExerciseCompletion.DurationSeconds -> {
            put("type", "durationSeconds")
            put("seconds", seconds)
        }
        PlannedExerciseCompletion.Manual -> put("type", "manual")
        PlannedExerciseCompletion.Unknown -> put("type", "unknown")
    }
}

private fun JsonObject.toPlannedExerciseCompletion(): PlannedExerciseCompletion =
    when (string("type")) {
        "repetitions" -> PlannedExerciseCompletion.Repetitions(int("repetitions"))
        "durationSeconds" -> PlannedExerciseCompletion.DurationSeconds(long("seconds"))
        "manual" -> PlannedExerciseCompletion.Manual
        else -> PlannedExerciseCompletion.Unknown
    }

internal fun SleepData.toJson(): JsonObject = buildJsonObject {
    put("id", id)
    put("startTime", startTime.toString())
    put("endTime", endTime.toString())
    put("durationMs", durationMs)
    put("source", source)
    putNullable("title", title)
    putNullable("notes", notes)
    putNullable("lastModifiedTime", lastModifiedTime?.toString())
    putNullable("clientRecordId", clientRecordId)
    putNullable("clientRecordVersion", clientRecordVersion)
    putNullable("recordingMethod", recordingMethod)
    put("stages", stages.toJsonArray { it.toJson() })
}

internal fun JsonObject.toSleepData(): SleepData = SleepData(
    id = string("id"),
    startTime = instant("startTime"),
    endTime = instant("endTime"),
    durationMs = long("durationMs"),
    source = string("source"),
    title = stringOrNull("title"),
    notes = stringOrNull("notes"),
    lastModifiedTime = instantOrNull("lastModifiedTime"),
    clientRecordId = stringOrNull("clientRecordId"),
    clientRecordVersion = longOrNull("clientRecordVersion"),
    recordingMethod = intOrNull("recordingMethod"),
    stages = array("stages").map { it.jsonObject.toSleepStage() },
)

private fun SleepStage.toJson(): JsonObject = buildJsonObject {
    put("startTime", startTime.toString())
    put("endTime", endTime.toString())
    put("stageType", stageType)
}

private fun JsonObject.toSleepStage(): SleepStage = SleepStage(
    startTime = instant("startTime"),
    endTime = instant("endTime"),
    stageType = int("stageType"),
)

internal fun HeartRateSample.toJson(): JsonObject = buildJsonObject {
    put("time", time.toString())
    put("beatsPerMinute", beatsPerMinute)
    put("source", source)
}

internal fun JsonObject.toHeartRateSample(): HeartRateSample = HeartRateSample(
    time = instant("time"),
    beatsPerMinute = long("beatsPerMinute"),
    source = string("source"),
)

internal fun HeartRateSummary.toJson(): JsonObject = buildJsonObject {
    put("date", date.toString())
    put("avgBpm", avgBpm)
    put("minBpm", minBpm)
    put("maxBpm", maxBpm)
}

internal fun JsonObject.toHeartRateSummary(): HeartRateSummary = HeartRateSummary(
    date = localDate("date"),
    avgBpm = long("avgBpm"),
    minBpm = long("minBpm"),
    maxBpm = long("maxBpm"),
)

internal fun DailyRestingHR.toJson(): JsonObject = buildJsonObject {
    put("date", date.toString())
    put("bpm", bpm)
}

internal fun JsonObject.toDailyRestingHR(): DailyRestingHR = DailyRestingHR(localDate("date"), long("bpm"))

internal fun DailyHrv.toJson(): JsonObject = buildJsonObject {
    put("date", date.toString())
    put("rmssdMs", rmssdMs)
}

internal fun JsonObject.toDailyHrv(): DailyHrv = DailyHrv(localDate("date"), double("rmssdMs"))

internal fun DailyHydration.toJson(): JsonObject = buildJsonObject {
    put("date", date.toString())
    put("liters", liters)
}

internal fun JsonObject.toDailyHydration(): DailyHydration = DailyHydration(localDate("date"), double("liters"))

internal fun HydrationEntry.toJson(): JsonObject = buildJsonObject {
    put("startTime", startTime.toString())
    put("endTime", endTime.toString())
    put("liters", liters)
    put("source", source)
    put("id", id)
    put("isOpenVitalsEntry", isOpenVitalsEntry)
}

internal fun JsonObject.toHydrationEntry(): HydrationEntry = HydrationEntry(
    startTime = instant("startTime"),
    endTime = instant("endTime"),
    liters = double("liters"),
    source = string("source"),
    id = string("id"),
    isOpenVitalsEntry = boolean("isOpenVitalsEntry"),
)

internal fun DailyMacros.toJson(): JsonObject = buildJsonObject {
    put("date", date.toString())
    put("nutrientValues", nutrientValues.entries.toJsonArray { (nutrient, value) ->
        buildJsonObject {
            put("nutrient", nutrient.name)
            put("value", value)
        }
    })
}

internal fun JsonObject.toDailyMacros(): DailyMacros = DailyMacros(
    date = localDate("date"),
    nutrientValues = array("nutrientValues").associate { element ->
        val obj = element.jsonObject
        NutritionNutrient.valueOf(obj.string("nutrient")) to obj.double("value")
    },
)

internal fun NutritionEntry.toJson(): JsonObject = buildJsonObject {
    put("time", time.toString())
    put("mealType", mealType)
    putNullable("name", name)
    putNullable("energyKcal", energyKcal)
    putNullable("proteinGrams", proteinGrams)
    putNullable("carbsGrams", carbsGrams)
    putNullable("fatGrams", fatGrams)
    putNullable("fiberGrams", fiberGrams)
    putNullable("sugarGrams", sugarGrams)
    put("source", source)
    put("nutrientValues", nutrientValues.entries.toJsonArray { (nutrient, value) ->
        buildJsonObject {
            put("nutrient", nutrient.name)
            put("value", value)
        }
    })
}

internal fun JsonObject.toNutritionEntry(): NutritionEntry = NutritionEntry(
    time = instant("time"),
    mealType = int("mealType"),
    name = stringOrNull("name"),
    energyKcal = doubleOrNull("energyKcal"),
    proteinGrams = doubleOrNull("proteinGrams"),
    carbsGrams = doubleOrNull("carbsGrams"),
    fatGrams = doubleOrNull("fatGrams"),
    fiberGrams = doubleOrNull("fiberGrams"),
    sugarGrams = doubleOrNull("sugarGrams"),
    source = string("source"),
    nutrientValues = array("nutrientValues").associate { element ->
        val obj = element.jsonObject
        NutritionNutrient.valueOf(obj.string("nutrient")) to obj.double("value")
    },
)

internal fun MindfulnessSession.toJson(): JsonObject = buildJsonObject {
    put("id", id)
    putNullable("title", title)
    put("startTime", startTime.toString())
    put("endTime", endTime.toString())
    put("durationMs", durationMs)
    put("source", source)
    put("isOpenVitalsEntry", isOpenVitalsEntry)
}

internal fun JsonObject.toMindfulnessSession(): MindfulnessSession = MindfulnessSession(
    id = string("id"),
    title = stringOrNull("title"),
    startTime = instant("startTime"),
    endTime = instant("endTime"),
    durationMs = long("durationMs"),
    source = string("source"),
    isOpenVitalsEntry = boolean("isOpenVitalsEntry"),
)

internal fun WeightEntry.toJson(): JsonObject = entryJson(time, source, id, isOpenVitalsEntry) { put("weightKg", weightKg) }
internal fun JsonObject.toWeightEntry(): WeightEntry = WeightEntry(instant("time"), double("weightKg"), string("source"), string("id"), boolean("isOpenVitalsEntry"))
internal fun HeightEntry.toJson(): JsonObject = entryJson(time, source, id, isOpenVitalsEntry) { put("heightCm", heightCm) }
internal fun JsonObject.toHeightEntry(): HeightEntry = HeightEntry(instant("time"), double("heightCm"), string("source"), string("id"), boolean("isOpenVitalsEntry"))
internal fun BodyFatEntry.toJson(): JsonObject = entryJson(time, source, id, isOpenVitalsEntry) { put("percent", percent) }
internal fun JsonObject.toBodyFatEntry(): BodyFatEntry = BodyFatEntry(instant("time"), double("percent"), string("source"), string("id"), boolean("isOpenVitalsEntry"))
internal fun LeanBodyMassEntry.toJson(): JsonObject = simpleMassJson(time, massKg, source)
internal fun JsonObject.toLeanBodyMassEntry(): LeanBodyMassEntry = LeanBodyMassEntry(instant("time"), double("massKg"), string("source"))
internal fun BmrEntry.toJson(): JsonObject = buildJsonObject { put("time", time.toString()); put("kcalPerDay", kcalPerDay); put("source", source) }
internal fun JsonObject.toBmrEntry(): BmrEntry = BmrEntry(instant("time"), double("kcalPerDay"), string("source"))
internal fun BoneMassEntry.toJson(): JsonObject = simpleMassJson(time, massKg, source)
internal fun JsonObject.toBoneMassEntry(): BoneMassEntry = BoneMassEntry(instant("time"), double("massKg"), string("source"))
internal fun BodyWaterMassEntry.toJson(): JsonObject = simpleMassJson(time, massKg, source)
internal fun JsonObject.toBodyWaterMassEntry(): BodyWaterMassEntry = BodyWaterMassEntry(instant("time"), double("massKg"), string("source"))

internal fun BloodPressureEntry.toJson(): JsonObject = entryJson(time, source, id, isOpenVitalsEntry) {
    put("systolicMmHg", systolicMmHg)
    put("diastolicMmHg", diastolicMmHg)
}
internal fun JsonObject.toBloodPressureEntry(): BloodPressureEntry = BloodPressureEntry(instant("time"), int("systolicMmHg"), int("diastolicMmHg"), string("source"), string("id"), boolean("isOpenVitalsEntry"))
internal fun SpO2Entry.toJson(): JsonObject = entryJson(time, source, id, isOpenVitalsEntry) { put("percent", percent) }
internal fun JsonObject.toSpO2Entry(): SpO2Entry = SpO2Entry(instant("time"), double("percent"), string("source"), string("id"), boolean("isOpenVitalsEntry"))
internal fun RespiratoryRateEntry.toJson(): JsonObject = entryJson(time, source, id, isOpenVitalsEntry) { put("breathsPerMinute", breathsPerMinute) }
internal fun JsonObject.toRespiratoryRateEntry(): RespiratoryRateEntry = RespiratoryRateEntry(instant("time"), double("breathsPerMinute"), string("source"), string("id"), boolean("isOpenVitalsEntry"))
internal fun BodyTempEntry.toJson(): JsonObject = entryJson(time, source, id, isOpenVitalsEntry) { put("temperatureCelsius", temperatureCelsius) }
internal fun JsonObject.toBodyTempEntry(): BodyTempEntry = BodyTempEntry(instant("time"), double("temperatureCelsius"), string("source"), string("id"), boolean("isOpenVitalsEntry"))
internal fun BloodGlucoseEntry.toJson(): JsonObject = buildJsonObject {
    put("time", time.toString())
    put("millimolesPerLiter", millimolesPerLiter)
    put("specimenSource", specimenSource)
    put("mealType", mealType)
    put("relationToMeal", relationToMeal)
    put("source", source)
}
internal fun JsonObject.toBloodGlucoseEntry(): BloodGlucoseEntry = BloodGlucoseEntry(instant("time"), double("millimolesPerLiter"), int("specimenSource"), int("mealType"), int("relationToMeal"), string("source"))
internal fun SkinTemperatureEntry.toJson(): JsonObject = buildJsonObject {
    put("startTime", startTime.toString())
    put("endTime", endTime.toString())
    putNullable("baselineCelsius", baselineCelsius)
    putNullable("averageDeltaCelsius", averageDeltaCelsius)
    putNullable("minDeltaCelsius", minDeltaCelsius)
    putNullable("maxDeltaCelsius", maxDeltaCelsius)
    put("measurementLocation", measurementLocation)
    put("source", source)
}
internal fun JsonObject.toSkinTemperatureEntry(): SkinTemperatureEntry = SkinTemperatureEntry(instant("startTime"), instant("endTime"), doubleOrNull("baselineCelsius"), doubleOrNull("averageDeltaCelsius"), doubleOrNull("minDeltaCelsius"), doubleOrNull("maxDeltaCelsius"), int("measurementLocation"), string("source"))
internal fun Vo2MaxEntry.toJson(): JsonObject = buildJsonObject { put("time", time.toString()); put("vo2MaxMlPerKgPerMin", vo2MaxMlPerKgPerMin); put("source", source) }
internal fun JsonObject.toVo2MaxEntry(): Vo2MaxEntry = Vo2MaxEntry(instant("time"), double("vo2MaxMlPerKgPerMin"), string("source"))

internal fun CycleData.toJson(): JsonObject = buildJsonObject {
    put("menstruationFlows", menstruationFlows.toJsonArray { it.toJson() })
    put("menstruationPeriods", menstruationPeriods.toJsonArray { it.toJson() })
    put("ovulationTests", ovulationTests.toJsonArray { it.toJson() })
    put("cervicalMucus", cervicalMucus.toJsonArray { it.toJson() })
    put("basalBodyTemperature", basalBodyTemperature.toJsonArray { it.toJson() })
    put("intermenstrualBleeding", intermenstrualBleeding.toJsonArray { it.toJson() })
    put("sexualActivity", sexualActivity.toJsonArray { it.toJson() })
}

internal fun JsonObject.toCycleData(): CycleData = CycleData(
    menstruationFlows = array("menstruationFlows").map { it.jsonObject.toMenstruationFlowEntry() },
    menstruationPeriods = array("menstruationPeriods").map { it.jsonObject.toMenstruationPeriodEntry() },
    ovulationTests = array("ovulationTests").map { it.jsonObject.toOvulationTestEntry() },
    cervicalMucus = array("cervicalMucus").map { it.jsonObject.toCervicalMucusEntry() },
    basalBodyTemperature = array("basalBodyTemperature").map { it.jsonObject.toBasalBodyTemperatureEntry() },
    intermenstrualBleeding = array("intermenstrualBleeding").map { it.jsonObject.toIntermenstrualBleedingEntry() },
    sexualActivity = array("sexualActivity").map { it.jsonObject.toSexualActivityEntry() },
)

private fun MenstruationFlowEntry.toJson(): JsonObject = buildJsonObject { put("time", time.toString()); put("flow", flow); put("source", source) }
private fun JsonObject.toMenstruationFlowEntry(): MenstruationFlowEntry = MenstruationFlowEntry(instant("time"), int("flow"), string("source"))
private fun MenstruationPeriodEntry.toJson(): JsonObject = buildJsonObject { put("startTime", startTime.toString()); put("endTime", endTime.toString()); put("source", source) }
private fun JsonObject.toMenstruationPeriodEntry(): MenstruationPeriodEntry = MenstruationPeriodEntry(instant("startTime"), instant("endTime"), string("source"))
private fun OvulationTestEntry.toJson(): JsonObject = buildJsonObject { put("time", time.toString()); put("result", result); put("source", source) }
private fun JsonObject.toOvulationTestEntry(): OvulationTestEntry = OvulationTestEntry(instant("time"), int("result"), string("source"))
private fun CervicalMucusEntry.toJson(): JsonObject = buildJsonObject { put("time", time.toString()); put("appearance", appearance); put("sensation", sensation); put("source", source) }
private fun JsonObject.toCervicalMucusEntry(): CervicalMucusEntry = CervicalMucusEntry(instant("time"), int("appearance"), int("sensation"), string("source"))
private fun BasalBodyTemperatureEntry.toJson(): JsonObject = buildJsonObject { put("time", time.toString()); put("temperatureCelsius", temperatureCelsius); put("measurementLocation", measurementLocation); put("source", source) }
private fun JsonObject.toBasalBodyTemperatureEntry(): BasalBodyTemperatureEntry = BasalBodyTemperatureEntry(instant("time"), double("temperatureCelsius"), int("measurementLocation"), string("source"))
private fun IntermenstrualBleedingEntry.toJson(): JsonObject = buildJsonObject { put("time", time.toString()); put("source", source) }
private fun JsonObject.toIntermenstrualBleedingEntry(): IntermenstrualBleedingEntry = IntermenstrualBleedingEntry(instant("time"), string("source"))
private fun SexualActivityEntry.toJson(): JsonObject = buildJsonObject { put("time", time.toString()); put("protectionUsed", protectionUsed); put("source", source) }
private fun JsonObject.toSexualActivityEntry(): SexualActivityEntry = SexualActivityEntry(instant("time"), int("protectionUsed"), string("source"))

private fun entryJson(
    time: Instant,
    source: String,
    id: String,
    isOpenVitalsEntry: Boolean,
    extra: kotlinx.serialization.json.JsonObjectBuilder.() -> Unit,
): JsonObject = buildJsonObject {
    put("time", time.toString())
    put("source", source)
    put("id", id)
    put("isOpenVitalsEntry", isOpenVitalsEntry)
    extra()
}

private fun simpleMassJson(time: Instant, massKg: Double, source: String): JsonObject = buildJsonObject {
    put("time", time.toString())
    put("massKg", massKg)
    put("source", source)
}
