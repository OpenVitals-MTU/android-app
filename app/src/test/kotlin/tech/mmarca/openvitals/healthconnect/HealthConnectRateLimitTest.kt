package tech.mmarca.openvitals.healthconnect

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthConnectRateLimitTest {

    @After
    fun tearDown() {
        HealthConnectRateLimitBackoff.resetForTest()
    }

    @Test fun `isRateLimitFailure detects quota errors in cause chain`() {
        val failure = RuntimeException(
            "wrapper",
            RuntimeException("Request rejected. Rate limited request quota has been exceeded."),
        )

        assertTrue(HealthConnectRateLimitBackoff.isRateLimitFailure(failure))
    }

    @Test fun `isRateLimitFailure ignores unrelated errors`() {
        val failure = RuntimeException("Health Connect unavailable")

        assertFalse(HealthConnectRateLimitBackoff.isRateLimitFailure(failure))
    }

    @Test fun `markRateLimited activates cooldown`() {
        val failure = RuntimeException("Rate limited request quota has been exceeded.")
        val messageForRetryAfter: (Long) -> String = { retryAfterMillis ->
            "localized ${retryAfterMinutes(retryAfterMillis)}m"
        }

        val exception = HealthConnectRateLimitBackoff.markRateLimited(
            cause = failure,
            messageForRetryAfter = messageForRetryAfter,
            nowMillis = 1_000L,
        )

        assertEquals(60_000L, exception.retryAfterMillis)
        assertEquals("localized 1m", exception.message)
        val active = assertThrows(HealthConnectRateLimitException::class.java) {
            HealthConnectRateLimitBackoff.throwIfActive(
                messageForRetryAfter = messageForRetryAfter,
                nowMillis = 30_000L,
            )
        }
        assertEquals(31_000L, active.retryAfterMillis)
        assertEquals("localized 1m", active.message)
    }
}
