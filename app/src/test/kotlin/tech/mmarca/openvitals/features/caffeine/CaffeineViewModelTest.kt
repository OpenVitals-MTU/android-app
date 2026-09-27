package tech.mmarca.openvitals.features.caffeine

import tech.mmarca.openvitals.data.repository.contract.FakePreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import androidx.lifecycle.SavedStateHandle
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.PeriodLoadQuery
import tech.mmarca.openvitals.core.period.PeriodRangePreferenceKey
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.contract.CaffeineRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeinePeriodData
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
import tech.mmarca.openvitals.navigation.CAFFEINE_ENTRY_ID_ARG
import tech.mmarca.openvitals.navigation.SELECTED_DAY_ARG
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class CaffeineViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val today = LocalDate.now()

    @Test
    fun `first load shows setup when caffeine exists and profile is incomplete`() = runTest {
        val vm = viewModel(
            repository = repo(entries = listOf(entryAt(today))),
            preferences = prefs(CaffeinePreferences(profileCompleted = false)),
        )

        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.showSetup)
        assertEquals(100.0, vm.uiState.value.display.todayTotalMg, 0.001)
    }

    @Test
    fun `skipSetup stores completed defaults and hides setup`() = runTest {
        val preferences = prefs(CaffeinePreferences(profileCompleted = false))
        val vm = viewModel(
            repository = repo(entries = listOf(entryAt(today))),
            preferences = preferences,
        )

        vm.skipSetup()
        advanceUntilIdle()

        assertTrue(preferences.caffeinePreferences().profileCompleted)
        assertEquals(CaffeinePreferences.DefaultHalfLifeMinutes, preferences.caffeinePreferences().halfLifeMinutes)
        assertFalse(vm.uiState.value.showSetup)
    }

    @Test
    fun `preference updates rebuild display`() = runTest {
        val preferences = prefs(CaffeinePreferences(profileCompleted = true, sleepThresholdMg = 60))
        val vm = viewModel(
            repository = repo(entries = listOf(entryAt(today))),
            preferences = preferences,
        )

        preferences.setCaffeinePreferences(preferences.caffeinePreferences().copy(sleepThresholdMg = 35))
        advanceUntilIdle()

        assertEquals(35, vm.uiState.value.preferences.sleepThresholdMg)
        assertEquals(35, vm.uiState.value.display.sleepThresholdMg)
    }

    @Test
    fun `the screen opens on the saved range and a new range is saved and loaded`() = runTest {
        val repository = repo()
        val preferences = prefs(CaffeinePreferences(profileCompleted = true))
        val vm = viewModel(repository = repository, preferences = preferences)

        // The day first: active caffeine and tonight are why the screen is opened.
        assertEquals(TimeRange.DAY, vm.uiState.value.selectedRange)

        vm.selectRange(TimeRange.MONTH)

        assertEquals(TimeRange.MONTH, vm.uiState.value.selectedRange)
        assertEquals(TimeRange.MONTH, preferences.timeRangeFor(PeriodRangePreferenceKey.CAFFEINE))
        coVerify { repository.loadCaffeinePeriod(match { it.range == TimeRange.MONTH }) }
    }

    @Test
    fun `the drink route opens on the drink's day and leaves the saved range alone`() = runTest {
        val repository = repo()
        val preferences = prefs(CaffeinePreferences(profileCompleted = true), initialRange = TimeRange.YEAR)
        val drinkDay = today.minusDays(40)
        val vm = viewModel(
            repository = repository,
            preferences = preferences,
            savedStateHandle = SavedStateHandle(
                mapOf(CAFFEINE_ENTRY_ID_ARG to "coffee", SELECTED_DAY_ARG to drinkDay.toString()),
            ),
        )

        assertEquals(TimeRange.DAY, vm.uiState.value.selectedRange)
        assertEquals(drinkDay, vm.uiState.value.selectedDate)
        assertEquals(TimeRange.YEAR, preferences.timeRangeFor(PeriodRangePreferenceKey.CAFFEINE))
        coVerify {
            repository.loadCaffeinePeriod(
                match { it.range == TimeRange.DAY && it.windows.current == DatePeriod(drinkDay, drinkDay) },
            )
        }
    }

    @Test
    fun `stepping back a period loads the one before`() = runTest {
        val repository = repo()
        val vm = viewModel(
            repository = repository,
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
        )

        vm.previousPeriod()

        assertEquals(today.minusDays(1), vm.uiState.value.selectedDate)
        coVerify {
            repository.loadCaffeinePeriod(
                match { it.windows.current == DatePeriod(today.minusDays(1), today.minusDays(1)) },
            )
        }
    }

    @Test
    fun `the previous period's total becomes the comparison`() = runTest {
        val repository = mockk<CaffeineRepository>()
        coEvery { repository.loadCaffeinePeriod(any()) } returns CaffeinePeriodData(
            entries = listOf(entryAt(today)),
            previousTotalMg = 50.0,
        )
        val vm = viewModel(
            repository = repository,
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
        )

        assertEquals(100.0, vm.uiState.value.periodComparison.currentValue, 0.001)
        assertEquals(50.0, vm.uiState.value.periodComparison.previousValue, 0.001)
    }

    @Test
    fun `load reloads`() = runTest {
        val repository = repo()
        val vm = viewModel(
            repository = repository,
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
        )

        vm.load()

        coVerify(atLeast = 2) { repository.loadCaffeinePeriod(any()) }
    }

    @Test
    fun `newer load wins when range requests overlap`() = runTest {
        val repository = mockk<CaffeineRepository>()
        coEvery { repository.loadCaffeinePeriod(any()) } coAnswers {
            val query = firstArg<PeriodLoadQuery>()
            if (query.range == TimeRange.YEAR) {
                delay(100)
            }
            CaffeinePeriodData(
                entries = listOf(entryAt(query.windows.current.start, id = query.range.name))
            )
        }
        val vm = viewModel(
            repository = repository,
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
        )

        vm.selectRange(TimeRange.YEAR)
        vm.selectRange(TimeRange.DAY)
        advanceUntilIdle()

        assertEquals(TimeRange.DAY, vm.uiState.value.selectedRange)
        assertEquals(TimeRange.DAY.name, vm.uiState.value.entries.single().id)
    }

    @Test
    fun `deleting a drink removes it optimistically and force-reloads`() = runTest {
        val entries = listOf(
            entryAt(today, id = "a", isOpenVitalsEntry = true),
            entryAt(today, id = "b", isOpenVitalsEntry = true),
        )
        val repository = mockk<CaffeineRepository>()
        // The reload returns the trimmed list, as Health Connect would after the delete.
        coEvery { repository.loadCaffeinePeriod(any()) } returnsMany listOf(
            CaffeinePeriodData(entries),
            CaffeinePeriodData(entries.drop(1)),
        )
        val nutrition = mockk<NutritionRepository>()
        coEvery { nutrition.deleteNutritionEntry("a") } returns Unit

        val vm = viewModel(
            repository = repository,
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
            nutritionRepository = nutrition,
        )
        vm.deleteCaffeineEntry("a")

        assertEquals(listOf("b"), vm.uiState.value.entries.map { it.id })
        assertNull(vm.uiState.value.error)
        // A caffeine entry IS a nutrition record, so the nutrition repository is what deletes.
        coVerify { nutrition.deleteNutritionEntry("a") }
        coVerify(atLeast = 2) { repository.loadCaffeinePeriod(any()) }
    }

    @Test
    fun `a failed delete restores the drink and surfaces the error`() = runTest {
        val entries = listOf(
            entryAt(today, id = "a", isOpenVitalsEntry = true),
            entryAt(today, id = "b", isOpenVitalsEntry = true),
        )
        val nutrition = mockk<NutritionRepository>()
        coEvery { nutrition.deleteNutritionEntry("a") } throws RuntimeException("denied")

        val vm = viewModel(
            repository = repo(entries = entries),
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
            nutritionRepository = nutrition,
        )
        vm.deleteCaffeineEntry("a")

        assertEquals(listOf("a", "b"), vm.uiState.value.entries.map { it.id })
        assertEquals(ScreenError.Message("denied"), vm.uiState.value.error)
    }

    @Test
    fun `a foreign or unidentified drink is never deleted`() = runTest {
        val nutrition = mockk<NutritionRepository>()
        val vm = viewModel(
            repository = repo(entries = listOf(entryAt(today, id = "foreign"))),
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
            nutritionRepository = nutrition,
        )

        vm.deleteCaffeineEntry("foreign")
        vm.deleteCaffeineEntry("")
        vm.deleteCaffeineEntry("missing")

        assertEquals(listOf("foreign"), vm.uiState.value.entries.map { it.id })
        coVerify(exactly = 0) { nutrition.deleteNutritionEntry(any()) }
    }

    @Test
    fun `a permission failure becomes ScreenError PermissionDenied`() = runTest {
        val repository = mockk<CaffeineRepository>()
        coEvery {
            repository.loadCaffeinePeriod(any())
        } throws SecurityException("nutrition read")

        val vm = viewModel(
            repository = repository,
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.PermissionDenied, vm.uiState.value.error)
        assertTrue(vm.uiState.value.display.curvePoints.isEmpty())
    }

    @Test
    fun `an unexpected failure carries its message to the screen`() = runTest {
        val repository = mockk<CaffeineRepository>()
        coEvery {
            repository.loadCaffeinePeriod(any())
        } throws RuntimeException("the provider hung up")

        val vm = viewModel(
            repository = repository,
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(ScreenError.Message("the provider hung up"), vm.uiState.value.error)
        // A failed load leaves the default insights in place, so there is nothing to draw.
        assertTrue(vm.uiState.value.display.curvePoints.isEmpty())
    }

    @Test
    fun `an empty load still gives the screen a display to render`() = runTest {
        val vm = viewModel(
            repository = repo(entries = emptyList()),
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
        )
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(CaffeineSleepImpactStatus.UNLIKELY, caffeineSleepImpactStatus(state.display))
        assertTrue(caffeineDistributionBars(state.display.sourceTotals).isEmpty())
        // The curve is still plotted as a flat zero line, and the threshold line has to fit.
        assertTrue(state.display.curvePoints.isNotEmpty())
        assertTrue(
            caffeineCurveMaxMg(
                points = state.display.curvePoints,
                thresholdMg = state.display.sleepThresholdMg.toDouble(),
            ) > 0.0
        )
    }

    private fun viewModel(
        repository: CaffeineRepository,
        preferences: FakePreferences,
        nutritionRepository: NutritionRepository = mockk(),
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
    ): CaffeineViewModel =
        CaffeineViewModel(
            repository = repository,
            caffeineModel = preferences,
            bodyProfilePreferences = preferences,
            nutritionRepository = nutritionRepository,
            periodPreferences = preferences,
            dispatchers = mainDispatcherRule.dispatcherProvider,
            savedStateHandle = savedStateHandle,
        )

    private fun repo(entries: List<CaffeineEntry> = emptyList()): CaffeineRepository =
        mockk<CaffeineRepository>().also { repository ->
            coEvery { repository.loadCaffeinePeriod(any()) } returns CaffeinePeriodData(entries)
        }

    private fun prefs(initial: CaffeinePreferences, initialRange: TimeRange? = null): FakePreferences =
        FakePreferences(initialRange = initialRange, initialCaffeine = initial)

    private fun entryAt(
        date: LocalDate,
        id: String = "coffee",
        caffeineMg: Double = 100.0,
        isOpenVitalsEntry: Boolean = false,
    ): CaffeineEntry {
        val start = date.atTime(8, 0).atZone(ZoneId.systemDefault()).toInstant()
        return CaffeineEntry(
            id = id,
            startTime = start,
            endTime = start.plusSeconds(10 * 60L),
            caffeineMg = caffeineMg,
            name = "Coffee",
            source = "test.source",
            mealType = 0,
            isOpenVitalsEntry = isOpenVitalsEntry,
        )
    }
}
