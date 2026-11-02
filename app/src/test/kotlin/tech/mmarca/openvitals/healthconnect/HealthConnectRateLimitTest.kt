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

    @Test fun `a second hit inside the window waits longer, up to the top of the ladder`() {
        // A minute's wait and the same burst again just refilled the refusal; each round must wait longer.
        val waits = listOf(0L, 70_000L, 200_000L, 520_000L, 1_500_000L).map { now ->
            HealthConnectRateLimitBackoff.markRateLimited(failure, message, nowMillis = now).retryAfterMillis
        }

        assertEquals(listOf(60_000L, 120_000L, 300_000L, 900_000L, 900_000L), waits)
    }

    @Test fun `a hit long after the last one starts the ladder over`() {
        HealthConnectRateLimitBackoff.markRateLimited(failure, message, nowMillis = 0L)
        HealthConnectRateLimitBackoff.markRateLimited(failure, message, nowMillis = 70_000L)

        val fresh = HealthConnectRateLimitBackoff.markRateLimited(failure, message, nowMillis = 70_000L + 31L * 60_000L)

        assertEquals(60_000L, fresh.retryAfterMillis)
    }

    @Test fun `a read that was already out when the last hit landed does not escalate`() {
        // The two reads in flight beside the refused one are the same burst, not three bursts.
        HealthConnectRateLimitBackoff.markRateLimited(failure, message, nowMillis = 10_000L, readStartedAtMillis = 9_000L)

        val sameBurst = HealthConnectRateLimitBackoff.markRateLimited(
            failure, message, nowMillis = 10_500L, readStartedAtMillis = 9_500L,
        )

        assertEquals(59_500L, sameBurst.retryAfterMillis)
        assertEquals(59_500L, HealthConnectRateLimitBackoff.remainingMillis(nowMillis = 10_500L))
    }

    @Test fun `the deadline flow carries the end of the backoff and clears once it has passed`() {
        assertEquals(0L, HealthConnectRateLimitBackoff.deadlineEpochMillis.value)

        HealthConnectRateLimitBackoff.markRateLimited(failure, message, nowMillis = 5_000L)
        assertEquals(65_000L, HealthConnectRateLimitBackoff.deadlineEpochMillis.value)

        assertEquals(0L, HealthConnectRateLimitBackoff.remainingMillis(nowMillis = 65_000L))
        assertEquals(0L, HealthConnectRateLimitBackoff.deadlineEpochMillis.value)
    }

    private val failure = RuntimeException("Rate limited request quota has been exceeded.")
    private val message: (Long) -> String = { "rate limited" }
}
