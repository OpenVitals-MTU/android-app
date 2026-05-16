package tech.mmarca.openvitals.healthconnect

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

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

    @Test fun `withLogging waits and retries once after rate limit`() = runTest {
        val support = support()
        var attempts = 0

        val result = support.withLogging("read", fallback = 0) {
            attempts += 1
            if (attempts == 1) {
                throw RuntimeException("Request rejected. Rate limited request quota has been exceeded.")
            }
            42
        }

        assertEquals(42, result)
        assertEquals(2, attempts)
    }

    @Test fun `withLogging returns fallback when retry is rate limited again`() = runTest {
        val support = support()

        val result = support.withLogging("read", fallback = 7) {
            throw RuntimeException("Request rejected. Rate limited request quota has been exceeded.")
        }

        assertEquals(7, result)
    }

    @Test fun `withLogging serializes concurrent reads`() = runTest {
        val support = support()
        val events = mutableListOf<String>()

        awaitAll(
            async {
                support.withLogging("first", fallback = Unit) {
                    events += "first-start"
                    delay(100)
                    events += "first-end"
                }
            },
            async {
                support.withLogging("second", fallback = Unit) {
                    events += "second-start"
                    delay(100)
                    events += "second-end"
                }
            },
        )

        assertEquals(
            listOf("first-start", "first-end", "second-start", "second-end"),
            events,
        )
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
