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
internal fun ActivityRecordingFocusMode(
    state: ActivityRecordingState,
    totalTime: Duration,
    movingTime: Duration,
    now: Instant,
    unitFormatter: UnitFormatter,
    isOutdoorMode: Boolean,
    onOutdoorModeChanged: (Boolean) -> Unit,
    appThemeMode: AppThemeMode,
    onPauseRecording: () -> Unit,
    onResumeRecording: () -> Unit,
    onExitFocusMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val availableFields = availableRecordingDashboardFields(state)
    val layout = state.dashboardLayout.withAvailableFields(availableFields)
    val stats = recordingDashboardStats(
        state = state,
        totalTime = totalTime,
        movingTime = movingTime,
        now = now,
        unitFormatter = unitFormatter,
    )
    val clockText = now
        .atZone(ZoneId.systemDefault())
        .format(TwentyFourHourClockFormatter)
    val isPaused = state.status == ActivityRecordingStatus.PAUSED

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            ActivityRecordingOutdoorModeToggle(
                enabled = isOutdoorMode,
                onEnabledChange = onOutdoorModeChanged,
                appThemeMode = appThemeMode,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            Text(
                text = clockText,
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.align(Alignment.Center),
                maxLines = 1,
            )
            OpenVitalsIconButton(
                onClick = onExitFocusMode,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    imageVector = Icons.Outlined.FullscreenExit,
                    contentDescription = stringResource(R.string.cd_exit_recording_focus_mode),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        RecordingDashboardGrid(
            layout = layout,
            stats = stats,
            isEditingDashboard = false,
            onUpdateLayout = {},
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            fillHeight = true,
        )

        OpenVitalsButton(
            onClick = if (isPaused) onResumeRecording else onPauseRecording,
            enabled = state.status == ActivityRecordingStatus.RECORDING || isPaused,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Outlined.PlayArrow else Icons.Outlined.Pause,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(if (isPaused) R.string.action_resume else R.string.action_pause),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
internal fun ActivityRecordingSystemBars(
    hideSystemBars: Boolean,
    outdoorModeEnabled: Boolean,
    outdoorUsesLightScheme: Boolean,
) {
    val view = LocalView.current
    DisposableEffect(hideSystemBars, outdoorModeEnabled, outdoorUsesLightScheme, view) {
        val window = view.context.findActivity()?.window
        if (window == null) {
            return@DisposableEffect onDispose {}
        }
        val controller = WindowInsetsControllerCompat(window, view)
        val previousBehavior = controller.systemBarsBehavior
        val previousLightStatusBars = controller.isAppearanceLightStatusBars
        val previousLightNavigationBars = controller.isAppearanceLightNavigationBars
        val previousStatusBarColor = window.statusBarColor
        val previousNavigationBarColor = window.navigationBarColor

        if (outdoorModeEnabled) {
            if (outdoorUsesLightScheme) {
                window.statusBarColor = AndroidColor.WHITE
                window.navigationBarColor = AndroidColor.WHITE
                controller.isAppearanceLightStatusBars = true
                controller.isAppearanceLightNavigationBars = true
            } else {
                window.statusBarColor = AndroidColor.BLACK
                window.navigationBarColor = AndroidColor.BLACK
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }

        if (hideSystemBars) {
            val hiddenBars = if (view.context.isGestureNavigationMode()) {
                WindowInsetsCompat.Type.statusBars()
            } else {
                WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
            }
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(hiddenBars)
        }

        onDispose {
            controller.systemBarsBehavior = previousBehavior
            controller.isAppearanceLightStatusBars = previousLightStatusBars
            controller.isAppearanceLightNavigationBars = previousLightNavigationBars
            window.statusBarColor = previousStatusBarColor
            window.navigationBarColor = previousNavigationBarColor
            if (hideSystemBars) {
                controller.show(
                    WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars(),
                )
            }
        }
    }
}


internal tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

internal fun Context.isGestureNavigationMode(): Boolean {
    val resourceId = resources.getIdentifier(
        "config_navBarInteractionMode",
        "integer",
        "android",
    )
    return resourceId != 0 && resources.getInteger(resourceId) == AndroidGestureNavigationMode
}

internal const val AndroidGestureNavigationMode = 2
internal val TwentyFourHourClockFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")