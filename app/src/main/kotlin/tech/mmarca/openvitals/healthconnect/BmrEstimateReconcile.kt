package tech.mmarca.openvitals.healthconnect

import java.time.LocalDate
import kotlin.math.abs

internal const val BmrEstimateClientRecordIdPrefix = "openvitals_bmr_"

/** An estimated BasalMetabolicRateRecord OpenVitals wrote earlier. */
internal data class OwnBmrRecord(
    val uid: String,
    val day: LocalDate,
    val kcalPerDay: Double,
)

internal data class BmrUpsert(
    val day: LocalDate,
    val kcalPerDay: Double,
)

internal data class BmrReconcileActions(
    val toUpsert: List<BmrUpsert>,
    val toDeleteUids: List<String>,
) {
    val isEmpty: Boolean get() = toUpsert.isEmpty() && toDeleteUids.isEmpty()
}

/**
 * Per-day decision table for the basal metabolic rate estimate. A day
 * qualifies when an estimate exists and no other source wrote a rate; a
 * real rate always evicts the estimate. Days outside [days] are untouched.
 */
internal fun bmrReconcileActions(
    days: Collection<LocalDate>,
    estimateByDay: Map<LocalDate, Double>,
    foreignBmrDays: Set<LocalDate>,
    ownByDay: Map<LocalDate, OwnBmrRecord>,
): BmrReconcileActions {
    val toUpsert = mutableListOf<BmrUpsert>()
    val toDeleteUids = mutableListOf<String>()

    for (day in days.toSortedSet()) {
        val own = ownByDay[day]
        val desired = estimateByDay[day]

        when {
            day in foreignBmrDays -> {
                if (own != null) toDeleteUids.add(own.uid)
            }
            desired != null -> {
                if (own == null || abs(own.kcalPerDay - desired) >= BmrUpdateEpsilonKcal) {
                    toUpsert.add(BmrUpsert(day, desired))
                }
            }
            else -> {
                if (own != null) toDeleteUids.add(own.uid)
            }
        }
    }

    return BmrReconcileActions(toUpsert = toUpsert, toDeleteUids = toDeleteUids)
}

private const val BmrUpdateEpsilonKcal = 1.0
