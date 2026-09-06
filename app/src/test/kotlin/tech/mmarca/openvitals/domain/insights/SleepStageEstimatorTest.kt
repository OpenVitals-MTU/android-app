package tech.mmarca.openvitals.domain.insights

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import kotlin.math.PI
import kotlin.math.sin
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.SleepMinute
import tech.mmarca.openvitals.domain.model.SleepMinuteKind

class SleepStageEstimatorTest {

    private val zone: ZoneOffset = ZoneOffset.ofHours(2)
    private val bedtime: Instant = LocalDate.of(2024, 3, 4).atTime(22, 30).toInstant(zone)

    // Generators. Each yields minutes in order; `sequence` stitches them from a start.

    private fun still(count: Int, heartRate: (Int) -> Float = { 52f }): List<(Instant) -> SleepMinute> =
        List(count) { index -> { at -> SleepMinute(at, SleepMinuteKind.RAW, movement = 0f, heartRate = heartRate(index)) } }

    private fun restless(count: Int, heartRate: Float = 64f): List<(Instant) -> SleepMinute> =
        List(count) { index ->
            { at -> SleepMinute(at, SleepMinuteKind.RAW, movement = 8f + (index * 5) % 8, heartRate = heartRate) }
        }

    private fun awakeCodes(count: Int): List<(Instant) -> SleepMinute> =
        List(count) { { at -> SleepMinute(at, SleepMinuteKind.AWAKE) } }

    private fun gap(count: Int): List<(Instant) -> SleepMinute> =
        List(count) { { at -> SleepMinute(at, SleepMinuteKind.UNMEASURABLE) } }

    private fun sequence(start: Instant, vararg parts: List<(Instant) -> SleepMinute>): List<SleepMinute> =
        parts.flatMap { it }.mapIndexed { index, make -> make(start.plusSeconds(60L * index)) }

    /** A night's heart rate: settles over the first hour, then dips every 90 minutes. */
    private fun nightHeartRate(index: Int): Float {
        val settling = if (index < 60) (60 - index) / 8f else 0f
        val cycle = 4f * sin(2 * PI * index / 90.0).toFloat()
        val jitter = ((index * 7) % 3 - 1) * 0.5f
        return 52f + settling + cycle + jitter
    }

    /** Thirty restless minutes, the sleep, then an hour of restlessness so the night can end. */
    private fun night(sleepMinutes: Int = 450): List<(Instant) -> SleepMinute> =
        restless(30) + still(sleepMinutes, ::nightHeartRate) + restless(60)

    private fun minutesBetween(a: Instant, b: Instant): Long = Duration.between(a, b).toMinutes()

    /** Onset trails the first still minute by the smoothing window and the Webster carry-over, up to 15 minutes. */
    private fun assertWithin(expected: Instant, actual: Instant, minutes: Long) {
        val delta = minutesBetween(expected, actual)
        assertTrue("expected $expected within $minutes min, was $actual", delta in -minutes..minutes)
    }

    private fun EstimatedSleepSession.share(stage: EstimatedStage): Float =
        stages.filter { it.stage == stage }.sumOf { minutesBetween(it.start, it.end) }.toFloat() / sleepMinutes

    @Test
    fun `a pure night is one session with plausible stages`() {
        val session = SleepStageEstimator.estimate(sequence(bedtime, night()))

        assertNotNull(session)
        session!!
        assertWithin(bedtime.plusSeconds(30 * 60), session.onset, 20)
        assertWithin(bedtime.plusSeconds(480 * 60), session.end, 5)
        assertEquals(0, session.awakeMinutes)
        assertTrue("sleep ${session.sleepMinutes}", session.sleepMinutes in 425..450)
        assertFalse(session.inProgress)
        assertTrue("deep ${session.share(EstimatedStage.DEEP)}", session.share(EstimatedStage.DEEP) in 0.10f..0.25f)
        assertTrue("rem ${session.share(EstimatedStage.REM)}", session.share(EstimatedStage.REM) in 0.15f..0.30f)
        assertTrue(
            session.stages.none { it.stage == EstimatedStage.REM && minutesBetween(session.onset, it.start) < 60 },
        )
        assertTrue(
            session.stages
                .filter { it.stage == EstimatedStage.DEEP || it.stage == EstimatedStage.REM }
                .all { minutesBetween(it.start, it.end) >= 5 },
        )
        // Spans tile the window without gaps.
        session.stages.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start) }
    }

    @Test
    fun `an awakening stays awake and re-enters through light sleep`() {
        val minutes = sequence(bedtime, restless(30), still(200, ::nightHeartRate), awakeCodes(20), still(250, ::nightHeartRate), restless(30))

        val session = SleepStageEstimator.estimate(minutes)!!

        assertTrue("awake ${session.awakeMinutes}", session.awakeMinutes in 20..40)
        val awake = session.stages.first { it.stage == EstimatedStage.AWAKE }
        val afterWake = session.stages.filter { it.start >= awake.end && minutesBetween(awake.end, it.start) < 10 }
        assertTrue(afterWake.none { it.stage == EstimatedStage.DEEP })
        session.stages.zipWithNext().forEach { (a, b) -> assertEquals(a.end, b.start) }
    }

    @Test
    fun `a daytime sedentary run is not a night`() {
        val parts = List(8) { still(8) { 58f } + restless(4) }.flatten()

        assertNull(SleepStageEstimator.estimate(sequence(bedtime, parts)))
    }

    @Test
    fun `an evening couch pocket does not pull the onset forward`() {
        // The pocket, ninety restless minutes, then a night whose stillness starts at bedtime.
        val minutes = sequence(bedtime.minusSeconds(160 * 60), still(40) { 60f }, restless(90), night())

        val session = SleepStageEstimator.estimate(minutes)!!

        assertWithin(bedtime, session.onset, 20)
    }

    @Test
    fun `a long not-worn gap splits the night and the longer half wins`() {
        val minutes = sequence(bedtime, restless(30), still(240, ::nightHeartRate), gap(60), still(300, ::nightHeartRate), restless(30))

        val session = SleepStageEstimator.estimate(minutes)!!

        assertWithin(bedtime.plusSeconds((30 + 240 + 60) * 60L), session.onset, 20)
        assertTrue("sleep ${session.sleepMinutes}", session.sleepMinutes in 280..300)
        assertEquals(0, session.unknownMinutes)
    }

    @Test
    fun `a short not-worn gap stays inside the night as unknown`() {
        val minutes = sequence(bedtime, restless(30), still(240, ::nightHeartRate), gap(20), still(200, ::nightHeartRate), restless(30))

        val session = SleepStageEstimator.estimate(minutes)!!

        assertWithin(bedtime.plusSeconds(30 * 60), session.onset, 20)
        assertEquals(20, session.unknownMinutes)
        val unknown = session.stages.single { it.stage == EstimatedStage.UNKNOWN }
        assertEquals(20, minutesBetween(unknown.start, unknown.end))
    }

    @Test
    fun `a night still being recorded is in progress until 45 minutes of not sleeping`() {
        val partial = sequence(bedtime, restless(30), still(200, ::nightHeartRate))
        val session = SleepStageEstimator.estimate(partial)!!
        assertTrue(session.inProgress)

        val finished = partial + sequence(bedtime.plusSeconds(230 * 60), awakeCodes(45))
        val later = SleepStageEstimator.estimate(finished)!!
        assertFalse(later.inProgress)
        assertEquals(session.onset, later.onset)
    }

    @Test
    fun `too little sleep is no night`() {
        assertNull(SleepStageEstimator.estimate(sequence(bedtime, still(150, ::nightHeartRate))))
    }

    @Test
    fun `order and duplicates do not change the result`() {
        val minutes = sequence(bedtime, night())
        val reference = SleepStageEstimator.estimate(minutes)

        val shuffled = SleepStageEstimator.estimate(minutes.shuffled(kotlin.random.Random(7)))
        val duplicated = SleepStageEstimator.estimate(minutes.take(100) + minutes)

        assertEquals(reference, shuffled)
        assertEquals(reference, duplicated)
    }

    @Test
    fun `a night slice gives the same answer as the whole stream`() {
        val eveningBefore = bedtime.minusSeconds(24 * 3600)
        // Two nights: one the evening before, one whose stillness starts at bedtime.
        val stream = sequence(eveningBefore, night(), awakeCodes(60)) +
            sequence(bedtime.minusSeconds(150 * 60), restless(120), night(), awakeCodes(60))
        val nightDate = SleepStageEstimator.nightDateOf(bedtime, zone)!!
        val slice = stream.filter { SleepStageEstimator.nightDateOf(it.time, zone) == nightDate }

        val fromSlice = SleepStageEstimator.estimate(slice)!!
        val fromStream = SleepStageEstimator.estimate(stream)!!
        assertWithin(bedtime, fromSlice.onset, 20)
        // The whole stream holds two equal nights; the slice isolates the second one.
        assertEquals(fromSlice.sleepMinutes, fromStream.sleepMinutes)
        assertTrue("sleep ${fromSlice.sleepMinutes}", fromSlice.sleepMinutes in 425..450)
    }

    @Test
    fun `a flat heart rate still lands in range through the sanity loop`() {
        val session = SleepStageEstimator.estimate(sequence(bedtime, restless(30), still(450) { 55f }, restless(30)))!!

        assertNotEquals(0.6f, session.deepThreshold)
        assertNotEquals(0.6f, session.remThreshold)
        assertTrue(session.share(EstimatedStage.DEEP) in 0.10f..0.25f)
        assertTrue(session.share(EstimatedStage.REM) in 0.15f..0.30f)
    }

    @Test
    fun `night dates follow the 18 to 14 window`() {
        val date = LocalDate.of(2024, 3, 4)
        assertEquals(date.plusDays(1), SleepStageEstimator.nightDateOf(date.atTime(18, 0).toInstant(zone), zone))
        assertEquals(date, SleepStageEstimator.nightDateOf(date.atTime(13, 59).toInstant(zone), zone))
        assertNull(SleepStageEstimator.nightDateOf(date.atTime(14, 0).toInstant(zone), zone))
        assertNull(SleepStageEstimator.nightDateOf(date.atTime(17, 59).toInstant(zone), zone))
    }

    // The derived Venu SQ fixture: four days of real-shaped data, values rounded and dates shifted.

    private fun fixtureMinutes(): Pair<List<SleepMinute>, ZoneOffset> {
        val text = requireNotNull(javaClass.getResourceAsStream("/fit/sleep/venu_sq_minutes.json")) {
            "fixture missing"
        }.use { it.readBytes().decodeToString() }
        val root = JSONObject(text)
        val origin = root.getLong("originEpochSeconds")
        val step = root.getLong("minuteSeconds")
        val rows = root.getJSONArray("minutes")
        var offset = ZoneOffset.UTC
        val minutes = List(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            offset = ZoneOffset.ofTotalSeconds(row.getInt("z"))
            val at = Instant.ofEpochSecond(origin + step * row.getLong("t"))
            when (row.getString("k")) {
                "R" -> SleepMinute(
                    at,
                    SleepMinuteKind.RAW,
                    movement = row.optDouble("mv", 0.0).toFloat(),
                    heartRate = if (row.isNull("hr")) null else row.getDouble("hr").toFloat(),
                )
                "A" -> SleepMinute(at, SleepMinuteKind.AWAKE)
                else -> SleepMinute(at, SleepMinuteKind.UNMEASURABLE)
            }
        }
        return minutes to offset
    }

    @Test
    fun `the Venu SQ fixture yields exactly its three nights`() {
        val (minutes, offset) = fixtureMinutes()
        val byNight = minutes.groupBy { SleepStageEstimator.nightDateOf(it.time, offset) }.filterKeys { it != null }

        val sessions = byNight.mapNotNull { (night, slice) ->
            SleepStageEstimator.estimate(slice)?.let { night!! to it }
        }.sortedBy { it.first }

        assertEquals(sessions.joinToString { "${it.first}: ${it.second.onset}→${it.second.end}" }, 3, sessions.size)
        for ((night, session) in sessions) {
            val onset = session.onset.atOffset(offset).toLocalTime()
            val end = session.end.atOffset(offset).toLocalDateTime()
            assertTrue("$night onset $onset", onset >= LocalTime.of(21, 0) || onset <= LocalTime.of(0, 30))
            assertEquals(night, end.toLocalDate())
            assertTrue("$night end $end", end.toLocalTime() in LocalTime.of(5, 0)..LocalTime.of(9, 0))
            assertTrue("$night sleep ${session.sleepMinutes}", session.sleepMinutes >= 360)
            assertTrue("$night deep ${session.share(EstimatedStage.DEEP)}", session.share(EstimatedStage.DEEP) in 0.10f..0.25f)
            assertTrue("$night rem ${session.share(EstimatedStage.REM)}", session.share(EstimatedStage.REM) in 0.15f..0.30f)
            assertFalse(session.inProgress)
        }
    }

    @Test
    fun `the fixture's daytime hours hold no night`() {
        val (minutes, offset) = fixtureMinutes()
        val daytime = minutes.filter { it.time.atOffset(offset).hour in 7 until 18 }
        val byDay = daytime.groupBy { it.time.atOffset(offset).toLocalDate() }

        for ((day, slice) in byDay) {
            assertNull("$day", SleepStageEstimator.estimate(slice))
        }
    }
}
