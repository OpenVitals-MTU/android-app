package tech.mmarca.openvitals.domain.insights

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.minutes
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.table
import tech.mmarca.openvitals.domain.insights.WearSleepFixtures.worn
import tech.mmarca.openvitals.domain.model.SleepMinuteKind
import tech.mmarca.openvitals.domain.model.WearSleepMinute

class SleepWakeScorerTest {

    private val start: Instant = Instant.parse("2026-10-07T22:00:00Z")

    private fun score(rows: List<WearSleepMinute>, config: SleepWakeScorer.Config = SleepWakeScorer.Config()): String {
        val grid = WearMinuteGrid.of(rows)!!
        return String(SleepWakeScorer.score(grid, WearStateDetector.detect(grid), config))
    }

    @Test
    fun `movement alone reproduces the Cole-Kripke boundary`() {
        // No heart rate term (no pulse, grant off) and a still arm: only the activity term can speak.
        val rows = minutes(start, 120) { index, time ->
            val movement = if (index in 40 until 80) 9f else 0f
            worn(time, movement, 0f, flags = 0, zAngleDelta = 0f).copy(heartRate = null, heartRateSamples = 0, heartRateSd = null, zAngleMax = 85f)
        }
        val config = SleepWakeScorer.Config(angleMovingWeight = 0f)

        val labels = score(rows, config)

        assertTrue(labels, labels.substring(0, 40).all { it == 'S' })
        // The movement weighs on the two minutes after it, and Webster's three passes keep twelve more.
        assertTrue(labels, labels.substring(40, 94).all { it == 'W' })
        assertTrue(labels, labels.substring(94, 120).all { it == 'S' })
    }

    @Test
    fun `a rising pulse tips a lightly moving minute to wake, and alone it does not`() {
        // A twenty-minute bump of fifteen beats in an otherwise flat night, the arm still throughout.
        val stillArm = minutes(start, 180) { index, time ->
            val rate = if (index in 80 until 100) 65f else 50f
            worn(time, 0f, rate, zAngleDelta = 0f).copy(zAngleMax = 85f)
        }
        // The same bump with light movement all night: 2.5 weighs 7.2 against the threshold of 8.
        val lightArm = minutes(start, 180) { index, time ->
            val rate = if (index in 80 until 100) 65f else 50f
            worn(time, 2.5f, rate, zAngleDelta = 0f).copy(zAngleMax = 85f)
        }

        val still = score(stillArm)
        val light = score(lightArm)

        // Heart rate alone is a weak vote (Walch: 50% specificity on its own); it never flips a still minute.
        assertTrue(still, still.all { it == 'S' })
        assertTrue(light, light.substring(10, 70).all { it == 'S' })
        assertTrue(light, light.substring(76, 104).count { it == 'W' } >= 10)
        assertTrue(light, light.substring(130, 180).all { it == 'S' })
    }

    @Test
    fun `an arm that keeps moving pushes a borderline minute to wake`() {
        val rows = minutes(start, 120) { index, time ->
            val delta = if (index in 40 until 80) 8f else 0f
            worn(time, 2.5f, 50f, zAngleDelta = delta).copy(zAngleMax = 85f + delta)
        }

        val labels = score(rows)

        // Movement 2.5 weighs 7.2 against the threshold of 8 on its own; with the arm moving it crosses it.
        assertTrue(labels, labels.substring(10, 36).all { it == 'S' })
        assertTrue(labels, labels.substring(46, 78).all { it == 'W' })
    }

    @Test
    fun `not worn is a gap and the watch's own awake verdict is wake`() {
        val rows = minutes(start, 90) { index, time ->
            when {
                index < 20 -> table(time, charging = true)
                index < 40 -> worn(time, 0f, 50f, kind = SleepMinuteKind.AWAKE)
                else -> worn(time, 0f, 50f, zAngleDelta = 0f).copy(zAngleMax = 85f)
            }
        }

        val labels = score(rows)

        assertEquals("G".repeat(20), labels.substring(0, 20))
        assertEquals("W".repeat(20), labels.substring(20, 40))
        // The awake minutes weigh on the next two, and Webster's passes keep a dozen more after that wake run.
        assertTrue(labels, labels.substring(60, 90).all { it == 'S' })
    }
}
