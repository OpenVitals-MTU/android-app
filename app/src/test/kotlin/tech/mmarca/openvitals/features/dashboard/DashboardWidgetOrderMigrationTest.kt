package tech.mmarca.openvitals.features.dashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Appending widgets added by an update to a layout saved before they existed, without
 * resurrecting widgets the user removed.
 */
class DashboardWidgetOrderMigrationTest {

    private class Recorder {
        var order: List<String>? = null
        var known: Set<String>? = null
        var writes = 0

        fun persist(newOrder: List<String>?, newKnown: Set<String>) {
            writes++
            newOrder?.let { order = it }
            known = newKnown
        }
    }

    private val allIds = DashboardWidgetId.entries.map { it.name }.toSet()

    @Test
    fun `a layout saved before the watch widget existed gains it where the default puts it`() {
        val stored = listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.SLEEP.name,
            DashboardWidgetId.CYCLE.name,
        )
        val recorder = Recorder()

        val result = dashboardWidgetIdsWithNewOnesAppended(
            storedIds = stored,
            knownIds = null,
            persist = recorder::persist,
        )

        // The default order puts the watch after the hero rings, before SLEEP.
        assertEquals(
            listOf(DashboardWidgetId.STEPS, DashboardWidgetId.WATCH, DashboardWidgetId.SLEEP, DashboardWidgetId.CYCLE),
            result,
        )
        // Only the new id moved: the kept widgets stay in the user's order.
        assertEquals(
            listOf(DashboardWidgetId.STEPS.name, DashboardWidgetId.SLEEP.name, DashboardWidgetId.CYCLE.name),
            recorder.order?.filterNot { it == DashboardWidgetId.WATCH.name },
        )
        assertEquals(allIds, recorder.known)
    }

    @Test
    fun `a widget already in the layout is never inserted twice`() {
        val stored = listOf(
            DashboardWidgetId.STEPS.name,
            DashboardWidgetId.WATCH.name,
            DashboardWidgetId.SLEEP.name,
        )
        val recorder = Recorder()

        // The saved order says it is already placed, and the order wins.
        val result = dashboardWidgetIdsWithNewOnesAppended(
            storedIds = stored,
            knownIds = null,
            persist = recorder::persist,
        )

        assertEquals(1, result.count { it == DashboardWidgetId.WATCH })
        assertEquals(1, recorder.order?.count { it == DashboardWidgetId.WATCH.name } ?: 1)
        // And it stays where the user put it.
        assertEquals(1, result.indexOf(DashboardWidgetId.WATCH))
    }

    @Test
    fun `a widget the user removed stays removed`() {
        val stored = listOf(DashboardWidgetId.STEPS.name, DashboardWidgetId.SLEEP.name)
        val recorder = Recorder()

        // Everything has been offered already, so FLOORS is absent because the user dropped it.
        val result = dashboardWidgetIdsWithNewOnesAppended(
            storedIds = stored,
            knownIds = allIds,
            persist = recorder::persist,
        )

        assertEquals(listOf(DashboardWidgetId.STEPS, DashboardWidgetId.SLEEP), result)
        assertEquals(0, recorder.writes)
    }

    @Test
    fun `a dashboard never edited keeps the defaults and is never given an order`() {
        val recorder = Recorder()

        val result = dashboardWidgetIdsWithNewOnesAppended(
            storedIds = null,
            knownIds = null,
            persist = recorder::persist,
        )

        assertEquals(DefaultDashboardWidgetIds, result)
        // Writing an order here would read as "the user removed everything".
        assertNull(recorder.order)
        assertEquals(allIds, recorder.known)
        assertTrue(DashboardWidgetId.WATCH in result)
    }

    @Test
    fun `running twice appends once`() {
        val stored = listOf(DashboardWidgetId.STEPS.name)
        val recorder = Recorder()

        val first = dashboardWidgetIdsWithNewOnesAppended(stored, null, recorder::persist)
        val second = dashboardWidgetIdsWithNewOnesAppended(
            storedIds = recorder.order,
            knownIds = recorder.known,
            persist = recorder::persist,
        )

        assertEquals(first, second)
        assertEquals(1, second.count { it == DashboardWidgetId.WATCH })
        // The second pass had nothing to do.
        assertEquals(1, recorder.writes)
    }

    @Test
    fun `a future widget is placed without a hardcoded list`() {
        val stored = DashboardWidgetId.entries.map { it.name }
        val recorder = Recorder()

        // Pretend CYCLE is the new one. The migration is driven by the known-set difference alone.
        val result = dashboardWidgetIdsWithNewOnesAppended(
            storedIds = stored - DashboardWidgetId.CYCLE.name,
            knownIds = allIds - DashboardWidgetId.CYCLE.name,
            persist = recorder::persist,
        )

        // After MINDFULNESS, which precedes it in the default order.
        assertTrue(
            result.indexOf(DashboardWidgetId.CYCLE) >
                result.indexOf(DashboardWidgetId.MINDFULNESS),
        )
    }

    @Test
    fun `an id the default order never mentions does not block placement`() {
        val recorder = Recorder()

        // BLOOD_GLUCOSE has no default position; it must not act as a wall.
        val result = dashboardWidgetIdsWithNewOnesAppended(
            storedIds = listOf(DashboardWidgetId.BLOOD_GLUCOSE.name, DashboardWidgetId.SLEEP.name),
            knownIds = null,
            persist = recorder::persist,
        )

        assertTrue(DashboardWidgetId.WATCH in result)
        // Before SLEEP, which is where the default order puts it.
        assertTrue(result.indexOf(DashboardWidgetId.WATCH) < result.indexOf(DashboardWidgetId.SLEEP))
    }
}
