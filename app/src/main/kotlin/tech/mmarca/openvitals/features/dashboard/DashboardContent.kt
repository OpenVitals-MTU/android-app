package tech.mmarca.openvitals.features.dashboard

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import java.time.LocalDate
import java.time.ZoneId

/**
 * The order the dashboard renders. Outside edit mode, carousel tiles with
 * no data sink below the ones with some, in a stable partition. Edit mode
 * keeps the saved order, so a drag lands where the card is.
 */
internal fun dashboardVisibleWidgetIds(
    dashboardWidgets: List<DashboardWidgetId>,
    specIds: Set<DashboardWidgetId>,
    display: DashboardDisplayState,
    isEditingDashboard: Boolean,
    placedWidgetIds: Set<DashboardWidgetId> = emptySet(),
    sortEmptyTilesLast: Boolean = true,
): List<DashboardWidgetId> {
    val ordered = dashboardWidgets
        .filter { it in specIds }
        // While editing, an unsupported metric the user never placed goes to the tray.
        .filterNot { isEditingDashboard && it in display.unsupportedIds && it !in placedWidgetIds }
    if (isEditingDashboard || !sortEmptyTilesLast) return ordered
    // A tile still loading is not empty. Hold the saved order until every tile has answered.
    if (display.widgets.values.any { it.isLoading }) return ordered
    val fixed = dashboardWidgetIdsThatFitRows(ordered, DashboardFixedWidgetRows).toSet()
    val rest = ordered.filterNot { it in fixed }
    // An empty tile goes to the back; a setup offer does not. See [isDemotableEmptyTile].
    val (withData, empty) = rest.partition { id ->
        display.widgets[id]?.isDemotableEmptyTile() != true
    }
    return ordered.filter { it in fixed } + withData + empty
}

/** The edit-mode add tray: everything the grid does not show, in layout order. */
internal fun dashboardTrayWidgetIds(
    specIds: List<DashboardWidgetId>,
    visibleIds: List<DashboardWidgetId>,
    isEditingDashboard: Boolean,
): List<DashboardWidgetId> =
    if (isEditingDashboard) specIds.filterNot { it in visibleIds } else emptyList()

/** Today's activities: the workout list wins; the lone workout is the fallback. */
internal fun dashboardActivitiesForDay(data: DashboardData): List<ExerciseData> =
    data.workouts.ifEmpty { data.workout?.let(::listOf).orEmpty() }

private val DashboardScrollTopPadding = 4.dp
private val DashboardScrollBottomPadding = 24.dp
private val DashboardQuickActionsTopPadding = 14.dp

@Composable
internal fun DashboardContent(
    data: DashboardData,
    /** The day the user asked for, ahead of [data] while a switch loads. */
    selectedDate: LocalDate = data.date,
    display: DashboardDisplayState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    canGoForward: Boolean,
    isRefreshing: Boolean,
    syncPaused: Boolean = false,
    dashboardWidgets: List<DashboardWidgetId>,
    isEditingDashboard: Boolean,
    sortEmptyTilesLast: Boolean = true,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onMoveWidgetToTarget: (DashboardWidgetId, DashboardWidgetId) -> Unit,
    onRemoveWidget: (DashboardWidgetId) -> Unit,
    onAddWidget: (DashboardWidgetId) -> Unit,
    onOpenMetric: (DashboardWidgetId) -> Unit,
    onSyncWatch: () -> Unit = {},
    onOpenActivities: () -> Unit,
    onOpenActivity: (String) -> Unit,
    onEditActivity: (String) -> Unit,
    onDeleteActivity: (String) -> Unit,
    onOpenLog: () -> Unit,
    onStartActivity: () -> Unit,
    onToggleDashboardEdit: () -> Unit,
    /** The widgets the user placed. Only these keep an unsupported metric in the edit grid. */
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
    // While a switch loads, the tiles read "loading" rather than the old day's numbers.
    // Ordering keeps the old day's partition until the new day lands.
    val awaitingSelectedDay = data.date != selectedDate
    val widgetDisplay = remember(display, awaitingSelectedDay) {
        if (awaitingSelectedDay) {
            display.copy(
                widgets = display.widgets.mapValues { (_, model) -> model.copy(isLoading = true) },
            )
        } else {
            display
        }
    }
    val specs = dashboardWidgetSpecs(
        display = widgetDisplay,
        unitFormatter = unitFormatter,
        widgetIds = specWidgetIds,
        isEditingDashboard = isEditingDashboard,
        onOpenMetric = onOpenMetric,
        onSyncWatch = onSyncWatch,
    )
    val specsById = remember(specs) { specs.associateBy { it.id } }
    val visibleIds = remember(
        dashboardWidgets,
        placedWidgetIds,
        specsById,
        display,
        isEditingDashboard,
        sortEmptyTilesLast,
    ) {
        dashboardVisibleWidgetIds(
            dashboardWidgets = dashboardWidgets,
            specIds = specsById.keys,
            display = display,
            isEditingDashboard = isEditingDashboard,
            placedWidgetIds = placedWidgetIds,
            sortEmptyTilesLast = sortEmptyTilesLast,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 1080.dp),
        ) {
            if (syncPaused) {
                HealthConnectSyncStatusBanner(
                    syncPaused = syncPaused,
                    syncInProgress = false,
                    modifier = Modifier.padding(
                        start = DashboardScreenPadding,
                        end = DashboardScreenPadding,
                        top = DashboardScrollTopPadding,
                    ),
                )
            }

            androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    top = DashboardScrollTopPadding,
                    bottom = DashboardScrollBottomPadding,
                ),
            ) {
                // Explicit keys, or a change above re-creates every sibling below.
                item(key = "day_navigator") {
                    DayNavigator(
                        date = selectedDate,
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

                item(key = "widgets") {
                    // Reveal animations play once per day on display.
                    key(data.date) {
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
                                    // 14dp under the rings; the divider carries its own air.
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
                }

                // The sensor-status card is absent: the top-bar battery action is the entry point.

                dashboardActivitiesToday(
                    // The old day's workouts must not sit under the new day's date.
                    workouts = if (awaitingSelectedDay) emptyList() else dashboardActivitiesForDay(data),
                    isLoading = awaitingSelectedDay,
                    zone = zone,
                    unitFormatter = unitFormatter,
                    dateTimeFormatterProvider = dateTimeFormatterProvider,
                    onOpenActivities = onOpenActivities,
                    onOpenActivity = onOpenActivity,
                    onEditActivity = onEditActivity,
                    onRequestDeleteActivity = { workout -> activityPendingDelete = workout },
                )

                item(key = "bottom_spacer") { Spacer(Modifier.height(10.dp)) }
            }
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
