package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.dataSourceEducationItem

internal fun LazyListScope.sleepDayContent(
    state: SleepUiState,
    display: SleepDisplayState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenSleepSession: (String) -> Unit,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    val summary = display.dailySummary ?: return

    item {
        SleepSessionTimelineCard(
            session = summary,
            selectedDate = state.selectedDate,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            timeRangeText = dailySleepTimeRangeText(
                sessions = display.dailySessions,
                selectedDate = state.selectedDate,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
            ),
            onClick = display.dailySessions.singleOrNull()?.let { session ->
                { onOpenSleepSession(session.id) }
            },
            preserveTimelineGaps = display.dailySessions.size > 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    sleepInsightSections(
        state = state,
        period = period,
        confidenceSessions = display.dailySessions,
        display = display,
        unitFormatter = unitFormatter,
        onDecreaseGoal = onDecreaseGoal,
        onIncreaseGoal = onIncreaseGoal,
    )

    if (display.dailySessions.size > 1) {
        item {
            PaginatedEntryList(
                title = stringResource(R.string.section_sleep_sessions),
                entries = display.dailySessions.sortedByDescending { it.endTime },
            ) { session, rowModifier ->
                SleepSessionItem(
                    session = session,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onClick = { onOpenSleepSession(session.id) },
                    modifier = rowModifier,
                )
            }
        }
    }
    dataSourceEducationItem()
}

internal fun LazyListScope.sleepNoDataContent(selectedRange: TimeRange) {
    item {
        Text(
            text = if (selectedRange == TimeRange.DAY) {
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
