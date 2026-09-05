package tech.mmarca.openvitals.core.performance

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The newest-wins guard every screen's load runs under. One [LoadCoordinator], pinned once. */
class LoadCoordinatorTest {

    @Test
    fun `a stale load does not clobber a newer one`() = runTest {
        val coordinator = LoadCoordinator()
        val gate = CompletableDeferred<Unit>()
        val settled = mutableListOf<String>()
        var stale: LoadCoordinator.LoadScope? = null

        // A slow load: it reaches the repository and is still waiting there.
        coordinator.launch(this) {
            stale = this
            // Swallowing the cancellation proves the `isCurrent` check carries the guard on its own.
            runCatching { gate.await() }
            if (isCurrent) settled += "first"
        }
        advanceUntilIdle()

        // The user changes the period before the first load comes back.
        coordinator.launch(this) {
            if (isCurrent) settled += "second"
        }
        advanceUntilIdle()

        // The first load finally answers — with the wrong period's data.
        gate.complete(Unit)
        advanceUntilIdle()

        assertEquals(listOf("second"), settled)
        assertFalse(
            "the stale load still believed it owned the screen, so its result " +
                "would have overwritten the newer one and reset the loading flag",
            stale!!.isCurrent,
        )
    }

    @Test
    fun `the load in flight is the one that owns the screen`() = runTest {
        val coordinator = LoadCoordinator()
        var current: LoadCoordinator.LoadScope? = null

        coordinator.launch(this) { current = this }
        advanceUntilIdle()

        assertTrue(current!!.isCurrent)
    }

    @Test
    fun `a superseded load is cancelled, not merely ignored`() = runTest {
        val coordinator = LoadCoordinator()
        val gate = CompletableDeferred<Unit>()
        var reachedTheEnd = false

        coordinator.launch(this) {
            gate.await()
            reachedTheEnd = true
        }
        advanceUntilIdle()

        coordinator.launch(this) { }
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        assertFalse(reachedTheEnd)
    }
}
