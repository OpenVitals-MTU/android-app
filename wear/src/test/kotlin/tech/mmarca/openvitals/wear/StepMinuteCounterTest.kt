package tech.mmarca.openvitals.wear

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.wearlink.WearLinkProtocol
import tech.mmarca.openvitals.wearlink.WearLinkProtocol.StepMinute

class StepMinuteCounterTest {

    private val minute = 1_791_568_860_000L

    private fun counter(lastAt: Long? = null, lastTotal: Long? = null, latest: StepMinute? = null) =
        StepMinuteCounter(lastTotal?.let { StepMinuteCounter.Reading(lastAt ?: minute, it) }, latest)

    @Test
    fun `the first reading of a boot only sets the baseline`() {
        val counter = counter()

        counter.onTotal(minute + 5_000, 12_000)
        assertEquals(emptyList<StepMinute>(), counter.drain())
        counter.onTotal(minute + 20_000, 12_030)
        assertEquals(listOf(StepMinute(minute, 30)), counter.drain())
    }

    @Test
    fun `steps add up within a minute and start again in the next`() {
        val counter = counter(lastTotal = 100)

        counter.onTotal(minute + 10_000, 120)
        counter.onTotal(minute + 59_999, 150)
        assertEquals(listOf(StepMinute(minute, 50)), counter.drain())
        counter.onTotal(minute + 65_000, 155)
        assertEquals(listOf(StepMinute(minute + 60_000, 5)), counter.drain())
    }

    @Test
    fun `a restart inside a minute adds to the stored row instead of replacing it`() {
        val counter = counter(lastAt = minute + 20_000, lastTotal = 500, latest = StepMinute(minute, 40))

        counter.onTotal(minute + 30_000, 507)

        assertEquals(listOf(StepMinute(minute, 47)), counter.drain())
    }

    @Test
    fun `no new steps, or a counter that went back, store nothing`() {
        val counter = counter(lastTotal = 500)

        counter.onTotal(minute, 500)
        counter.onTotal(minute + 1_000, 3)
        assertEquals(emptyList<StepMinute>(), counter.drain())
        counter.onTotal(minute + 2_000, 7)
        assertEquals(listOf(StepMinute(minute, 4)), counter.drain())
    }

    @Test
    fun `steps across a gap are spread over its minutes in proportion to time`() {
        val counter = counter(lastAt = minute + 30_000, lastTotal = 0)

        // 30 s of the first minute, two whole minutes, 30 s of the last: 6 parts of 30 s.
        counter.onTotal(minute + 3 * 60_000 + 30_000, 1_200)

        assertEquals(
            listOf(
                StepMinute(minute, 200),
                StepMinute(minute + 60_000, 400),
                StepMinute(minute + 120_000, 400),
                StepMinute(minute + 180_000, 200),
            ),
            counter.drain(),
        )
    }

    @Test
    fun `a spread keeps every step it can place, and only a minute past a sprint is capped`() {
        val counter = counter(lastAt = minute, lastTotal = 0)

        counter.onTotal(minute + 10 * 60_000, 1_003)
        val rows = counter.drain()
        assertEquals(1_003, rows.sumOf { it.steps })
        assertEquals(10, rows.size)

        val sprint = counter(lastAt = minute, lastTotal = 0)
        sprint.onTotal(minute + 30_000, 5_000)
        val row = sprint.drain().single()
        assertEquals(WearLinkProtocol.MAX_STEPS_PER_MINUTE, row.steps)
        assertEquals(row, WearLinkProtocol.parseStepMinute(WearLinkProtocol.formatStepMinute(row)))
    }

    @Test
    fun `a spread adds to the stored minute it starts in`() {
        val counter = counter(lastAt = minute + 30_000, lastTotal = 100, latest = StepMinute(minute, 90))

        counter.onTotal(minute + 90_000, 160)

        assertEquals(listOf(StepMinute(minute, 120), StepMinute(minute + 60_000, 30)), counter.drain())
    }

    @Test
    fun `a clock set back never replaces a stored minute with a smaller count`() {
        val counter = counter(lastAt = minute + 70_000, lastTotal = 100, latest = StepMinute(minute + 60_000, 90))

        // The wall clock went back across the minute boundary.
        counter.onTotal(minute + 50_000, 103)
        counter.onTotal(minute + 55_000, 105)

        assertEquals(listOf(StepMinute(minute + 60_000, 95)), counter.drain())
    }

    @Test
    fun `a gap of days keeps only the last hour's share, at the gap's pace`() {
        val counter = counter(lastAt = minute, lastTotal = 0)
        val hour = StepMinuteCounter.MAX_SPREAD_MILLIS

        // Two days, 28,800 steps: 10 a minute on average.
        counter.onTotal(minute + 48 * hour, 28_800)

        val rows = counter.drain()
        assertEquals(60, rows.size)
        assertEquals(minute + 47 * hour, rows.first().epochMillis)
        assertTrue(rows.all { it.steps == 10 })
    }

    @Test
    fun `after a reboot the steps since the boot are counted, spread from the boot`() {
        // On a new boot the recorder starts from total 0 at the boot time.
        val counter = counter(lastAt = minute, lastTotal = 0)

        counter.onTotal(minute + 5 * 60_000, 250)

        val rows = counter.drain()
        assertEquals(250, rows.sumOf { it.steps })
        assertTrue(rows.all { it.steps == 50 })
    }
}
