package tech.mmarca.openvitals.features.recovery

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.insights.SleepScoreConfidence
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.SleepStage
import tech.mmarca.openvitals.data.repository.SleepRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import kotlinx.coroutines.test.runTest

class RecoveryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()

    private fun sleepRepo(sessions: List<SleepData> = emptyList()) =
        mockk<SleepRepository>().also { repo ->
            coEvery { repo.loadSleepSessions(any(), any()) } returns sessions
        }

    @Test
    fun `load builds seven day recovery overview from sleep sessions`() = runTest {
        val repo = sleepRepo(
            sessions = listOf(
                sleepSession(today),
            ),
        )

        val vm = RecoveryViewModel(repo, mainDispatcherRule.dispatcherProvider)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(today, state.selectedDate)
        assertEquals(7, state.days.size)
        assertEquals(hours(7) + minutes(30), state.today.sleepDurationMs)
        assertEquals(hours(1) + minutes(30), state.today.remDurationMs)
        assertEquals(hours(2), state.today.deepDurationMs)
        assertEquals("sleep-$today", state.today.mainSleepSession?.id)
        assertEquals(93, state.today.sleepScore.score)
        assertEquals(SleepScoreConfidence.MEDIUM, state.today.sleepScore.confidence)
        assertEquals(93.75, state.today.sleepScore.sleepEfficiencyPercent, 0.001)
        assertEquals(30.0, state.today.sleepScore.wakeAfterSleepOnsetMinutes, 0.001)
        coVerify { repo.loadSleepSessions(today.minusDays(6), today) }
    }

    @Test
    fun `main sleep session uses longest session for sleep schedule`() = runTest {
        val repo = sleepRepo(
            sessions = listOf(
                sleepSession(today, id = "overnight", durationHours = 8),
                sleepSession(today, id = "nap", durationHours = 1),
            ),
        )

        val vm = RecoveryViewModel(repo, mainDispatcherRule.dispatcherProvider)

        assertEquals("overnight", vm.uiState.value.today.mainSleepSession?.id)
    }

    @Test
    fun `sleep score has high confidence with stages awake stages and regularity baseline`() = runTest {
        val repo = sleepRepo(
            sessions = (0L..3L).map { offset ->
                sleepSession(today.minusDays(offset))
            },
        )

        val vm = RecoveryViewModel(repo, mainDispatcherRule.dispatcherProvider)

        assertEquals(SleepScoreConfidence.HIGH, vm.uiState.value.today.sleepScore.confidence)
        assertEquals(0.0, vm.uiState.value.today.sleepScore.regularityDifferenceMinutes!!, 0.001)
    }

    @Test
    fun `load returns empty days when repository has no sleep data`() = runTest {
        val vm = RecoveryViewModel(sleepRepo(), mainDispatcherRule.dispatcherProvider)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(7, state.days.size)
        assertEquals(0L, state.today.sleepDurationMs)
        assertEquals(0L, state.today.remDurationMs)
        assertEquals(0L, state.today.deepDurationMs)
        assertEquals(SleepScoreConfidence.NO_DATA, state.today.sleepScore.confidence)
    }

    @Test
    fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<SleepRepository>()
        coEvery { repo.loadSleepSessions(any(), any()) } throws RuntimeException("offline")

        val vm = RecoveryViewModel(repo, mainDispatcherRule.dispatcherProvider)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals("offline", vm.uiState.value.error)
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
