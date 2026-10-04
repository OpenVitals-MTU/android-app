package tech.mmarca.openvitals.features.cycle

import androidx.lifecycle.SavedStateHandle
import io.mockk.slot
import io.mockk.verify
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertNotNull
import tech.mmarca.openvitals.data.repository.contract.CycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.FakeCycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.FakePillIntakeRepository
import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import tech.mmarca.openvitals.domain.cycle.CycleExclusionReason
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.model.CycleEntryWriteRequest
import tech.mmarca.openvitals.domain.model.CycleJournalEntry
import tech.mmarca.openvitals.domain.model.CycleRecordValues
import tech.mmarca.openvitals.features.cycle.reminders.FakeCycleReminderSettings
import tech.mmarca.openvitals.features.homewidgets.HomeWidgetRefreshScheduler
import tech.mmarca.openvitals.core.presentation.ScreenError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.model.MenstruationFlowEntry
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.query.CyclePeriodData
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class CycleViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val pastAnchor = today.minusMonths(4)

    private fun repo(
        data: CycleData = CycleData(),
        missingPermissions: Set<String> = emptySet(),
    ) = mockk<CycleRepository>().also { repo ->
        every { repo.phase4Permissions } returns setOf("cycle")
        coEvery { repo.missingPermissions() } returns missingPermissions
        coEvery { repo.loadCycleData(any(), any()) } returns data
        coEvery { repo.loadCyclePeriod(any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            val period = query.windows.current
            CyclePeriodData(
                data = repo.loadCycleData(period.start, period.end),
                missingPermissions = repo.missingPermissions(),
            )
        }
    }

    private fun viewModel(
        repository: CycleRepository,
        journal: CycleJournalRepository = FakeCycleJournalRepository(),
        reminders: FakeCycleReminderSettings = FakeCycleReminderSettings(),
        widgets: HomeWidgetRefreshScheduler = mockk(relaxed = true),
    ) = CycleViewModel(
        repository = repository,
        periodPreferences = FakePreferences(),
        journal = journal,
        pillIntakes = FakePillIntakeRepository(),
        cyclePreferences = FakePreferences(),
        bodyProfilePreferences = FakePreferences(),
        reminders = reminders,
        dispatchers = mainDispatcherRule.dispatcherProvider,
        savedStateHandle = SavedStateHandle(),
        homeWidgetRefreshScheduler = widgets,
    )

    @Test fun `initial range is MONTH`() = runTest {
        val vm = viewModel(repo())

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
    }

    @Test fun `initial load clears loading and sets empty data`() = runTest {
        val vm = viewModel(repo())
        val state = vm.uiState.value

        assertFalse(state.isLoading)
        assertFalse(state.data.hasData)
        assertNull(state.error)
    }

    @Test fun `load success populates cycle data and missing permissions`() = runTest {
        val cycleData = CycleData(
            menstruationFlows = listOf(
                MenstruationFlowEntry(
                    time = Instant.now(),
                    flow = 2,
                    source = "test",
                )
            )
        )
        val vm = viewModel(
            repo(
                data = cycleData,
                missingPermissions = setOf("ovulation"),
            )
        )

        assertEquals(cycleData, vm.uiState.value.data)
        assertEquals(setOf("ovulation"), vm.uiState.value.missingPermissions)
        assertTrue(vm.uiState.value.display.hasData)
    }

    @Test fun `initial load requests the current month period`() = runTest {
        val repo = repo()

        viewModel(repo)

        coVerify {
            repo.loadCycleData(today.withDayOfMonth(1), today)
        }
    }

    @Test fun `cyclePermissions exposes repository phase 4 permissions`() = runTest {
        val vm = viewModel(repo())

        assertEquals(setOf("cycle"), vm.cyclePermissions)
    }

    @Test fun `onCyclePermissionsResult refreshes missing permissions`() = runTest {
        val repo = repo(missingPermissions = setOf("cycle"))
        coEvery { repo.missingPermissions() } returnsMany listOf(setOf("cycle"), emptySet())
        val vm = viewModel(repo)

        vm.onCyclePermissionsResult(setOf("cycle"))

        assertTrue(vm.uiState.value.missingPermissions.isEmpty())
    }

    @Test fun `a permission failure becomes ScreenError PermissionDenied`() = runTest {
        // The read itself is refused, and the screen turns it into a grant affordance.
        val repo = mockk<CycleRepository>()
        every { repo.phase4Permissions } returns setOf("cycle")
        coEvery { repo.loadCyclePeriod(any()) } throws SecurityException("cycle read")

        val vm = viewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.PermissionDenied, vm.uiState.value.error)
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<CycleRepository>()
        every { repo.phase4Permissions } returns setOf("cycle")
        coEvery { repo.loadCyclePeriod(any()) } throws RuntimeException("timeout")

        val vm = viewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.Message("timeout"), vm.uiState.value.error)
    }

    @Test fun `previousPeriod MONTH moves back one month`() = runTest {
        val vm = viewModel(repo())
        val before = vm.uiState.value.selectedDate

        vm.previousPeriod()

        assertEquals(before.minusMonths(1), vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod is blocked in the current month`() = runTest {
        val vm = viewModel(repo())
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before, vm.uiState.value.selectedDate)
        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
    }

    @Test fun `nextPeriod MONTH advances from a past month`() = runTest {
        val vm = viewModel(repo())
        vm.selectDate(pastAnchor)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusMonths(1), vm.uiState.value.selectedDate)
    }

    @Test fun `selectDate clamps future date to today`() = runTest {
        val vm = viewModel(repo())

        vm.selectDate(today.plusDays(10))

        assertEquals(today, vm.uiState.value.selectedDate)
    }

    @Test fun `resuming the current period reloads the current selection`() = runTest {
        val repo = repo()
        val vm = viewModel(repo)

        // The selection is already today's period, so the refresh flag is what reloads it.
        vm.resumeCurrentPeriod(refreshCurrent = true)
        advanceUntilIdle()

        assertEquals(today, vm.uiState.value.selectedDate)
        coVerify(atLeast = 2) { repo.loadCyclePeriod(any()) }
    }

    @Test fun `a stale load cannot overwrite the newer one it lost to`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val staleData = CycleData(
            menstruationFlows = listOf(
                MenstruationFlowEntry(time = Instant.ofEpochSecond(1_000), flow = 3, source = "stale"),
            )
        )
        val freshData = CycleData(
            menstruationFlows = listOf(
                MenstruationFlowEntry(time = Instant.ofEpochSecond(2_000), flow = 1, source = "fresh"),
            )
        )
        val repo = mockk<CycleRepository>()
        every { repo.phase4Permissions } returns setOf("cycle")
        val thisMonth = today.withDayOfMonth(1)
        suspend fun answer(query: PeriodLoadQuery): CyclePeriodData =
            if (!query.selectedDate.isBefore(thisMonth)) {
                // Held in flight; by the time it answers, a newer load won.
                gate.await()
                CyclePeriodData(data = staleData, missingPermissions = emptySet())
            } else {
                CyclePeriodData(data = freshData, missingPermissions = emptySet())
            }
        coEvery { repo.loadCyclePeriod(any()) } coAnswers { answer(firstArg()) }

        val vm = viewModel(repo)
        runCurrent()
        vm.previousPeriod()
        advanceUntilIdle()
        gate.complete(Unit)
        advanceUntilIdle()

        // The previous month's load won: this month's late answer is dropped, not painted.
        val state = vm.uiState.value
        assertTrue(state.selectedDate.isBefore(thisMonth))
        assertEquals(freshData, state.data)
        assertEquals(1, state.display.summary.totalEntryCount)
        assertFalse(state.isLoading)
    }

    @Test fun `deleting a journal row drops it at once, re-plans the reminders and redraws the widget`() = runTest {
        val date = today.minusDays(1)
        val journal = FakeCycleJournalRepository(initialEntries = listOf(CycleJournalEntry(date = date, painLevel = 2)))
        val reminders = FakeCycleReminderSettings()
        val widgets = mockk<HomeWidgetRefreshScheduler>(relaxed = true)
        val vm = viewModel(repo(), journal = journal, reminders = reminders, widgets = widgets)

        vm.deleteJournalEntry(date)
        advanceUntilIdle()

        assertNull(journal.entries[date])
        assertEquals(1, reminders.applied)
        verify(exactly = 1) { widgets.refreshNow() }
        assertNull(vm.uiState.value.error)
    }

    @Test fun `a failed journal delete surfaces the error and plans nothing`() = runTest {
        val journal = mockk<CycleJournalRepository> {
            coEvery { delete(any()) } throws IllegalStateException("disk")
        }
        val reminders = FakeCycleReminderSettings()
        val vm = viewModel(repo(), journal = journal, reminders = reminders)

        vm.deleteJournalEntry(today.minusDays(1))
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertEquals(0, reminders.applied)
    }

    @Test fun `deleting an own record asks the repository once and re-plans`() = runTest {
        val flow = MenstruationFlowEntry(time = Instant.now(), flow = 2, source = "OpenVitals", id = "own", isOpenVitalsEntry = true)
        val repo = repo(data = CycleData(menstruationFlows = listOf(flow)))
        coEvery { repo.deleteCycleEntry(any(), any()) } returns Unit
        val reminders = FakeCycleReminderSettings()
        val vm = viewModel(repo, reminders = reminders)

        vm.deleteCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, "own")
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.deleteCycleEntry(CycleEntryKind.MENSTRUATION_FLOW, "own") }
        assertEquals(1, reminders.applied)
    }

    @Test fun `excluding a cycle stores the reason, and including it clears the row`() = runTest {
        val journal = FakeCycleJournalRepository()
        val reminders = FakeCycleReminderSettings()
        val repo = repo()
        val vm = viewModel(repo, journal = journal, reminders = reminders)
        val start = today.minusDays(40)

        vm.setCycleExclusion(start, today.minusDays(12), excluded = true, reason = CycleExclusionReason.ILLNESS)
        advanceUntilIdle()
        assertEquals(CycleExclusionReason.ILLNESS, journal.exclusionsByDate[start])
        assertEquals(1, reminders.applied)

        vm.setCycleExclusion(start, today.minusDays(12), excluded = false, reason = null)
        advanceUntilIdle()
        assertTrue(journal.exclusionsByDate.isEmpty())
        assertEquals(2, reminders.applied)
        // The initial load plus one reload per change.
        coVerify(exactly = 3) { repo.loadCyclePeriod(any()) }
    }

    @Test fun `a past period is one light-flow record at noon`() = runTest {
        val repo = repo()
        val request = slot<CycleEntryWriteRequest>()
        coEvery { repo.writeCycleEntry(capture(request)) } returns "new"
        val reminders = FakeCycleReminderSettings()
        val widgets = mockk<HomeWidgetRefreshScheduler>(relaxed = true)
        val vm = viewModel(repo, reminders = reminders, widgets = widgets)
        val date = today.minusDays(30)

        vm.addPastPeriod(date)
        advanceUntilIdle()

        assertEquals(CycleEntryKind.MENSTRUATION_FLOW, request.captured.kind)
        assertEquals(CycleRecordValues.FLOW_LIGHT, request.captured.flow)
        assertEquals(date.atTime(LocalTime.NOON).atZone(ZoneId.systemDefault()).toInstant(), request.captured.time)
        assertEquals(1, reminders.applied)
        verify(exactly = 1) { widgets.refreshNow() }
    }

    @Test fun `a refused past period surfaces the error and plans nothing`() = runTest {
        val repo = repo()
        coEvery { repo.writeCycleEntry(any()) } throws SecurityException("no write permission")
        val reminders = FakeCycleReminderSettings()
        val vm = viewModel(repo, reminders = reminders)

        vm.addPastPeriod(today.minusDays(30))
        advanceUntilIdle()

        assertNotNull(vm.uiState.value.error)
        assertEquals(0, reminders.applied)
    }
}
