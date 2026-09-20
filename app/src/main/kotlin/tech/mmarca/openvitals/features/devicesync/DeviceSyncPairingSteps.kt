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
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Lock
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
                body = stringResource(R.string.device_sync_host_option_body_compare),
                onClick = onChooseHost,
            )
        }
        item {
            DeviceSyncRoleCard(
                icon = Icons.Outlined.Smartphone,
                title = stringResource(R.string.device_sync_guest_option),
                body = stringResource(R.string.device_sync_guest_option_body_compare),
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
internal fun DeviceSyncHostStep(onCancel: () -> Unit) {
    LazyColumn {
        item {
            DeviceSyncHero(
                icon = Icons.Outlined.WifiTethering,
                title = stringResource(R.string.device_sync_host_heading),
                body = stringResource(R.string.device_sync_host_body_compare),
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
                body = stringResource(R.string.device_sync_scan_body_compare),
            )
        }
        state.error?.let { error ->
            item { DeviceSyncBanner(deviceSyncErrorText(error), isError = true) }
        }
        items(state.devices, key = { it.address }) { device ->
            OpenVitalsCard(
                // The view model ignores a tap while it connects.
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
        if (state.connecting) {
            item {
                val deviceLabel = state.selectedDevice?.let { it.name ?: it.address }.orEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xxl),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        strokeWidth = SpinnerStroke,
                        modifier = Modifier.size(Spacing.xl),
                    )
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.device_sync_connecting, deviceLabel))
                }
            }
        } else if (state.scanning) {
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

/** Both phones show the same six digits. The user says whether they match. */
@Composable
internal fun DeviceSyncCompareStep(
    state: DeviceSyncState,
    onMatch: () -> Unit,
    onMismatch: () -> Unit,
) {
    LazyColumn {
        item {
            DeviceSyncHero(
                icon = Icons.Outlined.Lock,
                title = stringResource(R.string.device_sync_compare_heading),
                body = stringResource(R.string.device_sync_compare_body),
            )
        }
        item {
            OpenVitalsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Spacing.lg),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Spacing.xl),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.device_sync_code_label),
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        // Two groups of three are easier to compare than six in a row.
                        text = state.code.chunked(PAIRING_CODE_DIGITS / 2).joinToString(" "),
                        style = MaterialTheme.typography.displaySmall.copy(letterSpacing = 8.sp),
                    )
                }
            }
        }
        item {
            OpenVitalsFilledButton(
                onClick = onMatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.sm),
            ) {
                Text(stringResource(R.string.device_sync_compare_match))
            }
        }
        item {
            OpenVitalsOutlinedButton(
                onClick = onMismatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.lg),
            ) {
                Text(stringResource(R.string.device_sync_compare_mismatch))
            }
        }
    }
}

private val SpinnerStroke = 2.dp
