package tech.mmarca.openvitals.domain.insights

import androidx.health.connect.client.records.ExerciseSegment
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.HeartRateSample

class HeartRateRecoveryTest {

    /** The instant effort stops. Everything is an offset from it, in seconds. */
    private val stop: Instant = Instant.parse("2026-07-14T18:30:00Z")

    private fun at(seconds: Int): Instant = stop.plusSeconds(seconds.toLong())

    private fun hr(atSeconds: Int, bpm: Long, source: String = "strap"): HeartRateSample =
        HeartRateSample(time = at(atSeconds), beatsPerMinute = bpm, source = source)

    /** A hard effort that stops dead, sampled every second. Peak 180 at -5 s, 178 at the stop, then a normal decay. */
    private fun bpmAt(seconds: Int): Long {
        if (seconds <= 0) return if (seconds == -5 || seconds == -4) 180 else 178
        val anchors = mapOf(
            0 to 178,
            10 to 170,
            30 to 155,
            60 to 145,
            120 to 130,
            180 to 120,
            240 to 115,
            300 to 110,
        )
        val keys = anchors.keys.sorted()
        for (i in 1 until keys.size) {
            val lo = keys[i - 1]
            val hi = keys[i]
            if (seconds <= hi) {
                val span = hi - lo
                val t = (seconds - lo).toDouble() / span
                return (anchors.getValue(lo) + (anchors.getValue(hi) - anchors.getValue(lo)) * t)
                    .roundToLong()
            }
        }
        return anchors.getValue(300).toLong()
    }

    private fun strapSamples(everySeconds: Int = 1): List<HeartRateSample> =
        (-60..300 step everySeconds).map { t -> hr(t, bpmAt(t)) }

    private fun calculate(
        samples: List<HeartRateSample>,
        restingHeartRate: Int? = 55,
        age: Int? = 40,
        observedMax: Int? = 190,
        explicitMax: Int? = null,
    ): HeartRateRecoveryReading =
        calculateHeartRateRecovery(
            recoveryStart = stop,
            samples = samples,
            restingHeartRateBpm = restingHeartRate,
            ageYears = age,
            observedMaxHeartRateBpm = observedMax,
            explicitMaxHeartRateBpm = explicitMax,
        )

    private fun dropAt(reading: HeartRateRecoveryReading, offset: Duration): Long? =
        reading.markAt(offset)!!.dropBpm

    private fun bpmMark(reading: HeartRateRecoveryReading, offset: Duration): Long? =
        reading.markAt(offset)!!.heartRateBpm

    private fun session(
        segments: List<ExerciseSegmentData> = emptyList(),
        duration: Duration = Duration.ofMinutes(30),
    ): ExerciseData {
        val start = stop.minus(duration)
        return ExerciseData(
            id = "w1",
            title = "Bike",
            exerciseType = 0,
            startTime = start,
            endTime = stop,
            durationMs = duration.toMillis(),
            source = "test",
            segments = segments,
        )
    }

    private fun rest(fromSeconds: Int, toSeconds: Int): ExerciseSegmentData =
        ExerciseSegmentData(
            startTime = at(fromSeconds),
            endTime = at(toSeconds),
            segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
            repetitions = 0,
        )

    // calculateHeartRateRecovery.

    @Test
    fun `the offsets are 30s to 5min, no 10s mark`() {
        assertEquals(
            listOf(
                Duration.ofSeconds(30),
                Duration.ofMinutes(1),
                Duration.ofMinutes(2),
                Duration.ofMinutes(3),
                Duration.ofMinutes(4),
                Duration.ofMinutes(5),
            ),
            heartRateRecoveryOffsets,
        )
    }

    @Test
    fun `a chest strap at 1Hz measures every mark and reads clean`() {
        val reading = calculate(strapSamples())

        assertEquals(HeartRateRecoveryQuality.CLEAN, reading.quality)
        assertTrue(reading.issues.isEmpty())
        assertEquals(180L, reading.peakBpm)
        assertEquals(10, reading.peakWindowSeconds)

        // Every mark measured, and the drop is peak minus the sample there.
        for (offset in heartRateRecoveryOffsets) {
            assertNotNull("no sample at $offset", bpmMark(reading, offset))
        }
        assertEquals(145L, bpmMark(reading, Duration.ofMinutes(1)))
        assertEquals(180L - 145L, dropAt(reading, Duration.ofMinutes(1)))
        assertEquals(35L, reading.headlineDropBpm)
        assertTrue(reading.isComparable)
    }

    @Test
    fun `a watch that samples once a minute after the workout leaves the 30s mark BLANK rather than interpolated`() {
        val samples = buildList {
            // Dense during the effort...
            for (t in -60..0 step 5) add(hr(t, bpmAt(t), source = "watch"))
            // ...then the watch reverts to a reading a minute.
            for (t in 60..300 step 60) add(hr(t, bpmAt(t), source = "watch"))
        }

        val reading = calculate(samples)

        // The 30 s mark cannot be produced from this data and is never invented.
        assertNull(bpmMark(reading, Duration.ofSeconds(30)))
        assertNull(dropAt(reading, Duration.ofSeconds(30)))

        // The one-minute mark it can, so the reading still charts.
        assertEquals(145L, bpmMark(reading, Duration.ofMinutes(1)))
        assertEquals(35L, dropAt(reading, Duration.ofMinutes(1)))
        assertEquals(110L, bpmMark(reading, Duration.ofMinutes(5)))
        assertTrue(reading.isComparable)
    }

    @Test
    fun `a watch every 5 seconds keeps all six marks`() {
        val reading = calculate(strapSamples(everySeconds = 5))

        for (offset in heartRateRecoveryOffsets) {
            assertNotNull("missing $offset", bpmMark(reading, offset))
        }
        assertEquals(6, reading.marks.size)
        assertEquals(HeartRateRecoveryQuality.CLEAN, reading.quality)
    }

    @Test
    fun `a watch that stops recording at the workout end measures nothing`() {
        val samples = (-60..0 step 5).map { t -> hr(t, bpmAt(t)) }

        val reading = calculate(samples)

        assertEquals(HeartRateRecoveryQuality.NO_DATA, reading.quality)
        assertTrue(HeartRateRecoveryIssue.NO_RECOVERY_SAMPLES in reading.issues)
        assertEquals(0, reading.recoverySampleCount)
        assertEquals(6, reading.marks.size)
        for (mark in reading.marks) {
            assertNull(mark.heartRateBpm)
            assertNull(mark.dropBpm)
        }
        assertFalse(reading.isComparable)
    }

    @Test
    fun `no samples at all is noData, not a crash`() {
        val reading = calculate(emptyList())
        assertEquals(HeartRateRecoveryQuality.NO_DATA, reading.quality)
        assertNull(reading.peakBpm)
    }

    @Test
    fun `nothing in the hard last-10s window means no peak, and noData`() {
        // Nothing in the last 40 seconds before the stop. A wider peak window would inflate the recovery;
        // the hard window refuses to measure.
        val samples = buildList {
            for (t in -60..-40 step 5) add(hr(t, if (t == -45) 180L else 176L))
            for (t in 60..300 step 60) add(hr(t, bpmAt(t)))
        }

        val reading = calculate(samples)

        assertNull(reading.peakBpm)
        assertEquals(HeartRateRecoveryQuality.NO_DATA, reading.quality)
    }

    @Test
    fun `easing off before pressing stop is caught, not rewarded`() {
        // Peak 180 at -45 s, walked down to 160 by the stop: a 20 bpm fall, well over the gate.
        val samples = buildList {
            for (t in -60..0) {
                add(hr(t, if (t <= -45) 180L else (180 - (t + 45) * 20.0 / 45).roundToLong()))
            }
            for (t in 1..300) add(hr(t, bpmAt(t)))
        }

        val reading = calculate(samples)

        assertTrue(HeartRateRecoveryIssue.COOLDOWN_BEFORE_STOP in reading.issues)
        assertEquals(HeartRateRecoveryQuality.INVALID, reading.quality)
        assertFalse(
            "an invalid reading must never reach the trend",
            reading.isComparable,
        )
    }

    @Test
    fun `a fall of just five bpm before the stop still counts as a cool-down`() {
        // The gate is 4 bpm, just above beat-to-beat noise. 176 to 171 is a 5 bpm easing-off the old 8 bpm gate missed.
        val samples = buildList {
            for (t in -60..0) add(hr(t, if (t <= -20) 176L else 171L))
            for (t in 1..300) add(hr(t, (171L - t / 6).coerceIn(120L, 171L)))
        }

        val reading = calculate(samples)

        assertTrue(HeartRateRecoveryIssue.COOLDOWN_BEFORE_STOP in reading.issues)
        assertEquals(HeartRateRecoveryQuality.INVALID, reading.quality)
    }

    @Test
    fun `a heart rate that ROSE after the stop is not a recovery`() {
        val samples = buildList {
            for (t in -180..0 step 60) add(hr(t, 113))
            add(hr(60, 115))
            add(hr(120, 117))
            add(hr(180, 115))
            add(hr(240, 116))
            add(hr(300, 94))
        }

        val reading = calculate(samples, observedMax = 130)

        assertTrue(HeartRateRecoveryIssue.HEART_RATE_DID_NOT_FALL in reading.issues)
        assertEquals(HeartRateRecoveryQuality.INVALID, reading.quality)
        assertFalse(
            "a negative recovery must never reach the trend",
            reading.isComparable,
        )
    }

    @Test
    fun `a reading with no one-minute mark cannot be charted`() {
        val samples = buildList {
            for (t in -120..0 step 60) add(hr(t, 120))
            add(hr(30, 98))
            add(hr(210, 92))
        }

        val reading = calculate(samples, observedMax = 130)

        assertEquals(98L, bpmMark(reading, Duration.ofSeconds(30)))
        assertEquals(22L, dropAt(reading, Duration.ofSeconds(30)))
        assertNull(bpmMark(reading, Duration.ofMinutes(1)))
        assertFalse(
            "no one-minute fall means no point to plot",
            reading.isComparable,
        )
    }

    @Test
    fun `a submaximal effort is shown, flagged not-comparable, never hidden`() {
        // Peak 152 against a stated max of 190 is submaximal. The drop is still measured.
        val samples = (-60..300 step 5).map { t ->
            hr(t, if (t <= 0) 152L else (152L - t / 6).coerceIn(110L, 152L))
        }

        val reading = calculate(samples)

        assertTrue(HeartRateRecoveryIssue.SUBMAXIMAL_EFFORT in reading.issues)
        assertEquals(HeartRateRecoveryQuality.NOT_COMPARABLE, reading.quality)
        // There is no separate "not vigorous" hide-gate: even a weak effort is shown.
        assertNotNull(dropAt(reading, Duration.ofMinutes(1)))
        assertFalse(reading.isComparable)
    }

    @Test
    fun `near-max is an absolute band, wider for an ESTIMATED max`() {
        // A 40-year-old's estimated max is 180. A peak of 160 is inside the 22 bpm band, so not submaximal.
        val samples = (-60..300 step 5).map { t -> hr(t, if (t <= 0) 160L else 150L) }
        val reading = calculate(samples, observedMax = null, age = 40)

        assertEquals(180, reading.maxHeartRateBpmUsed)
        assertTrue(reading.maxHeartRateEstimated)
        assertFalse(HeartRateRecoveryIssue.SUBMAXIMAL_EFFORT in reading.issues)
    }

    @Test
    fun `the same peak against a KNOWN max is submaximal (tighter band)`() {
        // Peak 160 against a measured max of 180 is beyond the 10 bpm band for a known maximum.
        val samples = (-60..300 step 5).map { t -> hr(t, if (t <= 0) 160L else 150L) }
        val reading = calculate(samples, observedMax = 180, age = 40)

        assertFalse(reading.maxHeartRateEstimated)
        assertTrue(HeartRateRecoveryIssue.SUBMAXIMAL_EFFORT in reading.issues)
        assertEquals(HeartRateRecoveryQuality.NOT_COMPARABLE, reading.quality)
    }

    @Test
    fun `an unknown max heart rate still reports every mark`() {
        val reading = calculate(
            strapSamples(),
            observedMax = null,
            age = null,
            restingHeartRate = null,
        )

        assertTrue(HeartRateRecoveryIssue.UNKNOWN_MAX_HEART_RATE in reading.issues)
        assertNull(reading.maxHeartRateBpmUsed)
        assertEquals(HeartRateRecoveryQuality.APPROXIMATE, reading.quality)
        assertEquals(35L, dropAt(reading, Duration.ofMinutes(1)))
    }

    @Test
    fun `the age formula is Tanaka (208 - 0,7 x age), flagged estimated`() {
        // 20yo: 208 - 0.7*20 = 194 (the old 220-age gave 200).
        val young = calculate(strapSamples(), observedMax = null, age = 20)
        assertEquals(194, young.maxHeartRateBpmUsed)
        assertTrue(young.maxHeartRateEstimated)

        // 40yo: 208 - 28 = 180.
        val middle = calculate(strapSamples(), observedMax = null, age = 40)
        assertEquals(180, middle.maxHeartRateBpmUsed)
    }

    @Test
    fun `an observed max below the trust bar is not used as a maximum`() {
        val reading = calculate(
            strapSamples(),
            observedMax = 140,
            restingHeartRate = 55,
            age = 40,
        )

        assertEquals(
            "the age estimate, not the untrustworthy observed 140",
            180,
            reading.maxHeartRateBpmUsed,
        )
        assertTrue(reading.maxHeartRateEstimated)
    }

    // The explicit max: BodyProfile.maxHeartRateBpm resolves first.

    @Test
    fun `an explicit max wins over a trustworthy observed max`() {
        val reading = calculate(
            strapSamples(),
            observedMax = 190,
            explicitMax = 175,
        )

        assertEquals(175, reading.maxHeartRateBpmUsed)
        assertFalse(
            "a user-stated maximum is known, not estimated",
            reading.maxHeartRateEstimated,
        )
    }

    @Test
    fun `an explicit max is used without the trustworthy check`() {
        // 140 is below the 150 bpm trust bar for an observed max, but the user stated it.
        val reading = calculate(
            strapSamples(),
            observedMax = null,
            explicitMax = 140,
        )

        assertEquals(140, reading.maxHeartRateBpmUsed)
        assertFalse(reading.maxHeartRateEstimated)
    }

    @Test
    fun `a peak 20 below an explicit max is submaximal (known band)`() {
        // Peak 160 against a stated max of 180 is beyond the 10 bpm band for a known maximum.
        val samples = (-60..300 step 5).map { t -> hr(t, if (t <= 0) 160L else 150L) }
        val reading = calculate(samples, observedMax = null, explicitMax = 180, age = 40)

        assertFalse(reading.maxHeartRateEstimated)
        assertTrue(HeartRateRecoveryIssue.SUBMAXIMAL_EFFORT in reading.issues)
        assertEquals(HeartRateRecoveryQuality.NOT_COMPARABLE, reading.quality)
    }

    @Test
    fun `two sources on the same instant collapse to the higher reading`() {
        val samples = buildList {
            addAll(strapSamples())
            for (t in -60..300) add(hr(t, bpmAt(t) - 3, source = "watch"))
        }

        val reading = calculate(samples)

        // The higher of the two is kept: the strap's 145. That reports the smaller drop.
        assertEquals(145L, bpmMark(reading, Duration.ofMinutes(1)))
    }

    @Test
    fun `samples arriving out of order are sorted, not trusted`() {
        val shuffled = strapSamples().reversed()
        val reading = calculate(shuffled)

        assertEquals(180L, reading.peakBpm)
        assertEquals(35L, dropAt(reading, Duration.ofMinutes(1)))
        assertEquals(HeartRateRecoveryQuality.CLEAN, reading.quality)
    }

    @Test
    fun `a sample exactly on the tighter 1-minute tolerance boundary counts`() {
        // The 1-minute tolerance is +-5s. The only recovery sample sits at 65s.
        val samples = buildList {
            for (t in -60..0) add(hr(t, bpmAt(t)))
            add(hr(65, 144))
        }

        val reading = calculate(samples)

        assertEquals(144L, bpmMark(reading, Duration.ofMinutes(1)))
        assertEquals(
            Duration.ofSeconds(5),
            reading.markAt(Duration.ofMinutes(1))!!.sampleSkew,
        )
    }

    @Test
    fun `a sample one second beyond the tolerance does not`() {
        val samples = buildList {
            for (t in -60..0) add(hr(t, bpmAt(t)))
            add(hr(66, 144))
        }

        val reading = calculate(samples)

        assertNull(bpmMark(reading, Duration.ofMinutes(1)))
    }

    @Test
    fun `a tie between two samples goes to the earlier, higher one`() {
        // Equidistant either side of the 2-minute mark (+-5s tolerance).
        val samples = buildList {
            for (t in -60..0) add(hr(t, bpmAt(t)))
            add(hr(60, 145))
            add(hr(118, 133))
            add(hr(122, 128))
        }

        val reading = calculate(samples)

        // 118 and 122 are both 2 s from 120. The earlier wins: on a falling curve it reports the smaller drop.
        assertEquals(133L, bpmMark(reading, Duration.ofMinutes(2)))
    }

    // heartRateRecoveryWindowFor.

    @Test
    fun `a session with no rest segment has no recovery window`() {
        // An ordinary workout gives no guarantee effort ceased, so its end is not a stop.
        assertNull(heartRateRecoveryWindowFor(session()))
    }

    @Test
    fun `a qualifying trailing rest segment is the moment effort stopped`() {
        val window = heartRateRecoveryWindowFor(
            session(segments = listOf(rest(-300, 0))),
        )

        assertNotNull(window)
        assertEquals(at(-300), window!!.recoveryStart)
        // Reads a minute back for the peak, and past the last mark for the tail.
        assertEquals(at(-360), window.readStart)
        assertEquals(at(30), window.readEnd)
    }

    @Test
    fun `the rest segment after the last set of a strength workout is NOT a recovery`() {
        // A 60 s breather after a set is too short to qualify.
        assertNull(
            heartRateRecoveryWindowFor(
                session(segments = listOf(rest(-600, -540), rest(-60, 0))),
            ),
        )
    }

    @Test
    fun `a long rest that is not at the end is not a recovery either`() {
        // Five minutes of rest, but the session ran on for four more minutes afterwards.
        assertNull(
            heartRateRecoveryWindowFor(
                session(segments = listOf(rest(-540, -240))),
            ),
        )
    }

    @Test
    fun `a rest ending just shy of the session end still qualifies`() {
        val window = heartRateRecoveryWindowFor(
            session(segments = listOf(rest(-300, -20))),
        )

        assertNotNull(window)
        assertEquals(at(-300), window!!.recoveryStart)
    }
}
