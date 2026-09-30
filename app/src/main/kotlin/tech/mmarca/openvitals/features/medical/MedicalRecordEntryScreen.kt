package tech.mmarca.openvitals.features.medical

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.presentation.ScreenError
import tech.mmarca.openvitals.core.presentation.resolve
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.DeclareAppBar
import tech.mmarca.openvitals.ui.components.FullScreenLoading
import tech.mmarca.openvitals.ui.components.ScreenAppBar
import tech.mmarca.openvitals.ui.components.ScreenErrorContent
import tech.mmarca.openvitals.ui.components.StepBar
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * Adds or edits a vaccine, an allergy, a medication or a condition. Write access is asked for
 * here, as the import wizard asks: all thirteen before the area's first request, then write
 * alone, or Health Connect's settings once it no longer asks.
 */
@Composable
fun MedicalRecordEntryScreen(viewModel: MedicalRecordEntryViewModel, onDone: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    state.kind?.let { kind ->
        val title = stringResource(entryTitleRes(kind, state.isEdit))
        DeclareAppBar(remember(title) { ScreenAppBar(title = title) })
    }
    LaunchedEffect(state.saved) { if (state.saved) onDone() }
    val permissionLauncher = rememberHealthConnectPermissionLauncher(onResult = viewModel::load)
    val access = rememberMedicalAccessAction(
        missing = if (state.canWrite) emptySet() else viewModel.permissionsToRequest(),
        firstRequestDone = state.firstRequestDone,
    )
    val sourceName = stringResource(R.string.medical_entry_source_name)
    val onNext: (() -> Unit)? = when {
        state.canWrite -> if (state.canSave(LocalDate.now())) ({ viewModel.save(sourceName) }) else null
        access is MedicalAccessAction.Ask -> {
            {
                permissionLauncher.launch(access.permissions)
                viewModel.onPermissionsRequested()
            }
        }
        access == MedicalAccessAction.OpenSettings -> ({ openHealthConnectPermissionSettings(context) })
        else -> null
    }
    val nextLabel = stringResource(accessLabelRes(state.canWrite, access == MedicalAccessAction.OpenSettings))

    WithHealthConnectFeatureScreen(feature = HealthConnectFeature.MEDICAL_IMPORT, modifier = Modifier.fillMaxSize()) {
        val draft = state.draft
        when {
            state.isLoading -> FullScreenLoading()
            draft == null -> ScreenErrorContent(state.error ?: ScreenError.MissingArgument)
            else -> Column(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(LayoutMetrics.screenGutter),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    if (!state.canWrite) {
                        item { AccessNote(blocked = access == MedicalAccessAction.OpenSettings) }
                    }
                    if (state.needsIdentity) {
                        item { MedicalOwnerCard(state.owner, viewModel::updateOwner) }
                    }
                    item { MedicalRecordEntryFields(draft, viewModel::update) }
                    state.error?.let { error ->
                        item { Text(text = error.resolve().orEmpty(), color = MaterialTheme.colorScheme.error) }
                    }
                }
                StepBar(nextLabel = nextLabel, onNext = if (state.isSaving) null else onNext)
            }
        }
    }
}

@Composable
private fun AccessNote(blocked: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(text = stringResource(R.string.medical_import_needs_write), style = MaterialTheme.typography.bodyMedium)
        if (blocked) {
            Text(text = stringResource(R.string.medical_access_blocked), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Save once write access is there; otherwise the way to get it. */
private fun accessLabelRes(canWrite: Boolean, blocked: Boolean): Int = when {
    canWrite -> R.string.action_save
    blocked -> R.string.medical_record_open_health_connect
    else -> R.string.medical_import_allow_access
}
