package tech.mmarca.openvitals.domain.model

import androidx.health.connect.client.records.ExerciseSegment

/** Paused and moving time from the pause segments. In the domain because the splits engine needs it. */

internal fun ExerciseData.pausedDurationMs(): Long =
    segments
        .filter { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE }
        .sumOf { it.durationMs.coerceAtLeast(0L) }
        .coerceAtMost(durationMs.coerceAtLeast(0L))

internal fun ExerciseData.movingDurationMs(): Long =
    (durationMs.coerceAtLeast(0L) - pausedDurationMs()).coerceAtLeast(0L)
