package tech.mmarca.openvitals.features.manualentry.activity.recording

import androidx.health.connect.client.records.ExerciseSessionRecord
import java.time.Duration
import java.time.Instant
import tech.mmarca.openvitals.domain.insights.RouteElevation
import tech.mmarca.openvitals.domain.model.ActivityRecordingLap
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences

/**
 * Whether focus mode has a session to focus on: the flag alone is not enough.
 * An idle recorder has nothing to show, and a repetition session has no focus
 * mode in the first place.
 */
internal val ActivityRecordingState.canUseFocusMode: Boolean
    get() = isActive && recordingKind != ActivityRecordingKind.REPETITION

fun ActivityRecordingState.elapsedDuration(now: Instant = Instant.now()): Duration {
    val start = startTime ?: return Duration.ZERO
    val effectiveEnd = endTime
        ?: if (status == ActivityRecordingStatus.PAUSED) pausedStartedAt ?: now else now
    return Duration.ofMillis((effectiveEnd.toEpochMilli() - start.toEpochMilli()).coerceAtLeast(0L))
}

fun ActivityRecordingState.restDuration(now: Instant = Instant.now()): Duration =
    Duration.ofMillis((accumulatedRestMillis + openRestMillis(now)).coerceAtLeast(0L))

fun ActivityRecordingState.movingDuration(now: Instant = Instant.now()): Duration {
    val elapsedMillis = elapsedDuration(now).toMillis()
    val openPauseMillis = pausedStartedAt
        ?.takeIf { status == ActivityRecordingStatus.PAUSED }
        ?.let { Duration.between(it, now).toMillis().coerceAtLeast(0L) }
        ?: 0L
    val pausedMillis = totalPausedMillis + openPauseMillis
    val idleMillis = totalIdleMillis + openIdleMillis(now)
    val restMillis = restDuration(now).toMillis().takeIf {
        recordingKind == ActivityRecordingKind.REPETITION
    } ?: 0L
    return Duration.ofMillis((elapsedMillis - pausedMillis - idleMillis - restMillis).coerceAtLeast(0L))
}

fun ActivityRecordingState.restRemainingDuration(now: Instant = Instant.now()): Duration {
    val restEnd = restEndTime() ?: return Duration.ZERO
    return Duration.ofMillis(Duration.between(now, restEnd).toMillis().coerceAtLeast(0L))
}

/** Whether this recording walks through a plan. */
val ActivityRecordingState.isPlanRun: Boolean
    get() = planSteps.isNotEmpty()

val ActivityRecordingState.currentPlanStep: ActivityPlanRunStep?
    get() = planSteps.getOrNull(planStepIndex)

val ActivityRecordingState.nextPlanStep: ActivityPlanRunStep?
    get() = planSteps.getOrNull(planStepIndex + 1)

val ActivityRecordingState.isPlanComplete: Boolean
    get() = isPlanRun && planStepIndex >= planSteps.size

/** When a timed step ends, or null when the step in progress is not timed. */
internal fun ActivityRecordingState.planStepEndTime(): Instant? {
    val step = currentPlanStep ?: return null
    if (status != ActivityRecordingStatus.RECORDING || step.goalKind != ActivityPlanGoalKind.SECONDS) return null
    return currentSetStartedAt?.plusSeconds(step.goalValue)
}

fun ActivityRecordingState.planStepRemaining(now: Instant = Instant.now()): Duration? {
    val end = planStepEndTime() ?: return null
    return Duration.ofMillis(Duration.between(now, end).toMillis().coerceAtLeast(0L))
}

internal fun ActivityRecordingState.restEndTime(): Instant? =
    restStartedAt
        ?.takeIf { status == ActivityRecordingStatus.RESTING && repetitionRestSeconds > 0L }
        ?.plusSeconds(repetitionRestSeconds)

internal fun ActivityRecordingState.openRestMillis(now: Instant): Long =
    restStartedAt
        ?.takeIf { status == ActivityRecordingStatus.RESTING }
        ?.let { Duration.between(it, now).toMillis().coerceAtLeast(0L) }
        ?: 0L

internal fun ActivityRecordingState.recordedRepetitionSets(end: Instant): List<ActivityRecordedRepetitionSet> {
    val sets = if (status == ActivityRecordingStatus.RESTING) {
        repetitionSets.withLastRestSeconds((openRestMillis(end) / 1_000L).coerceAtLeast(0L))
    } else {
        repetitionSets
    }
    if (status != ActivityRecordingStatus.RECORDING) return sets
    val step = currentPlanStep
    val openTimedStep = step?.goalKind == ActivityPlanGoalKind.SECONDS
    if (currentSetRepetitionCount <= 0L && !openTimedStep) return sets
    val activeMillis = Duration.between(currentSetStartedAt ?: startTime ?: end, end)
        .toMillis()
        .coerceAtLeast(1L)
    return sets + ActivityRecordedRepetitionSet(
        repetitions = if (openTimedStep) 0L else currentSetRepetitionCount,
        restSeconds = 0L,
        activeMillis = activeMillis,
        segmentType = step?.segmentType,
        label = step?.label,
        isDuration = openTimedStep,
    )
}

internal fun List<ActivityRecordedRepetitionSet>.withLastRestSeconds(restSeconds: Long): List<ActivityRecordedRepetitionSet> =
    if (isEmpty()) {
        this
    } else {
        dropLast(1) + last().copy(restSeconds = restSeconds.coerceAtLeast(0L))
    }

/**
 * Elevation gain to show while recording — and the same figure that is saved,
 * so the dashboard cannot disagree with the review screen.
 *
 * [ActivityRecordingState.elevationGainedMeters] is a running sum of raw
 * per-point rises and is NOT used: GPS vertical noise makes it wildly high (a
 * real 750 m climb reported ~15 km). The route is re-derived through the
 * smoothing + hysteresis filter instead. The barometer figure is already
 * filtered and wins when present.
 */
fun ActivityRecordingState.displayElevationGainedMeters(): Double =
    if (hasBarometerElevation) {
        barometerElevationGainedMeters
    } else {
        RouteElevation.routeElevationGain(points)
    }

fun ActivityRecordingState.closedManualLaps(endTime: Instant): List<ActivityRecordingLap> {
    if (manualLaps.isEmpty()) return emptyList()
    val openStart = manualLaps.maxByOrNull { it.endTime }?.endTime ?: startTime ?: return manualLaps
    val finalLap = if (openStart.isBefore(endTime)) {
        ActivityRecordingLap(
            startTime = openStart,
            endTime = endTime,
            distanceMeters = activityRecordingRouteDistanceMeters(
                points = points,
                routeBreakIndexes = routeBreakIndexes,
                startTime = openStart,
                endTime = endTime,
            ).takeIf { it > 0.0 },
        )
    } else {
        null
    }
    return manualLaps + listOfNotNull(finalLap)
}

fun ActivityRecordingState.effectiveCurrentSpeedMetersPerSecond(now: Instant = Instant.now()): Double =
    if (
        status != ActivityRecordingStatus.RECORDING ||
        isAutoIdle(now) ||
        gpsStatus == ActivityGpsStatus.POOR_ACCURACY ||
        gpsStatus == ActivityGpsStatus.LOST ||
        gpsStatus == ActivityGpsStatus.DISABLED
    ) {
        0.0
    } else {
        currentSpeedMetersPerSecond
    }

fun ActivityRecordingState.isAutoIdle(now: Instant = Instant.now()): Boolean =
    status == ActivityRecordingStatus.RECORDING &&
        autoIdleEnabled &&
        lastMovementAt?.plusMillis(autoIdleTimeoutMillis)?.let { !now.isBefore(it) } == true

internal fun ActivityRecordingState.openIdleMillis(now: Instant): Long {
    if (status != ActivityRecordingStatus.RECORDING || !autoIdleEnabled) return 0L
    val movementAt = lastMovementAt ?: return 0L
    val idleStartedAt = movementAt.plusMillis(autoIdleTimeoutMillis)
    return if (now.isAfter(idleStartedAt)) {
        Duration.between(idleStartedAt, now).toMillis().coerceAtLeast(0L)
    } else {
        0L
    }
}

internal fun ActivityRecordingState.minimumSampleDistanceMeters(
    recordingPreferences: ActivityRecordingPreferences,
): Double =
    recordingPreferences.recordingDistanceIntervalMeters?.toDouble() ?: when (exerciseType) {
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING,
        ExerciseSessionRecord.EXERCISE_TYPE_SKIING,
        ExerciseSessionRecord.EXERCISE_TYPE_SNOWBOARDING,
        ExerciseSessionRecord.EXERCISE_TYPE_SAILING -> 10.0
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING,
        ExerciseSessionRecord.EXERCISE_TYPE_PADDLING,
        ExerciseSessionRecord.EXERCISE_TYPE_SKATING,
        ExerciseSessionRecord.EXERCISE_TYPE_SURFING -> 7.0
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER -> 4.0
        else -> 5.0
    }
