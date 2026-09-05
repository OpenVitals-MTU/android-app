package tech.mmarca.openvitals.features.recovery

import tech.mmarca.openvitals.core.presentation.ScreenError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.domain.preferences.BodyProfile
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.HeartRepository
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class RecoveryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()

    private fun sleepRepo(sessions: List<SleepData> = emptyList()) =
        mockk<SleepRepository>().also { repo ->
            coEvery { repo.loadSleepSessions(any(), any()) } returns sessions
        }

    private fun heartRepo() =
        mockk<HeartRepository>().also { repo ->
            coEvery { repo.loadHrvSamples(any(), any()) } returns emptyList()
        }

    private fun preferencesRepo(ageYears: Int? = null) =
        mockk<PreferencesRepository>().also { repo ->
            every { repo.bodyProfile() } returns BodyProfile(
                birthYear = ageYears?.let { today.year - it },
            )
        }

    private fun viewModel(
        sessions: List<SleepData> = emptyList(),
        sleepRepository: SleepRepository = sleepRepo(sessions),
        heartRepository: HeartRepository = heartRepo(),
        preferencesRepository: PreferencesRepository = preferencesRepo(),
    ) = RecoveryViewModel(
        sleepRepository = sleepRepository,
        heartRepository = heartRepository,
        preferencesRepository = preferencesRepository,
        dispatchers = mainDispatcherRule.dispatcherProvider,
    )

    @Test
    fun `load builds seven day recovery overview from sleep sessions`() = runTest {
        val repo = sleepRepo(
            sessions = listOf(
                sleepSession(today),
            ),
        )

        val vm = viewModel(sleepRepository = repo)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(today, state.selectedDate)
        assertEquals(7, state.days.size)
        assertEquals(hours(7) + minutes(30), state.today.sleepDurationMs)
        assertEquals(hours(1) + minutes(30), state.today.remDurationMs)
        assertEquals(hours(2), state.today.deepDurationMs)
        assertEquals("sleep-$today", state.today.mainSleepSession?.id)
        assertEquals(90, state.today.sleepScore.score)
        assertEquals(SleepScoreConfidence.MEDIUM, state.today.sleepScore.confidence)
        assertEquals(93.75, state.today.sleepScore.sleepEfficiencyPercent, 0.001)
        assertEquals(30.0, state.today.sleepScore.wakeAfterSleepOnsetMinutes, 0.001)
        coVerify { repo.loadSleepSessions(today.minusDays(6), today) }
    }

    @Test
    fun `main sleep session uses longest session for sleep schedule`() = runTest {
        val vm = viewModel(
            sessions = listOf(
                sleepSession(today, id = "overnight", durationHours = 8),
                sleepSession(today, id = "nap", durationHours = 1),
            ),
        )

        assertEquals("overnight", vm.uiState.value.today.mainSleepSession?.id)
    }

    @Test
    fun `sleep score has medium confidence with stages and regularity but no overnight HRV`() = runTest {
        val vm = viewModel(
            sessions = (0L..3L).map { offset ->
                sleepSession(today.minusDays(offset))
            },
        )

        assertEquals(SleepScoreConfidence.MEDIUM, vm.uiState.value.today.sleepScore.confidence)
        assertEquals(0.0, vm.uiState.value.today.sleepScore.regularityDifferenceMinutes!!, 0.001)
        assertFalse(vm.uiState.value.today.sleepScore.usesOvernightHrv)
    }

    @Test
    fun `load returns empty days when repository has no sleep data`() = runTest {
        val vm = viewModel()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(7, state.days.size)
        assertEquals(0L, state.today.sleepDurationMs)
        assertEquals(0L, state.today.remDurationMs)
        assertEquals(0L, state.today.deepDurationMs)
        assertEquals(SleepScoreConfidence.NO_DATA, state.today.sleepScore.confidence)
        // Blank, not an error: no sessions and therefore no main session either.
        assertTrue(state.today.sessions.isEmpty())
        assertNull(state.today.mainSleepSession)
        assertNull(state.error)
    }

    @Test
    fun `a day the lookback never reached is blank, not an error`() = runTest {
        // A day outside the seven-day window is not in `days`; `today` falls back to a blank day.
        val outOfLookback = today.minusDays(30)
        val vm = viewModel(sessions = listOf(sleepSession(today)))

        val fallback = vm.uiState.value.copy(selectedDate = outOfLookback).today

        assertEquals(outOfLookback, fallback.date)
        assertTrue(fallback.sessions.isEmpty())
        assertNull(fallback.mainSleepSession)
        assertEquals(SleepScoreConfidence.NO_DATA, fallback.sleepScore.confidence)
        assertEquals(0L, fallback.sleepDurationMs)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun `a failed reload keeps the week already on screen`() = runTest {
        val repo = mockk<SleepRepository>()
        coEvery { repo.loadSleepSessions(any(), any()) } returns listOf(sleepSession(today))
        val vm = viewModel(sleepRepository = repo)
        assertEquals(7, vm.uiState.value.days.size)

        coEvery { repo.loadSleepSessions(any(), any()) } throws RuntimeException("the provider hung up")
        vm.load(today)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(ScreenError.Message("the provider hung up"), state.error)
        assertFalse(state.isLoading)
        // The week the screen is already rendering survives the failed reload.
        assertEquals(7, state.days.size)
        assertEquals("sleep-$today", state.today.mainSleepSession?.id)
    }

    @Test
    fun `a permission failure becomes ScreenError PermissionDenied`() = runTest {
        val repo = mockk<SleepRepository>()
        coEvery { repo.loadSleepSessions(any(), any()) } throws SecurityException("sleep read")

        val vm = viewModel(sleepRepository = repo)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(ScreenError.PermissionDenied, state.error)
        // The screens fall back to the error only when there is nothing to show.
        assertTrue(state.days.isEmpty())
    }

    @Test
    fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<SleepRepository>()
        coEvery { repo.loadSleepSessions(any(), any()) } throws RuntimeException("offline")

        val vm = viewModel(sleepRepository = repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.Message("offline"), vm.uiState.value.error)
    }

    private fun sleepSession(
        date: LocalDate,
        id: String = "sleep-$date",
        durationHours: Long = 8L,
    ): SleepData {
        val zone = ZoneId.systemDefault()
        val end = date.atTime(7, 0).atZone(zone).toInstant()
        val start = end.minus(Duration.ofHours(durationHours))
        val deepEnd = start.plus(Duration.ofHours(2))
        val remEnd = deepEnd.plus(Duration.ofMinutes(90))
        val awakeEnd = remEnd.plus(Duration.ofMinutes(30))
        val lightEnd = awakeEnd.plus(Duration.ofHours(4))
        val stages = if (durationHours >= 8L) {
            listOf(
                SleepStage(start, deepEnd, SleepStage.STAGE_DEEP),
                SleepStage(deepEnd, remEnd, SleepStage.STAGE_REM),
                SleepStage(remEnd, awakeEnd, SleepStage.STAGE_AWAKE),
                SleepStage(awakeEnd, lightEnd, SleepStage.STAGE_LIGHT),
            )
        } else {
            emptyList()
        }
        return SleepData(
            id = id,
            startTime = start,
            endTime = end,
            durationMs = Duration.between(start, end).toMillis(),
            source = "test",
            stages = stages,
        )
    }

    private fun hours(value: Long): Long = Duration.ofHours(value).toMillis()

    private fun minutes(value: Long): Long = Duration.ofMinutes(value).toMillis()
}
