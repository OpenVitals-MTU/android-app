package tech.mmarca.openvitals.features.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.domain.model.BleDiscoveredDevice
import tech.mmarca.openvitals.domain.model.BleSensorCapability
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.components.SectionHeader

@Composable
fun BleDevicesSettingsSection(
    modifier: Modifier = Modifier,
    viewModel: BleDevicesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted ->
        if (granted.values.all { it }) {
            viewModel.openAddFlow()
            viewModel.startScan()
        }
    }

    DisposableEffect(Unit) {
        viewModel.refresh()
        onDispose { viewModel.stopScan() }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(stringResource(R.string.settings_sensors_group_title))
        Text(
            text = stringResource(R.string.settings_sensors_group_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        if (state.devices.isEmpty()) {
            OpenVitalsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = stringResource(R.string.settings_sensors_empty_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_sensors_empty_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OpenVitalsButton(
                        onClick = {
                            val missing = viewModel.requiredBluetoothPermissions.filter {
                                ContextCompat.checkSelfPermission(context, it) !=
                                    android.content.pm.PackageManager.PERMISSION_GRANTED
                            }
                            if (missing.isEmpty()) {
                                viewModel.openAddFlow()
                                viewModel.startScan()
                            } else {
                                permissionLauncher.launch(missing.toTypedArray())
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = stringResource(R.string.settings_sensors_add_device),
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        } else {
            state.devices.forEach { device ->
                BleDeviceRow(
                    device = device,
                    onToggleEnabled = { enabled -> viewModel.setDeviceEnabled(device.id, enabled) },
                    onEdit = { viewModel.openEditDevice(device.id) },
                    onRemove = { viewModel.removeDevice(device.id) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            OpenVitalsOutlinedButton(
                onClick = {
                    val missing = viewModel.requiredBluetoothPermissions.filter {
                        ContextCompat.checkSelfPermission(context, it) !=
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    }
                    if (missing.isEmpty()) {
                        viewModel.openAddFlow()
                        viewModel.startScan()
                    } else {
                        permissionLauncher.launch(missing.toTypedArray())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.settings_sensors_add_device),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    if (state.showAddFlow) {
        BleAddDeviceDialog(
            state = state,
            onDismiss = viewModel::closeAddFlow,
            onToggleShowAllDevices = viewModel::setShowAllDevices,
            onSelectDevice = viewModel::selectDiscoveredDevice,
            onDisplayNameChange = viewModel::updateAddDisplayName,
            onToggleCapability = viewModel::toggleAddCapability,
            onWheelCircumferenceChange = viewModel::updateAddWheelCircumference,
            onSave = viewModel::saveAddedDevice,
            onOpenBluetoothSettings = {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
            },
        )
    }

    state.editingDeviceId?.let {
        BleEditDeviceDialog(
            state = state,
            onDismiss = viewModel::closeEditDevice,
            onDisplayNameChange = viewModel::updateEditDisplayName,
            onToggleCapability = viewModel::toggleEditCapability,
            onEnabledChange = viewModel::setEditEnabled,
            onWheelCircumferenceChange = viewModel::updateEditWheelCircumference,
            onSave = viewModel::saveEditedDevice,
            onRemove = { viewModel.removeDevice(it) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BleDeviceRow(
    device: BleSensorDevice,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(
        onClick = onEdit,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = device.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = device.bluetoothName ?: device.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = device.enabled, onCheckedChange = onToggleEnabled)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                device.capabilities.forEach { capability ->
                    AssistChip(
                        onClick = onEdit,
                        label = { Text(text = capabilityLabel(capability)) },
                    )
                }
            }
            TextButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.settings_sensors_remove_device),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BleAddDeviceDialog(
    state: BleDevicesUiState,
    onDismiss: () -> Unit,
    onToggleShowAllDevices: (Boolean) -> Unit,
    onSelectDevice: (BleDiscoveredDevice) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onToggleCapability: (BleSensorCapability) -> Unit,
    onWheelCircumferenceChange: (String) -> Unit,
    onSave: () -> Unit,
    onOpenBluetoothSettings: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_sensors_add_device)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (state.isScanning) {
                            stringResource(R.string.settings_sensors_scanning)
                        } else {
                            stringResource(R.string.settings_sensors_scan_stopped)
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                    FilterChip(
                        selected = state.showAllDevices,
                        onClick = { onToggleShowAllDevices(!state.showAllDevices) },
                        label = { Text(text = stringResource(R.string.settings_sensors_show_all_devices)) },
                    )
                }
                if (state.discoveredDevices.isEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.BluetoothSearching,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.settings_sensors_scan_empty),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        TextButton(onClick = onOpenBluetoothSettings) {
                            Text(text = stringResource(R.string.settings_sensors_open_bluetooth))
                        }
                    }
                } else {
                    state.discoveredDevices.forEach { device ->
                        OpenVitalsOutlinedButton(
                            onClick = { onSelectDevice(device) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(text = device.name ?: device.address)
                                Text(
                                    text = device.address,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
                state.selectedDevice?.let {
                    OutlinedTextField(
                        value = state.addDisplayName,
                        onValueChange = onDisplayNameChange,
                        label = { Text(text = stringResource(R.string.settings_sensors_device_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    if (state.isDiscoveringCapabilities) {
                        Text(text = stringResource(R.string.settings_sensors_discovering))
                    } else {
                        Text(
                            text = stringResource(R.string.settings_sensors_capabilities_title),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BleSensorCapability.entries.forEach { capability ->
                                FilterChip(
                                    selected = capability in state.addCapabilities,
                                    onClick = { onToggleCapability(capability) },
                                    label = { Text(text = capabilityLabel(capability)) },
                                )
                            }
                        }
                        state.capabilityConflicts.forEach { (capability, existing) ->
                            Text(
                                text = stringResource(
                                    R.string.settings_sensors_capability_conflict,
                                    capabilityLabel(capability),
                                    existing.displayName,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                        if (BleSensorCapability.CYCLING_SPEED_DISTANCE in state.addCapabilities) {
                            OutlinedTextField(
                                value = state.addWheelCircumferenceMm,
                                onValueChange = onWheelCircumferenceChange,
                                label = { Text(text = stringResource(R.string.settings_sensors_wheel_circumference)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                            )
                        }
                    }
                    state.errorMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onSave,
                enabled = state.selectedDevice != null &&
                    state.addCapabilities.isNotEmpty() &&
                    !state.isDiscoveringCapabilities,
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun BleEditDeviceDialog(
    state: BleDevicesUiState,
    onDismiss: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onToggleCapability: (BleSensorCapability) -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onWheelCircumferenceChange: (String) -> Unit,
    onSave: () -> Unit,
    onRemove: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_sensors_edit_device)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = state.editDisplayName,
                    onValueChange = onDisplayNameChange,
                    label = { Text(text = stringResource(R.string.settings_sensors_device_name)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = stringResource(R.string.settings_sensors_enabled))
                    Switch(checked = state.editEnabled, onCheckedChange = onEnabledChange)
                }
                Text(
                    text = stringResource(R.string.settings_sensors_capabilities_title),
                    style = MaterialTheme.typography.labelLarge,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BleSensorCapability.entries.forEach { capability ->
                        FilterChip(
                            selected = capability in state.editCapabilities,
                            onClick = { onToggleCapability(capability) },
                            label = { Text(text = capabilityLabel(capability)) },
                        )
                    }
                }
                state.capabilityConflicts.forEach { (capability, existing) ->
                    Text(
                        text = stringResource(
                            R.string.settings_sensors_capability_conflict,
                            capabilityLabel(capability),
                            existing.displayName,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (BleSensorCapability.CYCLING_SPEED_DISTANCE in state.editCapabilities) {
                    OutlinedTextField(
                        value = state.editWheelCircumferenceMm,
                        onValueChange = onWheelCircumferenceChange,
                        label = { Text(text = stringResource(R.string.settings_sensors_wheel_circumference)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
                state.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onRemove) {
                Text(text = stringResource(R.string.settings_sensors_remove_device))
            }
        },
    )
}

@Composable
private fun capabilityLabel(capability: BleSensorCapability): String =
    when (capability) {
        BleSensorCapability.HEART_RATE -> stringResource(R.string.settings_sensors_capability_heart_rate)
        BleSensorCapability.CYCLING_CADENCE -> stringResource(R.string.settings_sensors_capability_cycling_cadence)
        BleSensorCapability.CYCLING_POWER -> stringResource(R.string.settings_sensors_capability_cycling_power)
        BleSensorCapability.CYCLING_SPEED_DISTANCE ->
            stringResource(R.string.settings_sensors_capability_cycling_speed)
        BleSensorCapability.RUNNING_SPEED_CADENCE ->
            stringResource(R.string.settings_sensors_capability_running_speed_cadence)
    }
