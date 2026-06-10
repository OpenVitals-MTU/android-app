package tech.mmarca.openvitals.features.cycle

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.DataValueKind
import tech.mmarca.openvitals.domain.insights.dataConfidence
import tech.mmarca.openvitals.core.period.DatePeriod
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CycleData
import tech.mmarca.openvitals.ui.components.DataConfidenceCard
import tech.mmarca.openvitals.ui.components.InsightStat
import tech.mmarca.openvitals.ui.components.InsightStatGrid
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.PaginatedEntryList
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.localizedPeriodTitle
import tech.mmarca.openvitals.ui.theme.CycleColor
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleScreen(
    viewModel: CycleViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalContext.current.resources
    val requestCyclePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        viewModel.onCyclePermissionsResult(granted)
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
        weekPeriodMode = state.weekPeriodMode,
    ) { period ->
        if (state.missingPermissions.isNotEmpty()) {
            item {
                PermissionCallout(
                    title = stringResource(R.string.cycle_permissions_missing_title),
                    body = stringResource(R.string.cycle_permissions_missing_body),
                    onGrant = { requestCyclePermissions.launch(viewModel.cyclePermissions) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        if (state.data.hasData) {
            item {
                CycleSummary(
                    data = state.data,
                    period = period,
                    subtitle = localizedPeriodTitle(state.selectedRange, period),
                    unitFormatter = unitFormatter,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            cycleDataConfidence(
                data = state.data,
                period = period,
            )
            cycleStatistics(
                data = state.data,
                period = period,
                unitFormatter = unitFormatter,
            )
            item { SectionHeader(stringResource(R.string.section_cycle_calendar)) }
            item {
                CycleCalendarCard(
                    data = state.data,
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

            val observations = observationsFor(state.data, resources)
            if (observations.isNotEmpty()) {
                item {
                    PaginatedEntryList(
                        title = stringResource(R.string.section_entries),
                        entries = observations,
                    ) { observation, rowModifier ->
                        CycleObservationRow(
                            observation = observation,
                            dateTimeFormatterProvider = dateTimeFormatterProvider,
                            modifier = rowModifier,
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
}

private fun androidx.compose.foundation.lazy.LazyListScope.cycleDataConfidence(
    data: CycleData,
    period: DatePeriod,
) {
    if (period.start == period.end) return

    item {
        val zone = ZoneId.systemDefault()
        DataConfidenceCard(
            confidence = dataConfidence(
                period = period,
                trackedDates =
                    data.menstruationFlows.map { it.time.atZone(zone).toLocalDate() } +
                        data.menstruationPeriods.map { it.startTime.atZone(zone).toLocalDate() } +
                        data.ovulationTests.map { it.time.atZone(zone).toLocalDate() } +
                        data.cervicalMucus.map { it.time.atZone(zone).toLocalDate() } +
                        data.basalBodyTemperature.map { it.time.atZone(zone).toLocalDate() } +
                        data.intermenstrualBleeding.map { it.time.atZone(zone).toLocalDate() } +
                        data.sexualActivity.map { it.time.atZone(zone).toLocalDate() },
                sampleCount = data.menstruationFlows.size +
                    data.menstruationPeriods.size +
                    data.ovulationTests.size +
                    data.cervicalMucus.size +
                    data.basalBodyTemperature.size +
                    data.intermenstrualBleeding.size +
                    data.sexualActivity.size,
                sources =
                    data.menstruationFlows.map { it.source } +
                        data.menstruationPeriods.map { it.source } +
                        data.ovulationTests.map { it.source } +
                        data.cervicalMucus.map { it.source } +
                        data.basalBodyTemperature.map { it.source } +
                        data.intermenstrualBleeding.map { it.source } +
                        data.sexualActivity.map { it.source },
                valueKind = DataValueKind.MEASURED,
            ),
            accentColor = CycleColor,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.cycleStatistics(
    data: CycleData,
    period: DatePeriod,
    unitFormatter: UnitFormatter,
) {
    item { SectionHeader(stringResource(R.string.section_statistics)) }
    item {
        val zone = ZoneId.systemDefault()
        val periodDays = cycleDays(period, data, zone)
            .count { it.inSelectedPeriod && (it.periodActive || it.flows.isNotEmpty()) }
        val entries = data.menstruationFlows.size +
            data.menstruationPeriods.size +
            data.ovulationTests.size +
            data.cervicalMucus.size +
            data.basalBodyTemperature.size +
            data.intermenstrualBleeding.size +
            data.sexualActivity.size

        InsightStatGrid(
            stats = listOf(
                InsightStat(
                    title = stringResource(R.string.metric_period_days),
                    value = unitFormatter.count(periodDays),
                    unit = stringResource(R.string.unit_days),
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = CycleColor,
                ),
                InsightStat(
                    title = stringResource(R.string.metric_ovulation_tests),
                    value = unitFormatter.count(data.ovulationTests.size),
                    unit = stringResource(R.string.unit_tests),
                    icon = Icons.Outlined.CheckCircle,
                    accentColor = CycleColor,
                ),
                InsightStat(
                    title = stringResource(R.string.stat_bbt_readings),
                    value = unitFormatter.count(data.basalBodyTemperature.size),
                    unit = "",
                    icon = Icons.Outlined.DeviceThermostat,
                    accentColor = CycleColor,
                ),
                InsightStat(
                    title = stringResource(R.string.section_entries),
                    value = unitFormatter.count(entries),
                    unit = "",
                    icon = Icons.Outlined.Star,
                    accentColor = CycleColor,
                ),
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
