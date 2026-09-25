package tech.mmarca.openvitals.healthconnect

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The per-day decision table of the basal metabolic rate estimate. A wrong branch overwrites a watch's rate or deletes an unrebuildable record. */
class BmrEstimateReconcileTest {

    private fun d(n: Int): LocalDate = LocalDate.of(2026, 8, 1).plusDays((n - 1).toLong())

    private fun own(n: Int, kcal: Double) = OwnBmrRecord("uid-$n", d(n), kcal)

    @Test
    fun `a day with an estimate and nothing else gets a record`() {
        val actions = bmrReconcileActions(
            days = listOf(d(1)),
            estimateByDay = mapOf(d(1) to 1648.75),
            foreignBmrDays = emptySet(),
            ownByDay = emptyMap(),
        )
        assertEquals(listOf(BmrUpsert(d(1), 1648.75)), actions.toUpsert)
        assertTrue(actions.toDeleteUids.isEmpty())
    }

    @Test
    fun `a foreign rate evicts the estimate`() {
        val actions = bmrReconcileActions(
            days = listOf(d(1)),
            estimateByDay = mapOf(d(1) to 1648.75),
            foreignBmrDays = setOf(d(1)),
            ownByDay = mapOf(d(1) to own(1, 1648.75)),
        )
        assertEquals(listOf("uid-1"), actions.toDeleteUids)
        assertTrue(actions.toUpsert.isEmpty())
    }

    @Test
    fun `a foreign day without an own record is a no-op`() {
        val actions = bmrReconcileActions(
            days = listOf(d(1)),
            estimateByDay = mapOf(d(1) to 1648.75),
            foreignBmrDays = setOf(d(1)),
            ownByDay = emptyMap(),
        )
        assertTrue(actions.isEmpty)
    }

    @Test
    fun `a changed input rewrites the record, a sub-kcal drift does not`() {
        val rewritten = bmrReconcileActions(
            days = listOf(d(1)),
            estimateByDay = mapOf(d(1) to 1748.75),
            foreignBmrDays = emptySet(),
            ownByDay = mapOf(d(1) to own(1, 1648.75)),
        )
        assertEquals(listOf(BmrUpsert(d(1), 1748.75)), rewritten.toUpsert)

        val kept = bmrReconcileActions(
            days = listOf(d(1)),
            estimateByDay = mapOf(d(1) to 1649.5),
            foreignBmrDays = emptySet(),
            ownByDay = mapOf(d(1) to own(1, 1648.75)),
        )
        assertTrue(kept.isEmpty)
    }

    @Test
    fun `a day that lost its estimate loses its record, days outside the window are untouched`() {
        val actions = bmrReconcileActions(
            days = listOf(d(1), d(2)),
            estimateByDay = mapOf(d(2) to 1648.75),
            foreignBmrDays = emptySet(),
            ownByDay = mapOf(d(1) to own(1, 1648.75), d(2) to own(2, 1648.75), d(3) to own(3, 1648.75)),
        )
        assertEquals(listOf("uid-1"), actions.toDeleteUids)
        assertTrue(actions.toUpsert.isEmpty())
    }
}
