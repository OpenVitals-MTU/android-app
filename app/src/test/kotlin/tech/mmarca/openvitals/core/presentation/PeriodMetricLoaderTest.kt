package tech.mmarca.openvitals.core.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.performance.LoadCoordinator

/** Every period screen runs its load through [LoadCoordinator], which coalesces a burst of navigations into one fetch. */
@OptIn(ExperimentalCoroutinesApi::class)
class PeriodMetricLoaderTest {

    @Test
    fun `rapid navigations coalesce - one fetch in flight, latest wins`() = runTest {
        val coordinator = LoadCoordinator()
        val fetched = mutableListOf<String>()
        val displayed = mutableListOf<String>()

        // Three navigations before the first fetch reaches the wire: only the newest is dispatched.
        listOf("week", "month", "year").forEach { selection ->
            coordinator.launch(this) {
                fetched += selection
                if (!isCurrent) return@launch
                displayed += selection
            }
        }
        advanceUntilIdle()

        assertEquals(listOf("year"), fetched)
        assertEquals(listOf("year"), displayed)
    }
}
