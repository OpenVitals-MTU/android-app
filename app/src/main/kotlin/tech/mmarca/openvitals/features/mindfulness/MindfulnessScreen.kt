package tech.mmarca.openvitals.features.mindfulness

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Star
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.MindfulnessSession
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.MindfulnessColor
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindfulnessScreen(
    viewModel: MindfulnessViewModel,
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
        if (state.sessions.isEmpty() && !state.isLoading) {
            item {
                MetricCardPlaceholder(
                    title = stringResource(R.string.metric_mindfulness),
                    icon = Icons.Outlined.SelfImprovement,
                    accentColor = MindfulnessColor,
                    message = stringResource(R.string.message_no_mindfulness_period),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        if (state.sessions.isNotEmpty()) {
            item {
                MindfulnessSummary(
                    state = state,
                    subtitle = localizedPeriodTitle(state.selectedRange, period),
                    unitFormatter = unitFormatter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            mindfulnessStatistics(
                sessions = state.sessions,
                unitFormatter = unitFormatter,
            )
            item { SectionHeader(stringResource(R.string.section_sessions)) }
            items(state.sessions) { session ->
                MindfulnessSessionRow(
                    session = session,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.mindfulnessStatistics(
    sessions: List<MindfulnessSession>,
    unitFormatter: UnitFormatter,
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        val totalMs = sessions.sumOf { it.durationMs.coerceAtLeast(0L) }
        val averageMs = sessions.takeIf { it.isNotEmpty() }
            ?.let { totalMs / it.size }
            ?: 0L
        val longestMs = sessions.maxOfOrNull { it.durationMs.coerceAtLeast(0L) } ?: 0L

        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.stat_total),
                    value = unitFormatter.duration(totalMs),
                    unit = "",
                    icon = Icons.Outlined.SelfImprovement,
                    accentColor = MindfulnessColor,
                ),
                InsightStat(
                    title = stringResource(R.string.section_sessions),
                    value = unitFormatter.count(sessions.size),
                    unit = "",
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = MindfulnessColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_average_duration),
                    value = unitFormatter.duration(averageMs),
                    unit = "",
                    icon = Icons.Outlined.Star,
                    accentColor = MindfulnessColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_longest_session),
                    value = unitFormatter.duration(longestMs),
                    unit = "",
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = MindfulnessColor,
                ),
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun MindfulnessSummary(
    state: MindfulnessUiState,
    subtitle: String,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val total = unitFormatter.minutes(state.totalMinutes)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        MetricCard(
            title = stringResource(R.string.metric_total_mindfulness),
            value = total.value,
            unit = total.unit,
            icon = Icons.Outlined.SelfImprovement,
            accentColor = MindfulnessColor,
            subtitle = subtitle,
            modifier = Modifier.weight(1f),
        )
        MetricCard(
            title = stringResource(R.string.section_sessions),
            value = unitFormatter.count(state.sessions.size),
            unit = stringResource(R.string.unit_total),
            icon = Icons.Outlined.SelfImprovement,
            accentColor = MindfulnessColor,
            subtitle = stringResource(R.string.period_selected),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun MindfulnessSessionRow(
    session: MindfulnessSession,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
) {
    val zone = ZoneId.systemDefault()
    val start = session.startTime.atZone(zone)
    val dateFormatter = dateTimeFormatterProvider.mediumDate()
    val timeFormatter = dateTimeFormatterProvider.shortTime()

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.Outlined.SelfImprovement,
                contentDescription = null,
                tint = MindfulnessColor,
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title ?: stringResource(R.string.metric_mindfulness),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = "${dateFormatter.format(start)}  ·  ${timeFormatter.format(start)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = unitFormatter.duration(session.durationMs),
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.height(4.dp))
                SourceChip(source = session.source)
            }
        }
    }
}
