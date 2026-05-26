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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import kotlinx.coroutines.delay
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
private const val DashboardCarouselEdgeScrollDelayMillis = 450L
private val DashboardCarouselEdgeScrollThreshold = 56.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onGrantPermissions: () -> Unit,
    onOpenMetric: (DashboardWidgetId) -> Unit,
    onOpenBrowse: () -> Unit,
    onEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
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
                onOpenMetric = onOpenMetric,
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
    onOpenMetric: (DashboardWidgetId) -> Unit,
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
        onOpenMetric = onOpenMetric,
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

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1080.dp),
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
    var sectionBounds by remember { mutableStateOf<Rect?>(null) }
    var fixedSectionBounds by remember { mutableStateOf<Rect?>(null) }
    var carouselSectionBounds by remember { mutableStateOf<Rect?>(null) }
    val dragOffsetState = remember { mutableStateOf(Offset.Zero) }
    var draggedWidgetStartBounds by remember { mutableStateOf<Rect?>(null) }
    val density = LocalDensity.current
    val edgeScrollThresholdPx = with(density) { DashboardCarouselEdgeScrollThreshold.toPx() }
    val currentDropTargetIds: (DashboardWidgetId, Offset) -> List<DashboardWidgetId> = { draggedId, droppedOffset ->
        val currentPageIds = carouselPages.getOrNull(pagerState.currentPage).orEmpty()
        val draggedBounds = draggedWidgetStartBounds ?: widgetBounds[draggedId]
        val dropCenter = draggedBounds?.let { it.center + droppedOffset }
        val isOverFixedSection = dropCenter?.let { fixedSectionBounds?.containsPoint(it) } == true
        val isOverCarouselSection = dropCenter?.let { carouselSectionBounds?.containsPoint(it) } == true

        when {
            draggedId in fixedIds && isOverCarouselSection -> currentPageIds
            draggedId in fixedIds -> fixedIds
            isOverFixedSection -> fixedIds
            else -> currentPageIds
        }
    }

    LaunchedEffect(draggingWidgetId) {
        if (draggingWidgetId == null) {
            dragOffsetState.value = Offset.Zero
            draggedWidgetStartBounds = null
        }
    }

    LaunchedEffect(draggingWidgetId, carouselPages.size, sectionBounds, edgeScrollThresholdPx) {
        if (draggingWidgetId == null || carouselPages.size <= 1 || sectionBounds == null) {
            return@LaunchedEffect
        }

        val section = sectionBounds ?: return@LaunchedEffect
        val widgetId = draggingWidgetId ?: return@LaunchedEffect
        while (true) {
            val draggedBounds = draggedWidgetStartBounds ?: widgetBounds[widgetId]
            if (widgetId in carouselIds && draggedBounds != null && !pagerState.isScrollInProgress) {
                val draggedCenterX = draggedBounds.center.x + dragOffsetState.value.x
                val currentPage = pagerState.currentPage
                val targetPage = when {
                    draggedCenterX <= section.left + edgeScrollThresholdPx -> currentPage - 1
                    draggedCenterX >= section.right - edgeScrollThresholdPx -> currentPage + 1
                    else -> currentPage
                }.coerceIn(0, carouselPages.lastIndex)

                if (targetPage != currentPage) {
                    pagerState.animateScrollToPage(targetPage)
                }
            }
            delay(DashboardCarouselEdgeScrollDelayMillis)
        }
    }

    val onGridDraggingWidgetChanged: (DashboardWidgetId?) -> Unit = { widgetId ->
        draggedWidgetStartBounds = widgetId?.let { widgetBounds[it] }
        onDraggingWidgetChanged(widgetId)
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
                dropTargetIdsProvider = currentDropTargetIds,
                isEditingDashboard = isEditingDashboard,
                draggingWidgetId = draggingWidgetId,
                draggedWidgetStartBounds = draggedWidgetStartBounds,
                widgetBounds = widgetBounds,
                onDraggingWidgetChanged = onGridDraggingWidgetChanged,
                onDragOffsetChanged = { offset -> dragOffsetState.value = offset },
                onMoveWidgetToTarget = onMoveWidgetToTarget,
                onRemoveWidget = onRemoveWidget,
                modifier = Modifier
                    .onGloballyPositioned { coordinates -> fixedSectionBounds = coordinates.boundsInRoot() }
                    .zIndex(if (draggingWidgetId in fixedIds) 2f else 0f),
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
                        .onGloballyPositioned { coordinates -> carouselSectionBounds = coordinates.boundsInRoot() }
                        .zIndex(if (draggingWidgetId in carouselIds) 2f else 0f),
                    pageSpacing = 12.dp,
                    beyondViewportPageCount = 1.coerceAtMost(carouselPages.lastIndex),
                ) { page ->
                    val pageIds = carouselPages.getOrNull(page).orEmpty()
                    DashboardWidgetGridRows(
                        ids = pageIds,
                        specsById = specsById,
                        dropTargetIdsProvider = currentDropTargetIds,
                        isEditingDashboard = isEditingDashboard,
                        draggingWidgetId = draggingWidgetId,
                        draggedWidgetStartBounds = draggedWidgetStartBounds,
                        widgetBounds = widgetBounds,
                        onDraggingWidgetChanged = onGridDraggingWidgetChanged,
                        onDragOffsetChanged = { offset -> dragOffsetState.value = offset },
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
            draggedWidgetStartBounds = draggedWidgetStartBounds,
            sectionBounds = sectionBounds,
            dragOffsetState = dragOffsetState,
        )
    }
}

@Composable
private fun DashboardWidgetGridRows(
    ids: List<DashboardWidgetId>,
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
                    val visibleIndex = ids.indexOf(spec.id)
                    val previousId = ids.getOrNull(visibleIndex - 1)
                    val nextId = ids.getOrNull(visibleIndex + 1)
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
private fun DashboardDraggedWidgetOverlay(
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

private fun closestDashboardWidgetId(
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

private fun Rect.containsPoint(point: Offset): Boolean =
    point.x >= left && point.x <= right && point.y >= top && point.y <= bottom

@Composable
private fun dashboardWidgetSpecs(
    data: DashboardData,
    zone: ZoneId,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    trackCycle: Boolean,
    isEditingDashboard: Boolean,
    onOpenMetric: (DashboardWidgetId) -> Unit,
): List<DashboardWidgetSpec> = buildList {
    val openMetric: (DashboardWidgetId) -> (() -> Unit)? = { widgetId ->
        if (isEditingDashboard) null else ({ onOpenMetric(widgetId) })
    }

    addMetric(
        id = DashboardWidgetId.STEPS,
        title = stringResource(R.string.metric_steps),
        value = DisplayValue(unitFormatter.count(data.steps), stringResource(R.string.unit_steps)),
        icon = Icons.AutoMirrored.Outlined.DirectionsWalk,
        accentColor = StepsColor,
        onClick = openMetric(DashboardWidgetId.STEPS),
    )
    addMetric(
        id = DashboardWidgetId.DISTANCE,
        title = stringResource(R.string.metric_distance),
        value = unitFormatter.distance(data.distanceMeters),
        icon = Icons.Outlined.Straighten,
        accentColor = DistanceColor,
        onClick = openMetric(DashboardWidgetId.DISTANCE),
    )
    addMetric(
        id = DashboardWidgetId.CALORIES_OUT,
        title = stringResource(R.string.metric_calories_out),
        value = unitFormatter.energy(data.caloriesKcal),
        icon = Icons.Outlined.LocalFireDepartment,
        accentColor = CaloriesColor,
        onClick = openMetric(DashboardWidgetId.CALORIES_OUT),
    )
    addOptionalMetric(
        id = DashboardWidgetId.ACTIVE_CALORIES,
        title = stringResource(R.string.metric_active_calories),
        value = data.activeCaloriesKcal?.let(unitFormatter::energy),
        icon = Icons.Outlined.LocalFireDepartment,
        accentColor = ActiveCaloriesColor,
        onClick = openMetric(DashboardWidgetId.ACTIVE_CALORIES),
    )
    addOptionalMetric(
        id = DashboardWidgetId.FLOORS,
        title = stringResource(R.string.metric_floors_climbed),
        value = data.floorsClimbed?.let {
            DisplayValue(unitFormatter.count(it), stringResource(R.string.unit_floors))
        },
        icon = Icons.Outlined.Stairs,
        accentColor = FloorsColor,
        onClick = openMetric(DashboardWidgetId.FLOORS),
    )
    addOptionalMetric(
        id = DashboardWidgetId.ELEVATION,
        title = stringResource(R.string.metric_elevation),
        value = data.elevationGainedMeters?.let(unitFormatter::elevation),
        icon = Icons.Outlined.Terrain,
        accentColor = ElevationColor,
        onClick = openMetric(DashboardWidgetId.ELEVATION),
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
                    onClick = openMetric(DashboardWidgetId.WORKOUT),
                )
            } ?: MetricCardPlaceholder(
                title = stringResource(R.string.metric_workout),
                icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                accentColor = WorkoutColor,
                message = stringResource(R.string.message_no_workouts_day),
                modifier = modifier,
                contentAtBottom = true,
                onClick = openMetric(DashboardWidgetId.WORKOUT),
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
                    onClick = openMetric(DashboardWidgetId.SLEEP),
                )
            } ?: MetricCardPlaceholder(
                title = stringResource(R.string.metric_sleep),
                icon = Icons.Outlined.Bed,
                accentColor = SleepColor,
                message = stringResource(R.string.message_no_sleep_day),
                modifier = modifier,
                contentAtBottom = true,
                onClick = openMetric(DashboardWidgetId.SLEEP),
            )
        }
    )
    addMetric(
        id = DashboardWidgetId.HYDRATION,
        title = stringResource(R.string.metric_hydration),
        value = unitFormatter.hydration(data.hydrationLiters),
        icon = Icons.Outlined.LocalDrink,
        accentColor = HydrationColor,
        onClick = openMetric(DashboardWidgetId.HYDRATION),
    )
    addOptionalMetric(
        id = DashboardWidgetId.CALORIES_IN,
        title = stringResource(R.string.metric_calories_in),
        value = data.caloriesInKcal?.let(unitFormatter::energy),
        icon = Icons.Outlined.Restaurant,
        accentColor = NutritionColor,
        onClick = openMetric(DashboardWidgetId.CALORIES_IN),
    )
    addOptionalMetric(
        id = DashboardWidgetId.PROTEIN,
        title = stringResource(R.string.metric_protein),
        value = data.proteinGrams?.let { DisplayValue(unitFormatter.count(it.roundToInt()), stringResource(R.string.unit_grams)) },
        icon = Icons.Outlined.Restaurant,
        accentColor = NutritionColor,
        onClick = openMetric(DashboardWidgetId.PROTEIN),
    )
    addOptionalMetric(
        id = DashboardWidgetId.CARBS,
        title = stringResource(R.string.metric_carbs),
        value = data.carbsGrams?.let { DisplayValue(unitFormatter.count(it.roundToInt()), stringResource(R.string.unit_grams)) },
        icon = Icons.Outlined.Restaurant,
        accentColor = NutritionColor,
        onClick = openMetric(DashboardWidgetId.CARBS),
    )
    addOptionalMetric(
        id = DashboardWidgetId.FAT,
        title = stringResource(R.string.metric_fat),
        value = data.fatGrams?.let { DisplayValue(unitFormatter.count(it.roundToInt()), stringResource(R.string.unit_grams)) },
        icon = Icons.Outlined.Restaurant,
        accentColor = NutritionColor,
        onClick = openMetric(DashboardWidgetId.FAT),
    )
    addMetric(
        id = DashboardWidgetId.WEIGHT,
        title = stringResource(R.string.metric_latest_weight),
        value = unitFormatter.weight(data.weightKg),
        icon = Icons.Outlined.MonitorWeight,
        accentColor = WeightColor,
        onClick = openMetric(DashboardWidgetId.WEIGHT),
    )
    addOptionalMetric(
        id = DashboardWidgetId.HEIGHT,
        title = stringResource(R.string.metric_height),
        value = data.heightCm?.let(unitFormatter::height),
        icon = Icons.Outlined.Straighten,
        accentColor = WeightColor,
        onClick = openMetric(DashboardWidgetId.HEIGHT),
    )
    addOptionalMetric(
        id = DashboardWidgetId.BMI,
        title = stringResource(R.string.metric_bmi),
        value = data.bmi?.let { DisplayValue(unitFormatter.decimal(it, 1), "") },
        icon = Icons.Outlined.MonitorWeight,
        accentColor = WeightColor,
        onClick = openMetric(DashboardWidgetId.BMI),
    )
    addMetric(
        id = DashboardWidgetId.BODY_FAT,
        title = stringResource(R.string.metric_body_fat),
        value = unitFormatter.percent(data.bodyFatPercent),
        icon = Icons.Outlined.MonitorWeight,
        accentColor = BodyFatColor,
        onClick = openMetric(DashboardWidgetId.BODY_FAT),
    )
    addOptionalMetric(
        id = DashboardWidgetId.LEAN_MASS,
        title = stringResource(R.string.metric_lean_mass),
        value = data.leanMassKg?.let(unitFormatter::bodyMass),
        icon = Icons.Outlined.MonitorWeight,
        accentColor = WeightColor,
        onClick = openMetric(DashboardWidgetId.LEAN_MASS),
    )
    addOptionalMetric(
        id = DashboardWidgetId.BMR,
        title = stringResource(R.string.metric_bmr),
        value = data.bmrKcal?.let(unitFormatter::energy),
        icon = Icons.Outlined.LocalFireDepartment,
        accentColor = CaloriesColor,
        onClick = openMetric(DashboardWidgetId.BMR),
    )
    addOptionalMetric(
        id = DashboardWidgetId.BONE_MASS,
        title = stringResource(R.string.metric_bone_mass),
        value = data.boneMassKg?.let { unitFormatter.bodyMass(it, decimals = 2) },
        icon = Icons.Outlined.MonitorWeight,
        accentColor = WeightColor,
        onClick = openMetric(DashboardWidgetId.BONE_MASS),
    )
    addMetric(
        id = DashboardWidgetId.AVG_HEART_RATE,
        title = stringResource(R.string.metric_avg_heart_rate),
        value = unitFormatter.heartRate(data.avgHeartRateBpm),
        icon = Icons.Outlined.Favorite,
        accentColor = HeartColor,
        onClick = openMetric(DashboardWidgetId.AVG_HEART_RATE),
    )
    addMetric(
        id = DashboardWidgetId.RESTING_HEART_RATE,
        title = stringResource(R.string.metric_resting_heart_rate),
        value = unitFormatter.heartRate(data.restingHeartRateBpm),
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
        onClick = openMetric(DashboardWidgetId.RESTING_HEART_RATE),
    )
    addOptionalMetric(
        id = DashboardWidgetId.HRV,
        title = stringResource(R.string.metric_hrv),
        value = data.hrvRmssdMs?.let(unitFormatter::hrv),
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = HeartColor,
        onClick = openMetric(DashboardWidgetId.HRV),
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
        onClick = openMetric(DashboardWidgetId.BLOOD_PRESSURE),
    )
    addOptionalMetric(
        id = DashboardWidgetId.SPO2,
        title = stringResource(R.string.metric_spo2),
        value = data.latestSpO2Percent?.let(unitFormatter::percent),
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = VitalsColor,
        noDataMessage = stringResource(R.string.message_no_oxygen),
        onClick = openMetric(DashboardWidgetId.SPO2),
    )
    addOptionalMetric(
        id = DashboardWidgetId.VO2_MAX,
        title = stringResource(R.string.metric_vo2_max),
        value = data.latestVo2Max?.let(unitFormatter::vo2Max),
        icon = Icons.AutoMirrored.Outlined.DirectionsRun,
        accentColor = VitalsColor,
        noDataMessage = stringResource(R.string.message_no_vo2_max),
        onClick = openMetric(DashboardWidgetId.VO2_MAX),
    )
    addOptionalMetric(
        id = DashboardWidgetId.RESPIRATORY_RATE,
        title = stringResource(R.string.metric_respiratory_rate),
        value = data.avgRespiratoryRate?.let(unitFormatter::respiratoryRate),
        icon = Icons.Outlined.Favorite,
        accentColor = VitalsColor,
        onClick = openMetric(DashboardWidgetId.RESPIRATORY_RATE),
    )
    addOptionalMetric(
        id = DashboardWidgetId.BODY_TEMPERATURE,
        title = stringResource(R.string.metric_body_temp),
        value = data.latestBodyTemperatureCelsius?.let(unitFormatter::temperature),
        icon = Icons.Outlined.FavoriteBorder,
        accentColor = VitalsColor,
        onClick = openMetric(DashboardWidgetId.BODY_TEMPERATURE),
    )
    addMetric(
        id = DashboardWidgetId.MINDFULNESS,
        title = stringResource(R.string.metric_mindfulness),
        value = unitFormatter.minutes((data.mindfulnessMinutes ?: 0).toLong()),
        icon = Icons.Outlined.SelfImprovement,
        accentColor = MindfulnessColor,
        onClick = openMetric(DashboardWidgetId.MINDFULNESS),
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
                        onClick = openMetric(DashboardWidgetId.CYCLE),
                    )
                } else {
                    MetricCardPlaceholder(
                        title = stringResource(R.string.metric_cycle),
                        icon = Icons.Outlined.CalendarMonth,
                        accentColor = CycleColor,
                        message = stringResource(R.string.message_cycle_browse),
                        modifier = modifier,
                        contentAtBottom = true,
                        onClick = openMetric(DashboardWidgetId.CYCLE),
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(onClick?.let { Modifier.clickable(onClick = it) } ?: Modifier),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
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
                    imageVector = Icons.AutoMirrored.Outlined.DirectionsRun,
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
