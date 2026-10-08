package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.minutes
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.table
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.worn

/**
 * The nights that must never exist (a watch on a table, on the charger,
 * off the wrist) and the one that must: the wear state in front of the
 * estimator is what tells them apart.
 */
class WearSleepEstimatorTest {

    private val start: Instant = Instant.parse("2026-10-07T20:30:00Z")

    @Test
    fun `a worn night is one session`() {
        val night = WearSleepEstimator.estimate(WearSleepFixtures.wornNight(start))

        assertNotNull(night)
        night!!
        assertTrue(night.session.sleepMinutes > 400)
        assertEquals(0, night.notWornMinutes)
        assertTrue(night.summary().contains("worn=540"))
    }

    @Test
    fun `a watch on the table all night is no session, with or without the heart rate grant`() {
        assertNull(WearSleepEstimator.estimate(minutes(start, 600) { _, time -> table(time) }))
        assertNull(WearSleepEstimator.estimate(minutes(start, 600) { _, time -> table(time, heartRateRecording = false) }))
    }

    @Test
    fun `a watch on the charger all night is no session`() {
        assertNull(WearSleepEstimator.estimate(minutes(start, 600) { _, time -> table(time, charging = true) }))
    }

    @Test
    fun `a watch off the wrist all night is no session`() {
        assertNull(WearSleepEstimator.estimate(minutes(start, 600) { _, time -> table(time, offBody = true) }))
    }

    @Test
    fun `two hours on the charger before bed do not pull the night forward`() {
        val rows = minutes(start, 120) { _, time -> table(time, charging = true) } +
            WearSleepFixtures.wornNight(start.plusSeconds(120 * 60))

        val night = WearSleepEstimator.estimate(rows)

        assertNotNull(night)
        assertTrue(!night!!.session.onset.isBefore(start.plusSeconds(120 * 60)))
        assertEquals(120, night.notWornReasons[NotWornReason.CHARGING])
    }

    @Test
    fun `too few worn minutes is no session`() {
        val rows = minutes(start, 100) { _, time -> worn(time, 0f, 50f) } +
            minutes(start.plusSeconds(100 * 60), 500) { _, time -> table(time) }

        assertNull(WearSleepEstimator.estimate(rows))
    }

    @Test
    fun `order and duplicates do not change the night`() {
        val rows = WearSleepFixtures.wornNight(start)
        val shuffled = rows.shuffled(java.util.Random(7)) + rows.take(50)

        assertEquals(WearSleepEstimator.estimate(rows), WearSleepEstimator.estimate(shuffled))
    }
}
