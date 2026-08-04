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
    onEditStateChanged: (Boolean, () -> Unit) -> Unit = { _, _ -> },
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
    val specs = manualEntryWidgetSpecs(
        isEditingWidgets = state.isEditingWidgets,
        onOpenHydrationEntry = viewModel::onHydrationWidgetTapped,
        onOpenCarbsEntry = viewModel::onCarbsWidgetTapped,
        onOpenActivityEntry = viewModel::onActivityWidgetTapped,
        onOpenMindfulnessEntry = viewModel::onMindfulnessWidgetTapped,
        onOpenBodyMeasurementEntry = viewModel::onBodyMeasurementWidgetTapped,
        onOpenVitalsMeasurementEntry = viewModel::onVitalsMeasurementWidgetTapped,
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

    if (state.showHydrationWritePermissionPrompt) {
        HydrationWritePermissionPrompt(
            onDismiss = viewModel::dismissHydrationWritePermissionPrompt,
            onOpenEntry = viewModel::continueHydrationEntryFromWritePermissionPrompt,
            onGrant = {
                viewModel.grantHydrationWritePermissionFromPrompt()
                requestWritePermissions.launch(state.hydrationWritePermissions)
            },
        )
    }

    if (state.showActivityWritePermissionPrompt) {
        ActivityWritePermissionPrompt(
            onDismiss = viewModel::dismissActivityWritePermissionPrompt,
            onOpenEntry = viewModel::continueActivityEntryFromWritePermissionPrompt,
            onGrant = {
                viewModel.grantActivityWritePermissionFromPrompt()
                requestActivityWritePermissions.launch(state.activityWritePermissions)
            },
        )
    }

    if (state.showNutritionWritePermissionPrompt) {
        NutritionWritePermissionPrompt(
            onDismiss = viewModel::dismissNutritionWritePermissionPrompt,
            onOpenEntry = viewModel::continueCarbsEntryFromWritePermissionPrompt,
            onGrant = {
                viewModel.grantNutritionWritePermissionFromPrompt()
                requestNutritionWritePermissions.launch(state.nutritionWritePermissions)
            },
        )
    }

    if (state.showBodyWritePermissionPrompt) {
        state.bodyWritePermissionPromptType?.let { type ->
            BodyWritePermissionPrompt(
                type = type,
                onDismiss = viewModel::dismissBodyWritePermissionPrompt,
                onOpenEntry = viewModel::continueBodyEntryFromWritePermissionPrompt,
                onGrant = {
                    viewModel.grantBodyWritePermissionFromPrompt()
                    requestBodyWritePermissions.launch(state.bodyWritePermissions)
                },
            )
        }
    }

    if (state.showVitalsWritePermissionPrompt) {
        state.vitalsWritePermissionPromptType?.let { type ->
            VitalsWritePermissionPrompt(
                type = type,
                onDismiss = viewModel::dismissVitalsWritePermissionPrompt,
                onOpenEntry = viewModel::continueVitalsEntryFromWritePermissionPrompt,
                onGrant = {
                    viewModel.grantVitalsWritePermissionFromPrompt()
                    requestVitalsWritePermissions.launch(state.vitalsWritePermissions)
                },
            )
        }
    }

    if (state.showMindfulnessWritePermissionPrompt) {
        MindfulnessWritePermissionPrompt(
            onDismiss = viewModel::dismissMindfulnessWritePermissionPrompt,
            onOpenEntry = viewModel::continueMindfulnessEntryFromWritePermissionPrompt,
            onGrant = {
                viewModel.grantMindfulnessWritePermissionFromPrompt()
                requestMindfulnessWritePermissions.launch(state.mindfulnessWritePermissions)
            },
        )
    }
}
