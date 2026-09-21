package tech.mmarca.openvitals.features.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@Immutable
data class DisplaySettingsUiState(
    val appLanguage: AppLanguage = AppLanguage.SYSTEM,
    val unitSystemPreference: UnitSystemPreference = UnitSystemPreference.SYSTEM,
    /** Already resolved: never carries the SYSTEM preference itself. */
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    /** Per-quantity display overrides; an absent quantity follows [unitSystem]. */
    val unitOverrides: Map<UnitQuantity, UnitSystem> = emptyMap(),
    val appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val chartAggregationMode: ChartAggregationMode = ChartAggregationMode.OFF,
    val dashboardSortEmptyTilesLast: Boolean = true,
    val homeWidgetRefreshInterval: HomeWidgetRefreshInterval = HomeWidgetRefreshInterval.DEFAULT,
    val activityWeekMode: ActivityWeekMode = ActivityWeekMode.MONDAY_TO_SUNDAY,
) {
    /** What one quantity displays in: its override, else the resolved base. */
    fun effectiveUnitSystem(quantity: UnitQuantity): UnitSystem =
        unitOverrides[quantity] ?: unitSystem
}

/** The Display section: language, units, theme, and how charts, the dashboard and the week read. */
@HiltViewModel
class DisplaySettingsViewModel @Inject constructor(
    private val preferencesRepository: PreferencesRepository,
    private val homeWidgetRefreshScheduler: HomeWidgetRefreshScheduler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(read())
    val uiState: StateFlow<DisplaySettingsUiState> = _uiState.asStateFlow()

    /** The resolved unit follows the system locale, so a return to the screen re-reads it. */
    fun refresh() {
        _uiState.value = read()
    }

    private fun read(): DisplaySettingsUiState = DisplaySettingsUiState(
        appLanguage = preferencesRepository.appLanguage,
        unitSystemPreference = preferencesRepository.unitSystemPreference,
        unitSystem = preferencesRepository.unitSystem,
        unitOverrides = preferencesRepository.unitOverridesFlow.value,
        appThemeMode = preferencesRepository.appThemeMode,
        dynamicColor = preferencesRepository.dynamicColor,
        chartAggregationMode = preferencesRepository.chartAggregationMode,
        dashboardSortEmptyTilesLast = preferencesRepository.dashboardSortEmptyTilesLast,
        homeWidgetRefreshInterval = preferencesRepository.homeWidgetRefreshInterval,
        activityWeekMode = preferencesRepository.activityWeekMode,
    )

    fun selectAppLanguage(appLanguage: AppLanguage) {
        preferencesRepository.appLanguage = appLanguage
        _uiState.value = _uiState.value.copy(appLanguage = appLanguage)
    }

    fun selectUnitSystem(preference: UnitSystemPreference) {
        preferencesRepository.unitSystemPreference = preference
        _uiState.value = _uiState.value.copy(
            unitSystemPreference = preference,
            unitSystem = preferencesRepository.unitSystem,
        )
    }

    fun selectUnitOverride(quantity: UnitQuantity, override: UnitSystem?) {
        preferencesRepository.setUnitOverride(quantity, override)
        _uiState.value = _uiState.value.copy(
            unitOverrides = preferencesRepository.unitOverridesFlow.value,
        )
    }

    fun selectAppThemeMode(appThemeMode: AppThemeMode) {
        preferencesRepository.appThemeMode = appThemeMode
        _uiState.value = _uiState.value.copy(appThemeMode = appThemeMode)
    }

    fun setDynamicColor(enabled: Boolean) {
        preferencesRepository.dynamicColor = enabled
        _uiState.value = _uiState.value.copy(dynamicColor = enabled)
    }

    fun setChartAggregationMode(mode: ChartAggregationMode) {
        preferencesRepository.chartAggregationMode = mode
        _uiState.value = _uiState.value.copy(chartAggregationMode = mode)
    }

    fun setDashboardSortEmptyTilesLast(enabled: Boolean) {
        preferencesRepository.dashboardSortEmptyTilesLast = enabled
        _uiState.value = _uiState.value.copy(dashboardSortEmptyTilesLast = enabled)
    }

    /** The scheduler stores the choice: it also re-plans the running work. */
    fun setHomeWidgetRefreshInterval(interval: HomeWidgetRefreshInterval) {
        homeWidgetRefreshScheduler.setInterval(interval)
        _uiState.value = _uiState.value.copy(homeWidgetRefreshInterval = interval)
    }

    fun selectActivityWeekMode(activityWeekMode: ActivityWeekMode) {
        preferencesRepository.activityWeekMode = activityWeekMode
        _uiState.value = _uiState.value.copy(activityWeekMode = activityWeekMode)
    }
}
