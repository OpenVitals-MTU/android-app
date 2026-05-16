package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.SectionHeader
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    viewModel: SleepViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepSession: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsState()
    val dailySessions = remember(state.sessions, state.selectedDate, state.sleepRangeMode) {
        sleepSessionsForRange(
            sessions = state.sessions,
            selectedDate = state.selectedDate,
            sleepRangeMode = state.sleepRangeMode,
        )
    }
    val dailySummary = remember(state.sessions, state.selectedDate, state.sleepRangeMode) {
        dailySleepSummary(
            sessions = state.sessions,
            selectedDate = state.selectedDate,
            sleepRangeMode = state.sleepRangeMode,
        )
    }

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
        when {
            state.selectedRange == TimeRange.DAY && dailySummary != null -> {
                item {
                    val summary = dailySummary
                    SleepSessionTimelineCard(
                        session = summary,
                        selectedDate = state.selectedDate,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        timeRangeText = dailySleepTimeRangeText(
                            sessions = dailySessions,
                            selectedDate = state.selectedDate,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                        ),
                        onClick = dailySessions.singleOrNull()?.let { session ->
                            { onOpenSleepSession(session.id) }
                        },
                        preserveTimelineGaps = dailySessions.size > 1,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                if (dailySessions.size > 1) {
                    item { SectionHeader(stringResource(R.string.section_sleep_sessions)) }
                    items(dailySessions.sortedByDescending { it.endTime }) { session ->
                        SleepSessionItem(
                            session = session,
                            unitFormatter = unitFormatter,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            onClick = { onOpenSleepSession(session.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                    }
                }
            }

            state.selectedRange != TimeRange.DAY && state.sessions.isNotEmpty() -> {
                item {
                    SleepDurationChart(
                        sessions = state.sessions,
                        selectedRange = state.selectedRange,
                        period = period,
                        sleepRangeMode = state.sleepRangeMode,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                item { SectionHeader(stringResource(R.string.section_sleep_sessions)) }
                items(state.sessions.sortedByDescending { it.endTime }) { session ->
                    SleepSessionItem(
                        session = session,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        onClick = { onOpenSleepSession(session.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            !state.isLoading -> {
                item {
                    Text(
                        text = if (state.selectedRange == TimeRange.DAY) {
                            stringResource(R.string.message_no_sleep_day_selected)
                        } else {
                            stringResource(R.string.message_no_sleep_period)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

private fun dailySleepTimeRangeText(
    sessions: List<SleepData>,
    selectedDate: LocalDate,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String {
    val zone = ZoneId.systemDefault()
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()
    val ranges = sessions
        .sortedWith(compareBy<SleepData> { it.startTime }.thenBy { it.endTime })
        .joinToString(" | ") { session ->
            val start = session.startTime.atZone(zone)
            val end = session.endTime.atZone(zone)
            "${timeFormatter.format(start)} - ${timeFormatter.format(end)}"
        }

    return "${dateFormatter.format(selectedDate)}  ·  $ranges"
}
