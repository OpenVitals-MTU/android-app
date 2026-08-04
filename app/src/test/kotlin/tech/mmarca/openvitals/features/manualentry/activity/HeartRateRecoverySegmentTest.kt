package tech.mmarca.openvitals.features.manualentry.activity

import androidx.health.connect.client.records.ExerciseSegment
import java.time.LocalDateTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.heartRateRecoveryWindowFor
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ActivityWriteRequest
import tech.mmarca.openvitals.domain.model.BleHeartRateSample
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import java.time.Instant

/**
 * The guided test writes the moment effort stopped; the reader finds it again.
 *
 * These two halves are the feature. The recording knows the instant of
 * cessation and nothing else does — Health Connect has no field for it, so it
 * goes in as a trailing REST segment. If the segment the writer produces is
 * not one the reader accepts, the guided test silently degrades to an ordinary
 * workout — which is then not measured at all — and nobody would notice until
 * the recovery went missing.
 */
class HeartRateRecoverySegmentTest {

    private val zone = ZoneId.systemDefault()
    private val start: Instant =
        LocalDateTime.of(2026, 7, 14, 10, 0).atZone(zone).toInstant()
    private val effortEnded: Instant =
        LocalDateTime.of(2026, 7, 14, 10, 20).atZone(zone).toInstant()

    private fun recordedTest(
        recoveryStart: Instant?,
        pauses: List<ActivityPauseInterval> = emptyList(),
    ): ActivityEntryUiState = ActivityEntryUiState(
        selectedActivityType = DefaultActivityEntryTypes.first(),
        startDateText = "2026-07-14",
        startTimeText = "10:00",
        // Effort for 20 minutes, then 5 minutes of recovery.
        durationMinutesText = "25",
        isRecordingDraft = true,
        recordedRecoveryStartTime = recoveryStart,
        recordedPauseIntervals = pauses,
        recordedBleSamples = BleRecordingSampleBuffer(
            heartRateSamples = listOf(
                BleHeartRateSample(time = start, beatsPerMinute = 120),
                BleHeartRateSample(time = effortEnded, beatsPerMinute = 178),
            ),
        ),
    )

    /** The session as it comes back OUT of Health Connect, built from what went in. */
    private fun readBack(request: ActivityWriteRequest): ExerciseData = ExerciseData(
        id = "w1",
        title = request.title,
        exerciseType = request.exerciseType,
        startTime = request.startTime,
        endTime = request.endTime,
        durationMs = request.endTime.toEpochMilli() - request.startTime.toEpochMilli(),
        source = "openvitals",
        segments = request.exerciseSegments.map { segment ->
            ExerciseSegmentData(
                startTime = segment.startTime,
                endTime = segment.endTime,
                segmentType = segment.segmentType,
                repetitions = segment.repetitions,
            )
        },
    )

    @Test
    fun `the recovery is written as a trailing rest segment and read back`() {
        val request = buildWriteRequest(recordedTest(recoveryStart = effortEnded), ActivityEntryUnits.uniform(UnitSystem.METRIC))
        assertNotNull(request)

        val rest = request!!.exerciseSegments
            .filter { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST }
        assertEquals(1, rest.size)
        assertEquals(effortEnded, rest.single().startTime)
        // To the END of the session, not for a fixed five minutes: a rider who
        // takes a while to press save must not leave the segment stranded
        // mid-session, where the reader would ignore it and measure from the
        // session end instead.
        assertEquals(request.endTime, rest.single().endTime)

        // No "active" segment. Health Connect validates a segment's type
        // against the session's exercise type and THROWS on a mismatch; rest
        // and pause are the two universal types.
        assertTrue(
            request.exerciseSegments.all {
                it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST ||
                    it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE
            },
        )

        // And now the half that matters: the reader finds it.
        val window = heartRateRecoveryWindowFor(readBack(request))
        assertNotNull(window)
        assertEquals(effortEnded, window!!.recoveryStart)
    }

    @Test
    fun `pauses during the effort survive alongside the recovery mark`() {
        // Explicit segments suppress the ones the native writer would
        // synthesize from the pause intervals, so they have to be carried
        // through by hand or they vanish.
        val pauseStart = LocalDateTime.of(2026, 7, 14, 10, 5).atZone(zone).toInstant()
        val pauseEnd = LocalDateTime.of(2026, 7, 14, 10, 7).atZone(zone).toInstant()
        val request = buildWriteRequest(
            recordedTest(
                recoveryStart = effortEnded,
                pauses = listOf(ActivityPauseInterval(startTime = pauseStart, endTime = pauseEnd)),
            ),
            ActivityEntryUnits.uniform(UnitSystem.METRIC),
        )

        val pauses = request!!.exerciseSegments
            .filter { it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_PAUSE }
        assertEquals(1, pauses.size)
        assertEquals(pauseStart, pauses.single().startTime)
    }

    @Test
    fun `an ordinary recording gets no recovery mark at all`() {
        val request = buildWriteRequest(recordedTest(recoveryStart = null), ActivityEntryUnits.uniform(UnitSystem.METRIC))

        assertTrue(
            request!!.exerciseSegments.none {
                it.segmentType == ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST
            },
        )
        // And the reader has no recovery window at all — an ordinary recording
        // carries no abrupt-stop mark, so it is not measured.
        assertNull(heartRateRecoveryWindowFor(readBack(request)))
    }
}
