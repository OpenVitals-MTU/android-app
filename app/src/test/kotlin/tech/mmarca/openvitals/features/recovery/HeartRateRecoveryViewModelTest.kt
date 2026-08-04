package tech.mmarca.openvitals.features.recovery

import android.util.Log
import androidx.health.connect.client.records.ExerciseSegment
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.contract.ActivityRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.domain.model.ExerciseSegmentData
import tech.mmarca.openvitals.domain.model.HeartRateSample
import tech.mmarca.openvitals.domain.model.HeartRateSummary
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.util.MainDispatcherRule

class HeartRateRecoveryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val zone = ZoneId.systemDefault()
    private val monthWindow = PeriodLoadQuery(range = TimeRange.MONTH, anchorDate = today)
        .windows.current

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(Log::class)
    }

    private fun activityRepo(workouts: List<ExerciseData>) =
        mockk<ActivityRepository>().also { repo ->
            coEvery { repo.loadWorkouts(any(), any()) } returns workouts
        }

    private fun heartRepo(
        samples: List<HeartRateSample> = emptyList(),
        summaries: List<HeartRateSummary> = emptyList(),
    ) = mockk<HeartRepository>().also { repo ->
        coEvery { repo.loadHeartRateSamples(any<Instant>(), any<Instant>()) } returns samples
        coEvery { repo.loadDailyHeartRateSummaries(any(), any()) } returns summaries
        coEvery { repo.loadRestingHeartRate(any()) } returns 55L
    }

    private fun viewModel(
        activityRepository: ActivityRepository,
        heartRepository: HeartRepository,
        profile: BodyProfile = BodyProfile(),
    ) = HeartRateRecoveryViewModel(
        activityRepository = activityRepository,
        heartRepository = heartRepository,
        bodyProfileProvider = { profile },
        dispatchers = mainDispatcherRule.dispatcherProvider,
    )

    @Test
    fun `only sessions of five minutes or more with a stop mark cost a heart rate read`() = runTest {
        val tooShort = testSession(id = "short", startHour = 8, durationMinutes = 4)
        val noMark = plainWorkout(id = "no-mark", startHour = 10, durationMinutes = 30)
        val candidate = testSession(id = "candidate", startHour = 12, durationMinutes = 30)
        val heartRepo = heartRepo()

        val vm = viewModel(activityRepo(listOf(tooShort, noMark, candidate)), heartRepo)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(listOf("candidate"), state.readings.map { it.sessionId })
        assertFalse(state.truncated)
        coVerify(exactly = 1) { heartRepo.loadHeartRateSamples(any<Instant>(), any<Instant>()) }
    }

    @Test
    fun `sessions come back newest first and a period over the cap is truncated and says so`() = runTest {
        val workouts = (0 until 401).map { index ->
            testSession(id = "session-$index", startHour = 6, startMinuteOffset = index, durationMinutes = 10)
        }
        val heartRepo = heartRepo()

        val vm = viewModel(activityRepo(workouts), heartRepo)

        val state = vm.uiState.value
        assertEquals(400, state.readings.size)
        assertTrue(state.truncated)
        assertEquals("session-400", state.readings.first().sessionId)
        // The single oldest session is the one the cap dropped.
        assertEquals("session-1", state.readings.last().sessionId)
        coVerify(exactly = 400) { heartRepo.loadHeartRateSamples(any<Instant>(), any<Instant>()) }
    }

    @Test
    fun `only comparable readings feed the chart and the rest are counted as unmeasured`() = runTest {
        val measured = testSession(id = "measured", startHour = 8, durationMinutes = 30)
        val unmeasured = testSession(id = "unmeasured", startHour = 12, durationMinutes = 30)
        val recoveryStart = recoveryStartOf(measured)
        val heartRepo = heartRepo()
        coEvery {
            heartRepo.loadHeartRateSamples(recoveryStart.minusSeconds(60), recoveryStart.plusSeconds(330))
        } returns listOf(
            HeartRateSample(recoveryStart.minusSeconds(4), 176L, "test"),
            HeartRateSample(recoveryStart.minusSeconds(2), 178L, "test"),
            HeartRateSample(recoveryStart.plusSeconds(60), 140L, "test"),
        )

        val vm = viewModel(
            activityRepo(listOf(measured, unmeasured)),
            heartRepo,
            profile = BodyProfile(maxHeartRateBpm = 180),
        )

        val state = vm.uiState.value
        assertEquals(2, state.readings.size)
        assertEquals(listOf("measured"), state.comparable.map { it.sessionId })
        assertEquals(38L, state.comparable.single().reading.headlineDropBpm)
    }

    @Test
    fun `observed maximum is fetched once for the whole period over the trailing ninety days`() = runTest {
        val workouts = listOf(
            testSession(id = "a", startHour = 8, durationMinutes = 30),
            testSession(id = "b", startHour = 12, durationMinutes = 30),
        )
        val heartRepo = heartRepo(
            summaries = listOf(HeartRateSummary(today, avgBpm = 70L, minBpm = 50L, maxBpm = 185L)),
        )

        viewModel(activityRepo(workouts), heartRepo)

        coVerify(exactly = 1) {
            heartRepo.loadDailyHeartRateSummaries(
                monthWindow.end.minusDays(90),
                monthWindow.end,
            )
        }
    }

    @Test
    fun `a period with no candidates issues no heart rate reads at all`() = runTest {
        val heartRepo = heartRepo()

        val vm = viewModel(activityRepo(listOf(plainWorkout(id = "w", startHour = 9, durationMinutes = 60))), heartRepo)

        assertTrue(vm.uiState.value.readings.isEmpty())
        coVerify(exactly = 0) { heartRepo.loadHeartRateSamples(any<Instant>(), any<Instant>()) }
        coVerify(exactly = 0) { heartRepo.loadDailyHeartRateSummaries(any(), any()) }
    }

    @Test
    fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<ActivityRepository>()
        coEvery { repo.loadWorkouts(any(), any()) } throws RuntimeException("offline")

        val vm = viewModel(repo, heartRepo())

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.Message("offline"), vm.uiState.value.error)
    }

    private fun recoveryStartOf(session: ExerciseData): Instant =
        session.segments.single().startTime

    /** A guided recovery test: a session whose last two minutes are a rest segment. */
    private fun testSession(
        id: String,
        startHour: Int,
        durationMinutes: Long,
        startMinuteOffset: Int = 0,
    ): ExerciseData {
        val start = today
            .atStartOfDay(zone)
            .plusHours(startHour.toLong())
            .plusMinutes(startMinuteOffset.toLong())
            .toInstant()
        val end = start.plusSeconds(durationMinutes * 60)
        return ExerciseData(
            id = id,
            title = "Recovery test",
            exerciseType = 56,
            startTime = start,
            endTime = end,
            durationMs = durationMinutes * 60_000,
            source = "test",
            segments = listOf(
                ExerciseSegmentData(
                    startTime = end.minusSeconds(120),
                    endTime = end,
                    segmentType = ExerciseSegment.EXERCISE_SEGMENT_TYPE_REST,
                    repetitions = 0,
                ),
            ),
        )
    }

    private fun plainWorkout(
        id: String,
        startHour: Int,
        durationMinutes: Long,
    ): ExerciseData {
        val start = today.atStartOfDay(zone).plusHours(startHour.toLong()).toInstant()
        return ExerciseData(
            id = id,
            title = "Morning run",
            exerciseType = 56,
            startTime = start,
            endTime = start.plusSeconds(durationMinutes * 60),
            durationMs = durationMinutes * 60_000,
            source = "test",
        )
    }
}
