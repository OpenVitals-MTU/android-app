package tech.mmarca.openvitals.features.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Stairs
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Terrain
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.data.model.DashboardData
import tech.mmarca.openvitals.data.model.ExerciseData
import tech.mmarca.openvitals.data.model.SleepData
import tech.mmarca.openvitals.features.activity.exerciseTypeLabel
import tech.mmarca.openvitals.ui.components.DayNavigator
import tech.mmarca.openvitals.ui.components.ErrorMessage
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.HealthDatePickerDialog
import tech.mmarca.openvitals.ui.components.MetricCard
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
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

private const val DashboardGridColumns = 2
private const val DashboardCarouselRows = 3
private const val DashboardCarouselPageSize = DashboardGridColumns * DashboardCarouselRows
private const val DashboardDragLongPressMillis = 500L
private const val DashboardEditWiggleDegrees = 0.45f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onGrantPermissions: () -> Unit,
    onOpenSteps: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenHeart: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenHydration: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenMindfulness: () -> Unit,
    onOpenCycle: () -> Unit,
    onOpenBrowse: () -> Unit,
    onEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsState()
    val dashboardData = state.data
    var showDatePicker by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshPreferences()
    }

    LaunchedEffect(state.isEditingDashboard) {
        onEditStateChanged(state.isEditingDashboard, viewModel::toggleDashboardEdit)
    }
    DisposableEffect(Unit) {
        onDispose { onEditStateChanged(false) {} }
    }

    PullToRefreshBox(
        isRefreshing = state.isLoading && dashboardData != null,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            state.isLoading && dashboardData == null -> FullScreenLoading()
            state.errorMessage != null && dashboardData == null ->
                ErrorMessage(state.errorMessage ?: stringResource(R.string.unknown_error))
            dashboardData != null -> DashboardContent(
                data = dashboardData,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                canGoForward = state.selectedDate.isBefore(LocalDate.now()),
                showPermissionsCallout = state.showPermissionsCallout,
                trackCycle = state.trackCycle,
                dashboardWidgets = state.dashboardWidgets,
                isEditingDashboard = state.isEditingDashboard,
                onPreviousDay = viewModel::previousDay,
                onNextDay = viewModel::nextDay,
                onOpenCalendar = { showDatePicker = true },
                onGrantPermissions = {
                    viewModel.acknowledgePermissionsCallout()
                    onGrantPermissions()
                },
                onDismissPermissionsCallout = viewModel::acknowledgePermissionsCallout,
                onMoveWidgetToTarget = viewModel::moveDashboardWidgetToTarget,
                onRemoveWidget = viewModel::removeDashboardWidget,
                onAddWidget = viewModel::addDashboardWidget,
                onOpenSteps = onOpenSteps,
                onOpenActivities = onOpenActivities,
                onOpenSleep = onOpenSleep,
                onOpenHeart = onOpenHeart,
                onOpenBody = onOpenBody,
                onOpenHydration = onOpenHydration,
                onOpenNutrition = onOpenNutrition,
                onOpenMindfulness = onOpenMindfulness,
                onOpenCycle = onOpenCycle,
                onOpenBrowse = onOpenBrowse,
            )
            else -> ErrorMessage(stringResource(R.string.message_no_dashboard_data))
        }
    }

    if (showDatePicker) {
        HealthDatePickerDialog(
            selectedDate = state.selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { date ->
                showDatePicker = false
                viewModel.selectDate(date)
            },
        )
    }
}

@Composable
private fun DashboardContent(
    data: DashboardData,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    canGoForward: Boolean,
    showPermissionsCallout: Boolean,
    trackCycle: Boolean,
    dashboardWidgets: List<DashboardWidgetId>,
    isEditingDashboard: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onGrantPermissions: () -> Unit,
    onDismissPermissionsCallout: () -> Unit,
    onMoveWidgetToTarget: (DashboardWidgetId, DashboardWidgetId) -> Unit,
    onRemoveWidget: (DashboardWidgetId) -> Unit,
    onAddWidget: (DashboardWidgetId) -> Unit,
    onOpenSteps: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenHeart: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenHydration: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenMindfulness: () -> Unit,
    onOpenCycle: () -> Unit,
    onOpenBrowse: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val specs = dashboardWidgetSpecs(
        data = data,
        zone = zone,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        trackCycle = trackCycle,
        isEditingDashboard = isEditingDashboard,
        onOpenSteps = onOpenSteps,
        onOpenActivities = onOpenActivities,
        onOpenSleep = onOpenSleep,
        onOpenHeart = onOpenHeart,
        onOpenBody = onOpenBody,
        onOpenHydration = onOpenHydration,
        onOpenNutrition = onOpenNutrition,
        onOpenMindfulness = onOpenMindfulness,
        onOpenCycle = onOpenCycle,
    )
    val specsById = specs.associateBy { it.id }
    val visibleIds = dashboardWidgets.filter { it in specsById }
    val hiddenSpecs = specs.filter { it.id !in visibleIds }
    val widgetBounds = remember { mutableStateMapOf<DashboardWidgetId, Rect>() }
    var draggingWidgetId by remember { mutableStateOf<DashboardWidgetId?>(null) }

    LaunchedEffect(visibleIds) {
        val visibleSet = visibleIds.toSet()
        widgetBounds.keys.toList().forEach { widgetId ->
            if (widgetId !in visibleSet) {
                widgetBounds.remove(widgetId)
            }
        }
        if (draggingWidgetId !in visibleSet) {
            draggingWidgetId = null
        }
    }

    LaunchedEffect(isEditingDashboard) {
        if (!isEditingDashboard) {
            draggingWidgetId = null
        }
    }

    androidx.compose.foundation.lazy.LazyColumn(
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            DayNavigator(
                date = data.date,
                canGoForward = canGoForward,
                onPreviousDay = onPreviousDay,
                onNextDay = onNextDay,
                onOpenCalendar = onOpenCalendar,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (showPermissionsCallout) {
            item {
                PermissionCallout(
                    title = stringResource(R.string.message_missing_permissions_title),
                    body = stringResource(R.string.message_missing_permissions_body),
                    onGrant = onGrantPermissions,
                    onDismiss = onDismissPermissionsCallout,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
        }

        item {
            DashboardWidgetSections(
                visibleIds = visibleIds,
                specsById = specsById,
                isEditingDashboard = isEditingDashboard,
                draggingWidgetId = draggingWidgetId,
                widgetBounds = widgetBounds,
                onDraggingWidgetChanged = { widgetId -> draggingWidgetId = widgetId },
                onMoveWidgetToTarget = onMoveWidgetToTarget,
                onRemoveWidget = onRemoveWidget,
            )
        }

        if (isEditingDashboard) {
            hiddenDashboardWidgets(
                hiddenSpecs = hiddenSpecs,
                onAddWidget = onAddWidget,
            )
        }

        browseDashboardItem(
            onOpenBrowse = onOpenBrowse,
            isEditingDashboard = isEditingDashboard,
        )

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardWidgetSections(
    visibleIds: List<DashboardWidgetId>,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    isEditingDashboard: Boolean,
    draggingWidgetId: DashboardWidgetId?,
    widgetBounds: MutableMap<DashboardWidgetId, Rect>,
    onDraggingWidgetChanged: (DashboardWidgetId?) -> Unit,
    onMoveWidgetToTarget: (DashboardWidgetId, DashboardWidgetId) -> Unit,
    onRemoveWidget: (DashboardWidgetId) -> Unit,
) {
    val fixedIds = visibleIds.take(DashboardFixedWidgetCount)
    val carouselIds = visibleIds.drop(DashboardFixedWidgetCount)
    val carouselPages = carouselIds.chunked(DashboardCarouselPageSize)
    val pagerState = rememberPagerState(pageCount = { carouselPages.size.coerceAtLeast(1) })
    val currentCarouselIds = carouselPages.getOrNull(pagerState.currentPage).orEmpty()
    var sectionBounds by remember { mutableStateOf<Rect?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(draggingWidgetId) {
        if (draggingWidgetId == null) {
            dragOffset = Offset.Zero
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates -> sectionBounds = coordinates.boundsInRoot() },
    ) {
        Column {
            DashboardWidgetGridRows(
                ids = fixedIds,
                specsById = specsById,
                dropTargetIds = fixedIds + currentCarouselIds,
                isEditingDashboard = isEditingDashboard,
                draggingWidgetId = draggingWidgetId,
                widgetBounds = widgetBounds,
                onDraggingWidgetChanged = onDraggingWidgetChanged,
                onDragOffsetChanged = { offset -> dragOffset = offset },
                onMoveWidgetToTarget = onMoveWidgetToTarget,
                onRemoveWidget = onRemoveWidget,
                modifier = Modifier.zIndex(if (draggingWidgetId in fixedIds) 2f else 0f),
            )

            if (carouselPages.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .zIndex(if (draggingWidgetId in carouselIds) 2f else 0f),
                    pageSpacing = 12.dp,
                ) { page ->
                    val pageIds = carouselPages.getOrNull(page).orEmpty()
                    DashboardWidgetGridRows(
                        ids = pageIds,
                        specsById = specsById,
                        dropTargetIds = fixedIds + pageIds,
                        isEditingDashboard = isEditingDashboard,
                        draggingWidgetId = draggingWidgetId,
                        widgetBounds = widgetBounds,
                        onDraggingWidgetChanged = onDraggingWidgetChanged,
                        onDragOffsetChanged = { offset -> dragOffset = offset },
                        onMoveWidgetToTarget = onMoveWidgetToTarget,
                        onRemoveWidget = onRemoveWidget,
                    )
                }

                if (carouselPages.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        carouselPages.forEachIndexed { page, _ ->
                            val color = if (page == pagerState.currentPage) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                            }
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(6.dp)
                                    .background(color, CircleShape)
                            )
                        }
                    }
                }
            }
        }

        DashboardDraggedWidgetOverlay(
            draggingWidgetId = draggingWidgetId,
            specsById = specsById,
            widgetBounds = widgetBounds,
            sectionBounds = sectionBounds,
            dragOffset = dragOffset,
        )
    }
}

@Composable
private fun DashboardWidgetGridRows(
    ids: List<DashboardWidgetId>,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    dropTargetIds: List<DashboardWidgetId>,
    isEditingDashboard: Boolean,
    draggingWidgetId: DashboardWidgetId?,
    widgetBounds: MutableMap<DashboardWidgetId, Rect>,
    onDraggingWidgetChanged: (DashboardWidgetId?) -> Unit,
    onDragOffsetChanged: (Offset) -> Unit,
    onMoveWidgetToTarget: (DashboardWidgetId, DashboardWidgetId) -> Unit,
    onRemoveWidget: (DashboardWidgetId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val specs = ids.mapNotNull { specsById[it] }
    Column(modifier = modifier) {
        specs.chunked(DashboardGridColumns).forEach { rowSpecs ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .zIndex(if (rowSpecs.any { it.id == draggingWidgetId }) 1f else 0f)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(IntrinsicSize.Min)
                    .animateContentSize(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rowSpecs.forEach { spec ->
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
                                targetIds = dropTargetIds,
                                widgetBounds = widgetBounds,
                            )?.let { targetId ->
                                onMoveWidgetToTarget(spec.id, targetId)
                            }
                        },
                        onRemove = { onRemoveWidget(spec.id) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                if (rowSpecs.size == 1) {
                    Spacer(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

private fun LazyListScope.hiddenDashboardWidgets(
    hiddenSpecs: List<DashboardWidgetSpec>,
    onAddWidget: (DashboardWidgetId) -> Unit,
) {
    item { SectionHeader(stringResource(R.string.dashboard_add_widgets)) }

    if (hiddenSpecs.isEmpty()) {
        item {
            Text(
                text = stringResource(R.string.dashboard_all_widgets_added),
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

private fun LazyListScope.browseDashboardItem(
    onOpenBrowse: () -> Unit,
    isEditingDashboard: Boolean,
) {
    item(key = "fixed_browse") {
        MetricCardPlaceholder(
            title = stringResource(R.string.metric_browse),
            icon = Icons.Outlined.FolderOpen,
            accentColor = MaterialTheme.colorScheme.primary,
            message = stringResource(R.string.message_browse_records),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            onClick = if (isEditingDashboard) null else onOpenBrowse,
        )
    }
}

@Composable
private fun DashboardWidgetTile(
    spec: DashboardWidgetSpec,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    isEditingDashboard: Boolean,
    onPositioned: (Rect) -> Unit,
    onDraggingChanged: (Boolean) -> Unit,
    onDragOffsetChanged: (Offset) -> Unit,
    onDrop: (Offset) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragOffset by remember(spec.id, isEditingDashboard) { mutableStateOf(Offset.Zero) }
    var isDragging by remember(spec.id, isEditingDashboard) { mutableStateOf(false) }
    val wiggleTransition = rememberInfiniteTransition(label = "DashboardWidgetWiggle")
    val wiggleRotation by wiggleTransition.animateFloat(
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

@Composable
private fun DashboardDraggedWidgetOverlay(
    draggingWidgetId: DashboardWidgetId?,
    specsById: Map<DashboardWidgetId, DashboardWidgetSpec>,
    widgetBounds: Map<DashboardWidgetId, Rect>,
    sectionBounds: Rect?,
    dragOffset: Offset,
) {
    val widgetId = draggingWidgetId ?: return
    val spec = specsById[widgetId] ?: return
    val bounds = widgetBounds[widgetId] ?: return
    val section = sectionBounds ?: return
    val density = LocalDensity.current

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

private fun closestDashboardWidgetId(
    draggedId: DashboardWidgetId,
    dragOffset: Offset,
    targetIds: List<DashboardWidgetId>,
    widgetBounds: Map<DashboardWidgetId, Rect>,
): DashboardWidgetId? {
    val draggedBounds = widgetBounds[draggedId] ?: return null
    val dropCenter = draggedBounds.center + dragOffset

    return targetIds
        .filter { it in widgetBounds }
        .minByOrNull { widgetId ->
            val center = widgetBounds.getValue(widgetId).center
            val delta = dropCenter - center
            delta.x * delta.x + delta.y * delta.y
        }
}

@Composable
private fun dashboardWidgetSpecs(
    data: DashboardData,
    zone: ZoneId,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    trackCycle: Boolean,
    isEditingDashboard: Boolean,
    onOpenSteps: () -> Unit,
    onOpenActivities: () -> Unit,
    onOpenSleep: () -> Unit,
    onOpenHeart: () -> Unit,
    onOpenBody: () -> Unit,
    onOpenHydration: () -> Unit,
    onOpenNutrition: () -> Unit,
    onOpenMindfulness: () -> Unit,
    onOpenCycle: () -> Unit,
): List<DashboardWidgetSpec> = buildList {
    val openSteps = onOpenSteps.takeUnless { isEditingDashboard }
    val openActivities = onOpenActivities.takeUnless { isEditingDashboard }
    val openSleep = onOpenSleep.takeUnless { isEditingDashboard }
    val openHeart = onOpenHeart.takeUnless { isEditingDashboard }
    val openBody = onOpenBody.takeUnless { isEditingDashboard }
    val openHydration = onOpenHydration.takeUnless { isEditingDashboard }
    val openNutrition = onOpenNutrition.takeUnless { isEditingDashboard }
    val openMindfulness = onOpenMindfulness.takeUnless { isEditingDashboard }
    val openCycle = onOpenCycle.takeUnless { isEditingDashboard }

    addMetric(
        id = DashboardWidgetId.STEPS,
        title = stringResource(R.string.metric_steps),
        value = DisplayValue(unitFormatter.count(data.steps), stringResource(R.string.unit_steps)),
        icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
        accentColor = StepsColor,
        onClick = openSteps,
    )
    addMetric(
        id = DashboardWidgetId.DISTANCE,
        title = stringResource(R.string.metric_distance),
        value = unitFormatter.distance(data.distanceMeters),
        icon = Icons.Outlined.Straighten,
        accentColor = DistanceColor,
        onClick = openSteps,
    )
    addMetric(
        id = DashboardWidgetId.CALORIES_OUT,
        title = stringResource(R.string.metric_calories_out),
        value = unitFormatter.energy(data.caloriesKcal),
        icon = Icons.Outlined.LocalFireDepartment,
        accentColor = CaloriesColor,
        onClick = openSteps,
    )
    addOptionalMetric(
        id = DashboardWidgetId.ACTIVE_CALORIES,
        title = stringResource(R.string.metric_active_calories),
        value = data.activeCaloriesKcal?.let(unitFormatter::energy),
        icon = Icons.Outlined.LocalFireDepartment,
        accentColor = ActiveCaloriesColor,
        onClick = openSteps,
    )
    addOptionalMetric(
        id = DashboardWidgetId.FLOORS,
        title = stringResource(R.string.metric_floors_climbed),
        value = data.floorsClimbed?.let {
            DisplayValue(unitFormatter.count(it), stringResource(R.string.unit_floors))
        },
        icon = Icons.Outlined.Stairs,
        accentColor = FloorsColor,
        onClick = openSteps,
    )
    addOptionalMetric(
        id = DashboardWidgetId.ELEVATION,
        title = stringResource(R.string.metric_elevation),
        value = data.elevationGainedMeters?.let(unitFormatter::elevation),
        icon = Icons.Outlined.Terrain,
        accentColor = ElevationColor,
        onClick = openSteps,
    )
    add(
        DashboardWidgetSpec(DashboardWidgetId.WORKOUT, stringResource(R.string.metric_workout)) { modifier ->
            data.workout?.let { workout ->
                WorkoutCard(
                    workout = workout,
                    zone = zone,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = modifier,
                    onClick = openActivities,
                )
            } ?: MetricCardPlaceholder(
                title = stringResource(R.string.metric_workout),
                icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                accentColor = WorkoutColor,
                message = stringResource(R.string.message_no_workouts_day),
                modifier = modifier,
                contentAtBottom = true,
                onClick = openActivities,
            )
        }
    )
    add(
        DashboardWidgetSpec(DashboardWidgetId.SLEEP, stringResource(R.string.metric_sleep)) { modifier ->
            data.sleep?.let { sleep ->
                SleepCard(
                    sleep = sleep,
                    unitFormatter = unitFormatter,
                    modifier = modifier,
                    onClick = openSleep,
                )
            } ?: MetricCardPlaceholder(
                title = stringResource(R.string.metric_sleep),
                icon = Icons.Outlined.Bed,
                accentColor = SleepColor,
                message = stringResource(R.string.message_no_sleep_day),
                modifier = modifier,
                contentAtBottom = true,
                onClick = openSleep,
            )
        }
    )
    addMetric(
        id = DashboardWidgetId.HYDRATION,
        title = stringResource(R.string.metric_hydration),
        value = unitFormatter.hydration(data.hydrationLiters),
        icon = Icons.Outlined.LocalDrink,
        accentColor = HydrationColor,
        onClick = openHydration,
    )
    addOptionalMetric(
        id = DashboardWidgetId.CALORIES_IN,
        title = stringResource(R.string.metric_calories_in),
        value = data.caloriesInKcal?.let(unitFormatter::energy),
        icon = Icons.Outlined.Restaurant,
        accentColor = NutritionColor,
        onClick = openNutrition,
    )
    addOptionalMetric(
        id = DashboardWidgetId.PROTEIN,
        title = stringResource(R.string.metric_protein),
        value = data.proteinGrams?.let { DisplayValue(unitFormatter.count(it.roundToInt()), stringResource(R.string.unit_grams)) },
        icon = Icons.Outlined.Restaurant,
        accentColor = NutritionColor,
        onClick = openNutrition,
    )
    addOptionalMetric(
        id = DashboardWidgetId.CARBS,
        title = stringResource(R.string.metric_carbs),
        value = data.carbsGrams?.let { DisplayValue(unitFormatter.count(it.roundToInt()), stringResource(R.string.unit_grams)) },
        icon = Icons.Outlined.Restaurant,
        accentColor = NutritionColor,
        onClick = openNutrition,
    )
    addOptionalMetric(
        id = DashboardWidgetId.FAT,
        title = stringResource(R.string.metric_fat),
        value = data.fatGrams?.let { DisplayValue(unitFormatter.count(it.roundToInt()), stringResource(R.string.unit_grams)) },
        icon = Icons.Outlined.Restaurant,
        accentColor = NutritionColor,
        onClick = openNutrition,
    )
    addMetric(
        id = DashboardWidgetId.WEIGHT,
        title = stringResource(R.string.metric_latest_weight),
        value = unitFormatter.weight(data.weightKg),
        icon = Icons.Outlined.MonitorWeight,
        accentColor = WeightColor,
        onClick = openBody,
    )
    addOptionalMetric(
        id = DashboardWidgetId.HEIGHT,
        title = stringResource(R.string.metric_height),
        value = data.heightCm?.let(unitFormatter::height),
        icon = Icons.Outlined.Straighten,
        accentColor = WeightColor,
        onClick = openBody,
    )
    addOptionalMetric(
        id = DashboardWidgetId.BMI,
        title = stringResource(R.string.metric_bmi),
        value = data.bmi?.let { DisplayValue(unitFormatter.decimal(it, 1), "") },
        icon = Icons.Outlined.MonitorWeight,
        accentColor = WeightColor,
        onClick = openBody,
    )
    addMetric(
        id = DashboardWidgetId.BODY_FAT,
        title = stringResource(R.string.metric_body_fat),
        value = unitFormatter.percent(data.bodyFatPercent),
        icon = Icons.Outlined.MonitorWeight,
        accentColor = BodyFatColor,
        onClick = openBody,
    )
    addOptionalMetric(
        id = DashboardWidgetId.LEAN_MASS,
        title = stringResource(R.string.metric_lean_mass),
        value = data.leanMassKg?.let(unitFormatter::bodyMass),
        icon = Icons.Outlined.MonitorWeight,
        accentColor = WeightColor,
        onClick = openBody,
    )
    addOptionalMetric(
        id = DashboardWidgetId.BMR,
        title = stringResource(R.string.metric_bmr),
        value = data.bmrKcal?.let(unitFormatter::energy),
        icon = Icons.Outlined.LocalFireDepartment,
        accentColor = CaloriesColor,
        onClick = openBody,
    )
    addOptionalMetric(
        id = DashboardWidgetId.BONE_MASS,
        title = stringResource(R.string.metric_bone_mass),
        value = data.boneMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
        icon = Icons.Outlined.MonitorWeight,
        accentColor = WeightColor,
        onClick = openBody,
    )
    addMetric(
        id = DashboardWidgetId.AVG_HEART_RATE,
        title = stringResource(R.string.metric_avg_heart_rate),
        value = unitFormatter.heartRate(data.avgHeartRateBpm),
        icon = Icons.Outlined.Favorite,
        accentColor = HeartColor,
        onClick = openHeart,
    )
    addMetric(
        id = DashboardWidgetId.RESTING_HEART_RATE,
        title = stringResource(R.string.metric_resting_heart_rate),
        value = unitFormatter.heartRate(data.restingHeartRateBpm),
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
        onClick = openHeart,
    )
    addOptionalMetric(
        id = DashboardWidgetId.HRV,
        title = stringResource(R.string.metric_hrv),
        value = data.hrvRmssdMs?.let(unitFormatter::hrv),
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
        onClick = openHeart,
    )
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
        onClick = openHeart,
    )
    addOptionalMetric(
        id = DashboardWidgetId.SPO2,
        title = stringResource(R.string.metric_spo2),
        value = data.latestSpO2Percent?.let(unitFormatter::percent),
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = VitalsColor,
        noDataMessage = stringResource(R.string.message_no_oxygen),
        onClick = openHeart,
    )
    addOptionalMetric(
        id = DashboardWidgetId.VO2_MAX,
        title = stringResource(R.string.metric_vo2_max),
        value = data.latestVo2Max?.let(unitFormatter::vo2Max),
        icon = Icons.AutoMirrored.Outlined.DirectionsRun,
        accentColor = VitalsColor,
        noDataMessage = stringResource(R.string.message_no_vo2_max),
        onClick = openHeart,
    )
    addOptionalMetric(
        id = DashboardWidgetId.RESPIRATORY_RATE,
        title = stringResource(R.string.metric_respiratory_rate),
        value = data.avgRespiratoryRate?.let(unitFormatter::respiratoryRate),
        icon = Icons.Outlined.Favorite,
        accentColor = VitalsColor,
        onClick = openHeart,
    )
    addOptionalMetric(
        id = DashboardWidgetId.BODY_TEMPERATURE,
        title = stringResource(R.string.metric_body_temp),
        value = data.latestBodyTemperatureCelsius?.let(unitFormatter::temperature),
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = VitalsColor,
        onClick = openHeart,
    )
    addMetric(
        id = DashboardWidgetId.MINDFULNESS,
        title = stringResource(R.string.metric_mindfulness),
        value = unitFormatter.minutes((data.mindfulnessMinutes ?: 0).toLong()),
        icon = Icons.Outlined.SelfImprovement,
        accentColor = MindfulnessColor,
        onClick = openMindfulness,
    )
    if (trackCycle) {
        add(
            DashboardWidgetSpec(DashboardWidgetId.CYCLE, stringResource(R.string.metric_cycle)) { modifier ->
                val cycleValue = cycleDisplayValue(data, unitFormatter)
                if (cycleValue != null) {
                    MetricCard(
                        title = stringResource(R.string.metric_cycle),
                        value = cycleValue.value,
                        unit = cycleValue.unit,
                        icon = Icons.Outlined.CalendarMonth,
                        accentColor = CycleColor,
                        modifier = modifier,
                        contentAtBottom = true,
                        onClick = openCycle,
                    )
                } else {
                    MetricCardPlaceholder(
                        title = stringResource(R.string.metric_cycle),
                        icon = Icons.Outlined.CalendarMonth,
                        accentColor = CycleColor,
                        message = stringResource(R.string.message_cycle_browse),
                        modifier = modifier,
                        contentAtBottom = true,
                        onClick = openCycle,
                    )
                }
            }
        )
    }
}

private fun MutableList<DashboardWidgetSpec>.addMetric(
    id: DashboardWidgetId,
    title: String,
    value: DisplayValue,
    icon: ImageVector,
    accentColor: Color,
    onClick: (() -> Unit)?,
) {
    add(
        DashboardWidgetSpec(id, title) { modifier ->
            MetricCard(
                title = title,
                value = value.value,
                unit = value.unit,
                icon = icon,
                accentColor = accentColor,
                modifier = modifier,
                contentAtBottom = true,
                onClick = onClick,
            )
        }
    )
}

private fun MutableList<DashboardWidgetSpec>.addOptionalMetric(
    id: DashboardWidgetId,
    title: String,
    value: DisplayValue?,
    icon: ImageVector,
    accentColor: Color,
    noDataMessage: String? = null,
    onClick: (() -> Unit)?,
) {
    add(
        DashboardWidgetSpec(id, title) { modifier ->
            if (value != null) {
                MetricCard(
                    title = title,
                    value = value.value,
                    unit = value.unit,
                    icon = icon,
                    accentColor = accentColor,
                    modifier = modifier,
                    contentAtBottom = true,
                    onClick = onClick,
                )
            } else {
                MetricCardPlaceholder(
                    title = title,
                    icon = icon,
                    accentColor = accentColor,
                    message = noDataMessage ?: stringResource(R.string.no_data),
                    modifier = modifier,
                    contentAtBottom = true,
                    onClick = onClick,
                )
            }
        }
    )
}

@Composable
private fun cycleDisplayValue(data: DashboardData, unitFormatter: UnitFormatter): DisplayValue? =
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

private data class DashboardWidgetSpec(
    val id: DashboardWidgetId,
    val title: String,
    val content: @Composable (Modifier) -> Unit,
)

@Composable
private fun WorkoutCard(
    workout: ExerciseData,
    zone: ZoneId,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val start = workout.startTime.atZone(zone)
    MetricCard(
        title = stringResource(R.string.metric_workout),
        value = unitFormatter.duration(workout.durationMs),
        unit = exerciseTypeLabel(workout.exerciseType),
        icon = Icons.AutoMirrored.Outlined.DirectionsRun,
        accentColor = WorkoutColor,
        subtitle = "${dateTimeFormatterProvider.mediumDate().format(start)} · ${dateTimeFormatterProvider.shortTime().format(start)}",
        source = workout.source,
        modifier = modifier,
        onClick = onClick,
    )
}

@Composable
private fun SleepCard(
    sleep: SleepData,
    unitFormatter: UnitFormatter,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    MetricCard(
        title = stringResource(R.string.metric_sleep),
        value = unitFormatter.duration(sleep.durationMs),
        unit = "",
        icon = Icons.Outlined.Bed,
        accentColor = SleepColor,
        modifier = modifier,
        contentAtBottom = true,
        onClick = onClick,
    )
}
