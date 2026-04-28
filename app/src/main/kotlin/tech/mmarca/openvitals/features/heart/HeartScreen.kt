package tech.mmarca.openvitals.features.heart

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import tech.mmarca.openvitals.core.period.TimeRange
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeartScreen(
    viewModel: HeartViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
) {
    val state by viewModel.uiState.collectAsState()
    val dayRestingBpm = state.dayRestingBpm
    val dayHrvMs = state.dayHrvMs
    val requestVitalsPermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { granted ->
        viewModel.onVitalsPermissionsResult(granted)
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
            state.selectedRange == TimeRange.DAY && state.daySamples.isNotEmpty() -> {
                item {
                    HeartRateTimelineCard(
                        date = state.selectedDate,
                        samples = state.daySamples,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            state.selectedRange == TimeRange.DAY && !state.isLoading -> {
                item {
                    HeartRateEmptyDayCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            state.dailySummaries.isNotEmpty() -> {
                item {
                    HeartRateChart(
                        summaries = state.dailySummaries,
                        selectedRange = state.selectedRange,
                        period = period,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                item { SectionHeader("Daily breakdown") }
                val restingByDate = state.dailyRestingHR.associateBy { it.date }
                val hrvByDate = state.dailyHrv.associateBy { it.date }
                items(state.dailySummaries.sortedByDescending { it.date }) { summary ->
                    HeartRateDayRow(
                        summary = summary,
                        restingBpm = restingByDate[summary.date]?.bpm,
                        hrvMs = hrvByDate[summary.date]?.rmssdMs,
                        unitFormatter = unitFormatter,
                        dateTimeFormatterProvider = dateTimeFormatterProvider,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                }
            }

            !state.isLoading -> {
                item {
                    Text(
                        text = "No heart rate data in the selected period.\n\nMake sure the heart rate permission is granted and a connected device has synced data.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }

        if (state.selectedRange == TimeRange.DAY && dayRestingBpm != null) {
            item {
                RestingHRDayCard(
                    bpm = dayRestingBpm,
                    unitFormatter = unitFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (state.selectedRange == TimeRange.DAY && dayHrvMs != null) {
            item {
                HRVDayCard(
                    rmssdMs = dayHrvMs,
                    unitFormatter = unitFormatter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (state.selectedRange != TimeRange.DAY && state.dailyRestingHR.isNotEmpty()) {
            item { SectionHeader("Resting heart rate") }
            item {
                RestingHRChart(
                    entries = state.dailyRestingHR,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (state.selectedRange != TimeRange.DAY && state.dailyHrv.isNotEmpty()) {
            item { SectionHeader("Heart rate variability (HRV)") }
            item {
                HRVChart(
                    entries = state.dailyHrv,
                    selectedRange = state.selectedRange,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        item { SectionHeader("Vitals") }
        HeartVitalsContent(
            state = state,
            phase3Permissions = viewModel.vitalsPermissions,
            onGrantPermissions = requestVitalsPermissions::launch,
            selectedRange = state.selectedRange,
            period = period,
            unitFormatter = unitFormatter,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
        )
    }
}
