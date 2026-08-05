package tech.mmarca.openvitals.healthconnect

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The per-day decision table of the step-derived distance backfill. A wrong
 * branch either double-counts distance (derived record beside real data) or
 * deletes records outside the readable window that the next run cannot
 * rebuild, so every branch is pinned.
 */
class StepDistanceReconcileTest {

    private fun d(n: Int): LocalDate = LocalDate.of(2026, 8, 1).plusDays((n - 1).toLong())

    private fun own(n: Int, meters: Double) = OwnStepDistanceRecord("uid-$n", d(n), meters)

    @Test
    fun `a steps-only day gets a derived record`() {
        val actions = stepDistanceReconcileActions(
            days = listOf(d(1)),
            stepsByDay = mapOf(d(1) to 1000L),
            foreignDistanceDays = emptySet(),
            ownByDay = emptyMap(),
            strideMeters = 0.7,
        )
        assertEquals(listOf(StepDistanceUpsert(d(1), 700.0)), actions.toUpsert)
        assertTrue(actions.toDeleteUids.isEmpty())
    }

    @Test
    fun `foreign distance evicts the derived record`() {
        val actions = stepDistanceReconcileActions(
            days = listOf(d(1)),
            stepsByDay = mapOf(d(1) to 1000L),
            foreignDistanceDays = setOf(d(1)),
            ownByDay = mapOf(d(1) to own(1, 700.0)),
            strideMeters = 0.7,
        )
        assertEquals(listOf("uid-1"), actions.toDeleteUids)
        assertTrue(actions.toUpsert.isEmpty())
    }

    @Test
    fun `a foreign day without an own record is a no-op`() {
        val actions = stepDistanceReconcileActions(
            days = listOf(d(1)),
            stepsByDay = mapOf(d(1) to 1000L),
            foreignDistanceDays = setOf(d(1)),
            ownByDay = emptyMap(),
            strideMeters = 0.7,
        )
        assertTrue(actions.isEmpty)
    }

    @Test
    fun `a stride change rewrites the derived record`() {
        val actions = stepDistanceReconcileActions(
            days = listOf(d(1)),
            stepsByDay = mapOf(d(1) to 1000L),
            foreignDistanceDays = emptySet(),
            ownByDay = mapOf(d(1) to own(1, 700.0)),
            strideMeters = 0.8,
        )
        assertEquals(listOf(StepDistanceUpsert(d(1), 800.0)), actions.toUpsert)
    }

    @Test
    fun `an unchanged value within the epsilon is not rewritten`() {
        val actions = stepDistanceReconcileActions(
            days = listOf(d(1)),
            stepsByDay = mapOf(d(1) to 1000L),
            foreignDistanceDays = emptySet(),
            ownByDay = mapOf(d(1) to own(1, 700.2)),
            strideMeters = 0.7,
        )
        assertTrue(actions.isEmpty)
    }

    @Test
    fun `a day whose steps vanished loses its derived record`() {
        val actions = stepDistanceReconcileActions(
            days = listOf(d(1)),
            stepsByDay = emptyMap(),
            foreignDistanceDays = emptySet(),
            ownByDay = mapOf(d(1) to own(1, 700.0)),
            strideMeters = 0.7,
        )
        assertEquals(listOf("uid-1"), actions.toDeleteUids)
    }

    @Test
    fun `days outside the window are never touched`() {
        val actions = stepDistanceReconcileActions(
            days = listOf(d(2)),
            stepsByDay = mapOf(d(2) to 500L),
            foreignDistanceDays = emptySet(),
            // Own record on d(1), which is OUTSIDE the readable window.
            ownByDay = mapOf(d(1) to own(1, 700.0), d(2) to own(2, 350.0)),
            strideMeters = 0.7,
        )
        assertTrue(actions.isEmpty)
    }

    @Test
    fun `mixed window resolves each day independently`() {
        val actions = stepDistanceReconcileActions(
            days = listOf(d(1), d(2), d(3)),
            stepsByDay = mapOf(d(1) to 1000L, d(2) to 2000L, d(3) to 0L),
            foreignDistanceDays = setOf(d(2)),
            ownByDay = mapOf(d(2) to own(2, 1400.0), d(3) to own(3, 100.0)),
            strideMeters = 0.7,
        )
        assertEquals(listOf(StepDistanceUpsert(d(1), 700.0)), actions.toUpsert)
        assertEquals(listOf("uid-2", "uid-3"), actions.toDeleteUids)
    }
}
