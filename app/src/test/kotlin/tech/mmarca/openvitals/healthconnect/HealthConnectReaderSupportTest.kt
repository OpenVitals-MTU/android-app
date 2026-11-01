package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.core.performance.DefaultDispatcherProvider
import tech.mmarca.openvitals.core.performance.DispatcherProvider
import java.util.concurrent.atomic.AtomicInteger

/** Matches HealthConnectRateLimitBackoff's own backoff, which is not public. */
private const val DEFAULT_BACKOFF_MILLIS = 60_000L

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HealthConnectReaderSupportTest {

    @Before
    fun setUp() {
        HealthConnectRateLimitBackoff.resetForTest()
        HealthConnectCallCounters.resetForTest()
        mockkStatic(Log::class)
        every { Log.d(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
        every { Log.e(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
        HealthConnectRateLimitBackoff.resetForTest()
    }

    @Test fun `withLogging gives up on a rate limit instead of waiting it out`() = runTest {
        val support = support()
        var attempts = 0
        val startedAt = testScheduler.currentTime

        val result = support.withLogging("read", fallback = 7) {
            attempts += 1
            throw RuntimeException("Request rejected. Rate limited request quota has been exceeded.")
        }

        assertEquals(7, result)
        assertEquals(1, attempts)
        // Riding out the minute-long backoff once per read let a single throttled call hold a screen for minutes.
        assertTrue(testScheduler.currentTime - startedAt < DEFAULT_BACKOFF_MILLIS)
    }

    @Test fun `withLogging does not attempt a read while the backoff is armed`() = runTest {
        val support = support()
        support.withLogging("first", fallback = 0) {
            throw RuntimeException("Request rejected. Rate limited request quota has been exceeded.")
        }

        var attempted = false
        val startedAt = testScheduler.currentTime
        val result = support.withLogging("second", fallback = 7) {
            attempted = true
            42
        }

        assertEquals(7, result)
        assertFalse(attempted)
        assertEquals(0L, testScheduler.currentTime - startedAt)
    }

    @Test fun `withLoggingOrThrow waits the backoff out rather than giving up`() = runTest {
        val support = support(nowMillis = { testScheduler.currentTime })
        var attempts = 0

        // The sync path has no screen behind it, so resuming after a minute beats restarting.
        val result = support.withLoggingOrThrow("sync") {
            attempts += 1
            if (attempts == 1) {
                throw RuntimeException("Request rejected. Rate limited request quota has been exceeded.")
            }
            42
        }

        assertEquals(42, result)
        assertEquals(2, attempts)
    }

    @Test fun `withLoggingOrThrow keeps waiting when the deadline moves while it sleeps`() = runTest {
        val support = support(nowMillis = { testScheduler.currentTime })
        support.withLogging("screen", fallback = 0) { throw quotaError() }
        val deadlineAtFirstHit = HealthConnectRateLimitBackoff.deadlineEpochMillis.value
        var ranAt = -1L

        val sync = async {
            support.withLoggingOrThrow("sync") { ranAt = testScheduler.currentTime; 1 }
        }
        // Another reader is refused while the sync sleeps: the deadline is now later than when it went to sleep.
        advanceTimeBy(1_000)
        HealthConnectRateLimitBackoff.markRateLimited(quotaError(), { "rate limited" }, nowMillis = testScheduler.currentTime)
        sync.await()

        // The first hit armed a minute; the second, inside the window, two. The sync waited for the second.
        assertEquals(60_000L, deadlineAtFirstHit)
        assertEquals(121_000L, ranAt)
    }

    @Test fun `reads queued behind the permit when a refusal lands return their fallback without calling`() = runTest {
        // Eight reads behind two permits. The first is refused; the ones still waiting for a permit must not go out.
        // On the test dispatcher the two reads in flight resume in order, so the refusal lands before any permit moves on.
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = object : DispatcherProvider {
            override val main = dispatcher
            override val io = dispatcher
            override val default = dispatcher
        }
        val support = support(nowMillis = { testScheduler.currentTime }, dispatchers = dispatchers)
        val calls = AtomicInteger(0)

        val results = awaitAll(
            *(0 until 8).map { index ->
                async {
                    support.withLogging("read-$index", fallback = -1) {
                        calls.incrementAndGet()
                        delay(50)
                        if (index == 0) throw quotaError()
                        index
                    }
                }
            }.toTypedArray(),
        )

        // Only the two that held a permit when the refusal landed were sent.
        assertEquals(2, calls.get())
        assertEquals(7, results.count { it == -1 })
        assertEquals(2L, HealthConnectCallCounters.snapshot().total.started)
        assertEquals(1L, HealthConnectCallCounters.snapshot().total.rateLimited)
        assertEquals(6L, HealthConnectCallCounters.snapshot().total.skippedForBackoff)
    }

    @Test fun `the counters tell a sent read from one skipped for the backoff`() = runTest {
        val support = support(nowMillis = { testScheduler.currentTime })

        support.withLogging("readSleep[2026]", fallback = 0) { 1 }
        support.withLogging("readSleep[2027]", fallback = 0) { throw quotaError() }
        support.withLogging("readSteps", fallback = 0) { 1 }

        val counts = HealthConnectCallCounters.snapshot().counts
        assertEquals(HealthConnectCallCount(started = 2, succeeded = 1, rateLimited = 1), counts.getValue("readSleep"))
        assertEquals(HealthConnectCallCount(skippedForBackoff = 1), counts.getValue("readSteps"))
    }

    @Test fun `withLogging rethrows cancellation`() = runTest {
        val support = support()
        var cancelled = false

        try {
            support.withLogging("read[id]", fallback = 7) {
                throw CancellationException("cancelled")
            }
        } catch (error: CancellationException) {
            cancelled = true
        }

        assertTrue(cancelled)
    }

    @Test fun `withLogging allows bounded concurrent reads`() = runTest {
        val support = support()
        val activeReads = AtomicInteger(0)
        val maxActiveReads = AtomicInteger(0)

        awaitAll(
            *(0 until 8).map { index ->
                async {
                    support.withLogging("read-$index", fallback = Unit) {
                        val active = activeReads.incrementAndGet()
                        maxActiveReads.updateAndGet { currentMax -> maxOf(currentMax, active) }
                        delay(100)
                        activeReads.decrementAndGet()
                    }
                }
            }.toTypedArray(),
        )

        assertTrue(maxActiveReads.get() > 1)
        assertTrue(maxActiveReads.get() <= 4)
    }

    @Test fun `a strict read rethrows the failure a screen read swallows`() = runTest {
        val support = support()

        assertEquals(7, support.withLogging("read", fallback = 7) { throw IllegalStateException("binder died") })
        val thrown = runCatching {
            withStrictHealthConnectReads {
                support.withLogging("read", fallback = 7) { throw IllegalStateException("binder died") }
            }
        }.exceptionOrNull()

        // A writer that got 7 here would save it as the truth.
        assertTrue(thrown is IllegalStateException)
        assertEquals("binder died", thrown?.message)
    }

    @Test fun `a strict read throws while the backoff is armed, without trying`() = runTest {
        val support = support()
        support.withLogging("read", fallback = 0) { throw RuntimeException("Rate limited request quota has been exceeded.") }
        var attempts = 0

        val thrown = runCatching {
            withStrictHealthConnectReads { support.withLogging("read", fallback = 0) { attempts += 1; 1 } }
        }.exceptionOrNull()

        // After one rate-limit hit every later chunk of a 730-day sync came back empty at once.
        assertTrue(thrown is HealthConnectRateLimitException)
        assertEquals(0, attempts)
    }

    @Test fun `a strict read throws while Health Connect access is paused`() = runTest {
        val support = support(syncEnabled = { false })

        assertEquals(7, support.withLogging("read", fallback = 7) { 1 })
        val thrown = runCatching {
            withStrictHealthConnectReads { support.withNullableLogging("read") { 1 } }
        }.exceptionOrNull()

        assertTrue(thrown is IllegalStateException)
    }

    @Test fun `strictness reaches the child coroutines a sync fans out to`() = runTest {
        val support = support()

        val thrown = runCatching {
            withStrictHealthConnectReads {
                async { support.withLogging("read", fallback = emptyList<Int>()) { error("ipc failed") } }.await()
            }
        }.exceptionOrNull()

        assertEquals("ipc failed", thrown?.message)
    }

    @Test fun `a skipped or refused read marks the outcome degraded, a successful one does not`() = runTest {
        // A screen read never throws for a refusal, so this is how a tile tells "no data" from "would not say".
        val support = support(nowMillis = { testScheduler.currentTime })

        val clean = withHealthConnectReadOutcome { support.withLogging("read", fallback = 0) { 1 } }
        val refused = withHealthConnectReadOutcome { support.withLogging("read", fallback = 0) { throw quotaError() } }
        val skipped = withHealthConnectReadOutcome {
            async { support.withLogging("read", fallback = 0) { 1 } }.await()
        }

        assertEquals(1, clean.value)
        assertFalse(clean.degraded)
        assertTrue(refused.degraded)
        // Skipped for the backoff, in a child coroutine: the outcome reaches it.
        assertTrue(skipped.degraded)
    }

    @Test fun `call starts keep the pacing gap, and a wider one once a refusal has been seen`() = runTest {
        // A hundred calls in three seconds spend a window quota; the same hundred spaced out do not.
        val dispatcher = StandardTestDispatcher(testScheduler)
        val dispatchers = object : DispatcherProvider {
            override val main = dispatcher
            override val io = dispatcher
            override val default = dispatcher
        }
        val support = support(nowMillis = { testScheduler.currentTime }, dispatchers = dispatchers, pacingGapMillis = 150L)
        val starts = mutableListOf<Long>()

        awaitAll(*(0 until 4).map { async { support.withLogging("read", fallback = 0) { starts += testScheduler.currentTime; 1 } } }.toTypedArray())
        assertEquals(listOf(0L, 150L, 300L, 450L), starts.sorted())

        starts.clear()
        support.withLogging("read", fallback = 0) { throw quotaError() }
        advanceTimeBy(DEFAULT_BACKOFF_MILLIS + 1)
        awaitAll(*(0 until 3).map { async { support.withLogging("read", fallback = 0) { starts += testScheduler.currentTime; 1 } } }.toTypedArray())
        val gaps = starts.sorted().zipWithNext { a, b -> b - a }
        assertEquals(listOf(250L, 250L), gaps)
    }

    private fun quotaError() = RuntimeException("Request rejected. Rate limited request quota has been exceeded.")

    private fun support(
        syncEnabled: () -> Boolean = { true },
        nowMillis: () -> Long = System::currentTimeMillis,
        dispatchers: DispatcherProvider = DefaultDispatcherProvider,
        pacingGapMillis: Long = 0L,
    ): HealthConnectReaderSupport {
        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "diagnostics"
        return HealthConnectReaderSupport(
            clientProvider = { mockk<HealthConnectClient>() },
            diagnostics = diagnostics,
            rateLimitMessage = { "rate limited" },
            syncEnabled = syncEnabled,
            dispatchers = dispatchers,
            nowMillis = nowMillis,
            pacingGapMillis = pacingGapMillis,
        )
    }
}
