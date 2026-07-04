package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds

class BodyEnergyTimelineTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val date: LocalDate = LocalDate.now(zone)
    private val dayStart: Instant = date.atStartOfDay(zone).toInstant()

    @Test
    fun `manual zones classify sustained exercise as high confidence drain`() {
        val start = dayStart
        val end = start.plus(Duration.ofMinutes(90))

        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 90,
                heartRateSamples = heartRateSamples(start, end, bpm = 165),
                workouts = listOf(workout(start, end)),
                calibration = BodyEnergyCalibration(
                    manualRestingHeartRateBpm = 60,
                    manualMaxHeartRateBpm = 190,
                    manualZoneThresholdsBpm = HeartZoneThresholds(95, 115, 135, 155, 175),
                    useManualZones = true,
                ),
            )
        )

        assertEquals(90, timeline.startScore)
        assertTrue(timeline.currentScore < 65)
        assertTrue(timeline.drained >= 25)
        assertEquals(BodyEnergyConfidence.HIGH, timeline.confidence)
        assertTrue(timeline.points.any { it.intensityDrain > 0.0 })
        assertTrue(timeline.points.any { it.primaryInfluence == BodyEnergyPrimaryInfluence.EXERTION })
    }

    @Test
    fun `long continuous activity adds fatigue beyond simple duration`() {
        val start = dayStart
        val shortEnd = start.plus(Duration.ofMinutes(40))
        val longEnd = start.plus(Duration.ofMinutes(100))
        val calibration = BodyEnergyCalibration(
            manualRestingHeartRateBpm = 60,
            manualMaxHeartRateBpm = 190,
        )

        val shortTimeline = calculateBodyEnergyTimeline(
            inputs(
                now = shortEnd,
                previousEndScore = 90,
                heartRateSamples = heartRateSamples(start, shortEnd, bpm = 130),
                workouts = listOf(workout(start, shortEnd)),
                calibration = calibration,
            )
        )
        val longTimeline = calculateBodyEnergyTimeline(
            inputs(
                now = longEnd,
                previousEndScore = 90,
                heartRateSamples = heartRateSamples(start, longEnd, bpm = 130),
                workouts = listOf(workout(start, longEnd)),
                calibration = calibration,
            )
        )

        assertTrue(longTimeline.drained > shortTimeline.drained * 2)
        assertTrue(longTimeline.currentScore < shortTimeline.currentScore)
    }

    @Test
    fun `sleep charges body energy from the previous score`() {
        val start = dayStart
        val end = start.plus(Duration.ofHours(6))

        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 40,
                heartRateSamples = heartRateSamples(start, end, bpm = 55),
                sleepSessions = listOf(sleep(start, end)),
                calibration = BodyEnergyCalibration(
                    manualRestingHeartRateBpm = 58,
                    manualMaxHeartRateBpm = 188,
                ),
            )
        )

        assertEquals(40, timeline.startScore)
        assertTrue(timeline.currentScore > 70)
        assertTrue(timeline.charged > 30)
        assertTrue(timeline.points.any { it.charge > 0.0 })
        assertTrue(timeline.points.any { it.primaryInfluence == BodyEnergyPrimaryInfluence.SLEEP_RECOVERY })
    }

    @Test
    fun `awake elevated heart rate suppresses charging and adds stress drain`() {
        val start = dayStart
        val end = start.plus(Duration.ofMinutes(60))

        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 70,
                heartRateSamples = heartRateSamples(start, end, bpm = 88),
                calibration = BodyEnergyCalibration(
                    manualRestingHeartRateBpm = 60,
                    manualMaxHeartRateBpm = 190,
                ),
            )
        )

        assertEquals(0, timeline.charged)
        assertTrue(timeline.drained > 0)
        assertTrue(timeline.currentScore < 70)
        assertTrue(timeline.points.any { it.stressDrain > 0.0 })
        assertTrue(timeline.points.any { it.primaryInfluence == BodyEnergyPrimaryInfluence.ELEVATED_HEART_RATE })
    }

    @Test
    fun `recovery debt drain is reported after harder effort`() {
        val start = dayStart
        val workoutEnd = start.plus(Duration.ofMinutes(30))
        val end = start.plus(Duration.ofMinutes(90))

        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 90,
                heartRateSamples = heartRateSamples(start, workoutEnd, bpm = 165) +
                    heartRateSamples(workoutEnd, end, bpm = 62),
                workouts = listOf(workout(start, workoutEnd)),
                calibration = BodyEnergyCalibration(
                    manualRestingHeartRateBpm = 60,
                    manualMaxHeartRateBpm = 190,
                    manualZoneThresholdsBpm = HeartZoneThresholds(95, 115, 135, 155, 175),
                    useManualZones = true,
                ),
            )
        )

        assertTrue(timeline.drained > 0)
        assertTrue(timeline.points.any { it.recoveryDebtDrain > 0.0 })
        assertTrue(timeline.points.any { it.primaryInfluence == BodyEnergyPrimaryInfluence.RECOVERY_DEBT })
    }

    private fun inputs(
        now: Instant,
        previousEndScore: Int,
        heartRateSamples: List<HeartRateSample>,
        calibration: BodyEnergyCalibration,
        workouts: List<ExerciseData> = emptyList(),
        sleepSessions: List<SleepData> = emptyList(),
    ): BodyEnergyTimelineInputs =
        BodyEnergyTimelineInputs(
            date = date,
            heartRateSamples = heartRateSamples,
            sleepSessions = sleepSessions,
            workouts = workouts,
            restingHeartRateBpm = calibration.manualRestingHeartRateBpm?.toLong(),
            observedMaxHeartRateBpm = calibration.manualMaxHeartRateBpm?.toLong(),
            previousEndScore = previousEndScore,
            calibration = calibration,
            now = now,
            zone = zone,
        )

    private fun heartRateSamples(
        start: Instant,
        end: Instant,
        bpm: Long,
    ): List<HeartRateSample> =
        generateSequence(start) { it.plus(Duration.ofMinutes(5)) }
            .takeWhile { it < end }
            .map { time -> HeartRateSample(time = time, beatsPerMinute = bpm, source = "test") }
            .toList()

    private fun workout(start: Instant, end: Instant): ExerciseData =
        ExerciseData(
            id = "workout",
            title = null,
            exerciseType = 0,
            startTime = start,
            endTime = end,
            durationMs = Duration.between(start, end).toMillis(),
            source = "test",
        )

    private fun sleep(start: Instant, end: Instant): SleepData =
        SleepData(
            id = "sleep",
            startTime = start,
            endTime = end,
            durationMs = Duration.between(start, end).toMillis(),
            source = "test",
        )
}
