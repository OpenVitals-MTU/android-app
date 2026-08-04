package tech.mmarca.openvitals.features.sleep

import androidx.compose.foundation.lazy.LazyListScope
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.MetricDetailSectionContext
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.ChartDaySelection

internal fun LazyListScope.sleepPeriodContent(
    state: SleepUiState,
    display: SleepDisplayState,
    period: DatePeriod,
    chartDaySelection: ChartDaySelection,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    sectionContext: MetricDetailSectionContext,
    onOpenSleepSession: (String) -> Unit,
    onOpenSleepScore: (() -> Unit)?,
    onOpenSleepEfficiency: (() -> Unit)?,
    onDecreaseGoal: () -> Unit,
    onIncreaseGoal: () -> Unit,
) {
    renderSleepPeriodOrderedContent(
        sectionContext = sectionContext,
        state = state,
        display = display,
        period = period,
        chartDaySelection = chartDaySelection,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        onOpenSleepSession = onOpenSleepSession,
        onOpenSleepScore = onOpenSleepScore,
        onOpenSleepEfficiency = onOpenSleepEfficiency,
        onDecreaseGoal = onDecreaseGoal,
        onIncreaseGoal = onIncreaseGoal,
    )
}
