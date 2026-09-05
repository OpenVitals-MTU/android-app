package tech.mmarca.openvitals.features.manualentry

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.domain.model.BodyMeasurementType
import tech.mmarca.openvitals.domain.model.VitalsMeasurementType

@Composable
fun ManualEntryScreen(
    viewModel: ManualEntryViewModel,
    onOpenHydrationEntry: () -> Unit,
    onOpenCarbsEntry: () -> Unit,
    onOpenActivityEntry: () -> Unit,
    onOpenMindfulnessEntry: () -> Unit,
    onOpenBodyMeasurementEntry: (BodyMeasurementType) -> Unit,
    onOpenVitalsMeasurementEntry: (VitalsMeasurementType) -> Unit,
    onOpenCycleEntry: () -> Unit,
    onEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
    onOpenWorkoutPlans: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val requestWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.onHydrationWritePermissionResult()
    }
    val requestBodyWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.onBodyWritePermissionResult()
    }
    val requestNutritionWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.onNutritionWritePermissionResult()
    }
    val requestActivityWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.onActivityWritePermissionResult()
    }
    val requestVitalsWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.onVitalsWritePermissionResult()
    }
    val requestMindfulnessWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.onMindfulnessWritePermissionResult()
    }
    val requestCycleWritePermissions = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) {
        viewModel.onCycleWritePermissionResult()
    }
    val specs = manualEntryWidgetSpecs(
        isEditingWidgets = state.isEditingWidgets,
        onOpenHydrationEntry = viewModel::onHydrationWidgetTapped,
        onOpenCarbsEntry = viewModel::onCarbsWidgetTapped,
        onOpenActivityEntry = viewModel::onActivityWidgetTapped,
        onOpenMindfulnessEntry = viewModel::onMindfulnessWidgetTapped,
        onOpenBodyMeasurementEntry = viewModel::onBodyMeasurementWidgetTapped,
        onOpenVitalsMeasurementEntry = viewModel::onVitalsMeasurementWidgetTapped,
        onOpenCycleEntry = viewModel::onCycleWidgetTapped,
        // The plans screen carries its own Health Connect gate, so the tile
        // navigates directly instead of going through a permission check here.
        onOpenWorkoutPlans = onOpenWorkoutPlans,
    )
    val specsById = specs.associateBy { it.id }
    val visibleIds = state.widgets.filter { it in specsById }
    val hiddenSpecs = specs.filter { it.id !in visibleIds }

    LaunchedEffect(state.isEditingWidgets) {
        onEditStateChanged(state.isEditingWidgets, viewModel::toggleWidgetEdit)
    }
    DisposableEffect(Unit) {
        onDispose { onEditStateChanged(false) {} }
    }
    LaunchedEffect(state.pendingHydrationEntryNavigation) {
        if (state.pendingHydrationEntryNavigation) {
            viewModel.onHydrationEntryNavigationHandled()
            onOpenHydrationEntry()
        }
    }
    LaunchedEffect(state.pendingCarbsEntryNavigation) {
        if (state.pendingCarbsEntryNavigation) {
            viewModel.onCarbsEntryNavigationHandled()
            onOpenCarbsEntry()
        }
    }
    LaunchedEffect(state.pendingActivityEntryNavigation) {
        if (state.pendingActivityEntryNavigation) {
            viewModel.onActivityEntryNavigationHandled()
            onOpenActivityEntry()
        }
    }
    LaunchedEffect(state.pendingMindfulnessEntryNavigation) {
        if (state.pendingMindfulnessEntryNavigation) {
            viewModel.onMindfulnessEntryNavigationHandled()
            onOpenMindfulnessEntry()
        }
    }
    LaunchedEffect(state.pendingCycleEntryNavigation) {
        if (state.pendingCycleEntryNavigation) {
            viewModel.onCycleEntryNavigationHandled()
            onOpenCycleEntry()
        }
    }
    LaunchedEffect(state.pendingBodyEntryNavigation) {
        val type = state.pendingBodyEntryNavigation
        if (type != null) {
            viewModel.onBodyEntryNavigationHandled()
            onOpenBodyMeasurementEntry(type)
        }
    }
    LaunchedEffect(state.pendingVitalsEntryNavigation) {
        val type = state.pendingVitalsEntryNavigation
        if (type != null) {
            viewModel.onVitalsEntryNavigationHandled()
            onOpenVitalsMeasurementEntry(type)
        }
    }

    // A tile whose write set is missing asks Health Connect for exactly that
    // set. The trigger is cleared before launching so a configuration change
    // while the system dialog is up does not launch it a second time; the
    // result callback opens the entry form either way.
    LaunchedEffect(state.pendingHydrationWritePermissionRequest) {
        if (state.pendingHydrationWritePermissionRequest) {
            viewModel.onHydrationWritePermissionRequestLaunched()
            requestWritePermissions.launch(state.hydrationWritePermissions)
        }
    }
    LaunchedEffect(state.pendingCarbsWritePermissionRequest) {
        if (state.pendingCarbsWritePermissionRequest) {
            viewModel.onCarbsWritePermissionRequestLaunched()
            requestNutritionWritePermissions.launch(state.nutritionWritePermissions)
        }
    }
    LaunchedEffect(state.pendingActivityWritePermissionRequest) {
        if (state.pendingActivityWritePermissionRequest) {
            viewModel.onActivityWritePermissionRequestLaunched()
            requestActivityWritePermissions.launch(state.activityWritePermissions)
        }
    }
    LaunchedEffect(state.pendingMindfulnessWritePermissionRequest) {
        if (state.pendingMindfulnessWritePermissionRequest) {
            viewModel.onMindfulnessWritePermissionRequestLaunched()
            requestMindfulnessWritePermissions.launch(state.mindfulnessWritePermissions)
        }
    }
    LaunchedEffect(state.pendingCycleWritePermissionRequest) {
        if (state.pendingCycleWritePermissionRequest) {
            viewModel.onCycleWritePermissionRequestLaunched()
            requestCycleWritePermissions.launch(state.cycleWritePermissions)
        }
    }
    LaunchedEffect(state.pendingBodyWritePermissionRequest) {
        if (state.pendingBodyWritePermissionRequest != null) {
            viewModel.onBodyWritePermissionRequestLaunched()
            requestBodyWritePermissions.launch(state.bodyWritePermissions)
        }
    }
    LaunchedEffect(state.pendingVitalsWritePermissionRequest) {
        if (state.pendingVitalsWritePermissionRequest != null) {
            viewModel.onVitalsWritePermissionRequestLaunched()
            requestVitalsWritePermissions.launch(state.vitalsWritePermissions)
        }
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 8.dp)) {
        item {
            ManualEntryWidgetGrid(
                visibleIds = visibleIds,
                specsById = specsById,
                isEditingWidgets = state.isEditingWidgets,
                onMoveWidgetToTarget = viewModel::moveWidgetToTarget,
                onRemoveWidget = viewModel::removeWidget,
            )
        }
        if (state.isEditingWidgets) {
            hiddenManualEntryWidgets(
                hiddenSpecs = hiddenSpecs,
                onAddWidget = viewModel::addWidget,
            )
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}
