package tech.mmarca.openvitals.features.manualentry

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.HydrationColor
import tech.mmarca.openvitals.ui.theme.MindfulnessColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import tech.mmarca.openvitals.ui.theme.accentSurfaceContainerColor

private const val ManualEntryGridColumns = 3
private const val ManualEntryDragLongPressMillis = 500L
private const val ManualEntryEditWiggleDegrees = 0.45f
private val ManualEntryTileIconSize = 34.dp

@Composable
fun ManualEntryScreen(
    viewModel: ManualEntryViewModel,
    onOpenHydrationEntry: () -> Unit,
    onOpenActivityEntry: () -> Unit,
    onOpenMindfulnessEntry: () -> Unit,
    onOpenBodyMeasurementEntry: (BodyMeasurementType) -> Unit,
    onOpenVitalsMeasurementEntry: (VitalsMeasurementType) -> Unit,
    onEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.onHydrationWritePermissionResult()
    }
    val requestBodyWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.onBodyWritePermissionResult()
    }
    val requestActivityWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.onActivityWritePermissionResult()
    }
    val requestVitalsWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.onVitalsWritePermissionResult()
    }
    val requestMindfulnessWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.onMindfulnessWritePermissionResult()
    }
    val specs = manualEntryWidgetSpecs(
        isEditingWidgets = state.isEditingWidgets,
        onOpenHydrationEntry = viewModel::onHydrationWidgetTapped,
        onOpenActivityEntry = viewModel::onActivityWidgetTapped,
        onOpenMindfulnessEntry = viewModel::onMindfulnessWidgetTapped,
        onOpenBodyMeasurementEntry = viewModel::onBodyMeasurementWidgetTapped,
        onOpenVitalsMeasurementEntry = viewModel::onVitalsMeasurementWidgetTapped,
    )
    val specsById = specs.associateBy { it.id }
    val visibleIds = state.widgets.filter { it in specsById }
    val hiddenSpecs = specs.filter { it.id !in visibleIds }

    LaunchedEffect(state.isEditingWidgets) {
        onEditStateChanged(state.isEditingWidgets, viewModel::toggleWidgetEdit)
    }
    DisposableEffect(Unit) {
        onDispose { onEditStateChanged(false) {} }
    }
    LaunchedEffect(state.pendingHydrationEntryNavigation) {
        if (state.pendingHydrationEntryNavigation) {
            viewModel.onHydrationEntryNavigationHandled()
            onOpenHydrationEntry()
        }
    }
    LaunchedEffect(state.pendingActivityEntryNavigation) {
        if (state.pendingActivityEntryNavigation) {
            viewModel.onActivityEntryNavigationHandled()
            onOpenActivityEntry()
        }
    }
    LaunchedEffect(state.pendingMindfulnessEntryNavigation) {
        if (state.pendingMindfulnessEntryNavigation) {
            viewModel.onMindfulnessEntryNavigationHandled()
            onOpenMindfulnessEntry()
        }
    }
    LaunchedEffect(state.pendingBodyEntryNavigation) {
        val type = state.pendingBodyEntryNavigation
        if (type != null) {
            viewModel.onBodyEntryNavigationHandled()
            onOpenBodyMeasurementEntry(type)
        }
    }
    LaunchedEffect(state.pendingVitalsEntryNavigation) {
        val type = state.pendingVitalsEntryNavigation
        if (type != null) {
            viewModel.onVitalsEntryNavigationHandled()
            onOpenVitalsMeasurementEntry(type)
        }
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            ManualEntryWidgetGrid(
                visibleIds = visibleIds,
                specsById = specsById,
                isEditingWidgets = state.isEditingWidgets,
                onMoveWidgetToTarget = viewModel::moveWidgetToTarget,
                onRemoveWidget = viewModel::removeWidget,
            )
        }
        if (state.isEditingWidgets) {
            hiddenManualEntryWidgets(
                hiddenSpecs = hiddenSpecs,
                onAddWidget = viewModel::addWidget,
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }

    if (state.showHydrationWritePermissionPrompt) {
        HydrationWritePermissionPrompt(
            onDismiss = viewModel::dismissHydrationWritePermissionPrompt,
            onOpenEntry = viewModel::continueHydrationEntryFromWritePermissionPrompt,
            onGrant = {
                viewModel.grantHydrationWritePermissionFromPrompt()
                requestWritePermissions.launch(state.hydrationWritePermissions)
            },
        )
    }

    if (state.showActivityWritePermissionPrompt) {
        ActivityWritePermissionPrompt(
            onDismiss = viewModel::dismissActivityWritePermissionPrompt,
            onOpenEntry = viewModel::continueActivityEntryFromWritePermissionPrompt,
            onGrant = {
                viewModel.grantActivityWritePermissionFromPrompt()
                requestActivityWritePermissions.launch(state.activityWritePermissions)
            },
        )
    }

    if (state.showBodyWritePermissionPrompt) {
        state.bodyWritePermissionPromptType?.let { type ->
            BodyWritePermissionPrompt(
                type = type,
                onDismiss = viewModel::dismissBodyWritePermissionPrompt,
                onOpenEntry = viewModel::continueBodyEntryFromWritePermissionPrompt,
                onGrant = {
                    viewModel.grantBodyWritePermissionFromPrompt()
                    requestBodyWritePermissions.launch(state.bodyWritePermissions)
                },
            )
        }
    }

    if (state.showVitalsWritePermissionPrompt) {
        state.vitalsWritePermissionPromptType?.let { type ->
            VitalsWritePermissionPrompt(
                type = type,
                onDismiss = viewModel::dismissVitalsWritePermissionPrompt,
                onOpenEntry = viewModel::continueVitalsEntryFromWritePermissionPrompt,
                onGrant = {
                    viewModel.grantVitalsWritePermissionFromPrompt()
                    requestVitalsWritePermissions.launch(state.vitalsWritePermissions)
                },
            )
        }
    }

    if (state.showMindfulnessWritePermissionPrompt) {
        MindfulnessWritePermissionPrompt(
            onDismiss = viewModel::dismissMindfulnessWritePermissionPrompt,
            onOpenEntry = viewModel::continueMindfulnessEntryFromWritePermissionPrompt,
            onGrant = {
                viewModel.grantMindfulnessWritePermissionFromPrompt()
                requestMindfulnessWritePermissions.launch(state.mindfulnessWritePermissions)
            },
        )
    }
}

@Composable
private fun HydrationWritePermissionPrompt(
    onDismiss: () -> Unit,
    onOpenEntry: () -> Unit,
    onGrant: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_entry_write_permission_title)) },
        text = { Text(stringResource(R.string.hydration_tracker_permission_needed)) },
        confirmButton = {
            Button(onClick = onGrant) {
                Text(stringResource(R.string.action_grant))
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenEntry) {
                Text(stringResource(R.string.action_open))
            }
        },
    )
}

@Composable
private fun MindfulnessWritePermissionPrompt(
    onDismiss: () -> Unit,
    onOpenEntry: () -> Unit,
    onGrant: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_entry_mindfulness_write_permission_title)) },
        text = { Text(stringResource(R.string.mindfulness_entry_permission_needed)) },
        confirmButton = {
            Button(onClick = onGrant) {
                Text(stringResource(R.string.action_grant))
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenEntry) {
                Text(stringResource(R.string.action_open))
            }
        },
    )
}

@Composable
private fun ActivityWritePermissionPrompt(
    onDismiss: () -> Unit,
    onOpenEntry: () -> Unit,
    onGrant: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.manual_entry_activity_write_permission_title)) },
        text = { Text(stringResource(R.string.activity_entry_permission_needed)) },
        confirmButton = {
            Button(onClick = onGrant) {
                Text(stringResource(R.string.action_grant))
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenEntry) {
                Text(stringResource(R.string.action_open))
            }
        },
    )
}

@Composable
private fun BodyWritePermissionPrompt(
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
            Button(onClick = onGrant) {
                Text(stringResource(R.string.action_grant))
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenEntry) {
                Text(stringResource(R.string.action_open))
            }
        },
    )
}

@Composable
private fun VitalsWritePermissionPrompt(
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
            Button(onClick = onGrant) {
                Text(stringResource(R.string.action_grant))
            }
        },
        dismissButton = {
            TextButton(onClick = onOpenEntry) {
                Text(stringResource(R.string.action_open))
            }
        },
    )
}

@Composable
private fun ManualEntryWidgetGrid(
    visibleIds: List<ManualEntryWidgetId>,
    specsById: Map<ManualEntryWidgetId, ManualEntryWidgetSpec>,
    isEditingWidgets: Boolean,
    onMoveWidgetToTarget: (ManualEntryWidgetId, ManualEntryWidgetId) -> Unit,
    onRemoveWidget: (ManualEntryWidgetId) -> Unit,
) {
    val widgetBounds = remember { mutableStateMapOf<ManualEntryWidgetId, Rect>() }

    LaunchedEffect(visibleIds) {
        val visibleSet = visibleIds.toSet()
        widgetBounds.keys.toList().forEach { widgetId ->
            if (widgetId !in visibleSet) {
                widgetBounds.remove(widgetId)
            }
        }
    }

    Column {
        visibleIds
            .mapNotNull { specsById[it] }
            .chunked(ManualEntryGridColumns)
            .forEach { rowSpecs ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .animateContentSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowSpecs.forEach { spec ->
                        ManualEntryWidgetTile(
                            spec = spec,
                            isEditingWidgets = isEditingWidgets,
                            widgetBounds = widgetBounds,
                            onPositioned = { bounds -> widgetBounds[spec.id] = bounds },
                            onMoveWidgetToTarget = onMoveWidgetToTarget,
                            onRemove = { onRemoveWidget(spec.id) },
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                        )
                    }
                    repeat(ManualEntryGridColumns - rowSpecs.size) {
                        Spacer(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
    }
}

@Composable
private fun ManualEntryWidgetTile(
    spec: ManualEntryWidgetSpec,
    isEditingWidgets: Boolean,
    widgetBounds: Map<ManualEntryWidgetId, Rect>,
    onPositioned: (Rect) -> Unit,
    onMoveWidgetToTarget: (ManualEntryWidgetId, ManualEntryWidgetId) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragOffset by remember(spec.id, isEditingWidgets) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(spec.id, isEditingWidgets) { mutableStateOf(false) }
    val density = LocalDensity.current
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
    val viewConfiguration = LocalViewConfiguration.current
    val dragViewConfiguration = remember(viewConfiguration) {
        object : ViewConfiguration by viewConfiguration {
            override val longPressTimeoutMillis: Long = ManualEntryDragLongPressMillis
        }
    }
    val dragModifier = if (isEditingWidgets) {
        Modifier.pointerInput(spec.id) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    isDragging = true
                    dragOffset = Offset.Zero
                },
                onDragCancel = {
                    isDragging = false
                    dragOffset = Offset.Zero
                },
                onDragEnd = {
                    val droppedOffset = dragOffset
                    closestManualEntryWidgetId(
                        draggedId = spec.id,
                        dragOffset = droppedOffset,
                        widgetBounds = widgetBounds,
                    )?.let { targetId ->
                        onMoveWidgetToTarget(spec.id, targetId)
                    }
                    isDragging = false
                    dragOffset = Offset.Zero
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount
                },
            )
        }
    } else {
        Modifier
    }

    CompositionLocalProvider(
        LocalViewConfiguration provides if (isEditingWidgets) dragViewConfiguration else viewConfiguration,
    ) {
        Box(
            modifier = modifier
                .onGloballyPositioned { coordinates -> onPositioned(coordinates.boundsInRoot()) }
                .zIndex(if (isDragging) 1f else 0f)
                .graphicsLayer {
                    translationX = if (isDragging) dragOffset.x else 0f
                    translationY = if (isDragging) dragOffset.y else 0f
                    rotationZ = if (isEditingWidgets && !isDragging) wiggleRotation else 0f
                    scaleX = if (isDragging) 1.02f else 1f
                    scaleY = if (isDragging) 1.02f else 1f
                    shadowElevation = if (isDragging) with(density) { 12.dp.toPx() } else 0f
                }
                .then(dragModifier),
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
}

private fun LazyListScope.hiddenManualEntryWidgets(
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
            OutlinedButton(
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
private fun manualEntryWidgetSpecs(
    isEditingWidgets: Boolean,
    onOpenHydrationEntry: () -> Unit,
    onOpenActivityEntry: () -> Unit,
    onOpenMindfulnessEntry: () -> Unit,
    onOpenBodyMeasurementEntry: (BodyMeasurementType) -> Unit,
    onOpenVitalsMeasurementEntry: (VitalsMeasurementType) -> Unit,
): List<ManualEntryWidgetSpec> {
    val hydrationClick = if (isEditingWidgets) null else onOpenHydrationEntry
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
private fun bodyMeasurementWidgetSpec(
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
private fun vitalsMeasurementWidgetSpec(
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
private fun ManualEntryMetricTile(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = accentSurfaceContainerColor(accentColor, amoledAlpha = 0.09f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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

private fun closestManualEntryWidgetId(
    draggedId: ManualEntryWidgetId,
    dragOffset: Offset,
    widgetBounds: Map<ManualEntryWidgetId, Rect>,
): ManualEntryWidgetId? {
    val draggedBounds = widgetBounds[draggedId] ?: return null
    val dropCenter = draggedBounds.center + dragOffset

    return widgetBounds.keys
        .filter { it != draggedId }
        .minByOrNull { widgetId ->
            val center = widgetBounds.getValue(widgetId).center
            val delta = dropCenter - center
            delta.x * delta.x + delta.y * delta.y
        }
}

private data class ManualEntryWidgetSpec(
    val id: ManualEntryWidgetId,
    val title: String,
    val content: @Composable (Modifier) -> Unit,
)
