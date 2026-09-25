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
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.data.repository.contract.CaffeineRepository
import tech.mmarca.openvitals.data.repository.contract.NutritionRepository
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeinePeriodData
import tech.mmarca.openvitals.domain.preferences.CaffeinePreferences
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
            initialAnalyticsRange = CaffeineAnalyticsRange.TODAY,
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
            initialAnalyticsRange = CaffeineAnalyticsRange.TODAY,
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
            initialAnalyticsRange = CaffeineAnalyticsRange.TODAY,
        )

        preferences.setCaffeinePreferences(preferences.caffeinePreferences().copy(sleepThresholdMg = 35))
        advanceUntilIdle()

        assertEquals(35, vm.uiState.value.preferences.sleepThresholdMg)
        assertEquals(35, vm.uiState.value.display.sleepThresholdMg)
    }

    @Test
    fun `analytics range selection reloads matching caffeine window`() = runTest {
        val repository = repo()
        val vm = viewModel(
            repository = repository,
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
            initialAnalyticsRange = CaffeineAnalyticsRange.TODAY,
        )

        vm.selectAnalyticsRange(CaffeineAnalyticsRange.LAST_90_DAYS)

        assertEquals(CaffeineAnalyticsRange.LAST_90_DAYS, vm.uiState.value.analyticsRange)
        coVerify {
            repository.loadCaffeineData(
                DatePeriod(today.minusDays(89), today),
            )
        }

        // The same range again is not a reload.
        vm.selectAnalyticsRange(CaffeineAnalyticsRange.LAST_90_DAYS)
        advanceUntilIdle()

        coVerify(exactly = 1) {
            repository.loadCaffeineData(
                DatePeriod(today.minusDays(89), today),
            )
        }
    }

    @Test
    fun `refresh reloads`() = runTest {
        val repository = repo()
        val vm = viewModel(
            repository = repository,
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
        )

        vm.refresh()

        coVerify(atLeast = 2) { repository.loadCaffeineData(any()) }
    }

    @Test
    fun `newer load wins when analytics range requests overlap`() = runTest {
        val repository = mockk<CaffeineRepository>()
        coEvery { repository.loadCaffeineData(any()) } coAnswers {
            val period = firstArg<DatePeriod>()
            if (period.start == today.minusDays(89)) {
                delay(100)
            }
            CaffeinePeriodData(
                entries = listOf(entryAt(period.start, id = period.start.toString()))
            )
        }
        val vm = viewModel(
            repository = repository,
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
            initialAnalyticsRange = CaffeineAnalyticsRange.TODAY,
        )

        vm.selectAnalyticsRange(CaffeineAnalyticsRange.LAST_90_DAYS)
        vm.selectAnalyticsRange(CaffeineAnalyticsRange.TODAY)

        assertEquals(CaffeineAnalyticsRange.TODAY, vm.uiState.value.analyticsRange)
        assertEquals(today.toString(), vm.uiState.value.entries.single().id)
    }

    @Test
    fun `deleting a drink removes it optimistically and force-reloads`() = runTest {
        val entries = listOf(
            entryAt(today, id = "a", isOpenVitalsEntry = true),
            entryAt(today, id = "b", isOpenVitalsEntry = true),
        )
        val repository = mockk<CaffeineRepository>()
        // The reload returns the trimmed list, as Health Connect would after the delete.
        coEvery { repository.loadCaffeineData(any()) } returnsMany listOf(
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
        coVerify(atLeast = 2) { repository.loadCaffeineData(any()) }
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
            repository.loadCaffeineData(any())
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
            repository.loadCaffeineData(any())
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
        assertTrue(caffeineDistributionBars(state.analyticsDisplay.sourceTotals).isEmpty())
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
        initialAnalyticsRange: CaffeineAnalyticsRange = CaffeineAnalyticsRange.LAST_30_DAYS,
        nutritionRepository: NutritionRepository = mockk(),
    ): CaffeineViewModel =
        CaffeineViewModel(
            repository = repository,
            caffeineModel = preferences,
            bodyProfilePreferences = preferences,
            nutritionRepository = nutritionRepository,
            dispatchers = mainDispatcherRule.dispatcherProvider,
        ).also { vm ->
            // The screen opens on 30 days. A test that wants another range moves it, as the user would.
            if (initialAnalyticsRange != CaffeineAnalyticsRange.LAST_30_DAYS) vm.selectAnalyticsRange(initialAnalyticsRange)
        }

    private fun repo(entries: List<CaffeineEntry> = emptyList()): CaffeineRepository =
        mockk<CaffeineRepository>().also { repository ->
            coEvery { repository.loadCaffeineData(any()) } returns CaffeinePeriodData(entries)
        }

    private fun prefs(initial: CaffeinePreferences): FakePreferences =
        FakePreferences(initialCaffeine = initial)

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
