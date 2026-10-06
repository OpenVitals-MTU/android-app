package tech.mmarca.openvitals.features.scales

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.settings.SettingsCardSpacer
import tech.mmarca.openvitals.features.settings.SettingsSectionList
import tech.mmarca.openvitals.ui.components.ScreenErrorContent
import tech.mmarca.openvitals.ui.components.SectionHeader
import tech.mmarca.openvitals.ui.components.rememberHealthConnectPermissionLauncher
import tech.mmarca.openvitals.ui.theme.LayoutMetrics

/** Scales: the key that opens the scale's broadcasts, whether the phone is listening, and the last weigh-in. */
@Composable
fun ScalesSettingsScreen(
    viewModel: ScalesViewModel,
    onOpenBodyProfile: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Grants change in the system's own screens, so everything is read again on the way back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    val healthConnectLauncher = rememberHealthConnectPermissionLauncher(onResult = { viewModel.refresh() })
    val scanPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh() }

    // Android 12 gave scanning its own grant, and lets the system wake an app for a device it watches.
    // Before that a scan rides on location and stops with the app.
    val android12 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val scanPermission = if (android12) {
        Manifest.permission.BLUETOOTH_SCAN
    } else {
        Manifest.permission.ACCESS_FINE_LOCATION
    }
    val cardModifier = Modifier.padding(horizontal = LayoutMetrics.screenGutter)

    SettingsSectionList {
        item { SectionHeader(stringResource(R.string.settings_scales_group_title)) }
        item {
            ScaleSetupCard(
                state = state,
                onKeyInputChange = viewModel::onKeyInputChange,
                onSaveKey = viewModel::saveKey,
                onReplaceKey = viewModel::startReplacingKey,
                onCancelReplaceKey = viewModel::cancelReplacingKey,
                modifier = cardModifier,
            )
        }
        if (state.hasKey) {
            item { SettingsCardSpacer() }
            item {
                ScaleListeningCard(
                    state = state,
                    android12 = android12,
                    onAllowSystemWake = viewModel::allowSystemWake,
                    onGrantScanPermission = { scanPermissionLauncher.launch(scanPermission) },
                    onOpenBluetoothSettings = {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    },
                    onClaimIgnoredProfile = viewModel::claimIgnoredProfile,
                    modifier = cardModifier,
                )
            }
        }
        state.lastWeighIn?.let { weighIn ->
            item { SettingsCardSpacer() }
            item {
                ScaleLastWeighInCard(
                    weighIn = weighIn,
                    state = state,
                    onGrantHealthConnect = { healthConnectLauncher.launch(state.missingWritePermissions) },
                    onOpenBodyProfile = onOpenBodyProfile,
                    onDelete = viewModel::deleteLastWeighIn,
                    modifier = cardModifier,
                )
            }
        }
        if (state.screenError != null) {
            item { ScreenErrorContent(screenError = state.screenError) }
        }
        if (state.hasKey) {
            item { SettingsCardSpacer() }
            item { ScaleRemoveCard(onRemove = viewModel::removeScale, modifier = cardModifier) }
        }
    }
}
