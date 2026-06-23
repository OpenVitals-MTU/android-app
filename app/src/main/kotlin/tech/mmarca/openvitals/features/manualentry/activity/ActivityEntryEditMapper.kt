package tech.mmarca.openvitals.features.manualentry.activity

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Clock
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseLapData
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.domain.model.ExerciseRouteStatus
import tech.mmarca.openvitals.data.repository.ActivityRepository

internal fun ExerciseData.toEditState(
    unitSystem: UnitSystem,
    clock: Clock,
    repository: ActivityRepository,
    canWrite: Boolean,
    isCheckingPermission: Boolean,
): ActivityEntryUiState {
    val selectedType = inferStoredActivityType()
    val routeImport = route.takeIf { it.status == ExerciseRouteStatus.DATA && it.points.isNotEmpty() }
        ?.let { routeData ->
            RouteFileImport(
                fileName = null,
                points = routeData.points,
                distanceMeters = totalDistanceMeters ?: 0.0,
                elevationGainedMeters = elevationGainedMeters ?: 0.0,
                startTime = startTime,
                endTime = endTime,
                name = title,
                description = notes,
                hasRecordedTimestamps = true,
                hasImportedTimeRange = true,
                originalPointCount = routeData.points.size,
            )
        }
    val start = startTime.atZone(clock.zone)
    val durationMinutes = ceil(
        Duration.between(startTime, endTime).seconds.coerceAtLeast(1).toDouble() / 60.0
    ).toLong().coerceIn(1, MaxActivityDurationMinutes)
    val repetitionEditState = toRepetitionEditState(selectedType)
    return ActivityEntryUiState(
        mode = if (routeImport == null) ActivityEntryMode.MANUAL else ActivityEntryMode.ROUTE_IMPORT,
        selectedActivityType = selectedType,
        titleText = title.orEmpty(),
        notesText = notes.orEmpty(),
        startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(start),
        startTimeText = TimeFormatter.format(start.toLocalTime()),
        durationMinutesText = durationMinutes.toString(),
        distanceText = totalDistanceMeters?.takeIf { it > 0.0 }?.toDistanceInputText(unitSystem).orEmpty(),
        elevationText = elevationGainedMeters?.takeIf { it > 0.0 }?.toElevationInputText(unitSystem).orEmpty(),
        activeCaloriesText = activeCaloriesKcal?.takeIf { it > 0.0 }?.toInputText(maxFractionDigits = 1).orEmpty(),
        totalCaloriesText = totalCaloriesKcal?.takeIf { it > 0.0 }?.toInputText(maxFractionDigits = 1).orEmpty(),
        repetitionMode = repetitionEditState.mode,
        repetitionTotalText = repetitionEditState.totalText,
        repetitionSets = repetitionEditState.sets,
        importedRoute = routeImport,
        recordedPauseIntervals = segments
            .filter { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE }
            .map { ActivityPauseInterval(startTime = it.startTime, endTime = it.endTime) },
        recordedLaps = laps,
        writePermissions = repository.activityWritePermissions(),
        canWrite = canWrite,
        isCheckingPermission = isCheckingPermission,
        editRecordId = id,
    )
}

internal fun ExerciseData.inferStoredActivityType(): ActivityEntryType {
    val titleText = title.orEmpty().lowercase()
    val activeSegments = segments.filterNot {
        it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE ||
            it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST
    }
    return when {
        exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> {
            DefaultActivityEntryTypes.first { it.id == "treadmill" }
        }
        exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS &&
            activeSegments.any { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PULL_UP } -> {
            DefaultActivityEntryTypes.first { it.id == "pull_ups" }
        }
        exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS &&
            activeSegments.any { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_JUMP_ROPE } -> {
            DefaultActivityEntryTypes.first { it.id == "rope_skipping" }
        }
        exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS && titleText.contains("push") -> {
            DefaultActivityEntryTypes.first { it.id == "push_ups" }
        }
        exerciseType == ExerciseSessionRecord.EXERCISE_TYPE_GYMNASTICS && titleText.contains("trampoline") -> {
            DefaultActivityEntryTypes.first { it.id == "trampoline_jumping" }
        }
        else -> DefaultActivityEntryTypes
            .firstOrNull { it.exerciseType == exerciseType && !it.isRepetitionLike }
            ?: DefaultActivityEntryTypes.firstOrNull { it.exerciseType == exerciseType }
            ?: DefaultActivityEntryTypes.first()
    }
}

internal data class RepetitionEditState(
    val mode: ActivityRepetitionEntryMode = ActivityRepetitionEntryMode.TOTAL,
    val totalText: String = "",
    val sets: List<ActivityRepetitionSetInput> = listOf(ActivityRepetitionSetInput()),
)

internal fun ExerciseData.toRepetitionEditState(type: ActivityEntryType): RepetitionEditState {
    if (type.repetitionUnit == ActivityRepetitionUnit.STEPS) {
        return RepetitionEditState(totalText = steps?.takeIf { it > 0L }?.toString().orEmpty())
    }
    if (type.repetitionUnit != ActivityRepetitionUnit.REPETITIONS) return RepetitionEditState()

    val activeSegments = segments
        .filter { it.segmentType == type.segmentType && it.repetitions > 0 }
        .sortedBy { it.startTime }
    if (activeSegments.isEmpty()) return RepetitionEditState()
    if (activeSegments.size == 1) {
        return RepetitionEditState(totalText = activeSegments.first().repetitions.toString())
    }

    val sortedSegments = segments.sortedBy { it.startTime }
    val sets = activeSegments.mapIndexed { index, segment ->
        val restMinutes = sortedSegments
            .firstOrNull {
                it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST &&
                    !it.startTime.isBefore(segment.endTime) &&
                    activeSegments.getOrNull(index + 1)?.let { next -> !it.endTime.isAfter(next.startTime) } == true
            }
            ?.let { rest ->
                ceil(Duration.between(rest.startTime, rest.endTime).seconds / 60.0)
                    .toLong()
                    .takeIf { it > 0L }
                    ?.toString()
            }
            .orEmpty()
        ActivityRepetitionSetInput(
            repetitionsText = segment.repetitions.toString(),
            restMinutesText = restMinutes,
        )
    }
    return RepetitionEditState(
        mode = ActivityRepetitionEntryMode.SETS,
        sets = sets.ifEmpty { listOf(ActivityRepetitionSetInput()) },
    )
}

internal fun Double.toDistanceInputText(unitSystem: UnitSystem): String {
    val value = when (unitSystem) {
        UnitSystem.METRIC -> this / 1000.0
        UnitSystem.IMPERIAL -> this / MilesToMeters
    }
    return value.toInputText(maxFractionDigits = 2)
}

internal fun Double.toElevationInputText(unitSystem: UnitSystem): String {
    val value = when (unitSystem) {
        UnitSystem.METRIC -> this
        UnitSystem.IMPERIAL -> this / FeetToMeters
    }
    return value.toInputText(maxFractionDigits = 1)
}

internal fun inferActivityType(
    routeImport: RouteFileImport,
    currentType: ActivityEntryType,
): ActivityEntryType {
    val sourceText = listOfNotNull(routeImport.type, routeImport.name, routeImport.fileName)
        .joinToString(separator = " ")
        .lowercase()
    val exerciseType = when {
        sourceText.containsAny("snowboard") -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING
        sourceText.containsAny("snowshoe") -> ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING
        sourceText.containsAny("ski") -> ExerciseSessionRecord.EXERCISE_TYPE_SKIING
        sourceText.containsAny("hike", "hiking") -> ExerciseSessionRecord.EXERCISE_TYPE_HIKING
        sourceText.containsAny("run", "running", "jog") -> ExerciseSessionRecord.EXERCISE_TYPE_RUNNING
        sourceText.containsAny("bike", "biking", "bicycle", "cycling", "cycle", "ride") -> {
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING
        }
        sourceText.containsAny("walk", "walking") -> ExerciseSessionRecord.EXERCISE_TYPE_WALKING
        sourceText.containsAny("wheelchair") -> ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR
        sourceText.containsAny("row", "rowing") -> ExerciseSessionRecord.EXERCISE_TYPE_ROWING
        sourceText.containsAny("paddle", "kayak", "canoe") -> ExerciseSessionRecord.EXERCISE_TYPE_PADDLING
        sourceText.containsAny("skate", "skating") -> ExerciseSessionRecord.EXERCISE_TYPE_SKATING
        sourceText.containsAny("sail", "sailing") -> ExerciseSessionRecord.EXERCISE_TYPE_SAILING
        sourceText.containsAny("surf", "surfing") -> ExerciseSessionRecord.EXERCISE_TYPE_SURFING
        sourceText.containsAny("swim", "swimming") -> ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER
        sourceText.containsAny("golf") -> ExerciseSessionRecord.EXERCISE_TYPE_GOLF
        else -> null
    }
    return DefaultActivityEntryTypes
        .firstOrNull { it.exerciseType == exerciseType && it.supportsGpsRoute }
        ?: currentType.takeIf { it.supportsGpsRoute }
        ?: DefaultActivityEntryTypes.first()
}

internal fun String.containsAny(vararg values: String): Boolean =
    values.any(::contains)

internal data class ActivityCalorieEstimate(
    val activeCaloriesText: String,
    val totalCaloriesText: String,
)

internal fun activityCalorieEstimate(
    activityType: ActivityEntryType,
    distanceMeters: Double?,
    durationMinutesText: String,
): ActivityCalorieEstimate? {
    if (!activityType.supportsGpsRoute) return null
    val durationMinutes = durationMinutesText.trim().toLongOrNull()
        ?.takeIf { it in 1..MaxActivityDurationMinutes }
        ?: return null
    val hours = durationMinutes / 60.0
    val met = activityMet(activityType.exerciseType) ?: return null
    val restingCalories = DefaultCalorieEstimateWeightKg * hours * RestingMet
    val activeByMet = (met - RestingMet)
        .coerceAtLeast(0.0) * DefaultCalorieEstimateWeightKg * hours
    val activeByDistance = distanceBasedActiveCalories(
        exerciseType = activityType.exerciseType,
        distanceMeters = distanceMeters,
    ) ?: 0.0
    val activeCalories = maxOf(activeByMet, activeByDistance).takeIf { it > 0.0 } ?: return null

    return ActivityCalorieEstimate(
        activeCaloriesText = activeCalories.toCaloriesInputText(),
        totalCaloriesText = (activeCalories + restingCalories).toCaloriesInputText(),
    )
}

internal fun activityMet(exerciseType: Int): Double? =
    when (exerciseType) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> 9.8
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> 7.5
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> 3.5
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> 6.0
        ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> 4.0
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        ExerciseSessionRecord.EXERCISE_TYPE_PADDLING -> 7.0
        ExerciseSessionRecord.EXERCISE_TYPE_SKIING -> 7.0
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING -> 5.3
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> 8.0
        ExerciseSessionRecord.EXERCISE_TYPE_SKATING -> 7.0
        ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> 3.0
        ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> 3.0
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> 8.0
        ExerciseSessionRecord.EXERCISE_TYPE_GOLF -> 4.8
        else -> null
    }

internal fun distanceBasedActiveCalories(
    exerciseType: Int,
    distanceMeters: Double?,
): Double? {
    val distanceKm = distanceMeters?.takeIf { it > 0.0 }?.div(1000.0) ?: return null
    val kcalPerKgKm = when (exerciseType) {
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWSHOEING -> RunningKcalPerKgKm
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING,
        ExerciseSessionRecord.EXERCISE_TYPE_WHEELCHAIR -> WalkingKcalPerKgKm
        else -> return null
    }
    return DefaultCalorieEstimateWeightKg * distanceKm * kcalPerKgKm
}

internal fun parseDistanceMeters(text: String, unitSystem: UnitSystem): Double? {
    val value = text.toPositiveDoubleOrNull() ?: return null
    return when (unitSystem) {
        UnitSystem.METRIC -> value * 1000.0
        UnitSystem.IMPERIAL -> value * MilesToMeters
    }
}

internal fun parseElevationMeters(text: String, unitSystem: UnitSystem): Double? {
    val value = text.toPositiveDoubleOrNull() ?: return null
    return when (unitSystem) {
        UnitSystem.METRIC -> value
        UnitSystem.IMPERIAL -> value * FeetToMeters
    }
}

internal fun routeDistanceInputText(routeImport: RouteFileImport, unitSystem: UnitSystem): String {
    val distance = routeImport.distanceMeters.takeIf { it > 0.0 } ?: return ""
    val value = when (unitSystem) {
        UnitSystem.METRIC -> distance / 1000.0
        UnitSystem.IMPERIAL -> distance / MilesToMeters
    }
    return value.toInputText(maxFractionDigits = 2)
}

internal fun routeElevationInputText(routeImport: RouteFileImport, unitSystem: UnitSystem): String {
    val elevation = routeImport.elevationGainedMeters.takeIf { it > 0.0 } ?: return ""
    val value = when (unitSystem) {
        UnitSystem.METRIC -> elevation
        UnitSystem.IMPERIAL -> elevation / FeetToMeters
    }
    return value.toInputText(maxFractionDigits = 1)
}

internal fun List<ExerciseRoutePoint>.withActivityTimeRange(
    start: java.time.Instant,
    end: java.time.Instant,
): List<ExerciseRoutePoint> {
    if (isEmpty()) return emptyList()
    val totalMillis = Duration.between(start, end)
        .toMillis()
        .coerceAtLeast(size.toLong())
    val lastOffset = (totalMillis - 1).coerceAtLeast(0L)
    return mapIndexed { index, point ->
        val offset = if (size == 1) {
            0L
        } else {
            lastOffset * index / (size - 1)
        }
        point.copy(time = start.plusMillis(offset))
    }
}

internal fun List<ActivityPauseInterval>.insideActivityRange(
    start: java.time.Instant,
    end: java.time.Instant,
): List<ActivityPauseInterval> =
    sortedBy { it.startTime }
        .filter { interval ->
            !interval.startTime.isBefore(start) &&
                interval.startTime.isBefore(interval.endTime) &&
                !interval.endTime.isAfter(end)
        }

internal fun List<ExerciseLapData>.insideLapActivityRange(
    start: java.time.Instant,
    end: java.time.Instant,
): List<ExerciseLapData> =
    sortedBy { it.startTime }
        .filter { lap ->
            !lap.startTime.isBefore(start) &&
                lap.startTime.isBefore(lap.endTime) &&
                !lap.endTime.isAfter(end)
        }

internal fun Double.toInputText(maxFractionDigits: Int): String =
    "%.${maxFractionDigits}f"
        .format(Locale.US, this)
        .trimEnd('0')
        .trimEnd('.')

internal fun Double.toCaloriesInputText(): String =
    roundToInt()
        .coerceAtLeast(1)
        .toString()

internal fun String.toPositiveDoubleOrNull(): Double? =
    trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it > 0.0 }

internal fun String.toPositiveIntOrNull(max: Int): Int? =
    trim()
        .toIntOrNull()
        ?.takeIf { it in 1..max }

internal fun String.toPositiveLongOrNull(max: Long): Long? =
    trim()
        .toLongOrNull()
        ?.takeIf { it in 1..max }

internal fun String.toOptionalNonNegativeLongOrNull(max: Long): Long? {
    val trimmed = trim()
    if (trimmed.isBlank()) return 0L
    return trimmed.toLongOrNull()?.takeIf { it in 0..max }
}

internal val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm")
