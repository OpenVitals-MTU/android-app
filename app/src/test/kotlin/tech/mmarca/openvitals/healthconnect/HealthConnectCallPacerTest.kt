package tech.mmarca.openvitals.healthconnect

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class HealthConnectCallPacerTest {

    @Test fun `with no gap nothing waits`() = runTest {
        val pacer = HealthConnectCallPacer(nowMillis = { 0L }, baseGapMillis = 0L)

        assertEquals(listOf(0L, 0L, 0L), List(3) { pacer.reserve(afterRefusal = false) })
    }

    @Test fun `slots are handed out one gap apart from the latest of now and the last slot`() = runTest {
        var now = 1_000L
        val pacer = HealthConnectCallPacer(nowMillis = { now }, baseGapMillis = 150L)

        assertEquals(0L, pacer.reserve(afterRefusal = false))
        assertEquals(150L, pacer.reserve(afterRefusal = false))
        assertEquals(300L, pacer.reserve(afterRefusal = false))

        // A quiet spell: the next caller does not pay for slots nobody used.
        now = 10_000L
        assertEquals(0L, pacer.reserve(afterRefusal = false))
    }

    @Test fun `after a refusal the gap widens, on the platform provider too`() = runTest {
        val pacer = HealthConnectCallPacer(nowMillis = { 0L }, baseGapMillis = 0L, afterRefusalGapMillis = 250L)

        assertEquals(0L, pacer.reserve(afterRefusal = true))
        assertEquals(250L, pacer.reserve(afterRefusal = true))
    }
}
