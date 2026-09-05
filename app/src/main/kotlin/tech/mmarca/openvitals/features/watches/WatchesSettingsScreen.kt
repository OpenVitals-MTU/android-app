package tech.mmarca.openvitals.features.watches

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
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.devices.garmin.GarminOnboardStep
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.domain.model.DeviceIntegration
import tech.mmarca.openvitals.domain.model.OsPermissionId
import tech.mmarca.openvitals.domain.model.OsPermissionRow
import tech.mmarca.openvitals.ui.components.OpenVitalsButton
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.theme.Spacing
import tech.mmarca.openvitals.ui.components.OpenVitalsOutlinedButton

/**
 * The Watches settings screen: the paired list plus the add flow. A watch
 * row only identifies and opens; every action lives in the device view.
 */
@Composable
fun WatchesSettingsScreen(
    viewModel: WatchesViewModel,
    onOpenWatch: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // One batched dialog for the runtime permissions; settings-screen ones follow through the queue.
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        viewModel.refreshOsPermissions()
        viewModel.openNextSpecialPermission()
    }

    fun requestOsPermissions(permissions: List<String>) {
        if (permissions.isEmpty()) {
            viewModel.openNextSpecialPermission()
        } else {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    DisposableEffect(Unit) {
        viewModel.refresh()
        onDispose { viewModel.stopScan() }
    }

    // Returning from a settings screen is the cue to hand over the next one.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshOsPermissions()
        viewModel.openNextSpecialPermission()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.settings_watches_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.lg),
        )

        state.onboardNotice?.let { notice ->
            // Told once, after the sheet closes. Inline: this scaffold has no snackbar host.
            OpenVitalsCard(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.dismissNotice() }
                        .padding(Spacing.lg),
                ) {
                    Text(
                        text = when (notice) {
                            WatchOnboardNotice.UNSUPPORTED_TRANSPORT ->
                                stringResource(R.string.settings_watch_unsupported_notice)

                            WatchOnboardNotice.NO_COMPANION ->
                                stringResource(R.string.settings_watch_no_companion_notice)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (state.watches.isEmpty()) {
            OpenVitalsCard(modifier = Modifier.padding(horizontal = Spacing.lg)) {
                Column(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Text(
                        text = stringResource(R.string.settings_watches_empty_title),
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = stringResource(R.string.settings_watches_empty_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OpenVitalsButton(
                        onClick = viewModel::startAdd,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(WatchPermissionIconSize),
                        )
                        Text(
                            text = stringResource(R.string.settings_watches_add),
                            modifier = Modifier.padding(start = Spacing.sm),
                        )
                    }
                }
            }
        } else {
            state.watches.forEach { watch ->
                WatchRow(
                    device = watch,
                    onOpen = { onOpenWatch(watch.id) },
                    modifier = Modifier.padding(horizontal = Spacing.lg),
                )
            }
            OpenVitalsOutlinedButton(
                onClick = viewModel::startAdd,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Spacing.lg),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    modifier = Modifier.size(WatchPermissionIconSize),
                )
                Text(
                    text = stringResource(R.string.settings_watches_add),
                    modifier = Modifier.padding(start = Spacing.sm),
                )
            }
        }
    }

    if (state.showPermissionsGate) {
        WatchPermissionsDialog(
            state = state,
            onGrantAll = {
                // Queue the settings walks before the dialog: its callback drains them.
                viewModel.queueAllSpecialPermissions()
                requestOsPermissions(state.osPermissions.requestablePermissions)
            },
            onGrantRow = { row ->
                if (row.isSpecial) {
                    viewModel.queueSpecialPermission(row.id)
                    viewModel.openNextSpecialPermission()
                } else {
                    requestOsPermissions(row.permissions)
                }
            },
            onContinue = viewModel::openAddFlow,
            onDismiss = viewModel::dismissPermissionsGate,
        )
    }

    if (state.showAddFlow) {
        AddWatchDialog(viewModel = viewModel, state = state)
    }
}

private val WatchPermissionIconSize = 18.dp

/** The permission checklist shown when adding a watch. Continuing anyway stays available. */
@Composable
private fun WatchPermissionsDialog(
    state: WatchesUiState,
    onGrantAll: () -> Unit,
    onGrantRow: (OsPermissionRow) -> Unit,
    onContinue: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.watch_permissions_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                Text(
                    text = stringResource(R.string.watch_permissions_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                // Ordered once when the dialog opens, so rows do not move under the finger.
                val rowOrder = remember {
                    state.osPermissions.rows.sortedBy { it.granted }.map { it.id }
                }
                rowOrder.mapNotNull { id -> state.osPermissions.rows.firstOrNull { it.id == id } }
                    .forEach { row ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (row.granted) {
                                    Icons.Outlined.CheckCircle
                                } else {
                                    Icons.Outlined.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (row.granted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(WatchPermissionIconSize),
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = Spacing.md),
                            ) {
                                Text(
                                    text = stringResource(row.titleRes()),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    text = stringResource(row.descriptionRes()),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (!row.granted) {
                                TextButton(onClick = { onGrantRow(row) }) {
                                    Text(text = stringResource(R.string.action_grant))
                                }
                            }
                        }
                    }
            }
        },
        confirmButton = {
            if (state.osPermissions.allGranted) {
                TextButton(onClick = onContinue) {
                    Text(text = stringResource(R.string.settings_watches_add))
                }
            } else {
                TextButton(onClick = onGrantAll) {
                    Text(text = stringResource(R.string.onboarding_action_grant_all))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onContinue) {
                Text(text = stringResource(R.string.watch_permissions_continue))
            }
        },
    )
}

private fun OsPermissionRow.titleRes(): Int = when (id) {
    OsPermissionId.BLUETOOTH -> R.string.onboarding_os_bluetooth
    OsPermissionId.NOTIFICATIONS -> R.string.onboarding_os_notifications
    OsPermissionId.LOCATION -> R.string.onboarding_os_location
    OsPermissionId.BATTERY_OPTIMIZATION -> R.string.onboarding_os_battery
    OsPermissionId.NOTIFICATION_FORWARDING -> R.string.onboarding_os_notification_forwarding
}

private fun OsPermissionRow.descriptionRes(): Int = when (id) {
    OsPermissionId.BLUETOOTH -> R.string.onboarding_os_bluetooth_desc
    OsPermissionId.NOTIFICATIONS -> R.string.onboarding_os_notifications_desc
    OsPermissionId.LOCATION -> R.string.onboarding_os_location_desc
    OsPermissionId.BATTERY_OPTIMIZATION -> R.string.onboarding_os_battery_desc
    OsPermissionId.NOTIFICATION_FORWARDING -> R.string.onboarding_os_notification_forwarding_desc
}

@Composable
private fun WatchRow(
    device: BleSensorDevice,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OpenVitalsCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(Spacing.lg),
        ) {
            WatchAvatar()
            Spacer(modifier = Modifier.width(WatchRowIconGap))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.displayName,
                    style = MaterialTheme.typography.titleSmall,
                )
                // A watch is named after its Bluetooth name; its state says more.
                Text(
                    text = if (device.enabled) {
                        stringResource(R.string.settings_watch_connected)
                    } else {
                        stringResource(R.string.settings_watch_not_connected)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    // A watch reports its battery over GFDI, not the standard service.
                    text = device.lastSyncedAt?.let {
                        stringResource(
                            R.string.settings_watch_last_synced,
                            formatWatchSyncTime(it),
                        )
                    } ?: stringResource(R.string.settings_watch_never_synced),
                    style = MaterialTheme.typography.labelSmall,
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

@Composable
private fun AddWatchDialog(
    viewModel: WatchesViewModel,
    state: WatchesUiState,
) {
    AlertDialog(
        // Bonding cannot be taken back halfway.
        onDismissRequest = { if (!state.isOnboarding) viewModel.closeAddFlow() },
        title = { Text(stringResource(R.string.settings_watches_add)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Text(
                    text = if (state.isScanning) {
                        stringResource(R.string.settings_sensors_scanning)
                    } else {
                        stringResource(R.string.settings_sensors_scan_stopped)
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                if (state.discoveredWatches.isEmpty() && state.selectedDevice == null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.BluetoothSearching,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = stringResource(R.string.settings_watches_scan_empty),
                        style = MaterialTheme.typography.bodySmall,
                    )
                } else if (state.selectedDevice == null) {
                    state.discoveredWatches.forEach { device ->
                        OpenVitalsOutlinedButton(
                            onClick = { viewModel.selectDiscoveredDevice(device) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(device.name ?: device.address)
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
                        onValueChange = viewModel::updateAddDisplayName,
                        label = { Text(stringResource(R.string.settings_watch_name_label)) },
                        singleLine = true,
                        enabled = !state.isOnboarding,
                    )
                    // A GFDI device picks no capabilities. WearOS has no bond or probe steps.
                    if (state.addingIntegration != DeviceIntegration.WEAROS) {
                        WatchPairSteps(step = state.onboardStep)
                    }
                    state.errorMessage?.let { message ->
                        Text(
                            text = message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (state.selectedDevice != null) {
                TextButton(
                    onClick = {
                        // The dialog owns the close: a refused pairing leaves the sheet open.
                        viewModel.onboardSelectedWatch()
                    },
                    enabled = !state.isOnboarding,
                ) {
                    Text(stringResource(R.string.settings_watch_pair_action))
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = viewModel::closeAddFlow,
                enabled = !state.isOnboarding,
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

/** The platform steps of onboarding, as a checklist. [step] is null outside pairing. */
@Composable
private fun WatchPairSteps(step: GarminOnboardStep?) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = stringResource(R.string.settings_watch_pair_title),
            style = MaterialTheme.typography.labelLarge,
        )
        Text(
            text = stringResource(R.string.settings_watch_pair_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.padding(top = Spacing.xs))
        WatchPairStepRow(
            label = stringResource(R.string.settings_watch_step_bonding),
            active = step == GarminOnboardStep.BONDING,
            done = step == GarminOnboardStep.ASSOCIATING || step == GarminOnboardStep.PROBING,
        )
        WatchPairStepRow(
            label = stringResource(R.string.settings_watch_step_associating),
            active = step == GarminOnboardStep.ASSOCIATING,
            done = step == GarminOnboardStep.PROBING,
        )
        Text(
            text = stringResource(R.string.settings_watch_step_associating_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.xxxl),
        )
        WatchPairStepRow(
            label = stringResource(R.string.settings_watch_step_probing),
            active = step == GarminOnboardStep.PROBING,
            done = false,
        )
    }
}

@Composable
private fun WatchPairStepRow(
    label: String,
    active: Boolean,
    done: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when {
            active -> CircularProgressIndicator(
                modifier = Modifier.size(Spacing.lg),
                strokeWidth = WatchProgressStroke,
            )

            done -> Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(WatchPermissionIconSize),
                tint = MaterialTheme.colorScheme.primary,
            )

            else -> Icon(
                imageVector = Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                modifier = Modifier.size(WatchPermissionIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (active) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
    }
}

/** The small in-row progress spinner's stroke. */
private val WatchProgressStroke = 2.dp
/** Gap between a row's leading icon and its text. */
private val WatchRowIconGap = 14.dp
