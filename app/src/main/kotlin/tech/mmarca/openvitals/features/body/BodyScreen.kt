package tech.mmarca.openvitals.features.body

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.BodyFatColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.WeightColor

enum class BodyMetric {
    WEIGHT,
    HEIGHT,
    BMI,
    BODY_FAT,
    LEAN_MASS,
    BMR,
    BONE_MASS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen(
    viewModel: BodyViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    metric: BodyMetric = BodyMetric.WEIGHT,
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
    ) { _ ->
        when (metric) {
            BodyMetric.WEIGHT -> weightContent(state, unitFormatter, dateTimeFormatterProvider)
            BodyMetric.HEIGHT -> singleBodyMetricContent(
                state = state,
                titleRes = R.string.metric_height,
                value = state.heightCm?.let(unitFormatter::height),
                icon = Icons.Outlined.Straighten,
                accentColor = WeightColor,
            )
            BodyMetric.BMI -> singleBodyMetricContent(
                state = state,
                titleRes = R.string.metric_bmi,
                value = state.bmi?.let { DisplayValue(unitFormatter.decimal(it, 1), "") },
                icon = Icons.Outlined.MonitorWeight,
                accentColor = WeightColor,
            )
            BodyMetric.BODY_FAT -> bodyFatContent(state, unitFormatter)
            BodyMetric.LEAN_MASS -> singleBodyMetricContent(
                state = state,
                titleRes = R.string.metric_lean_mass,
                value = state.leanMassKg?.let(unitFormatter::bodyMass),
                icon = Icons.Outlined.MonitorWeight,
                accentColor = WeightColor,
            )
            BodyMetric.BMR -> singleBodyMetricContent(
                state = state,
                titleRes = R.string.metric_bmr,
                value = state.bmrKcal?.let(unitFormatter::energy),
                icon = Icons.Outlined.LocalFireDepartment,
                accentColor = CaloriesColor,
            )
            BodyMetric.BONE_MASS -> singleBodyMetricContent(
                state = state,
                titleRes = R.string.metric_bone_mass,
                value = state.boneMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
                icon = Icons.Outlined.MonitorWeight,
                accentColor = WeightColor,
            )
        }
    }
}

private fun LazyListScope.weightContent(
    state: BodyUiState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    if (state.weightEntries.isNotEmpty()) {
        item { SectionHeader(stringResource(R.string.section_weight)) }
        item {
            WeightSummaryCard(
                latestKg = state.latestWeightKg,
                changeKg = state.weightChangKg,
                unitFormatter = unitFormatter,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item { Spacer(Modifier.height(12.dp)) }
        item {
            WeightLineChart(
                entries = state.weightEntries,
                unitFormatter = unitFormatter,
                modifier = metricModifier(),
            )
        }
        item { SectionHeader(stringResource(R.string.section_entries)) }
        items(state.weightEntries.sortedByDescending { it.time }) { entry ->
            WeightEntryRow(
                entry = entry,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    } else if (!state.isLoading) {
        noBodyMetricData(
            titleRes = R.string.metric_weight,
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            messageRes = R.string.message_no_weight_period,
        )
    }
}

private fun LazyListScope.bodyFatContent(
    state: BodyUiState,
    unitFormatter: UnitFormatter,
) {
    val latest = state.latestBodyFatPercent
    if (latest != null) {
        item {
            val value = unitFormatter.percent(latest)
            MetricCard(
                title = stringResource(R.string.metric_body_fat),
                value = value.value,
                unit = value.unit,
                icon = Icons.Outlined.MonitorWeight,
                accentColor = BodyFatColor,
                modifier = metricModifier(),
            )
        }
        if (state.bodyFatEntries.size >= 2) {
            item {
                BodyFatLineChart(
                    entries = state.bodyFatEntries,
                    unitFormatter = unitFormatter,
                    modifier = metricModifier(),
                )
            }
        }
    } else if (!state.isLoading) {
        noBodyMetricData(
            titleRes = R.string.metric_body_fat,
            icon = Icons.Outlined.MonitorWeight,
            accentColor = BodyFatColor,
        )
    }
}

private fun LazyListScope.singleBodyMetricContent(
    state: BodyUiState,
    titleRes: Int,
    value: DisplayValue?,
    icon: ImageVector,
    accentColor: Color,
) {
    if (value != null) {
        item {
            MetricCard(
                title = stringResource(titleRes),
                value = value.value,
                unit = value.unit,
                icon = icon,
                accentColor = accentColor,
                modifier = metricModifier(),
            )
        }
    } else if (!state.isLoading) {
        noBodyMetricData(titleRes, icon, accentColor)
    }
}

private fun LazyListScope.noBodyMetricData(
    titleRes: Int,
    icon: ImageVector,
    accentColor: Color,
    messageRes: Int = R.string.message_no_readings_period,
) {
    item {
        MetricCardPlaceholder(
            title = stringResource(titleRes),
            icon = icon,
            accentColor = accentColor,
            message = stringResource(messageRes),
            modifier = metricModifier(),
        )
    }
}

private fun metricModifier(): Modifier =
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
