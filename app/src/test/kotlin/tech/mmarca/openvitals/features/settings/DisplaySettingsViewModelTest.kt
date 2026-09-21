package tech.mmarca.openvitals.features.settings

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import tech.mmarca.openvitals.data.repository.PreferencesRepository
import tech.mmarca.openvitals.domain.preferences.ActivityWeekMode
import tech.mmarca.openvitals.domain.preferences.AppLanguage
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.ChartAggregationMode
import tech.mmarca.openvitals.domain.preferences.HomeWidgetRefreshInterval
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.preferences.UnitSystemPreference
import tech.mmarca.openvitals.features.homewidgets.HomeWidgetRefreshScheduler
import tech.mmarca.openvitals.util.MainDispatcherRule

@OptIn(ExperimentalCoroutinesApi::class)
class DisplaySettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test fun `selectAppLanguage persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.selectAppLanguage(AppLanguage.SPANISH)

        verify { prefs.appLanguage = AppLanguage.SPANISH }
        assertEquals(AppLanguage.SPANISH, vm.uiState.value.appLanguage)
    }

    @Test fun `selectAppThemeMode persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.selectAppThemeMode(AppThemeMode.AMOLED)

        verify { prefs.appThemeMode = AppThemeMode.AMOLED }
        assertEquals(AppThemeMode.AMOLED, vm.uiState.value.appThemeMode)
    }

    @Test fun `selectUnitSystem persists preference and re-reads the resolved system`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.selectUnitSystem(UnitSystemPreference.IMPERIAL)

        verify { prefs.unitSystemPreference = UnitSystemPreference.IMPERIAL }
        assertEquals(UnitSystemPreference.IMPERIAL, vm.uiState.value.unitSystemPreference)
        // The displayed system is what the repository resolved; the fake resolves to metric.
        assertEquals(UnitSystem.METRIC, vm.uiState.value.unitSystem)
    }

    @Test fun `selectUnitOverride persists and resolves through ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.selectUnitOverride(UnitQuantity.WEIGHT, UnitSystem.IMPERIAL)

        verify { prefs.setUnitOverride(UnitQuantity.WEIGHT, UnitSystem.IMPERIAL) }
        assertEquals(
            mapOf(UnitQuantity.WEIGHT to UnitSystem.IMPERIAL),
            vm.uiState.value.unitOverrides,
        )
        // The override wins for its quantity; everything else stays on the base.
        assertEquals(UnitSystem.IMPERIAL, vm.uiState.value.effectiveUnitSystem(UnitQuantity.WEIGHT))
        assertEquals(UnitSystem.METRIC, vm.uiState.value.effectiveUnitSystem(UnitQuantity.DISTANCE))

        vm.selectUnitOverride(UnitQuantity.WEIGHT, null)
        assertTrue(vm.uiState.value.unitOverrides.isEmpty())
    }

    @Test fun `setDynamicColor persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.setDynamicColor(true)

        verify { prefs.dynamicColor = true }
        assertTrue(vm.uiState.value.dynamicColor)
    }

    @Test fun `setChartAggregationMode persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.setChartAggregationMode(ChartAggregationMode.MIN10)

        verify { prefs.chartAggregationMode = ChartAggregationMode.MIN10 }
        assertEquals(ChartAggregationMode.MIN10, vm.uiState.value.chartAggregationMode)
    }

    @Test fun `setDashboardSortEmptyTilesLast persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.setDashboardSortEmptyTilesLast(false)

        verify { prefs.dashboardSortEmptyTilesLast = false }
        assertFalse(vm.uiState.value.dashboardSortEmptyTilesLast)
    }

    @Test fun `setHomeWidgetRefreshInterval hands the choice to the scheduler and updates ui state`() = runTest {
        val scheduler = mockk<HomeWidgetRefreshScheduler>(relaxed = true)
        val vm = viewModel(homeWidgetRefreshScheduler = scheduler)
        assertEquals(HomeWidgetRefreshInterval.DEFAULT, vm.uiState.value.homeWidgetRefreshInterval)

        vm.setHomeWidgetRefreshInterval(HomeWidgetRefreshInterval.EVERY_15_MINUTES)

        // The scheduler stores the preference; the view model must not write it a second time.
        verify(exactly = 1) { scheduler.setInterval(HomeWidgetRefreshInterval.EVERY_15_MINUTES) }
        assertEquals(HomeWidgetRefreshInterval.EVERY_15_MINUTES, vm.uiState.value.homeWidgetRefreshInterval)
    }

    @Test fun `selectActivityWeekMode persists preference and updates ui state`() = runTest {
        val prefs = prefs()
        val vm = viewModel(preferencesRepository = prefs)

        vm.selectActivityWeekMode(ActivityWeekMode.LAST_7_DAYS)

        verify { prefs.activityWeekMode = ActivityWeekMode.LAST_7_DAYS }
        assertEquals(ActivityWeekMode.LAST_7_DAYS, vm.uiState.value.activityWeekMode)
    }

    private fun viewModel(
        preferencesRepository: PreferencesRepository = prefs(),
        homeWidgetRefreshScheduler: HomeWidgetRefreshScheduler = mockk(relaxed = true),
    ): DisplaySettingsViewModel =
        DisplaySettingsViewModel(
            preferencesRepository = preferencesRepository,
            homeWidgetRefreshScheduler = homeWidgetRefreshScheduler,
        )

    private fun prefs(): PreferencesRepository =
        mockk<PreferencesRepository>().also { prefs ->
            every { prefs.unitSystemPreference } returns UnitSystemPreference.SYSTEM
            every { prefs.unitSystemPreference = any() } just runs
            every { prefs.unitSystem } returns UnitSystem.METRIC
            var unitOverrides = mapOf<UnitQuantity, UnitSystem>()
            every { prefs.unitOverridesFlow } answers { MutableStateFlow(unitOverrides) }
            every { prefs.unitOverride(any()) } answers { unitOverrides[firstArg()] }
            every { prefs.setUnitOverride(any(), any()) } answers {
                unitOverrides = unitOverrides + (firstArg<UnitQuantity>() to secondArg<UnitSystem>())
            }
            every { prefs.setUnitOverride(any(), isNull()) } answers {
                unitOverrides = unitOverrides - firstArg<UnitQuantity>()
            }
            every { prefs.appLanguage } returns AppLanguage.SYSTEM
            every { prefs.appLanguage = any() } just runs
            every { prefs.appThemeMode } returns AppThemeMode.SYSTEM
            every { prefs.appThemeMode = any() } just runs
            every { prefs.dynamicColor } returns false
            every { prefs.dynamicColor = any() } just runs
            every { prefs.chartAggregationMode } returns ChartAggregationMode.OFF
            every { prefs.chartAggregationMode = any() } just runs
            every { prefs.dashboardSortEmptyTilesLast } returns true
            every { prefs.dashboardSortEmptyTilesLast = any() } just runs
            every { prefs.homeWidgetRefreshInterval } returns HomeWidgetRefreshInterval.DEFAULT
            every { prefs.activityWeekMode } returns ActivityWeekMode.MONDAY_TO_SUNDAY
            every { prefs.activityWeekMode = any() } just runs
        }
}
