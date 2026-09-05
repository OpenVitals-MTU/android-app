package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ActivityProgressPoint
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.domain.preferences.HeartZoneThresholds

class BodyEnergyTimelineTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val date: LocalDate = LocalDate.now(zone)
    private val dayStart: Instant = date.atStartOfDay(zone).toInstant()

    // Resting/max pair for the energy-balance cases.
    private val restfulResting = 55L
    private val restfulMax = 190L

    @Test
    fun `manual zones classify sustained exercise as high confidence drain`() {
        val start = dayStart
        val end = start.plus(Duration.ofMinutes(90))

        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 90,
                samples = heartRateSamples(start, end, bpm = 165),
                workouts = listOf(workout(start, end)),
                restingHeartRateBpm = 60,
                observedMaxHeartRateBpm = 190,
                calibration = BodyEnergyCalibration(
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

        val shortTimeline = calculateBodyEnergyTimeline(
            inputs(
                now = shortEnd,
                previousEndScore = 90,
                samples = heartRateSamples(start, shortEnd, bpm = 130),
                workouts = listOf(workout(start, shortEnd)),
                restingHeartRateBpm = 60,
                observedMaxHeartRateBpm = 190,
            )
        )
        val longTimeline = calculateBodyEnergyTimeline(
            inputs(
                now = longEnd,
                previousEndScore = 90,
                samples = heartRateSamples(start, longEnd, bpm = 130),
                workouts = listOf(workout(start, longEnd)),
                restingHeartRateBpm = 60,
                observedMaxHeartRateBpm = 190,
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
                samples = heartRateSamples(start, end, bpm = 55),
                sleepSessions = listOf(sleep(start, end)),
                restingHeartRateBpm = 58,
                observedMaxHeartRateBpm = 188,
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
                samples = heartRateSamples(start, end, bpm = 88),
                restingHeartRateBpm = 60,
                observedMaxHeartRateBpm = 190,
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
                samples = heartRateSamples(start, workoutEnd, bpm = 165) +
                    heartRateSamples(workoutEnd, end, bpm = 62),
                workouts = listOf(workout(start, workoutEnd)),
                restingHeartRateBpm = 60,
                observedMaxHeartRateBpm = 190,
                calibration = BodyEnergyCalibration(
                    manualZoneThresholdsBpm = HeartZoneThresholds(95, 115, 135, 155, 175),
                    useManualZones = true,
                ),
            )
        )

        assertTrue(timeline.drained > 0)
        assertTrue(timeline.points.any { it.recoveryDebtDrain > 0.0 })
        assertTrue(timeline.points.any { it.primaryInfluence == BodyEnergyPrimaryInfluence.RECOVERY_DEBT })
    }

    // Energy-balance behaviour.

    @Test
    fun `an idle waking day declines rather than staying flat`() {
        val start = dayStart.plus(Duration.ofHours(8))
        val end = start.plus(Duration.ofHours(8))
        // Calm, resting heart rate all day, no activity: only basal drain.
        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 80,
                samples = heartRateSamples(start, end, bpm = 58),
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
            )
        )

        // Basal drain should pull an idle day down, never up.
        assertTrue(timeline.currentScore < timeline.startScore)
        assertTrue(timeline.drained > 0)
        // Awake buckets carry a basal cost.
        assertTrue(timeline.points.any { it.basalDrain > 0.0 })
    }

    @Test
    fun `a data gap after the day has shown life keeps draining basal`() {
        // Worn 08:00-12:00, then charging until 18:00. Gap buckets keep the basal drain.
        val wornStart = dayStart.plus(Duration.ofHours(8))
        val wornEnd = wornStart.plus(Duration.ofHours(4))
        val now = dayStart.plus(Duration.ofHours(18))
        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = now,
                previousEndScore = 80,
                samples = heartRateSamples(wornStart, wornEnd, bpm = 58),
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
            )
        )

        val gapPoints = timeline.points.filter { it.time > wornEnd.plus(Duration.ofMinutes(5)) }
        assertTrue(gapPoints.isNotEmpty())
        // An unmeasured awake bucket after the first signal still pays the cost.
        assertTrue(gapPoints.any { it.basalDrain > 0.0 })
        assertTrue(gapPoints.last().score < gapPoints.first().score)
    }

    @Test
    fun `a gap before the first signal of the day stays frozen`() {
        // Nothing recorded until 14:00: the untracked stretch holds the seed
        // and is not billed as wakefulness.
        val firstData = dayStart.plus(Duration.ofHours(14))
        val now = dayStart.plus(Duration.ofHours(16))
        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = now,
                previousEndScore = 80,
                samples = heartRateSamples(firstData, firstData.plus(Duration.ofHours(2)), bpm = 58),
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
            )
        )

        val beforeData = timeline.points.filter { it.time < firstData }
        assertTrue(beforeData.isNotEmpty())
        assertTrue(beforeData.all { it.basalDrain == 0.0 })
        assertEquals(timeline.startScore, beforeData.last().score)
    }

    @Test
    fun `steps without active calories still drain through a gap`() {
        // Phone steps have no calorie series and no heart rate; the steps stand in for the calories.
        val wornStart = dayStart.plus(Duration.ofHours(8))
        val wornEnd = wornStart.plus(Duration.ofHours(4))
        val now = dayStart.plus(Duration.ofHours(18))
        val walkHourEnd = dayStart.plus(Duration.ofHours(15))
        val base = calculateBodyEnergyTimeline(
            inputs(
                now = now,
                previousEndScore = 80,
                samples = heartRateSamples(wornStart, wornEnd, bpm = 58),
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
            )
        )
        val withWalk = calculateBodyEnergyTimeline(
            inputs(
                now = now,
                previousEndScore = 80,
                samples = heartRateSamples(wornStart, wornEnd, bpm = 58),
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
                progress = listOf(
                    // Cumulative steps: 0 by 14:00, 4000 by 15:00 — no kcal series.
                    ActivityProgressPoint(
                        time = dayStart.plus(Duration.ofHours(14)),
                        totalSteps = 0L,
                        totalDistanceMeters = null,
                        totalCaloriesBurnedKcal = null,
                    ),
                    ActivityProgressPoint(
                        time = walkHourEnd,
                        totalSteps = 4000L,
                        totalDistanceMeters = null,
                        totalCaloriesBurnedKcal = null,
                    ),
                ),
            )
        )

        // Phone-recorded steps must drain even with no kcal and no heart rate.
        assertTrue(withWalk.currentScore < base.currentScore)
        val walkPoints = withWalk.points.filter {
            it.time >= dayStart.plus(Duration.ofHours(14)) && it.time < walkHourEnd
        }
        assertTrue(walkPoints.any { it.activityEnergyDrain > 0.0 })
    }

    @Test
    fun `a low-heart-rate high-step day out-drains a sedentary day`() {
        val start = dayStart.plus(Duration.ofHours(8))
        val end = start.plus(Duration.ofHours(8))
        val samples = heartRateSamples(start, end, bpm = 72) // brisk but low zone

        val sedentary = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 80,
                samples = heartRateSamples(start, end, bpm = 58),
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
            )
        )
        // Eight hours of walking/chores: ~80 active kcal/hour, heart rate low.
        val active = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 80,
                samples = samples,
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
                progress = activityProgress(List(8) { 80.0 }, fromHour = 8),
            )
        )

        // Active calories must register even without elevated heart rate.
        assertTrue(active.drained > sedentary.drained)
        // Low-heart-rate movement should read as everyday activity.
        assertTrue(active.points.any { it.primaryInfluence == BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY })
    }

    @Test
    fun `a run out-drains a walk of the same duration`() {
        val start = dayStart.plus(Duration.ofHours(9))
        val end = start.plus(Duration.ofHours(1))

        val walk = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 80,
                samples = heartRateSamples(start, end, bpm = 75),
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
                progress = activityProgress(listOf(120.0), fromHour = 9),
            )
        )
        val run = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 80,
                samples = heartRateSamples(start, end, bpm = 165),
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
                workouts = listOf(workout(start, end)),
                progress = activityProgress(listOf(600.0), fromHour = 9),
            )
        )

        assertTrue(run.drained > walk.drained)
    }

    @Test
    fun `a higher activity-drain gain drains more`() {
        val start = dayStart.plus(Duration.ofHours(8))
        val end = start.plus(Duration.ofHours(8))
        val samples = heartRateSamples(start, end, bpm = 72)
        val progress = activityProgress(List(8) { 80.0 }, fromHour = 8)

        // Seeded full, so neither variant reaches the floor — see below.
        val neutral = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 100,
                samples = samples,
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
                progress = progress,
            )
        )
        val amplified = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 100,
                samples = samples,
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
                progress = progress,
                calibration = BodyEnergyCalibration(activityDrainGain = 1.5),
            )
        )

        // Both must stay off the floor, or the fall caps the drain instead of the model.
        assertTrue(neutral.currentScore > 0)
        assertTrue(amplified.currentScore > 0)
        assertTrue(amplified.drained > neutral.drained)
    }

    // The carry-over seed. Body Energy is a chain, so the seed is what makes midnight not a reset.

    private fun seeded(previousEndScore: Int?): BodyEnergyTimeline =
        calculateBodyEnergyTimeline(
            BodyEnergyTimelineInputs(
                date = date,
                heartRateSamples = heartRateSamples(dayStart, dayStart.plus(Duration.ofHours(1)), bpm = 60),
                previousEndScore = previousEndScore,
                restingHeartRateBpm = 60,
                now = dayStart.plus(Duration.ofHours(1)),
                zone = zone,
            )
        )

    @Test
    fun `a carried score below the floor is raised, and says so`() {
        val timeline = seeded(0)

        assertEquals(BodyEnergyCarryOverFloor, timeline.startScore)
        assertTrue(timeline.inputSummary.carryOverFloorApplied)
        // The raw carried score is kept so the UI can show both.
        assertEquals(0, timeline.inputSummary.previousEndScore)
    }

    @Test
    fun `a carried score above the floor passes through untouched`() {
        val timeline = seeded(40)

        assertEquals(40, timeline.startScore)
        assertFalse(timeline.inputSummary.carryOverFloorApplied)
        assertEquals(BodyEnergySeedSource.CARRIED_OVER, timeline.inputSummary.seedSource)
    }

    @Test
    fun `no previous day starts neutral, and the floor does not apply`() {
        val timeline = seeded(null)

        assertEquals(BodyEnergyNeutralStartScore, timeline.startScore)
        // The floor is for carried scores, not for a cold start.
        assertFalse(timeline.inputSummary.carryOverFloorApplied)
        assertEquals(BodyEnergySeedSource.NEUTRAL, timeline.inputSummary.seedSource)
    }

    @Test
    fun `a day with no usable data carries the seed instead of resetting`() {
        // empty() used to hardcode 50, so a data-less day reset the chain.
        val timeline = calculateBodyEnergyTimeline(
            BodyEnergyTimelineInputs(
                date = date,
                heartRateSamples = emptyList(),
                previousEndScore = 30,
                restingHeartRateBpm = 60,
                now = dayStart.plus(Duration.ofHours(1)),
                zone = zone,
            )
        )

        assertEquals(BodyEnergyConfidence.NO_DATA, timeline.confidence)
        assertEquals(30, timeline.startScore)
        assertEquals(30, timeline.currentScore)
        assertEquals(BodyEnergyReasonCode.NEEDS_HEART_RATE_OR_SLEEP, timeline.confidenceReasonCode)
    }

    @Test
    fun `a data-less day with a sub-floor seed still floors it`() {
        val timeline = calculateBodyEnergyTimeline(
            BodyEnergyTimelineInputs(
                date = date,
                heartRateSamples = emptyList(),
                previousEndScore = 2,
                restingHeartRateBpm = 60,
                now = dayStart.plus(Duration.ofHours(1)),
                zone = zone,
            )
        )

        assertEquals(BodyEnergyCarryOverFloor, timeline.startScore)
        assertEquals(BodyEnergyCarryOverFloor, timeline.currentScore)
    }

    @Test
    fun `an empty timeline window reports its own reason code`() {
        val timeline = calculateBodyEnergyTimeline(
            BodyEnergyTimelineInputs(
                date = date,
                heartRateSamples = emptyList(),
                previousEndScore = 44,
                now = dayStart,
                zone = zone,
            )
        )

        assertEquals(BodyEnergyReasonCode.NO_TIMELINE_WINDOW, timeline.confidenceReasonCode)
        assertEquals(44, timeline.startScore)
    }

    @Test
    fun `a persisted English reason maps back to its code`() {
        assertEquals(
            BodyEnergyReasonCode.STRONG_CALIBRATION,
            bodyEnergyReasonCodeForText("Heart-rate intensity has strong calibration."),
        )
        assertEquals(
            BodyEnergyReasonCode.SPARSE_BUCKETS,
            bodyEnergyReasonCodeForText("Some timeline buckets have sparse Health Connect data."),
        )
        assertEquals(BodyEnergyReasonCode.LEGACY, bodyEnergyReasonCodeForText("something else entirely"))
    }

    // Day totals reconcile: Start + Charged - Drained == end score.
    // Gross unclamped sums used to break this.

    private fun assertReconciles(timeline: BodyEnergyTimeline) {
        assertEquals(
            "Start ${timeline.startScore} + ${timeline.charged} - ${timeline.drained}" +
                " should equal ${timeline.currentScore}",
            timeline.currentScore,
            timeline.startScore + timeline.charged - timeline.drained,
        )
    }

    @Test
    fun `an ordinary day adds up`() {
        val start = dayStart.plus(Duration.ofHours(8))
        val end = start.plus(Duration.ofHours(8))

        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 70,
                samples = heartRateSamples(start, end, bpm = 72),
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
            )
        )

        assertTrue(timeline.currentScore > 0)
        assertTrue(timeline.drained > 0)
        assertReconciles(timeline)
    }

    @Test
    fun `a day that bottoms out reports the fall, not the model`() {
        // Seeded low and drained far harder than there was energy for.
        val timeline = bottomedOutDay()

        assertEquals(0, timeline.currentScore)
        // The cap is what was available, not the starting score.
        assertEquals(timeline.startScore + timeline.charged, timeline.drained)
        assertReconciles(timeline)
    }

    @Test
    fun `a day that tops out reports the rise, not the model`() {
        val start = dayStart
        val end = start.plus(Duration.ofHours(10))

        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 90,
                samples = heartRateSamples(start, end, bpm = 55),
                sleepSessions = listOf(sleep(start, end)),
                restingHeartRateBpm = 58,
            )
        )

        assertEquals(100, timeline.currentScore)
        // A day starting at 90 cannot charge more than 10.
        assertEquals(10, timeline.charged)
        assertReconciles(timeline)
    }

    @Test
    fun `the breakdown sums to the headline on a clamped day`() {
        // Scaling only the totals would leave the breakdown showing the full drain.
        val timeline = bottomedOutDay()

        var charge = 0.0
        var drain = 0.0
        for (point in timeline.points) {
            charge += point.charge
            drain += point.basalDrain + point.appliedActivityDrain + point.stressDrain + point.recoveryDebtDrain
        }

        assertEquals(0, timeline.currentScore)
        assertEquals(timeline.drained.toDouble(), drain, 1.0)
        assertEquals(timeline.charged.toDouble(), charge, 1.0)
    }

    @Test
    fun `a bucket that both charges and drains still feeds both totals`() {
        // Waking mid-bucket: charge and drain in one bucket.
        // The clamp is attributed proportionally so neither side is lost.
        val wake = dayStart.plus(Duration.ofHours(6)).plus(Duration.ofMinutes(2))
        val end = dayStart.plus(Duration.ofHours(8))

        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 50,
                samples = heartRateSamples(dayStart, end, bpm = 62),
                sleepSessions = listOf(sleep(dayStart, wake)),
                restingHeartRateBpm = 58,
            )
        )

        assertTrue(timeline.charged > 0)
        assertTrue(timeline.drained > 0)
        assertReconciles(timeline)
    }

    @Test
    fun `a fully clamped bucket keeps a truthful driver at zero magnitude`() {
        // primaryInfluence comes from the raw magnitudes, or a hard workout would read as steady.
        val timeline = bottomedOutDay()

        val flattened = timeline.points.filter { it.score == 0 && it.delta == 0.0 }
        // The fixture must run past the floor.
        assertTrue(flattened.isNotEmpty())
        // A bucket that moved nothing must contribute nothing.
        assertTrue(flattened.all { it.basalDrain == 0.0 && it.appliedActivityDrain == 0.0 })
        // The driver must survive the scaling.
        assertTrue(flattened.any { it.primaryInfluence != BodyEnergyPrimaryInfluence.STEADY })
    }

    private fun bottomedOutDay(): BodyEnergyTimeline {
        val start = dayStart.plus(Duration.ofHours(8))
        val end = start.plus(Duration.ofHours(8))
        return calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 30,
                samples = heartRateSamples(start, end, bpm = 72),
                restingHeartRateBpm = restfulResting,
                observedMaxHeartRateBpm = restfulMax,
                progress = activityProgress(List(8) { 120.0 }, fromHour = 8),
                calibration = BodyEnergyCalibration(activityDrainGain = 2.0),
            )
        )
    }

    // The recovery-debt drain is correctable.
    // It used to be scaled by no gain, so the fit could not correct it.

    private fun afterHardWorkout(activityGain: Double): BodyEnergyTimeline {
        val start = dayStart.plus(Duration.ofHours(8))
        val workoutEnd = start.plus(Duration.ofMinutes(40))
        val end = start.plus(Duration.ofHours(3))
        return calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 100,
                samples = heartRateSamples(start, workoutEnd, bpm = 170) +
                    heartRateSamples(workoutEnd, end, bpm = 62),
                workouts = listOf(workout(start, workoutEnd)),
                restingHeartRateBpm = 60,
                observedMaxHeartRateBpm = 190,
                calibration = BodyEnergyCalibration(activityDrainGain = activityGain),
            )
        )
    }

    @Test
    fun `recovery debt scales with the activity gain`() {
        val neutral = afterHardWorkout(1.0).points.sumOf { it.recoveryDebtDrain }
        val amplified = afterHardWorkout(2.0).points.sumOf { it.recoveryDebtDrain }

        // The fixture must actually arm recovery debt.
        assertTrue(neutral > 0.0)
        assertEquals(neutral * 2.0, amplified, 0.01)
    }

    @Test
    fun `recovery debt does not drag the basal drain with it`() {
        // Basal answers for the waking floor only; the activity gain must not move it.
        val neutral = afterHardWorkout(1.0).points.sumOf { it.basalDrain }
        val amplified = afterHardWorkout(2.0).points.sumOf { it.basalDrain }

        assertEquals(neutral, amplified, 0.01)
    }

    @Test
    fun `a sleep-then-workout day never labels a bucket quiet rest`() {
        val start = dayStart
        val wake = dayStart.plus(Duration.ofHours(7))
        val workoutStart = dayStart.plus(Duration.ofHours(9))
        val end = dayStart.plus(Duration.ofHours(14))

        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 80,
                samples = heartRateSamples(start, wake, bpm = 52) +
                    // A gap between wake and the workout leaves unmeasurable buckets.
                    heartRateSamples(workoutStart, end, bpm = 150),
                sleepSessions = listOf(sleep(start, wake)),
                workouts = listOf(workout(workoutStart, end)),
                restingHeartRateBpm = 55,
                observedMaxHeartRateBpm = 190,
            )
        )

        assertTrue(timeline.points.isNotEmpty())
        assertFalse(timeline.points.any { it.primaryInfluence == BodyEnergyPrimaryInfluence.QUIET_REST })
        // The fixture must exercise the branches that could have reached it.
        assertTrue(timeline.points.any { it.primaryInfluence == BodyEnergyPrimaryInfluence.SLEEP_RECOVERY })
    }

    // The waking-rest charge. v3 removed it and the chain sat on the floor, losing ~10 points a day.

    private fun quietDay(wakingBpm: Long, restingBpm: Long = 58): BodyEnergyTimeline {
        val wake = dayStart.plus(Duration.ofHours(7))
        val end = dayStart.plus(Duration.ofHours(22))
        return calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 50,
                samples = heartRateSamples(dayStart, end, bpm = wakingBpm),
                sleepSessions = listOf(sleep(dayStart, wake)),
                restingHeartRateBpm = restingBpm,
            )
        )
    }

    private fun chargesAwake(timeline: BodyEnergyTimeline): Boolean =
        timeline.points.any { it.charge > 0.0 && it.state != BodyEnergyBucketState.SLEEP }

    @Test
    fun `a quiet waking day now recovers instead of only declining`() {
        val timeline = quietDay(wakingBpm = 60)

        // A day spent resting should end higher than it began.
        assertTrue(timeline.currentScore > timeline.startScore)
        // The charge must come from waking buckets, not only sleep.
        assertTrue(chargesAwake(timeline))
    }

    @Test
    fun `the rest ceiling is a share of reserve, not a fixed offset`() {
        // The old resting-plus-8 band earned zero rest charge six days in seven.
        // A reserve fraction moves with the person. Resting 60, max 190: reserve 130, 15% is 79.5 bpm.
        fun at(bpm: Long): BodyEnergyTimeline {
            val wake = dayStart.plus(Duration.ofHours(7))
            val end = dayStart.plus(Duration.ofHours(20))
            return calculateBodyEnergyTimeline(
                inputs(
                    now = end,
                    previousEndScore = 50,
                    samples = heartRateSamples(dayStart, end, bpm = bpm),
                    sleepSessions = listOf(sleep(dayStart, wake)),
                    restingHeartRateBpm = 60,
                    observedMaxHeartRateBpm = 190,
                )
            )
        }

        // 75 bpm is 15 above resting — outside the old band, inside this one.
        assertTrue(chargesAwake(at(75)))
        // 88 bpm is 21% of reserve but 28 above resting and in the top stress tier.
        assertFalse(chargesAwake(at(88)))
    }

    @Test
    fun `the rest charge does not fire once the heart rate leaves the resting band`() {
        // The gate: an active day must gain nothing from it.
        val resting = quietDay(wakingBpm = 60)
        val busy = quietDay(wakingBpm = 95)

        assertTrue(busy.charged < resting.charged)
        assertFalse(chargesAwake(busy))
    }

    @Test
    fun `a trickle of activity drain does not block the rest charge`() {
        // Requiring zero activity drain made the charge inert:
        // the interpolated hourly series puts a sliver of drain nearly everywhere.
        val wake = dayStart.plus(Duration.ofHours(7))
        val end = dayStart.plus(Duration.ofHours(22))
        val timeline = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 50,
                samples = heartRateSamples(dayStart, end, bpm = 60),
                sleepSessions = listOf(sleep(dayStart, wake)),
                // A sedentary day still logs a slow drip of active calories.
                progress = activityProgress(List(15) { 4.0 }, fromHour = 7),
                restingHeartRateBpm = 58,
            )
        )

        val charging = timeline.points.filter {
            it.charge > 0.0 && it.state != BodyEnergyBucketState.SLEEP
        }
        assertTrue(charging.isNotEmpty())
        // The point of the fix: charge and a small drain coexist.
        assertTrue(charging.any { it.appliedActivityDrain > 0.0 })
        assertTrue(timeline.currentScore > timeline.startScore)
    }

    @Test
    fun `the rest charge is suppressed while recovery debt is still being billed`() {
        // Sitting quietly after a hard session is recovery debt. Charging through it would overstate recovery.
        val timeline = quietAfterWorkout()

        val debtBuckets = timeline.points.filter { it.recoveryDebtDrain > 0.0 }
        // The fixture must arm recovery debt.
        assertTrue(debtBuckets.isNotEmpty())
        // No bucket may both carry recovery debt and charge.
        assertTrue(debtBuckets.all { it.charge == 0.0 })
    }

    @Test
    fun `a charging waking bucket reports quiet rest`() {
        val timeline = quietDay(wakingBpm = 60)

        assertTrue(timeline.points.any { it.primaryInfluence == BodyEnergyPrimaryInfluence.QUIET_REST })
    }

    @Test
    fun `but a larger drain still outranks quiet rest`() {
        // QUIET_REST competes; whichever moved the score more is reported.
        val timeline = quietAfterWorkout()

        assertTrue(timeline.points.any { it.primaryInfluence == BodyEnergyPrimaryInfluence.RECOVERY_DEBT })
    }

    private fun quietAfterWorkout(): BodyEnergyTimeline {
        val start = dayStart.plus(Duration.ofHours(8))
        val workoutEnd = start.plus(Duration.ofMinutes(40))
        val end = start.plus(Duration.ofHours(3))
        return calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 80,
                samples = heartRateSamples(start, workoutEnd, bpm = 170) +
                    heartRateSamples(workoutEnd, end, bpm = 60),
                workouts = listOf(workout(start, workoutEnd)),
                restingHeartRateBpm = 58,
                observedMaxHeartRateBpm = 190,
            )
        )
    }

    @Test
    fun `the age-derived max heart rate uses Tanaka, like the rest of the app`() {
        // Body Energy used 220 - age while heart-rate recovery used Tanaka.
        // With age 33 and resting 60, zone 3 starts at 135 bpm under Tanaka and 136.2 under 220 - age.
        // So 136 is zone 3 only under Tanaka; 140 is zone 3 under both.
        val start = dayStart.plus(Duration.ofHours(8))
        val end = start.plus(Duration.ofHours(2))

        fun drainAt(bpm: Long): Int = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 100,
                samples = heartRateSamples(start, end, bpm = bpm),
                // Birth year only, so the max has to be derived.
                bodyProfile = BodyProfile(birthYear = date.year - 33),
                restingHeartRateBpm = 60,
            )
        ).drained

        // 136 bpm must already be zone 3, as Tanaka puts it.
        assertEquals(drainAt(140), drainAt(136))
        // 130 must still be zone 2, or the fixture spans one zone.
        assertTrue(drainAt(130) < drainAt(136))
    }

    @Test
    fun `the manual profile heart rates no longer reach the model`() {
        // v11 removed the manual resting/max inputs; Body Energy must ignore them.
        val start = dayStart.plus(Duration.ofHours(8))
        val end = start.plus(Duration.ofHours(2))
        val samples = heartRateSamples(start, end, bpm = 120)

        val withoutProfile = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 100,
                samples = samples,
                restingHeartRateBpm = 60,
            )
        )
        val withProfile = calculateBodyEnergyTimeline(
            inputs(
                now = end,
                previousEndScore = 100,
                samples = samples,
                restingHeartRateBpm = 60,
                bodyProfile = BodyProfile(restingHeartRateBpm = 40, maxHeartRateBpm = 220),
            )
        )

        assertNotNull(withoutProfile.confidenceReasonCode)
        assertEquals(withoutProfile.drained, withProfile.drained)
        assertEquals(withoutProfile.confidence, withProfile.confidence)
    }

    // Fixtures.

    private fun inputs(
        now: Instant,
        previousEndScore: Int?,
        samples: List<HeartRateSample>,
        bodyProfile: BodyProfile = BodyProfile(),
        restingHeartRateBpm: Long? = null,
        observedMaxHeartRateBpm: Long? = null,
        calibration: BodyEnergyCalibration = BodyEnergyCalibration(),
        workouts: List<ExerciseData> = emptyList(),
        sleepSessions: List<SleepData> = emptyList(),
        progress: List<ActivityProgressPoint> = emptyList(),
        basalMetabolicRate: Double? = null,
    ): BodyEnergyTimelineInputs =
        BodyEnergyTimelineInputs(
            date = date,
            heartRateSamples = samples,
            sleepSessions = sleepSessions,
            workouts = workouts,
            activityProgress = progress,
            basalMetabolicRateKcalPerDay = basalMetabolicRate,
            restingHeartRateBpm = restingHeartRateBpm,
            observedMaxHeartRateBpm = observedMaxHeartRateBpm,
            previousEndScore = previousEndScore,
            calibration = calibration,
            bodyProfile = bodyProfile,
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

    /** Hourly active kcal from [fromHour], accumulated into the cumulative series the algorithm expects. */
    private fun activityProgress(
        hourlyActiveKcal: List<Double>,
        fromHour: Long = 0,
    ): List<ActivityProgressPoint> {
        var cumulative = 0.0
        return hourlyActiveKcal.mapIndexed { index, kcal ->
            cumulative += kcal
            ActivityProgressPoint(
                time = dayStart.plus(Duration.ofHours(fromHour + index + 1)),
                totalSteps = 0L,
                totalDistanceMeters = null,
                totalCaloriesBurnedKcal = null,
                totalActiveCaloriesKcal = cumulative,
            )
        }
    }

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
