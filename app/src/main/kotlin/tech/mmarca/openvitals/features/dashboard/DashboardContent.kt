package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.DashboardData
import tech.mmarca.openvitals.domain.model.ExerciseData
import tech.mmarca.openvitals.ui.components.DayNavigator
import tech.mmarca.openvitals.ui.components.HealthConnectSyncStatusBanner
import java.time.ZoneId

/**
 * The order the dashboard body actually renders, derived from the saved layout.
 *
 * Outside edit mode, tiles with no data sink below the ones with some — but only
 * within the carousel, because the fixed hero section keeps its geometry. The
 * partition is stable, so both groups keep their saved relative order. In edit
 * mode the true saved order is kept instead: a drag has to land where the card
 * visually is, and showing a partition the grid would immediately re-apply makes
 * that impossible.
 */
internal fun dashboardVisibleWidgetIds(
    dashboardWidgets: List<DashboardWidgetId>,
    specIds: Set<DashboardWidgetId>,
    display: DashboardDisplayState,
    isEditingDashboard: Boolean,
    placedWidgetIds: Set<DashboardWidgetId> = emptySet(),
): List<DashboardWidgetId> {
    val ordered = dashboardWidgets
        .filter { it in specIds }
        // Outside edit mode an unsupported metric has no widget at all, so this
        // only bites while editing: a metric the device cannot serve that the
        // user never deliberately placed goes to the add tray, not the grid.
        // Once they place it from the tray it stays, exactly like any other.
        .filterNot { isEditingDashboard && it in display.unsupportedIds && it !in placedWidgetIds }
    if (isEditingDashboard) return ordered
    val fixed = dashboardWidgetIdsThatFitRows(ordered, DashboardFixedWidgetRows).toSet()
    val rest = ordered.filterNot { it in fixed }
    // A tile with nothing behind it today goes to the back; a tile offering to
    // set a feature UP does not. See [isDemotableEmptyTile].
    val (withData, empty) = rest.partition { id ->
        display.widgets[id]?.isDemotableEmptyTile() != true
    }
    return ordered.filter { it in fixed } + withData + empty
}

/**
 * The edit-mode add tray: everything the grid does NOT show, in layout order.
 * That is where a removed widget waits — and where a metric the device cannot
 * serve is offered, so it can be placed rather than silently vanishing.
 */
internal fun dashboardTrayWidgetIds(
    specIds: List<DashboardWidgetId>,
    visibleIds: List<DashboardWidgetId>,
    isEditingDashboard: Boolean,
): List<DashboardWidgetId> =
    if (isEditingDashboard) specIds.filterNot { it in visibleIds } else emptyList()

/**
 * Today's activities: the workout LIST wins whenever it has entries; the lone
 * [DashboardData.workout] is only the fallback for a day that carries one.
 */
internal fun dashboardActivitiesForDay(data: DashboardData): List<ExerciseData> =
    data.workouts.ifEmpty { data.workout?.let(::listOf).orEmpty() }

private val DashboardScrollTopPadding = 4.dp
private val DashboardScrollBottomPadding = 24.dp
private val DashboardQuickActionsTopPadding = 14.dp

@Composable
internal fun DashboardContent(
    data: DashboardData,
    display: DashboardDisplayState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    canGoForward: Boolean,
    isRefreshing: Boolean,
    syncPaused: Boolean = false,
    dashboardWidgets: List<DashboardWidgetId>,
    isEditingDashboard: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onMoveWidgetToTarget: (DashboardWidgetId, DashboardWidgetId) -> Unit,
    onRemoveWidget: (DashboardWidgetId) -> Unit,
    onAddWidget: (DashboardWidgetId) -> Unit,
    onOpenMetric: (DashboardWidgetId) -> Unit,
    onOpenActivities: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onOpenLog: () -> Unit,
    onStartActivity: () -> Unit,
    onToggleDashboardEdit: () -> Unit,
    /**
     * The widgets the user deliberately placed (the persisted layout). Only
     * these keep a metric the device cannot serve in the edit grid; the rest of
     * those go to the add tray.
     */
    placedWidgetIds: Set<DashboardWidgetId> = emptySet(),
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
        display = display,
        unitFormatter = unitFormatter,
        widgetIds = specWidgetIds,
        isEditingDashboard = isEditingDashboard,
        onOpenMetric = onOpenMetric,
    )
    val specsById = remember(specs) { specs.associateBy { it.id } }
    val visibleIds = remember(dashboardWidgets, placedWidgetIds, specsById, display, isEditingDashboard) {
        dashboardVisibleWidgetIds(
            dashboardWidgets = dashboardWidgets,
            specIds = specsById.keys,
            display = display,
            isEditingDashboard = isEditingDashboard,
            placedWidgetIds = placedWidgetIds,
        )
    }
    val hiddenSpecs = remember(isEditingDashboard, specs, visibleIds) {
        val trayIds = dashboardTrayWidgetIds(
            specIds = specs.map { it.id },
            visibleIds = visibleIds,
            isEditingDashboard = isEditingDashboard,
        ).toSet()
        specs.filter { it.id in trayIds }
    }
    var activityPendingDelete by remember { mutableStateOf<ExerciseData?>(null) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1080.dp),
            contentPadding = PaddingValues(
                top = DashboardScrollTopPadding,
                bottom = DashboardScrollBottomPadding,
            ),
        ) {
            // A reload that kept the old data on screen announces itself with the
            // syncing banner instead of blanking anything. This is the ONLY sync
            // banner the dashboard renders — the screen shell's inline one is
            // switched off so a single load cannot show up twice.
            if (syncPaused || isRefreshing) {
                item {
                    HealthConnectSyncStatusBanner(
                        syncPaused = syncPaused,
                        syncInProgress = isRefreshing && !syncPaused,
                        modifier = Modifier.padding(
                            horizontal = DashboardScreenPadding,
                            vertical = 4.dp,
                        ),
                    )
                }
            }

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

            item {
                DashboardWidgetCarousel(
                    visibleIds = visibleIds,
                    specsById = specsById,
                    isEditingDashboard = isEditingDashboard,
                    onMoveWidgetToTarget = onMoveWidgetToTarget,
                    onRemoveWidget = onRemoveWidget,
                    actionContent = {
                        DashboardQuickActions(
                            isEditingDashboard = isEditingDashboard,
                            onOpenLog = onOpenLog,
                            onStartActivity = onStartActivity,
                            onToggleDashboardEdit = onToggleDashboardEdit,
                            // 14dp under the rings, nothing below: the divider
                            // carries its own 16dp of air.
                            modifier = Modifier.padding(
                                start = DashboardScreenPadding,
                                end = DashboardScreenPadding,
                                top = DashboardQuickActionsTopPadding,
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

            // The sensor-status card is deliberately absent here: the top-bar
            // battery action is the sensors entry point.

            dashboardActivitiesToday(
                workouts = dashboardActivitiesForDay(data),
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
