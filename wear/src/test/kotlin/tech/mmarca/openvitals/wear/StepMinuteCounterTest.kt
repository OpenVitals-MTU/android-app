package tech.mmarca.openvitals.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import tech.mmarca.openvitals.wearlink.WearLinkProtocol

class StepMinuteCounterTest {

    private val minute = 1_791_568_860_000L

    @Test
    fun `the first reading of a boot only sets the baseline`() {
        val counter = StepMinuteCounter(lastTotal = null, latest = null)

        assertNull(counter.onTotal(minute + 5_000, 12_000))
        assertEquals(WearLinkProtocol.StepMinute(minute, 30), counter.onTotal(minute + 20_000, 12_030))
    }

    @Test
    fun `steps add up within a minute and start again in the next`() {
        val counter = StepMinuteCounter(lastTotal = 100, latest = null)

        assertEquals(WearLinkProtocol.StepMinute(minute, 20), counter.onTotal(minute + 10_000, 120))
        assertEquals(WearLinkProtocol.StepMinute(minute, 50), counter.onTotal(minute + 59_999, 150))
        assertEquals(WearLinkProtocol.StepMinute(minute + 60_000, 5), counter.onTotal(minute + 60_000, 155))
    }

    @Test
    fun `a restart inside a minute adds to the stored row instead of replacing it`() {
        val counter = StepMinuteCounter(lastTotal = 500, latest = WearLinkProtocol.StepMinute(minute, 40))

        assertEquals(WearLinkProtocol.StepMinute(minute, 47), counter.onTotal(minute + 30_000, 507))
    }

    @Test
    fun `no new steps, or a counter that went back, store nothing`() {
        val counter = StepMinuteCounter(lastTotal = 500, latest = null)

        assertNull(counter.onTotal(minute, 500))
        assertNull(counter.onTotal(minute + 1_000, 3))
        assertEquals(WearLinkProtocol.StepMinute(minute, 4), counter.onTotal(minute + 2_000, 7))
    }

    @Test
    fun `a jump past a sprint is capped, so the row stays one the phone accepts`() {
        val counter = StepMinuteCounter(lastTotal = 0, latest = null)

        val row = counter.onTotal(minute, 5_000)

        assertEquals(WearLinkProtocol.MAX_STEPS_PER_MINUTE, row?.steps)
        assertEquals(row, WearLinkProtocol.parseStepMinute(WearLinkProtocol.formatStepMinute(row!!)))
    }
}
