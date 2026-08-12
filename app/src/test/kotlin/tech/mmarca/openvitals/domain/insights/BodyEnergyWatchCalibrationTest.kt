package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.preferences.BodyEnergyCalibration

class BodyEnergyWatchCalibrationTest {

    private val day: Instant = Instant.parse("2026-07-22T00:00:00Z")

    private fun point(
        time: Instant,
        score: Int,
        state: BodyEnergyBucketState = BodyEnergyBucketState.REST,
        charge: Double = 0.0,
        intensityDrain: Double = 0.0,
        activityEnergyDrain: Double = 0.0,
        basalDrain: Double = 0.0,
        stressDrain: Double = 0.0,
        recoveryDebtDrain: Double = 0.0,
        primaryInfluence: BodyEnergyPrimaryInfluence = BodyEnergyPrimaryInfluence.STEADY,
    ): BodyEnergyTimelinePoint =
        BodyEnergyTimelinePoint(
            time = time,
            score = score,
            delta = 0.0,
            state = state,
            confidence = BodyEnergyConfidence.HIGH,
            charge = charge,
            intensityDrain = intensityDrain,
            activityEnergyDrain = activityEnergyDrain,
            basalDrain = basalDrain,
            stressDrain = stressDrain,
            recoveryDebtDrain = recoveryDebtDrain,
            primaryInfluence = primaryInfluence,
        )

    private fun timeline(points: List<BodyEnergyTimelinePoint>): BodyEnergyTimeline =
        BodyEnergyTimeline(
            date = LocalDate.of(2026, 7, 22),
            startScore = 80,
            currentScore = points.lastOrNull()?.score ?: 80,
            charged = 0,
            drained = 0,
            points = points,
            confidence = BodyEnergyConfidence.HIGH,
            confidenceReason = "test",
        )

    @Test
    fun `buildWatchObservations downsamples to one reading per bucket`() {
        // The watch emits ~1/minute; feeding all of them in would let one day
        // outvote months of evidence.
        val samples = List(180) { i ->
            WatchBodyEnergySample(time = day.plus(Duration.ofMinutes(i.toLong())), score = 70)
        }
        val timeline = timeline(
            (0 until 180 step 10).map { i ->
                point(time = day.plus(Duration.ofMinutes(i.toLong())), score = 60)
            }
        )

        val readings = buildWatchObservations(samples = samples, timeline = timeline)

        // Three hours of samples → three observations.
        assertEquals(3, readings.size)
    }

    @Test
    fun `buildWatchObservations pairs each reading with the model score at that moment`() {
        val samples = listOf(
            WatchBodyEnergySample(time = day.plus(Duration.ofHours(1)), score = 65),
        )
        val timeline = timeline(
            listOf(
                point(time = day, score = 90),
                point(time = day.plus(Duration.ofHours(1)), score = 80),
            )
        )

        val reading = buildWatchObservations(samples = samples, timeline = timeline).single()

        assertEquals(65, reading.observedScore)
        assertEquals(80, reading.predictedScore)
    }

    @Test
    fun `buildWatchObservations drops readings with no nearby point`() {
        // Attributing an error to a gain the model was not exercising then would
        // teach it the wrong lesson.
        val samples = listOf(
            WatchBodyEnergySample(time = day.plus(Duration.ofHours(12)), score = 50),
        )
        val timeline = timeline(listOf(point(time = day, score = 90)))

        assertTrue(buildWatchObservations(samples = samples, timeline = timeline).isEmpty())
    }

    @Test
    fun `buildWatchObservations skips points the model could not measure`() {
        val samples = listOf(WatchBodyEnergySample(time = day, score = 50))
        val timeline = timeline(
            listOf(point(time = day, score = 50, state = BodyEnergyBucketState.UNMEASURABLE))
        )

        assertTrue(buildWatchObservations(samples = samples, timeline = timeline).isEmpty())
    }

    @Test
    fun `no samples, or no timeline, yields nothing`() {
        assertTrue(
            buildWatchObservations(samples = emptyList(), timeline = timeline(emptyList())).isEmpty()
        )
        assertTrue(
            buildWatchObservations(
                samples = listOf(WatchBodyEnergySample(time = day, score = 50)),
                timeline = timeline(emptyList()),
            ).isEmpty()
        )
    }

    @Test
    fun `the influence comes from the timeline, not a re-derivation`() {
        // The point already carries the influence the timeline computed, with
        // the zone/workout context that reconstructing it from drain components
        // alone would lose.
        val samples = listOf(WatchBodyEnergySample(time = day, score = 50))
        val timeline = timeline(
            listOf(
                point(
                    time = day,
                    score = 80,
                    // Would look like ELEVATED_HEART_RATE if re-derived.
                    stressDrain = 9.0,
                    primaryInfluence = BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY,
                )
            )
        )

        val reading = buildWatchObservations(samples = samples, timeline = timeline).single()

        assertEquals(BodyEnergyPrimaryInfluence.EVERYDAY_ACTIVITY, reading.dominantInfluence)
    }

    @Test
    fun `the observation bucket index is stable across a bucket`() {
        assertEquals(
            watchObservationBucketIndex(day),
            watchObservationBucketIndex(day.plus(Duration.ofMinutes(59))),
        )
        assertEquals(
            watchObservationBucketIndex(day) + 1,
            watchObservationBucketIndex(day.plus(Duration.ofHours(1))),
        )
    }

    // ── fitBodyEnergyGains with watch readings ──────────────────────────────

    private fun exertionReading(observed: Int, predicted: Int): BodyEnergyWatchReading =
        BodyEnergyWatchReading(
            time = day,
            observedScore = observed,
            predictedScore = predicted,
            dominantInfluence = BodyEnergyPrimaryInfluence.EXERTION,
        )

    @Test
    fun `no readings leaves the gains untouched`() {
        val start = BodyEnergyCalibration(activityDrainGain = 1.3)

        assertEquals(1.3, fitBodyEnergyGains(start).activityDrainGain, 1e-9)
    }

    @Test
    fun `a watch reading below prediction raises the drain gain`() {
        // Observed lower than predicted → drained harder than modelled.
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = listOf(exertionReading(50, 70)),
        )

        assertTrue(fitted.activityDrainGain > 1.0)
    }

    @Test
    fun `one reading barely moves a gain`() {
        // A watch reading is another model's opinion, and the fit is meant to
        // converge over days of agreement rather than chase an hour of
        // disagreement.
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = listOf(exertionReading(50, 70)),
        )

        assertTrue(fitted.activityDrainGain - 1.0 < 0.05)
    }

    @Test
    fun `watch readings are counted`() {
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = listOf(exertionReading(50, 70), exertionReading(55, 70)),
        )

        // "Learned from N watch readings" must keep meaning what it says.
        assertEquals(2, fitted.watchObservationCount)
        assertTrue(fitted.hasWatchObservations)
    }

    @Test
    fun `a realistic day of disagreement converges without saturating`() {
        // 24 hourly readings each ~10 points off — the everyday case. The gain
        // should move usefully in a day without pinning to its limit.
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = List(24) { exertionReading(60, 70) },
        )

        assertTrue(fitted.activityDrainGain > 1.1)
        assertTrue(fitted.activityDrainGain < BodyEnergyCalibration.MaxGain)
    }

    @Test
    fun `a day of MAXIMAL disagreement does reach the clamp`() {
        // 24 readings each 100 points wrong. Documented, not accidental: at this
        // learning rate such a day means the model is badly wrong, and a large
        // correction is the right answer. The clamp stops it running away.
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = List(24) { exertionReading(0, 100) },
        )

        assertEquals(BodyEnergyCalibration.MaxGain, fitted.activityDrainGain, 0.0)
    }

    @Test
    fun `gains stay within their bounds however extreme the disagreement`() {
        val fitted = fitBodyEnergyGains(
            BodyEnergyCalibration(),
            watchReadings = List(5000) { exertionReading(0, 100) },
        )

        assertTrue(fitted.activityDrainGain <= BodyEnergyCalibration.MaxGain)
        assertTrue(fitted.activityDrainGain >= BodyEnergyCalibration.MinGain)
    }
}
