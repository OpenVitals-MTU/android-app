package tech.mmarca.openvitals.healthconnect

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The reconcile diff decides which derived MenstruationPeriodRecords are inserted, rewritten or removed. Each branch is pinned. */
class CyclePeriodReconcileTest {

    private fun d(n: Int): LocalDate = LocalDate.of(2026, 3, 1).plusDays((n - 1).toLong())

    @Test
    fun `a new span with no existing records is inserted`() {
        val actions = periodReconcileActions(
            desiredSpans = listOf(d(1)..d(4)),
            ownExisting = emptyList(),
            foreignSpans = emptyList(),
        )
        assertEquals(listOf(d(1)..d(4)), actions.toInsert)
        assertTrue(actions.toUpdate.isEmpty())
        assertTrue(actions.toDeleteUids.isEmpty())
    }

    @Test
    fun `a matching unchanged span is a no-op`() {
        val actions = periodReconcileActions(
            desiredSpans = listOf(d(1)..d(4)),
            ownExisting = listOf(ExistingPeriod("uid-1", d(1)..d(4))),
            foreignSpans = emptyList(),
        )
        assertTrue(actions.isEmpty)
    }

    @Test
    fun `a grown span updates the record that shares its start`() {
        val actions = periodReconcileActions(
            desiredSpans = listOf(d(1)..d(5)),
            ownExisting = listOf(ExistingPeriod("uid-1", d(1)..d(4))),
            foreignSpans = emptyList(),
        )
        assertEquals(listOf("uid-1" to (d(1)..d(5))), actions.toUpdate)
        assertTrue(actions.toInsert.isEmpty())
        assertTrue(actions.toDeleteUids.isEmpty())
    }

    @Test
    fun `an earlier first bleeding day updates by overlap not by start`() {
        val actions = periodReconcileActions(
            desiredSpans = listOf(d(1)..d(4)),
            ownExisting = listOf(ExistingPeriod("uid-1", d(2)..d(4))),
            foreignSpans = emptyList(),
        )
        assertEquals(listOf("uid-1" to (d(1)..d(4))), actions.toUpdate)
    }

    @Test
    fun `an own record with no matching span is deleted`() {
        val actions = periodReconcileActions(
            desiredSpans = emptyList(),
            ownExisting = listOf(ExistingPeriod("uid-1", d(1)..d(4))),
            foreignSpans = emptyList(),
        )
        assertEquals(listOf("uid-1"), actions.toDeleteUids)
    }

    @Test
    fun `deleting a middle flow day splits the span into update plus insert`() {
        val actions = periodReconcileActions(
            desiredSpans = listOf(d(1)..d(2), d(6)..d(8)),
            ownExisting = listOf(ExistingPeriod("uid-1", d(1)..d(8))),
            foreignSpans = emptyList(),
        )
        assertEquals(listOf("uid-1" to (d(1)..d(2))), actions.toUpdate)
        assertEquals(listOf(d(6)..d(8)), actions.toInsert)
        assertTrue(actions.toDeleteUids.isEmpty())
    }

    @Test
    fun `a span overlapping a foreign period is not written`() {
        val actions = periodReconcileActions(
            desiredSpans = listOf(d(1)..d(4), d(20)..d(23)),
            ownExisting = emptyList(),
            foreignSpans = listOf(d(3)..d(6)),
        )
        assertEquals(listOf(d(20)..d(23)), actions.toInsert)
    }

    @Test
    fun `an own record shadowed by a new foreign period is removed`() {
        val actions = periodReconcileActions(
            desiredSpans = listOf(d(1)..d(4)),
            ownExisting = listOf(ExistingPeriod("uid-1", d(1)..d(4))),
            foreignSpans = listOf(d(1)..d(4)),
        )
        assertEquals(listOf("uid-1"), actions.toDeleteUids)
        assertTrue(actions.toInsert.isEmpty())
    }
}
