package tech.mmarca.openvitals.features.body

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.BodyFatEntry
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.theme.BodyFatColor
import tech.mmarca.openvitals.ui.theme.WeightColor
import java.time.ZoneId

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
            item { SectionHeader("Weight") }
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
            item { SectionHeader("Entries") }
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
            item { SectionHeader("Body composition") }
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
                    text = "No weight data in the selected period.\n\nSync a scale or wearable that reports weight to Health Connect.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun BodyCompositionCard(
    bmi: Double?,
    bodyFatPercent: Double?,
    leanMassKg: Double?,
    bmrKcal: Double?,
    boneMassKg: Double?,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                bmi?.let {
                    CompositionStat(
                        label = "BMI",
                        value = unitFormatter.decimal(it, 1),
                        modifier = Modifier.weight(1f),
                    )
                }
                bodyFatPercent?.let {
                    CompositionStat(
                        label = "Body fat",
                        value = unitFormatter.percent(it).text,
                        color = BodyFatColor,
                        modifier = Modifier.weight(1f),
                    )
                }
                leanMassKg?.let {
                    CompositionStat(
                        label = "Lean mass",
                        value = unitFormatter.bodyMass(it).text,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            if (bmrKcal != null || boneMassKg != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    bmrKcal?.let {
                        CompositionStat(
                            label = "BMR",
                            value = unitFormatter.energy(it).text,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    boneMassKg?.let {
                        CompositionStat(
                            label = "Bone mass",
                            value = unitFormatter.bodyMass(it, decimals = 2).text,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (bmrKcal != null && boneMassKg != null) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompositionStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = color,
        )
    }
}

@Composable
private fun BodyFatLineChart(
    entries: List<BodyFatEntry>,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.time }
    val maxPct = sorted.maxOfOrNull { it.percent } ?: 30.0
    val minPct = sorted.minOfOrNull { it.percent } ?: 0.0
    val range = (maxPct - minPct).coerceAtLeast(0.5)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
            ) {
                if (sorted.size < 2) return@Canvas
                val stepX = size.width / (sorted.size - 1)
                val points = sorted.mapIndexed { i, entry ->
                    val x = i * stepX
                    val y = size.height * (1f - ((entry.percent - minPct) / range).toFloat())
                    Offset(x, y)
                }
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = BodyFatColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.dp.toPx(),
                    )
                }
                points.forEach { pt ->
                    drawCircle(color = BodyFatColor, radius = 4.dp.toPx(), center = pt)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${unitFormatter.percent(minPct).text} - ${unitFormatter.percent(maxPct).text} · ${unitFormatter.count(sorted.size)} entries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeightSummaryCard(
    latestKg: Double?,
    changeKg: Double?,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = "Latest",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = latestKg?.let { unitFormatter.weight(it).text } ?: "-",
                    style = MaterialTheme.typography.headlineSmall,
                    color = WeightColor,
                )
            }
            if (changeKg != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Change",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val sign = if (changeKg >= 0) "+" else ""
                    val change = unitFormatter.weight(kotlin.math.abs(changeKg))
                    Text(
                        text = "$sign${change.text}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = if (changeKg < 0) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun WeightLineChart(
    entries: List<WeightEntry>,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val sorted = entries.sortedBy { it.time }
    val maxKg = sorted.maxOfOrNull { it.weightKg } ?: 100.0
    val minKg = sorted.minOfOrNull { it.weightKg } ?: 50.0
    val range = (maxKg - minKg).coerceAtLeast(0.5)

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
            ) {
                if (sorted.size < 2) return@Canvas
                val stepX = size.width / (sorted.size - 1)
                val points = sorted.mapIndexed { i, entry ->
                    val x = i * stepX
                    val y = size.height * (1f - ((entry.weightKg - minKg) / range).toFloat())
                    Offset(x, y)
                }
                for (i in 0 until points.size - 1) {
                    drawLine(
                        color = WeightColor,
                        start = points[i],
                        end = points[i + 1],
                        strokeWidth = 2.dp.toPx(),
                    )
                }
                points.forEach { pt ->
                    drawCircle(color = WeightColor, radius = 4.dp.toPx(), center = pt)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${unitFormatter.weight(minKg).text} - ${unitFormatter.weight(maxKg).text} · ${unitFormatter.count(sorted.size)} entries",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun WeightEntryRow(
    entry: WeightEntry,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val time = entry.time.atZone(zone)
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = dateTimeFormatterProvider.mediumDateTime().format(time),
                    style = MaterialTheme.typography.bodyMedium,
                )
                SourceChip(source = entry.source)
            }
            Text(
                text = unitFormatter.weight(entry.weightKg).text,
                style = MaterialTheme.typography.titleMedium,
                color = WeightColor,
            )
        }
    }
}
