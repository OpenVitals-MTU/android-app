package tech.mmarca.openvitals.features.cycle

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.data.repository.contract.CycleRepository
import tech.mmarca.openvitals.data.repository.contract.FakeCycleJournalRepository
import tech.mmarca.openvitals.data.repository.contract.FakePillIntakeRepository
import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.cycle.NightlyVitals
import tech.mmarca.openvitals.domain.cycle.RecordedCycle
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.domain.query.CyclePeriodData
import tech.mmarca.openvitals.domain.usecase.LoadCycleNightlyVitalsUseCase
import tech.mmarca.openvitals.features.cycle.reminders.FakeCycleReminderSettings
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class CycleViewModelNightlyTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()
    private val start = today.minusDays(5)

    private var currentStart: LocalDate? = start

    private val repository = mockk<CycleRepository>().also { repo ->
        every { repo.phase4Permissions } returns setOf("cycle")
        every { repo.cycleNightlyPermissions } returns setOf("sleep", "hrv")
        coEvery { repo.missingPermissions() } returns emptySet()
        coEvery { repo.missingNightlyPermissions() } returns setOf("hrv")
        coEvery { repo.loadCyclePeriod(any()) } coAnswers {
            val begin = currentStart
            CyclePeriodData(
                data = CycleData(),
                missingPermissions = emptySet(),
                statistics = begin?.let { CycleStatistics(cycles = listOf(RecordedCycle(startDate = it, endDate = null))) },
            )
        }
    }

    private fun nightly(at: LocalDate) = NightlyVitals(cycleStart = at, nightCount = 1, series = emptyList())

    private fun viewModel(useCase: LoadCycleNightlyVitalsUseCase) = CycleViewModel(
        repository = repository,
        periodPreferences = FakePreferences(),
        journal = FakeCycleJournalRepository(),
        pillIntakes = FakePillIntakeRepository(),
        cyclePreferences = FakePreferences(),
        bodyProfilePreferences = FakePreferences(),
        reminders = FakeCycleReminderSettings(),
        dispatchers = mainDispatcherRule.dispatcherProvider,
        savedStateHandle = SavedStateHandle(),
        homeWidgetRefreshScheduler = mockk(relaxed = true),
        nightlyVitals = useCase,
    )

    @Test fun `the nightly values and the missing permissions reach the state`() = runTest {
        val useCase = mockk<LoadCycleNightlyVitalsUseCase>()
        coEvery { useCase(any(), any(), any()) } returns nightly(start)

        val vm = viewModel(useCase)

        assertEquals(nightly(start), vm.uiState.value.nightly)
        assertEquals(setOf("hrv"), vm.uiState.value.missingNightlyPermissions)
        assertEquals(setOf("sleep", "hrv"), vm.nightlyPermissions)
    }

    @Test fun `month navigation does not read the nights again, a refresh does`() = runTest {
        val useCase = mockk<LoadCycleNightlyVitalsUseCase>()
        coEvery { useCase(any(), any(), any()) } returns nightly(start)
        val vm = viewModel(useCase)

        vm.previousPeriod()
        vm.nextPeriod()
        vm.selectDate(today.minusMonths(2))
        coVerify(exactly = 1) { useCase(any(), any(), any()) }

        vm.load()
        coVerify(exactly = 2) { useCase(any(), any(), any()) }
        assertNotNull(vm.uiState.value.nightly)
    }

    @Test fun `a new cycle start is read even on navigation`() = runTest {
        val useCase = mockk<LoadCycleNightlyVitalsUseCase>()
        coEvery { useCase(any(), any(), any()) } answers { nightly(firstArg()) }
        val vm = viewModel(useCase)

        currentStart = today.minusDays(1)
        vm.previousPeriod()

        assertEquals(today.minusDays(1), vm.uiState.value.nightly?.cycleStart)
    }

    @Test fun `losing the current cycle cancels the read in flight and clears the card`() = runTest {
        val gate = CompletableDeferred<NightlyVitals>()
        val useCase = mockk<LoadCycleNightlyVitalsUseCase>()
        coEvery { useCase(any(), any(), any()) } coAnswers { gate.await() }
        val vm = viewModel(useCase)

        currentStart = null
        vm.load()
        // The answer that was on its way must not land on a screen without a cycle.
        gate.complete(nightly(start))

        assertNull(vm.uiState.value.nightly)
        assertEquals(emptySet<String>(), vm.uiState.value.missingNightlyPermissions)
    }

    @Test fun `without the use case the card state stays empty`() = runTest {
        val vm = CycleViewModel(
            repository = repository,
            periodPreferences = FakePreferences(),
            journal = FakeCycleJournalRepository(),
            pillIntakes = FakePillIntakeRepository(),
            cyclePreferences = FakePreferences(),
            bodyProfilePreferences = FakePreferences(),
            reminders = FakeCycleReminderSettings(),
            dispatchers = mainDispatcherRule.dispatcherProvider,
            savedStateHandle = SavedStateHandle(),
        )

        assertNull(vm.uiState.value.nightly)
    }
}
