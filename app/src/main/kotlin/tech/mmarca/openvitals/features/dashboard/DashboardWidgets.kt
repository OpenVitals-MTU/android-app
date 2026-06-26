package tech.mmarca.openvitals.features.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Accessible
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.insights.SleepScoreConfidence
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.CaloriesBurnedSource
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.DashboardWeeklyCardioLoad
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.features.activity.exerciseTypeIcon
import tech.mmarca.openvitals.features.activity.exerciseTypeLabel
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.components.SourceChip
import tech.mmarca.openvitals.ui.components.DayNavigator
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.features.dashboard.components.DashboardSummaryCard
import tech.mmarca.openvitals.features.dashboard.components.MetricStatCard
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.theme.ActiveCaloriesColor
import tech.mmarca.openvitals.ui.theme.BodyFatColor
import tech.mmarca.openvitals.ui.theme.CaloriesColor
import tech.mmarca.openvitals.ui.theme.CycleColor
import tech.mmarca.openvitals.ui.theme.DistanceColor
import tech.mmarca.openvitals.ui.theme.ElevationColor
import tech.mmarca.openvitals.ui.theme.FloorsColor
import tech.mmarca.openvitals.ui.theme.HeartColor
import tech.mmarca.openvitals.ui.theme.HydrationColor
import tech.mmarca.openvitals.ui.theme.MindfulnessColor
import tech.mmarca.openvitals.ui.theme.NutritionColor
import tech.mmarca.openvitals.ui.theme.SleepColor
import tech.mmarca.openvitals.ui.theme.StepsColor
import tech.mmarca.openvitals.ui.theme.VitalsColor
import tech.mmarca.openvitals.ui.theme.WeightColor
import tech.mmarca.openvitals.ui.theme.WheelchairPushesColor
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import tech.mmarca.openvitals.ui.theme.accentSurfaceContainerColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

private const val DashboardDragLongPressMillis = 500L
private const val DashboardEditWiggleDegrees = 0.45f
private val DashboardCompactWidgetHeight = 82.dp
private val DashboardWidgetGridSpacing = 12.dp
private val DashboardWidgetHorizontalPadding = 14.dp
private val DashboardWidgetSectionPadding = 8.dp
private val DashboardCardHeightWeight = 12.dp
private val DashboardPillIconSize = 36.dp
private val DashboardPillIconCircle = 20.dp
private val DashboardPillProgressHeight = 4.dp
private val DashboardCircleWidgetStroke = 12.dp

@Composable
private fun dashboardContainerColor(hasProgress: Boolean): Color =
    if (hasProgress) {
        MaterialTheme.colorScheme.surfaceContainerHigh
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }

@Composable
private fun dashboardIconContainerColor(accentColor: Color): Color =
    accentSurfaceContainerColor(
        accentColor = accentColor,
        amoledAlpha = 0.2f,
        fallback = MaterialTheme.colorScheme.surfaceContainerHighest,
    )

@Composable
private fun dashboardAccentTrackColor(accentColor: Color): Color =
    accentSurfaceContainerColor(
        accentColor = accentColor,
        amoledAlpha = 0.24f,
        fallback = MaterialTheme.colorScheme.outlineVariant,
    )

@Composable
private fun dashboardProgressFillColor(accentColor: Color): Color =
    accentColor.copy(alpha = 0.62f)

@Composable
internal fun DashboardWidgetGrid(
    ids: List<DashboardWidgetId>,
    rows: Int,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    dropTargetIdsProvider: (DashboardWidgetId, Offset) -> List<DashboardWidgetId>,
    isEditingDashboard: Boolean,
    draggingWidgetId: DashboardWidgetId?,
    draggedWidgetStartBounds: Rect?,
    widgetBounds: MutableMap<DashboardWidgetId, Rect>,
    onDraggingWidgetChanged: (DashboardWidgetId?) -> Unit,
    onDragOffsetChanged: (Offset) -> Unit,
    onMoveWidgetToTarget: (DashboardWidgetId, DashboardWidgetId) -> Unit,
    onRemoveWidget: (DashboardWidgetId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val placements = remember(ids, rows) {
        dashboardGridPlacements(
            ids = ids,
            rows = rows,
        )
    }

    if (placements.isEmpty()) return

    Layout(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DashboardWidgetHorizontalPadding, vertical = DashboardWidgetSectionPadding)
            .animateContentSize(),
        content = {
            placements.forEach { placement ->
                val spec = specsById[placement.id] ?: return@forEach
                val visibleIndex = ids.indexOf(spec.id)
                val previousId = ids.getOrNull(visibleIndex - 1)
                val nextId = ids.getOrNull(visibleIndex + 1)
                key(spec.id) {
                    DashboardWidgetTile(
                        spec = spec,
                        specsById = specsById,
                        isEditingDashboard = isEditingDashboard,
                        onPositioned = { bounds -> widgetBounds[spec.id] = bounds },
                        onDraggingChanged = { isDragging ->
                            onDraggingWidgetChanged(if (isDragging) spec.id else null)
                        },
                        onDragOffsetChanged = onDragOffsetChanged,
                        onDrop = { dragOffset ->
                            closestDashboardWidgetId(
                                draggedId = spec.id,
                                dragOffset = dragOffset,
                                draggedBounds = draggedWidgetStartBounds,
                                targetIds = dropTargetIdsProvider(spec.id, dragOffset),
                                widgetBounds = widgetBounds,
                            )?.let { targetId ->
                                onMoveWidgetToTarget(spec.id, targetId)
                            }
                        },
                        onRemove = { onRemoveWidget(spec.id) },
                        onMovePrevious = previousId?.let { targetId ->
                            { onMoveWidgetToTarget(spec.id, targetId) }
                        },
                        onMoveNext = nextId?.let { targetId ->
                            { onMoveWidgetToTarget(spec.id, targetId) }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
    ) { measurables, constraints ->
        val spacingPx = DashboardWidgetGridSpacing.roundToPx()
        val cellHeightPx = DashboardCompactWidgetHeight.roundToPx()
        val layoutWidth = constraints.maxWidth
        val cellWidth = (
            (layoutWidth - spacingPx * (DashboardWidgetGridColumns - 1)) / DashboardWidgetGridColumns
            ).coerceAtLeast(0)
        val layoutHeight = cellHeightPx * rows + spacingPx * (rows - 1)
        val placeables = measurables.mapIndexed { index, measurable ->
            val rowSpan = placements[index].rowSpan
            val widgetHeight = cellHeightPx * rowSpan + spacingPx * (rowSpan - 1)
            measurable.measure(Constraints.fixed(cellWidth, widgetHeight))
        }

        layout(layoutWidth, layoutHeight) {
            placeables.forEachIndexed { index, placeable ->
                val placement = placements[index]
                val x = placement.column * (cellWidth + spacingPx)
                val y = placement.row * (cellHeightPx + spacingPx)
                placeable.placeRelative(x, y)
            }
        }
    }
}

internal data class DashboardGridPlacement(
    val id: DashboardWidgetId,
    val row: Int,
    val column: Int,
    val rowSpan: Int,
)

internal fun dashboardGridPlacements(
    ids: List<DashboardWidgetId>,
    rows: Int,
): List<DashboardGridPlacement> {
    val usedRows = IntArray(DashboardWidgetGridColumns)
    return buildList {
        ids.forEach { widgetId ->
            val rowSpan = widgetId.dashboardWidgetRowSpan().coerceIn(1, rows)
            val column = usedRows.indices.firstOrNull { usedRows[it] + rowSpan <= rows } ?: return@forEach
            val row = usedRows[column]
            usedRows[column] += rowSpan
            add(
                DashboardGridPlacement(
                    id = widgetId,
                    row = row,
                    column = column,
                    rowSpan = rowSpan,
                )
            )
        }
    }
}

@Composable
internal fun DashboardHiddenWidgets(
    hiddenSpecs: List<DashboardWidgetSpec>,
    onAddWidget: (DashboardWidgetId) -> Unit,
) {
    SectionHeader(stringResource(R.string.dashboard_add_widgets))

    if (hiddenSpecs.isEmpty()) {
        Text(
            text = stringResource(R.string.dashboard_all_widgets_added),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    } else {
        hiddenSpecs.forEach { spec ->
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
internal fun DashboardWidgetTile(
    spec: DashboardWidgetSpec,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    isEditingDashboard: Boolean,
    onPositioned: (Rect) -> Unit,
    onDraggingChanged: (Boolean) -> Unit,
    onDragOffsetChanged: (Offset) -> Unit,
    onDrop: (Offset) -> Unit,
    onRemove: () -> Unit,
    onMovePrevious: (() -> Unit)?,
    onMoveNext: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    var dragOffset by remember(spec.id, isEditingDashboard) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(spec.id, isEditingDashboard) { mutableStateOf(false) }
    val wiggleRotation = if (isEditingDashboard) {
        val wiggleTransition = rememberInfiniteTransition(label = "DashboardWidgetWiggle")
        val rotation by wiggleTransition.animateFloat(
            initialValue = -DashboardEditWiggleDegrees,
            targetValue = DashboardEditWiggleDegrees,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 140,
                    delayMillis = (spec.id.ordinal % 4) * 35,
                    easing = LinearEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "DashboardWidgetWiggleRotation",
        )
        rotation
    } else {
        0f
    }
    val viewConfiguration = LocalViewConfiguration.current
    val dragViewConfiguration = remember(viewConfiguration) {
        object : ViewConfiguration by viewConfiguration {
            override val longPressTimeoutMillis: Long = DashboardDragLongPressMillis
        }
    }
    val dragModifier = if (isEditingDashboard) {
        Modifier.pointerInput(spec.id) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    isDragging = true
                    onDraggingChanged(true)
                    dragOffset = Offset.Zero
                    onDragOffsetChanged(Offset.Zero)
                },
                onDragCancel = {
                    isDragging = false
                    onDraggingChanged(false)
                    dragOffset = Offset.Zero
                    onDragOffsetChanged(Offset.Zero)
                },
                onDragEnd = {
                    val droppedOffset = dragOffset
                    onDrop(droppedOffset)
                    isDragging = false
                    onDraggingChanged(false)
                    dragOffset = Offset.Zero
                    onDragOffsetChanged(Offset.Zero)
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragOffset += dragAmount
                    onDragOffsetChanged(dragOffset)
                },
            )
        }
    } else {
        Modifier
    }
    val removeWidgetLabel = stringResource(R.string.cd_remove_widget)
    val movePreviousLabel = stringResource(R.string.cd_move_widget_up)
    val moveNextLabel = stringResource(R.string.cd_move_widget_down)
    val editSemanticsModifier = if (isEditingDashboard) {
        Modifier.semantics {
            contentDescription = spec.title
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

    CompositionLocalProvider(
        LocalViewConfiguration provides if (isEditingDashboard) dragViewConfiguration else viewConfiguration,
    ) {
        Box(
            modifier = modifier
                .onGloballyPositioned { coordinates -> onPositioned(coordinates.boundsInRoot()) }
                .zIndex(if (isDragging) 1f else 0f)
                .graphicsLayer {
                    alpha = if (isDragging) 0f else 1f
                    rotationZ = if (isEditingDashboard && !isDragging) wiggleRotation else 0f
                }
                .then(editSemanticsModifier)
                .then(dragModifier),
        ) {
            AnimatedContent(
                targetState = spec.id,
                modifier = Modifier.fillMaxSize(),
                label = "DashboardWidgetSwap",
            ) { widgetId ->
                val displayedSpec = specsById[widgetId] ?: spec
                displayedSpec.content(Modifier.fillMaxSize())
            }
            if (isEditingDashboard) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(48.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                            shape = CircleShape,
                        )
                        .clickable(
                            onClickLabel = removeWidgetLabel,
                            onClick = onRemove,
                        ),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

@Composable
internal fun DashboardDraggedWidgetOverlay(
    draggingWidgetId: DashboardWidgetId?,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    widgetBounds: Map<DashboardWidgetId, Rect>,
    draggedWidgetStartBounds: Rect?,
    sectionBounds: Rect?,
    dragOffsetState: State<Offset>,
) {
    val widgetId = draggingWidgetId ?: return
    val spec = specsById[widgetId] ?: return
    val bounds = draggedWidgetStartBounds ?: widgetBounds[widgetId] ?: return
    val section = sectionBounds ?: return
    val density = LocalDensity.current
    val dragOffset by dragOffsetState

    Box(
        modifier = Modifier
            .width(with(density) { bounds.width.toDp() })
            .height(with(density) { bounds.height.toDp() })
            .zIndex(10f)
            .graphicsLayer {
                translationX = bounds.left - section.left + dragOffset.x
                translationY = bounds.top - section.top + dragOffset.y
                scaleX = 1.02f
                scaleY = 1.02f
                shadowElevation = with(density) { 12.dp.toPx() }
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium,
                )
        ) {
            spec.content(Modifier.fillMaxSize())
        }
    }
}

internal fun closestDashboardWidgetId(
    draggedId: DashboardWidgetId,
    dragOffset: Offset,
    draggedBounds: Rect?,
    targetIds: List<DashboardWidgetId>,
    widgetBounds: Map<DashboardWidgetId, Rect>,
): DashboardWidgetId? {
    val draggedBounds = draggedBounds ?: widgetBounds[draggedId] ?: return null
    val dropCenter = draggedBounds.center + dragOffset

    return targetIds
        .filter { it in widgetBounds }
        .minByOrNull { widgetId ->
            val center = widgetBounds.getValue(widgetId).center
            val delta = dropCenter - center
            delta.x * delta.x + delta.y * delta.y
        }
}

internal fun Rect.containsPoint(point: Offset): Boolean =
    point.x >= left && point.x <= right && point.y >= top && point.y <= bottom


@Composable
internal fun DashboardMetricStatWidget(
    title: String,
    value: DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    progress: DashboardWidgetProgress? = null,
    message: String? = null,
    subtitle: String? = null,
    subtitleColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    showTitle: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    MetricStatCard(
        title = title,
        value = value,
        icon = icon,
        accentColor = accentColor,
        modifier = modifier,
        message = message,
        subtitle = subtitle,
        subtitleColor = subtitleColor,
        showTitle = showTitle,
        progressFraction = progress?.fraction,
        onClick = onClick,
    )
}

@Composable
internal fun DashboardPillWidget(
    title: String,
    value: DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    progress: DashboardWidgetProgress? = null,
    message: String? = null,
    subtitle: String? = null,
    subtitleColor: Color = accentColor,
    showTitle: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.medium
    val containerColor = dashboardContainerColor(progress != null)
    val iconContainerColor = dashboardIconContainerColor(accentColor)
    val progressFillColor = dashboardProgressFillColor(accentColor)
    OpenVitalsCard(
        modifier = modifier
            .fillMaxWidth(),
        onClick = onClick,
        containerColor = containerColor,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = DashboardCardHeightWeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(DashboardPillIconSize)
                        .background(
                            color = iconContainerColor,
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(DashboardPillIconCircle),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (showTitle) {
                        AutoResizeText(
                            text = title,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                    AutoResizeText(
                        text = message ?: dashboardDisplayValue(value),
                        style = if (subtitle == null || !showTitle) {
                            MaterialTheme.typography.titleLarge
                        } else {
                            MaterialTheme.typography.titleMedium
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = if (message == null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                    )
                    if (message == null && subtitle != null) {
                        AutoResizeText(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = subtitleColor,
                            maxLines = 1,
                        )
                    }
                }
            }
            if (progress != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .height(DashboardPillProgressHeight)
                        .fillMaxWidth(progress.fraction)
                        .background(progressFillColor),
                )
            }
        }
    }
}

@Composable
internal fun DashboardCircleWidget(
    title: String,
    value: DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    progress: DashboardWidgetProgress,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    DashboardSummaryCard(
        title = title,
        value = value,
        icon = icon,
        accentColor = accentColor,
        progress = progress,
        modifier = modifier,
        onClick = onClick,
    )
}

internal fun dashboardDisplayValue(value: DisplayValue): String =
    if (value.unit.isBlank()) {
        value.value
    } else {
        "${value.value} ${value.unit}"
    }

@Composable
internal fun sleepScoreRatingLabel(score: Int): String =
    stringResource(
        when {
            score >= 90 -> R.string.sleep_score_rating_excellent
            score >= 80 -> R.string.sleep_score_rating_good
            score >= 60 -> R.string.sleep_score_rating_fair
            else -> R.string.sleep_score_rating_poor
        }
    )

@Composable
internal fun dashboardGramDisplayValue(value: Double, unitFormatter: UnitFormatter): DisplayValue =
    DisplayValue(unitFormatter.count(value.roundToInt()), stringResource(R.string.unit_grams))

internal fun dashboardGoalProgress(current: Double, target: Double, label: String): DashboardWidgetProgress =
    DashboardWidgetProgress(
        fraction = if (target > 0.0) {
            (current / target).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        },
        label = label,
    )

@Composable
internal fun WorkoutCard(
    workout: ExerciseData,
    zone: ZoneId,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
) {
    val start = workout.startTime.atZone(zone)
    OpenVitalsCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = dashboardIconContainerColor(WorkoutColor),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = exerciseTypeIcon(workout.exerciseType),
                        contentDescription = null,
                        tint = WorkoutColor,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.metric_workout),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (onEdit != null) {
                OpenVitalsIconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp),
                ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = stringResource(R.string.cd_edit_entry),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                if (workout.source.isNotBlank()) {
                    SourceChip(source = workout.source)
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = exerciseTypeLabel(workout.exerciseType),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = unitFormatter.duration(workout.durationMs),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = dateTimeFormatterProvider.shortTime().format(start),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
