package tech.mmarca.openvitals.features.imports.medical

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MedicalInformation
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.core.performance.offMainIo
import tech.mmarca.openvitals.features.medical.MedicalAccessAction
import tech.mmarca.openvitals.features.medical.rememberMedicalAccessAction
import tech.mmarca.openvitals.healthconnect.HealthConnectFeature
import tech.mmarca.openvitals.healthconnect.openHealthConnectPermissionSettings
import tech.mmarca.openvitals.ui.components.ConfirmLeaveWhileImporting
import tech.mmarca.openvitals.ui.components.EmptyState
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.ScreenErrorContent
import tech.mmarca.openvitals.ui.components.WithHealthConnectFeatureScreen
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/**
 * The medical records import: a stepped screen like the CSV import. The records come from a
 * picked file or from a SMART Health Card scanned with the camera. Write access is asked for
 * before anything is read, because matching the sources already in Health Connect needs it.
 */
@Composable
fun MedicalImportScreen(
    onDone: () -> Unit,
    /** A file to read straight away, such as the Apple Health export the Apple import just analysed. */
    initialUri: String? = null,
    viewModel: MedicalImportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    ConfirmLeaveWhileImporting(importing = state.isImporting)

    val analyze: (Uri) -> Unit = { uri ->
        scope.launch {
            val name = offMainIo { documentName(context, uri) }.getOrNull()
            viewModel.analyze(name, documentOpener(context, uri))
        }
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // Cancelling the picker is not an error.
        if (uri != null) analyze(uri)
    }
    // The scanner replaces the wizard while it is open. The camera is asked for only when Scan is tapped.
    var scanning by rememberSaveable { mutableStateOf(false) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scanning = true else viewModel.onPickFailed(MedicalImportPickError.CAMERA_DENIED)
    }
    val hasCamera = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    var initialRead by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(initialUri, state.canWrite) {
        // The file is read once write access is there, since matching sources needs it.
        if (initialUri != null && !initialRead && state.canWrite) {
            initialRead = true
            analyze(initialUri.toUri())
        }
    }
    val permissionLauncher = rememberHealthConnectPermissionLauncher(onResult = viewModel::refresh)
    val access = rememberMedicalAccessAction(
        missing = if (state.canWrite) emptySet() else viewModel.permissionsToRequest(),
        firstRequestDone = state.firstRequestDone,
    )
    val accessBlocked = access == MedicalAccessAction.OpenSettings
    val requestAccess: () -> Unit = {
        when (access) {
            is MedicalAccessAction.Ask -> {
                permissionLauncher.launch(access.permissions)
                viewModel.onPermissionsRequested()
            }
            // Health Connect would close the request at once: only its settings can grant write now.
            MedicalAccessAction.OpenSettings -> openHealthConnectPermissionSettings(context)
            MedicalAccessAction.None -> Unit
        }
    }

    WithHealthConnectFeatureScreen(feature = HealthConnectFeature.MEDICAL_IMPORT, modifier = Modifier.fillMaxSize()) {
        if (!state.available) {
            EmptyState(
                message = stringResource(R.string.screen_error_feature_unavailable),
                detail = stringResource(R.string.medical_records_unavailable),
                icon = Icons.Outlined.MedicalInformation,
            )
            return@WithHealthConnectFeatureScreen
        }
        if (scanning) {
            BackHandler { scanning = false }
            MedicalCardScanner(
                onScanned = { card ->
                    scanning = false
                    viewModel.analyze(ScannedCardFileName) { card.byteInputStream() }
                },
                onCancel = { scanning = false },
            )
            return@WithHealthConnectFeatureScreen
        }
        when (state.step) {
            MedicalImportStep.PICK -> MedicalImportPickStep(
                state = state,
                accessBlocked = accessBlocked,
                onPick = { if (state.canWrite) picker.launch(MedicalImportMimeTypes) else requestAccess() },
                onScan = { cameraLauncher.launch(Manifest.permission.CAMERA) }.takeIf { hasCamera && state.canWrite },
            )
            MedicalImportStep.REVIEW -> MedicalImportReviewStep(state = state, viewModel = viewModel)
            MedicalImportStep.CONFIRM -> MedicalImportConfirmStep(
                state = state,
                viewModel = viewModel,
                accessBlocked = accessBlocked,
                onRequestAccess = requestAccess,
            )
            MedicalImportStep.IMPORTING -> MedicalImportProgressStep(state = state)
            MedicalImportStep.DONE -> MedicalImportDoneStep(state = state, viewModel = viewModel, onDone = onDone)
        }
    }
}

/** [onScan] is null on a phone with no camera, and until write access is there. */
@Composable
private fun MedicalImportPickStep(state: MedicalImportUiState, accessBlocked: Boolean, onPick: () -> Unit, onScan: (() -> Unit)?) {
    LazyColumn(contentPadding = PaddingValues(LayoutMetrics.screenGutter)) {
        item {
            OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(text = stringResource(R.string.medical_import_title), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = stringResource(R.string.medical_import_pick_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!state.canWrite) {
                        Text(text = stringResource(R.string.medical_import_needs_write), style = MaterialTheme.typography.bodySmall)
                    }
                    if (accessBlocked) {
                        Text(text = stringResource(R.string.medical_access_blocked), style = MaterialTheme.typography.bodySmall)
                    }
                    state.pickError?.let { error ->
                        Text(
                            text = stringResource(error.messageRes),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    if (state.isReading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text(text = stringResource(R.string.medical_import_reading), style = MaterialTheme.typography.bodySmall)
                    }
                    OpenVitalsOutlinedButton(onClick = onPick, enabled = !state.isReading, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(accessLabelRes(state.canWrite, accessBlocked, ready = R.string.medical_import_choose_file)))
                    }
                    if (onScan != null) {
                        OpenVitalsOutlinedButton(onClick = onScan, enabled = !state.isReading, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.medical_import_scan_action))
                        }
                    }
                }
            }
        }
        state.error?.let { error -> item { ScreenErrorContent(error) } }
    }
}

@Composable
private fun MedicalImportProgressStep(state: MedicalImportUiState) {
    val progress = state.progress
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(LayoutMetrics.screenGutter),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        val fraction = progress?.takeIf { it.total > 0 }?.let { it.done.toFloat() / it.total }
        if (fraction != null) {
            LinearProgressIndicator(progress = { fraction }, modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Text(
            text = stringResource(R.string.medical_import_progress, progress?.done ?: 0, progress?.total ?: 0),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

private val MedicalImportPickError.messageRes: Int
    get() = when (this) {
        MedicalImportPickError.NOT_SUPPORTED -> R.string.medical_import_error_not_supported
        MedicalImportPickError.TOO_LARGE -> R.string.medical_import_error_too_large
        MedicalImportPickError.UNREADABLE -> R.string.medical_import_error_unreadable
        MedicalImportPickError.NO_RECORDS -> R.string.medical_import_error_no_records
        MedicalImportPickError.CAMERA_DENIED -> R.string.medical_import_error_camera_denied
    }

/** The name a scanned card's text takes when it is kept as a document. */
private const val ScannedCardFileName = "smart-health-card.txt"
