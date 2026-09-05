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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/** Matches HealthConnectRateLimitBackoff's own backoff, which is not public. */
private const val DEFAULT_BACKOFF_MILLIS = 60_000L

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class HealthConnectReaderSupportTest {

    @Before
    fun setUp() {
        HealthConnectRateLimitBackoff.resetForTest()
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
        val support = support()
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

    private fun support(): HealthConnectReaderSupport {
        val diagnostics = mockk<HealthConnectDiagnostics>()
        every { diagnostics.summary() } returns "diagnostics"
        return HealthConnectReaderSupport(
            clientProvider = { mockk<HealthConnectClient>() },
            diagnostics = diagnostics,
            rateLimitMessage = { "rate limited" },
        )
    }
}
