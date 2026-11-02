package tech.mmarca.openvitals.features.dashboard

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardRateLimitRecoveryTest {

    private val deadlines = MutableStateFlow(0L)
    private var canReload = true
    private var reloads = 0

    /** Runs on the test clock, so a deadline is a point on the scheduler's time line. */
    private fun TestScope.startRecovery(jitter: Long = 0L) = DashboardRateLimitRecovery(
        deadlines = deadlines,
        canReload = { canReload },
        reload = { reloads += 1 },
        nowMillis = { testScheduler.currentTime },
        jitterMillis = { jitter },
    ).start(backgroundScope)

    @Test fun `reloads once, after the deadline and the jitter`() = runTest {
        startRecovery(jitter = 2_000L)

        deadlines.value = 60_000L
        advanceTimeBy(61_000L)
        assertEquals(0, reloads)
        advanceTimeBy(1_001L)
        assertEquals(1, reloads)

        advanceUntilIdle()
        assertEquals(1, reloads)
    }

    @Test fun `a deadline that moves while it waits restarts the wait`() = runTest {
        // Another reader was refused meanwhile and the backoff climbed a step; firing at the old deadline would be refused too.
        startRecovery()

        deadlines.value = 60_000L
        advanceTimeBy(30_000L)
        deadlines.value = 120_000L
        advanceTimeBy(31_000L)
        assertEquals(0, reloads)
        advanceTimeBy(60_000L)
        assertEquals(1, reloads)
    }

    @Test fun `a second refusal after the reload starts another wait`() = runTest {
        startRecovery()

        deadlines.value = 60_000L
        advanceTimeBy(60_001L)
        assertEquals(1, reloads)

        // The reload was refused too: the readers published a later deadline.
        deadlines.value = 180_001L
        advanceTimeBy(120_001L)
        assertEquals(2, reloads)
    }

    @Test fun `no reload when nothing is degraded, a load is running or the app is in the background`() = runTest {
        canReload = false
        startRecovery()

        deadlines.value = 60_000L
        advanceUntilIdle()

        assertEquals(0, reloads)
    }

    @Test fun `a cleared deadline does nothing`() = runTest {
        startRecovery()

        deadlines.value = 0L
        advanceUntilIdle()

        assertEquals(0, reloads)
    }
}
