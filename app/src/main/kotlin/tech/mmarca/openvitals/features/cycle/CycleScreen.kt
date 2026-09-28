package tech.mmarca.openvitals.features.cycle

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.DateTimeFormatterProvider
import tech.mmarca.openvitals.core.presentation.UnitFormatter
import tech.mmarca.openvitals.domain.cycle.LongitudinalCycleItem
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.ui.components.MetricAction
import tech.mmarca.openvitals.ui.components.MetricDetailScaffold
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleScreen(
    viewModel: CycleViewModel,
    unitFormatter: UnitFormatter,
    dateTimeFormatterProvider: DateTimeFormatterProvider,
    onLogCycleEntry: (LocalDate) -> Unit = {},
    onChooseCycleEntry: () -> Unit = {},
    onStartPeriod: () -> Unit = {},
    onOpenCycleSettings: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val resources = LocalResources.current
    var exclusionTarget by remember { mutableStateOf<LongitudinalCycleItem?>(null) }
    var showBackfill by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<CycleObservation?>(null) }

    // A day log saved on the way back must show; the current period is reloaded on every resume.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.resumeCurrentPeriod(refreshCurrent = true)
    }

    val actions = CycleContentActions(
        onOpenDayLog = onLogCycleEntry,
        onChooseLog = onChooseCycleEntry,
        onTogglePillTaken = viewModel::setPillTaken,
        onStartPeriod = onStartPeriod,
        onAddPastPeriod = { showBackfill = true },
        onOpenSettings = onOpenCycleSettings,
        onManageExclusion = { exclusionTarget = it },
        onRequestDelete = { deleteTarget = it },
    )

    WithHealthConnectFeatureScreen(
        feature = HealthConnectFeature.CYCLE,
        isLoading = state.isLoading,
        showInlineSyncBanner = false,
    ) { hcUx ->
        MetricDetailScaffold(
            isLoading = state.isLoading,
            selectedRange = state.selectedRange,
            selectedDate = state.selectedDate,
            screenError = state.error,
            onRefresh = viewModel::load,
            // A month calendar has one range; the arrows and the date picker move it.
            showTimeRangeSelector = false,
            onSelectRange = {},
            onPreviousPeriod = viewModel::previousPeriod,
            onNextPeriod = viewModel::nextPeriod,
            onSelectDate = viewModel::selectDate,
            onSelectDay = viewModel::selectDate,
            weekPeriodMode = state.weekPeriodMode,
            syncPaused = hcUx.syncPaused,
            primaryAction = MetricAction(
                labelRes = R.string.cycle_log_action,
                icon = Icons.Outlined.Add,
                onClick = onChooseCycleEntry,
            ),
        ) { period ->
            cyclePeriodContent(
                state = state,
                period = period,
                unitFormatter = unitFormatter,
                dateTimeFormatterProvider = dateTimeFormatterProvider,
                observations = observationsFor(state.data, resources, state.journalEntries, unitFormatter),
                actions = actions,
            )
        }
    }

    exclusionTarget?.let { item ->
        CycleExclusionDialog(
            cycle = item,
            onDismiss = { exclusionTarget = null },
            onConfirm = { excluded, reason ->
                viewModel.setCycleExclusion(item.startDate, item.endDate, excluded, reason)
                exclusionTarget = null
            },
        )
    }
    deleteTarget?.let { observation ->
        CycleDeleteObservationDialog(
            observation = observation,
            dateTimeFormatterProvider = dateTimeFormatterProvider,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                val kind = observation.kind
                val date = observation.dayLogDate
                when {
                    kind != null && observation.id.isNotBlank() -> viewModel.deleteCycleEntry(kind, observation.id)
                    date != null -> viewModel.deleteJournalEntry(date)
                }
                deleteTarget = null
            },
        )
    }
    if (showBackfill) {
        CycleBackfillDialog(
            existingStarts = state.statistics?.cycleStarts.orEmpty().toSet(),
            onConfirm = { date ->
                viewModel.addPastPeriod(date)
                showBackfill = false
            },
            onDismiss = { showBackfill = false },
        )
    }
}
