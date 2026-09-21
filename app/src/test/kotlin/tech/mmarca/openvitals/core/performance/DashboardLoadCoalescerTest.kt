package tech.mmarca.openvitals.core.performance

import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepWindow

class DashboardLoadCoalescerTest {

    @Test fun `concurrent callers share one dashboard load`() = runTest {
        val coalescer = DashboardLoadCoalescer()
        val key = DashboardLoadCoalesceKey.from(
            query = DashboardQuery(
                date = LocalDate.of(2026, 6, 27),
                sleepWindow = SleepWindow.Default,
                activityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
                visibleMetrics = setOf(DashboardMetric.STEPS),
            ),
            granted = setOf("steps"),
            showOpenVitalsCalculatedCalories = false,
        )
        val started = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        var loadCount = 0

        val first = async {
            coalescer.getOrPut(key) {
                loadCount += 1
                started.complete(Unit)
                finish.await()
                DashboardData(date = key.date, steps = 1)
            }
        }
        started.await()

        val second = async {
            coalescer.getOrPut(key) {
                loadCount += 1
                DashboardData(date = key.date, steps = 2)
            }
        }

        finish.complete(Unit)
        assertEquals(1, loadCount)
        assertEquals(1L, first.await().steps)
        assertEquals(1L, second.await().steps)
    }

    @Test fun `a cancelled load does not wedge the key for every later caller`() = runTest {
        // A dashboard tile that stayed on "Loading..." for good: the owner of the shared
        // load was cancelled, its slot was never released, and every later caller waited
        // on a result nobody would ever produce.
        val coalescer = DashboardLoadCoalescer()
        val key = key(DashboardMetric.HYDRATION)
        val started = CompletableDeferred<Unit>()

        val cancelled = async {
            coalescer.getOrPut(key) {
                started.complete(Unit)
                CompletableDeferred<DashboardData>().await()
            }
        }
        started.await()
        cancelled.cancelAndJoin()

        // The next load must run, not wait on the dead one.
        val second = withTimeoutOrNull(1_000) {
            coalescer.getOrPut(key) { DashboardData(date = key.date, steps = 7) }
        }

        assertEquals(7L, second?.steps)
    }

    private fun key(metric: DashboardMetric) = DashboardLoadCoalesceKey.from(
        query = DashboardQuery(
            date = LocalDate.of(2026, 6, 27),
            sleepWindow = SleepWindow.Default,
            activityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
            visibleMetrics = setOf(metric),
        ),
        granted = setOf("steps"),
        showOpenVitalsCalculatedCalories = false,
    )
}
