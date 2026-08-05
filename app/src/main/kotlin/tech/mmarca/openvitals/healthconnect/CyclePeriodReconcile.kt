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
 * Diffs the period spans implied by the current flow days against the period
 * records OpenVitals wrote earlier, so the derived records track the source
 * data instead of accumulating.
 *
 * A desired span that overlaps a foreign-origin period record is dropped: the
 * other app is treated as authoritative for that stretch, which keeps a single
 * period from being represented twice (and double-counted by every consumer
 * that sums period days).
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
