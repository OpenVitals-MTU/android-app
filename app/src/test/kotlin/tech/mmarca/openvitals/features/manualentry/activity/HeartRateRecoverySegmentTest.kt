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
 * The guided test writes the moment effort stopped as a trailing REST segment; the reader
 * finds it again. If the two disagree, the guided test degrades to an ordinary workout.
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
        // To the end of the session, not a fixed five minutes, or a slow save strands the segment mid-session.
        assertEquals(request.endTime, rest.single().endTime)

        // No "active" segment: Health Connect throws on a type mismatch. Rest and pause are universal.
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
        // Explicit segments suppress the ones the writer synthesizes from pauses, so they are carried by hand.
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
        // An ordinary recording carries no abrupt-stop mark, so it is not measured.
        assertNull(heartRateRecoveryWindowFor(readBack(request)))
    }
}
