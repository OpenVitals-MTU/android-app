package tech.mmarca.openvitals.features.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/** Display: language, units, theme, charts, the dashboard order and the week. */
@Composable
fun DisplaySettingsScreen(viewModel: DisplaySettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // The resolved unit follows the system locale, which can change while the app is away.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    SettingsSectionList { displaySettingsCards(state, viewModel) }
}

private fun LazyListScope.displaySettingsCards(
    state: DisplaySettingsUiState,
    viewModel: DisplaySettingsViewModel,
) {
    item { SectionHeader(stringResource(SettingsSection.DISPLAY.titleRes)) }
    item {
        LanguageCard(
            selected = state.appLanguage,
            onSelect = viewModel::selectAppLanguage,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        UnitSystemCard(
            selected = state.unitSystemPreference,
            resolvedUnitSystem = state.unitSystem,
            onSelect = viewModel::selectUnitSystem,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        UnitOverridesCard(
            overrides = state.unitOverrides,
            baseUnitSystem = state.unitSystem,
            onSelect = viewModel::selectUnitOverride,
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )
    }
    item { SettingsCardSpacer() }
    item {
        ThemeModeCard(
            selected = state.appThemeMode,
            onSelect = viewModel::selectAppThemeMode,
            dynamicColor = state.dynamicColor,
            onDynamicColorChange = viewModel::setDynamicColor,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        ChartAggregationCard(
            selected = state.chartAggregationMode,
            onSelect = viewModel::setChartAggregationMode,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        DashboardSortEmptyTilesCard(
            enabled = state.dashboardSortEmptyTilesLast,
            onEnabledChange = viewModel::setDashboardSortEmptyTilesLast,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        HomeWidgetRefreshCard(
            selected = state.homeWidgetRefreshInterval,
            onSelect = viewModel::setHomeWidgetRefreshInterval,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        ActivityWeekModeCard(
            selected = state.activityWeekMode,
            onSelect = viewModel::selectActivityWeekMode,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
}
