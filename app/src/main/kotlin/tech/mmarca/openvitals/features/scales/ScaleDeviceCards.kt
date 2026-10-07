package tech.mmarca.openvitals.features.scales

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.devices.xiaomi.ScaleListenerStatus
import tech.mmarca.openvitals.devices.xiaomi.ScaleWriteFailure
import tech.mmarca.openvitals.ui.components.DeviceAvatar
import tech.mmarca.openvitals.ui.components.DeviceValueRow
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton
import tech.mmarca.openvitals.ui.components.formatDeviceTime
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/** What the scale is doing and when it was last heard. No battery: the scale never broadcasts one. */
@Composable
internal fun ScaleStatusCard(state: ScaleDeviceUiState, onRename: () -> Unit) {
    OpenVitalsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(Spacing.lg),
        ) {
            DeviceAvatar(size = 44, icon = Icons.Outlined.MonitorWeight)
            Spacer(modifier = Modifier.width(RowIconGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(scaleStatusRes(state.listenerStatus, state.wokenBySystem)),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = state.lastWeighIn?.let {
                        stringResource(R.string.settings_scales_last_weigh_in_at, formatDeviceTime(it.time))
                    } ?: stringResource(R.string.settings_scales_no_weigh_in),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OpenVitalsIconButton(onClick = onRename) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.settings_scales_rename),
                )
            }
        }
    }
}

/** The last weigh-in's values, what they are worth, and the way to take it back. */
@Composable
internal fun ScaleLastWeighInCard(weighIn: ScaleWeighInDisplay, onDelete: () -> Unit) {
    var confirmingDelete by remember { mutableStateOf(false) }

    ScaleCard {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            ValueLine(R.string.metric_weight, weighIn.weight)
            weighIn.heartRate?.let { ValueLine(R.string.screen_heart_rate, it) }
            weighIn.bodyFat?.let { ValueLine(R.string.metric_body_fat, it) }
            weighIn.leanMass?.let { ValueLine(R.string.metric_lean_mass, it) }
            weighIn.bodyWater?.let { ValueLine(R.string.metric_body_water_mass, it) }
        }
        when {
            weighIn.weightOnly -> Note(R.string.settings_scales_weight_only)
            weighIn.bodyFat != null -> Note(R.string.settings_scales_estimate_note)
        }
        OpenVitalsTextButton(onClick = { confirmingDelete = true }) {
            Text(text = stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
        }
    }

    if (confirmingDelete) {
        ScaleConfirmDialog(
            title = stringResource(R.string.settings_scales_delete_title),
            body = stringResource(R.string.settings_scales_delete_body),
            confirm = stringResource(R.string.action_delete),
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

/** What stands between the scale and the app, each with its one action. */
@Composable
internal fun ScaleListeningCard(
    state: ScaleDeviceUiState,
    android12: Boolean,
    onGrantScanPermission: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onAllowSystemWake: () -> Unit,
    onClaimIgnoredProfile: () -> Unit,
) {
    ScaleCard {
        when (state.listenerStatus) {
            ScaleListenerStatus.PERMISSION_MISSING -> {
                Note(
                    if (android12) R.string.settings_scales_scan_permission else R.string.settings_scales_location_permission,
                    isError = true,
                )
                OpenVitalsTonalButton(onClick = onGrantScanPermission) {
                    Text(stringResource(R.string.action_grant_permission))
                }
            }

            ScaleListenerStatus.BLUETOOTH_OFF -> {
                Note(R.string.settings_scales_bluetooth_off, isError = true)
                OpenVitalsTonalButton(onClick = onOpenBluetoothSettings) {
                    Text(stringResource(R.string.settings_sensors_open_bluetooth))
                }
            }

            ScaleListenerStatus.UNAVAILABLE -> Note(R.string.settings_scales_unavailable, isError = true)
            ScaleListenerStatus.LISTENING, ScaleListenerStatus.OFF -> Unit
        }
        if (!state.wokenBySystem) {
            Note(R.string.settings_scales_wake_row)
            if (state.systemWakeFailed) Note(R.string.settings_scales_wake_failed, isError = true)
            OpenVitalsTonalButton(onClick = onAllowSystemWake) {
                Text(stringResource(R.string.settings_scales_wake_action))
            }
        }
        if (state.otherProfileIgnored) {
            Note(R.string.settings_scales_other_profile)
            OpenVitalsTextButton(onClick = onClaimIgnoredProfile) {
                Text(stringResource(R.string.settings_scales_other_profile_action))
            }
        }
    }
}

/** What Health Connect still lacks to take everything the scale measures. */
@Composable
internal fun ScaleHealthConnectCard(
    state: ScaleDeviceUiState,
    onGrantHealthConnect: () -> Unit,
    onOpenBodyProfile: () -> Unit,
) {
    ScaleCard {
        if (state.hasPendingWeighIns) {
            Note(R.string.settings_scales_pending, isError = true)
            if (state.writeFailure == ScaleWriteFailure.SYNC_PAUSED) Note(R.string.health_connect_sync_paused)
        }
        if (state.missingWritePermissions.isNotEmpty()) {
            Note(R.string.settings_scales_health_connect_missing)
            OpenVitalsTonalButton(onClick = onGrantHealthConnect) {
                Text(stringResource(R.string.action_grant_permission))
            }
        }
        if (state.missingProfileInputs.isNotEmpty()) {
            Note(R.string.settings_scales_profile_hint)
            OpenVitalsTonalButton(onClick = onOpenBodyProfile) {
                Text(stringResource(R.string.settings_body_profile_group_title))
            }
        }
    }
}

/** The scale itself: how it is known, which of its users this is, and the key. */
@Composable
internal fun ScaleDetailsCards(state: ScaleDeviceUiState, onChangeKey: () -> Unit) {
    DeviceValueRow(
        label = stringResource(R.string.settings_scales_address),
        value = state.address.orEmpty(),
    )
    DeviceValueRow(
        label = stringResource(R.string.settings_scales_slot_label),
        value = state.profile?.let { stringResource(R.string.settings_scales_slot, it) } ?: "",
        supporting = if (state.profile == null) stringResource(R.string.settings_scales_slot_unknown) else null,
    )
    OpenVitalsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = stringResource(R.string.settings_scales_key_label), style = MaterialTheme.typography.bodyLarge)
                if (state.keyRejected) {
                    Text(
                        text = stringResource(R.string.settings_scales_key_rejected),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            OpenVitalsTextButton(onClick = onChangeKey) {
                Text(stringResource(R.string.settings_scales_key_replace))
            }
        }
    }
}

/** Last, and its own card: a destructive action wants distance. */
@Composable
internal fun ScaleRemoveCard(onRemove: () -> Unit) {
    OpenVitalsCard {
        Text(
            text = stringResource(R.string.settings_scales_remove),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onRemove)
                .padding(Spacing.lg),
        )
    }
}

@Composable
internal fun ScaleSectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(ChipGap))
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
internal fun ScaleRenameDialog(initialName: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_scales_rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_watch_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** A new key for the same scale. [onSave] says whether the text was a key; the dialog stays open if not. */
@Composable
internal fun ScaleKeyDialog(onSave: (String) -> Boolean, onDismiss: () -> Unit) {
    var key by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_scales_key_replace)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                Note(R.string.settings_scales_key_help)
                ScaleKeyField(
                    value = key,
                    onValueChange = {
                        key = it
                        invalid = false
                    },
                    isError = invalid,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { invalid = !onSave(key) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
internal fun ScaleConfirmDialog(
    title: String,
    body: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = confirm, color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun ScaleCard(content: @Composable () -> Unit) {
    OpenVitalsCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(LayoutMetrics.cardPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            content()
        }
    }
}

@Composable
private fun Note(@StringRes text: Int, isError: Boolean = false) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodySmall,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ValueLine(@StringRes label: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Gap between a row's leading icon and its text. */
private val RowIconGap = 14.dp

/** Gap between a section title and its divider. */
private val ChipGap = 10.dp
