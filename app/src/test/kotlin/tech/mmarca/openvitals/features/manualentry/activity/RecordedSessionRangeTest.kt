package tech.mmarca.openvitals.features.manualentry.activity

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.model.BleHeartRateSample
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.preferences.UnitSystem

/**
 * The session written to Health Connect has to CONTAIN the samples it carries.
 *
 * A recording reaches the entry form as TEXT, at minute granularity: the start
 * time loses its seconds, and the duration is rounded up to a whole minute.
 * The write path then rebuilds the session range from that text — so the
 * rebuilt end can land before the last sample that was actually recorded.
 *
 * Health Connect does not drop a sample that falls outside its session. It
 * CLAMPS it to the session bounds. So the samples past the end are not lost,
 * they are stacked onto the closing instant, all sharing one timestamp —
 * which is worse than losing them. For a heart-rate recovery, computed from
 * precisely those last samples, it is everything.
 */
class RecordedSessionRangeTest {

    private val zone = ZoneId.systemDefault()
    private val start: Instant =
        LocalDateTime.of(2026, 7, 14, 10, 0).atZone(zone).toInstant()

    private fun heartRate(samples: List<Pair<Instant, Long>>) = BleRecordingSampleBuffer(
        heartRateSamples = samples.map { (time, bpm) ->
            BleHeartRateSample(time = time, beatsPerMinute = bpm)
        },
    )

    private fun recorded(bleSamples: BleRecordingSampleBuffer) = ActivityEntryUiState(
        selectedActivityType = DefaultActivityEntryTypes.first(),
        startDateText = "2026-07-14",
        startTimeText = "10:00",
        durationMinutesText = "2",
        isRecordingDraft = true,
        recordedBleSamples = bleSamples,
    )

    @Test
    fun `the end is stretched to cover the last sample`() {
        // The recording really ran 10:00:59 -> 10:02:59, but the form's text
        // rebuilt it as 10:00 -> 10:02. The last minute of samples would be
        // clamped onto 10:02:00 without the stretch.
        val lastSample = start.plusSeconds(179)
        val request = buildWriteRequest(
            recorded(heartRate(listOf(start.plusSeconds(60) to 120L, lastSample to 150L))),
            ActivityEntryUnits.uniform(UnitSystem.METRIC),
        )

        assertTrue(request!!.endTime.isAfter(lastSample))
        assertEquals(lastSample.plusSeconds(1), request.endTime)
    }

    @Test
    fun `samples before the start are dropped rather than clamped onto it`() {
        // The start is the user's: they may have moved it forward on purpose.
        // A sample before it would be clamped ONTO it, inventing a reading at
        // a time it was never taken — better to write no heart rate than a
        // fiction.
        val request = buildWriteRequest(
            recorded(heartRate(listOf(start.minusSeconds(30) to 110L, start.plusSeconds(30) to 120L))),
            ActivityEntryUnits.uniform(UnitSystem.METRIC),
        )

        assertTrue(request!!.bleSamples.isEmpty())
    }

    @Test
    fun `a session already containing its samples is untouched`() {
        val request = buildWriteRequest(
            recorded(heartRate(listOf(start.plusSeconds(30) to 120L, start.plusSeconds(60) to 130L))),
            ActivityEntryUnits.uniform(UnitSystem.METRIC),
        )

        assertEquals(start, request!!.startTime)
        assertEquals(start.plusSeconds(120), request.endTime)
        assertEquals(2, request.bleSamples.heartRateSamples.size)
    }

    @Test
    fun `sample span helpers report the earliest and latest across series`() {
        val buffer = heartRate(
            listOf(start.plusSeconds(10) to 100L, start.plusSeconds(50) to 130L),
        )
        assertEquals(start.plusSeconds(10), buffer.firstSampleTime())
        assertEquals(start.plusSeconds(50), buffer.lastSampleTime())
        assertEquals(null, BleRecordingSampleBuffer().firstSampleTime())
    }
}
