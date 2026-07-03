package tech.mmarca.openvitals.features.caffeine

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.data.repository.contract.CaffeineRepository
import tech.mmarca.openvitals.domain.model.CaffeineEntry
import tech.mmarca.openvitals.domain.model.CaffeinePeriodData
import tech.mmarca.openvitals.domain.model.RefreshMode
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

        assertTrue(preferences.flow.value.profileCompleted)
        assertEquals(CaffeinePreferences.DefaultHalfLifeMinutes, preferences.flow.value.halfLifeMinutes)
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

        preferences.flow.value = preferences.flow.value.copy(sleepThresholdMg = 35)
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
                RefreshMode.NORMAL,
            )
        }
    }

    @Test
    fun `refresh reloads with force mode`() = runTest {
        val repository = repo()
        val vm = viewModel(
            repository = repository,
            preferences = prefs(CaffeinePreferences(profileCompleted = true)),
        )

        vm.refresh()

        coVerify { repository.loadCaffeineData(any(), RefreshMode.FORCE) }
    }

    @Test
    fun `newer load wins when analytics range requests overlap`() = runTest {
        val repository = mockk<CaffeineRepository>()
        coEvery { repository.loadCaffeineData(any(), any()) } coAnswers {
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

    private fun viewModel(
        repository: CaffeineRepository,
        preferences: PreferencesFixture,
        initialAnalyticsRange: CaffeineAnalyticsRange = CaffeineAnalyticsRange.LAST_30_DAYS,
    ): CaffeineViewModel =
        CaffeineViewModel(
            repository = repository,
            preferencesRepository = preferences.repository,
            dispatchers = mainDispatcherRule.dispatcherProvider,
            initialAnalyticsRange = initialAnalyticsRange,
            preferenceChanges = preferences.flow,
        )

    private fun repo(entries: List<CaffeineEntry> = emptyList()): CaffeineRepository =
        mockk<CaffeineRepository>().also { repository ->
            coEvery { repository.loadCaffeineData(any(), any()) } returns CaffeinePeriodData(entries)
        }

    private fun prefs(initial: CaffeinePreferences): PreferencesFixture {
        val flow = MutableStateFlow(initial)
        val repository = mockk<PreferencesRepository>().also { prefs ->
            every { prefs.caffeinePreferences() } answers { flow.value }
            every { prefs.setCaffeinePreferences(any()) } answers {
                flow.value = firstArg<CaffeinePreferences>()
            }
        }
        return PreferencesFixture(repository = repository, flow = flow)
    }

    private fun entryAt(
        date: LocalDate,
        id: String = "coffee",
        caffeineMg: Double = 100.0,
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
        )
    }

    private data class PreferencesFixture(
        val repository: PreferencesRepository,
        val flow: MutableStateFlow<CaffeinePreferences>,
    )
}
