package tech.mmarca.openvitals.features.cycle

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Today
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import kotlin.math.roundToInt
import tech.mmarca.openvitals.domain.cycle.CycleStatistics
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.model.CycleEntryKind
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.CycleColor

internal fun LazyListScope.cyclePeriodContent(
    state: CycleUiState,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    observations: List<CycleObservation>,
    onEditCycleEntry: (CycleEntryKind, String) -> Unit = { _, _ -> },
    onDeleteCycleEntry: (CycleEntryKind, String) -> Unit = { _, _ -> },
) {
    val display = state.display
    if (display.hasData) {
        item {
            CycleSummary(
                display = display,
                subtitle = localizedPeriodTitle(state.selectedRange, period),
                unitFormatter = unitFormatter,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        cycleDataConfidence(
            display = display,
            period = period,
        )
        cycleStatistics(
            display = display,
            unitFormatter = unitFormatter,
            statistics = state.statistics,
        )
        cyclePrediction(
            statistics = state.statistics,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
        item { SectionHeader(stringResource(R.string.section_cycle_calendar)) }
        item {
            CycleCalendarCard(
                days = display.calendarDays,
                period = period,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }
        if (state.data.basalBodyTemperature.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.section_basal_body_temperature)) }
            item {
                BasalTemperatureTrendCard(
                    entries = state.data.basalBodyTemperature,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
        if (observations.isNotEmpty()) {
            item {
                PaginatedEntryList(
                    title = stringResource(R.string.section_entries),
                    entries = observations,
                ) { observation, rowModifier ->
                    val editable = observation.isOpenVitalsEntry &&
                        observation.id.isNotBlank() &&
                        observation.kind != null
                    CycleObservationRow(
                        observation = observation,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = rowModifier,
                        onEdit = if (editable) {
                            { onEditCycleEntry(observation.kind!!, observation.id) }
                        } else {
                            null
                        },
                        onDelete = if (editable) {
                            { onDeleteCycleEntry(observation.kind!!, observation.id) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
    } else if (!state.isLoading) {
        item {
            MetricCardPlaceholder(
                title = stringResource(R.string.metric_cycle_tracking),
                icon = Icons.Outlined.CalendarMonth,
                accentColor = CycleColor,
                message = stringResource(R.string.message_no_cycle_period),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

private fun LazyListScope.cycleDataConfidence(
    display: CycleDisplayState,
    period: DatePeriod,
) {
    if (period.start == period.end) return

    item {
        DataConfidenceCard(
            confidence = dataConfidence(
                period = period,
                trackedDates = display.trackedDates,
                sampleCount = display.sampleCount,
                sources = display.sources,
                valueKind = DataValueKind.MEASURED,
            ),
            accentColor = CycleColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

private fun LazyListScope.cycleStatistics(
    display: CycleDisplayState,
    unitFormatter: UnitFormatter,
    statistics: CycleStatistics?,
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        val summary = display.summary
        InsightStatGrid(
            stats = listOfNotNull(
                statistics?.currentCycleDay?.let { cycleDay ->
                    InsightStat(
                        title = stringResource(R.string.cycle_stat_cycle_day),
                        value = unitFormatter.count(cycleDay),
                        unit = "",
                        icon = Icons.Outlined.Today,
                        accentColor = CycleColor,
                    )
                },
                statistics?.averageCycleLengthDays?.let { average ->
                    InsightStat(
                        title = stringResource(R.string.cycle_stat_avg_length),
                        value = unitFormatter.count(average.roundToInt()),
                        unit = stringResource(R.string.unit_days),
                        icon = Icons.Outlined.Repeat,
                        accentColor = CycleColor,
                    )
                },
                InsightStat(
                    title = stringResource(R.string.metric_period_days),
                    value = unitFormatter.count(summary.periodDays),
                    unit = stringResource(R.string.unit_days),
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = CycleColor,
                ),
                InsightStat(
                    title = stringResource(R.string.metric_ovulation_tests),
                    value = unitFormatter.count(summary.ovulationTestCount),
                    unit = stringResource(R.string.unit_tests),
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = CycleColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_bbt_readings),
                    value = unitFormatter.count(summary.bbtReadingCount),
                    unit = "",
                    icon = Icons.Outlined.DeviceThermostat,
                    accentColor = CycleColor,
                ),
                InsightStat(
                    title = stringResource(R.string.section_entries),
                    value = unitFormatter.count(summary.totalEntryCount),
                    unit = "",
                    icon = Icons.Outlined.Star,
                    accentColor = CycleColor,
                ),
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

private fun LazyListScope.cyclePrediction(
    statistics: CycleStatistics?,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val window = statistics?.predictedWindows?.firstOrNull() ?: return
    item {
        val formatter = dateTimeFormatterProvider.mediumDate()
        MetricCard(
            title = stringResource(R.string.cycle_prediction_next_period),
            value = stringResource(
                R.string.cycle_prediction_window_value,
                formatter.format(window.start),
                formatter.format(window.endInclusive),
            ),
            unit = "",
            icon = Icons.Outlined.Event,
            accentColor = CycleColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
