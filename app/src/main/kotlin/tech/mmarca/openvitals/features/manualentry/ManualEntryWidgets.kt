package tech.mmarca.openvitals.features.manualentry

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.features.manualentry.body.accentColor
import tech.mmarca.openvitals.features.manualentry.body.icon
import tech.mmarca.openvitals.features.manualentry.body.titleRes
import tech.mmarca.openvitals.features.manualentry.vitals.accentColor
import tech.mmarca.openvitals.features.manualentry.vitals.icon
import tech.mmarca.openvitals.features.manualentry.vitals.titleRes
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsCardStyle
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.HydrationColor
import tech.mmarca.openvitals.ui.theme.MindfulnessColor
import tech.mmarca.openvitals.ui.theme.NutritionColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor

internal const val ManualEntryGridColumns = 3
internal const val ManualEntryEditWiggleDegrees = 0.45f
internal val ManualEntryTileIconSize = 34.dp

@Composable
internal fun HydrationWritePermissionPrompt(
    onDismiss: () -> Unit,
    onOpenEntry: () -> Unit,
    onGrant: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_entry_write_permission_title)) },
        text = { Text(stringResource(R.string.hydration_tracker_permission_needed)) },
        confirmButton = {
            OpenVitalsButton(onClick = onGrant) {
                Text(stringResource(R.string.action_grant))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onOpenEntry) {
                Text(stringResource(R.string.action_open))
            }
        },
    )
}

@Composable
internal fun MindfulnessWritePermissionPrompt(
    onDismiss: () -> Unit,
    onOpenEntry: () -> Unit,
    onGrant: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_entry_mindfulness_write_permission_title)) },
        text = { Text(stringResource(R.string.mindfulness_entry_permission_needed)) },
        confirmButton = {
            OpenVitalsButton(onClick = onGrant) {
                Text(stringResource(R.string.action_grant))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onOpenEntry) {
                Text(stringResource(R.string.action_open))
            }
        },
    )
}

@Composable
internal fun ActivityWritePermissionPrompt(
    onDismiss: () -> Unit,
    onOpenEntry: () -> Unit,
    onGrant: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_entry_activity_write_permission_title)) },
        text = { Text(stringResource(R.string.activity_entry_permission_needed)) },
        confirmButton = {
            OpenVitalsButton(onClick = onGrant) {
                Text(stringResource(R.string.action_grant))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onOpenEntry) {
                Text(stringResource(R.string.action_open))
            }
        },
    )
}

@Composable
internal fun NutritionWritePermissionPrompt(
    onDismiss: () -> Unit,
    onOpenEntry: () -> Unit,
    onGrant: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_entry_carbs_write_permission_title)) },
        text = { Text(stringResource(R.string.carbs_entry_permission_needed)) },
        confirmButton = {
            OpenVitalsButton(onClick = onGrant) {
                Text(stringResource(R.string.action_grant))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onOpenEntry) {
                Text(stringResource(R.string.action_open))
            }
        },
    )
}

@Composable
internal fun BodyWritePermissionPrompt(
    type: BodyMeasurementType,
    onDismiss: () -> Unit,
    onOpenEntry: () -> Unit,
    onGrant: () -> Unit,
) {
    val title = stringResource(type.titleRes())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_entry_body_write_permission_title, title)) },
        text = { Text(stringResource(R.string.body_entry_permission_needed, title)) },
        confirmButton = {
            OpenVitalsButton(onClick = onGrant) {
                Text(stringResource(R.string.action_grant))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onOpenEntry) {
                Text(stringResource(R.string.action_open))
            }
        },
    )
}

@Composable
internal fun VitalsWritePermissionPrompt(
    type: VitalsMeasurementType,
    onDismiss: () -> Unit,
    onOpenEntry: () -> Unit,
    onGrant: () -> Unit,
) {
    val title = stringResource(type.titleRes())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_entry_vitals_write_permission_title, title)) },
        text = { Text(stringResource(R.string.vitals_entry_permission_needed, title)) },
        confirmButton = {
            OpenVitalsButton(onClick = onGrant) {
                Text(stringResource(R.string.action_grant))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onOpenEntry) {
                Text(stringResource(R.string.action_open))
            }
        },
    )
}

@Composable
internal fun ManualEntryWidgetGrid(
    visibleIds: List<ManualEntryWidgetId>,
    specsById: Map<ManualEntryWidgetId, ManualEntryWidgetSpec>,
    isEditingWidgets: Boolean,
    onMoveWidgetToTarget: (ManualEntryWidgetId, ManualEntryWidgetId) -> Unit,
    onRemoveWidget: (ManualEntryWidgetId) -> Unit,
) {
    val visibleSpecs = remember(visibleIds, specsById) {
        visibleIds.mapNotNull { specsById[it] }
    }
    if (visibleSpecs.isEmpty()) return

    val lazyGridState = rememberLazyGridState()
    val reorderableState = rememberReorderableLazyGridState(lazyGridState) { from, to ->
        val fromId = from.key as? ManualEntryWidgetId
        val toId = to.key as? ManualEntryWidgetId
        if (fromId != null && toId != null && fromId != toId) {
            onMoveWidgetToTarget(fromId, toId)
        }
    }
    val rowCount = (visibleSpecs.size + ManualEntryGridColumns - 1) / ManualEntryGridColumns
    val horizontalPadding = 12.dp
    val verticalPadding = 4.dp
    val spacing = 8.dp

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val tileSize = (
            maxWidth - horizontalPadding * 2 - spacing * (ManualEntryGridColumns - 1)
            ).coerceAtLeast(0.dp) / ManualEntryGridColumns
        val gridHeight = tileSize * rowCount +
            spacing * (rowCount - 1).coerceAtLeast(0) +
            verticalPadding * 2

        LazyVerticalGrid(
            columns = GridCells.Fixed(ManualEntryGridColumns),
            state = lazyGridState,
            userScrollEnabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(gridHeight)
                .animateContentSize(),
            contentPadding = PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            itemsIndexed(
                items = visibleSpecs,
                key = { _, spec -> spec.id },
            ) { _, spec ->
                ReorderableItem(
                    state = reorderableState,
                    key = spec.id,
                    enabled = isEditingWidgets,
                ) { isDragging ->
                    Box(modifier = Modifier.aspectRatio(1f)) {
                        ManualEntryWidgetTile(
                            spec = spec,
                            isEditingWidgets = isEditingWidgets,
                            isDragging = isDragging,
                            dragHandleModifier = Modifier.longPressDraggableHandle(
                                enabled = isEditingWidgets,
                            ),
                            onRemove = { onRemoveWidget(spec.id) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun ManualEntryWidgetTile(
    spec: ManualEntryWidgetSpec,
    isEditingWidgets: Boolean,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val wiggleRotation = if (isEditingWidgets) {
        val wiggleTransition = rememberInfiniteTransition(label = "ManualEntryWidgetWiggle")
        val rotation by wiggleTransition.animateFloat(
            initialValue = -ManualEntryEditWiggleDegrees,
            targetValue = ManualEntryEditWiggleDegrees,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 140,
                    delayMillis = (spec.id.ordinal % 4) * 35,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "ManualEntryWidgetWiggleRotation",
        )
        rotation
    } else {
        0f
    }

    Box(
        modifier = modifier
            .zIndex(if (isDragging) 1f else 0f)
            .graphicsLayer {
                rotationZ = if (isEditingWidgets && !isDragging) wiggleRotation else 0f
                scaleX = if (isDragging) 1.02f else 1f
                scaleY = if (isDragging) 1.02f else 1f
                shadowElevation = if (isDragging) 12.dp.toPx() else 0f
            }
            .then(dragHandleModifier),
    ) {
        spec.content(Modifier.fillMaxSize())
        if (isEditingWidgets) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(24.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                        shape = CircleShape,
                    )
                    .clickable(
                        onClickLabel = stringResource(R.string.cd_remove_widget),
                        onClick = onRemove,
                    ),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

internal fun LazyListScope.hiddenManualEntryWidgets(
    hiddenSpecs: List<ManualEntryWidgetSpec>,
    onAddWidget: (ManualEntryWidgetId) -> Unit,
) {
    item { SectionHeader(stringResource(R.string.manual_entry_add_widgets)) }

    if (hiddenSpecs.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.manual_entry_all_widgets_added),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    } else {
        items(hiddenSpecs, key = { "add_${it.id.name}" }) { spec ->
            OpenVitalsOutlinedButton(
                onClick = { onAddWidget(spec.id) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Text(
                    text = spec.title,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
internal fun manualEntryWidgetSpecs(
    isEditingWidgets: Boolean,
    onOpenHydrationEntry: () -> Unit,
    onOpenCarbsEntry: () -> Unit,
    onOpenActivityEntry: () -> Unit,
    onOpenMindfulnessEntry: () -> Unit,
    onOpenBodyMeasurementEntry: (BodyMeasurementType) -> Unit,
    onOpenVitalsMeasurementEntry: (VitalsMeasurementType) -> Unit,
): List<ManualEntryWidgetSpec> {
    val hydrationClick = if (isEditingWidgets) null else onOpenHydrationEntry
    val carbsClick = if (isEditingWidgets) null else onOpenCarbsEntry
    val activityClick = if (isEditingWidgets) null else onOpenActivityEntry
    val mindfulnessClick = if (isEditingWidgets) null else onOpenMindfulnessEntry
    return listOf(
        ManualEntryWidgetSpec(
            id = ManualEntryWidgetId.HYDRATION,
            title = stringResource(R.string.manual_entry_hydration_title),
            content = { modifier ->
                ManualEntryMetricTile(
                    title = stringResource(R.string.manual_entry_hydration_title),
                    icon = Icons.Outlined.LocalDrink,
                    accentColor = HydrationColor,
                    modifier = modifier,
                    onClick = hydrationClick,
                )
            },
        ),
        ManualEntryWidgetSpec(
            id = ManualEntryWidgetId.CARBS,
            title = stringResource(R.string.metric_carbs),
            content = { modifier ->
                ManualEntryMetricTile(
                    title = stringResource(R.string.metric_carbs),
                    icon = Icons.Outlined.Restaurant,
                    accentColor = NutritionColor,
                    modifier = modifier,
                    onClick = carbsClick,
                )
            },
        ),
        ManualEntryWidgetSpec(
            id = ManualEntryWidgetId.ACTIVITY,
            title = stringResource(R.string.manual_entry_activity_title),
            content = { modifier ->
                ManualEntryMetricTile(
                    title = stringResource(R.string.manual_entry_activity_title),
                    icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                    accentColor = WorkoutColor,
                    modifier = modifier,
                    onClick = activityClick,
                )
            },
        ),
        ManualEntryWidgetSpec(
            id = ManualEntryWidgetId.MINDFULNESS,
            title = stringResource(R.string.metric_mindfulness),
            content = { modifier ->
                ManualEntryMetricTile(
                    title = stringResource(R.string.metric_mindfulness),
                    icon = Icons.Outlined.SelfImprovement,
                    accentColor = MindfulnessColor,
                    modifier = modifier,
                    onClick = mindfulnessClick,
                )
            },
        ),
        bodyMeasurementWidgetSpec(
            id = ManualEntryWidgetId.WEIGHT,
            type = BodyMeasurementType.WEIGHT,
            isEditingWidgets = isEditingWidgets,
            onOpenBodyMeasurementEntry = onOpenBodyMeasurementEntry,
        ),
        bodyMeasurementWidgetSpec(
            id = ManualEntryWidgetId.HEIGHT,
            type = BodyMeasurementType.HEIGHT,
            isEditingWidgets = isEditingWidgets,
            onOpenBodyMeasurementEntry = onOpenBodyMeasurementEntry,
        ),
        bodyMeasurementWidgetSpec(
            id = ManualEntryWidgetId.BODY_FAT,
            type = BodyMeasurementType.BODY_FAT,
            isEditingWidgets = isEditingWidgets,
            onOpenBodyMeasurementEntry = onOpenBodyMeasurementEntry,
        ),
        vitalsMeasurementWidgetSpec(
            id = ManualEntryWidgetId.BLOOD_PRESSURE,
            type = VitalsMeasurementType.BLOOD_PRESSURE,
            isEditingWidgets = isEditingWidgets,
            onOpenVitalsMeasurementEntry = onOpenVitalsMeasurementEntry,
        ),
        vitalsMeasurementWidgetSpec(
            id = ManualEntryWidgetId.SPO2,
            type = VitalsMeasurementType.SPO2,
            isEditingWidgets = isEditingWidgets,
            onOpenVitalsMeasurementEntry = onOpenVitalsMeasurementEntry,
        ),
        vitalsMeasurementWidgetSpec(
            id = ManualEntryWidgetId.RESPIRATORY_RATE,
            type = VitalsMeasurementType.RESPIRATORY_RATE,
            isEditingWidgets = isEditingWidgets,
            onOpenVitalsMeasurementEntry = onOpenVitalsMeasurementEntry,
        ),
        vitalsMeasurementWidgetSpec(
            id = ManualEntryWidgetId.BODY_TEMPERATURE,
            type = VitalsMeasurementType.BODY_TEMPERATURE,
            isEditingWidgets = isEditingWidgets,
            onOpenVitalsMeasurementEntry = onOpenVitalsMeasurementEntry,
        ),
    )
}

@Composable
internal fun bodyMeasurementWidgetSpec(
    id: ManualEntryWidgetId,
    type: BodyMeasurementType,
    isEditingWidgets: Boolean,
    onOpenBodyMeasurementEntry: (BodyMeasurementType) -> Unit,
): ManualEntryWidgetSpec {
    val click = if (isEditingWidgets) null else ({ onOpenBodyMeasurementEntry(type) })
    return ManualEntryWidgetSpec(
        id = id,
        title = stringResource(type.titleRes()),
        content = { modifier ->
            ManualEntryMetricTile(
                title = stringResource(type.titleRes()),
                icon = type.icon(),
                accentColor = type.accentColor(),
                modifier = modifier,
                onClick = click,
            )
        },
    )
}

@Composable
internal fun vitalsMeasurementWidgetSpec(
    id: ManualEntryWidgetId,
    type: VitalsMeasurementType,
    isEditingWidgets: Boolean,
    onOpenVitalsMeasurementEntry: (VitalsMeasurementType) -> Unit,
): ManualEntryWidgetSpec {
    val click = if (isEditingWidgets) null else ({ onOpenVitalsMeasurementEntry(type) })
    return ManualEntryWidgetSpec(
        id = id,
        title = stringResource(type.titleRes()),
        content = { modifier ->
            ManualEntryMetricTile(
                title = stringResource(type.titleRes()),
                icon = type.icon(),
                accentColor = type.accentColor(),
                modifier = modifier,
                onClick = click,
            )
        },
    )
}

@Composable
internal fun ManualEntryMetricTile(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    OpenVitalsCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        style = OpenVitalsCardStyle.Accent,
        accentColor = accentColor,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor.copy(alpha = 0.75f),
                modifier = Modifier.size(ManualEntryTileIconSize),
            )
            AutoResizeText(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
    }
}

internal data class ManualEntryWidgetSpec(
    val id: ManualEntryWidgetId,
    val title: String,
    val content: @Composable (Modifier) -> Unit,
)
