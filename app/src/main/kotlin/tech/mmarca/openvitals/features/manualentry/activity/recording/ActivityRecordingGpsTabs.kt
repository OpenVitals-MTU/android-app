package tech.mmarca.openvitals.features.manualentry.activity.recording

import tech.mmarca.openvitals.features.manualentry.*
import tech.mmarca.openvitals.features.manualentry.activity.*
import tech.mmarca.openvitals.features.manualentry.activity.recording.*
import tech.mmarca.openvitals.features.manualentry.activity.routeimport.*
import tech.mmarca.openvitals.features.manualentry.body.*
import tech.mmarca.openvitals.features.manualentry.hydration.*
import tech.mmarca.openvitals.features.manualentry.mindfulness.*
import tech.mmarca.openvitals.features.manualentry.vitals.*



import android.content.ClipData
import android.graphics.Color as AndroidColor
import android.content.ClipDescription
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.location.Location
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropTransferData
import androidx.compose.ui.draganddrop.mimeTypes
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DisplayValue
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.model.ActivityRecordingMarker
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.CoMapsNavigationState
import tech.mmarca.openvitals.domain.model.CoMapsRoutePolyline
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.activity.maps.OfflineRouteMapOrPreview
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardField
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardItemSize
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardTemplate
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
import tech.mmarca.openvitals.domain.preferences.UnitQuantity
import tech.mmarca.openvitals.domain.preferences.UnitSystem
import tech.mmarca.openvitals.domain.preferences.isDarkTheme
import tech.mmarca.openvitals.ui.components.AutoResizeText
import tech.mmarca.openvitals.ui.theme.ActivityRecordingTheme
import tech.mmarca.openvitals.ui.theme.activityRecordingAccentColor
import tech.mmarca.openvitals.ui.theme.recordingOutdoorAccentForAppTheme
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.OpenVitalsSurface

@Composable
internal fun GpsRecordingTabs(
    state: ActivityRecordingState,
    preStartPoint: ExerciseRoutePoint?,
    totalTime: Duration,
    movingTime: Duration,
    now: Instant,
    unitFormatter: UnitFormatter,
    isEditingDashboard: Boolean,
    onUpdateDashboardLayout: (ActivityRecordingDashboardLayout) -> Unit,
    modifier: Modifier = Modifier,
    coMapsNavigation: CoMapsNavigationState = CoMapsNavigationState.Disabled,
    coMapsRoute: CoMapsRoutePolyline? = null,
    onRequestCoMapsPermission: () -> Unit = {},
    onPlanInCoMaps: (() -> Unit)? = null,
    coMapsGuidanceDismissed: Boolean = false,
    onDismissCoMapsGuidance: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableStateOf(ActivityRecordingTab.STATS) }
    var timeSplitMinutes by rememberSaveable { mutableIntStateOf(DefaultTimeSplitMinutes) }
    val unitSystem = unitFormatter.unitSystem(UnitQuantity.DISTANCE)
    var distanceSplitMeters by rememberSaveable(unitSystem) {
        mutableDoubleStateOf(defaultDistanceSplitMeters(unitSystem))
    }
    val activeTab = if (isEditingDashboard) ActivityRecordingTab.STATS else selectedTab

    if (isEditingDashboard) {
        val availableFields = availableRecordingDashboardFields(
            state = state,
            coMapsGuidance = coMapsNavigation !is CoMapsNavigationState.Disabled,
        )
        Column(
            modifier = modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            RecordingDashboardEditor(
                layout = state.dashboardLayout.withAvailableFields(availableFields),
                availableFields = availableFields,
                onUpdateLayout = onUpdateDashboardLayout,
            )
            RecordingStatsTab(
                state = state,
                totalTime = totalTime,
                movingTime = movingTime,
                now = now,
                unitFormatter = unitFormatter,
                isEditingDashboard = true,
                onUpdateDashboardLayout = onUpdateDashboardLayout,
                coMapsNavigation = coMapsNavigation,
            )
        }
        return
    }

    // Live guidance has a home on each tab that gets one: the map floats the turn
    // strip over the route, and the stats grid carries it as an ordinary cell the
    // layout editor can hide or move like any other. What gets a panel of its own
    // is only the states that are NOT guidance — no permission, no route, no
    // CoMaps — because those carry a button, and a button is not a metric. Only on
    // the two tabs that are about where the user is right now; the splits tabs are
    // a record of what already happened.
    //
    // The tab row stays the first thing on the screen. The panel is a card about
    // one tab's content, sometimes tall, and above the tabs it pushed the way
    // between them down the screen — so it sits under the row it belongs to.
    val guidedSnapshot = (coMapsNavigation as? CoMapsNavigationState.Active)?.snapshot

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ActivityRecordingTabRow(
            selectedTab = selectedTab,
            onSelect = { selectedTab = it },
        )

        if (coMapsNavigation !is CoMapsNavigationState.Disabled &&
            guidedSnapshot == null &&
            !coMapsGuidanceDismissed &&
            activeTab in CoMapsGuidanceTabs
        ) {
            CoMapsGuidancePanel(
                state = coMapsNavigation,
                onRequestPermission = onRequestCoMapsPermission,
                onPlanInCoMaps = onPlanInCoMaps,
                onDismiss = onDismissCoMapsGuidance,
                startGateHint = state.status == ActivityRecordingStatus.IDLE,
            )
        }

        when (activeTab) {
            ActivityRecordingTab.MAP -> Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                OfflineRouteMapOrPreview(
                    points = state.points,
                    routeBreakIndexes = state.routeBreakIndexes,
                    currentPoint = state.latestUiPoint ?: preStartPoint,
                    showRecenterControl = true,
                    plannedRoute = coMapsRoute,
                    modifier = Modifier.fillMaxSize(),
                )
                if (guidedSnapshot != null && !coMapsGuidanceDismissed) {
                    CoMapsMapGuidanceOverlay(
                        snapshot = guidedSnapshot,
                        onDismiss = onDismissCoMapsGuidance,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(12.dp),
                    )
                }
            }
            ActivityRecordingTab.STATS -> RecordingStatsTab(
                state = state,
                totalTime = totalTime,
                movingTime = movingTime,
                now = now,
                unitFormatter = unitFormatter,
                isEditingDashboard = isEditingDashboard,
                onUpdateDashboardLayout = onUpdateDashboardLayout,
                coMapsNavigation = coMapsNavigation,
            )
            ActivityRecordingTab.INTERVALS -> RecordingSplitsTab(
                splits = if (state.manualLaps.isNotEmpty()) {
                    activityRecordingLapSplits(
                        laps = state.manualLaps,
                        points = state.points,
                        routeBreakIndexes = state.routeBreakIndexes,
                        recordingStartTime = state.startTime,
                        activeEndTime = now,
                    )
                } else {
                    activityRecordingIntervalSplits(state.points, state.routeBreakIndexes)
                },
                emptyMessage = stringResource(R.string.activity_entry_recording_no_intervals),
                unitFormatter = unitFormatter,
                label = { split -> stringResource(R.string.activity_entry_recording_split_interval, split.index) },
            )
            ActivityRecordingTab.BY_TIME -> RecordingSplitsTab(
                splits = activityRecordingTimeSplits(
                    points = state.points,
                    routeBreakIndexes = state.routeBreakIndexes,
                    splitMillis = timeSplitMinutes * 60_000L,
                ),
                emptyMessage = stringResource(R.string.activity_entry_recording_no_time_splits),
                unitFormatter = unitFormatter,
                label = { split ->
                    val startMinute = (split.index - 1) * timeSplitMinutes
                    val endMinute = split.index * timeSplitMinutes
                    stringResource(R.string.activity_entry_recording_split_time_range, startMinute, endMinute)
                },
                controls = {
                    TimeSplitSelector(
                        selectedMinutes = timeSplitMinutes,
                        onSelect = { timeSplitMinutes = it },
                    )
                },
            )
            ActivityRecordingTab.BY_DISTANCE -> RecordingSplitsTab(
                splits = activityRecordingDistanceSplits(
                    points = state.points,
                    routeBreakIndexes = state.routeBreakIndexes,
                    splitMeters = distanceSplitMeters,
                ),
                emptyMessage = stringResource(R.string.activity_entry_recording_no_distance_splits),
                unitFormatter = unitFormatter,
                label = { split ->
                    split.endDistanceMeters?.let { endDistance ->
                        distanceRangeLabel(
                            startMeters = split.startDistanceMeters,
                            endMeters = endDistance,
                            unitSystem = unitSystem,
                            unitFormatter = unitFormatter,
                        )
                    } ?: stringResource(R.string.activity_entry_recording_split_interval, split.index)
                },
                controls = {
                    DistanceSplitSelector(
                        selectedMeters = distanceSplitMeters,
                        unitSystem = unitSystem,
                        unitFormatter = unitFormatter,
                        onSelect = { distanceSplitMeters = it },
                    )
                },
            )
        }
    }
}

internal val ActivityRecordingTab.labelRes: Int
    get() = when (this) {
        ActivityRecordingTab.MAP -> R.string.activity_entry_recording_tab_map
        ActivityRecordingTab.STATS -> R.string.activity_entry_recording_tab_stats
        ActivityRecordingTab.INTERVALS -> R.string.activity_entry_recording_tab_intervals
        ActivityRecordingTab.BY_TIME -> R.string.activity_entry_recording_tab_by_time
        ActivityRecordingTab.BY_DISTANCE -> R.string.activity_entry_recording_tab_by_distance
    }

internal const val DefaultTimeSplitMinutes = 5
internal val TimeSplitMinuteOptions = listOf(1, 5, 10)

/**
 * The tab the guidance panel appears on. Guidance is about the route, so it belongs with the
 * route; on the stats grid it was a card sitting on top of the numbers, and the grid already
 * carries guidance as a cell of its own that the layout editor can place like any other.
 */
private val CoMapsGuidanceTabs = setOf(ActivityRecordingTab.MAP)

@Composable
internal fun GpsRecordingOverflowContent(
    state: ActivityRecordingState,
    unitFormatter: UnitFormatter,
    onUpdateMarker: (ActivityRecordingMarker) -> Unit,
    onDeleteMarker: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.markers.isEmpty() && state.errorMessage == null) return

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RecordingMarkersList(
            markers = state.markers,
            unitFormatter = unitFormatter,
            onUpdateMarker = onUpdateMarker,
            onDeleteMarker = onDeleteMarker,
        )

        state.errorMessage?.let { errorMessage ->
            Text(
                text = errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
internal fun ActivityRecordingTabRow(
    selectedTab: ActivityRecordingTab,
    onSelect: (ActivityRecordingTab) -> Unit,
) {
    val tabs = ActivityRecordingTab.entries
    PrimaryScrollableTabRow(
        selectedTabIndex = tabs.indexOf(selectedTab),
        edgePadding = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = activityRecordingAccentColor(),
    ) {
        tabs.forEach { tab ->
            Tab(
                selected = selectedTab == tab,
                onClick = { onSelect(tab) },
                text = { Text(stringResource(tab.labelRes)) },
            )
        }
    }
}
