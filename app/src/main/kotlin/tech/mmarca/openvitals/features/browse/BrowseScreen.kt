package tech.mmarca.openvitals.features.browse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.InlineLoading
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PaginatedEntryList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    viewModel: BrowseViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivity: (String) -> Unit,
    onOpenSleepSession: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

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
                            label = { Text(browseCategoryLabel(cat)) },
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
                        item { EmptyState(stringResource(R.string.message_no_workouts_period), Modifier.padding(16.dp)) }
                    } else {
                        item {
                            PaginatedEntryList(
                                title = stringResource(R.string.browse_count_workouts, unitFormatter.count(state.workouts.size)),
                                entries = state.workouts,
                            ) { workout, rowModifier ->
                                WorkoutBrowseRow(
                                    workout = workout,
                                    unitFormatter = unitFormatter,
                                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                                    onClick = { onOpenActivity(workout.id) },
                                    modifier = rowModifier,
                                )
                            }
                        }
                    }
                }

                BrowseCategory.SLEEP -> {
                    if (state.sleepSessions.isEmpty()) {
                        item { EmptyState(stringResource(R.string.message_no_sleep_sessions_period), Modifier.padding(16.dp)) }
                    } else {
                        item {
                            PaginatedEntryList(
                                title = stringResource(R.string.browse_count_sleep_sessions, unitFormatter.count(state.sleepSessions.size)),
                                entries = state.sleepSessions,
                            ) { session, rowModifier ->
                                SleepBrowseRow(
                                    session = session,
                                    unitFormatter = unitFormatter,
                                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                                    onClick = { onOpenSleepSession(session.id) },
                                    modifier = rowModifier,
                                )
                            }
                        }
                    }
                }

                BrowseCategory.WEIGHT -> {
                    if (state.weightEntries.isEmpty()) {
                        item { EmptyState(stringResource(R.string.message_no_weight_entries_period), Modifier.padding(16.dp)) }
                    } else {
                        item {
                            PaginatedEntryList(
                                title = stringResource(R.string.browse_count_weight_entries, unitFormatter.count(state.weightEntries.size)),
                                entries = state.weightEntries.sortedByDescending { it.time },
                            ) { entry, rowModifier ->
                                WeightBrowseRow(
                                    entry = entry,
                                    unitFormatter = unitFormatter,
                                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                                    modifier = rowModifier,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun browseCategoryLabel(category: BrowseCategory): String = stringResource(
    when (category) {
        BrowseCategory.WORKOUTS -> R.string.browse_category_workouts
        BrowseCategory.SLEEP -> R.string.browse_category_sleep
        BrowseCategory.WEIGHT -> R.string.browse_category_weight
    }
)
