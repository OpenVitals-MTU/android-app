package tech.mmarca.openvitals.features.activity

import androidx.health.connect.client.records.ExerciseSegment
import androidx.health.connect.client.records.ExerciseSessionRecord
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.ActivityMarkerRepository
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.domain.insights.SplitSource
import tech.mmarca.openvitals.domain.model.ActivityCadenceKind
import tech.mmarca.openvitals.domain.model.ActivityCadenceSample
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.SpeedSample
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ActivityDetailViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `initial load fetches selected activity`() = runTest {
        val workout = workout(id = "activity-1")
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        stubMetricSamples(repo)

        val vm = ActivityDetailViewModel(repo, "activity-1")

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(workout, vm.uiState.value.workout)
        assertNull(vm.uiState.value.error)
        coVerify(exactly = 1) { repo.loadWorkout("activity-1") }
    }

    @Test fun `initial load backfills missing averages from samples`() = runTest {
        val workout = workout(id = "activity-1")
        val repo = mockk<ActivityRepository>()
        val heartRepo = mockk<HeartRepository>()
        val heartSamples = listOf(
            HeartRateSample(workout.startTime, 100L, "test"),
            HeartRateSample(workout.startTime.plusSeconds(60), 110L, "test"),
        )
        val speedSamples = listOf(
            SpeedSample(workout.startTime, 2.0, "test"),
            SpeedSample(workout.startTime.plusSeconds(60), 4.0, "test"),
        )
        val cadenceSamples = listOf(
            ActivityCadenceSample(workout.startTime, 160.0, ActivityCadenceKind.STEPS, "test"),
            ActivityCadenceSample(workout.startTime.plusSeconds(60), 180.0, ActivityCadenceKind.STEPS, "test"),
            ActivityCadenceSample(workout.startTime, 80.0, ActivityCadenceKind.CYCLING, "test"),
            ActivityCadenceSample(workout.startTime.plusSeconds(60), 100.0, ActivityCadenceKind.CYCLING, "test"),
        )
        coEvery { repo.loadWorkout("activity-1") } returns workout
        coEvery { heartRepo.loadHeartRateSamples(workout.startTime, workout.endTime) } returns heartSamples
        coEvery { repo.loadSpeedSamples(workout.startTime, workout.endTime) } returns speedSamples
        coEvery { repo.loadActivityCadenceSamples(workout.startTime, workout.endTime) } returns cadenceSamples

        val vm = ActivityDetailViewModel(repo, "activity-1", heartRepository = heartRepo)
        val backfilled = requireNotNull(vm.uiState.value.workout)

        assertEquals(105L, backfilled.averageHeartRateBpm)
        assertEquals(3.0, backfilled.averageSpeedMetersPerSecond ?: 0.0, 0.001)
        assertEquals(170.0, backfilled.averageStepsCadenceRate ?: 0.0, 0.001)
        assertEquals(90.0, backfilled.averageCyclingCadenceRpm ?: 0.0, 0.001)
        assertEquals(heartSamples, vm.uiState.value.heartRateSamples)
        assertEquals(speedSamples, vm.uiState.value.speedSamples)
        assertEquals(cadenceSamples, vm.uiState.value.cadenceSamples)
    }

    @Test fun `derives a distance from speed when no distance was written`() = runTest {
        // A watch that records speed but no DistanceRecord: 60 s at 3 m/s implies 180 m.
        val workout = workout(id = "activity-1", totalDistanceMeters = null)
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        coEvery { repo.loadActivityCadenceSamples(any(), any()) } returns emptyList()
        coEvery { repo.loadSpeedSamples(workout.startTime, workout.endTime) } returns listOf(
            SpeedSample(workout.startTime, 2.0, "test"),
            SpeedSample(workout.startTime.plusSeconds(60), 4.0, "test"),
        )

        val vm = ActivityDetailViewModel(repo, "activity-1")

        assertEquals(180.0, vm.uiState.value.workout!!.totalDistanceMeters ?: 0.0, 0.001)
    }

    @Test fun `a recorded distance is never overwritten by the derived one`() = runTest {
        val workout = workout(id = "activity-1", totalDistanceMeters = 5_000.0)
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        coEvery { repo.loadActivityCadenceSamples(any(), any()) } returns emptyList()
        coEvery { repo.loadSpeedSamples(workout.startTime, workout.endTime) } returns listOf(
            SpeedSample(workout.startTime, 2.0, "test"),
            SpeedSample(workout.startTime.plusSeconds(60), 4.0, "test"),
        )

        val vm = ActivityDetailViewModel(repo, "activity-1")

        assertEquals(5_000.0, vm.uiState.value.workout!!.totalDistanceMeters ?: 0.0, 0.001)
    }

    @Test fun `missing activity sets not found error`() = runTest {
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("missing") } returns null

        val vm = ActivityDetailViewModel(repo, "missing")

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.workout)
        assertEquals(ScreenError.NotFound, vm.uiState.value.error)
    }

    @Test fun `blank activity id fails without calling repository`() = runTest {
        val repo = mockk<ActivityRepository>(relaxed = true)

        val vm = ActivityDetailViewModel(repo, "")

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.MissingArgument, vm.uiState.value.error)
        coVerify(exactly = 0) { repo.loadWorkout(any()) }
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } throws RuntimeException("timeout")
        stubMetricSamples(repo)

        val vm = ActivityDetailViewModel(repo, "activity-1")

        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.workout)
        assertEquals(ScreenError.Message("timeout"), vm.uiState.value.error)
    }

    @Test fun `deleteActivity deletes OpenVitals activity and reports completion`() = runTest {
        val workout = workout(id = "activity-1", isOpenVitalsEntry = true)
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        coEvery { repo.deleteActivityEntry("activity-1") } returns Unit
        stubMetricSamples(repo)
        val vm = ActivityDetailViewModel(repo, "activity-1")
        var deleted = false

        vm.deleteActivity { deleted = true }

        assertFalse(vm.uiState.value.isDeleting)
        assertNull(vm.uiState.value.workout)
        assertEquals(true, deleted)
        coVerify(exactly = 1) { repo.deleteActivityEntry("activity-1") }
    }

    @Test fun `keeps the screen and records the error when the delete fails`() = runTest {
        val workout = workout(id = "activity-1", isOpenVitalsEntry = true)
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        coEvery { repo.deleteActivityEntry("activity-1") } throws
            IllegalStateException("the write was rejected")
        stubMetricSamples(repo)
        val vm = ActivityDetailViewModel(repo, "activity-1")
        var deleted = false

        vm.deleteActivity { deleted = true }

        // A failed delete must not look like a successful one.
        assertFalse(vm.uiState.value.isDeleting)
        assertEquals(workout, vm.uiState.value.workout)
        assertEquals(false, deleted)
        assertEquals(ScreenError.Message("the write was rejected"), vm.uiState.value.error)
    }

    @Test fun `deleteActivity ignores workout not created by OpenVitals`() = runTest {
        val workout = workout(id = "activity-1", isOpenVitalsEntry = false)
        val repo = mockk<ActivityRepository>(relaxed = true)
        coEvery { repo.loadWorkout("activity-1") } returns workout
        stubMetricSamples(repo)
        val vm = ActivityDetailViewModel(repo, "activity-1")

        vm.deleteActivity()

        assertEquals(workout, vm.uiState.value.workout)
        coVerify(exactly = 0) { repo.deleteActivityEntry(any()) }
    }

    @Test fun `load cuts splits at the preferred distance`() = runTest {
        val workout = workout(id = "activity-1", totalDistanceMeters = 3_000.0)
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        stubMetricSamples(repo)

        val vm = ActivityDetailViewModel(
            repo,
            "activity-1",
            preferencesRepository = prefs(splitDistanceMeters = 1_000.0),
        )

        val state = vm.uiState.value
        assertEquals(SplitSource.ESTIMATED, state.splits.source)
        assertEquals(3, state.splits.splits.size)
        assertEquals(1_000.0, state.splitDistanceMeters, 0.001)
        // The estimated source spreads the average over every split, so the scale collapses.
        assertEquals(state.slowestSplitPaceSeconds, state.fastestSplitPaceSeconds)
        // …and the split-speed trace must refuse to draw the flat line.
        assertNull(state.splitSpeedTrace)
    }

    @Test fun `split distance preference change re-cuts splits without reloading`() = runTest {
        val workout = workout(id = "activity-1", totalDistanceMeters = 3_000.0)
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        stubMetricSamples(repo)
        val distanceFlow = MutableStateFlow(1_000.0)
        val vm = ActivityDetailViewModel(
            repo,
            "activity-1",
            preferencesRepository = prefs(splitDistanceMeters = 1_000.0, flow = distanceFlow),
        )
        assertEquals(3, vm.uiState.value.splits.splits.size)

        distanceFlow.value = 500.0

        assertEquals(6, vm.uiState.value.splits.splits.size)
        assertEquals(500.0, vm.uiState.value.splitDistanceMeters, 0.001)
        // A preference change is a state update, not a Health Connect reload.
        coVerify(exactly = 1) { repo.loadWorkout("activity-1") }
    }

    @Test fun `non distance activity yields no splits`() = runTest {
        // Strength training: real GPS-drift distance must not become splits.
        val workout = workout(
            id = "activity-1",
            exerciseType = ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING,
            totalDistanceMeters = 200.0,
        )
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        stubMetricSamples(repo)

        val vm = ActivityDetailViewModel(
            repo,
            "activity-1",
            preferencesRepository = prefs(splitDistanceMeters = 1_000.0),
        )

        assertTrue(vm.uiState.value.splits.isEmpty)
    }

    @Test fun `a failing cadence read costs the card, not the screen`() = runTest {
        val workout = workout(id = "activity-1", totalDistanceMeters = 3_000.0)
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        coEvery { repo.loadSpeedSamples(any(), any()) } returns listOf(
            SpeedSample(workout.startTime, 2.0, "test"),
            SpeedSample(workout.startTime.plusSeconds(60), 4.0, "test"),
        )
        coEvery { repo.loadActivityCadenceSamples(any(), any()) } throws
            SecurityException("no cadence permission")

        val vm = ActivityDetailViewModel(repo, "activity-1")

        // The cadence card is empty, the speed card is not, and the screen renders.
        val state = vm.uiState.value
        assertTrue(state.cadenceSamples.isEmpty())
        assertEquals(2, state.speedSamples.size)
        assertEquals(workout.id, state.workout?.id)
        assertNull(state.error)
    }

    @Test fun `a failing speed read degrades to estimated splits instead of blowing up the screen`() = runTest {
        val workout = workout(id = "activity-1", totalDistanceMeters = 3_000.0)
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        coEvery { repo.loadActivityCadenceSamples(any(), any()) } returns emptyList()
        coEvery { repo.loadSpeedSamples(any(), any()) } throws
            SecurityException("no speed permission")

        val vm = ActivityDetailViewModel(
            repo,
            "activity-1",
            preferencesRepository = prefs(splitDistanceMeters = 1_000.0),
        )

        val state = vm.uiState.value
        assertTrue(state.speedSamples.isEmpty())
        assertEquals(SplitSource.ESTIMATED, state.splits.source)
        assertEquals(3, state.splits.splits.size)
        assertNull(state.error)
    }

    @Test fun `a failing marker read costs the marks, not the screen`() = runTest {
        // Kotlin folds the session metrics into loadWorkout, so the marker read is the degrading read.
        val workout = workout(id = "activity-1")
        val repo = mockk<ActivityRepository>()
        val markerRepo = mockk<ActivityMarkerRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        stubMetricSamples(repo)
        every { markerRepo.markersForActivity(any()) } throws
            IllegalStateException("the marker store is corrupt")

        val vm = ActivityDetailViewModel(repo, "activity-1", markerRepository = markerRepo)

        val state = vm.uiState.value
        assertTrue(state.markers.isEmpty())
        assertEquals(workout.id, state.workout?.id)
        assertNull(state.error)
    }

    @Test fun `a failed recovery read costs the card, not the screen`() = runTest {
        val workout = workout(
            id = "activity-1",
            segments = listOf(trailingRestSegment(Instant.EPOCH.plusSeconds(3_600))),
        )
        val recoveryStart = Instant.EPOCH.plusSeconds(3_600 - 120)
        val sessionSamples = listOf(HeartRateSample(workout.startTime, 120L, "test"))
        val repo = mockk<ActivityRepository>()
        val heartRepo = mockk<HeartRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        stubMetricSamples(repo)
        coEvery {
            heartRepo.loadHeartRateSamples(workout.startTime, workout.endTime)
        } returns sessionSamples
        coEvery {
            heartRepo.loadHeartRateSamples(recoveryStart.minusSeconds(60), any())
        } throws SecurityException("no heart rate permission")

        val vm = ActivityDetailViewModel(repo, "activity-1", heartRepository = heartRepo)

        // The workout still loads, its own samples are there, and the recovery
        // card simply has nothing to draw.
        val state = vm.uiState.value
        assertEquals(workout.id, state.workout?.id)
        assertEquals(sessionSamples, state.heartRateSamples)
        assertNull(state.heartRateRecovery)
        assertNull(state.error)
    }

    private fun prefs(
        splitDistanceMeters: Double,
        flow: MutableStateFlow<Double> = MutableStateFlow(splitDistanceMeters),
    ): PreferencesRepository =
        mockk<PreferencesRepository> {
            every { activitySplitDistanceMeters } answers { flow.value }
            every { activitySplitDistanceMetersFlow } returns flow
        }

    private fun stubMetricSamples(repo: ActivityRepository) {
        coEvery { repo.loadSpeedSamples(any(), any()) } returns emptyList()
        coEvery { repo.loadActivityCadenceSamples(any(), any()) } returns emptyList()
    }

    private fun workout(
        id: String,
        isOpenVitalsEntry: Boolean = false,
        exerciseType: Int = 56,
        totalDistanceMeters: Double? = null,
        segments: List<ExerciseSegmentData> = emptyList(),
    ) = ExerciseData(
        id = id,
        title = "Morning run",
        exerciseType = exerciseType,
        startTime = Instant.EPOCH,
        endTime = Instant.EPOCH.plusSeconds(3_600),
        durationMs = 3_600_000,
        source = "test",
        isOpenVitalsEntry = isOpenVitalsEntry,
        totalDistanceMeters = totalDistanceMeters,
        segments = segments,
    )

    @Test fun `recovery window issues its own heart rate read and exposes the reading`() = runTest {
        val workout = workout(
            id = "activity-1",
            segments = listOf(trailingRestSegment(Instant.EPOCH.plusSeconds(3_600))),
        )
        val recoveryStart = Instant.EPOCH.plusSeconds(3_600 - 120)
        val readStart = recoveryStart.minusSeconds(60)
        val readEnd = recoveryStart.plusSeconds(5 * 60 + 30)
        val repo = mockk<ActivityRepository>()
        val heartRepo = mockk<HeartRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        stubMetricSamples(repo)
        coEvery {
            heartRepo.loadHeartRateSamples(workout.startTime, workout.endTime)
        } returns emptyList()
        coEvery { heartRepo.loadHeartRateSamples(readStart, readEnd) } returns listOf(
            HeartRateSample(recoveryStart.minusSeconds(4), 176L, "test"),
            HeartRateSample(recoveryStart.minusSeconds(2), 178L, "test"),
            HeartRateSample(recoveryStart.plusSeconds(60), 140L, "test"),
        )

        val vm = ActivityDetailViewModel(repo, "activity-1", heartRepository = heartRepo)

        val reading = requireNotNull(vm.uiState.value.heartRateRecovery)
        assertEquals(178L, reading.peakBpm)
        assertEquals(38L, reading.headlineDropBpm)
        coVerify(exactly = 1) { heartRepo.loadHeartRateSamples(readStart, readEnd) }
    }

    @Test fun `no recovery window issues no extra heart rate read`() = runTest {
        val workout = workout(id = "activity-1")
        val repo = mockk<ActivityRepository>()
        val heartRepo = mockk<HeartRepository>()
        coEvery { repo.loadWorkout("activity-1") } returns workout
        stubMetricSamples(repo)
        coEvery {
            heartRepo.loadHeartRateSamples(workout.startTime, workout.endTime)
        } returns emptyList()

        val vm = ActivityDetailViewModel(repo, "activity-1", heartRepository = heartRepo)

        assertNull(vm.uiState.value.heartRateRecovery)
        coVerify(exactly = 1) { heartRepo.loadHeartRateSamples(any<Instant>(), any<Instant>()) }
    }

    private fun trailingRestSegment(sessionEnd: Instant) = ExerciseSegmentData(
        startTime = sessionEnd.minusSeconds(120),
        endTime = sessionEnd,
        segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
        repetitions = 0,
    )
}
