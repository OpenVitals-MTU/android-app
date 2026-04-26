package tech.mmarca.openvitals.features.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.data.model.WeightEntry
import tech.mmarca.openvitals.ui.components.InlineLoading
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.SourceChip
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivity: (String) -> Unit,
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
        headerItems = {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    BrowseCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = cat == state.selectedCategory,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(cat.label) },
                        )
                    }
                }
            }
        },
    ) { _ ->
        if (state.isLoading) {
            item { InlineLoading() }
        } else {
            when (state.selectedCategory) {
                BrowseCategory.WORKOUTS -> {
                    if (state.workouts.isEmpty()) {
                        item { EmptyState("No workouts in the selected period.", Modifier.padding(16.dp)) }
                    } else {
                        item { SectionHeader("${state.workouts.size} workouts") }
                        items(state.workouts) { w ->
                            WorkoutBrowseRow(
                                workout = w,
                                unitFormatter = unitFormatter,
                                dateTimeFormatterProvider = dateTimeFormatterProvider,
                                onClick = { onOpenActivity(w.id) },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                BrowseCategory.SLEEP -> {
                    if (state.sleepSessions.isEmpty()) {
                        item { EmptyState("No sleep sessions in the selected period.", Modifier.padding(16.dp)) }
                    } else {
                        item { SectionHeader("${state.sleepSessions.size} sleep sessions") }
                        items(state.sleepSessions) { s ->
                            SleepBrowseRow(
                                session = s,
                                unitFormatter = unitFormatter,
                                dateTimeFormatterProvider = dateTimeFormatterProvider,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                BrowseCategory.WEIGHT -> {
                    if (state.weightEntries.isEmpty()) {
                        item { EmptyState("No weight entries in the selected period.", Modifier.padding(16.dp)) }
                    } else {
                        item { SectionHeader("${state.weightEntries.size} weight entries") }
                        items(state.weightEntries.sortedByDescending { it.time }) { w ->
                            WeightBrowseRow(
                                entry = w,
                                unitFormatter = unitFormatter,
                                dateTimeFormatterProvider = dateTimeFormatterProvider,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String, modifier: Modifier = Modifier) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutBrowseRow(
    workout: ExerciseData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = workout.title ?: "Exercise",
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = dateTimeFormatterProvider.mediumDateTime().format(workout.startTime.atZone(zone)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SourceChip(source = workout.source)
            }
            Text(
                text = unitFormatter.duration(workout.durationMs),
                style = MaterialTheme.typography.labelLarge,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SleepBrowseRow(
    session: SleepData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = dateTimeFormatterProvider.mediumDateTime().format(session.startTime.atZone(zone)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "${session.stages.size} stages",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SourceChip(source = session.source)
            }
            Text(
                text = unitFormatter.duration(session.durationMs),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

@Composable
private fun WeightBrowseRow(
    entry: WeightEntry,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(
                    text = dateTimeFormatterProvider.mediumDateTime().format(entry.time.atZone(zone)),
                    style = MaterialTheme.typography.bodyMedium,
                )
                SourceChip(source = entry.source)
            }
            Text(
                text = unitFormatter.weight(entry.weightKg).text,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
