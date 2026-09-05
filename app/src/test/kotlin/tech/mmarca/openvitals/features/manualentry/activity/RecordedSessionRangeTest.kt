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
 * The session written to Health Connect has to contain the samples it carries. The form
 * rebuilds the range from minute-granularity text, so the end can land before the last
 * sample. Health Connect clamps such samples onto the closing instant, which ruins a recovery.
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
        // The recording ran 10:00:59 -> 10:02:59, rebuilt from text as 10:00 -> 10:02.
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
        // The start is the user's. A sample before it would be clamped onto it, inventing a reading.
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
