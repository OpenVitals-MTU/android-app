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
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository

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

    val sessionRange = activityEntrySessionRange(state) ?: return null
    val start = sessionRange.first
    var end = sessionRange.second
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
    // The session has to CONTAIN the samples it carries.
    //
    // A recording reaches this form as text, at minute granularity: the start
    // loses its seconds, and the duration is rounded up to a whole minute.
    // Start 10:00:59 for 120 seconds therefore rebuilds as 10:00:00 -> 10:02:00,
    // while the last sample was taken at 10:02:59. Health Connect does not drop
    // the samples past the end — it CLAMPS them into the session, stacking that
    // final minute of readings onto one instant. For a bike ride that is an
    // odd-looking tail. For a heart-rate recovery, which is measured from
    // exactly those samples, it is the whole measurement.
    //
    // So the end is stretched to cover the last sample, the same way the route
    // branch above stretches it to cover the last GPS point.
    //
    // A live recording fills `recordedBleSamples` from the paired sensors. An
    // IMPORT fills the same series from the file — heart rate, cadence, speed
    // parsed out of a TCX or FIT — and hands it over on the imported route.
    var bleSamples = if (state.recordedBleSamples.isEmpty()) {
        state.importedRoute?.bleSamples ?: BleRecordingSampleBuffer()
    } else {
        state.recordedBleSamples
    }
    val lastSampleTime = bleSamples.lastSampleTime()
    if (lastSampleTime != null && !lastSampleTime.isBefore(end)) {
        end = lastSampleTime.plusSeconds(1)
    }
    // The start cannot be stretched the same way: it is the user's, and they
    // may have moved it forward on purpose. But samples before it would be
    // clamped ONTO it, which invents a reading that was never taken. Rather
    // than write that, the samples are dropped.
    val firstSampleTime = bleSamples.firstSampleTime()
    if (firstSampleTime != null && firstSampleTime.isBefore(start)) {
        bleSamples = BleRecordingSampleBuffer()
    }

    val supportsDistance = state.selectedActivityType.supportsDistance
    val supportsElevation = state.selectedActivityType.supportsElevation

    val distanceMeters = when {
        !supportsDistance -> null
        state.distanceText.isNotBlank() && importedRoute != null &&
            state.distanceText.trim() == routeDistanceInputText(importedRoute, unitSystem) -> {
            importedRoute.distanceMeters.takeIf { it > 0.0 }
        }
        state.distanceText.isNotBlank() -> parseDistanceMeters(state.distanceText, unitSystem) ?: return null
        routePoints.isNotEmpty() -> state.importedRoute?.distanceMeters?.takeIf { it > 0.0 }
        else -> null
    }
    val elevationMeters = when {
        !supportsElevation -> null
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
    val stepsCount = if (state.selectedActivityType.supportsStepCounting) {
        if (state.repetitionTotalText.isBlank()) {
            null
        } else {
            state.repetitionTotalText.toPositiveLongOrNull(max = MaxActivityStepCount) ?: return null
        }
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
        notes = state.activitySaveNotes(),
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
        bleSamples = bleSamples,
    )
}

/**
 * Marks where the effort stopped, for a guided heart-rate-recovery test. Null
 * when this was not one.
 *
 * A REST segment running from the moment of cessation to the END OF THE
 * SESSION — not for a fixed five minutes. If it were fixed, a rider who took
 * ninety seconds to press save would leave it no longer trailing, and the
 * reader would quietly fall back to the session end as the moment effort
 * stopped: a later moment, a lower heart rate to measure from, and a
 * flattering recovery that never happened. Running it to the end is identical
 * when they save on time, and correct when they do not.
 *
 * Only REST and PAUSE segments are emitted, never an "active" one. Health
 * Connect validates a segment's type against the session's exercise type —
 * a mismatch throws and takes the whole save with it — and rest and pause are
 * the two that are universal. Nothing needs an active segment: the reader only
 * looks for the trailing rest, and Health Connect does not ask segments to
 * cover the session.
 */
internal fun buildRecoveryExerciseSegments(
    state: ActivityEntryUiState,
    start: Instant,
    end: Instant,
): List<ActivityExerciseSegmentWrite>? {
    val recoveryStart = state.recordedRecoveryStartTime ?: return null
    if (!recoveryStart.isAfter(start) || !recoveryStart.isBefore(end)) return null

    // Explicit segments SUPPRESS the ones the native writer would otherwise
    // synthesize from the pause intervals, so the pauses have to be carried
    // here by hand or they are lost.
    val pauseSegments = state.recordedPauseIntervals
        .insideActivityRange(start, end)
        .filter { it.startTime.isBefore(recoveryStart) }
        .map { pause ->
            ActivityExerciseSegmentWrite(
                startTime = pause.startTime,
                endTime = if (pause.endTime.isAfter(recoveryStart)) recoveryStart else pause.endTime,
                segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE,
                repetitions = 0,
            )
        }
    return pauseSegments + ActivityExerciseSegmentWrite(
        startTime = recoveryStart,
        endTime = end,
        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
        repetitions = 0,
    )
}

internal fun ActivityEntryUiState.activitySaveNotes(): String? {
    val feelingText = selectedFeeling?.noteText
    val noteText = notesText.trim().takeIf { it.isNotBlank() }
    return listOfNotNull(feelingText, noteText)
        .joinToString(separator = "\n\n")
        .takeIf { it.isNotBlank() }
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

    if (state.distanceText.isNotBlank() && state.selectedActivityType.supportsDistance) {
        when {
            importedRoute != null &&
                state.distanceText.trim() == routeDistanceInputText(importedRoute, unitSystem) -> Unit
            parseDistanceMeters(state.distanceText, unitSystem) == null -> {
                errors += ActivityEntryValidationError.DISTANCE_INVALID
            }
        }
    }

    if (state.elevationText.isNotBlank() && state.selectedActivityType.supportsElevation) {
        when {
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
    // A heart-rate-recovery test wins over everything else: the recovery mark
    // is the only record of where the effort stopped, and without it the
    // session read back later is just a workout that happens to end in some
    // sitting down.
    val recovery = buildRecoveryExerciseSegments(state, start, end)
    if (recovery != null) return recovery

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
        ActivityRepetitionUnit.STEPS -> repetitionTotalText.isBlank() ||
            repetitionTotalText.toPositiveLongOrNull(MaxActivityStepCount) != null
        ActivityRepetitionUnit.REPETITIONS -> buildActivityExerciseSegments(this, start, end) != null
    }

internal data class ParsedRepetitionSet(
    val repetitions: Int,
    val restSeconds: Long,
)

internal fun activityEntrySessionRange(state: ActivityEntryUiState): Pair<Instant, Instant>? {
    val startDate = state.startDateText.trim().let { runCatching { LocalDate.parse(it) }.getOrNull() }
        ?: return null
    val startTime = state.startTimeText.trim().let { runCatching { LocalTime.parse(it, TimeFormatter) }.getOrNull() }
        ?: return null
    val durationMinutes = state.durationMinutesText.trim().toLongOrNull()
        ?.takeIf { it in 1..MaxActivityDurationMinutes }
        ?: return null
    val zone = ZoneId.systemDefault()
    val start = LocalDateTime.of(startDate, startTime).atZone(zone).toInstant()
    val end = start.plus(Duration.ofMinutes(durationMinutes))
    return start to end
}

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
