package tech.mmarca.openvitals.features.manualentry.activity.recording

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.PlannedExerciseStep
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.devices.FakeSharedPreferences
import tech.mmarca.openvitals.domain.model.BleRecordingMetrics
import tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer
import tech.mmarca.openvitals.domain.model.PlannedExerciseBlockData
import tech.mmarca.openvitals.domain.model.PlannedExerciseCompletion
import tech.mmarca.openvitals.domain.model.PlannedExerciseData
import tech.mmarca.openvitals.domain.model.PlannedExerciseStepData
import tech.mmarca.openvitals.domain.model.restPlanStep
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingPreferences
import tech.mmarca.openvitals.features.manualentry.activity.DefaultActivityEntryTypes
import tech.mmarca.openvitals.sensors.ble.BleSensorCoordinator
import tech.mmarca.openvitals.util.MainDispatcherRule

/** The plan cursor inside a repetition recording: rep steps end at their target, timed steps on a deadline. */
@OptIn(ExperimentalCoroutinesApi::class)
class ActivityRecordingPlanRunTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val calisthenics = DefaultActivityEntryTypes.single { it.id == "calisthenics" }

    private lateinit var filesDir: File
    private lateinit var preferences: FakeSharedPreferences
    private lateinit var context: Context
    private lateinit var preferencesRepository: PreferencesRepository

    @Before fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0L
        mockkStatic(ContextCompat::class)
        every { ContextCompat.startForegroundService(any(), any()) } returns Unit
        mockkObject(ActivityRecordingService.Companion)
        every { ActivityRecordingService.intent(any(), any()) } returns mockk(relaxed = true)
        mockkConstructor(Intent::class)

        filesDir = Files.createTempDirectory("activity-recording-plan").toFile()
        preferences = FakeSharedPreferences()
        context = mockk(relaxed = true)
        every { context.getSharedPreferences(any(), any()) } returns preferences
        every { context.filesDir } returns filesDir
        every { context.getString(any()) } returns "recording error"
        every { context.getString(any(), *anyVararg()) } returns "cue"
        preferencesRepository = mockk()
        every { preferencesRepository.activityRecordingPreferences() } returns ActivityRecordingPreferences()
        every { preferencesRepository.activityRecordingDashboardLayout(any()) } returns
            ActivityRecordingDashboardLayout()
    }

    @After fun tearDown() {
        filesDir.deleteRecursively()
        unmockkAll()
    }

    private fun controller() = ActivityRecordingController(
        context = context,
        preferencesRepository = preferencesRepository,
        bleSensorCoordinator = mockk<BleSensorCoordinator>(relaxed = true).also { coordinator ->
            every { coordinator.metrics } returns MutableStateFlow(BleRecordingMetrics())
            every { coordinator.stopRecording() } returns BleRecordingSampleBuffer()
        },
        coMapsNavigationRepository = mockk(relaxed = true),
        coMapsGuidanceFeed = tech.mmarca.openvitals.comaps.CoMapsGuidanceFeed(mockk(relaxed = true)),
        recordingStore = ActivityRecordingStore(context),
    )

    private fun plan(): PlannedExerciseData = PlannedExerciseData(
        id = "plan-1",
        title = "Strength",
        exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_CALISTHENICS,
        startTime = Instant.parse("2026-08-26T17:00:00Z"),
        endTime = Instant.parse("2026-08-26T17:20:00Z"),
        hasExplicitTime = true,
        completedExerciseSessionId = null,
        notes = null,
        blockCount = 2,
        source = "tech.mmarca.openvitals",
        blocks = listOf(
            PlannedExerciseBlockData(
                repetitions = 2,
                description = "Push-ups",
                steps = listOf(
                    PlannedExerciseStepData(
                        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT,
                        exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                        description = "Push-ups",
                        completion = PlannedExerciseCompletion.Repetitions(3),
                    ),
                    restPlanStep(30),
                ),
            ),
            PlannedExerciseBlockData(
                repetitions = 1,
                description = null,
                steps = listOf(
                    PlannedExerciseStepData(
                        exerciseType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK,
                        exercisePhase = PlannedExerciseStep.EXERCISE_PHASE_ACTIVE,
                        description = null,
                        completion = PlannedExerciseCompletion.DurationSeconds(45),
                    ),
                ),
            ),
        ),
    )

    @Test fun `a rep step ends at its target and rests for its own seconds`() {
        val recorder = controller()
        assertTrue(recorder.startPlanRecording(plan(), calisthenics))
        val started = recorder.state.value
        assertEquals(ActivityRecordingKind.REPETITION, started.recordingKind)
        assertEquals("plan-1", started.planId)
        assertEquals(3, started.planSteps.size)
        assertEquals(0, started.planStepIndex)
        assertEquals(30L, started.repetitionRestSeconds)

        repeat(2) { recorder.acceptRecognizedRepetition() }
        assertEquals(ActivityRecordingStatus.RECORDING, recorder.state.value.status)
        recorder.acceptRecognizedRepetition()

        val resting = recorder.state.value
        assertEquals(ActivityRecordingStatus.RESTING, resting.status)
        assertEquals(1, resting.planStepIndex)
        assertEquals(30L, resting.repetitionRestSeconds)
        assertNotNull(resting.restStartedAt)
        val set = resting.repetitionSets.single()
        assertEquals(3L, set.repetitions)
        assertEquals(30L, set.restSeconds)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_OTHER_WORKOUT, set.segmentType)
        assertEquals("Push-ups", set.label)
        assertFalse(set.isDuration)

        recorder.startNextRepetitionSet()
        val next = recorder.state.value
        assertEquals(ActivityRecordingStatus.RECORDING, next.status)
        assertEquals(1, next.planStepIndex)
        assertEquals(0L, next.currentSetRepetitionCount)
        assertNotNull(next.currentSetStartedAt)
    }

    @Test fun `a timed step ends on its deadline and the last step rests for nothing`() {
        val recorder = controller()
        recorder.startPlanRecording(plan(), calisthenics)
        recorder.skipPlanStep()
        recorder.skipPlanStep()
        val plank = recorder.state.value
        assertEquals(2, plank.planStepIndex)
        assertEquals(ActivityRecordingStatus.RECORDING, plank.status)
        assertTrue(plank.repetitionSets.isEmpty())
        assertNotNull(plank.planStepRemaining())

        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(46_000L)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()

        val done = recorder.state.value
        assertTrue(done.isPlanComplete)
        assertEquals(ActivityRecordingStatus.RECORDING, done.status)
        val set = done.repetitionSets.single()
        assertTrue(set.isDuration)
        assertEquals(0L, set.repetitions)
        assertEquals(0L, set.restSeconds)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK, set.segmentType)
        assertNull(set.label)
    }

    @Test fun `done with nothing counted is a skip, done on a plank records the time held`() {
        val recorder = controller()
        recorder.startPlanRecording(plan(), calisthenics)

        recorder.completeCurrentPlanStep()
        assertEquals(1, recorder.state.value.planStepIndex)
        assertTrue(recorder.state.value.repetitionSets.isEmpty())
        assertEquals(ActivityRecordingStatus.RECORDING, recorder.state.value.status)

        recorder.skipPlanStep()
        recorder.completeCurrentPlanStep()
        val done = recorder.state.value
        assertTrue(done.isPlanComplete)
        assertTrue(done.repetitionSets.single().isDuration)
        assertNull(done.currentSetStartedAt)
    }

    @Test fun `finishing mid-plank closes the open step and names the plan`() {
        val recorder = controller()
        recorder.startPlanRecording(plan(), calisthenics)
        recorder.skipPlanStep()
        recorder.skipPlanStep()

        val snapshot = recorder.finishRecording()

        assertNotNull(snapshot)
        assertEquals("plan-1", snapshot!!.planId)
        assertEquals("Strength", snapshot.planTitle)
        val set = snapshot.repetitionSets.single()
        assertTrue(set.isDuration)
        assertEquals(ExerciseSegment.EXERCISE_SEGMENT_TYPE_PLANK, set.segmentType)
        assertFalse(recorder.state.value.isActive)
    }

    @Test fun `a plan run restored after process death keeps its cursor and re-arms the plank`() {
        val first = controller()
        first.startPlanRecording(plan(), calisthenics)
        first.acceptRecognizedRepetition()
        first.completeCurrentPlanStep()
        first.startNextRepetitionSet()
        first.skipPlanStep()
        assertEquals(2, first.state.value.planStepIndex)

        val restored = controller()
        val state = restored.state.value
        assertTrue(state.isPlanRun)
        assertEquals(first.state.value.planSteps, state.planSteps)
        assertEquals(2, state.planStepIndex)
        assertEquals("plan-1", state.planId)
        assertEquals(1, state.repetitionSets.size)
        assertEquals("Push-ups", state.repetitionSets.single().label)

        mainDispatcherRule.testDispatcher.scheduler.advanceTimeBy(46_000L)
        mainDispatcherRule.testDispatcher.scheduler.runCurrent()
        assertTrue(restored.state.value.isPlanComplete)
    }

    @Test fun `back reopens the step just finished with its count, and drops nothing on a skipped one`() {
        val recorder = controller()
        recorder.startPlanRecording(plan(), calisthenics)
        repeat(3) { recorder.acceptRecognizedRepetition() }
        assertEquals(ActivityRecordingStatus.RESTING, recorder.state.value.status)

        recorder.undoPlanStep()
        val reopened = recorder.state.value
        assertEquals(ActivityRecordingStatus.RECORDING, reopened.status)
        assertEquals(0, reopened.planStepIndex)
        assertEquals(3L, reopened.currentSetRepetitionCount)
        assertTrue(reopened.repetitionSets.isEmpty())
        assertNull(reopened.restStartedAt)

        // Take the phantom rep away and end the set again: two reps recorded.
        recorder.adjustRepetitionCount(-1)
        recorder.completeCurrentPlanStep()
        assertEquals(2L, recorder.state.value.repetitionSets.single().repetitions)
        assertEquals(0, recorder.state.value.repetitionSets.single().planStepIndex)

        // Skip the second set, then Back from the plank: the skipped step recorded nothing, so nothing is popped.
        recorder.startNextRepetitionSet()
        recorder.skipPlanStep()
        assertEquals(2, recorder.state.value.planStepIndex)
        recorder.undoPlanStep()
        assertEquals(1, recorder.state.value.planStepIndex)
        assertEquals(1, recorder.state.value.repetitionSets.size)
        assertEquals(0L, recorder.state.value.currentSetRepetitionCount)
    }

    @Test fun `back from a completed plan reopens the last step`() {
        val recorder = controller()
        recorder.startPlanRecording(plan(), calisthenics)
        recorder.skipPlanStep()
        recorder.skipPlanStep()
        recorder.completeCurrentPlanStep()
        assertTrue(recorder.state.value.isPlanComplete)

        recorder.undoPlanStep()

        val state = recorder.state.value
        assertFalse(state.isPlanComplete)
        assertEquals(2, state.planStepIndex)
        assertTrue(state.repetitionSets.isEmpty())
        assertNotNull(state.planStepRemaining())
    }
}
