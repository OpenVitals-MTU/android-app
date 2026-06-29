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
internal fun RecordingStatsTab(
    state: ActivityRecordingState,
    totalTime: Duration,
    movingTime: Duration,
    now: Instant,
    unitFormatter: UnitFormatter,
    isEditingDashboard: Boolean,
    onUpdateDashboardLayout: (ActivityRecordingDashboardLayout) -> Unit,
) {
    val availableFields = availableRecordingDashboardFields(state)
    val layout = state.dashboardLayout.withAvailableFields(availableFields)
    val stats = recordingDashboardStats(
        state = state,
        totalTime = totalTime,
        movingTime = movingTime,
        now = now,
        unitFormatter = unitFormatter,
    )

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RecordingDashboardGrid(
            layout = layout,
            stats = stats,
            isEditingDashboard = isEditingDashboard,
            onUpdateLayout = onUpdateDashboardLayout,
        )

        if (state.bleDeviceStatuses.isNotEmpty()) {
            ActivityRecordingSensorStatusCard(deviceStatuses = state.bleDeviceStatuses)
            if (state.bleHeartRateNoSignal && state.currentHeartRateBpm == null) {
                Text(
                    text = stringResource(R.string.activity_recording_sensors_garmin_broadcast_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        state.lastAccuracyMeters?.let { accuracyMeters ->
            Text(
                text = stringResource(
                    R.string.activity_entry_recording_accuracy,
                    unitFormatter.elevation(accuracyMeters).text,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

    }
}

internal data class RecordingDashboardStat(
    val value: DisplayValue,
    val label: String,
)

@Composable
internal fun recordingDashboardStats(
    state: ActivityRecordingState,
    totalTime: Duration,
    movingTime: Duration,
    now: Instant,
    unitFormatter: UnitFormatter,
): Map<ActivityRecordingDashboardField, RecordingDashboardStat> {
    val waiting = stringResource(R.string.activity_recording_sensors_waiting_short)
    val hasHeartRateSensor = state.bleDeviceStatuses.any {
        BleSensorCapability.HEART_RATE in it.capabilities
    }
    val heartRate = state.currentHeartRateBpm?.let { unitFormatter.heartRate(it) }
        ?: DisplayValue(
            value = if (hasHeartRateSensor && state.bleHeartRateNoSignal) {
                stringResource(R.string.activity_recording_sensors_no_signal_short)
            } else {
                waiting
            },
            unit = "bpm",
        )
    val cadence = state.currentCyclingCadenceRpm ?: state.currentRunningCadenceRpm
    val speed = state.currentSensorSpeedMetersPerSecond
        ?: state.effectiveCurrentSpeedMetersPerSecond(now)

    return mapOf(
        ActivityRecordingDashboardField.HEART_RATE to RecordingDashboardStat(
            value = heartRate,
            label = stringResource(R.string.activity_recording_live_heart_rate),
        ),
        ActivityRecordingDashboardField.CADENCE to RecordingDashboardStat(
            value = cadence?.let { unitFormatter.cadence(it.toDouble()) } ?: DisplayValue(waiting, "rpm"),
            label = stringResource(R.string.activity_recording_live_cadence),
        ),
        ActivityRecordingDashboardField.SPEED to RecordingDashboardStat(
            value = unitFormatter.speed(speed),
            label = stringResource(R.string.activity_entry_recording_speed),
        ),
        ActivityRecordingDashboardField.DISTANCE to RecordingDashboardStat(
            value = unitFormatter.distance(state.distanceMeters),
            label = stringResource(R.string.activity_entry_recording_distance),
        ),
        ActivityRecordingDashboardField.DURATION to RecordingDashboardStat(
            value = DisplayValue(formatRecordingElapsed(totalTime), ""),
            label = stringResource(R.string.activity_entry_recording_total_time),
        ),
        ActivityRecordingDashboardField.MOVING_TIME to RecordingDashboardStat(
            value = DisplayValue(formatRecordingElapsed(movingTime), ""),
            label = stringResource(R.string.activity_entry_recording_moving_time),
        ),
        ActivityRecordingDashboardField.AVERAGE_SPEED to RecordingDashboardStat(
            value = unitFormatter.averageSpeed(state.distanceMeters, totalTime.toMillis()),
            label = stringResource(R.string.activity_entry_recording_average_speed),
        ),
        ActivityRecordingDashboardField.AVERAGE_MOVING_SPEED to RecordingDashboardStat(
            value = unitFormatter.averageSpeed(state.distanceMeters, movingTime.toMillis()),
            label = stringResource(R.string.activity_entry_recording_average_moving_speed),
        ),
        ActivityRecordingDashboardField.MAX_SPEED to RecordingDashboardStat(
            value = unitFormatter.speed(state.maxSpeedMetersPerSecond),
            label = stringResource(R.string.activity_entry_recording_max_speed),
        ),
        ActivityRecordingDashboardField.ELEVATION_GAIN to RecordingDashboardStat(
            value = unitFormatter.elevation(state.displayElevationGainedMeters()),
            label = stringResource(R.string.activity_entry_recording_elevation_gain),
        ),
        ActivityRecordingDashboardField.POWER to RecordingDashboardStat(
            value = state.currentPowerWatts?.let(unitFormatter::power) ?: DisplayValue(waiting, "W"),
            label = stringResource(R.string.activity_recording_live_power),
        ),
        ActivityRecordingDashboardField.STEPS to RecordingDashboardStat(
            value = DisplayValue(unitFormatter.count(state.repetitionCount), ""),
            label = stringResource(R.string.activity_entry_steps_title),
        ),
    )
}

internal fun availableRecordingDashboardFields(
    state: ActivityRecordingState,
): List<ActivityRecordingDashboardField> = buildList {
    if (state.recordingKind == ActivityRecordingKind.TIMED) {
        add(ActivityRecordingDashboardField.HEART_RATE)
        add(ActivityRecordingDashboardField.DURATION)
        add(ActivityRecordingDashboardField.MOVING_TIME)
        add(ActivityRecordingDashboardField.POWER)
    } else {
        add(ActivityRecordingDashboardField.HEART_RATE)
        add(ActivityRecordingDashboardField.CADENCE)
        add(ActivityRecordingDashboardField.SPEED)
        add(ActivityRecordingDashboardField.DISTANCE)
        add(ActivityRecordingDashboardField.DURATION)
        add(ActivityRecordingDashboardField.MOVING_TIME)
        add(ActivityRecordingDashboardField.AVERAGE_SPEED)
        add(ActivityRecordingDashboardField.AVERAGE_MOVING_SPEED)
        add(ActivityRecordingDashboardField.MAX_SPEED)
        add(ActivityRecordingDashboardField.ELEVATION_GAIN)
        add(ActivityRecordingDashboardField.POWER)
        if (activityEntryTypeById(state.activityTypeId)?.supportsStepCounting == true) {
            add(ActivityRecordingDashboardField.STEPS)
        }
    }
}

internal fun ActivityRecordingDashboardLayout.withAvailableFields(
    availableFields: List<ActivityRecordingDashboardField>,
): ActivityRecordingDashboardLayout {
    val available = availableFields.toSet()
    val normalized = normalized()
    val items = normalized.items.filter { it.field in available }
    if (items.isNotEmpty()) {
        return normalized.copy(
            fields = items.map { it.field },
            sizes = items.associate { it.field to it.size },
        ).normalized()
    }
    val fields = ActivityRecordingDashboardLayout.DefaultFields.filter { it in available }
        .ifEmpty { availableFields }
    return normalized.copy(
        fields = fields,
        sizes = normalized.sizes.filterKeys { it in fields.toSet() },
    ).normalized()
}

@Composable
internal fun RecordingDashboardGrid(
    layout: ActivityRecordingDashboardLayout,
    stats: Map<ActivityRecordingDashboardField, RecordingDashboardStat>,
    isEditingDashboard: Boolean,
    onUpdateLayout: (ActivityRecordingDashboardLayout) -> Unit,
    modifier: Modifier = Modifier,
    fillHeight: Boolean = false,
) {
    val normalizedLayout = layout.normalized()
    val placements = normalizedLayout.placements()
    if (placements.isEmpty()) return

    val spacing = if (normalizedLayout.template == ActivityRecordingDashboardTemplate.THREE_BY_FOUR) {
        8.dp
    } else {
        10.dp
    }
    val cellHeight = when (normalizedLayout.template) {
        ActivityRecordingDashboardTemplate.LARGE_TOP -> 78.dp
        ActivityRecordingDashboardTemplate.TWO_BY_FOUR -> 96.dp
        ActivityRecordingDashboardTemplate.THREE_BY_FOUR -> 86.dp
    }

    Layout(
        modifier = modifier
            .fillMaxWidth()
            .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
            .animateContentSize(),
        content = {
            placements.forEach { placement ->
                val field = placement.item.field
                val stat = stats[field] ?: return@forEach
                val index = normalizedLayout.fields.indexOf(field)
                key(field) {
                    RecordingDashboardTile(
                        field = field,
                        stat = stat,
                        size = placement.item.size,
                        emphasized = placement.item.size.hasRoomyMetricText(),
                        compact = placement.item.size.hasCompactMetricText(),
                        isEditingDashboard = isEditingDashboard,
                        onDropField = { draggedField ->
                            if (draggedField != field) {
                                onUpdateLayout(normalizedLayout.withMovedFieldToTarget(draggedField, field))
                            }
                        },
                        onRemove = {
                            onUpdateLayout(normalizedLayout.withRemovedField(field))
                        },
                        onResize = { resizedSize ->
                            onUpdateLayout(
                                normalizedLayout.withFieldSize(
                                    field = field,
                                    size = resizedSize,
                                )
                            )
                        },
                        onMovePrevious = normalizedLayout.fields.getOrNull(index - 1)?.let { targetField ->
                            { onUpdateLayout(normalizedLayout.withMovedFieldToTarget(field, targetField)) }
                        },
                        onMoveNext = normalizedLayout.fields.getOrNull(index + 1)?.let { targetField ->
                            { onUpdateLayout(normalizedLayout.withMovedFieldToTarget(field, targetField)) }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val spacingPx = spacing.roundToPx()
        val defaultCellHeightPx = cellHeight.roundToPx()
        val layoutWidth = constraints.maxWidth
        val columnWidth = (
            (layoutWidth - spacingPx * (normalizedLayout.template.columns - 1)) /
                normalizedLayout.template.columns
            ).coerceAtLeast(0)
        val cellHeightPx = if (fillHeight && constraints.hasBoundedHeight) {
            (
                (constraints.maxHeight - spacingPx * (normalizedLayout.template.rows - 1)) /
                    normalizedLayout.template.rows
                ).coerceAtLeast(0)
        } else {
            defaultCellHeightPx
        }
        val occupiedRows = if (fillHeight) {
            normalizedLayout.template.rows
        } else {
            placements.maxOfOrNull { it.row + it.rowSpan } ?: normalizedLayout.template.rows
        }
        val layoutHeight = cellHeightPx * occupiedRows +
            spacingPx * (occupiedRows - 1).coerceAtLeast(0)
        val placeables = measurables.mapIndexed { index, measurable ->
            val placement = placements[index]
            val width = columnWidth * placement.columnSpan + spacingPx * (placement.columnSpan - 1)
            val height = cellHeightPx * placement.rowSpan + spacingPx * (placement.rowSpan - 1)
            measurable.measure(Constraints.fixed(width, height))
        }

        layout(layoutWidth, layoutHeight) {
            placeables.forEachIndexed { index, placeable ->
                val placement = placements[index]
                val x = placement.column * (columnWidth + spacingPx)
                val y = placement.row * (cellHeightPx + spacingPx)
                placeable.placeRelative(x, y)
            }
        }
    }
}

@Composable
internal fun RecordingDashboardTile(
    field: ActivityRecordingDashboardField,
    stat: RecordingDashboardStat,
    size: ActivityRecordingDashboardItemSize,
    emphasized: Boolean,
    compact: Boolean,
    isEditingDashboard: Boolean,
    onDropField: (ActivityRecordingDashboardField) -> Unit,
    onRemove: () -> Unit,
    onResize: (ActivityRecordingDashboardItemSize) -> Unit,
    onMovePrevious: (() -> Unit)?,
    onMoveNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var isDropTargetActive by remember(field, isEditingDashboard) { mutableStateOf(false) }
    val wiggleRotation = if (isEditingDashboard) {
        val wiggleTransition = rememberInfiniteTransition(label = "RecordingDashboardWiggle")
        val rotation by wiggleTransition.animateFloat(
            initialValue = -RecordingDashboardEditWiggleDegrees,
            targetValue = RecordingDashboardEditWiggleDegrees,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 140,
                    delayMillis = (field.ordinal % 4) * 35,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "RecordingDashboardWiggleRotation",
        )
        rotation
    } else {
        0f
    }
    val removeWidgetLabel = stringResource(R.string.cd_remove_widget)
    val movePreviousLabel = stringResource(R.string.cd_move_widget_up)
    val moveNextLabel = stringResource(R.string.cd_move_widget_down)
    val shrinkLabel = stringResource(R.string.cd_decrease_recording_dashboard_widget_size)
    val growLabel = stringResource(R.string.cd_increase_recording_dashboard_widget_size)
    val latestSize = rememberUpdatedState(size)
    val dragAndDropTarget = remember(field, isEditingDashboard) {
        object : DragAndDropTarget {
            override fun onEntered(event: DragAndDropEvent) {
                isDropTargetActive = event.toRecordingDashboardField() != field
            }

            override fun onExited(event: DragAndDropEvent) {
                isDropTargetActive = false
            }

            override fun onEnded(event: DragAndDropEvent) {
                isDropTargetActive = false
            }

            override fun onDrop(event: DragAndDropEvent): Boolean {
                val draggedField = event.toRecordingDashboardField() ?: return false
                isDropTargetActive = false
                onDropField(draggedField)
                return true
            }
        }
    }
    val dragSourceModifier = if (isEditingDashboard) {
        Modifier.dragAndDropSource { _ ->
            DragAndDropTransferData(
                clipData = ClipData(
                    ClipDescription(
                        RecordingDashboardDragLabel,
                        arrayOf(RecordingDashboardDragMimeType),
                    ),
                    ClipData.Item(field.name),
                ),
                localState = field,
            )
        }
    } else {
        Modifier
    }
    val dropTargetModifier = if (isEditingDashboard) {
        Modifier.dragAndDropTarget(
            shouldStartDragAndDrop = { startEvent ->
                RecordingDashboardDragMimeType in startEvent.mimeTypes()
            },
            target = dragAndDropTarget,
        )
    } else {
        Modifier
    }
    val editSemanticsModifier = if (isEditingDashboard) {
        Modifier.semantics {
            contentDescription = stat.label
            customActions = buildList {
                onMovePrevious?.let { action ->
                    add(
                        CustomAccessibilityAction(movePreviousLabel) {
                            action()
                            true
                        }
                    )
                }
                onMoveNext?.let { action ->
                    add(
                        CustomAccessibilityAction(moveNextLabel) {
                            action()
                            true
                        }
                    )
                }
                if (size.canShrink()) {
                    add(
                        CustomAccessibilityAction(shrinkLabel) {
                            onResize(size.previousSize())
                            true
                        }
                    )
                }
                if (size.canGrow()) {
                    add(
                        CustomAccessibilityAction(growLabel) {
                            onResize(size.nextSize())
                            true
                        }
                    )
                }
                add(
                    CustomAccessibilityAction(removeWidgetLabel) {
                        onRemove()
                        true
                    }
                )
            }
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .zIndex(if (isDropTargetActive) 1f else 0f)
            .graphicsLayer {
                scaleX = if (isDropTargetActive) 1.02f else 1f
                scaleY = if (isDropTargetActive) 1.02f else 1f
                rotationZ = if (isEditingDashboard && !isDropTargetActive) wiggleRotation else 0f
                shadowElevation = if (isDropTargetActive) 12.dp.toPx() else 0f
            }
            .then(editSemanticsModifier)
            .then(dropTargetModifier),
    ) {
        RecordingDashboardTileContent(
            stat = stat,
            size = size,
            emphasized = emphasized,
            compact = compact,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (isEditingDashboard) {
                        Modifier.border(
                            width = if (isDropTargetActive) 2.dp else 1.dp,
                            color = if (isDropTargetActive) {
                                activityRecordingAccentColor()
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.52f)
                            },
                            shape = MaterialTheme.shapes.medium,
                        )
                    } else {
                        Modifier
                    }
                )
                .then(dragSourceModifier),
        )
        if (isEditingDashboard) {
            RecordingDashboardEditButton(
                onClick = onRemove,
                contentDescription = removeWidgetLabel,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
            RecordingDashboardResizeHandle(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp),
                onResize = onResize,
                latestSize = { latestSize.value },
            )
        }
    }
}

@Composable
internal fun RecordingDashboardResizeHandle(
    onResize: (ActivityRecordingDashboardItemSize) -> Unit,
    latestSize: () -> ActivityRecordingDashboardItemSize,
    modifier: Modifier = Modifier,
) {
    val handleColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = modifier
            .size(38.dp)
            .pointerInput(Unit) {
                var dragStartSize = ActivityRecordingDashboardItemSize.SMALL
                var appliedSize = ActivityRecordingDashboardItemSize.SMALL
                var dragOffset = Offset.Zero
                detectDragGestures(
                    onDragStart = {
                        dragStartSize = latestSize()
                        appliedSize = dragStartSize
                        dragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount
                        val targetSize = dragStartSize.sizeForResizeDrag(
                            dragOffset = dragOffset,
                            stepPx = RecordingDashboardResizeStep.toPx(),
                        )
                        if (targetSize != appliedSize) {
                            appliedSize = targetSize
                            onResize(targetSize)
                        }
                    },
                )
            },
        contentAlignment = Alignment.BottomEnd,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 2.dp.toPx()
            val insets = listOf(10.dp.toPx(), 17.dp.toPx(), 24.dp.toPx())
            insets.forEach { inset ->
                drawLine(
                    color = handleColor,
                    start = Offset(size.width - inset, size.height),
                    end = Offset(size.width, size.height - inset),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
internal fun RecordingDashboardEditButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(34.dp)
            .background(
                color = MaterialTheme.colorScheme.surface.copy(alpha = if (enabled) 0.9f else 0.54f),
                shape = CircleShape,
            )
            .clickable(
                enabled = enabled,
                onClickLabel = contentDescription,
                onClick = onClick,
            ),
    ) {
        content()
    }
}

@Composable
internal fun RecordingDashboardTileContent(
    stat: RecordingDashboardStat,
    size: ActivityRecordingDashboardItemSize,
    emphasized: Boolean,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val valueStyle = when {
        emphasized -> MaterialTheme.typography.displayMedium
        compact -> MaterialTheme.typography.headlineSmall
        else -> MaterialTheme.typography.headlineMedium
    }
    val unitStyle = when {
        compact -> MaterialTheme.typography.labelMedium
        size.rowSpan == 1 -> MaterialTheme.typography.labelLarge
        else -> MaterialTheme.typography.titleSmall
    }
    val labelStyle = if (compact || size.rowSpan == 1) {
        MaterialTheme.typography.labelMedium
    } else {
        MaterialTheme.typography.labelLarge
    }
    OpenVitalsSurface(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
        contentPadding = PaddingValues(if (compact || size.rowSpan == 1) 8.dp else 10.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                AutoResizeText(
                    text = stat.value.value,
                    style = valueStyle,
                    color = if (emphasized) activityRecordingAccentColor() else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (stat.value.unit.isNotBlank()) {
                    Text(
                        text = stat.value.unit,
                        style = unitStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 3.dp, bottom = if (compact) 2.dp else 4.dp),
                        maxLines = 1,
                    )
                }
            }
            AutoResizeText(
                text = stat.label.uppercase(),
                style = labelStyle,
                color = activityRecordingAccentColor(),
                maxLines = 1,
            )
        }
    }
}

internal fun DragAndDropEvent.toRecordingDashboardField(): ActivityRecordingDashboardField? {
    val androidDragEvent = toAndroidDragEvent()
    (androidDragEvent.localState as? ActivityRecordingDashboardField)?.let { return it }
    return androidDragEvent.clipData
        ?.getItemAt(0)
        ?.text
        ?.toString()
        ?.let { fieldName ->
            runCatching { ActivityRecordingDashboardField.valueOf(fieldName) }.getOrNull()
        }
}

internal fun ActivityRecordingDashboardLayout.withMovedFieldToTarget(
    field: ActivityRecordingDashboardField,
    targetField: ActivityRecordingDashboardField,
): ActivityRecordingDashboardLayout {
    val fromIndex = fields.indexOf(field)
    val targetIndex = fields.indexOf(targetField)
    if (fromIndex == -1 || targetIndex == -1 || fromIndex == targetIndex) return this
    return copy(fields = fields.move(fromIndex, targetIndex)).normalized()
}

internal fun ActivityRecordingDashboardLayout.withRemovedField(
    field: ActivityRecordingDashboardField,
): ActivityRecordingDashboardLayout {
    if (fields.size <= 1) return this
    return copy(
        fields = fields.filterNot { it == field },
        sizes = sizes - field,
    ).normalized()
}

internal fun ActivityRecordingDashboardLayout.withAddedField(
    field: ActivityRecordingDashboardField,
): ActivityRecordingDashboardLayout {
    if (field in fields) return this
    val updated = copy(
        fields = fields + field,
        sizes = sizes - field,
    ).normalized()
    return if (field in updated.fields) updated else this
}

internal fun ActivityRecordingDashboardItemSize.nextSize(): ActivityRecordingDashboardItemSize =
    if (columnSpan < ActivityRecordingDashboardTemplate.LARGE_TOP.columns) {
        copy(columnSpan = columnSpan + 1)
    } else {
        copy(rowSpan = (rowSpan + 1).coerceAtMost(ActivityRecordingDashboardTemplate.LARGE_TOP.rows))
    }

internal fun ActivityRecordingDashboardItemSize.previousSize(): ActivityRecordingDashboardItemSize =
    if (rowSpan > 1) {
        copy(rowSpan = rowSpan - 1)
    } else {
        copy(columnSpan = (columnSpan - 1).coerceAtLeast(1))
    }

internal fun ActivityRecordingDashboardItemSize.canGrow(): Boolean =
    columnSpan < ActivityRecordingDashboardTemplate.LARGE_TOP.columns ||
        rowSpan < ActivityRecordingDashboardTemplate.LARGE_TOP.rows

internal fun ActivityRecordingDashboardItemSize.canShrink(): Boolean =
    columnSpan > 1 || rowSpan > 1

internal fun ActivityRecordingDashboardItemSize.hasCompactMetricText(): Boolean =
    columnSpan == 1 || rowSpan == 1

internal fun ActivityRecordingDashboardItemSize.hasRoomyMetricText(): Boolean =
    columnSpan >= 3 ||
        rowSpan >= 3 ||
        (columnSpan >= 2 && rowSpan >= 2)

internal fun ActivityRecordingDashboardItemSize.sizeForResizeDrag(
    dragOffset: Offset,
    stepPx: Float,
): ActivityRecordingDashboardItemSize {
    val targetColumnSpan = (columnSpan + dragOffset.x.dragSteps(stepPx))
        .coerceIn(1, ActivityRecordingDashboardTemplate.LARGE_TOP.columns)
    val targetRowSpan = (rowSpan + dragOffset.y.dragSteps(stepPx))
        .coerceIn(1, ActivityRecordingDashboardTemplate.LARGE_TOP.rows)
    return ActivityRecordingDashboardItemSize(
        columnSpan = targetColumnSpan,
        rowSpan = targetRowSpan,
    )
}

internal fun Float.dragSteps(stepPx: Float): Int =
    when {
        this >= stepPx -> (this / stepPx).toInt()
        this <= -stepPx -> (this / stepPx).toInt()
        else -> 0
    }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecordingDashboardEditor(
    layout: ActivityRecordingDashboardLayout,
    availableFields: List<ActivityRecordingDashboardField>,
    onUpdateLayout: (ActivityRecordingDashboardLayout) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val addableFields = availableFields
            .filterNot { it in layout.fields }
            .filter { field -> field in layout.withAddedField(field).fields }
        if (addableFields.isNotEmpty()) {
            Text(
                text = stringResource(R.string.activity_entry_recording_dashboard_add_field),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                addableFields.forEach { field ->
                    AssistChip(
                        onClick = {
                            onUpdateLayout(
                                layout.withAddedField(field),
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        label = { Text(stringResource(field.labelRes)) },
                    )
                }
            }
        }
    }
}

internal val ActivityRecordingDashboardTemplate.labelRes: Int
    get() = when (this) {
        ActivityRecordingDashboardTemplate.TWO_BY_FOUR -> R.string.activity_entry_recording_dashboard_layout_two_by_four
        ActivityRecordingDashboardTemplate.THREE_BY_FOUR -> R.string.activity_entry_recording_dashboard_layout_three_by_four
        ActivityRecordingDashboardTemplate.LARGE_TOP -> R.string.activity_entry_recording_dashboard_layout_large_top
    }

internal val ActivityRecordingDashboardField.labelRes: Int
    get() = when (this) {
        ActivityRecordingDashboardField.HEART_RATE -> R.string.activity_recording_live_heart_rate
        ActivityRecordingDashboardField.CADENCE -> R.string.activity_recording_live_cadence
        ActivityRecordingDashboardField.SPEED -> R.string.activity_entry_recording_speed
        ActivityRecordingDashboardField.DISTANCE -> R.string.activity_entry_recording_distance
        ActivityRecordingDashboardField.DURATION -> R.string.activity_entry_recording_total_time
        ActivityRecordingDashboardField.MOVING_TIME -> R.string.activity_entry_recording_moving_time
        ActivityRecordingDashboardField.AVERAGE_SPEED -> R.string.activity_entry_recording_average_speed
        ActivityRecordingDashboardField.AVERAGE_MOVING_SPEED -> R.string.activity_entry_recording_average_moving_speed
        ActivityRecordingDashboardField.MAX_SPEED -> R.string.activity_entry_recording_max_speed
        ActivityRecordingDashboardField.ELEVATION_GAIN -> R.string.activity_entry_recording_elevation_gain
        ActivityRecordingDashboardField.POWER -> R.string.activity_recording_live_power
        ActivityRecordingDashboardField.STEPS -> R.string.activity_entry_steps_title
    }

internal fun <T> List<T>.move(fromIndex: Int, toIndex: Int): List<T> {
    if (fromIndex !in indices || toIndex !in indices || fromIndex == toIndex) return this
    val mutable = toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)
    return mutable
}

internal const val RecordingDashboardDragMimeType = "application/vnd.openvitals.recording-dashboard-field"
internal const val RecordingDashboardDragLabel = "OpenVitals recording dashboard widget"
internal const val RecordingDashboardEditWiggleDegrees = 0.45f
internal val RecordingDashboardResizeStep = 44.dp