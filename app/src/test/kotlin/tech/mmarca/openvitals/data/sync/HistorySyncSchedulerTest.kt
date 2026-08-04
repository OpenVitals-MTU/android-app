package tech.mmarca.openvitals.data.sync

import android.util.Log
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Port of the Flutter `history_sync_scheduler_test.dart` suite.
 *
 * The Dart fakes implement the service interfaces; the Kotlin services are
 * concrete classes, so the same recording stand-in is built with mockk over
 * them. [Recorder] is the Dart `_Recorder`: it captures the order the drains
 * ran in and whether any two were ever in flight at once.
 */
class HistorySyncSchedulerTest {

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    /** Records the order and overlap of the drains it stands in for. */
    private class Recorder {
        val started = mutableListOf<String>()
        val finished = mutableListOf<String>()
        val inFlight = mutableSetOf<String>()
        val overlapped = mutableSetOf<String>()

        suspend fun run(name: String) {
            started += name
            if (inFlight.isNotEmpty()) overlapped += inFlight
            inFlight += name
            yield()
            inFlight -= name
            finished += name
        }
    }

    private val recorder = Recorder()

    private val vitals = mockk<VitalsHistorySyncService>().also {
        coEvery { it.syncIncremental() } coAnswers { recorder.run("vitals") }
        coEvery { it.syncAll() } coAnswers { recorder.run("vitals-full") }
    }

    private val calories = mockk<CaloriesHistorySyncService>().also {
        coEvery { it.syncIncremental() } coAnswers { recorder.run("calories") }
        coEvery { it.syncAll() } coAnswers { recorder.run("calories-full") }
    }

    private val bodyEnergy = mockk<BodyEnergyChainSyncService>().also {
        coEvery { it.syncAll(any()) } coAnswers { recorder.run("bodyEnergy") }
    }

    private val scheduler = HistorySyncScheduler(vitals, calories, bodyEnergy)

    @Test
    fun `the drains run one after another, never at the same time`() = runTest {
        // Health Connect serializes concurrent reads, so overlapping drains are
        // the 30s->80s contention the per-screen sequencing exists to avoid.
        scheduler.drainIncrementalOnce()

        assertEquals(listOf("vitals", "calories", "bodyEnergy"), recorder.started)
        assertEquals(emptySet<String>(), recorder.overlapped)
    }

    @Test
    fun `no drain starts a first full sync`() = runTest {
        // A first full sync is a multi-minute history read and stays owned by
        // the screen that needs the cache: a user who never opens the vitals
        // overview must not pay for its 730-day reads on every app open.
        scheduler.drainIncrementalOnce()

        coVerify(exactly = 0) { vitals.syncAll() }
        coVerify(exactly = 0) { calories.syncAll() }
    }

    @Test
    fun `a failing drain does not starve the ones after it`() = runTest {
        // The once-per-open latch is claimed before the first drain runs, so a
        // throw that escaped here would abandon the remaining drains for the
        // life of the process, not just this open.
        coEvery { vitals.syncIncremental() } throws IllegalStateException("read failed")

        scheduler.drainIncrementalOnce()

        assertEquals(listOf("calories", "bodyEnergy"), recorder.started)
    }

    @Test
    fun `a cancelled drain still unwinds`() = runTest {
        // Cancellation is not a failing drain: swallowing it would keep the
        // scheduler running after its caller has gone away.
        coEvery { calories.syncIncremental() } throws CancellationException("gone")

        var cancelled = false
        try {
            scheduler.drainIncrementalOnce()
        } catch (_: CancellationException) {
            cancelled = true
        }

        assertEquals(true, cancelled)
        assertEquals(listOf("vitals"), recorder.started)
    }

    @Test
    fun `concurrent calls share one run`() = runTest {
        coroutineScope {
            listOf(
                async { scheduler.drainIncrementalOnce() },
                async { scheduler.drainIncrementalOnce() },
                async { scheduler.drainIncrementalOnce() },
            ).awaitAll()
        }

        assertEquals(listOf("vitals", "calories", "bodyEnergy"), recorder.started)
    }
}
