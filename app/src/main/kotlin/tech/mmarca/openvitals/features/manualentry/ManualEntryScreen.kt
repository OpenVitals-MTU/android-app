package tech.mmarca.openvitals.features.manualentry

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
    // A tile only opens its entry screen. The screen shows the write-permission
    // callout when needed, and its Grant button is the one place that asks
    // Health Connect, so the single dialog Health Connect allows is not spent
    // here before the user has seen the form.
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
        // navigates directly instead of going through the view model.
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
