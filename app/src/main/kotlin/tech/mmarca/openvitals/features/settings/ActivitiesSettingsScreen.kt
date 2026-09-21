package tech.mmarca.openvitals.features.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.theme.LayoutMetrics

/** Activities: recording, the step-distance backfill, offline maps and elevation tiles. */
@Composable
fun ActivitiesSettingsScreen(viewModel: ActivitiesSettingsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    val offlineMapPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importOfflineMap) }

    val elevationTilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importElevationTile) }

    SettingsSectionList {
        activitiesSettingsCards(
            state = state,
            viewModel = viewModel,
            onImportOfflineMap = { offlineMapPicker.launch(OfflineMapMimeTypes) },
            onImportElevationTile = { elevationTilePicker.launch(ElevationTileMimeTypes) },
        )
    }
}

private fun LazyListScope.activitiesSettingsCards(
    state: ActivitiesSettingsUiState,
    viewModel: ActivitiesSettingsViewModel,
    onImportOfflineMap: () -> Unit,
    onImportElevationTile: () -> Unit,
) {
    item { SectionHeader(stringResource(SettingsSection.ACTIVITIES.titleRes)) }
    item {
        FavoriteActivityCard(
            selectedExerciseType = state.favoriteActivityExerciseType,
            onSelect = viewModel::selectFavoriteActivity,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        ActivitySplitDistanceCard(
            selectedMeters = state.activitySplitDistanceMeters,
            unitSystem = state.distanceUnitSystem,
            onSelect = viewModel::setActivitySplitDistance,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        StepDistanceBackfillCard(
            enabled = state.stepDistanceBackfillEnabled,
            strideLengthMeters = state.strideLengthMeters,
            unitSystem = state.distanceUnitSystem,
            onSave = viewModel::saveStepDistanceBackfill,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        ActivityRecordingPreferencesCard(
            preferences = state.activityRecordingPreferences,
            onChange = viewModel::updateActivityRecordingPreferences,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
            coMapsPermissionName = viewModel::coMapsPermissionName,
            onCoMapsPermissionResult = viewModel::onCoMapsPermissionChanged,
        )
    }
    item { SettingsCardSpacer() }
    item {
        OfflineMapsCard(
            mapPacks = state.offlineMapPacks,
            activeFormat = state.activeOfflineMapFormat,
            isImporting = state.isImportingOfflineMap,
            progress = state.offlineMapImportProgress,
            result = state.offlineMapImportResult,
            error = state.offlineMapImportError,
            onImport = onImportOfflineMap,
            onSelectActiveFormat = viewModel::selectOfflineMapFormat,
            onDeleteMap = viewModel::deleteOfflineMap,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
    item { SettingsCardSpacer() }
    item {
        ElevationCorrectionCard(
            enabled = state.elevationCorrectionEnabled,
            tiles = state.elevationTiles,
            isImporting = state.isImportingElevationTile,
            result = state.elevationTileImportResult,
            error = state.elevationTileImportError,
            onEnabledChange = viewModel::setElevationCorrectionEnabled,
            onImport = onImportElevationTile,
            onDeleteTile = viewModel::deleteElevationTile,
            modifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter),
        )
    }
}
