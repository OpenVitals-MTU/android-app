package tech.mmarca.openvitals.healthconnect

import java.time.LocalDate

/** An OpenVitals-authored MenstruationPeriodRecord already in Health Connect. */
internal data class ExistingPeriod(
    val uid: String,
    val span: ClosedRange<LocalDate>,
)

internal data class PeriodReconcileActions(
    val toInsert: List<ClosedRange<LocalDate>>,
    val toUpdate: List<Pair<String, ClosedRange<LocalDate>>>,
    val toDeleteUids: List<String>,
) {
    val isEmpty: Boolean get() = toInsert.isEmpty() && toUpdate.isEmpty() && toDeleteUids.isEmpty()
}

/**
 * Diffs the period spans implied by the flow days against the records we
 * wrote earlier. A span overlapping a foreign period record is dropped.
 */
internal fun periodReconcileActions(
    desiredSpans: List<ClosedRange<LocalDate>>,
    ownExisting: List<ExistingPeriod>,
    foreignSpans: List<ClosedRange<LocalDate>>,
): PeriodReconcileActions {
    val wanted = desiredSpans.filterNot { span -> foreignSpans.any { it.overlaps(span) } }

    val unmatched = ownExisting.toMutableList()
    val toInsert = mutableListOf<ClosedRange<LocalDate>>()
    val toUpdate = mutableListOf<Pair<String, ClosedRange<LocalDate>>>()

    for (span in wanted) {
        val match = unmatched.firstOrNull { it.span.start == span.start }
            ?: unmatched.firstOrNull { it.span.overlaps(span) }
        if (match == null) {
            toInsert.add(span)
        } else {
            unmatched.remove(match)
            if (match.span != span) {
                toUpdate.add(match.uid to span)
            }
        }
    }

    return PeriodReconcileActions(
        toInsert = toInsert,
        toUpdate = toUpdate,
        toDeleteUids = unmatched.map { it.uid },
    )
}

private fun ClosedRange<LocalDate>.overlaps(other: ClosedRange<LocalDate>): Boolean =
    !start.isAfter(other.endInclusive) && !other.start.isAfter(endInclusive)
