package tech.mmarca.openvitals.core.presentation

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.core.performance.LoadCoordinator

/**
 * Ported from the Flutter `test/core/presentation/period_metric_loader_test.dart`.
 *
 * Kotlin has no shared period-load mixin: every period screen's ViewModel runs
 * its load through [LoadCoordinator], which is where a burst of navigations is
 * coalesced into a single fetch for the newest selection.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PeriodMetricLoaderTest {

    @Test
    fun `rapid navigations coalesce - one fetch in flight, latest wins`() = runTest {
        val coordinator = LoadCoordinator()
        val fetched = mutableListOf<String>()
        val displayed = mutableListOf<String>()

        // Three navigations fired before the first fetch reaches the wire: only
        // the newest is dispatched. A Health Connect read cannot be cancelled,
        // so every skipped fetch here is a slow read that never hits the queue.
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
