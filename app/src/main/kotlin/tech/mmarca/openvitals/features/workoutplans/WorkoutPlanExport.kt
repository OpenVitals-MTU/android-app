package tech.mmarca.openvitals.features.workoutplans

import java.time.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExercisePerformanceTarget
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.PlannedExerciseWriteRequest

/**
 * Plans as a JSON file: a backup that does not depend on Health Connect
 * keeping them, and a way to carry a routine to another phone. Everything the
 * app reads from a plan is written, including goals and targets it cannot
 * edit, so a foreign plan survives the trip. Hand-built JSON because the
 * project uses the serialization runtime without its compiler plugin.
 */
object WorkoutPlanExport {
    const val FormatVersion = 1
    const val MimeType = "application/json"
    const val FileName = "openvitals-workout-plans.json"
}

private val json = Json { prettyPrint = true }

fun List<PlannedExerciseData>.toExportJson(exportedAt: Instant): String =
    json.encodeToString(
        JsonObject.serializer(),
        buildJsonObject {
            put("format", WorkoutPlanExport.FormatVersion)
            put("exportedAt", exportedAt.toEpochMilli())
            put(
                "plans",
                buildJsonArray {
                    this@toExportJson.forEach { plan ->
                        add(
                            buildJsonObject {
                                put("title", plan.title)
                                put("notes", plan.notes)
                                put("exerciseType", plan.exerciseType)
                                put("startEpochMillis", plan.startTime.toEpochMilli())
                                put("endEpochMillis", plan.endTime.toEpochMilli())
                                put("zoneOffsetSeconds", plan.startZoneOffset?.totalSeconds)
                                put("completed", plan.completedExerciseSessionId != null)
                                put("blocks", JsonArray(plan.blocks.map { it.toJson() }))
                            },
                        )
                    }
                },
            )
        },
    )

/** Null when the text is not a plan export this version understands. */
fun parseWorkoutPlanExport(text: String): List<PlannedExerciseWriteRequest>? {
    val root = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return null
    val format = root["format"]?.jsonPrimitive?.intOrNull ?: return null
    if (format > WorkoutPlanExport.FormatVersion) return null
    val plans = root["plans"]?.let { runCatching { it.jsonArray }.getOrNull() } ?: return null
    return plans.mapNotNull { element ->
        val plan = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
        PlannedExerciseWriteRequest(
            id = null,
            exerciseType = plan.int("exerciseType") ?: return@mapNotNull null,
            startTime = plan.long("startEpochMillis")?.let(Instant::ofEpochMilli) ?: return@mapNotNull null,
            endTime = plan.long("endEpochMillis")?.let(Instant::ofEpochMilli) ?: return@mapNotNull null,
            title = plan.string("title"),
            notes = plan.string("notes"),
            blocks = plan["blocks"]?.let { runCatching { it.jsonArray }.getOrNull() }
                ?.mapNotNull { block -> runCatching { block.jsonObject }.getOrNull()?.toBlock() }
                .orEmpty(),
        )
    }
}

private fun PlannedExerciseBlockData.toJson(): JsonObject = buildJsonObject {
    put("rounds", repetitions)
    put("description", description)
    put(
        "steps",
        JsonArray(
            steps.map { step ->
                buildJsonObject {
                    put("segmentType", step.exerciseType)
                    put("phase", step.exercisePhase)
                    put("description", step.description)
                    put("goal", step.completion.toJson())
                    put("targets", JsonArray(step.performanceTargets.map { it.toJson() }))
                }
            },
        ),
    )
}

private fun JsonObject.toBlock(): PlannedExerciseBlockData = PlannedExerciseBlockData(
    repetitions = (int("rounds") ?: 1).coerceAtLeast(1),
    description = string("description"),
    steps = this["steps"]?.let { runCatching { it.jsonArray }.getOrNull() }
        ?.mapNotNull { element ->
            val step = runCatching { element.jsonObject }.getOrNull() ?: return@mapNotNull null
            PlannedExerciseStepData(
                exerciseType = step.int("segmentType") ?: return@mapNotNull null,
                exercisePhase = step.int("phase") ?: return@mapNotNull null,
                description = step.string("description"),
                completion = step["goal"]?.let { runCatching { it.jsonObject }.getOrNull() }?.toCompletion()
                    ?: PlannedExerciseCompletion.Unknown,
                performanceTargets = step["targets"]?.let { runCatching { it.jsonArray }.getOrNull() }
                    ?.mapNotNull { target -> runCatching { target.jsonObject }.getOrNull()?.toTarget() }
                    .orEmpty(),
            )
        }
        .orEmpty(),
)

private fun PlannedExerciseCompletion.toJson(): JsonObject = buildJsonObject {
    when (val goal = this@toJson) {
        is PlannedExerciseCompletion.Repetitions -> { put("kind", "repetitions"); put("repetitions", goal.repetitions) }
        is PlannedExerciseCompletion.DurationSeconds -> { put("kind", "duration"); put("seconds", goal.seconds) }
        is PlannedExerciseCompletion.DistanceMeters -> { put("kind", "distance"); put("meters", goal.meters) }
        is PlannedExerciseCompletion.DistanceAndDuration -> {
            put("kind", "distance_duration"); put("meters", goal.meters); put("seconds", goal.seconds)
        }
        is PlannedExerciseCompletion.Steps -> { put("kind", "steps"); put("steps", goal.steps) }
        is PlannedExerciseCompletion.ActiveCaloriesKcal -> { put("kind", "active_calories"); put("kcal", goal.kcal) }
        is PlannedExerciseCompletion.TotalCaloriesKcal -> { put("kind", "total_calories"); put("kcal", goal.kcal) }
        PlannedExerciseCompletion.Manual -> put("kind", "manual")
        PlannedExerciseCompletion.Unknown -> put("kind", "unknown")
    }
}

private fun JsonObject.toCompletion(): PlannedExerciseCompletion = when (string("kind")) {
    "repetitions" -> int("repetitions")?.let { PlannedExerciseCompletion.Repetitions(it) }
    "duration" -> long("seconds")?.let { PlannedExerciseCompletion.DurationSeconds(it) }
    "distance" -> double("meters")?.let { PlannedExerciseCompletion.DistanceMeters(it) }
    "distance_duration" -> {
        val meters = double("meters"); val seconds = long("seconds")
        if (meters != null && seconds != null) PlannedExerciseCompletion.DistanceAndDuration(meters, seconds) else null
    }
    "steps" -> int("steps")?.let { PlannedExerciseCompletion.Steps(it) }
    "active_calories" -> double("kcal")?.let { PlannedExerciseCompletion.ActiveCaloriesKcal(it) }
    "total_calories" -> double("kcal")?.let { PlannedExerciseCompletion.TotalCaloriesKcal(it) }
    "manual" -> PlannedExerciseCompletion.Manual
    else -> null
} ?: PlannedExerciseCompletion.Unknown

private fun PlannedExercisePerformanceTarget.toJson(): JsonObject = buildJsonObject {
    when (val target = this@toJson) {
        is PlannedExercisePerformanceTarget.Power -> { put("kind", "power"); put("min", target.minWatts); put("max", target.maxWatts) }
        is PlannedExercisePerformanceTarget.Speed -> {
            put("kind", "speed"); put("min", target.minMetersPerSecond); put("max", target.maxMetersPerSecond)
        }
        is PlannedExercisePerformanceTarget.Cadence -> { put("kind", "cadence"); put("min", target.minRpm); put("max", target.maxRpm) }
        is PlannedExercisePerformanceTarget.HeartRate -> { put("kind", "heart_rate"); put("min", target.minBpm); put("max", target.maxBpm) }
        is PlannedExercisePerformanceTarget.Weight -> { put("kind", "weight"); put("value", target.kilograms) }
        is PlannedExercisePerformanceTarget.RateOfPerceivedExertion -> { put("kind", "rpe"); put("value", target.rpe) }
        PlannedExercisePerformanceTarget.Amrap -> put("kind", "amrap")
        PlannedExercisePerformanceTarget.Unknown -> put("kind", "unknown")
    }
}

private fun JsonObject.toTarget(): PlannedExercisePerformanceTarget {
    val min = double("min"); val max = double("max")
    return when (string("kind")) {
        "power" -> if (min != null && max != null) PlannedExercisePerformanceTarget.Power(min, max) else null
        "speed" -> if (min != null && max != null) PlannedExercisePerformanceTarget.Speed(min, max) else null
        "cadence" -> if (min != null && max != null) PlannedExercisePerformanceTarget.Cadence(min, max) else null
        "heart_rate" -> if (min != null && max != null) PlannedExercisePerformanceTarget.HeartRate(min, max) else null
        "weight" -> double("value")?.let { PlannedExercisePerformanceTarget.Weight(it) }
        "rpe" -> int("value")?.let { PlannedExercisePerformanceTarget.RateOfPerceivedExertion(it) }
        "amrap" -> PlannedExercisePerformanceTarget.Amrap
        else -> null
    } ?: PlannedExercisePerformanceTarget.Unknown
}

private fun JsonObject.primitive(key: String): JsonPrimitive? =
    this[key]?.takeIf { it !is JsonNull }?.let { runCatching { it.jsonPrimitive }.getOrNull() }

private fun JsonObject.string(key: String): String? = primitive(key)?.contentOrNull
private fun JsonObject.int(key: String): Int? = primitive(key)?.intOrNull
private fun JsonObject.long(key: String): Long? = primitive(key)?.longOrNull
private fun JsonObject.double(key: String): Double? = primitive(key)?.doubleOrNull
