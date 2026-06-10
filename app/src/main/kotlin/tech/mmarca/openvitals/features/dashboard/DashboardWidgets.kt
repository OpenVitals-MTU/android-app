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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import tech.mmarca.openvitals.ui.components.DayNavigator
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.MetricCardPlaceholder
import tech.mmarca.openvitals.ui.components.PermissionCallout
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.SectionHeader
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
            .padding(horizontal = 16.dp, vertical = 6.dp)
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
internal fun dashboardWidgetSpecs(
    data: DashboardData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    trackCycle: Boolean,
    dailyGoals: DashboardDailyGoals,
    widgetIds: Collection<DashboardWidgetId>,
    pendingWidgets: Set<DashboardWidgetId>,
    isEditingDashboard: Boolean,
    onOpenMetric: (DashboardWidgetId) -> Unit,
): List<DashboardWidgetSpec> = buildList {
    val widgetIdsToBuild = widgetIds.toSet()
    fun shouldBuild(widgetId: DashboardWidgetId): Boolean =
        widgetId in widgetIdsToBuild && (trackCycle || widgetId != DashboardWidgetId.CYCLE)
    val loadingMessage = stringResource(R.string.loading)
    fun loadingMessageFor(widgetId: DashboardWidgetId): String? =
        loadingMessage.takeIf { widgetId in pendingWidgets }
    val openMetric: (DashboardWidgetId) -> (() -> Unit)? = { widgetId ->
        if (isEditingDashboard) null else ({ onOpenMetric(widgetId) })
    }
    val sleepGoalMs = (dailyGoals.sleepHours * 60.0 * 60.0 * 1000.0).toLong()

    if (shouldBuild(DashboardWidgetId.STEPS)) {
        addMetric(
            id = DashboardWidgetId.STEPS,
            title = stringResource(R.string.metric_steps),
            value = DisplayValue(unitFormatter.count(data.steps), stringResource(R.string.unit_steps)),
            icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
            accentColor = StepsColor,
            progress = dashboardGoalProgress(
                current = data.steps.toDouble(),
                target = dailyGoals.steps,
                label = stringResource(R.string.dashboard_goal_of, unitFormatter.count(dailyGoals.steps.roundToInt())),
            ),
            style = DashboardWidgetStyle.CIRCLE,
            loadingMessage = loadingMessageFor(DashboardWidgetId.STEPS),
            onClick = openMetric(DashboardWidgetId.STEPS),
        )
    }
    if (shouldBuild(DashboardWidgetId.WEEKLY_CARDIO_LOAD)) {
        addWeeklyCardioLoadMetric(
            id = DashboardWidgetId.WEEKLY_CARDIO_LOAD,
            title = stringResource(R.string.metric_weekly_cardio_load),
            weeklyCardioLoad = data.weeklyCardioLoad,
            icon = Icons.Outlined.Favorite,
            accentColor = WorkoutColor,
            style = DashboardWidgetStyle.CIRCLE,
            loadingMessage = loadingMessageFor(DashboardWidgetId.WEEKLY_CARDIO_LOAD),
            onClick = openMetric(DashboardWidgetId.WEEKLY_CARDIO_LOAD),
        )
    }
    if (shouldBuild(DashboardWidgetId.CARDIO_LOAD)) {
        addWeeklyCardioLoadMetric(
            id = DashboardWidgetId.CARDIO_LOAD,
            title = stringResource(R.string.metric_weekly_cardio_load),
            weeklyCardioLoad = data.weeklyCardioLoad,
            icon = Icons.Outlined.Favorite,
            accentColor = WorkoutColor,
            style = DashboardWidgetStyle.PILL,
            loadingMessage = loadingMessageFor(DashboardWidgetId.CARDIO_LOAD),
            onClick = openMetric(DashboardWidgetId.CARDIO_LOAD),
        )
    }
    if (shouldBuild(DashboardWidgetId.DISTANCE)) {
        addMetric(
            id = DashboardWidgetId.DISTANCE,
            title = stringResource(R.string.metric_distance),
            value = unitFormatter.distance(data.distanceMeters),
            icon = Icons.Outlined.Straighten,
            accentColor = DistanceColor,
            progress = dashboardGoalProgress(
                current = data.distanceMeters,
                target = dailyGoals.distanceMeters,
                label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.distance(dailyGoals.distanceMeters))),
            ),
            loadingMessage = loadingMessageFor(DashboardWidgetId.DISTANCE),
            onClick = openMetric(DashboardWidgetId.DISTANCE),
        )
    }
    if (shouldBuild(DashboardWidgetId.CALORIES_OUT)) {
        val caloriesKcal = if (data.caloriesKcalSource == CaloriesBurnedSource.NO_DATA) 0.0 else data.caloriesKcal
        val caloriesValue = unitFormatter.energy(caloriesKcal)
        addOptionalMetric(
            id = DashboardWidgetId.CALORIES_OUT,
            title = stringResource(R.string.metric_calories_out),
            value = caloriesValue,
            icon = Icons.Outlined.LocalFireDepartment,
            accentColor = CaloriesColor,
            subtitle = if (data.caloriesKcalSource == CaloriesBurnedSource.ESTIMATED_ACTIVE_AND_BMR) {
                stringResource(R.string.calories_estimated_active_bmr)
            } else {
                null
            },
            subtitleColor = MaterialTheme.colorScheme.onSurface,
            progress = dashboardGoalProgress(
                current = caloriesKcal,
                target = dailyGoals.caloriesOutKcal,
                label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.energy(dailyGoals.caloriesOutKcal))),
            ),
            loadingMessage = loadingMessageFor(DashboardWidgetId.CALORIES_OUT),
            onClick = openMetric(DashboardWidgetId.CALORIES_OUT),
        )
    }
    if (shouldBuild(DashboardWidgetId.ACTIVE_CALORIES)) {
        addOptionalMetric(
            id = DashboardWidgetId.ACTIVE_CALORIES,
            title = stringResource(R.string.metric_active_calories),
            value = data.activeCaloriesKcal?.let(unitFormatter::energy),
            icon = Icons.Outlined.LocalFireDepartment,
            accentColor = ActiveCaloriesColor,
            progress = data.activeCaloriesKcal?.let {
                dashboardGoalProgress(
                    current = it,
                    target = dailyGoals.activeCaloriesKcal,
                    label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.energy(dailyGoals.activeCaloriesKcal))),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.ACTIVE_CALORIES),
            onClick = openMetric(DashboardWidgetId.ACTIVE_CALORIES),
        )
    }
    if (shouldBuild(DashboardWidgetId.FLOORS)) {
        addOptionalMetric(
            id = DashboardWidgetId.FLOORS,
            title = stringResource(R.string.metric_floors_climbed),
            value = data.floorsClimbed?.let {
                DisplayValue(unitFormatter.count(it), stringResource(R.string.unit_floors))
            },
            icon = Icons.Outlined.Stairs,
            accentColor = FloorsColor,
            progress = data.floorsClimbed?.let {
                dashboardGoalProgress(
                    current = it.toDouble(),
                    target = dailyGoals.floors,
                    label = stringResource(R.string.dashboard_goal_of, unitFormatter.count(dailyGoals.floors.roundToInt())),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.FLOORS),
            onClick = openMetric(DashboardWidgetId.FLOORS),
        )
    }
    if (shouldBuild(DashboardWidgetId.ELEVATION)) {
        addOptionalMetric(
            id = DashboardWidgetId.ELEVATION,
            title = stringResource(R.string.metric_elevation),
            value = data.elevationGainedMeters?.let(unitFormatter::elevation),
            icon = Icons.Outlined.Terrain,
            accentColor = ElevationColor,
            progress = data.elevationGainedMeters?.let {
                dashboardGoalProgress(
                    current = it,
                    target = dailyGoals.elevationMeters,
                    label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.elevation(dailyGoals.elevationMeters))),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.ELEVATION),
            onClick = openMetric(DashboardWidgetId.ELEVATION),
        )
    }
    if (shouldBuild(DashboardWidgetId.WHEELCHAIR_PUSHES)) {
        addOptionalMetric(
            id = DashboardWidgetId.WHEELCHAIR_PUSHES,
            title = stringResource(R.string.metric_wheelchair_pushes),
            value = data.wheelchairPushes?.let {
                DisplayValue(unitFormatter.count(it), stringResource(R.string.unit_pushes))
            },
            icon = Icons.AutoMirrored.Outlined.Accessible,
            accentColor = WheelchairPushesColor,
            progress = data.wheelchairPushes?.let {
                dashboardGoalProgress(
                    current = it.toDouble(),
                    target = dailyGoals.wheelchairPushes,
                    label = stringResource(
                        R.string.dashboard_goal_of,
                        unitFormatter.count(dailyGoals.wheelchairPushes.roundToInt()),
                    ),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.WHEELCHAIR_PUSHES),
            onClick = openMetric(DashboardWidgetId.WHEELCHAIR_PUSHES),
        )
    }
    if (shouldBuild(DashboardWidgetId.SLEEP)) {
        val sleepScoreSubtitle = data.sleepScore
            .takeIf { it.confidence != SleepScoreConfidence.NO_DATA }
            ?.let { score ->
                stringResource(
                    R.string.dashboard_sleep_score_subtitle,
                    unitFormatter.count(score.score),
                    sleepScoreRatingLabel(score.score),
                )
            }
        addOptionalMetric(
            id = DashboardWidgetId.SLEEP,
            title = stringResource(R.string.metric_sleep),
            value = data.sleep?.let { DisplayValue(unitFormatter.duration(it.durationMs), "") },
            icon = Icons.Outlined.Bed,
            accentColor = SleepColor,
            noDataMessage = stringResource(R.string.message_no_sleep_day),
            subtitle = sleepScoreSubtitle,
            subtitleColor = MaterialTheme.colorScheme.onSurface,
            showTitle = false,
            progress = data.sleep?.let {
                dashboardGoalProgress(
                    current = it.durationMs.toDouble(),
                    target = sleepGoalMs.toDouble(),
                    label = stringResource(R.string.dashboard_goal_of, unitFormatter.duration(sleepGoalMs)),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.SLEEP),
            onClick = openMetric(DashboardWidgetId.SLEEP),
        )
    }
    if (shouldBuild(DashboardWidgetId.HYDRATION)) {
        addMetric(
            id = DashboardWidgetId.HYDRATION,
            title = stringResource(R.string.metric_hydration),
            value = unitFormatter.hydration(data.hydrationLiters),
            icon = Icons.Outlined.LocalDrink,
            accentColor = HydrationColor,
            progress = dashboardGoalProgress(
                current = data.hydrationLiters,
                target = dailyGoals.hydrationLiters,
                label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.hydration(dailyGoals.hydrationLiters))),
            ),
            loadingMessage = loadingMessageFor(DashboardWidgetId.HYDRATION),
            onClick = openMetric(DashboardWidgetId.HYDRATION),
        )
    }
    if (shouldBuild(DashboardWidgetId.CALORIES_IN)) {
        addOptionalMetric(
            id = DashboardWidgetId.CALORIES_IN,
            title = stringResource(R.string.metric_calories_in),
            value = data.caloriesInKcal?.let(unitFormatter::energy),
            icon = Icons.Outlined.Restaurant,
            accentColor = NutritionColor,
            progress = data.caloriesInKcal?.let {
                dashboardGoalProgress(
                    current = it,
                    target = dailyGoals.caloriesInKcal,
                    label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.energy(dailyGoals.caloriesInKcal))),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.CALORIES_IN),
            onClick = openMetric(DashboardWidgetId.CALORIES_IN),
        )
    }
    if (shouldBuild(DashboardWidgetId.PROTEIN)) {
        addOptionalMetric(
            id = DashboardWidgetId.PROTEIN,
            title = stringResource(R.string.metric_protein),
            value = data.proteinGrams?.let { DisplayValue(unitFormatter.count(it.roundToInt()), stringResource(R.string.unit_grams)) },
            icon = Icons.Outlined.Restaurant,
            accentColor = NutritionColor,
            progress = data.proteinGrams?.let {
                dashboardGoalProgress(
                    current = it,
                    target = dailyGoals.proteinGrams,
                    label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(dashboardGramDisplayValue(dailyGoals.proteinGrams, unitFormatter))),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.PROTEIN),
            onClick = openMetric(DashboardWidgetId.PROTEIN),
        )
    }
    if (shouldBuild(DashboardWidgetId.CARBS)) {
        addOptionalMetric(
            id = DashboardWidgetId.CARBS,
            title = stringResource(R.string.metric_carbs),
            value = data.carbsGrams?.let { DisplayValue(unitFormatter.count(it.roundToInt()), stringResource(R.string.unit_grams)) },
            icon = Icons.Outlined.Restaurant,
            accentColor = NutritionColor,
            progress = data.carbsGrams?.let {
                dashboardGoalProgress(
                    current = it,
                    target = dailyGoals.carbsGrams,
                    label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(dashboardGramDisplayValue(dailyGoals.carbsGrams, unitFormatter))),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.CARBS),
            onClick = openMetric(DashboardWidgetId.CARBS),
        )
    }
    if (shouldBuild(DashboardWidgetId.FAT)) {
        addOptionalMetric(
            id = DashboardWidgetId.FAT,
            title = stringResource(R.string.metric_fat),
            value = data.fatGrams?.let { DisplayValue(unitFormatter.count(it.roundToInt()), stringResource(R.string.unit_grams)) },
            icon = Icons.Outlined.Restaurant,
            accentColor = NutritionColor,
            progress = data.fatGrams?.let {
                dashboardGoalProgress(
                    current = it,
                    target = dailyGoals.fatGrams,
                    label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(dashboardGramDisplayValue(dailyGoals.fatGrams, unitFormatter))),
                )
            },
            loadingMessage = loadingMessageFor(DashboardWidgetId.FAT),
            onClick = openMetric(DashboardWidgetId.FAT),
        )
    }
    if (shouldBuild(DashboardWidgetId.WEIGHT)) {
        addOptionalMetric(
            id = DashboardWidgetId.WEIGHT,
            title = stringResource(R.string.metric_latest_weight),
            value = data.weightKg?.let(unitFormatter::weight),
            subtitle = data.weightTime?.let { dashboardMeasurementDate(it, dateTimeFormatterProvider) },
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.WEIGHT),
            onClick = openMetric(DashboardWidgetId.WEIGHT),
        )
    }
    if (shouldBuild(DashboardWidgetId.HEIGHT)) {
        addOptionalMetric(
            id = DashboardWidgetId.HEIGHT,
            title = stringResource(R.string.metric_height),
            value = data.heightCm?.let(unitFormatter::height),
            subtitle = data.heightTime?.let { dashboardMeasurementDate(it, dateTimeFormatterProvider) },
            icon = Icons.Outlined.Straighten,
            accentColor = WeightColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.HEIGHT),
            onClick = openMetric(DashboardWidgetId.HEIGHT),
        )
    }
    if (shouldBuild(DashboardWidgetId.BMI)) {
        addOptionalMetric(
            id = DashboardWidgetId.BMI,
            title = stringResource(R.string.metric_bmi),
            value = data.bmi?.let { DisplayValue(unitFormatter.decimal(it, 1), "") },
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.BMI),
            onClick = openMetric(DashboardWidgetId.BMI),
        )
    }
    if (shouldBuild(DashboardWidgetId.BODY_FAT)) {
        addMetric(
            id = DashboardWidgetId.BODY_FAT,
            title = stringResource(R.string.metric_body_fat),
            value = unitFormatter.percent(data.bodyFatPercent),
            icon = Icons.Outlined.MonitorWeight,
            accentColor = BodyFatColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.BODY_FAT),
            onClick = openMetric(DashboardWidgetId.BODY_FAT),
        )
    }
    if (shouldBuild(DashboardWidgetId.LEAN_MASS)) {
        addOptionalMetric(
            id = DashboardWidgetId.LEAN_MASS,
            title = stringResource(R.string.metric_lean_mass),
            value = data.leanMassKg?.let(unitFormatter::bodyMass),
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.LEAN_MASS),
            onClick = openMetric(DashboardWidgetId.LEAN_MASS),
        )
    }
    if (shouldBuild(DashboardWidgetId.BMR)) {
        addOptionalMetric(
            id = DashboardWidgetId.BMR,
            title = stringResource(R.string.metric_bmr),
            value = data.bmrKcal?.let(unitFormatter::energy),
            icon = Icons.Outlined.LocalFireDepartment,
            accentColor = CaloriesColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.BMR),
            onClick = openMetric(DashboardWidgetId.BMR),
        )
    }
    if (shouldBuild(DashboardWidgetId.BONE_MASS)) {
        addOptionalMetric(
            id = DashboardWidgetId.BONE_MASS,
            title = stringResource(R.string.metric_bone_mass),
            value = data.boneMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
            icon = Icons.Outlined.MonitorWeight,
            accentColor = WeightColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.BONE_MASS),
            onClick = openMetric(DashboardWidgetId.BONE_MASS),
        )
    }
    if (shouldBuild(DashboardWidgetId.BODY_WATER_MASS)) {
        addOptionalMetric(
            id = DashboardWidgetId.BODY_WATER_MASS,
            title = stringResource(R.string.metric_body_water_mass),
            value = data.bodyWaterMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
            icon = Icons.Outlined.LocalDrink,
            accentColor = WeightColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.BODY_WATER_MASS),
            onClick = openMetric(DashboardWidgetId.BODY_WATER_MASS),
        )
    }
    if (shouldBuild(DashboardWidgetId.AVG_HEART_RATE)) {
        addMetric(
            id = DashboardWidgetId.AVG_HEART_RATE,
            title = stringResource(R.string.metric_avg_heart_rate),
            value = unitFormatter.heartRate(data.avgHeartRateBpm),
            icon = Icons.Outlined.Favorite,
            accentColor = HeartColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.AVG_HEART_RATE),
            onClick = openMetric(DashboardWidgetId.AVG_HEART_RATE),
        )
    }
    if (shouldBuild(DashboardWidgetId.RESTING_HEART_RATE)) {
        addMetric(
            id = DashboardWidgetId.RESTING_HEART_RATE,
            title = stringResource(R.string.metric_resting_heart_rate),
            value = unitFormatter.heartRate(data.restingHeartRateBpm),
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = HeartColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.RESTING_HEART_RATE),
            onClick = openMetric(DashboardWidgetId.RESTING_HEART_RATE),
        )
    }
    if (shouldBuild(DashboardWidgetId.HRV)) {
        addOptionalMetric(
            id = DashboardWidgetId.HRV,
            title = stringResource(R.string.metric_hrv),
            value = data.hrvRmssdMs?.let(unitFormatter::hrv),
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = HeartColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.HRV),
            onClick = openMetric(DashboardWidgetId.HRV),
        )
    }
    if (shouldBuild(DashboardWidgetId.BLOOD_PRESSURE)) {
        addOptionalMetric(
            id = DashboardWidgetId.BLOOD_PRESSURE,
            title = stringResource(R.string.metric_blood_pressure),
            value = if (data.latestSystolicMmHg != null && data.latestDiastolicMmHg != null) {
                unitFormatter.bloodPressure(data.latestSystolicMmHg, data.latestDiastolicMmHg)
            } else {
                null
            },
            icon = Icons.Outlined.Favorite,
            accentColor = VitalsColor,
            noDataMessage = stringResource(R.string.message_no_blood_pressure),
            loadingMessage = loadingMessageFor(DashboardWidgetId.BLOOD_PRESSURE),
            onClick = openMetric(DashboardWidgetId.BLOOD_PRESSURE),
        )
    }
    if (shouldBuild(DashboardWidgetId.SPO2)) {
        addOptionalMetric(
            id = DashboardWidgetId.SPO2,
            title = stringResource(R.string.metric_spo2),
            value = data.latestSpO2Percent?.let(unitFormatter::percent),
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = VitalsColor,
            noDataMessage = stringResource(R.string.message_no_oxygen),
            loadingMessage = loadingMessageFor(DashboardWidgetId.SPO2),
            onClick = openMetric(DashboardWidgetId.SPO2),
        )
    }
    if (shouldBuild(DashboardWidgetId.VO2_MAX)) {
        addOptionalMetric(
            id = DashboardWidgetId.VO2_MAX,
            title = stringResource(R.string.metric_vo2_max),
            value = data.latestVo2Max?.let(unitFormatter::vo2Max),
            icon = Icons.AutoMirrored.Outlined.DirectionsRun,
            accentColor = VitalsColor,
            noDataMessage = stringResource(R.string.message_no_vo2_max),
            loadingMessage = loadingMessageFor(DashboardWidgetId.VO2_MAX),
            onClick = openMetric(DashboardWidgetId.VO2_MAX),
        )
    }
    if (shouldBuild(DashboardWidgetId.RESPIRATORY_RATE)) {
        addOptionalMetric(
            id = DashboardWidgetId.RESPIRATORY_RATE,
            title = stringResource(R.string.metric_respiratory_rate),
            value = data.avgRespiratoryRate?.let(unitFormatter::respiratoryRate),
            icon = Icons.Outlined.Favorite,
            accentColor = VitalsColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.RESPIRATORY_RATE),
            onClick = openMetric(DashboardWidgetId.RESPIRATORY_RATE),
        )
    }
    if (shouldBuild(DashboardWidgetId.BODY_TEMPERATURE)) {
        addOptionalMetric(
            id = DashboardWidgetId.BODY_TEMPERATURE,
            title = stringResource(R.string.metric_body_temp),
            value = data.latestBodyTemperatureCelsius?.let(unitFormatter::temperature),
            icon = Icons.Outlined.FavoriteBorder,
            accentColor = VitalsColor,
            loadingMessage = loadingMessageFor(DashboardWidgetId.BODY_TEMPERATURE),
            onClick = openMetric(DashboardWidgetId.BODY_TEMPERATURE),
        )
    }
    if (shouldBuild(DashboardWidgetId.BLOOD_GLUCOSE)) {
        addOptionalMetric(
            id = DashboardWidgetId.BLOOD_GLUCOSE,
            title = stringResource(R.string.metric_blood_glucose),
            value = data.latestBloodGlucoseMillimolesPerLiter?.let(unitFormatter::bloodGlucose),
            icon = Icons.Outlined.Favorite,
            accentColor = VitalsColor,
            noDataMessage = stringResource(R.string.message_no_blood_glucose),
            loadingMessage = loadingMessageFor(DashboardWidgetId.BLOOD_GLUCOSE),
            onClick = openMetric(DashboardWidgetId.BLOOD_GLUCOSE),
        )
    }
    if (shouldBuild(DashboardWidgetId.SKIN_TEMPERATURE)) {
        addOptionalMetric(
            id = DashboardWidgetId.SKIN_TEMPERATURE,
            title = stringResource(R.string.metric_skin_temperature),
            value = data.latestSkinTemperatureDeltaCelsius?.let(unitFormatter::temperatureDelta),
            icon = Icons.Outlined.DeviceThermostat,
            accentColor = VitalsColor,
            noDataMessage = stringResource(R.string.message_no_skin_temperature),
            loadingMessage = loadingMessageFor(DashboardWidgetId.SKIN_TEMPERATURE),
            onClick = openMetric(DashboardWidgetId.SKIN_TEMPERATURE),
        )
    }
    if (shouldBuild(DashboardWidgetId.MINDFULNESS)) {
        addMetric(
            id = DashboardWidgetId.MINDFULNESS,
            title = stringResource(R.string.metric_mindfulness),
            value = unitFormatter.minutes((data.mindfulnessMinutes ?: 0).toLong()),
            icon = Icons.Outlined.SelfImprovement,
            accentColor = MindfulnessColor,
            progress = dashboardGoalProgress(
                current = (data.mindfulnessMinutes ?: 0).toDouble(),
                target = dailyGoals.mindfulnessMinutes,
                label = stringResource(R.string.dashboard_goal_of, dashboardDisplayValue(unitFormatter.minutes(dailyGoals.mindfulnessMinutes.roundToInt().toLong()))),
            ),
            loadingMessage = loadingMessageFor(DashboardWidgetId.MINDFULNESS),
            onClick = openMetric(DashboardWidgetId.MINDFULNESS),
        )
    }
    if (shouldBuild(DashboardWidgetId.CYCLE)) {
        add(
            DashboardWidgetSpec(DashboardWidgetId.CYCLE, stringResource(R.string.metric_cycle)) { modifier ->
                val cycleValue = cycleDisplayValue(data, unitFormatter)
                DashboardPillWidget(
                    title = stringResource(R.string.metric_cycle),
                    value = cycleValue ?: DisplayValue("", ""),
                    icon = Icons.Outlined.CalendarMonth,
                    accentColor = CycleColor,
                    message = loadingMessageFor(DashboardWidgetId.CYCLE)
                        ?: if (cycleValue == null) stringResource(R.string.message_cycle_browse) else null,
                    modifier = modifier,
                    onClick = openMetric(DashboardWidgetId.CYCLE),
                )
            }
        )
    }
}

internal fun MutableList<DashboardWidgetSpec>.addMetric(
    id: DashboardWidgetId,
    title: String,
    value: DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    progress: DashboardWidgetProgress? = null,
    style: DashboardWidgetStyle = DashboardWidgetStyle.PILL,
    loadingMessage: String? = null,
    onClick: (() -> Unit)?,
) {
    add(
        DashboardWidgetSpec(id = id, title = title, style = style) { modifier ->
            if (loadingMessage != null) {
                DashboardPillWidget(
                    title = title,
                    value = DisplayValue("", ""),
                    icon = icon,
                    accentColor = accentColor,
                    message = loadingMessage,
                    modifier = modifier,
                    onClick = onClick,
                )
            } else if (style == DashboardWidgetStyle.CIRCLE && progress != null) {
                DashboardCircleWidget(
                    title = title,
                    value = value,
                    icon = icon,
                    accentColor = accentColor,
                    progress = progress,
                    modifier = modifier,
                    onClick = onClick,
                )
            } else {
                DashboardPillWidget(
                    title = title,
                    value = value,
                    icon = icon,
                    accentColor = accentColor,
                    progress = progress,
                    modifier = modifier,
                    onClick = onClick,
                )
            }
        }
    )
}

internal fun MutableList<DashboardWidgetSpec>.addOptionalMetric(
    id: DashboardWidgetId,
    title: String,
    value: DisplayValue?,
    icon: ImageVector,
    accentColor: Color,
    noDataMessage: String? = null,
    subtitle: String? = null,
    subtitleColor: Color = accentColor,
    showTitle: Boolean = true,
    progress: DashboardWidgetProgress? = null,
    loadingMessage: String? = null,
    onClick: (() -> Unit)?,
) {
    add(
        DashboardWidgetSpec(id, title) { modifier ->
            if (loadingMessage != null) {
                DashboardPillWidget(
                    title = title,
                    value = DisplayValue("", ""),
                    icon = icon,
                    accentColor = accentColor,
                    message = loadingMessage,
                    showTitle = showTitle,
                    modifier = modifier,
                    onClick = onClick,
                )
            } else if (value != null) {
                DashboardPillWidget(
                    title = title,
                    value = value,
                    icon = icon,
                    accentColor = accentColor,
                    progress = progress,
                    subtitle = subtitle,
                    subtitleColor = subtitleColor,
                    showTitle = showTitle,
                    modifier = modifier,
                    onClick = onClick,
                )
            } else {
                DashboardPillWidget(
                    title = title,
                    value = DisplayValue("", ""),
                    icon = icon,
                    accentColor = accentColor,
                    message = noDataMessage ?: stringResource(R.string.no_data),
                    showTitle = showTitle,
                    modifier = modifier,
                    onClick = onClick,
                )
            }
        }
    )
}

internal fun dashboardMeasurementDate(
    time: Instant,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
): String =
    dateTimeFormatterProvider.mediumDate().format(time.atZone(ZoneId.systemDefault()).toLocalDate())

@Composable
internal fun cycleDisplayValue(data: DashboardData, unitFormatter: UnitFormatter): DisplayValue? =
    when {
        data.menstruationPeriodDays != null && data.menstruationPeriodDays > 0 -> {
            DisplayValue(
                value = unitFormatter.count(data.menstruationPeriodDays),
                unit = stringResource(R.string.unit_days),
            )
        }
        data.ovulationTestCount != null && data.ovulationTestCount > 0 -> {
            DisplayValue(
                value = unitFormatter.count(data.ovulationTestCount),
                unit = stringResource(R.string.unit_tests),
            )
        }
        data.latestBasalBodyTemperatureCelsius != null -> unitFormatter.temperature(data.latestBasalBodyTemperatureCelsius)
        else -> null
    }

internal enum class DashboardWidgetStyle {
    PILL,
    CIRCLE,
}

internal data class DashboardWidgetSpec(
    val id: DashboardWidgetId,
    val title: String,
    val style: DashboardWidgetStyle = DashboardWidgetStyle.PILL,
    val content: @Composable (Modifier) -> Unit,
)

internal data class DashboardWidgetProgress(
    val fraction: Float,
    val label: String,
)

internal fun MutableList<DashboardWidgetSpec>.addWeeklyCardioLoadMetric(
    id: DashboardWidgetId,
    title: String,
    weeklyCardioLoad: DashboardWeeklyCardioLoad?,
    icon: ImageVector,
    accentColor: Color,
    style: DashboardWidgetStyle,
    loadingMessage: String? = null,
    onClick: (() -> Unit)?,
) {
    add(
        DashboardWidgetSpec(id = id, title = title, style = style) { modifier ->
            if (loadingMessage != null) {
                DashboardPillWidget(
                    title = title,
                    value = DisplayValue("", ""),
                    icon = icon,
                    accentColor = accentColor,
                    message = loadingMessage,
                    modifier = modifier,
                    onClick = onClick,
                )
            } else if (weeklyCardioLoad == null) {
                DashboardPillWidget(
                    title = title,
                    value = DisplayValue("", ""),
                    icon = icon,
                    accentColor = accentColor,
                    message = stringResource(R.string.no_data),
                    modifier = modifier,
                    onClick = onClick,
                )
            } else {
                val progress = DashboardWidgetProgress(
                    fraction = weeklyCardioLoad.progressFraction,
                    label = stringResource(
                        R.string.dashboard_weekly_cardio_load_progress,
                        weeklyCardioLoad.currentScore,
                        weeklyCardioLoad.targetScore,
                    ),
                )
                if (style == DashboardWidgetStyle.CIRCLE) {
                    DashboardCircleWidget(
                        title = title,
                        value = DisplayValue(
                            value = stringResource(
                                R.string.dashboard_cardio_load_percent_only,
                                weeklyCardioLoad.progressPercent,
                            ),
                            unit = "",
                        ),
                        icon = icon,
                        accentColor = accentColor,
                        progress = progress,
                        modifier = modifier,
                        onClick = onClick,
                    )
                } else {
                    DashboardPillWidget(
                        title = title,
                        value = DisplayValue(
                            value = stringResource(
                                R.string.dashboard_cardio_load_percent,
                                weeklyCardioLoad.progressPercent,
                            ),
                            unit = "",
                        ),
                        icon = icon,
                        accentColor = accentColor,
                        progress = progress,
                        subtitle = weeklyCardioLoad.todayProgressPercent
                            .takeIf { it > 0 }
                            ?.let { todayPercent ->
                                stringResource(R.string.dashboard_cardio_load_today_delta, todayPercent)
                            },
                        modifier = modifier,
                        onClick = onClick,
                    )
                }
            }
        }
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
    val shape = RoundedCornerShape(28.dp)
    val containerColor = accentSurfaceContainerColor(
        accentColor = accentColor,
        amoledAlpha = if (progress != null) 0.12f else 0.09f,
        fallback = if (progress != null) {
            accentColor.copy(alpha = 0.24f)
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
    )
    val iconContainerColor = accentSurfaceContainerColor(
        accentColor = accentColor,
        amoledAlpha = 0.18f,
        fallback = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
    )
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(shape),
        ) {
            progress?.let { goal ->
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(goal.fraction)
                        .background(accentColor.copy(alpha = 0.62f)),
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
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
                        modifier = Modifier.size(22.dp),
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
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
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
    val containerColor = accentSurfaceContainerColor(accentColor, amoledAlpha = 0.09f)
    Card(
        modifier = modifier
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            val trackColor = accentSurfaceContainerColor(
                accentColor = accentColor,
                amoledAlpha = 0.24f,
                fallback = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 16.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(
                    x = (size.width - diameter) / 2f,
                    y = (size.height - diameter) / 2f,
                )
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = trackColor,
                    startAngle = 130f,
                    sweepAngle = 280f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                drawArc(
                    color = accentColor,
                    startAngle = 130f,
                    sweepAngle = 280f * progress.fraction,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.height(4.dp))
                AutoResizeText(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                AutoResizeText(
                    text = value.value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                )
                AutoResizeText(
                    text = progress.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor,
                    maxLines = 1,
                )
            }
        }
    }
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
) {
    val start = workout.startTime.atZone(zone)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        colors = CardDefaults.cardColors(
            containerColor = accentSurfaceContainerColor(WorkoutColor, amoledAlpha = 0.09f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = exerciseTypeIcon(workout.exerciseType),
                    contentDescription = null,
                    tint = WorkoutColor,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.metric_workout),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
