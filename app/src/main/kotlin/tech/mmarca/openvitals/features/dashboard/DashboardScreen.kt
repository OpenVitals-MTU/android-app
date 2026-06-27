package tech.mmarca.openvitals.features.dashboard

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
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
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.ContextualPermissionPrompt
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher
import androidx.compose.runtime.mutableIntStateOf
import tech.mmarca.openvitals.ui.components.shouldShowDashboardHealthConnectPromo
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import androidx.compose.ui.platform.LocalContext
import tech.mmarca.openvitals.ui.components.PullToRefreshBox
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton
import tech.mmarca.openvitals.ui.theme.WorkoutColor
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

private const val DashboardCarouselEdgeScrollDelayMillis = 450L
private val DashboardCarouselEdgeScrollThreshold = 56.dp
private val DashboardScreenPadding = 14.dp
private val DashboardSectionSeparatorSpacing = 4.dp
private val DashboardQuickActionHeight = 44.dp
private val DashboardActionsSpacing = 10.dp
private val DashboardQuickActionIconSize = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    refreshRequest: Int = 0,
    onOpenMetric: (DashboardWidgetId) -> Unit,
    onOpenActivities: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit = {},
    onOpenLog: () -> Unit,
    onStartActivity: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val dashboardData = state.data
    val context = LocalContext.current
    var showDatePicker by remember { mutableStateOf(false) }
    val showPromo = shouldShowDashboardHealthConnectPromo(
        availability = state.healthConnectAvailability,
        syncEnabled = state.healthConnectSyncEnabled,
        minimumPermissionsGranted = state.minimumPermissionsGranted,
    )
    var permissionReloadKey by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberHealthConnectPermissionLauncher(
        onResult = {
            permissionReloadKey++
            viewModel.refresh()
        },
    )

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentDay()
    }
    LaunchedEffect(refreshRequest) {
        if (refreshRequest > 0) {
            viewModel.refresh()
        }
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.DASHBOARD,
        isLoading = state.isLoading && dashboardData != null,
        refreshKey = refreshRequest to permissionReloadKey,
    ) {
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
                    unacknowledgedWidgetPermissions = state.unacknowledgedWidgetPermissions,
                    showHealthConnectPromo = showPromo,
                    healthConnectAvailability = state.healthConnectAvailability,
                    healthConnectSyncEnabled = state.healthConnectSyncEnabled,
                    dashboardWidgets = state.dashboardWidgets,
                    pendingWidgets = state.pendingWidgets,
                    visibleWidgetLoadToken = state.visibleWidgetLoadToken,
                    dailyGoals = state.dailyGoals,
                    isEditingDashboard = state.isEditingDashboard,
                    onPreviousDay = viewModel::previousDay,
                    onNextDay = viewModel::nextDay,
                    onOpenCalendar = { showDatePicker = true },
                    onGrantWidgetPermissions = {
                        permissionLauncher.launch(state.unacknowledgedWidgetPermissions)
                    },
                    onDismissWidgetPermissions = viewModel::acknowledgeWidgetMissingPermissions,
                    onMoveWidgetToTarget = viewModel::moveDashboardWidgetToTarget,
                    onRemoveWidget = viewModel::removeDashboardWidget,
                    onAddWidget = viewModel::addDashboardWidget,
                    onVisibleWidgetsChanged = viewModel::loadVisibleDashboardWidgets,
                    onOpenMetric = onOpenMetric,
                    onOpenActivities = onOpenActivities,
                    onOpenActivity = onOpenActivity,
                    onEditActivity = onEditActivity,
                    onDeleteActivity = viewModel::deleteActivityEntry,
                    onOpenLog = onOpenLog,
                    onStartActivity = onStartActivity,
                    onToggleDashboardEdit = viewModel::toggleDashboardEdit,
                    onHealthConnectPromoAction = {
                        permissionLauncher.launch(viewModel.minimumOnboardingPermissions)
                    },
                )
                else -> ErrorMessage(stringResource(R.string.message_no_dashboard_data))
            }
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
    unacknowledgedWidgetPermissions: Set<String>,
    showHealthConnectPromo: Boolean,
    healthConnectAvailability: HealthConnectAvailability,
    healthConnectSyncEnabled: Boolean,
    dashboardWidgets: List<DashboardWidgetId>,
    pendingWidgets: Set<DashboardWidgetId>,
    visibleWidgetLoadToken: Long,
    dailyGoals: DashboardDailyGoals,
    isEditingDashboard: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onGrantWidgetPermissions: () -> Unit,
    onDismissWidgetPermissions: () -> Unit,
    onMoveWidgetToTarget: (DashboardWidgetId, DashboardWidgetId) -> Unit,
    onRemoveWidget: (DashboardWidgetId) -> Unit,
    onAddWidget: (DashboardWidgetId) -> Unit,
    onVisibleWidgetsChanged: (Set<DashboardWidgetId>) -> Unit,
    onOpenMetric: (DashboardWidgetId) -> Unit,
    onOpenActivities: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onOpenLog: () -> Unit,
    onStartActivity: () -> Unit,
    onToggleDashboardEdit: () -> Unit,
    onHealthConnectPromoAction: () -> Unit,
) {
    val zone = ZoneId.systemDefault()
    val specWidgetIds = remember(dashboardWidgets, isEditingDashboard) {
        if (isEditingDashboard) {
            DashboardWidgetId.entries.toList()
        } else {
            dashboardWidgets
        }
    }
    val specs = dashboardWidgetSpecs(
        data = data,
        unitFormatter = unitFormatter,
        dateTimeFormatterProvider = dateTimeFormatterProvider,
        dailyGoals = dailyGoals,
        widgetIds = specWidgetIds,
        pendingWidgets = pendingWidgets,
        isEditingDashboard = isEditingDashboard,
        onOpenMetric = onOpenMetric,
    )
    val specsById = remember(specs) { specs.associateBy { it.id } }
    val visibleIds = remember(dashboardWidgets, specsById) { dashboardWidgets.filter { it in specsById } }
    val hiddenSpecs = remember(isEditingDashboard, specs, visibleIds) {
        if (isEditingDashboard) {
            specs.filter { it.id !in visibleIds }
        } else {
            emptyList()
        }
    }
    val widgetBounds = remember { mutableStateMapOf<DashboardWidgetId, Rect>() }
    var draggingWidgetId by remember { mutableStateOf<DashboardWidgetId?>(null) }
    var activityPendingDelete by remember { mutableStateOf<ExerciseData?>(null) }

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
            contentPadding = PaddingValues(
                top = 4.dp,
                bottom = 12.dp,
            ),
        ) {
            item {
                DayNavigator(
                    date = data.date,
                    canGoForward = canGoForward,
                    onPreviousDay = onPreviousDay,
                    onNextDay = onNextDay,
                    onOpenCalendar = onOpenCalendar,
                    modifier = Modifier.padding(
                        horizontal = DashboardScreenPadding,
                        vertical = 4.dp,
                    ),
                )
            }

            if (showHealthConnectPromo) {
                item {
                    DashboardHealthConnectPromoCard(
                        availability = healthConnectAvailability,
                        syncEnabled = healthConnectSyncEnabled,
                        onPrimaryAction = onHealthConnectPromoAction,
                        modifier = Modifier.padding(
                            horizontal = DashboardScreenPadding,
                            vertical = 4.dp,
                        ),
                    )
                }
            }

            if (unacknowledgedWidgetPermissions.isNotEmpty()) {
                item {
                    ContextualPermissionPrompt(
                        feature = HealthConnectFeature.DASHBOARD,
                        onGrant = onGrantWidgetPermissions,
                        onDismiss = onDismissWidgetPermissions,
                        modifier = Modifier.padding(
                            horizontal = DashboardScreenPadding,
                            vertical = 4.dp,
                        ),
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
                    visibleWidgetLoadToken = visibleWidgetLoadToken,
                    onVisibleWidgetsChanged = onVisibleWidgetsChanged,
                    actionContent = {
                        DashboardQuickActions(
                            isEditingDashboard = isEditingDashboard,
                            onOpenLog = onOpenLog,
                            onStartActivity = onStartActivity,
                            onToggleDashboardEdit = onToggleDashboardEdit,
                            modifier = Modifier.padding(
                                horizontal = DashboardScreenPadding,
                                vertical = DashboardSectionSeparatorSpacing,
                            ),
                        )
                    },
                    hiddenContent = {
                        if (isEditingDashboard) {
                            DashboardHiddenWidgets(
                                hiddenSpecs = hiddenSpecs,
                                onAddWidget = onAddWidget,
                            )
                        }
                    },
                )
            }

            dashboardActivitiesToday(
                workouts = data.workouts.ifEmpty { data.workout?.let(::listOf).orEmpty() },
                zone = zone,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                onOpenActivities = onOpenActivities,
                onOpenActivity = onOpenActivity,
                onEditActivity = onEditActivity,
                onRequestDeleteActivity = { workout -> activityPendingDelete = workout },
            )

            item { Spacer(Modifier.height(10.dp)) }
        }

        activityPendingDelete?.let { workout ->
            DeleteActivityConfirmationDialog(
                workout = workout,
                onDismiss = { activityPendingDelete = null },
                onConfirm = {
                    activityPendingDelete = null
                    onDeleteActivity(workout.id)
                },
            )
        }
    }
}

@Composable
private fun DashboardQuickActions(
    isEditingDashboard: Boolean,
    onOpenLog: () -> Unit,
    onStartActivity: () -> Unit,
    onToggleDashboardEdit: () -> Unit,
    modifier: Modifier = Modifier,
    ) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(DashboardActionsSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OpenVitalsTonalButton(
            onClick = onOpenLog,
            modifier = Modifier
                .weight(1f)
                .height(DashboardQuickActionHeight),
        ) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.dashboard_action_log))
        }
        OpenVitalsButton(
            onClick = onStartActivity,
            modifier = Modifier
                .weight(1f)
                .height(DashboardQuickActionHeight),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.DirectionsRun,
                contentDescription = null,
                modifier = Modifier.size(DashboardQuickActionIconSize),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.action_start))
        }
        OpenVitalsIconButton(
            onClick = onToggleDashboardEdit,
            modifier = Modifier
                .size(44.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = stringResource(
                    if (isEditingDashboard) {
                        R.string.cd_finish_dashboard_editing
                    } else {
                        R.string.cd_edit_dashboard
                    }
                ),
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private fun LazyListScope.dashboardActivitiesToday(
    workouts: List<ExerciseData>,
    zone: ZoneId,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onOpenActivities: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onRequestDeleteActivity: (ExerciseData) -> Unit,
) {
    item {
        DashboardActivitiesSectionHeader(onClick = onOpenActivities)
    }
    if (workouts.isNotEmpty()) {
        items(
            count = workouts.size,
            key = { index -> workouts[index].id.ifBlank { "workout_$index" } },
        ) { index ->
            val workout = workouts[index]
            val editable = workout.isOpenVitalsEntry && workout.id.isNotBlank()
            val openAction = workout.id.takeIf { it.isNotBlank() }?.let { activityId ->
                { onOpenActivity(activityId) }
            }
            val editAction = if (editable) {
                { onEditActivity(workout.id) }
            } else {
                null
            }
            val cardContent: @Composable (Modifier) -> Unit = { cardModifier ->
                WorkoutCard(
                    workout = workout,
                    zone = zone,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    modifier = cardModifier,
                    onClick = openAction,
                    onEdit = editAction,
                )
            }
            if (editable) {
                DashboardSwipeToDeleteActivityCard(
                    onDelete = { onRequestDeleteActivity(workout) },
                    modifier = Modifier.padding(
                        horizontal = DashboardScreenPadding,
                        vertical = 6.dp,
                    ),
                ) {
                    cardContent(Modifier)
                }
            } else {
                cardContent(
                    Modifier.padding(
                        horizontal = DashboardScreenPadding,
                        vertical = 6.dp,
                    )
                )
            }
        }
    } else {
        item {
            MetricCardPlaceholder(
                title = stringResource(R.string.section_activities),
                icon = Icons.AutoMirrored.Outlined.DirectionsRun,
                accentColor = WorkoutColor,
                message = stringResource(R.string.message_no_workouts_day),
                modifier = Modifier.padding(
                    horizontal = DashboardScreenPadding,
                    vertical = 6.dp,
                ),
                showHeader = false,
                onClick = onOpenActivities,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardSwipeToDeleteActivityCard(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val scope = rememberCoroutineScope()
    val dismissState = rememberSwipeToDismissBoxState()
    val shape = MaterialTheme.shapes.medium

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        onDismiss = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                currentOnDelete()
            }
            scope.launch {
                dismissState.reset()
            }
        },
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.errorContainer, shape)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = stringResource(R.string.cd_delete_entry),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        modifier = modifier.clip(shape),
        content = { content() },
    )
}

@Composable
private fun DeleteActivityConfirmationDialog(
    workout: ExerciseData,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.dashboard_delete_activity_title)) },
        text = {
            Text(
                stringResource(
                    R.string.dashboard_delete_activity_message,
                    exerciseTypeLabel(workout.exerciseType),
                )
            )
        },
        confirmButton = {
            OpenVitalsTextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete))
            }
        },
        dismissButton = {
            OpenVitalsTextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Composable
private fun DashboardActivitiesSectionHeader(
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(
                horizontal = DashboardScreenPadding,
                vertical = 6.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.dashboard_activities_today),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
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
    visibleWidgetLoadToken: Long,
    onVisibleWidgetsChanged: (Set<DashboardWidgetId>) -> Unit,
    actionContent: @Composable () -> Unit,
    hiddenContent: @Composable () -> Unit,
) {
    val fixedIds = dashboardWidgetIdsThatFitRows(
        widgetIds = visibleIds,
        rows = DashboardFixedWidgetRows,
    )
    val fixedIdSet = fixedIds.toSet()
    val carouselIds = visibleIds.filterNot { it in fixedIdSet }
    val carouselPages = dashboardWidgetIdsInGridPages(
        widgetIds = carouselIds,
        rows = DashboardCarouselWidgetRows,
    )
    val pagerState = rememberPagerState(pageCount = { carouselPages.size.coerceAtLeast(1) })
    var sectionBounds by remember { mutableStateOf<Rect?>(null) }
    var fixedSectionBounds by remember { mutableStateOf<Rect?>(null) }
    var carouselSectionBounds by remember { mutableStateOf<Rect?>(null) }
    val dragOffsetState = remember { mutableStateOf(Offset.Zero) }
    var draggedWidgetStartBounds by remember { mutableStateOf<Rect?>(null) }
    val onVisibleWidgetsChangedState = rememberUpdatedState(onVisibleWidgetsChanged)
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

    LaunchedEffect(visibleWidgetLoadToken, fixedIds, carouselPages, pagerState) {
        fun visibleWidgetsFor(page: Int): Set<DashboardWidgetId> =
            (
                fixedIds +
                    carouselPages.getOrNull(page).orEmpty()
            ).toSet()

        onVisibleWidgetsChangedState.value(visibleWidgetsFor(pagerState.currentPage))
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                onVisibleWidgetsChangedState.value(visibleWidgetsFor(page))
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
            DashboardWidgetGrid(
                ids = fixedIds,
                rows = DashboardFixedWidgetRows,
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

            actionContent()

            if (carouselPages.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        horizontal = DashboardScreenPadding,
                        vertical = DashboardSectionSeparatorSpacing,
                    ),
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
                    DashboardWidgetGrid(
                        ids = pageIds,
                        rows = DashboardCarouselWidgetRows,
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
                            .padding(
                                top = 6.dp,
                                bottom = 6.dp,
                            ),
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

            hiddenContent()
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
