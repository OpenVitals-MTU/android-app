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

/** [Recorder] captures the order the drains ran in and whether any two overlapped. */
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

    private val stepDistance = mockk<StepDistanceBackfillService>().also {
        coEvery { it.syncIncremental() } coAnswers { recorder.run("stepDistance") }
    }

    private val scheduler = HistorySyncScheduler(vitals, calories, bodyEnergy, stepDistance)

    @Test
    fun `the drains run one after another, never at the same time`() = runTest {
        // Health Connect serializes reads, so overlapping drains are the contention the sequencing avoids.
        scheduler.drainIncrementalOnce()

        assertEquals(listOf("vitals", "calories", "bodyEnergy", "stepDistance"), recorder.started)
        assertEquals(emptySet<String>(), recorder.overlapped)
    }

    @Test
    fun `no drain starts a first full sync`() = runTest {
        // A first full sync is a multi-minute read owned by the screen that needs it.
        scheduler.drainIncrementalOnce()

        coVerify(exactly = 0) { vitals.syncAll() }
        coVerify(exactly = 0) { calories.syncAll() }
    }

    @Test
    fun `a failing drain does not starve the ones after it`() = runTest {
        // The once-per-open latch is claimed before the first drain, so an escaped throw would abandon the rest for the process's life.
        coEvery { vitals.syncIncremental() } throws IllegalStateException("read failed")

        scheduler.drainIncrementalOnce()

        assertEquals(listOf("calories", "bodyEnergy", "stepDistance"), recorder.started)
    }

    @Test
    fun `a cancelled drain still unwinds`() = runTest {
        // Cancellation is not a failing drain.
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

        assertEquals(listOf("vitals", "calories", "bodyEnergy", "stepDistance"), recorder.started)
    }
}
