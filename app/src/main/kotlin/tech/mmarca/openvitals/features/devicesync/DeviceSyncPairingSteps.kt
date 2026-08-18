package tech.mmarca.openvitals.features.devicesync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Backspace
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.features.devicesync.bluetooth.DiscoveredSyncDevice
import tech.mmarca.openvitals.features.devicesync.protocol.PAIRING_CODE_DIGITS
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsFilledButton
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton
import tech.mmarca.openvitals.ui.theme.Spacing

/** Step 1 — choose a role: make this phone discoverable, or find a phone. */
@Composable
internal fun DeviceSyncRoleStep(
    state: DeviceSyncState,
    onChooseHost: () -> Unit,
    onChooseGuest: () -> Unit,
) {
    LazyColumn {
        item {
            DeviceSyncHero(
                icon = Icons.Outlined.Devices,
                title = stringResource(R.string.device_sync_role_heading),
                body = stringResource(R.string.device_sync_role_body),
            )
        }
        if (state.bluetoothUnavailable) {
            item { DeviceSyncBanner(stringResource(R.string.device_sync_bluetooth_off)) }
        }
        state.error?.let { error ->
            item { DeviceSyncBanner(deviceSyncErrorText(error), isError = true) }
        }
        item {
            DeviceSyncRoleCard(
                icon = Icons.Outlined.WifiTethering,
                title = stringResource(R.string.device_sync_host_option),
                body = stringResource(R.string.device_sync_host_option_body),
                onClick = onChooseHost,
            )
        }
        item {
            DeviceSyncRoleCard(
                icon = Icons.Outlined.Smartphone,
                title = stringResource(R.string.device_sync_guest_option),
                body = stringResource(R.string.device_sync_guest_option_body),
                onClick = onChooseGuest,
            )
        }
        item {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.device_sync_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (state.lastReportText.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.device_sync_last_report),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
                )
            }
            item { DeviceSyncReportActions(state.lastReportText) }
        }
    }
}

@Composable
private fun DeviceSyncRoleCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    onClick: () -> Unit,
) {
    OpenVitalsCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = null,
            )
        }
    }
}

/** Step 2 (host) — show the code and wait for the guest to connect. */
@Composable
internal fun DeviceSyncHostStep(state: DeviceSyncState, onCancel: () -> Unit) {
    LazyColumn {
        item {
            DeviceSyncHero(
                icon = Icons.Outlined.WifiTethering,
                title = stringResource(R.string.device_sync_host_heading),
                body = stringResource(R.string.device_sync_host_body),
            )
        }
        item {
            OpenVitalsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.device_sync_code_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.code,
                        style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 8.sp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.device_sync_waiting))
                    }
                }
            }
        }
        item { DeviceSyncCancelButton(onCancel) }
    }
}

/** Step 2 (guest) — scan for nearby phones and pick one. */
@Composable
internal fun DeviceSyncScanStep(
    state: DeviceSyncState,
    onSelectDevice: (DiscoveredSyncDevice) -> Unit,
    onRescan: () -> Unit,
    onCancel: () -> Unit,
) {
    LazyColumn {
        item {
            DeviceSyncHero(
                icon = Icons.Outlined.BluetoothSearching,
                title = stringResource(R.string.device_sync_scan_heading),
                body = stringResource(R.string.device_sync_scan_body),
            )
        }
        items(state.devices, key = { it.address }) { device ->
            OpenVitalsCard(
                onClick = { onSelectDevice(device) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(imageVector = Icons.Outlined.Smartphone, contentDescription = null)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = device.name ?: device.address,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (device.bonded) {
                            Text(
                                text = stringResource(R.string.device_sync_paired),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        contentDescription = null,
                    )
                }
            }
        }
        if (state.scanning) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                }
            }
        } else {
            if (state.devices.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.device_sync_no_devices),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                    )
                }
            }
            item {
                OpenVitalsOutlinedButton(
                    onClick = onRescan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = null,
                        modifier = Modifier.padding(end = 6.dp),
                    )
                    Text(stringResource(R.string.device_sync_rescan))
                }
            }
        }
        item { DeviceSyncCancelButton(onCancel) }
    }
}

/** Step 3 (guest) — type the code shown on the host. */
@Composable
internal fun DeviceSyncCodeStep(
    state: DeviceSyncState,
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
) {
    val deviceLabel = state.selectedDevice?.let { it.name ?: it.address }.orEmpty()
    LazyColumn {
        item {
            DeviceSyncHero(
                icon = Icons.Outlined.Password,
                title = stringResource(R.string.device_sync_code_heading, deviceLabel),
                body = stringResource(R.string.device_sync_code_body),
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                repeat(PAIRING_CODE_DIGITS) { index ->
                    val filled = index < state.codeEntry.length
                    val active = index == state.codeEntry.length
                    OpenVitalsCard(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        modifier = Modifier.padding(4.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(52.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (filled) state.codeEntry[index].toString() else "",
                                style = MaterialTheme.typography.headlineSmall,
                                color = if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                            )
                        }
                    }
                }
            }
        }
        if (state.codeError) {
            item {
                DeviceSyncBanner(stringResource(R.string.device_sync_wrong_code), isError = true)
            }
        }
        state.error?.let { error ->
            item { DeviceSyncBanner(deviceSyncErrorText(error), isError = true) }
        }
        item {
            DeviceSyncKeypad(
                onDigit = onDigit,
                onDelete = onDelete,
                modifier = Modifier.padding(16.dp),
            )
        }
        item {
            OpenVitalsFilledButton(
                onClick = onSubmit,
                enabled = state.codeEntry.length == PAIRING_CODE_DIGITS,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                Text(stringResource(R.string.device_sync_connect))
            }
        }
    }
}

@Composable
private fun DeviceSyncKeypad(
    onDigit: (Char) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf(null, '0', DELETE_KEY),
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { key ->
                    if (key == null) {
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        OpenVitalsOutlinedButton(
                            onClick = { if (key == DELETE_KEY) onDelete() else onDigit(key) },
                            modifier = Modifier.weight(1f),
                        ) {
                            if (key == DELETE_KEY) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Backspace,
                                    contentDescription = stringResource(
                                        R.string.device_sync_keypad_delete,
                                    ),
                                )
                            } else {
                                Text(
                                    text = key.toString(),
                                    style = MaterialTheme.typography.titleLarge,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val DELETE_KEY = '\b'
