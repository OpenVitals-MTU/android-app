package tech.mmarca.openvitals.features.scales

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.devices.xiaomi.ScaleListenerStatus
import tech.mmarca.openvitals.devices.xiaomi.ScaleWriteFailure
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsTextButton
import tech.mmarca.openvitals.ui.components.OpenVitalsTonalButton
import tech.mmarca.openvitals.ui.theme.LayoutMetrics
import tech.mmarca.openvitals.ui.theme.Spacing

/** What the scale is, what the key is, and the field the key goes in. */
@Composable
internal fun ScaleSetupCard(
    state: ScalesUiState,
    onKeyInputChange: (String) -> Unit,
    onSaveKey: () -> Unit,
    onReplaceKey: () -> Unit,
    onCancelReplaceKey: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScaleCard(modifier) {
        Note(R.string.settings_scales_intro)
        if (state.keyRejected) Note(R.string.settings_scales_key_rejected, isError = true)
        if (state.showKeyField) {
            Note(R.string.settings_scales_key_help)
            OutlinedTextField(
                value = state.keyInput,
                onValueChange = onKeyInputChange,
                label = { Text(stringResource(R.string.settings_scales_key_label)) },
                isError = state.keyInputInvalid,
                supportingText = if (state.keyInputInvalid) {
                    { Text(stringResource(R.string.settings_scales_key_invalid)) }
                } else {
                    null
                },
                singleLine = true,
                // A key is not a word: no suggestions, no learned entries.
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Password,
                ),
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OpenVitalsButton(onClick = onSaveKey) { Text(stringResource(R.string.action_save)) }
                if (state.replacingKey) {
                    OpenVitalsTextButton(onClick = onCancelReplaceKey) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            }
        } else {
            OpenVitalsTonalButton(onClick = onReplaceKey) {
                Text(stringResource(R.string.settings_scales_key_replace))
            }
        }
    }
}

/** Whether the phone hears the scale, and the one thing to do when it does not. */
@Composable
internal fun ScaleListeningCard(
    state: ScalesUiState,
    android12: Boolean,
    onAllowSystemWake: () -> Unit,
    onGrantScanPermission: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
    onClaimIgnoredProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScaleCard(modifier) {
        when (state.listenerStatus) {
            ScaleListenerStatus.LISTENING -> {
                Text(
                    text = stringResource(R.string.settings_scales_listening),
                    style = MaterialTheme.typography.titleSmall,
                )
                Note(
                    when {
                        !state.isBound -> R.string.settings_scales_waiting_first
                        state.wokenBySystem -> R.string.settings_scales_listening_body
                        else -> R.string.settings_scales_listening_while_open
                    },
                )
                if (android12 && state.isBound && !state.wokenBySystem) {
                    Note(R.string.settings_scales_wake_body)
                    if (state.systemWakeFailed) Note(R.string.settings_scales_wake_failed, isError = true)
                    OpenVitalsTonalButton(onClick = onAllowSystemWake) {
                        Text(stringResource(R.string.settings_scales_wake_action))
                    }
                }
            }

            ScaleListenerStatus.PERMISSION_MISSING -> {
                Note(
                    if (android12) {
                        R.string.settings_scales_scan_permission
                    } else {
                        R.string.settings_scales_location_permission
                    },
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

            // The status before the first attempt to listen. The screen makes one as it opens.
            ScaleListenerStatus.OFF -> Unit
        }
        if (state.otherProfileIgnored) {
            Note(R.string.settings_scales_other_profile)
            OpenVitalsTextButton(onClick = onClaimIgnoredProfile) {
                Text(stringResource(R.string.settings_scales_other_profile_action))
            }
        }
    }
}

/** The last weigh-in, what is still missing from it, and the way to take it back. */
@Composable
internal fun ScaleLastWeighInCard(
    weighIn: ScaleWeighInDisplay,
    state: ScalesUiState,
    onGrantHealthConnect: () -> Unit,
    onOpenBodyProfile: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }

    ScaleCard(modifier) {
        Column {
            Text(
                text = stringResource(R.string.settings_scales_last_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = weighIn.time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            ValueRow(R.string.metric_weight, weighIn.weight)
            weighIn.heartRate?.let { ValueRow(R.string.screen_heart_rate, it) }
            weighIn.bodyFat?.let { ValueRow(R.string.metric_body_fat, it) }
            weighIn.leanMass?.let { ValueRow(R.string.metric_lean_mass, it) }
            weighIn.bodyWater?.let { ValueRow(R.string.metric_body_water_mass, it) }
        }
        when {
            weighIn.weightOnly -> Note(R.string.settings_scales_weight_only)
            weighIn.bodyFat != null -> Note(R.string.settings_scales_estimate_note)
            state.missingProfileInputs.isNotEmpty() -> {
                Note(R.string.settings_scales_profile_hint)
                OpenVitalsTonalButton(onClick = onOpenBodyProfile) {
                    Text(stringResource(R.string.settings_body_profile_group_title))
                }
            }
        }
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
        OpenVitalsTextButton(onClick = { confirmingDelete = true }) {
            Text(
                text = stringResource(R.string.action_delete),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (confirmingDelete) {
        ConfirmDialog(
            title = R.string.settings_scales_delete_title,
            body = R.string.settings_scales_delete_body,
            confirm = R.string.action_delete,
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

@Composable
internal fun ScaleRemoveCard(onRemove: () -> Unit, modifier: Modifier = Modifier) {
    var confirming by rememberSaveable { mutableStateOf(false) }

    ScaleCard(modifier) {
        OpenVitalsTextButton(onClick = { confirming = true }) {
            Text(
                text = stringResource(R.string.settings_scales_remove),
                color = MaterialTheme.colorScheme.error,
            )
        }
    }

    if (confirming) {
        ConfirmDialog(
            title = R.string.settings_scales_remove,
            body = R.string.settings_scales_remove_body,
            confirm = R.string.action_remove,
            onConfirm = {
                confirming = false
                onRemove()
            },
            onDismiss = { confirming = false },
        )
    }
}

@Composable
private fun ScaleCard(modifier: Modifier, content: @Composable () -> Unit) {
    OpenVitalsCard(modifier = modifier.fillMaxWidth()) {
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
private fun ValueRow(@StringRes label: Int, value: String) {
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

@Composable
private fun ConfirmDialog(
    @StringRes title: Int,
    @StringRes body: Int,
    @StringRes confirm: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(title)) },
        text = { Text(stringResource(body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(text = stringResource(confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
