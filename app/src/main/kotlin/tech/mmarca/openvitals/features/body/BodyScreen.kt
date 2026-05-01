package tech.mmarca.openvitals.features.body

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BodyScreen(
    viewModel: BodyViewModel,
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
    ) { _ ->
        val hasComposition = state.bmi != null || state.latestBodyFatPercent != null ||
            state.leanMassKg != null || state.bmrKcal != null || state.boneMassKg != null

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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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
        }

        if (hasComposition) {
            item { SectionHeader(stringResource(R.string.section_body_composition)) }
            item {
                BodyCompositionCard(
                    bmi = state.bmi,
                    bodyFatPercent = state.latestBodyFatPercent,
                    leanMassKg = state.leanMassKg,
                    bmrKcal = state.bmrKcal,
                    boneMassKg = state.boneMassKg,
                    unitFormatter = unitFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
            if (state.bodyFatEntries.size >= 2) {
                item { Spacer(Modifier.height(12.dp)) }
                item {
                    BodyFatLineChart(
                        entries = state.bodyFatEntries,
                        unitFormatter = unitFormatter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        }

        if (state.weightEntries.isEmpty() && !hasComposition && !state.isLoading) {
            item {
                Text(
                    text = stringResource(R.string.message_no_weight_period),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}
