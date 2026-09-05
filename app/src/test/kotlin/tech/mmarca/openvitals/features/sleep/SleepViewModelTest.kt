package tech.mmarca.openvitals.features.sleep

import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.model.SleepData
import tech.mmarca.openvitals.domain.model.SleepStage
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.preferences.SleepWindow
import tech.mmarca.openvitals.domain.query.SleepPeriodData
import tech.mmarca.openvitals.data.repository.contract.SleepRepository
import tech.mmarca.openvitals.domain.model.RefreshMode
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SleepViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val pastAnchor = today.minusWeeks(4)

    private fun emptyRepo() = mockk<SleepRepository>().also { repo ->
        coEvery { repo.loadSleepSessions(any(), any()) } returns emptyList()
        suspend fun periodData(query: PeriodLoadQuery): SleepPeriodData {
            val windows = query.windows
            fun queryStart(date: LocalDate) = date.minusDays(1)
            return SleepPeriodData(
                sessions = repo.loadSleepSessions(queryStart(windows.current.start), windows.current.end),
                previousSessions = repo.loadSleepSessions(queryStart(windows.previous.start), windows.previous.end),
                baselineSessions = repo.loadSleepSessions(queryStart(windows.baseline.start), windows.baseline.end),
            )
        }
        coEvery { repo.loadSleepPeriod(any(), any()) } coAnswers { periodData(firstArg()) }
        coEvery { repo.loadSleepPeriod(any(), any(), any()) } coAnswers { periodData(firstArg()) }
    }

    /** One night ending at 07:00 LOCAL on [date], so day bucketing is zone-proof. */
    private fun localNight(date: LocalDate, hours: Double): SleepData {
        val zone = ZoneId.systemDefault()
        val end = date.atTime(7, 0).atZone(zone).toInstant()
        val durationMs = (hours * 3_600_000).toLong()
        val start = end.minusMillis(durationMs)
        return SleepData(
            id = "night-$date",
            startTime = start,
            endTime = end,
            durationMs = durationMs,
            source = "test",
            stages = listOf(SleepStage(start, end, SleepStage.STAGE_LIGHT)),
        )
    }

    private fun sleepSession(offsetDays: Long = 0) = SleepData(
        id = "s$offsetDays",
        startTime = Instant.EPOCH,
        endTime = Instant.ofEpochMilli(28_800_000L),
        durationMs = 28_800_000L,
        source = "test",
        stages = listOf(
            SleepStage(Instant.EPOCH, Instant.ofEpochMilli(3_600_000L), SleepStage.STAGE_DEEP),
        ),
    )

    // Initial state.

    @Test fun `initial range is WEEK`() = runTest {
        val vm = SleepViewModel(emptyRepo(), dispatchers = mainDispatcherRule.dispatcherProvider)
        assertEquals(TimeRange.WEEK, vm.uiState.value.selectedRange)
    }

    @Test fun `initial load clears loading`() = runTest {
        val vm = SleepViewModel(emptyRepo(), dispatchers = mainDispatcherRule.dispatcherProvider)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `initial sessions list is empty when repo returns nothing`() = runTest {
        val vm = SleepViewModel(emptyRepo(), dispatchers = mainDispatcherRule.dispatcherProvider)
        assertTrue(vm.uiState.value.sessions.isEmpty())
    }

    // Load success and failure.

    @Test fun `load success populates sessions`() = runTest {
        val sessions = listOf(sleepSession())
        val repo = emptyRepo()
        coEvery { repo.loadSleepSessions(any(), any()) } returns sessions

        val vm = SleepViewModel(repo, dispatchers = mainDispatcherRule.dispatcherProvider)

        assertEquals(sessions, vm.uiState.value.sessions)
        assertFalse(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
        assertTrue(vm.uiState.value.display.durationPoints.isNotEmpty())
    }

    @Test fun `a loaded period lands with its display precomputed`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.loadSleepSessions(any(), any()) } returns listOf(localNight(pastAnchor, 8.0))

        val vm = SleepViewModel(repo, dispatchers = mainDispatcherRule.dispatcherProvider)
        vm.selectDate(pastAnchor)
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(TimeRange.WEEK, state.selectedRange)
        // The screen renders this; it must exist by the time loading ends.
        assertEquals(7, state.display.durationPoints.size)
        val totals = sleepPeriodTotals(state.display.durationPoints)
        assertEquals(1, totals.nights)
        assertEquals(8.0, totals.averageHours, 1e-6)
        assertEquals(8.0, state.dailyGoalHours, 0.0)
        assertEquals(
            8.0,
            sleepGoalProgress(state, state.display.selectedPeriod, state.display.durationPoints).target,
            0.0,
        )
    }

    // Refresh, goal, staleness.

    @Test fun `refresh reloads the current selection in force mode`() = runTest {
        val repo = emptyRepo()
        val vm = SleepViewModel(repo, dispatchers = mainDispatcherRule.dispatcherProvider)
        val rangeBefore = vm.uiState.value.selectedRange

        vm.resumeCurrentPeriod(refreshCurrent = true)
        advanceUntilIdle()

        coVerify(exactly = 2) { repo.loadSleepPeriod(any(), any(), any()) }
        coVerify(exactly = 1) { repo.loadSleepPeriod(any(), any(), RefreshMode.FORCE) }
        assertEquals(rangeBefore, vm.uiState.value.selectedRange)
    }

    @Test fun `moving the goal rebuilds the display without reloading`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.loadSleepSessions(any(), any()) } returns listOf(localNight(pastAnchor, 8.0))
        var persisted: Double? = null
        val vm = SleepViewModel(
            repository = repo,
            dispatchers = mainDispatcherRule.dispatcherProvider,
            onDailyGoalChanged = { persisted = it },
        )
        vm.selectDate(pastAnchor)
        advanceUntilIdle()
        val pointsBefore = vm.uiState.value.display.durationPoints
        clearMocks(repo, answers = false, recordedCalls = true)

        vm.increaseDailyGoal()

        val state = vm.uiState.value
        assertEquals(8.25, state.dailyGoalHours, 0.0)
        assertEquals(8.25, persisted!!, 0.0)
        // The goal card and the goal statistics read this: it has to move with it.
        assertEquals(
            8.25,
            sleepGoalProgress(state, state.display.selectedPeriod, state.display.durationPoints).target,
            0.0,
        )
        // …and it did not go back to the repository to find that out.
        assertEquals(pointsBefore, state.display.durationPoints)
        coVerify(exactly = 0) { repo.loadSleepPeriod(any(), any()) }

        vm.decreaseDailyGoal()
        assertEquals(8.0, vm.uiState.value.dailyGoalHours, 0.0)
    }

    @Test fun `a same-range refresh keeps the display`() = runTest {
        val repo = emptyRepo()
        coEvery { repo.loadSleepSessions(any(), any()) } returns listOf(localNight(today, 8.0))
        val vm = SleepViewModel(repo, dispatchers = mainDispatcherRule.dispatcherProvider)
        advanceUntilIdle()
        val loaded = vm.uiState.value.display
        assertTrue(loaded.durationPoints.any { it.hours > 0.0 })

        // Same range + date: a refresh must NOT blank the chart while it reloads.
        val gate = CompletableDeferred<List<SleepData>>()
        coEvery { repo.loadSleepSessions(any(), any()) } coAnswers { gate.await() }
        vm.resumeCurrentPeriod(refreshCurrent = true)

        val midLoad = vm.uiState.value
        assertTrue(midLoad.isLoading)
        assertEquals(loaded.durationPoints, midLoad.display.durationPoints)

        gate.complete(listOf(localNight(today, 8.0)))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.display.durationPoints.any { it.hours > 0.0 })
    }

    @Test fun `a stale load cannot overwrite the newer one it lost to`() = runTest {
        val slow = CompletableDeferred<List<SleepData>>()
        val repo = emptyRepo()
        coEvery { repo.loadSleepSessions(any(), any()) } coAnswers { slow.await() }
        val vm = SleepViewModel(repo, dispatchers = mainDispatcherRule.dispatcherProvider)
        vm.selectDate(pastAnchor)

        // The week load is still on the wire; navigate to the day range before it lands.
        coEvery { repo.loadSleepSessions(any(), any()) } returns listOf(localNight(pastAnchor, 6.0))
        vm.selectRange(TimeRange.DAY)
        advanceUntilIdle()

        // The week's late answer is dropped, not painted.
        slow.complete(listOf(localNight(pastAnchor, 11.0)))
        advanceUntilIdle()

        val state = vm.uiState.value
        assertEquals(TimeRange.DAY, state.selectedRange)
        assertEquals(1, state.display.durationPoints.size)
        assertEquals(6.0, state.display.durationPoints.single().hours, 1e-6)
    }

    @Test fun `a permission failure becomes ScreenError PermissionDenied`() = runTest {
        val repo = mockk<SleepRepository>()
        coEvery { repo.loadSleepPeriod(any(), any()) } throws SecurityException("sleep read")

        val vm = SleepViewModel(repo, dispatchers = mainDispatcherRule.dispatcherProvider)

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(ScreenError.PermissionDenied, state.error)
        // Nothing landed for the screen to draw behind the callout.
        assertTrue(state.sessions.isEmpty())
    }

    @Test fun `load failure sets error message`() = runTest {
        val repo = mockk<SleepRepository>()
        coEvery { repo.loadSleepPeriod(any(), any()) } throws RuntimeException("offline")

        val vm = SleepViewModel(repo, dispatchers = mainDispatcherRule.dispatcherProvider)

        assertEquals(ScreenError.Message("offline"), vm.uiState.value.error)
        assertFalse(vm.uiState.value.isLoading)
    }

    // selectRange.

    @Test fun `selectRange updates range and triggers load`() = runTest {
        val repo = emptyRepo()
        val vm = SleepViewModel(repo, dispatchers = mainDispatcherRule.dispatcherProvider)
        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
        coVerify(atLeast = 2) { repo.loadSleepPeriod(any(), any()) }
    }

    @Test fun `initial non-midnight sleep range loads the previous day too`() = runTest {
        val repo = emptyRepo()

        SleepViewModel(
            repository = repo,
            initialRange = TimeRange.DAY,
            initialSleepWindow = SleepWindow.Default,
            dispatchers = mainDispatcherRule.dispatcherProvider,
        )

        coVerify { repo.loadSleepSessions(today.minusDays(1), today) }
    }

    // previousPeriod.

    @Test fun `previousPeriod DAY moves back one day`() = runTest {
        val vm = SleepViewModel(emptyRepo(), dispatchers = mainDispatcherRule.dispatcherProvider)
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusDays(1), vm.uiState.value.selectedDate)
    }

    @Test fun `previousPeriod WEEK moves back one week`() = runTest {
        val vm = SleepViewModel(emptyRepo(), dispatchers = mainDispatcherRule.dispatcherProvider)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `previousPeriod MONTH moves back one month`() = runTest {
        val vm = SleepViewModel(emptyRepo(), dispatchers = mainDispatcherRule.dispatcherProvider)
        vm.selectRange(TimeRange.MONTH)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusMonths(1), vm.uiState.value.selectedDate)
    }

    @Test fun `previousPeriod YEAR moves back one year`() = runTest {
        val vm = SleepViewModel(emptyRepo(), dispatchers = mainDispatcherRule.dispatcherProvider)
        vm.selectRange(TimeRange.YEAR)
        val before = vm.uiState.value.selectedDate
        vm.previousPeriod()
        assertEquals(before.minusYears(1), vm.uiState.value.selectedDate)
    }

    // nextPeriod.

    @Test fun `nextPeriod DAY is blocked when selectedDate is today`() = runTest {
        val vm = SleepViewModel(emptyRepo(), dispatchers = mainDispatcherRule.dispatcherProvider)
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before, vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod DAY advances from a past day`() = runTest {
        val vm = SleepViewModel(emptyRepo(), dispatchers = mainDispatcherRule.dispatcherProvider)
        vm.selectRange(TimeRange.DAY)
        vm.selectDate(today.minusDays(2))
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusDays(1), vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod WEEK advances from a past anchor`() = runTest {
        val vm = SleepViewModel(emptyRepo(), dispatchers = mainDispatcherRule.dispatcherProvider)
        vm.selectDate(pastAnchor)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusWeeks(1), vm.uiState.value.selectedDate)
    }

    // selectDate.

    @Test fun `selectDate clamps future date to today`() = runTest {
        val vm = SleepViewModel(emptyRepo(), dispatchers = mainDispatcherRule.dispatcherProvider)
        vm.selectDate(today.plusDays(5))
        assertEquals(today, vm.uiState.value.selectedDate)
    }

    @Test fun `selectDate accepts past date unchanged`() = runTest {
        val vm = SleepViewModel(emptyRepo(), dispatchers = mainDispatcherRule.dispatcherProvider)
        vm.selectDate(pastAnchor)
        assertEquals(pastAnchor, vm.uiState.value.selectedDate)
    }

    // Daily goal.

    @Test fun `the goal steppers move and persist the sleep target`() = runTest {
        val persisted = mutableListOf<Double>()
        val vm = SleepViewModel(
            emptyRepo(),
            dispatchers = mainDispatcherRule.dispatcherProvider,
            onDailyGoalChanged = { persisted += it },
        )

        // The sleep goal defaults to 8 h and steps by a quarter hour.
        assertEquals(8.0, vm.uiState.value.dailyGoalHours, 0.0001)

        vm.increaseDailyGoal()
        assertEquals(8.25, vm.uiState.value.dailyGoalHours, 0.0001)

        vm.decreaseDailyGoal()
        vm.decreaseDailyGoal()
        assertEquals(7.75, vm.uiState.value.dailyGoalHours, 0.0001)

        // ...and every step is persisted, not just held on screen.
        assertEquals(listOf(8.25, 8.0, 7.75), persisted)
    }
}
