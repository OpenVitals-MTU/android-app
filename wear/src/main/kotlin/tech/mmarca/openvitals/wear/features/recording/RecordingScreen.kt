package tech.mmarca.openvitals.wear.features.recording

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.RecordingUiState
import tech.mmarca.openvitals.wear.UnitSystem
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.ui.components.MetricValueRow
import tech.mmarca.openvitals.wear.ui.components.formatMetricValue
import tech.mmarca.openvitals.wear.ui.components.metricUnitLabel
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.Emphasis
import tech.mmarca.openvitals.wear.ui.theme.HeartColor
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme
import tech.mmarca.openvitals.wear.ui.theme.WorkoutColor

@Composable
fun RecordingScreen(
    state: RecordingUiState,
    unitSystem: UnitSystem,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
) {
    ScreenScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = state.activityType.icon,
                    contentDescription = null,
                    tint = WorkoutColor,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = stringResource(
                        if (state.paused) R.string.recording_paused else state.activityType.label,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = DateUtils.formatElapsedTime(state.elapsedSeconds),
                style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (state.paused) Emphasis.disabled else 1f,
                ),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Favorite,
                        contentDescription = stringResource(R.string.metric_heart_rate),
                        tint = HeartColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    MetricValueRow(
                        value = state.heartRateBpm?.toString(),
                        valueStyle = MaterialTheme.typography.titleMedium,
                    )
                }
                MetricValueRow(
                    value = state.distanceMeters?.let {
                        formatMetricValue(WearMetric.DISTANCE, it.toDouble(), unitSystem)
                    },
                    unit = stringResource(metricUnitLabel(WearMetric.DISTANCE, unitSystem)),
                    valueStyle = MaterialTheme.typography.titleMedium,
                )
            }
            Row(
                modifier = Modifier.padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalIconButton(onClick = onTogglePause) {
                    if (state.paused) {
                        Icon(Icons.Outlined.PlayArrow, stringResource(R.string.recording_resume))
                    } else {
                        Icon(Icons.Outlined.Pause, stringResource(R.string.recording_pause))
                    }
                }
                FilledIconButton(
                    onClick = onStop,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Outlined.Stop, stringResource(R.string.recording_stop))
                }
            }
        }
    }
}

@WearPreviews
@Composable
private fun RecordingScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            RecordingScreen(SampleData.recording, UnitSystem.METRIC, onTogglePause = {}, onStop = {})
        }
    }
}

@WearPreviews
@Composable
private fun RecordingScreenPausedPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            RecordingScreen(
                SampleData.recording.copy(paused = true),
                UnitSystem.METRIC,
                onTogglePause = {},
                onStop = {},
            )
        }
    }
}
