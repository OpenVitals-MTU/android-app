package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.minutes
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.table
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.worn
import tech.mmarca.openvitals.domain.model.WearMinuteFlags

class WearStateDetectorTest {

    private val start: Instant = Instant.parse("2026-10-07T21:00:00Z")

    private fun detect(rows: List<tech.mmarca.openvitals.domain.model.WearSleepMinute>) =
        WearStateDetector.detect(WearMinuteGrid.of(rows)!!)

    @Test
    fun `a worn night is worn throughout`() {
        val states = detect(WearSleepFixtures.wornNight(start))

        assertEquals(0, states.notWornMinutes)
    }

    @Test
    fun `charging and off-body are not worn whatever the pulse says`() {
        val rows = minutes(start, 240) { index, time ->
            when {
                index < 60 -> worn(time, 0f, 55f, flags = WearMinuteFlags.HR_RECORDING or WearMinuteFlags.CHARGING)
                index < 120 -> worn(time, 0f, 55f, flags = WearMinuteFlags.HR_RECORDING or WearMinuteFlags.OFF_BODY)
                else -> worn(time, 0f, 55f)
            }
        }

        val states = detect(rows)

        assertEquals(mapOf(NotWornReason.CHARGING to 60, NotWornReason.OFF_BODY to 60), states.reasonCounts)
        assertTrue((120 until 240).all { states.isWorn(it) })
    }

    @Test
    fun `a short pulse dropout is tolerated, a longer one is a bare wrist`() {
        val rows = minutes(start, 120) { index, time ->
            when (index) {
                in 30..31 -> table(time)
                in 60..69 -> table(time)
                else -> worn(time, 0f, 55f)
            }
        }

        val states = detect(rows)

        assertTrue(states.isWorn(30) && states.isWorn(31))
        assertEquals(10, states.reasonCounts[NotWornReason.NO_PULSE])
    }

    @Test
    fun `a still hour without any pulse is not worn even when the grant is missing`() {
        val rows = minutes(start, 180) { _, time -> table(time, heartRateRecording = false) }

        val states = detect(rows)

        assertEquals(180, states.reasonCounts[NotWornReason.STILL_NO_PULSE])
    }

    @Test
    fun `a still hour with a pulse is a sleeper, not a table`() {
        val rows = minutes(start, 180) { _, time -> worn(time, 0f, 50f, sdG = 0.002f, zAngleDelta = 0f) }

        assertEquals(0, detect(rows).notWornMinutes)
    }

    @Test
    fun `a restless watch without a pulse is worn by the accelerometer rule`() {
        // No grant, no pulse, but the wrist moves: the rule cannot call it a table.
        val rows = minutes(start, 180) { index, time -> worn(time, 3f, 0f, flags = 0, sdG = 0.05f).copy(heartRate = null, heartRateSamples = 0) }

        assertEquals(0, detect(rows).notWornMinutes)
    }

    @Test
    fun `a missing minute is absent and a watch that stops recording is a gap`() {
        val rows = minutes(start, 60) { _, time -> worn(time, 0f, 55f) } +
            minutes(start.plusSeconds(90 * 60), 60) { _, time -> worn(time, 0f, 55f) }

        val states = detect(rows)

        assertEquals(30, states.reasonCounts[NotWornReason.ABSENT])
        assertEquals(120, states.wornMinutes)
    }
}
