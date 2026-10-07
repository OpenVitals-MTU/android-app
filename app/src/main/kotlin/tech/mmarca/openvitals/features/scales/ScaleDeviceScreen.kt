package tech.mmarca.openvitals.features.scales

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.devices.xiaomi.ScaleListenerStatus
import tech.mmarca.openvitals.ui.components.DeclareAppBar
import tech.mmarca.openvitals.ui.components.ScreenAppBar
import tech.mmarca.openvitals.ui.components.ScreenErrorContent
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher
import tech.mmarca.openvitals.ui.theme.Spacing

/** One scale: whether it is heard, its last weigh-in, what Health Connect still lacks, and the scale itself. */
@Composable
fun ScaleDeviceScreen(
    viewModel: ScaleDeviceViewModel,
    onOpenBodyProfile: () -> Unit,
    onRemoved: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DeclareAppBar(ScreenAppBar(title = state.name))

    // Grants change in Android's own screens, so everything is read again on the way back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val healthConnectLauncher = rememberHealthConnectPermissionLauncher(onResult = { viewModel.refresh() })
    val scanPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh() }
    // Android 12 gave scanning its own grant. Before that a scan rides on location.
    val android12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scanPermission = if (android12) {
        Manifest.permission.BLUETOOTH_SCAN
    } else {
        Manifest.permission.ACCESS_FINE_LOCATION
    }

    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showKeyDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }

    val name = state.name
    if (name == null) {
        // Removed while this screen was open.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_data))
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        ScaleStatusCard(state = state, onRename = { showRenameDialog = true })

        state.lastWeighIn?.let { weighIn ->
            ScaleSectionHeader(stringResource(R.string.settings_scales_last_title))
            ScaleLastWeighInCard(weighIn = weighIn, onDelete = viewModel::deleteLastWeighIn)
        }
        state.screenError?.let { ScreenErrorContent(screenError = it) }

        if (state.needsListeningSection()) {
            ScaleSectionHeader(stringResource(R.string.settings_scales_section_listening))
            ScaleListeningCard(
                state = state,
                android12 = android12,
                onGrantScanPermission = { scanPermissionLauncher.launch(scanPermission) },
                onOpenBluetoothSettings = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) },
                onAllowSystemWake = viewModel::allowSystemWake,
                onClaimIgnoredProfile = viewModel::claimIgnoredProfile,
            )
        }

        if (state.needsHealthConnectSection()) {
            ScaleSectionHeader(stringResource(R.string.settings_health_connect_group_title))
            ScaleHealthConnectCard(
                state = state,
                onGrantHealthConnect = { healthConnectLauncher.launch(state.missingWritePermissions) },
                onOpenBodyProfile = onOpenBodyProfile,
            )
        }

        ScaleSectionHeader(stringResource(R.string.settings_scales_section_scale))
        ScaleDetailsCards(state = state, onChangeKey = { showKeyDialog = true })
        ScaleRemoveCard(onRemove = { showRemoveDialog = true })
    }

    if (showRenameDialog) {
        ScaleRenameDialog(
            initialName = name,
            onSave = { newName ->
                showRenameDialog = false
                viewModel.rename(newName)
            },
            onDismiss = { showRenameDialog = false },
        )
    }
    if (showKeyDialog) {
        ScaleKeyDialog(
            onSave = { key -> viewModel.changeKey(key).also { if (it) showKeyDialog = false } },
            onDismiss = { showKeyDialog = false },
        )
    }
    if (showRemoveDialog) {
        ScaleConfirmDialog(
            title = stringResource(R.string.settings_device_remove_confirm_title, name),
            body = stringResource(R.string.settings_scales_remove_body),
            confirm = stringResource(R.string.action_remove),
            onConfirm = {
                showRemoveDialog = false
                viewModel.remove()
                onRemoved()
            },
            onDismiss = { showRemoveDialog = false },
        )
    }
}

/** The Listening section shows only what stands in the way. */
private fun ScaleDeviceUiState.needsListeningSection(): Boolean =
    listenerStatus != ScaleListenerStatus.LISTENING || !wokenBySystem || otherProfileIgnored

private fun ScaleDeviceUiState.needsHealthConnectSection(): Boolean =
    hasPendingWeighIns || missingWritePermissions.isNotEmpty() || missingProfileInputs.isNotEmpty()
