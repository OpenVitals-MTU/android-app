package tech.mmarca.openvitals.features.sleep

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
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.dataSourceEducationItem

internal fun LazyListScope.sleepDayContent(
    state: SleepUiState,
    display: SleepDisplayState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    sectionContext: MetricDetailSectionContext,
    onOpenSleepSession: (String) -> Unit,
    onOpenSleepScore: (() -> Unit)?,
    onOpenSleepEfficiency: (() -> Unit)?,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    renderSleepDayOrderedContent(
        sectionContext = sectionContext,
        state = state,
        display = display,
        period = period,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        onOpenSleepSession = onOpenSleepSession,
        onOpenSleepScore = onOpenSleepScore,
        onOpenSleepEfficiency = onOpenSleepEfficiency,
        onDecreaseGoal = onDecreaseGoal,
        onIncreaseGoal = onIncreaseGoal,
    )
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
