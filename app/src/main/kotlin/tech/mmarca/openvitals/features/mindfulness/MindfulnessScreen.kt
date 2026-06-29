package tech.mmarca.openvitals.features.mindfulness

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.features.mindfulness.reminders.MindfulnessReminderController
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberChartDaySelection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MindfulnessScreen(
    viewModel: MindfulnessViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onEditMindfulnessSession: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val chartDaySelection = rememberChartDaySelection(state.selectedRange, state.selectedDate)
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(MindfulnessReminderController.hasNotificationPermission(context))
    }
    var enableRemindersAfterPermission by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        if (hasNotificationPermission && enableRemindersAfterPermission) {
            viewModel.setMindfulnessRemindersEnabled(true)
        }
        enableRemindersAfterPermission = false
    }
    val requestNotificationPermission = {
        enableRemindersAfterPermission = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            hasNotificationPermission = true
            viewModel.setMindfulnessRemindersEnabled(true)
            enableRemindersAfterPermission = false
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        hasNotificationPermission = MindfulnessReminderController.hasNotificationPermission(context)
        viewModel.resumeCurrentPeriod(refreshCurrent = true)
    }

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.MINDFULNESS,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { hcUx ->
        MetricDetailScaffold(
            isLoading = state.isLoading,
            selectedRange = state.selectedRange,
            selectedDate = state.selectedDate,
            screenError = state.error,
            onRefresh = viewModel::load,
            onSelectRange = viewModel::selectRange,
            onPreviousPeriod = viewModel::previousPeriod,
            onNextPeriod = viewModel::nextPeriod,
            onSelectDate = viewModel::selectDate,
            weekPeriodMode = state.weekPeriodMode,
            syncPaused = hcUx.syncPaused,
        ) { period ->
            mindfulnessPeriodContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                chartDaySelection = chartDaySelection,
                hasNotificationPermission = hasNotificationPermission,
                onDecreaseGoal = viewModel::decreaseDailyGoal,
                onIncreaseGoal = viewModel::increaseDailyGoal,
                onToggleReminders = viewModel::setMindfulnessRemindersEnabled,
                onRequestNotificationPermission = requestNotificationPermission,
                onSelectReminderTime = viewModel::setMindfulnessReminderTime,
                onEditMindfulnessSession = onEditMindfulnessSession,
                onDeleteMindfulnessSession = viewModel::deleteMindfulnessSessionEntry,
            )
        }
    }
}
