package tech.mmarca.openvitals.features.manualentry.hydration

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.core.presentation.UnitFormatter

@Composable
fun HydrationEntryScreen(
    viewModel: HydrationEntryViewModel,
    unitFormatter: UnitFormatter,
    onEntrySaved: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.refreshPermission()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }
    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            viewModel.onSaveCompletedHandled()
            onEntrySaved()
        }
    }

    LazyColumn {
        item {
            HydrationTrackerCard(
                state = state,
                unitFormatter = unitFormatter,
                onSelectBeverage = viewModel::selectBeverage,
                onSelectContainer = viewModel::selectContainer,
                onAddContainerEntry = viewModel::addContainerHydrationEntry,
                onAddSelectedEntry = viewModel::addSelectedHydrationEntry,
                onAddCustomEntry = viewModel::addCustomHydrationEntry,
                onEntryTimeChanged = viewModel::updateEntryTime,
                onRequestWritePermission = {
                    requestWritePermissions.launch(state.hydrationWritePermissions)
                },
                onUpdateContainerSize = viewModel::updateContainerSize,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}
