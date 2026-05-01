package tech.mmarca.openvitals.features.hydration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DailyHydration
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PeriodBarChart
import tech.mmarca.openvitals.ui.components.PeriodChartValue
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.HydrationColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HydrationScreen(
    viewModel: HydrationViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val state by viewModel.uiState.collectAsState()

    MetricDetailScaffold(
        isLoading = state.isLoading,
        selectedRange = state.selectedRange,
        selectedDate = state.selectedDate,
        error = state.error,
        onRefresh = viewModel::load,
        onSelectRange = viewModel::selectRange,
        onPreviousPeriod = viewModel::previousPeriod,
        onNextPeriod = viewModel::nextPeriod,
        onSelectDate = viewModel::selectDate,
    ) { period ->
        if (state.dailyHydration.none { it.liters > 0.0 }) {
            item {
                MetricCardPlaceholder(
                    title = stringResource(R.string.metric_hydration),
                    icon = Icons.Outlined.LocalDrink,
                    accentColor = HydrationColor,
                    message = stringResource(R.string.message_no_hydration_period),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        } else {
            item {
                HydrationSummary(
                    state = state,
                    period = period,
                    unitFormatter = unitFormatter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            item {
                HydrationBarChart(
                    data = state.dailyHydration,
                    selectedRange = state.selectedRange,
                    period = period,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun HydrationSummary(
    state: HydrationUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val title = if (state.selectedRange == TimeRange.DAY) {
        stringResource(R.string.metric_hydration)
    } else {
        stringResource(R.string.metric_total_hydration)
    }
    val subtitle = if (state.selectedRange == TimeRange.DAY) {
        localizedPeriodTitle(state.selectedRange, period)
    } else {
        stringResource(R.string.summary_daily_average, unitFormatter.hydration(state.averageLiters).text)
    }
    val total = unitFormatter.hydration(state.totalLiters)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            title = title,
            value = total.value,
            unit = total.unit,
            icon = Icons.Outlined.LocalDrink,
            accentColor = HydrationColor,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            title = stringResource(R.string.metric_logged_days),
            value = unitFormatter.count(state.dailyHydration.count { it.liters > 0.0 }),
            unit = stringResource(R.string.unit_days),
            icon = Icons.Outlined.LocalDrink,
            accentColor = HydrationColor,
            subtitle = stringResource(R.string.summary_days_in_range, unitFormatter.count(state.dailyHydration.size)),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun HydrationBarChart(
    data: List<DailyHydration>,
    selectedRange: TimeRange,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    PeriodBarChart(
        title = stringResource(R.string.metric_hydration_trend),
        values = data.map { PeriodChartValue(date = it.date, value = it.liters) },
        selectedRange = selectedRange,
        period = period,
        accentColor = HydrationColor.copy(alpha = 0.85f),
        summaryText = "${localizedPeriodTitle(selectedRange, period)} · ${unitFormatter.hydration(data.sumOf { it.liters }).text}",
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        modifier = modifier,
    )
}
