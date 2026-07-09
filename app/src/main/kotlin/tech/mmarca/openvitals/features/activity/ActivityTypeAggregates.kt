package tech.mmarca.openvitals.features.activity

import androidx.compose.runtime.Immutable
import tech.mmarca.openvitals.domain.model.ExerciseData

@Immutable
data class ActivityTypeAggregate(
    val exerciseType: Int,
    val count: Int,
    val totalDistanceMeters: Double,
    val totalDurationMs: Long,
    val totalMovingDurationMs: Long,
    val averageMovingSpeedMetersPerSecond: Double?,
    val bestSpeedMetersPerSecond: Double?,
)

internal fun List<ExerciseData>.activityTypeAggregates(): List<ActivityTypeAggregate> =
    groupBy { it.exerciseType }
        .mapNotNull { (exerciseType, workouts) ->
            val count = workouts.size
            if (count == 0) return@mapNotNull null
            val totalDistanceMeters = workouts.sumOf { it.totalDistanceMeters?.takeIf { meters -> meters > 0.0 } ?: 0.0 }
            val totalDurationMs = workouts.sumOf { it.durationMs.coerceAtLeast(0L) }
            val totalMovingDurationMs = workouts.sumOf { it.movingDurationMs().coerceAtLeast(0L) }
            val averageMovingSpeedMetersPerSecond = totalDistanceMeters
                .takeIf { it > 0.0 }
                ?.let { distance ->
                    totalMovingDurationMs
                        .takeIf { it > 0L }
                        ?.let { durationMs -> distance / (durationMs / 1_000.0) }
                }
                ?.takeIf { it.isFinite() && it > 0.0 }
            val bestSpeedMetersPerSecond = workouts
                .mapNotNull { workout ->
                    listOfNotNull(
                        workout.averageSpeedMetersPerSecond?.takeIf { it.isFinite() && it > 0.0 },
                        workout.averageMovingSpeedMetersPerSecond(),
                    ).maxOrNull()
                }
                .maxOrNull()

            ActivityTypeAggregate(
                exerciseType = exerciseType,
                count = count,
                totalDistanceMeters = totalDistanceMeters,
                totalDurationMs = totalDurationMs,
                totalMovingDurationMs = totalMovingDurationMs,
                averageMovingSpeedMetersPerSecond = averageMovingSpeedMetersPerSecond,
                bestSpeedMetersPerSecond = bestSpeedMetersPerSecond,
            )
        }
        .sortedWith(
            compareByDescending<ActivityTypeAggregate> { it.totalDurationMs }
                .thenBy { exerciseTypeLabel(it.exerciseType) },
        )

private fun ExerciseData.averageMovingSpeedMetersPerSecond(): Double? {
    val distanceMeters = totalDistanceMeters?.takeIf { it > 0.0 } ?: return null
    val durationSeconds = movingDurationMs()
        .takeIf { it > 0L }
        ?.let { it / 1_000.0 }
        ?: return null
    return (distanceMeters / durationSeconds).takeIf { it.isFinite() && it > 0.0 }
}
