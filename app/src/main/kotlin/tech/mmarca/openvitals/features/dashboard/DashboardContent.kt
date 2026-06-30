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
import tech.mmarca.openvitals.domain.model.HealthConnectAvailability
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.ContextualPermissionPrompt
import tech.mmarca.openvitals.ui.components.DayNavigator
import java.time.ZoneId

@Composable
internal fun DashboardContent(
    data: DashboardData,
    display: DashboardDisplayState,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    canGoForward: Boolean,
    unacknowledgedWidgetPermissions: Set<String>,
    showHealthConnectPromo: Boolean,
    healthConnectAvailability: HealthConnectAvailability,
    healthConnectSyncEnabled: Boolean,
    dashboardWidgets: List<DashboardWidgetId>,
    isEditingDashboard: Boolean,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onOpenCalendar: () -> Unit,
    onGrantWidgetPermissions: () -> Unit,
    onDismissWidgetPermissions: () -> Unit,
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
        display = display,
        unitFormatter = unitFormatter,
        widgetIds = specWidgetIds,
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
