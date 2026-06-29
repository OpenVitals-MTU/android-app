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
import tech.mmarca.openvitals.domain.model.ExerciseRoutePoint
import tech.mmarca.openvitals.features.activity.maps.OfflineRouteMapOrPreview
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardField
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardItemSize
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardLayout
import tech.mmarca.openvitals.domain.preferences.ActivityRecordingDashboardTemplate
import tech.mmarca.openvitals.domain.preferences.AppThemeMode
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
internal fun ActivityRecordingScreen(
    state: ActivityRecordingState,
    unitFormatter: UnitFormatter,
    onStartRecording: (Location?) -> Unit,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onAddLap: () -> Unit,
    onAddMarker: () -> Unit,
    onUpdateMarker: (ActivityRecordingMarker) -> Unit,
    onDeleteMarker: (String) -> Unit,
    onUpdateDashboardLayout: (ActivityRecordingDashboardLayout) -> Unit,
    onChooseSource: () -> Unit,
    onAdjustRepetitionCount: (Long) -> Unit,
    onEndRepetitionSet: () -> Unit,
    onStartNextRepetitionSet: () -> Unit,
    onFinishRecording: () -> Unit,
    onActivityRecordingTitleChanged: (Int?) -> Unit = {},
    onDashboardEditStateChanged: (Boolean, Boolean, () -> Unit) -> Unit = { _, _, _ -> },
    isFocusMode: Boolean = false,
    onFocusModeChanged: (Boolean) -> Unit = {},
    isOutdoorMode: Boolean = false,
    onOutdoorModeChanged: (Boolean) -> Unit = {},
    appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    modifier: Modifier = Modifier,
) {
    var now by remember { mutableStateOf(Instant.now()) }
    var isEditingDashboard by rememberSaveable(state.activityTypeId) { mutableStateOf(false) }
    val currentOnActivityRecordingTitleChanged by rememberUpdatedState(onActivityRecordingTitleChanged)
    val currentOnDashboardEditStateChanged by rememberUpdatedState(onDashboardEditStateChanged)
    val canUseFocusMode = state.isActive && state.recordingKind != ActivityRecordingKind.REPETITION
    val canEditDashboard = !isFocusMode &&
        state.recordingKind == ActivityRecordingKind.GPS_ROUTE &&
        (state.status == ActivityRecordingStatus.IDLE || state.status == ActivityRecordingStatus.PAUSED)
    val idleGpsFixState = rememberPreRecordingGpsFixState(
        enabled = state.recordingKind == ActivityRecordingKind.GPS_ROUTE &&
            state.status == ActivityRecordingStatus.IDLE,
    )
    val view = LocalView.current
    if (state.isActive && state.keepScreenOnDuringRecording) {
        DisposableEffect(view) {
            val previousKeepScreenOn = view.keepScreenOn
            view.keepScreenOn = true
            onDispose {
                view.keepScreenOn = previousKeepScreenOn
            }
        }
    }
    LaunchedEffect(state.status, isFocusMode) {
        if (state.status == ActivityRecordingStatus.RECORDING || isFocusMode) {
            isEditingDashboard = false
        }
    }
    LaunchedEffect(canUseFocusMode, isFocusMode) {
        if (!canUseFocusMode && isFocusMode) {
            onFocusModeChanged(false)
        }
    }
    LaunchedEffect(state.status) {
        while (state.isActive) {
            now = Instant.now()
            delay(1_000L)
        }
    }

    val movingTime = state.movingDuration(now)
    val totalTime = if (state.recordingKind == ActivityRecordingKind.REPETITION) {
        movingTime.plus(state.restDuration(now))
    } else {
        state.elapsedDuration(now)
    }
    LaunchedEffect(Unit) {
        currentOnActivityRecordingTitleChanged(R.string.activity_entry_recording_title)
    }
    LaunchedEffect(canEditDashboard, isEditingDashboard) {
        currentOnDashboardEditStateChanged(canEditDashboard, isEditingDashboard) {
            isEditingDashboard = !isEditingDashboard
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            currentOnActivityRecordingTitleChanged(null)
            currentOnDashboardEditStateChanged(false, false) {}
        }
    }
    BackHandler(enabled = isFocusMode) {
        onFocusModeChanged(false)
    }
    ActivityRecordingTheme(
        outdoorModeEnabled = isOutdoorMode,
        appThemeMode = appThemeMode,
    ) {
        val outdoorUsesLightScheme = isOutdoorMode &&
            !appThemeMode.isDarkTheme(isSystemInDarkTheme())
        ActivityRecordingSystemBars(
            hideSystemBars = isFocusMode && canUseFocusMode,
            outdoorModeEnabled = isOutdoorMode,
            outdoorUsesLightScheme = outdoorUsesLightScheme,
        )

        if (isFocusMode && canUseFocusMode) {
            ActivityRecordingFocusMode(
                state = state,
                totalTime = totalTime,
                movingTime = movingTime,
                now = now,
                unitFormatter = unitFormatter,
                isOutdoorMode = isOutdoorMode,
                onOutdoorModeChanged = onOutdoorModeChanged,
                appThemeMode = appThemeMode,
                onPauseRecording = onPauseRecording,
                onResumeRecording = onResumeRecording,
                onExitFocusMode = { onFocusModeChanged(false) },
                modifier = modifier,
            )
            return@ActivityRecordingTheme
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .then(
                    if (isOutdoorMode) {
                        Modifier.background(MaterialTheme.colorScheme.background)
                    } else {
                        Modifier
                    },
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state.recordingKind) {
            ActivityRecordingKind.REPETITION -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RepetitionRecordingStats(
                        state = state,
                        totalTime = totalTime,
                        movingTime = movingTime,
                        unitFormatter = unitFormatter,
                        onAdjustRepetitionCount = onAdjustRepetitionCount,
                    )

                    state.errorMessage?.let { errorMessage ->
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                RepetitionRecordingControls(
                    state = state,
                    onEndRepetitionSet = onEndRepetitionSet,
                    onStartNextRepetitionSet = onStartNextRepetitionSet,
                    onFinishRecording = onFinishRecording,
                )
            }
            ActivityRecordingKind.TIMED -> {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    RecordingStatsTab(
                        state = state,
                        totalTime = totalTime,
                        movingTime = movingTime,
                        now = now,
                        unitFormatter = unitFormatter,
                        isEditingDashboard = false,
                        onUpdateDashboardLayout = onUpdateDashboardLayout,
                    )

                    state.errorMessage?.let { errorMessage ->
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TimedRecordingControls(
                    state = state,
                    onPauseRecording = onPauseRecording,
                    onResumeRecording = onResumeRecording,
                    onEnterFocusMode = { onFocusModeChanged(true) },
                    onFinishRecording = onFinishRecording,
                )
            }
            ActivityRecordingKind.GPS_ROUTE -> {
                GpsRecordingTabs(
                    state = state,
                    preStartPoint = idleGpsFixState.latestPreciseFix?.toRoutePoint(),
                    totalTime = totalTime,
                    movingTime = movingTime,
                    now = now,
                    unitFormatter = unitFormatter,
                    isEditingDashboard = isEditingDashboard,
                    onUpdateDashboardLayout = onUpdateDashboardLayout,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                )
                GpsRecordingControls(
                    state = state,
                    canStartRecording = idleGpsFixState.latestPreciseFix != null,
                    onStartRecording = { onStartRecording(idleGpsFixState.latestPreciseFix) },
                    onPauseRecording = onPauseRecording,
                    onResumeRecording = onResumeRecording,
                    onEnterFocusMode = { onFocusModeChanged(true) },
                    onFinishRecording = onFinishRecording,
                    onAddLap = onAddLap,
                    onAddMarker = onAddMarker,
                    onChooseSource = onChooseSource,
                )
                GpsRecordingOverflowContent(
                    state = state,
                    unitFormatter = unitFormatter,
                    onUpdateMarker = onUpdateMarker,
                    onDeleteMarker = onDeleteMarker,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 72.dp),
                )
            }
        }
        }
    }
}

@Composable
internal fun ActivityRecordingOutdoorModeToggle(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    appThemeMode: AppThemeMode = AppThemeMode.SYSTEM,
    modifier: Modifier = Modifier,
) {
    val contentDescription = stringResource(R.string.cd_toggle_recording_outdoor_mode)
    OpenVitalsIconButton(
        onClick = { onEnabledChange(!enabled) },
        modifier = modifier,
    ) {
        Icon(
            imageVector = if (enabled) Icons.Outlined.LightMode else Icons.Outlined.WbSunny,
            contentDescription = contentDescription,
            tint = if (enabled) {
                recordingOutdoorAccentForAppTheme(appThemeMode)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}