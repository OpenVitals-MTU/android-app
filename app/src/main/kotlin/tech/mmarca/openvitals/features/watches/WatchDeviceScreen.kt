package tech.mmarca.openvitals.features.watches

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.automirrored.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.BrightnessMedium
import androidx.compose.material.icons.automirrored.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import tech.mmarca.openvitals.R
import tech.mmarca.openvitals.devices.garmin.GarminCapability
import tech.mmarca.openvitals.devices.garmin.GarminSettingsService
import tech.mmarca.openvitals.domain.model.BleSensorDevice
import tech.mmarca.openvitals.ui.components.OpenVitalsCard
import tech.mmarca.openvitals.ui.components.OpenVitalsIconButton

/**
 * The watch's on-device settings tree shipped with sub-milestone 7f
 * (`WatchSettingsScreen` behind the already-registered route), so the rows
 * that open it are live.
 */
internal const val WatchSettingsTreeAvailable: Boolean = true

/** The root of the watch's settings tree (7f); Flutter's `rootScreenId`. */
internal const val WatchSettingsRootScreenId: Int = GarminSettingsService.ROOT_SCREEN_ID

/** The alarms screen's well-known id inside that tree (7f). */
internal const val WatchSettingsAlarmsScreenId: Int = GarminSettingsService.ALARMS_SCREEN_ID

/**
 * One watch, and everything about it. Port of the Flutter build's
 * `watch_device_screen.dart`.
 *
 * The order is fixed and means something: **status** (what you came to
 * check), **actions** (verbs, as icons), **configuration** (least often
 * touched, so last, with removal at the bottom where a mis-tap cannot reach
 * it).
 */
@Composable
fun WatchDeviceScreen(
    viewModel: WatchDeviceViewModel,
    onOpenData: (String) -> Unit,
    onOpenNotifications: (String) -> Unit,
    onOpenWatchSettings: (String, Int) -> Unit,
    onRemoved: () -> Unit,
    onTitleChanged: (String?) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val device = state.device

    LaunchedEffect(device?.displayName) { onTitleChanged(device?.displayName) }
    DisposableEffect(Unit) { onDispose { onTitleChanged(null) } }

    if (device == null) {
        // Removed while this screen was open, or opened from a stale entry.
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_data))
        }
        return
    }

    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveDialog by rememberSaveable { mutableStateOf(false) }

    // Only a Garmin GFDI device (watch or Edge bike computer) has GFDI
    // sync/settings/find. A WearOS watch speaks none of it — its heart rate
    // streams over BLE and its recorded data arrives via Health Connect — so
    // those controls are hidden rather than shown dead.
    val isGarmin = device.isGarminGfdi
    // A bike computer records rides, not wrist wellness — its "Data" view
    // would always be empty, so it is hidden. Its rides import into the
    // normal activity history.
    val isBikeComputer = device.isBikeComputer

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatusCard(
            device = device,
            sync = state.sync,
            isGarmin = isGarmin,
            isBikeComputer = isBikeComputer,
            onRename = { showRenameDialog = true },
        )

        if (isGarmin) {
            ActionsRow(
                device = device,
                state = state,
                isBikeComputer = isBikeComputer,
                onOpenData = { onOpenData(device.id) },
                onSync = viewModel::syncNow,
                onOpenAlarms = {
                    onOpenWatchSettings(device.id, WatchSettingsAlarmsScreenId)
                },
                onToggleFind = viewModel::toggleFind,
            )
            val finding = state.find.isFindingDevice(device.id)
            if (finding || state.find.findFailed) {
                Text(
                    text = if (finding) {
                        stringResource(R.string.settings_watch_find_ringing)
                    } else {
                        stringResource(R.string.settings_watch_find_failed)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.find.findFailed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            state.sync.errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        // Only for a Garmin watch that says it HAS a settings tree — and only
        // once 7f's browser exists to open. A watch without REALTIME_SETTINGS
        // (or any WearOS watch) has no such screen to browse.
        if (WatchSettingsTreeAvailable && isGarmin &&
            state.supports(GarminCapability.REALTIME_SETTINGS)
        ) {
            SectionHeader(stringResource(R.string.settings_watch_settings_section))
            OnDeviceSettingsRow(
                onOpen = { onOpenWatchSettings(device.id, WatchSettingsRootScreenId) },
            )
        }

        // Only for a watch that says it speaks GNCS. `supports` treats an
        // unknown capability as present, so a watch that has never synced
        // still gets the row rather than having the feature hidden from it.
        if (isGarmin && state.supports(GarminCapability.GNCS)) {
            SectionHeader(stringResource(R.string.settings_watch_notifications_section))
            NotificationsRow(onOpen = { onOpenNotifications(device.id) })
        }

        // A bike computer can broadcast standard-BLE sensors into a
        // recording. The role is opt-in from here because broadcast mode is
        // usually only on during a ride, so it must be detected while the
        // device is live.
        if (device.isLiveSensorCapable) {
            SectionHeader(stringResource(R.string.settings_bike_live_sensor_section))
            LiveSensorSection(
                device = device,
                isDetecting = state.isDetectingSensors,
                foundNothing = state.detectFoundNothing,
                onDetect = viewModel::detectBroadcastSensors,
            )
        }

        SectionHeader(stringResource(R.string.settings_watch_section_device))
        OpenVitalsCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_watch_enabled),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = device.enabled,
                    onCheckedChange = { viewModel.setEnabled(it) },
                )
            }
        }
        // Last, and its own card: a destructive action wants distance from
        // the switch above it, not adjacency.
        OpenVitalsCard {
            Text(
                text = stringResource(R.string.settings_watch_remove),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showRemoveDialog = true }
                    .padding(16.dp),
            )
        }
    }

    if (showRenameDialog) {
        RenameDialog(
            initialName = device.displayName,
            onSave = { name ->
                viewModel.rename(name)
                showRenameDialog = false
            },
            onDismiss = { showRenameDialog = false },
        )
    }
    if (showRemoveDialog) {
        ConfirmRemoveWatchDialog(
            deviceName = device.displayName,
            onConfirm = {
                showRemoveDialog = false
                viewModel.removeDevice()
                onRemoved()
            },
            onDismiss = { showRemoveDialog = false },
        )
    }
}

@Composable
private fun StatusCard(
    device: BleSensorDevice,
    sync: DeviceSyncUiState,
    isGarmin: Boolean,
    isBikeComputer: Boolean,
    onRename: () -> Unit,
) {
    // A WearOS watch has no sync concept, so its status line names the device
    // rather than a last-sync time it will never have.
    val statusLine = buildString {
        val syncedAt = device.lastSyncedAt
        if (!isGarmin) {
            append(device.bluetoothName ?: device.address)
        } else if (syncedAt == null) {
            append(stringResource(R.string.settings_watch_never_synced))
        } else {
            append(
                stringResource(
                    R.string.settings_watch_last_synced,
                    formatWatchSyncTime(syncedAt),
                ),
            )
            // Only after a sync THIS session — the count is not persisted,
            // and an invented one would be worse than none.
            val files = sync.lastFileCount
            if (files != null && files > 0) {
                append(" · ")
                append(
                    pluralStringResource(
                        R.plurals.settings_watch_synced_files,
                        files,
                        files,
                    ),
                )
            }
        }
    }

    OpenVitalsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            WatchAvatar(
                size = 44,
                icon = if (isBikeComputer) Icons.AutoMirrored.Outlined.DirectionsBike else null,
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (device.enabled) {
                        stringResource(R.string.settings_watch_connected)
                    } else {
                        stringResource(R.string.settings_watch_not_connected)
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Text(
                    text = statusLine,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            device.batteryPercent?.let { battery ->
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "$battery%",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
            OpenVitalsIconButton(onClick = onRename) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = stringResource(R.string.settings_watch_rename),
                )
            }
        }
    }
}

@Composable
internal fun ActionsRow(
    device: BleSensorDevice,
    state: WatchDeviceUiState,
    isBikeComputer: Boolean,
    onOpenData: () -> Unit,
    onSync: () -> Unit,
    onOpenAlarms: () -> Unit,
    onToggleFind: () -> Unit,
) {
    val finding = state.find.isFindingDevice(device.id)
    // One radio: syncing and finding cannot overlap, and neither can a find
    // on a second watch. Stopping THIS find stays available throughout.
    val busy = (state.sync.isSyncing || state.find.findingDeviceId != null) && !finding
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        if (!isBikeComputer) {
            WatchAction(
                icon = Icons.Outlined.Insights,
                label = stringResource(R.string.settings_watch_action_data),
                enabled = true,
                onClick = onOpenData,
            )
        }
        WatchAction(
            icon = Icons.Outlined.Sync,
            label = stringResource(R.string.settings_watch_action_sync),
            busy = state.sync.isSyncingDevice(device.id),
            // One radio, one sync: disabled while ANY watch is syncing.
            enabled = !busy && !finding && !state.sync.isSyncing,
            onClick = onSync,
        )
        // Alarms are not a feature of their own: they are a screen in the
        // watch's settings tree, reached at a well-known id — so the action
        // arrives with 7f's browser.
        if (WatchSettingsTreeAvailable && state.supports(GarminCapability.REALTIME_SETTINGS)) {
            WatchAction(
                icon = Icons.Outlined.Alarm,
                label = stringResource(R.string.settings_watch_action_alarms),
                enabled = !busy,
                onClick = onOpenAlarms,
            )
        }
        if (state.supports(GarminCapability.FIND_MY_WATCH)) {
            // A toggle, not a one-shot: the watch alerts for a minute unless
            // stopped, so the same control stops it — in place, because you
            // are rummaging through a bag one-handed.
            WatchAction(
                icon = if (finding) Icons.Outlined.Stop else Icons.Outlined.WifiTethering,
                label = if (finding) {
                    stringResource(R.string.settings_watch_find_stop)
                } else {
                    stringResource(R.string.settings_watch_action_find)
                },
                enabled = !busy,
                onClick = onToggleFind,
            )
        }
    }
}

/**
 * The way into the watch's OWN settings, at the root of its tree. Hidden
 * until 7f lands ([WatchSettingsTreeAvailable]).
 */
@Composable
internal fun OnDeviceSettingsRow(onOpen: () -> Unit) {
    OpenVitalsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.BrightnessMedium,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_watch_on_device_settings),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.settings_watch_on_device_settings_body),
                    style = MaterialTheme.typography.bodySmall,
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

/**
 * Mirrors the phone's notifications to the watch. The master switch, its two
 * gates and the per-app blocklist all live on the notifications screen (7e
 * folded them into one), so this row only identifies and opens.
 */
@Composable
private fun NotificationsRow(onOpen: () -> Unit) {
    OpenVitalsCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpen)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.settings_watch_notifications_title),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(R.string.settings_watch_notifications_body),
                    style = MaterialTheme.typography.bodySmall,
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

/**
 * The live-sensor (BLE broadcast) controls for a bike computer: its currently
 * assigned capabilities, and a button to (re)detect what the device is
 * broadcasting right now.
 */
@Composable
internal fun LiveSensorSection(
    device: BleSensorDevice,
    isDetecting: Boolean,
    foundNothing: Boolean,
    onDetect: () -> Unit,
) {
    OpenVitalsCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.settings_bike_live_sensor_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (device.capabilities.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    device.capabilities.forEach { capability ->
                        AssistChip(
                            onClick = {},
                            label = { Text(capabilityLabel(capability)) },
                        )
                    }
                }
            }
            if (foundNothing) {
                Text(
                    text = stringResource(R.string.settings_bike_no_broadcast),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            OutlinedButton(
                onClick = onDetect,
                enabled = !isDetecting,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                if (isDetecting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.BluetoothSearching,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = if (isDetecting) {
                        stringResource(R.string.settings_bike_detecting)
                    } else {
                        stringResource(R.string.settings_bike_detect_sensors)
                    },
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(10.dp))
        HorizontalDivider(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RenameDialog(
    initialName: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_watch_rename)) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.settings_watch_name_label)) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(name) }) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
