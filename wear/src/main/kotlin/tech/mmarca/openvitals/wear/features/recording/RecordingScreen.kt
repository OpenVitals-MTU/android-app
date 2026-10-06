package tech.mmarca.openvitals.wear.features.recording

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.AlertDialogDefaults
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.FilledTonalIconButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButtonDefaults
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import tech.mmarca.openvitals.wear.R
import tech.mmarca.openvitals.wear.RecordingUiState
import tech.mmarca.openvitals.wear.UnitSystem
import tech.mmarca.openvitals.wear.WearMetric
import tech.mmarca.openvitals.wear.features.metric.HeartZoneLabels
import tech.mmarca.openvitals.wear.features.metric.heartRateZone
import tech.mmarca.openvitals.wear.features.metric.heartRateZoneProgress
import tech.mmarca.openvitals.wear.ui.components.MetricValueRow
import tech.mmarca.openvitals.wear.ui.components.formatCount
import tech.mmarca.openvitals.wear.ui.components.formatMetricValue
import tech.mmarca.openvitals.wear.ui.components.metricUnitLabel
import tech.mmarca.openvitals.wear.ui.preview.SampleData
import tech.mmarca.openvitals.wear.ui.preview.WearPreviews
import tech.mmarca.openvitals.wear.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.wear.ui.theme.DistanceColor
import tech.mmarca.openvitals.wear.ui.theme.ElevationColor
import tech.mmarca.openvitals.wear.ui.theme.Emphasis
import tech.mmarca.openvitals.wear.ui.theme.HeartColor
import tech.mmarca.openvitals.wear.ui.theme.HeartZoneColors
import tech.mmarca.openvitals.wear.ui.theme.OpenVitalsWearTheme
import tech.mmarca.openvitals.wear.ui.theme.WorkoutColor

private enum class WorkoutPage { CONTROLS, OVERVIEW, HEART, PACE }

/**
 * A running workout, laid out like the watch makers' own: the overview in
 * the middle, controls one swipe to the left, heart rate zones and pace to
 * the right. Only pages and values the workout reports in [fields] are shown.
 */
@Composable
fun RecordingScreen(
    state: RecordingUiState,
    fields: Set<WorkoutField>,
    unitSystem: UnitSystem,
    maxHeartRate: Int,
    onTogglePause: () -> Unit,
    onStop: () -> Unit,
) {
    val pages = remember(fields) {
        buildList {
            add(WorkoutPage.CONTROLS)
            add(WorkoutPage.OVERVIEW)
            if (WorkoutField.HEART_RATE in fields) add(WorkoutPage.HEART)
            if (fields.any { it == WorkoutField.PACE || it == WorkoutField.SPEED || it == WorkoutField.ELEVATION }) {
                add(WorkoutPage.PACE)
            }
        }
    }
    val pagerState = rememberPagerState(initialPage = pages.indexOf(WorkoutPage.OVERVIEW)) { pages.size }
    var confirmStop by rememberSaveable { mutableStateOf(false) }

    HorizontalPagerScaffold(pagerState = pagerState, modifier = Modifier.keepScreenOn()) {
        HorizontalPager(state = pagerState) { page ->
            ScreenScaffold {
                when (pages[page]) {
                    WorkoutPage.CONTROLS -> ControlsPage(state, onTogglePause, onStop = { confirmStop = true })
                    WorkoutPage.OVERVIEW -> OverviewPage(state, fields, unitSystem, maxHeartRate)
                    WorkoutPage.HEART -> HeartPage(state, maxHeartRate)
                    WorkoutPage.PACE -> PacePage(state, fields, unitSystem)
                }
            }
        }
    }

    AlertDialog(
        visible = confirmStop,
        onDismissRequest = { confirmStop = false },
        confirmButton = {
            AlertDialogDefaults.ConfirmButton(
                onClick = {
                    confirmStop = false
                    onStop()
                },
            )
        },
        title = { Text(stringResource(R.string.recording_stop_confirm)) },
    )
}

@Composable
private fun OverviewPage(
    state: RecordingUiState,
    fields: Set<WorkoutField>,
    unitSystem: UnitSystem,
    maxHeartRate: Int,
) {
    val zone = state.heartRateBpm?.let { heartRateZone(it.toDouble(), maxHeartRate) }
    PageColumn {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(state.activityType.icon, null, tint = WorkoutColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(if (state.paused) R.string.recording_paused else state.activityType.label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = DateUtils.formatElapsedTime(state.elapsedSeconds),
            style = MaterialTheme.typography.displayMedium.copy(fontFeatureSettings = "tnum"),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (state.paused) Emphasis.disabled else 1f),
        )
        if (WorkoutField.HEART_RATE in fields) {
            FieldRow(
                icon = Icons.Outlined.Favorite,
                color = zone?.let { HeartZoneColors[it] } ?: HeartColor,
                value = state.heartRateBpm?.toString(),
                unit = stringResource(R.string.unit_bpm),
            )
        }
        if (WorkoutField.DISTANCE in fields) {
            FieldRow(
                icon = Icons.Outlined.Straighten,
                color = DistanceColor,
                value = state.distanceMeters?.let { formatMetricValue(WearMetric.DISTANCE, it.toDouble(), unitSystem) },
                unit = stringResource(metricUnitLabel(WearMetric.DISTANCE, unitSystem)),
            )
        }
        if (WorkoutField.CALORIES in fields) {
            FieldRow(
                icon = Icons.Outlined.LocalFireDepartment,
                color = ActiveCaloriesColor,
                value = state.caloriesKcal?.let(::formatCount),
                unit = stringResource(R.string.unit_kcal),
            )
        }
    }
}

@Composable
private fun HeartPage(state: RecordingUiState, maxHeartRate: Int) {
    val bpm = state.heartRateBpm?.toDouble()
    val zone = bpm?.let { heartRateZone(it, maxHeartRate) }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        HeartZoneArc(
            zone = zone,
            progress = bpm?.let { heartRateZoneProgress(it, maxHeartRate) } ?: 0f,
            modifier = Modifier.fillMaxSize(),
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Outlined.Favorite,
                contentDescription = null,
                tint = zone?.let { HeartZoneColors[it] } ?: HeartColor,
                modifier = Modifier.size(20.dp),
            )
            MetricValueRow(
                value = state.heartRateBpm?.toString(),
                unit = stringResource(R.string.unit_bpm),
                valueStyle = MaterialTheme.typography.displayMedium,
            )
            Text(
                text = zone?.let {
                    stringResource(R.string.zone_label, it + 1, stringResource(HeartZoneLabels[it]))
                } ?: stringResource(R.string.zone_below),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PacePage(state: RecordingUiState, fields: Set<WorkoutField>, unitSystem: UnitSystem) {
    PageColumn {
        val speed = state.speedMetersPerSecond
        when {
            WorkoutField.PACE in fields -> BigValue(
                label = stringResource(R.string.recording_pace),
                value = speed?.let { formatPace(it, unitSystem) },
                unit = stringResource(paceUnit(unitSystem)),
            )
            WorkoutField.SPEED in fields -> BigValue(
                label = stringResource(R.string.recording_speed),
                value = speed?.let { formatSpeed(it, unitSystem) },
                unit = stringResource(speedUnit(unitSystem)),
            )
        }
        if (WorkoutField.ELEVATION in fields) {
            FieldRow(
                icon = Icons.Outlined.Terrain,
                color = ElevationColor,
                value = state.elevationGainMeters?.let {
                    formatMetricValue(WearMetric.ELEVATION, it.toDouble(), unitSystem)
                },
                unit = stringResource(metricUnitLabel(WearMetric.ELEVATION, unitSystem)),
            )
        }
    }
}

@Composable
private fun ControlsPage(state: RecordingUiState, onTogglePause: () -> Unit, onStop: () -> Unit) {
    PageColumn {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTonalIconButton(onClick = onTogglePause, modifier = Modifier.size(56.dp)) {
                    Icon(if (state.paused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause, contentDescription = null)
                }
                Text(
                    text = stringResource(if (state.paused) R.string.recording_resume else R.string.recording_pause),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledIconButton(
                    onClick = onStop,
                    modifier = Modifier.size(56.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    ),
                ) {
                    Icon(Icons.Outlined.Stop, contentDescription = null)
                }
                Text(text = stringResource(R.string.recording_stop), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun PageColumn(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        content()
    }
}

@Composable
private fun FieldRow(icon: ImageVector, color: Color, value: String?, unit: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        MetricValueRow(value = value, unit = unit, valueStyle = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun BigValue(label: String, value: String?, unit: String) {
    Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    MetricValueRow(value = value, unit = unit, valueStyle = MaterialTheme.typography.displaySmall)
}

@WearPreviews
@Composable
private fun RecordingScreenPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            RecordingScreen(
                state = SampleData.recording,
                fields = SampleData.capabilities.workouts.getValue(ActivityType.RUN),
                unitSystem = UnitSystem.METRIC,
                maxHeartRate = 190,
                onTogglePause = {},
                onStop = {},
            )
        }
    }
}

@WearPreviews
@Composable
private fun RecordingScreenPausedPreview() {
    OpenVitalsWearTheme {
        AppScaffold {
            RecordingScreen(
                state = SampleData.recording.copy(paused = true),
                fields = SampleData.capabilities.workouts.getValue(ActivityType.WORKOUT),
                unitSystem = UnitSystem.IMPERIAL,
                maxHeartRate = 190,
                onTogglePause = {},
                onStop = {},
            )
        }
    }
}
