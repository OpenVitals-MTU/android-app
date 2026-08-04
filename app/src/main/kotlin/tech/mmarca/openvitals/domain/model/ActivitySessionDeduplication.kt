package tech.mmarca.openvitals.domain.model

import java.time.Instant
import kotlin.math.max
import kotlin.math.min

private const val DuplicateOverlapRatio = 0.85
private const val DuplicateBoundaryToleranceMs = 15 * 60 * 1000L

fun deduplicateExerciseSessions(sessions: List<ExerciseData>): List<ExerciseData> {
    if (sessions.size < 2) return sessions.sortedByDescending { it.endTime }

    val kept = mutableListOf<ExerciseData>()
    sessions
        .sortedWith(compareBy<ExerciseData> { it.startTime }.thenBy { it.endTime })
        .forEach { session ->
            val duplicateIndex = kept.indexOfFirst { existing -> existing.isDuplicateOf(session) }
            if (duplicateIndex == -1) {
                kept += session
            } else {
                kept[duplicateIndex] = richerExerciseSession(kept[duplicateIndex], session)
            }
        }

    return kept.sortedByDescending { it.endTime }
}

private fun ExerciseData.isDuplicateOf(other: ExerciseData): Boolean {
    if (exerciseType != other.exerciseType) return false

    val shorterDuration = min(durationMs.coerceAtLeast(0L), other.durationMs.coerceAtLeast(0L))
    if (shorterDuration <= 0L) return false

    val overlapMs = min(endTime.toEpochMilli(), other.endTime.toEpochMilli()) -
        max(startTime.toEpochMilli(), other.startTime.toEpochMilli())
    if (overlapMs <= 0L) return false

    val startDiff = (startTime.toEpochMilli() - other.startTime.toEpochMilli()).abs()
    val endDiff = (endTime.toEpochMilli() - other.endTime.toEpochMilli()).abs()
    return overlapMs / shorterDuration.toDouble() >= DuplicateOverlapRatio &&
        startDiff <= DuplicateBoundaryToleranceMs &&
        endDiff <= DuplicateBoundaryToleranceMs
}

private fun richerExerciseSession(first: ExerciseData, second: ExerciseData): ExerciseData =
    compareBy<ExerciseData> { it.richnessScore() }
        .thenBy { it.durationMs }
        .thenBy { it.lastModifiedTime ?: Instant.EPOCH }
        .let { comparator -> if (comparator.compare(first, second) >= 0) first else second }

private fun ExerciseData.richnessScore(): Int =
    (if (isOpenVitalsEntry) 1_000 else 0) +
        (if (route.status == ExerciseRouteStatus.DATA) 200 else 0) +
        route.points.size.coerceAtMost(500) +
        listOfNotNull(
            totalDistanceMeters,
            totalCaloriesKcal,
            activeCaloriesKcal,
            steps,
            wheelchairPushes,
            averageSpeedMetersPerSecond,
            averagePowerWatts,
            averageStepsCadenceRate,
            averageCyclingCadenceRpm,
            averageHeartRateBpm,
            floorsClimbed,
            elevationGainedMeters,
        ).size * 20 +
        segments.size.coerceAtMost(20) * 5 +
        laps.size.coerceAtMost(20) * 5 +
        (if (device != null) 10 else 0) +
        (if (!title.isNullOrBlank()) 5 else 0) +
        (if (!notes.isNullOrBlank()) 5 else 0)

private fun Long.abs(): Long = if (this < 0L) -this else this
