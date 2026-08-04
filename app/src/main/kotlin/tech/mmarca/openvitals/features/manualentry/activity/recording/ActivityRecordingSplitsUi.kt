package tech.mmarca.openvitals.features.manualentry.activity.recording

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import android.content.ClipData
import android.graphics.Color as AndroidColor
import android.content.ClipDescription
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.location.Location
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.activity.maps.OfflineRouteMapOrPreview
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardField
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardItemSize
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardTemplate
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.preferences.isDarkTheme
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.theme.ActivityRecordingTheme
import tech.mmarca.openvitals.ui.theme.activityRecordingAccentColor
import tech.mmarca.openvitals.ui.theme.recordingOutdoorAccentForAppTheme
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface

@Composable
internal fun RecordingSplitsTab(
    splits: List<ActivityRecordingSplit>,
    emptyMessage: String,
    unitFormatter: UnitFormatter,
    label: @Composable (ActivityRecordingSplit) -> String,
    controls: (@Composable () -> Unit)? = null,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        controls?.invoke()

        if (splits.isEmpty()) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 24.dp),
            )
            return@Column
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            splits.forEachIndexed { index, split ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                RecordingSplitRow(
                    split = split,
                    label = label(split),
                    unitFormatter = unitFormatter,
                    modifier = Modifier.padding(vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
internal fun RecordingMarkersList(
    markers: List<ActivityRecordingMarker>,
    unitFormatter: UnitFormatter,
    onUpdateMarker: (ActivityRecordingMarker) -> Unit,
    onDeleteMarker: (String) -> Unit,
) {
    if (markers.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.activity_entry_recording_markers_title),
            style = MaterialTheme.typography.titleSmall,
        )
        markers.sortedBy { it.time }.forEachIndexed { index, marker ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = marker.name,
                        onValueChange = { name -> onUpdateMarker(marker.copy(name = name)) },
                        label = { Text(stringResource(R.string.activity_entry_recording_marker_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OpenVitalsIconButton(onClick = { onDeleteMarker(marker.id) }) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                        )
                    }
                }
                OutlinedTextField(
                    value = marker.note,
                    onValueChange = { note -> onUpdateMarker(marker.copy(note = note)) },
                    label = { Text(stringResource(R.string.activity_entry_recording_marker_note)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = marker.locationSummary(unitFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun RecordingSplitRow(
    split: ActivityRecordingSplit,
    label: String,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
) {
    val distance = unitFormatter.distance(split.distanceMeters)
    val elapsed = DisplayValue(formatRecordingElapsed(Duration.ofMillis(split.elapsedMillis)), "")
    val averageSpeed = unitFormatter.speed(split.averageSpeedMetersPerSecond)
    val maxSpeed = unitFormatter.speed(split.maxSpeedMetersPerSecond)
    val climb = unitFormatter.elevation(split.climbMeters)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
            )
            CompactSplitMetric(
                label = stringResource(R.string.activity_entry_recording_distance),
                value = distance,
            )
            CompactSplitMetric(
                label = stringResource(R.string.activity_entry_recording_split_elapsed),
                value = elapsed,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CompactSplitMetric(
                label = stringResource(R.string.activity_entry_recording_split_avg),
                value = averageSpeed,
                modifier = Modifier.weight(1f),
            )
            CompactSplitMetric(
                label = stringResource(R.string.activity_entry_recording_split_max),
                value = maxSpeed,
                modifier = Modifier.weight(1f),
            )
            CompactSplitMetric(
                label = stringResource(R.string.activity_entry_recording_elevation_gain),
                value = climb,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

internal fun ActivityRecordingMarker.locationSummary(unitFormatter: UnitFormatter): String {
    val coordinate = "%.5f, %.5f".format(Locale.US, latitude, longitude)
    val altitude = altitudeMeters?.let { " • ${unitFormatter.elevation(it).text}" }.orEmpty()
    return coordinate + altitude
}

@Composable
internal fun CompactSplitMetric(
    label: String,
    value: DisplayValue,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = if (value.unit.isBlank()) value.value else "${value.value} ${value.unit}",
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
internal fun TimeSplitSelector(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit,
) {
    SplitSelector(
        title = stringResource(R.string.activity_entry_recording_time_split),
        options = TimeSplitMinuteOptions,
        selected = selectedMinutes,
        label = { minutes -> stringResource(R.string.activity_entry_recording_split_minutes, minutes) },
        onSelect = onSelect,
    )
}

@Composable
internal fun DistanceSplitSelector(
    selectedMeters: Double,
    unitSystem: UnitSystem,
    unitFormatter: UnitFormatter,
    onSelect: (Double) -> Unit,
) {
    SplitSelector(
        title = stringResource(R.string.activity_entry_recording_distance_split),
        options = distanceSplitOptions(unitSystem),
        selected = selectedMeters,
        label = { meters -> distanceSplitOptionLabel(meters, unitSystem, unitFormatter) },
        onSelect = onSelect,
    )
}

@Composable
internal fun <T> SplitSelector(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = selected == option,
                    onClick = { onSelect(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = options.size,
                    ),
                    label = { Text(label(option)) },
                )
            }
        }
    }
}


@Composable
internal fun RecordingStat(
    value: DisplayValue,
    label: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (emphasized) Alignment.Start else Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value.value,
                style = if (emphasized) {
                    MaterialTheme.typography.displayMedium
                } else {
                    MaterialTheme.typography.displaySmall
                },
                color = if (emphasized) activityRecordingAccentColor() else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            if (value.unit.isNotBlank()) {
                Text(
                    text = value.unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 3.dp, bottom = 5.dp),
                )
            }
        }
        AutoResizeText(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = activityRecordingAccentColor(),
            maxLines = 1,
        )
    }
}

internal fun formatRecordingElapsed(duration: Duration): String {
    val totalSeconds = duration.seconds.coerceAtLeast(0L)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

internal fun defaultDistanceSplitMeters(unitSystem: UnitSystem): Double =
    when (unitSystem) {
        UnitSystem.METRIC -> 1_000.0
        UnitSystem.IMPERIAL -> MetersPerMile
    }

internal fun distanceSplitOptions(unitSystem: UnitSystem): List<Double> =
    when (unitSystem) {
        UnitSystem.METRIC -> listOf(500.0, 1_000.0, 5_000.0)
        UnitSystem.IMPERIAL -> listOf(0.5 * MetersPerMile, MetersPerMile, 5.0 * MetersPerMile)
    }

internal fun distanceSplitOptionLabel(
    meters: Double,
    unitSystem: UnitSystem,
    unitFormatter: UnitFormatter,
): String {
    val value = when (unitSystem) {
        UnitSystem.METRIC -> meters / 1_000.0
        UnitSystem.IMPERIAL -> meters / MetersPerMile
    }
    val unit = when (unitSystem) {
        UnitSystem.METRIC -> "km"
        UnitSystem.IMPERIAL -> "mi"
    }
    return "${unitFormatter.decimal(value, splitDistanceDecimals(value))} $unit"
}

internal fun distanceRangeLabel(
    startMeters: Double,
    endMeters: Double,
    unitSystem: UnitSystem,
    unitFormatter: UnitFormatter,
): String {
    val divisor = when (unitSystem) {
        UnitSystem.METRIC -> 1_000.0
        UnitSystem.IMPERIAL -> MetersPerMile
    }
    val unit = when (unitSystem) {
        UnitSystem.METRIC -> "km"
        UnitSystem.IMPERIAL -> "mi"
    }
    val start = startMeters / divisor
    val end = endMeters / divisor
    return "${unitFormatter.decimal(start, splitDistanceDecimals(start))}-" +
        "${unitFormatter.decimal(end, splitDistanceDecimals(end))} $unit"
}

internal fun splitDistanceDecimals(value: Double): Int =
    if (value < 1.0 || value % 1.0 != 0.0) 1 else 0

internal const val MetersPerMile = 1_609.344