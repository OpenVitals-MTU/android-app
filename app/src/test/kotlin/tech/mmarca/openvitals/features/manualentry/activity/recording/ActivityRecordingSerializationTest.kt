package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.nio.file.Files
import java.time.Duration
import java.time.Instant
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.model.ActivityPauseInterval
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint

/** Metadata goes to SharedPreferences and route points to a file, so the round-trip drives both halves of [ActivityRecordingStore]. */
class ActivityRecordingSerializationTest {

    private lateinit var filesDir: File
    private lateinit var store: ActivityRecordingStore
    private lateinit var preferences: FakeSharedPreferences

    @Before fun setUp() {
        filesDir = Files.createTempDirectory("activity-recording-store").toFile()
        val context = mockk<Context>()
        preferences = FakeSharedPreferences()
        every { context.getSharedPreferences(any(), any()) } returns preferences
        every { context.filesDir } returns filesDir
        store = ActivityRecordingStore(context)
    }

    @After fun tearDown() {
        filesDir.deleteRecursively()
    }

    @Test fun `recording state survives a SharedPreferences round-trip`() {
        val start = Instant.parse("2026-05-26T08:30:00Z")
        val state = ActivityRecordingState(
            status = ActivityRecordingStatus.RECORDING,
            recordingKind = ActivityRecordingKind.GPS_ROUTE,
            activityTypeId = "push_ups",
            exerciseType = 12,
            startTime = start,
            totalPausedMillis = 5_000L,
            pauseIntervals = listOf(
                ActivityPauseInterval(
                    startTime = start.plus(Duration.ofSeconds(60)),
                    endTime = start.plus(Duration.ofSeconds(90)),
                ),
            ),
            points = listOf(
                ExerciseRoutePoint(
                    time = start,
                    latitude = 59.0,
                    longitude = 24.0,
                    altitudeMeters = 10.0,
                    horizontalAccuracyMeters = 5.0,
                    verticalAccuracyMeters = null,
                ),
                ExerciseRoutePoint(
                    time = start.plus(Duration.ofSeconds(60)),
                    latitude = 59.001,
                    longitude = 24.002,
                    altitudeMeters = 18.0,
                    horizontalAccuracyMeters = 4.0,
                    verticalAccuracyMeters = 6.0,
                ),
            ),
            routeBreakIndexes = listOf(1),
            markers = listOf(
                ActivityRecordingMarker(
                    id = "marker-1",
                    time = start.plus(Duration.ofSeconds(30)),
                    latitude = 59.0005,
                    longitude = 24.001,
                    altitudeMeters = 12.0,
                    name = "Water stop",
                    note = "refill",
                ),
            ),
            distanceMeters = 111.2,
            elevationGainedMeters = 8.0,
            repetitionCount = 4L,
            repetitionSets = listOf(
                ActivityRecordedRepetitionSet(repetitions = 8, restSeconds = 60, activeMillis = 120_000),
            ),
            lastAccuracyMeters = 4.0,
            lastLocationTime = start.plus(Duration.ofSeconds(60)),
        )

        store.storeMetadata(state)
        store.replaceRoutePoints(state.points)
        val restored = store.restore()

        assertEquals(ActivityRecordingStatus.RECORDING, restored.status)
        assertEquals("push_ups", restored.activityTypeId)
        assertEquals(12, restored.exerciseType)
        assertEquals(start, restored.startTime)
        assertEquals(5_000L, restored.totalPausedMillis)
        assertEquals(1, restored.pauseIntervals.size)
        assertEquals(2, restored.points.size)
        assertEquals(59.001, restored.points[1].latitude, 1e-9)
        assertEquals(6.0, restored.points[1].verticalAccuracyMeters!!, 1e-9)
        assertEquals(listOf(1), restored.routeBreakIndexes)
        assertEquals(1, restored.markers.size)
        assertEquals("Water stop", restored.markers.first().name)
        assertEquals("refill", restored.markers.first().note)
        assertEquals(111.2, restored.distanceMeters, 1e-4)
        assertEquals(8.0, restored.elevationGainedMeters, 1e-6)
        assertEquals(4L, restored.repetitionCount)
        assertEquals(60L, restored.repetitionSets.single().restSeconds)
        assertEquals(4.0, restored.lastAccuracyMeters!!, 1e-6)
    }

    @Test fun `idle state clears persisted recording keys`() {
        store.storeMetadata(
            ActivityRecordingState(
                status = ActivityRecordingStatus.RECORDING,
                activityTypeId = "run",
            ),
        )
        assertEquals(ActivityRecordingStatus.RECORDING, store.restore().status)

        store.storeMetadata(ActivityRecordingState())

        assertEquals(ActivityRecordingStatus.IDLE, store.restore().status)
        assertTrue(store.restore().activityTypeId == null)
    }

    @Test fun `a plan run round-trips its steps, cursor and typed sets`() {
        val state = ActivityRecordingState(
            status = ActivityRecordingStatus.RESTING,
            recordingKind = ActivityRecordingKind.REPETITION,
            activityTypeId = "calisthenics",
            exerciseType = 12,
            startTime = Instant.parse("2026-08-26T17:00:00Z"),
            restStartedAt = Instant.parse("2026-08-26T17:02:00Z"),
            repetitionRestSeconds = 30L,
            planId = "plan-1",
            planTitle = "Strength, day 1",
            planStepIndex = 1,
            planSteps = listOf(
                ActivityPlanRunStep(
                    segmentType = 1,
                    label = "Push-ups, wide",
                    goalKind = ActivityPlanGoalKind.REPS,
                    goalValue = 10L,
                    restSeconds = 30L,
                    blockIndex = 0,
                    round = 1,
                    rounds = 2,
                    sensorTypeId = "push_ups",
                ),
                ActivityPlanRunStep(
                    segmentType = 44,
                    label = "Plank",
                    goalKind = ActivityPlanGoalKind.SECONDS,
                    goalValue = 45L,
                    restSeconds = 0L,
                    blockIndex = 1,
                    round = 1,
                    rounds = 1,
                    sensorTypeId = null,
                ),
            ),
            repetitionSets = listOf(
                ActivityRecordedRepetitionSet(
                    repetitions = 10L,
                    restSeconds = 30L,
                    activeMillis = 25_000L,
                    segmentType = 1,
                    label = "Push-ups, wide",
                ),
                ActivityRecordedRepetitionSet(
                    repetitions = 0L,
                    restSeconds = 0L,
                    activeMillis = 45_000L,
                    segmentType = 44,
                    label = "Plank",
                    isDuration = true,
                ),
            ),
        )

        store.storeMetadata(state)
        val restored = store.restore()

        assertEquals(state.planId, restored.planId)
        assertEquals(state.planTitle, restored.planTitle)
        assertEquals(state.planStepIndex, restored.planStepIndex)
        assertEquals(state.planSteps, restored.planSteps)
        assertEquals(state.repetitionSets, restored.repetitionSets)
    }

    @Test fun `repetition sets written before plan runs still decode`() {
        store.storeMetadata(
            ActivityRecordingState(
                status = ActivityRecordingStatus.RECORDING,
                recordingKind = ActivityRecordingKind.REPETITION,
                startTime = Instant.parse("2026-08-26T17:00:00Z"),
            ),
        )
        preferences.edit().putString(KeyRepetitionSets, "8,60,30000\n6,0,20000").apply()

        val restored = store.restore()

        assertEquals(
            listOf(
                ActivityRecordedRepetitionSet(repetitions = 8L, restSeconds = 60L, activeMillis = 30_000L),
                ActivityRecordedRepetitionSet(repetitions = 6L, restSeconds = 0L, activeMillis = 20_000L),
            ),
            restored.repetitionSets,
        )
    }
}
