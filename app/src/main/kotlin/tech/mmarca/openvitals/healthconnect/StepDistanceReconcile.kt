package tech.mmarca.openvitals.healthconnect

import java.time.LocalDate
import kotlin.math.abs

internal const val StepDistanceClientRecordIdPrefix = "openvitals_stepdistance_"

/** A step-derived DistanceRecord OpenVitals wrote earlier. */
internal data class OwnStepDistanceRecord(
    val uid: String,
    val day: LocalDate,
    val meters: Double,
)

internal data class StepDistanceUpsert(
    val day: LocalDate,
    val meters: Double,
)

internal data class StepDistanceReconcileActions(
    val toUpsert: List<StepDistanceUpsert>,
    val toDeleteUids: List<String>,
) {
    val isEmpty: Boolean get() = toUpsert.isEmpty() && toDeleteUids.isEmpty()
}

/**
 * Per-day decision table for the step-derived distance backfill.
 *
 * A day qualifies for a derived record only when its steps are known
 * ([days] is the window the caller actually read), it has steps, and no
 * OTHER source wrote any distance for it. Real distance from anywhere —
 * another app, a GPS workout logged in OpenVitals, a record synced from a
 * paired phone — always wins and evicts the derived record, so the feature
 * can never double-count. Days outside [days] are untouched: without the
 * history read permission their steps are unknowable, and deleting there
 * would destroy records the next run could not rebuild.
 */
internal fun stepDistanceReconcileActions(
    days: Collection<LocalDate>,
    stepsByDay: Map<LocalDate, Long>,
    foreignDistanceDays: Set<LocalDate>,
    ownByDay: Map<LocalDate, OwnStepDistanceRecord>,
    strideMeters: Double,
): StepDistanceReconcileActions {
    val toUpsert = mutableListOf<StepDistanceUpsert>()
    val toDeleteUids = mutableListOf<String>()

    for (day in days.toSortedSet()) {
        val own = ownByDay[day]
        val steps = stepsByDay[day] ?: 0L

        when {
            day in foreignDistanceDays -> {
                if (own != null) toDeleteUids.add(own.uid)
            }
            steps > 0L -> {
                val desired = steps * strideMeters
                if (own == null || abs(own.meters - desired) >= StepDistanceUpdateEpsilonMeters) {
                    toUpsert.add(StepDistanceUpsert(day, desired))
                }
            }
            else -> {
                if (own != null) toDeleteUids.add(own.uid)
            }
        }
    }

    return StepDistanceReconcileActions(toUpsert = toUpsert, toDeleteUids = toDeleteUids)
}

private const val StepDistanceUpdateEpsilonMeters = 0.5
