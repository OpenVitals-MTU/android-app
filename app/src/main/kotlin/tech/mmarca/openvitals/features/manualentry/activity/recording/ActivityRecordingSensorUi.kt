package tech.mmarca.openvitals.features.manualentry.activity.recording

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.BleConnectionStatus
import tech.mmarca.openvitals.domain.model.BleDeviceConnectionStatus
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface
import tech.mmarca.openvitals.features.activity.ActivityHeartRateChartCard
import tech.mmarca.openvitals.features.activity.toHeartRateSamples
import tech.mmarca.openvitals.ui.theme.activityRecordingAccentColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActivityRecordingSensorStatusCard(
    deviceStatuses: List<BleDeviceConnectionStatus>,
    modifier: Modifier = Modifier,
) {
    if (deviceStatuses.isEmpty()) {
        OpenVitalsSurface(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            modifier = modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        ) {
            Text(
                text = stringResource(R.string.activity_recording_sensors_add_in_settings),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    OpenVitalsSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.activity_recording_sensors_title),
                style = MaterialTheme.typography.titleSmall,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                deviceStatuses.forEach { status ->
                    AssistChip(
                        onClick = {},
                        enabled = false,
                        label = {
                            val battery = status.batteryPercent?.let { " · $it%" }.orEmpty()
                            Text(text = "${status.displayName} · ${statusLabel(status.status)}$battery")
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Bluetooth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = statusColor(status.status),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
internal fun ActivityRecordingLiveSensorStats(
    state: ActivityRecordingState,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    if (state.bleDeviceStatuses.isEmpty()) return
    val hasLiveMetric = state.currentHeartRateBpm != null ||
        state.currentCyclingCadenceRpm != null ||
        state.currentPowerWatts != null ||
        state.currentSensorSpeedMetersPerSecond != null ||
        state.currentRunningCadenceRpm != null

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ActivityRecordingSensorStatusCard(deviceStatuses = state.bleDeviceStatuses)
        if (!hasLiveMetric) {
            OpenVitalsSurface(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
            ) {
                Text(
                    text = if (state.bleHeartRateNoSignal) {
                        stringResource(R.string.activity_recording_sensors_garmin_broadcast_hint)
                    } else {
                        stringResource(R.string.activity_recording_sensors_waiting_for_data)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return
        }

        OpenVitalsSurface(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                state.currentCyclingCadenceRpm?.let { cadence ->
                    LiveSensorStat(
                        label = stringResource(R.string.activity_recording_live_cadence),
                        value = unitFormatter.count(cadence),
                        unit = "rpm",
                        modifier = Modifier.weight(1f),
                    )
                }
                state.currentPowerWatts?.let { power ->
                    LiveSensorStat(
                        label = stringResource(R.string.activity_recording_live_power),
                        value = unitFormatter.count(power.toLong()),
                        unit = "W",
                        modifier = Modifier.weight(1f),
                    )
                }
                state.currentSensorSpeedMetersPerSecond?.let { speed ->
                    LiveSensorStat(
                        label = stringResource(R.string.activity_recording_live_speed),
                        value = unitFormatter.speed(speed).text,
                        unit = "",
                        modifier = Modifier.weight(1f),
                    )
                }
                state.currentRunningCadenceRpm?.let { cadence ->
                    LiveSensorStat(
                        label = stringResource(R.string.activity_recording_live_cadence),
                        value = unitFormatter.count(cadence),
                        unit = "rpm",
                        modifier = Modifier.weight(1f),
                    )
                }
                state.currentHeartRateBpm?.let { bpm ->
                    LiveSensorStat(
                        label = stringResource(R.string.activity_recording_live_heart_rate),
                        value = unitFormatter.count(bpm),
                        unit = "bpm",
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveSensorStat(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = null,
                tint = activityRecordingAccentColor(),
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = if (unit.isBlank()) value else "$value $unit",
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun statusLabel(status: BleConnectionStatus): String =
    when (status) {
        BleConnectionStatus.CONNECTED -> stringResource(R.string.activity_recording_sensors_connected)
        BleConnectionStatus.CONNECTING -> stringResource(R.string.activity_recording_sensors_connecting)
        BleConnectionStatus.RECONNECTING -> stringResource(R.string.activity_recording_sensors_reconnecting)
        BleConnectionStatus.DISCONNECTED -> stringResource(R.string.activity_recording_sensors_disabled)
    }

@Composable
private fun statusColor(status: BleConnectionStatus) =
    when (status) {
        BleConnectionStatus.CONNECTED -> activityRecordingAccentColor()
        BleConnectionStatus.CONNECTING,
        BleConnectionStatus.RECONNECTING,
        -> MaterialTheme.colorScheme.tertiary
        BleConnectionStatus.DISCONNECTED -> MaterialTheme.colorScheme.error
    }

@Composable
internal fun ActivityRecordedSensorSummary(
    samples: tech.mmarca.openvitals.domain.model.BleRecordingSampleBuffer,
    unitFormatter: UnitFormatter,
    sessionStart: java.time.Instant? = null,
    sessionEnd: java.time.Instant? = null,
    savedHeartRateSamples: List<tech.mmarca.openvitals.domain.model.HeartRateSample> = emptyList(),
    modifier: Modifier = Modifier,
) {
    val heartRateSamples = samples.toHeartRateSamples().ifEmpty { savedHeartRateSamples }
    val hasOtherSamples = samples.powerSamples.isNotEmpty() ||
        samples.cyclingCadenceSamples.isNotEmpty() ||
        samples.speedSamples.isNotEmpty() ||
        samples.stepsCadenceSamples.isNotEmpty()

    if (heartRateSamples.isEmpty() && !hasOtherSamples) return

    val chartStart = sessionStart ?: heartRateSamples.minOfOrNull { it.time }
    val chartEnd = sessionEnd ?: heartRateSamples.maxOfOrNull { it.time }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (heartRateSamples.isNotEmpty() && chartStart != null && chartEnd != null) {
            ActivityHeartRateChartCard(
                samples = heartRateSamples,
                sessionStart = chartStart,
                sessionEnd = chartEnd,
                unitFormatter = unitFormatter,
            )
        }

        if (!hasOtherSamples) return

        OpenVitalsSurface(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.activity_recording_sensors_recorded_title),
                    style = MaterialTheme.typography.titleSmall,
                )
                samples.averagePowerWatts()?.let { watts ->
                    Text(
                        text = "${stringResource(R.string.metric_average_power)}: ${unitFormatter.power(watts).text}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (samples.cyclingCadenceSamples.isNotEmpty()) {
                    val avgCadence = samples.cyclingCadenceSamples.map { it.rpm }.average().toLong()
                    Text(
                        text = "${stringResource(R.string.metric_cycling_cadence)}: ${unitFormatter.cadence(avgCadence.toDouble()).text}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (samples.speedSamples.isNotEmpty()) {
                    val avgSpeed = samples.speedSamples.map { it.metersPerSecond }.average()
                    Text(
                        text = "${stringResource(R.string.metric_average_speed)}: ${unitFormatter.speed(avgSpeed).text}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
    }
}
