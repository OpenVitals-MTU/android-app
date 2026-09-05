package tech.mmarca.openvitals.features.devicesync

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.devicesync.protocol.SyncRole
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsCardStyle
import tech.mmarca.openvitals.ui.components.OpenVitalsFilledButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton

/**
 * The "Sync with another phone" wizard. The permission choreography lives
 * here because launchers are Compose-scoped. Leaving the route tears the
 * RFCOMM apparatus down via the ViewModel.
 */
@Composable
fun DeviceSyncScreen(
    onDone: () -> Unit,
    viewModel: DeviceSyncViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Which role tapped on the role step, carried across the permission chain.
    var pendingRole by rememberSaveable { mutableStateOf<String?>(null) }

    val discoverableLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        // The result code is the granted discoverable window in seconds.
        val seconds =
            if (result.resultCode == Activity.RESULT_CANCELED) 0 else result.resultCode
        viewModel.startHosting(seconds)
    }

    val healthPermissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract(),
    ) { _ ->
        viewModel.refreshHealthPermissions()
        when (pendingRole) {
            SyncRole.HOST.name -> discoverableLauncher.launch(viewModel.discoverableIntent())
            SyncRole.GUEST.name -> viewModel.startScanning()
        }
        pendingRole = null
    }

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grants ->
        if (grants.values.all { it }) {
            healthPermissionLauncher.launch(viewModel.healthPermissionsToRequest())
        } else {
            pendingRole = null
            viewModel.onBluetoothPermissionsDenied()
        }
    }

    fun chooseRole(role: SyncRole) {
        pendingRole = role.name
        bluetoothPermissionLauncher.launch(DeviceSyncPermissions.required())
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (state.step) {
            DeviceSyncStep.ROLE -> DeviceSyncRoleStep(
                state = state,
                onChooseHost = { chooseRole(SyncRole.HOST) },
                onChooseGuest = { chooseRole(SyncRole.GUEST) },
            )
            DeviceSyncStep.HOST_WAITING -> DeviceSyncHostStep(
                state = state,
                onCancel = { viewModel.cancel(); onDone() },
            )
            DeviceSyncStep.GUEST_SCANNING -> DeviceSyncScanStep(
                state = state,
                onSelectDevice = viewModel::selectDevice,
                onRescan = viewModel::rescan,
                onCancel = { viewModel.cancel(); onDone() },
            )
            DeviceSyncStep.GUEST_CODE -> DeviceSyncCodeStep(
                state = state,
                onDigit = viewModel::enterDigit,
                onDelete = viewModel::deleteDigit,
                onSubmit = viewModel::submitCode,
            )
            DeviceSyncStep.RANGE -> DeviceSyncRangeStep(
                state = state,
                onSetRange = viewModel::setRange,
                onNext = viewModel::goToTypes,
            )
            DeviceSyncStep.TYPES -> DeviceSyncTypesStep(
                state = state,
                onToggleType = viewModel::toggleType,
                onStartSync = viewModel::startSync,
            )
            DeviceSyncStep.SYNCING -> DeviceSyncProgressStep(
                state = state,
                onCancel = { viewModel.cancel(); onDone() },
            )
            DeviceSyncStep.REPORT -> DeviceSyncReportStep(
                state = state,
                onDone = { viewModel.reset(); onDone() },
            )
        }
    }
}

// Shared wizard pieces.

@Composable
internal fun DeviceSyncHero(icon: ImageVector, title: String, body: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 12.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(top = 4.dp)
                .size(32.dp),
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun DeviceSyncBanner(message: String, isError: Boolean = false) {
    OpenVitalsCard(
        style = if (isError) OpenVitalsCardStyle.Error else OpenVitalsCardStyle.Neutral,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(14.dp),
        )
    }
}

@Composable
internal fun DeviceSyncStatRow(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Text(text = "$value", style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun DeviceSyncBottomButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OpenVitalsFilledButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
    ) {
        Text(label)
    }
}

@Composable
internal fun DeviceSyncCancelButton(onCancel: () -> Unit) {
    OpenVitalsOutlinedButton(
        onClick = onCancel,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Text(stringResource(R.string.device_sync_cancel))
    }
}

/** The localized, user-facing message for a wizard error. */
@Composable
internal fun deviceSyncErrorText(error: DeviceSyncError): String = stringResource(
    when (error) {
        DeviceSyncError.CONNECT_FAILED,
        DeviceSyncError.CONNECT_TIMEOUT,
        -> R.string.device_sync_error_connect
        DeviceSyncError.PERMISSION_DENIED -> R.string.device_sync_error_permission
        DeviceSyncError.DISCOVERABLE_DECLINED -> R.string.device_sync_error_discoverable
        DeviceSyncError.RECORDING_ACTIVE -> R.string.device_sync_error_recording
        DeviceSyncError.SYNC_FAILED -> R.string.device_sync_error_generic
    },
)
