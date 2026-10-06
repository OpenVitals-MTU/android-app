package tech.mmarca.openvitals.wear.features.recording

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.RecordingUiState
import tech.mmarca.openvitals.wear.UnitSystem
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.ui.components.MetricValueRow
import tech.mmarca.openvitals.wear.ui.components.formatCount
import tech.mmarca.openvitals.wear.ui.components.formatMetricValue
import tech.mmarca.openvitals.wear.ui.components.metricUnitLabel
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme
import tech.mmarca.openvitals.wear.ui.theme.WorkoutColor

/** What a finished workout came to. Rows the workout did not report are left out. */
@Composable
fun WorkoutSummaryScreen(
    state: RecordingUiState,
    fields: Set<WorkoutField>,
    unitSystem: UnitSystem,
    onDone: () -> Unit,
) {
    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = { EdgeButton(onClick = onDone) { Text(stringResource(R.string.action_done)) } },
    ) { contentPadding ->
        TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
            item {
                ListHeader {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(state.activityType.icon, contentDescription = null, tint = WorkoutColor)
                        Text(
                            text = stringResource(state.activityType.label),
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
            item {
                SummaryRow(
                    label = stringResource(R.string.summary_duration),
                    value = DateUtils.formatElapsedTime(state.elapsedSeconds),
                )
            }
            if (WorkoutField.DISTANCE in fields && state.distanceMeters != null) {
                item {
                    SummaryRow(
                        label = stringResource(R.string.metric_distance),
                        value = formatMetricValue(WearMetric.DISTANCE, state.distanceMeters.toDouble(), unitSystem),
                        unit = stringResource(metricUnitLabel(WearMetric.DISTANCE, unitSystem)),
                    )
                }
            }
            if (WorkoutField.PACE in fields && state.speedMetersPerSecond != null) {
                item {
                    SummaryRow(
                        label = stringResource(R.string.recording_pace),
                        value = formatPace(state.speedMetersPerSecond, unitSystem),
                        unit = stringResource(paceUnit(unitSystem)),
                    )
                }
            }
            if (WorkoutField.HEART_RATE in fields && state.averageHeartRateBpm != null) {
                item {
                    SummaryRow(
                        label = stringResource(R.string.summary_average_heart_rate),
                        value = formatCount(state.averageHeartRateBpm),
                        unit = stringResource(R.string.unit_bpm),
                    )
                }
            }
            if (WorkoutField.CALORIES in fields && state.caloriesKcal != null) {
                item {
                    SummaryRow(
                        label = stringResource(R.string.metric_active_calories),
                        value = formatCount(state.caloriesKcal),
                        unit = stringResource(R.string.unit_kcal),
                    )
                }
            }
            if (WorkoutField.ELEVATION in fields && state.elevationGainMeters != null) {
                item {
                    SummaryRow(
                        label = stringResource(R.string.metric_elevation),
                        value = formatMetricValue(WearMetric.ELEVATION, state.elevationGainMeters.toDouble(), unitSystem),
                        unit = stringResource(metricUnitLabel(WearMetric.ELEVATION, unitSystem)),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String?, unit: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        MetricValueRow(value = value, unit = unit, valueStyle = MaterialTheme.typography.titleMedium)
    }
}

@WearPreviews
@Composable
private fun WorkoutSummaryScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            WorkoutSummaryScreen(
                state = SampleData.recording,
                fields = SampleData.capabilities.workouts.getValue(ActivityType.RUN),
                unitSystem = UnitSystem.METRIC,
                onDone = {},
            )
        }
    }
}
