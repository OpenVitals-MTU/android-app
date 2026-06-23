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
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.model.ActivityExerciseSegmentWrite
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.data.repository.ActivityRepository

internal const val MilesToMeters = 1609.344
internal const val FeetToMeters = 0.3048
internal const val MaxActivityDurationMinutes = 7 * 24 * 60L
internal const val MinRecordedRoutePoints = 2
internal const val DefaultCalorieEstimateWeightKg = 70.0
internal const val RestingMet = 1.0
internal const val RunningKcalPerKgKm = 1.0
internal const val WalkingKcalPerKgKm = 0.55
internal const val MaxActivityRepetitions = 100_000
internal const val MaxActivityRepetitionSets = 99
internal const val MaxActivityRestSeconds = 24 * 60 * 60L
internal const val MaxActivityStepCount = 1_000_000L

internal fun buildWriteRequest(
    state: ActivityEntryUiState,
    unitSystem: UnitSystem,
): ActivityWriteRequest? {
    if (validateActivityEntry(state, unitSystem).isNotEmpty()) return null

    val startDate = state.startDateText.trim().let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return null
    val startTime = state.startTimeText.trim().let { runCatching { LocalTime.parse(it, TimeFormatter) }.getOrNull() }
        ?: return null
    val durationMinutes = state.durationMinutesText.trim().toLongOrNull()
        ?.takeIf { it in 1..MaxActivityDurationMinutes }
        ?: return null
    val zone = ZoneId.systemDefault()
    val start = LocalDateTime.of(startDate, startTime).atZone(zone).toInstant()
    var end = start.plus(Duration.ofMinutes(durationMinutes))
    val importedRoute = state.importedRoute
    var routePoints = importedRoute?.points.orEmpty()
    if (routePoints.isNotEmpty()) {
        if (!state.selectedActivityType.supportsGpsRoute) return null
        if (importedRoute?.hasRecordedTimestamps == false) {
            routePoints = routePoints.withActivityTimeRange(start, end)
        } else {
            val firstPoint = routePoints.first()
            val lastPoint = routePoints.last()
            if (firstPoint.time.isBefore(start)) return null
            if (!lastPoint.time.isBefore(end)) {
                end = lastPoint.time.plusSeconds(1)
            }
        }
    }

    val distanceMeters = when {
        state.distanceText.isNotBlank() && importedRoute != null &&
            state.distanceText.trim() == routeDistanceInputText(importedRoute, unitSystem) -> {
            importedRoute.distanceMeters.takeIf { it > 0.0 }
        }
        state.distanceText.isNotBlank() -> parseDistanceMeters(state.distanceText, unitSystem) ?: return null
        routePoints.isNotEmpty() -> state.importedRoute?.distanceMeters?.takeIf { it > 0.0 }
        else -> null
    }
    val elevationMeters = when {
        state.elevationText.isNotBlank() && importedRoute != null &&
            state.elevationText.trim() == routeElevationInputText(importedRoute, unitSystem) -> {
            importedRoute.elevationGainedMeters.takeIf { it > 0.0 }
        }
        state.elevationText.isNotBlank() -> parseElevationMeters(state.elevationText, unitSystem) ?: return null
        routePoints.isNotEmpty() -> state.importedRoute?.elevationGainedMeters?.takeIf { it > 0.0 }
        else -> null
    }
    val activeCalories = if (state.activeCaloriesText.isBlank()) {
        null
    } else {
        state.activeCaloriesText.toPositiveDoubleOrNull() ?: return null
    }
    val totalCalories = if (state.totalCaloriesText.isBlank()) {
        null
    } else {
        state.totalCaloriesText.toPositiveDoubleOrNull() ?: return null
    }
    if (activeCalories != null && totalCalories != null && totalCalories < activeCalories) return null
    val exerciseSegments = buildActivityExerciseSegments(state, start, end) ?: return null
    val stepsCount = if (state.selectedActivityType.repetitionUnit == ActivityRepetitionUnit.STEPS) {
        state.repetitionTotalText.toPositiveLongOrNull(max = MaxActivityStepCount) ?: return null
    } else {
        null
    }
    val pauseIntervals = if (exerciseSegments.isEmpty() && state.selectedActivityType.supportsGpsRoute) {
        state.recordedPauseIntervals.insideActivityRange(start, end)
    } else {
        emptyList()
    }
    val laps = if (exerciseSegments.isEmpty() && state.selectedActivityType.supportsGpsRoute) {
        state.recordedLaps.insideLapActivityRange(start, end)
    } else {
        emptyList()
    }

    return ActivityWriteRequest(
        exerciseType = state.selectedActivityType.exerciseType,
        startTime = start,
        endTime = end,
        title = state.titleText.trim().takeIf { it.isNotBlank() } ?: state.selectedActivityType.defaultTitle,
        notes = state.notesText.trim().takeIf { it.isNotBlank() },
        plannedExerciseSessionId = state.selectedPlannedWorkoutId,
        routePoints = routePoints,
        pauseIntervals = pauseIntervals,
        laps = laps,
        exerciseSegments = exerciseSegments,
        stepsCount = stepsCount,
        distanceMeters = distanceMeters,
        elevationGainedMeters = elevationMeters,
        activeCaloriesKcal = activeCalories,
        totalCaloriesKcal = totalCalories,
    )
}

internal fun validateActivityEntry(
    state: ActivityEntryUiState,
    unitSystem: UnitSystem,
): Set<ActivityEntryValidationError> {
    val errors = mutableSetOf<ActivityEntryValidationError>()
    val startDate = state.startDateText.trim()
        .let { runCatching { LocalDate.parse(it) }.getOrNull() }
    val startTime = state.startTimeText.trim()
        .let { runCatching { LocalTime.parse(it, TimeFormatter) }.getOrNull() }
    val durationMinutes = state.durationMinutesText.trim().toLongOrNull()
        ?.takeIf { it in 1..MaxActivityDurationMinutes }

    if (startDate == null) errors += ActivityEntryValidationError.START_DATE_INVALID
    if (startTime == null) errors += ActivityEntryValidationError.START_TIME_INVALID
    if (durationMinutes == null) errors += ActivityEntryValidationError.DURATION_INVALID

    val importedRoute = state.importedRoute
    val routePoints = importedRoute?.points.orEmpty()
    if (routePoints.isNotEmpty() && !state.selectedActivityType.supportsGpsRoute) {
        errors += ActivityEntryValidationError.ACTIVITY_TYPE_DOES_NOT_SUPPORT_ROUTE
    }
    if (
        routePoints.isNotEmpty() &&
        importedRoute?.hasRecordedTimestamps != false &&
        startDate != null &&
        startTime != null
    ) {
        val start = LocalDateTime.of(startDate, startTime).atZone(ZoneId.systemDefault()).toInstant()
        if (routePoints.first().time.isBefore(start)) {
            errors += ActivityEntryValidationError.START_TIME_AFTER_ROUTE_START
        }
    }

    if (state.distanceText.isNotBlank()) {
        when {
            !state.selectedActivityType.supportsDistance -> {
                errors += ActivityEntryValidationError.DISTANCE_UNSUPPORTED
            }
            importedRoute != null &&
                state.distanceText.trim() == routeDistanceInputText(importedRoute, unitSystem) -> Unit
            parseDistanceMeters(state.distanceText, unitSystem) == null -> {
                errors += ActivityEntryValidationError.DISTANCE_INVALID
            }
        }
    }

    if (state.elevationText.isNotBlank()) {
        when {
            !state.selectedActivityType.supportsElevation -> {
                errors += ActivityEntryValidationError.ELEVATION_UNSUPPORTED
            }
            importedRoute != null &&
                state.elevationText.trim() == routeElevationInputText(importedRoute, unitSystem) -> Unit
            parseElevationMeters(state.elevationText, unitSystem) == null -> {
                errors += ActivityEntryValidationError.ELEVATION_INVALID
            }
        }
    }

    val activeCalories = if (state.activeCaloriesText.isBlank()) {
        null
    } else {
        state.activeCaloriesText.toPositiveDoubleOrNull()
            ?: run {
                errors += ActivityEntryValidationError.ACTIVE_CALORIES_INVALID
                null
            }
    }
    val totalCalories = if (state.totalCaloriesText.isBlank()) {
        null
    } else {
        state.totalCaloriesText.toPositiveDoubleOrNull()
            ?: run {
                errors += ActivityEntryValidationError.TOTAL_CALORIES_INVALID
                null
            }
    }
    if (activeCalories != null && totalCalories != null && totalCalories < activeCalories) {
        errors += ActivityEntryValidationError.TOTAL_CALORIES_BELOW_ACTIVE
    }
    if (startDate != null && startTime != null && durationMinutes != null) {
        val start = LocalDateTime.of(startDate, startTime).atZone(ZoneId.systemDefault()).toInstant()
        if (!state.hasValidRepetitionInput(start, start.plus(Duration.ofMinutes(durationMinutes)))) {
            errors += ActivityEntryValidationError.REPETITIONS_INVALID
        }
    }

    return errors
}

internal fun buildActivityExerciseSegments(
    state: ActivityEntryUiState,
    start: Instant,
    end: Instant,
): List<ActivityExerciseSegmentWrite>? {
    val type = state.selectedActivityType
    if (type.repetitionUnit != ActivityRepetitionUnit.REPETITIONS) return emptyList()
    val segmentType = type.segmentType ?: return null
    return when (state.repetitionMode) {
        ActivityRepetitionEntryMode.TOTAL -> {
            val repetitions = state.repetitionTotalText.toPositiveIntOrNull(MaxActivityRepetitions) ?: return null
            listOf(
                ActivityExerciseSegmentWrite(
                    startTime = start,
                    endTime = end,
                    segmentType = segmentType,
                    repetitions = repetitions,
                    setIndex = 0,
                )
            )
        }
        ActivityRepetitionEntryMode.SETS -> buildSetExerciseSegments(state, start, end, segmentType)
    }
}

internal fun buildSetExerciseSegments(
    state: ActivityEntryUiState,
    start: Instant,
    end: Instant,
    segmentType: Int,
): List<ActivityExerciseSegmentWrite>? {
    val sets = state.repetitionSets
        .takeIf { it.isNotEmpty() && it.size <= MaxActivityRepetitionSets }
        ?.map { input ->
            ParsedRepetitionSet(
                repetitions = input.repetitionsText.toPositiveIntOrNull(MaxActivityRepetitions) ?: return null,
                restSeconds = input.restMinutesText.toOptionalNonNegativeLongOrNull(MaxActivityRestSeconds) ?: return null,
            )
        }
        ?: return null
    val durationSeconds = Duration.between(start, end).seconds.coerceAtLeast(1L)
    val restSeconds = sets.sumOf { it.restSeconds }
    val activeSeconds = durationSeconds - restSeconds
    if (activeSeconds < sets.size) return null

    var cursor = start
    var activeRemainder = activeSeconds % sets.size
    val baseActiveSeconds = activeSeconds / sets.size
    return buildList {
        sets.forEachIndexed { index, set ->
            val thisActiveSeconds = baseActiveSeconds + if (activeRemainder > 0L) 1L else 0L
            if (activeRemainder > 0L) activeRemainder -= 1L
            val activeEnd = cursor.plusSeconds(thisActiveSeconds)
            add(
                ActivityExerciseSegmentWrite(
                    startTime = cursor,
                    endTime = activeEnd,
                    segmentType = segmentType,
                    repetitions = set.repetitions,
                    setIndex = index,
                )
            )
            val restSeconds = set.restSeconds
            if (restSeconds > 0L) {
                val restEnd = activeEnd.plusSeconds(restSeconds)
                add(
                    ActivityExerciseSegmentWrite(
                        startTime = activeEnd,
                        endTime = restEnd,
                        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
                    )
                )
                cursor = restEnd
            } else {
                cursor = activeEnd
            }
        }
    }
}

internal fun ActivityEntryUiState.hasValidRepetitionInput(start: Instant, end: Instant): Boolean =
    when (selectedActivityType.repetitionUnit) {
        null -> true
        ActivityRepetitionUnit.STEPS -> repetitionTotalText.toPositiveLongOrNull(MaxActivityStepCount) != null
        ActivityRepetitionUnit.REPETITIONS -> buildActivityExerciseSegments(this, start, end) != null
    }

internal data class ParsedRepetitionSet(
    val repetitions: Int,
    val restSeconds: Long,
)

internal fun initialActivityEntryState(
    clock: Clock,
    repository: ActivityRepository,
    selectedActivityType: ActivityEntryType = DefaultActivityEntryTypes.first(),
): ActivityEntryUiState {
    val now = LocalDateTime.now(clock).withSecond(0).withNano(0)
    return ActivityEntryUiState(
        selectedActivityType = selectedActivityType,
        startDateText = DateTimeFormatter.ISO_LOCAL_DATE.format(now),
        startTimeText = TimeFormatter.format(now.toLocalTime()),
        writePermissions = repository.activityWritePermissions(),
    )
}

internal fun clearedAfterSaveState(
    clock: Clock,
    repository: ActivityRepository,
    selectedType: ActivityEntryType,
): ActivityEntryUiState =
    initialActivityEntryState(clock, repository, selectedType)
