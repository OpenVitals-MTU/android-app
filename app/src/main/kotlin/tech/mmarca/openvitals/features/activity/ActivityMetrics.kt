package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSegment
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.ExerciseData

internal fun ExerciseData.pausedDurationMs(): Long =
    segments
        .filter { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE }
        .sumOf { it.durationMs.coerceAtLeast(0L) }
        .coerceAtMost(durationMs.coerceAtLeast(0L))

internal fun ExerciseData.movingDurationMs(): Long =
    (durationMs.coerceAtLeast(0L) - pausedDurationMs()).coerceAtLeast(0L)

internal fun ExerciseData.averageSpeed(unitFormatter: UnitFormatter): DisplayValue? {
    val distanceMeters = totalDistanceMeters?.takeIf { it > 0.0 } ?: return null
    val movingDurationMs = movingDurationMs().takeIf { it > 0L } ?: return null
    return unitFormatter.averageSpeed(distanceMeters, movingDurationMs)
}

internal fun ExerciseData.averagePace(unitFormatter: UnitFormatter): DisplayValue? {
    val distanceMeters = totalDistanceMeters?.takeIf { it > 0.0 } ?: return null
    val movingDurationMs = movingDurationMs().takeIf { it > 0L } ?: return null
    return unitFormatter.averagePace(distanceMeters, movingDurationMs)
}
