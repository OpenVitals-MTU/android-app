package tech.mmarca.openvitals.features.scales

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.devices.xiaomi.ScaleListenerStatus
import tech.mmarca.openvitals.ui.components.DeviceAvatar
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.OsPermissionsDialog
import tech.mmarca.openvitals.ui.components.formatDeviceTime
import tech.mmarca.openvitals.ui.theme.Spacing

/** The scale's card, or the way to add one. Everything about the scale lives in its device view. */
@Composable
fun ScalesSettingsScreen(
    viewModel: ScalesViewModel,
    onOpenScale: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.refreshOsPermissions() }

    // Grants change in Android's own screens, so they are re-read on the way back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.settings_scales_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )

        val scale = state.scale
        if (scale == null) {
            NoScaleCard(onAdd = viewModel::startAdd, modifier = Modifier.padding(horizontal = Spacing.lg))
        } else {
            ScaleRow(scale = scale, onOpen = onOpenScale, modifier = Modifier.padding(horizontal = Spacing.lg))
        }
    }

    if (state.showPermissionsGate) {
        OsPermissionsDialog(
            catalog = state.osPermissions,
            title = stringResource(R.string.settings_scales_permissions_title),
            body = stringResource(R.string.settings_scales_permissions_body),
            continueLabel = stringResource(R.string.settings_scales_add),
            onGrantAll = { permissionLauncher.launch(state.osPermissions.requestablePermissions.toTypedArray()) },
            // The scale's rows are all runtime grants: no settings screen to walk.
            onGrantRow = { row -> permissionLauncher.launch(row.permissions.toTypedArray()) },
            onContinue = viewModel::openAddFlow,
            onDismiss = viewModel::dismissPermissionsGate,
        )
    }

    if (state.showAddFlow) {
        AddScaleDialog(viewModel = viewModel, state = state)
    }
}

@Composable
private fun NoScaleCard(onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val supportedScalesUrl = stringResource(R.string.settings_scales_supported_url)
    OpenVitalsCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = stringResource(R.string.settings_scales_empty_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.settings_scales_empty_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // The list lives on the site: it grows without an app update.
            OpenVitalsTextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, supportedScalesUrl.toUri())) }) {
                Text(stringResource(R.string.settings_scales_supported_action))
            }
            OpenVitalsButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(AddIconSize),
                )
                Text(
                    text = stringResource(R.string.settings_scales_add),
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
    }
}

@Composable
private fun ScaleRow(scale: ScaleCardState, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    OpenVitalsCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(Spacing.lg),
        ) {
            DeviceAvatar(icon = Icons.Outlined.MonitorWeight)
            Spacer(modifier = Modifier.width(RowIconGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = scale.name, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = stringResource(scale.statusRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = scale.lastWeighInAt?.let {
                        stringResource(R.string.settings_scales_last_weigh_in_at, formatDeviceTime(it))
                    } ?: stringResource(R.string.settings_scales_no_weigh_in),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** One line on what the scale is doing, for the card and the device header. */
internal fun ScaleCardState.statusRes(): Int = scaleStatusRes(status, wokenBySystem)

internal fun scaleStatusRes(status: ScaleListenerStatus, wokenBySystem: Boolean): Int = when (status) {
    ScaleListenerStatus.LISTENING ->
        if (wokenBySystem) R.string.settings_scales_listening else R.string.settings_scales_listening_while_open

    ScaleListenerStatus.BLUETOOTH_OFF -> R.string.settings_scales_bluetooth_off
    ScaleListenerStatus.PERMISSION_MISSING -> R.string.settings_scales_not_listening
    ScaleListenerStatus.UNAVAILABLE -> R.string.settings_scales_unavailable
    ScaleListenerStatus.OFF -> R.string.settings_scales_not_listening
}

private val AddIconSize = 18.dp

/** Gap between a row's leading icon and its text. */
private val RowIconGap = 14.dp
