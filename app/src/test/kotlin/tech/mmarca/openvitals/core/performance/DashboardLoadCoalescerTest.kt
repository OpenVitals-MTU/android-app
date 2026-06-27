package tech.mmarca.openvitals.core.performance

import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardMetric
import tech.mmarca.openvitals.domain.model.DashboardQuery
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.SleepRangeMode

class DashboardLoadCoalescerTest {

    @Test fun `concurrent callers share one dashboard load`() = runTest {
        val coalescer = DashboardLoadCoalescer()
        val key = DashboardLoadCoalesceKey.from(
            query = DashboardQuery(
                date = LocalDate.of(2026, 6, 27),
                sleepRangeMode = SleepRangeMode.EVENING_18H,
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
}
