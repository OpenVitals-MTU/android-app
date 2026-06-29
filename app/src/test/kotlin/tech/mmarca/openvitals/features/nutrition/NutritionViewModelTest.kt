package tech.mmarca.openvitals.features.nutrition

import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.domain.model.DailyMacros
import tech.mmarca.openvitals.domain.model.NutritionEntry
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.domain.query.NutritionPeriodData
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.util.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NutritionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()

    private fun emptyRepo() = mockk<NutritionRepository>().also { repo ->
        coEvery { repo.loadDailyMacros(any(), any()) } returns emptyList()
        coEvery { repo.loadNutritionEntries(any(), any()) } returns emptyList()
        coEvery { repo.loadNutritionPeriod(any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            val windows = query.windows
            NutritionPeriodData(
                dailyMacros = repo.loadDailyMacros(windows.current.start, windows.current.end),
                previousDailyMacros = repo.loadDailyMacros(windows.previous.start, windows.previous.end),
                baselineDailyMacros = repo.loadDailyMacros(windows.baseline.start, windows.baseline.end),
                entries = repo.loadNutritionEntries(windows.current.start, windows.current.end),
            )
        }
    }

    private fun viewModel(
        repo: NutritionRepository,
        selectedMetric: NutritionMetric = NutritionMetric.CALORIES_IN,
        initialRange: TimeRange = TimeRange.WEEK,
    ) = NutritionViewModel(
        repository = repo,
        dispatchers = mainDispatcherRule.dispatcherProvider,
        selectedMetric = selectedMetric,
        initialRange = initialRange,
    )

    @Test fun `initial range is WEEK`() = runTest {
        val vm = viewModel(emptyRepo())
        assertEquals(TimeRange.WEEK, vm.uiState.value.selectedRange)
    }

    @Test fun `initial load clears loading and sets empty lists`() = runTest {
        val vm = viewModel(emptyRepo())
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.dailyMacros.isEmpty())
        assertTrue(state.entries.isEmpty())
    }

    @Test fun `load success populates macros entries and derived totals`() = runTest {
        val macros = listOf(
            DailyMacros(
                date = today.minusDays(1),
                energyKcal = 1_900.0,
                proteinGrams = 90.0,
                carbsGrams = 220.0,
                fatGrams = 60.0,
            ),
            DailyMacros(
                date = today,
                energyKcal = 2_100.0,
                proteinGrams = 100.0,
                carbsGrams = 250.0,
                fatGrams = 70.0,
            ),
        )
        val entries = listOf(
            NutritionEntry(
                time = Instant.now(),
                mealType = 1,
                name = "Breakfast",
                energyKcal = 500.0,
                proteinGrams = 25.0,
                carbsGrams = 60.0,
                fatGrams = 15.0,
                fiberGrams = 8.0,
                sugarGrams = 12.0,
                source = "test",
            )
        )
        val repo = emptyRepo()
        coEvery { repo.loadDailyMacros(any(), any()) } returns macros
        coEvery { repo.loadNutritionEntries(any(), any()) } returns entries

        val vm = viewModel(repo)

        assertEquals(macros, vm.uiState.value.dailyMacros)
        assertEquals(entries, vm.uiState.value.entries)
        assertEquals(4_000.0, vm.uiState.value.display.totals.energyKcal, 0.01)
        assertEquals(190.0, vm.uiState.value.display.totals.proteinGrams, 0.01)
        assertEquals(470.0, vm.uiState.value.display.totals.carbsGrams, 0.01)
        assertEquals(130.0, vm.uiState.value.display.totals.fatGrams, 0.01)
    }

    @Test fun `load failure sets error and clears loading`() = runTest {
        val repo = mockk<NutritionRepository>()
        coEvery { repo.loadNutritionPeriod(any()) } throws RuntimeException("timeout")

        val vm = viewModel(repo)

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.Message("timeout"), vm.uiState.value.error)
    }

    @Test fun `selectRange updates selectedRange and reloads`() = runTest {
        val repo = emptyRepo()
        val vm = viewModel(repo)

        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
        coVerify(atLeast = 2) { repo.loadNutritionPeriod(any()) }
    }

    @Test fun `year range loads raw meal entries`() = runTest {
        val repo = emptyRepo()
        viewModel(repo, initialRange = TimeRange.YEAR)

        coVerify(exactly = 1) { repo.loadNutritionEntries(any(), any()) }
    }

    @Test fun `macro metrics load raw meal entries`() = runTest {
        val repo = emptyRepo()
        viewModel(repo, selectedMetric = NutritionMetric.PROTEIN)

        coVerify(exactly = 1) { repo.loadNutritionEntries(any(), any()) }
    }

    @Test fun `nextPeriod DAY is blocked when selectedDate is today`() = runTest {
        val vm = viewModel(emptyRepo())
        vm.selectRange(TimeRange.DAY)
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before, vm.uiState.value.selectedDate)
    }

    @Test fun `nextPeriod WEEK advances from a past week`() = runTest {
        val vm = viewModel(emptyRepo())
        vm.selectDate(today.minusWeeks(4))
        val before = vm.uiState.value.selectedDate

        vm.nextPeriod()

        assertEquals(before.plusWeeks(1), vm.uiState.value.selectedDate)
    }

    @Test fun `selectDate clamps future date to today`() = runTest {
        val vm = viewModel(emptyRepo())

        vm.selectDate(today.plusDays(10))

        assertEquals(today, vm.uiState.value.selectedDate)
        assertNull(vm.uiState.value.error)
    }
}
