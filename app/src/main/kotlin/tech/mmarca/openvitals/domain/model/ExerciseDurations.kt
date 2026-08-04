package tech.mmarca.openvitals.domain.model

import androidx.health.connect.client.records.ExerciseSegment

/**
 * Paused/moving time of a session, from its pause segments.
 *
 * Lives in the DOMAIN, not with the activity screen's formatting helpers,
 * because the splits engine divides by moving time too — and domain code
 * importing a `features/` file is the dependency arrow pointing backwards.
 */

internal fun ExerciseData.pausedDurationMs(): Long =
    segments
        .filter { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE }
        .sumOf { it.durationMs.coerceAtLeast(0L) }
        .coerceAtMost(durationMs.coerceAtLeast(0L))

internal fun ExerciseData.movingDurationMs(): Long =
    (durationMs.coerceAtLeast(0L) - pausedDurationMs()).coerceAtLeast(0L)
