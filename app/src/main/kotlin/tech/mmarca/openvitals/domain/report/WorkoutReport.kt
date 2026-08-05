package tech.mmarca.openvitals.domain.report

import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ReportWorkoutSession
import tech.mmarca.openvitals.domain.model.ReportWorkoutTypeTotal
import tech.mmarca.openvitals.domain.model.ReportWorkoutsDetail

/**
 * The workout section's arithmetic: the session list (what was actually done)
 * and totals per activity type, biggest time investment first. Null when the
 * range holds no workouts.
 */
fun workoutsDetail(workouts: List<ExerciseData>): ReportWorkoutsDetail? {
    if (workouts.isEmpty()) return null

    val sessions = workouts
        .sortedBy { it.startTime }
        .map { workout ->
            ReportWorkoutSession(
                start = workout.startTime,
                exerciseType = workout.exerciseType,
                title = workout.title?.takeIf { it.isNotBlank() },
                durationMs = workout.durationMs,
                distanceMeters = workout.totalDistanceMeters?.takeIf { it > 0 },
            )
        }

    val byType = sessions
        .groupBy { it.exerciseType }
        .map { (type, typeSessions) ->
            ReportWorkoutTypeTotal(
                exerciseType = type,
                sessions = typeSessions.size,
                totalDurationMs = typeSessions.sumOf { it.durationMs },
                totalDistanceMeters = typeSessions.mapNotNull { it.distanceMeters }
                    .takeIf { it.isNotEmpty() }
                    ?.sum(),
            )
        }
        .sortedByDescending { it.totalDurationMs }

    return ReportWorkoutsDetail(sessions = sessions, byType = byType)
}
